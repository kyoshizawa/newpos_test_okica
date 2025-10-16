package jp.mcapps.android.multi_payment_terminal.iCAS;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IFeliCaChip;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IFeliCaClientEventListener;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import timber.log.Timber;

public class iCASClientDemo {
    // スレッド間メッセージ
    public static final int FC_MSG_TO_MAIN_ON_FINISH            = 0x0001;             // 正常終了
    public static final int FC_MSG_TO_MAIN_ON_ERROR             = 0x0002;             // エラー終了
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_STATUS    = 0x1002;             // デバイス操作要求（STATUS）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM  = 0x1003;             // デバイス操作要求（R/W_PARAM）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_OPERATION = 0x1004;             // デバイス操作要求（OPERATION）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RESULT    = 0x1008;             // デバイス操作要求（RESULT 交通系）

    private IiCASClient _rwUIEventListener;         // R/W UIイベントリスナ
    private int _status;                            // 業務ステータス（1:処理開始 2:未確定 3:処理完了）
    private iCASClient.moneyType _money;            // 処理中のマネー種別
    private Handler _handlerToMainThread;           // メインスレッドへのハンドラ
    private boolean _bIsAborted;                    // 中断フラグ

    public iCASClientDemo(IiCASClient _listener, iCASClient.moneyType money) {
        _rwUIEventListener = _listener;
        _money = money;
        _bIsAborted = false;
    }

    public long OnStart(BusinessParameter parameter) throws JSONException, IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, InterruptedException {
        // ワーカースレッドからの通知受信処理（メインスレッドへ通知が必要な場合、ここで受信する）
        _handlerToMainThread = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (_rwUIEventListener == null) return;

                switch(msg.what) {
                    case FC_MSG_TO_MAIN_ON_FINISH:
                        Timber.tag("iCAS Demo").d("FC_MSG_TO_MAIN_ON_FINISH OnFinished");
                        _rwUIEventListener.OnFinished(msg.arg1);
                        break;
                    case FC_MSG_TO_MAIN_ON_ERROR:
                        // 処理失敗 エラーコールバック実行
                        Timber.tag("iCAS Demo").d("FC_MSG_TO_MAIN_ON_ERROR OnErrorOccurred arg1=%d string=%s", msg.arg1, (String)msg.obj);
                        _rwUIEventListener.OnErrorOccurred(msg.arg1, (String) msg.obj);
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_STATUS:
                        DeviceClient.Status status;
                        status = (DeviceClient.Status)msg.obj;
                        _status = Integer.parseInt(status.status);
                        _rwUIEventListener.OnStatusChanged(status);
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM:
                        _rwUIEventListener.OnUIUpdate((DeviceClient.RWParam)(msg.obj));
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_OPERATION:
                        _rwUIEventListener.OnOperation((DeviceClient.Operation)(msg.obj));
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_RESULT:
                        switch (_money) {
                            case MONEY_SUICA:
                                _rwUIEventListener.OnResultSuica((DeviceClient.Result)(msg.obj));
                                break;
                            case MONEY_ID:
                                _rwUIEventListener.OnResultID((DeviceClient.ResultID) (msg.obj));
                                break;
                            case MONEY_WAON:
                                _rwUIEventListener.OnResultWAON((DeviceClient.ResultWAON)(msg.obj));
                                break;
                            case MONEY_QUICPAY:
                                _rwUIEventListener.OnResultQUICPay((DeviceClient.ResultQUICPay)(msg.obj));
                                break;
                            case MONEY_EDY:
                                _rwUIEventListener.OnResultEdy((DeviceClient.ResultEdy)(msg.obj));
                                break;
                            case MONEY_NANACO:
                                _rwUIEventListener.OnResultnanaco((DeviceClient.Resultnanaco)(msg.obj));
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        _status = 1;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    demoThreadProc(parameter);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        return FeliCaClient.FC_ERR_SUCCESS;
    }

    public void demoThreadProc(BusinessParameter parameter) throws InterruptedException {
        byte[] rwCommand;
        byte[] rwResponse = new byte[256];
        byte[] idm = new byte[8];
        long lRet = 0;
        String IDi = "JK123456789012345";
        String sprwid = "JE01234567890";                    // 交通系
        String termIdentId = "9999901234567";               // iD、WAON、QUICPay
        String termIdentId_edy = "G0123456";                // Edy
        String termIdentId_nanaco = "01234567890123456789"; // nanaco
        Message message;
        DeviceClient.Status status;
        DeviceClient.RWParam rwParam = new DeviceClient.RWParam();
        int bRem = 15001;
        int rem = bRem;
        int refund = 0;
        String str = "";
        int value = 0;
        boolean bRemainErr = false;

        // value設定
        switch (_money) {
            case MONEY_SUICA:
                if(((BusinessParameter.Suica) (parameter.money)).value != null) {
                    value = Integer.parseInt(((BusinessParameter.Suica) (parameter.money)).value);
                }
                break;
            case MONEY_ID:
                if(((BusinessParameter.iD) (parameter.money)).value != null) {
                    value = Integer.parseInt(((BusinessParameter.iD) (parameter.money)).value);
                }
                break;
            case MONEY_WAON:
                if(((BusinessParameter.Waon) (parameter.money)).value != null) {
                    value = Integer.parseInt(((BusinessParameter.Waon) (parameter.money)).value);
                }
                break;
            case MONEY_QUICPAY:
                if(((BusinessParameter.QUICPay) (parameter.money)).value != null) {
                    value = Integer.parseInt(((BusinessParameter.QUICPay) (parameter.money)).value);
                }
                break;
            case MONEY_EDY:
                if(((BusinessParameter.Edy) (parameter.money)).value != null) {
                    value = Integer.parseInt(((BusinessParameter.Edy) (parameter.money)).value);
                }
                break;
            case MONEY_NANACO:
                if(((BusinessParameter.nanaco) (parameter.money)).value != null) {
                    value = Integer.parseInt(((BusinessParameter.nanaco) (parameter.money)).value);
                }
                break;
            default:
                return;
        }
        refund = value;

        // rem設定
        if(value <= bRem) {
            rem = bRem - value;
        } else {
            rem = 0;
            if(_money == iCASClient.moneyType.MONEY_SUICA
            || _money == iCASClient.moneyType.MONEY_WAON
            || _money == iCASClient.moneyType.MONEY_EDY
            || _money == iCASClient.moneyType.MONEY_NANACO) {
                bRemainErr = true;
            }
        }

        // 開始時に時間がかかるのでスリープで本物と同じような動きにする
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 0.開始時ステータス変化通知
        Timber.tag("iCAS Demo").d("Step:0");

        status = new DeviceClient.Status();
        status.status = "1";
        message = new Message();
        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_STATUS;
        message.obj = status;
        _handlerToMainThread.sendMessage(message);

        // 1.カードをかざす前のUI指示
        Timber.tag("iCAS Demo").d("Step:1");
        rwParam.bar = new Integer[3];
        rwParam.ring = new Integer[3];
        rwParam.sound = new Integer[2];
        rwParam.lcd1 = new String[3];
        rwParam.lcd2 = new String[3];
        rwParam.lcd3 = new String[3];

        switch(Integer.parseInt(parameter.businessId)) {
            case iCASClient.BUSINESS_ID_SUICA_PAY:
            case iCASClient.BUSINESS_ID_SUICA_IDI_PAY:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 1;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "P01-1-001";
                str = "ｺｳﾂｳｹｲｼﾊﾗｲ           " + String.valueOf(value) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "P01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "P01-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_SUICA_REFUND:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 1;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "P02-1-001";
                str = "ｺｳﾂｳｹｲｼﾊﾗｲﾄﾘｹｼ       " + String.valueOf(refund) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "P02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "P02-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_SUICA_REMAIN:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 1;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "P07-1-001";
                rwParam.lcd1[1] = "ｺｳﾂｳｹｲｻﾞﾝﾀﾞｶｼｮｳｶｲ        ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "P07-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "P07-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_ID_PAY:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 2;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "I01-1-001";
                str = "iDｳﾘｱｹﾞ              " + String.valueOf(value) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "I01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "I01-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_ID_REFUND:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 2;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "I02-1-001";
                str = "iDﾄﾘｹｼ                " + String.valueOf(value) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "I02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "I02-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_WAON_PAY:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 5;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "W03-1-001";
                str = "WAONｼﾊﾗｲ             " + String.valueOf(value) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "W03-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "W03-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_WAON_REFUND:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 5;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "W04-1-001";
                str = "WAONﾄﾘｹｼ             " + String.valueOf(refund) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "W04-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "W04-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_WAON_REMAIN:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.lcd1[0] = "W06-1-001";
                rwParam.lcd1[1] = "WAONｼｮｳｶｲ               ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "W06-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "W06-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_PAY:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 3;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "Q01-1-001";
                str = "QPｳﾘｱｹﾞ              " + String.valueOf(value) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "Q01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "Q01-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_REFUND:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 3;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "Q02-1-001";
                str = "QPﾄﾘｹｼ               " + String.valueOf(refund) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "Q02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "Q02-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_EDY_PAY:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 4;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "E01-1-001";
                str = "Edyｼﾊﾗｲ              " + String.valueOf(value) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "E01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "E01-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_EDY_REMAIN:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 4;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "E03-1-001";
                rwParam.lcd1[1] = "Edyｻﾞﾝﾀﾞｶｼｮｳｶｲ           ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "E03-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "E03-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_NANACO_PAY:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 6;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "N01-1-001";
                str = "ﾅﾅｺｼﾊﾗｲ              " + String.valueOf(value) + "�";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "0";
                rwParam.lcd2[0] = "N01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "0";
                rwParam.lcd3[0] = "N01-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "0";
                break;
            case iCASClient.BUSINESS_ID_NANACO_REMAIN:
                rwParam.bar[0] = 0;
                rwParam.bar[1] = 0;
                rwParam.bar[2] = 0;
                rwParam.ring[0] = 2;
                rwParam.ring[1] = 1;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 6;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "N02-1-001";
                rwParam.lcd1[1] = "ﾅﾅｺｻﾞﾝﾀﾞｶｼｮｳｶｲ           ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "N02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "N02-3-001";
                rwParam.lcd3[1] = "ﾀｯﾁｼﾃｸﾀﾞｻｲ              ";
                rwParam.lcd3[2] = "30";
                break;
            default:
                return;
        }
        message = new Message();
        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM;
        message.obj = rwParam;
        _handlerToMainThread.sendMessage(message);

        // 2.チップアクセスの無い業務はないためポーリングを実施する
        Timber.tag("iCAS Demo").d("Step:2");
        rwCommand = new byte[6];
        rwCommand[0] = 0x06;
        rwCommand[1] = 0x00;
        rwCommand[2] = (byte)0xFF;
        rwCommand[3] = (byte)0xFF;
        rwCommand[4] = 0x01;
        rwCommand[5] = 0x03;

        for(int i = 0; i < 10*15; i++) {    // 30秒間ポーリング
            lRet = _rwUIEventListener.OnTransmitRW(rwCommand, 200, rwResponse);
            if(_bIsAborted) {
                lRet = IFeliCaChip.errorCode.ERR_ABORTED.getInt();
                break;
            } else if(lRet < 0) {
                // 実行失敗
                return;
            } else if(((rwResponse[(int)lRet-2] & 0x000000FF) == 0x63) && (rwResponse[(int)lRet-1] == 0x00)) {
                // (SW1, SW2) = (0x63, 0x00)の場合はタイムアウトエラー
                lRet = IFeliCaChip.errorCode.ERR_TIMEOUT.getInt();
            } else {
                // コマンド実行成功
                lRet = IFeliCaChip.errorCode.ERR_NONE.getInt();
                // idm取得
                System.arraycopy(rwResponse, 1, idm, 0, 8);
                break;
            }
        }

        if(lRet == IFeliCaChip.errorCode.ERR_TIMEOUT.getInt() || lRet == IFeliCaChip.errorCode.ERR_ABORTED.getInt()) {
            String errorCode = "95";
            // タイムアウト or キャンセル時の処理
            if(lRet == IFeliCaChip.errorCode.ERR_ABORTED.getInt()) {
                // 実際にはキャンセルに時間がかかるのでスリープを入れる
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                errorCode = "370";
            }

            message = new Message();

            switch (_money) {
                case MONEY_SUICA:
                    DeviceClient.Result result = new DeviceClient.Result();
                    result.result = "false";
                    result.code = errorCode;
                    result.sid = parameter.sid;
                    result.sprwid = sprwid;
                    message.obj = result;
                    break;
                case MONEY_ID:
                    DeviceClient.ResultID resultID = new DeviceClient.ResultID();
                    resultID.result = "false";
                    resultID.code = errorCode;
                    resultID.sid = parameter.sid;
                    message.obj = resultID;
                    break;
                case MONEY_WAON:
                    DeviceClient.ResultWAON resultWAON = new DeviceClient.ResultWAON();
                    resultWAON.result = "false";
                    resultWAON.code = errorCode;
                    resultWAON.sid = parameter.sid;
                    message.obj = resultWAON;
                    break;
                case MONEY_QUICPAY:
                    DeviceClient.ResultQUICPay resultQuicpay = new DeviceClient.ResultQUICPay();
                    resultQuicpay.result = "false";
                    resultQuicpay.code = errorCode;
                    resultQuicpay.sid = parameter.sid;
                    message.obj = resultQuicpay;
                    break;
                case MONEY_EDY:
                    DeviceClient.ResultEdy resultEdy = new DeviceClient.ResultEdy();
                    resultEdy.result = "false";
                    resultEdy.code = errorCode;
                    resultEdy.sid = parameter.sid;
                    message.obj = resultEdy;
                    break;
                case MONEY_NANACO:
                    DeviceClient.Resultnanaco resultnanaco = new DeviceClient.Resultnanaco();
                    resultnanaco.result = "false";
                    resultnanaco.code = errorCode;
                    resultnanaco.sid = parameter.sid;
                    message.obj = resultnanaco;
                    break;
                default:
                    return;
            }
            message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
            _handlerToMainThread.sendMessage(message);

            message = new Message();
            message.what = iCASClient.FC_MSG_TO_MAIN_ON_FINISH;
            message.obj = status;
            _handlerToMainThread.sendMessage(message);

            return;
        }

        // 3.カード検出時のUI指示
        Timber.tag("iCAS Demo").d("Step:3");
        rwParam = new DeviceClient.RWParam();
        rwParam.bar = null;
        rwParam.ring = null;
        rwParam.sound = null;
        rwParam.lcd1 = new String[3];
        rwParam.lcd2 = new String[3];
        rwParam.lcd3 = new String[3];

        switch(Integer.parseInt(parameter.businessId)) {
            case iCASClient.BUSINESS_ID_SUICA_PAY:
            case iCASClient.BUSINESS_ID_SUICA_IDI_PAY:
                rwParam.lcd1[0] = "P01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "P01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "P01-3-003";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_SUICA_REFUND:
                rwParam.lcd1[0] = "P02-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "P02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "P02-3-003";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_SUICA_REMAIN:
                rwParam.lcd1[0] = "P07-1-001";
                rwParam.lcd1[1] = "ｺｳﾂｳｹｲｻﾞﾝﾀﾞｶｼｮｳｶｲ        ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "P07-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "P07-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_ID_PAY:
                rwParam.lcd1[0] = "I01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "I01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "I01-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_ID_REFUND:
                rwParam.lcd1[0] = "I02-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "I02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "I02-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_WAON_PAY:
                rwParam.lcd1[0] = "W03-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "W03-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "W03-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_WAON_REFUND:
                rwParam.lcd1[0] = "W04-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "W04-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "W04-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_WAON_REMAIN:
                rwParam.lcd1[0] = "W06-1-001";
                rwParam.lcd1[1] = "WAONｼｮｳｶｲ               ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "W06-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "W06-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_PAY:
                rwParam.lcd1[0] = "Q01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "Q01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "Q01-3-004";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_REFUND:
                rwParam.lcd1[0] = "Q02-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "Q02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "Q02-3-004";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_EDY_PAY:
                rwParam.lcd1[0] = "E01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "E01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "E01-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_EDY_REMAIN:
                rwParam.lcd1[0] = "E03-1-001";
                rwParam.lcd1[1] = "Edyｻﾞﾝﾀﾞｶｼｮｳｶｲ           ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "E03-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "E03-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            case iCASClient.BUSINESS_ID_NANACO_PAY:
                rwParam.lcd1[0] = "N01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "0";
                rwParam.lcd2[0] = "N01-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "0";
                rwParam.lcd3[0] = "N01-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "0";
                break;
            case iCASClient.BUSINESS_ID_NANACO_REMAIN:
                rwParam.lcd1[0] = "N02-1-001";
                rwParam.lcd1[1] = "ﾅﾅｺｻﾞﾝﾀﾞｶｼｮｳｶｲ           ";
                rwParam.lcd1[2] = "30";
                rwParam.lcd2[0] = "N02-2-000";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "30";
                rwParam.lcd3[0] = "N02-3-002";
                rwParam.lcd3[1] = "ｶｰﾄﾞｦﾊﾅｻﾅｲﾃﾞｸﾀﾞｻｲ       ";
                rwParam.lcd3[2] = "30";
                break;
            default:
                return;
        }
        message = new Message();
        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM;
        message.obj = rwParam;
        _handlerToMainThread.sendMessage(message);

        // 4.カード書き込み時のSTATUS通知
        Timber.tag("iCAS Demo").d("Step:4");
        if(!parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_SUICA_REMAIN))
                && !parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_WAON_REMAIN))
                && !parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_HISTORY))
                && !parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_EDY_REMAIN))
                && !parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_NANACO_REMAIN))) {
            Thread.sleep(300);

            status = new DeviceClient.Status();
            status.status = "2";
            message = new Message();
            message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_STATUS;
            message.obj = status;
            _handlerToMainThread.sendMessage(message);
        }
        Thread.sleep(500);

        // 5.完了時のUI指示
        Timber.tag("iCAS Demo").d("Step:5");
        rwParam = new DeviceClient.RWParam();
        rwParam.bar = new Integer[3];
        rwParam.ring = new Integer[3];
        rwParam.sound = new Integer[2];
        rwParam.lcd1 = new String[3];
        rwParam.lcd2 = new String[3];
        rwParam.lcd3 = new String[3];

        switch(Integer.parseInt(parameter.businessId)) {
            case iCASClient.BUSINESS_ID_SUICA_PAY:
            case iCASClient.BUSINESS_ID_SUICA_IDI_PAY:
                if(bRemainErr) {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 2;
                    rwParam.bar[2] = 5;
                    rwParam.sound[0] = 1;
                    rwParam.sound[1] = 3;
                    rwParam.lcd2[0] = "P01-2-002";
                    rwParam.lcd2[1] = "ｼﾊﾗｲﾏｴ             " + String.valueOf(bRem) + "�";
                    rwParam.lcd2[2] = "5";
                    rwParam.lcd3[0] = "P01-3-005";
                    rwParam.lcd3[1] = "ｻﾞﾝﾀﾞｶﾌﾞｿｸﾃﾞｽ           ";
                    rwParam.lcd3[2] = "5";
                } else {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 1;
                    rwParam.bar[2] = 5;
                    rwParam.sound[0] = 1;
                    rwParam.sound[1] = 4;
                    rwParam.lcd2[0] = "P01-2-001";
                    rwParam.lcd2[1] = "ｻﾞﾝﾀﾞｶ            " + String.valueOf(rem) + "�";
                    rwParam.lcd2[2] = "5";
                    rwParam.lcd3[0] = "P01-3-004";
                    rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                    rwParam.lcd3[2] = "5";
                }
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 5;
                rwParam.lcd1[0] = "P01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_SUICA_REFUND:
                if(refund > bRem) {
                    rem = refund;
                } else {
                    rem = bRem;
                }
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 5;
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.sound[0] = 1;
                rwParam.sound[1] = 4;
                rwParam.lcd1[0] = "P02-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "P02-2-001";
                rwParam.lcd2[1] = "ｻﾞﾝﾀﾞｶ            " + String.valueOf(rem) + "�";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "P02-3-004";
                rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_SUICA_REMAIN:
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 5;
                rwParam.sound[0] = 1;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "P07-1-001";
                rwParam.lcd1[1] = "ｺｳﾂｳｹｲｻﾞﾝﾀﾞｶｼｮｳｶｲ        ";
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "P07-2-001";
                rwParam.lcd2[1] = "ｻﾞﾝﾀﾞｶ            " + String.valueOf(bRem) + "�";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "P07-3-003";
                rwParam.lcd3[1] = "ｻﾞﾝﾀﾞｶｼｮｳｶｲｶﾝﾘｮｳ        ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_ID_PAY:
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.sound[0] = 2;
                rwParam.sound[1] = 4;
                rwParam.lcd1[0] = "I01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "I01-2-001";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "I01-3-004";
                rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_ID_REFUND:
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.sound[0] = 2;
                rwParam.sound[1] = 4;
                rwParam.lcd1[0] = "I02-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "I02-2-001";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "I02-3-004";
                rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_WAON_PAY:
                if(bRemainErr) {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 2;
                    rwParam.bar[2] = 5;
                    rwParam.sound[0] = 5;
                    rwParam.sound[1] = 5;
                    rwParam.lcd2[0] = "W03-2-002";
                    rwParam.lcd2[1] = "ｼﾊﾗｲﾏｴ             " + String.valueOf(bRem) + "�";
                    rwParam.lcd2[2] = "5";
                    rwParam.lcd3[0] = "W03-3-008";
                    rwParam.lcd3[1] = "ｻﾞﾝﾀﾞｶﾌﾞｿｸﾃﾞｽ           ";
                    rwParam.lcd3[2] = "5";
                } else {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 1;
                    rwParam.bar[2] = 5;
                    rwParam.sound[0] = 5;
                    rwParam.sound[1] = 4;
                    rwParam.lcd2[0] = "W03-2-001";
                    rwParam.lcd2[1] = "WAONｻﾞﾝﾀﾞｶ        " + String.valueOf(rem) + "�";
                    rwParam.lcd2[2] = "5";
                    rwParam.lcd3[0] = "W03-3-005";
                    rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                    rwParam.lcd3[2] = "5";
                }
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.lcd1[0] = "W03-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_WAON_REFUND:
                if(refund > bRem) {
                    rem = refund;
                } else {
                    rem = bRem;
                }
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 5;
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.sound[0] = 1;
                rwParam.sound[1] = 4;
                rwParam.lcd1[0] = "W04-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "W04-2-001";
                rwParam.lcd2[1] = "WAONｻﾞﾝﾀﾞｶ        " + String.valueOf(rem) + "�";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "W04-3-004";
                rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_WAON_REMAIN:
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 5;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "W06-1-001";
                rwParam.lcd1[1] = "WAONｼｮｳｶｲ               ";
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "W06-2-001";
                rwParam.lcd2[1] = "WAONｻﾞﾝﾀﾞｶ        " + String.valueOf(bRem) + "�";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "W06-3-003";
                rwParam.lcd3[1] = "ｼｮｳｶｲｶﾝﾘｮｳ              ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_PAY:
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 3;
                rwParam.sound[1] = 4;
                rwParam.lcd1[0] = "Q01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "Q01-2-001";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "Q01-3-005";
                rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_QUICPAY_REFUND:
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.sound[0] = 3;
                rwParam.sound[1] = 4;
                rwParam.lcd1[0] = "Q02-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "Q02-2-001";
                rwParam.lcd2[1] = "                        ";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "Q02-3-005";
                rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_EDY_PAY:
                if(bRemainErr) {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 2;
                    rwParam.bar[2] = 5;
                    rwParam.sound[0] = 1;
                    rwParam.sound[1] = 3;
                    rwParam.lcd2[0] = "E01-2-002";
                    rwParam.lcd2[1] = "ｼﾊﾗｲﾏｴ             " + String.valueOf(bRem) + "�";
                    rwParam.lcd2[2] = "5";
                    rwParam.lcd3[0] = "E01-3-007";
                    rwParam.lcd3[1] = "ｻﾞﾝﾀﾞｶﾌﾞｿｸﾃﾞｽ           ";
                    rwParam.lcd3[2] = "5";
                } else {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 1;
                    rwParam.bar[2] = 5;
                    rwParam.sound[0] = 4;
                    rwParam.sound[1] = 4;
                    rwParam.lcd2[0] = "E01-2-001";
                    rwParam.lcd2[1] = "Edyｻﾞﾝﾀﾞｶ         " + String.valueOf(rem) + "�";
                    rwParam.lcd2[2] = "5";
                    rwParam.lcd3[0] = "E01-3-004";
                    rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                    rwParam.lcd3[2] = "5";
                }
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.lcd1[0] = "E01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_EDY_REMAIN:
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 5;
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 4;
                rwParam.sound[1] = 99;
                rwParam.lcd1[0] = "E03-1-001";
                rwParam.lcd1[1] = "Edyｻﾞﾝﾀﾞｶｼｮｳｶｲ           ";
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "E03-2-001";
                rwParam.lcd2[1] = "Edyｻﾞﾝﾀﾞｶ         " + String.valueOf(bRem) + "�";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "E03-3-003";
                rwParam.lcd3[1] = "ｼｮｳｶｲｶﾝﾘｮｳ              ";
                rwParam.lcd3[2] = "5";
                break;
            case iCASClient.BUSINESS_ID_NANACO_PAY:
                if(bRemainErr) {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 2;
                    rwParam.bar[2] = 2;
                    rwParam.sound[0] = 6;
                    rwParam.sound[1] = 3;
                    rwParam.lcd2[0] = "N01-2-002";
                    rwParam.lcd2[1] = "ｼﾊﾗｲﾏｴ             " + String.valueOf(bRem) + "�";
                    rwParam.lcd2[2] = "5";
                    rwParam.lcd3[0] = "N01-3-004";
                    rwParam.lcd3[1] = "ｻﾞﾝﾀﾞｶﾌﾞｿｸﾃﾞｽ           ";
                    rwParam.lcd3[2] = "5";
                } else {
                    rwParam.bar[0] = 1;
                    rwParam.bar[1] = 1;
                    rwParam.bar[2] = 1;
                    rwParam.sound[0] = 6;
                    rwParam.sound[1] = 4;
                    rwParam.lcd2[0] = "N01-2-001";
                    rwParam.lcd2[1] = "ﾅﾅｺｻﾞﾝﾀﾞｶ         " + String.valueOf(rem) + "�";
                    rwParam.lcd2[2] = "3";
                    rwParam.lcd3[0] = "N01-3-003";
                    rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ          ";
                    rwParam.lcd3[2] = "3";
                }
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.lcd1[0] = "N01-1-001";
                rwParam.lcd1[1] = str;
                rwParam.lcd1[2] = "3";
                break;
            case iCASClient.BUSINESS_ID_NANACO_REMAIN:
                rwParam.bar[0] = 1;
                rwParam.bar[1] = 1;
                rwParam.bar[2] = 1;
                rwParam.ring[0] = 0;
                rwParam.ring[1] = 0;
                rwParam.ring[2] = 0;
                rwParam.sound[0] = 6;
                rwParam.sound[1] = 5;
                rwParam.lcd1[0] = "N02-1-001";
                rwParam.lcd1[1] = "ﾅﾅｺｻﾞﾝﾀﾞｶｼｮｳｶｲ           ";
                rwParam.lcd1[2] = "5";
                rwParam.lcd2[0] = "N02-2-001";
                rwParam.lcd2[1] = "ﾅﾅｺｻﾞﾝﾀﾞｶ         " + String.valueOf(bRem) + "�";
                rwParam.lcd2[2] = "5";
                rwParam.lcd3[0] = "N02-3-003";
                rwParam.lcd3[1] = "ｱﾘｶﾞﾄｳｺﾞｻﾞｲﾏｼﾀ           ";
                rwParam.lcd3[2] = "5";
                break;
            default:
                return;
        }
        message = new Message();
        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM;
        message.obj = rwParam;
        _handlerToMainThread.sendMessage(message);

        // 6.完了時ステータス変化通知
        Timber.tag("iCAS Demo").d("Step:6");
        status = new DeviceClient.Status();
        status.status = "3";
        message = new Message();
        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_STATUS;
        message.obj = status;
        _handlerToMainThread.sendMessage(message);

        // 7.OnResult通知
        Timber.tag("iCAS Demo").d("Step:7");
        message = new Message();

        switch (_money) {
            case MONEY_SUICA:
                DeviceClient.Result result = new DeviceClient.Result();
                result.result = "true";
                result.IDm = new String(idm);
                result.IDi = IDi;
                if(parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_SUICA_REFUND))) {
                    if(refund > bRem) {
                        result.bRem = "0";
                    } else {
                        result.bRem = String.valueOf(bRem - refund);
                    }
                    result.rem = String.valueOf(rem);
                } else {
                    result.bRem = String.valueOf(bRem);
                    result.rem = String.valueOf(rem);
                }
                result.sid = parameter.sid;
                result.sprwid = sprwid;
                if(!Objects.equals(parameter.businessId, String.valueOf(iCASClient.BUSINESS_ID_SUICA_REMAIN)) && !bRemainErr) {
                    if(parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_SUICA_REFUND))) {
                        result.ICsequence = "2";
                    } else {
                        result.ICsequence = "1";
                    }
                    result.SFLogID = "3219";
                    result.oldSFLogID = "3218";
                    result.oldstatementID = "3218";
                    result.time = "20" + parameter.time;
                    result.value = ((BusinessParameter.Suica) (parameter.money)).value;
                    if(parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_SUICA_REFUND))) {
                        result.value = String.valueOf(refund);
                    }
                }
                if(bRemainErr) {
                    result.result = "false";
                    result.rem = String.valueOf(bRem);
                    result.code = "88";
                }
                message.obj = result;
                break;
            case MONEY_ID:
                DeviceClient.ResultID resultID = new DeviceClient.ResultID();
                resultID.result = "true";
                resultID.businessId = parameter.businessId;
                resultID.cardCompMaskMembershipNum = "1234567890123456";
                resultID.effectiveTerm = "20990101";
                resultID.memberMaskMembershipNum = "1234567890123***";
                resultID.payment = "sin";
                resultID.sid = parameter.sid;
                if(parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_ID_REFUND))) {
                    resultID.oldSlipNo = "1";
                    resultID.slipNo = "2";
                } else {
                    resultID.slipNo = "1";
                }
                resultID.tenantMaskMembershipNum = "1234567890123***";
                resultID.termIdentId = termIdentId;
                resultID.time = "20" + parameter.time;
                resultID.totalAmount = String.valueOf(value);
                resultID.trade = String.valueOf(value);
                resultID.value = String.valueOf(value);
                resultID.userMaskMembershipNum = "************3456";
                message.obj = resultID;
                break;
            case MONEY_WAON:
                DeviceClient.ResultWAON resultWAON = new DeviceClient.ResultWAON();
                resultWAON.result = "true";
                resultWAON.value = String.valueOf(value);
                resultWAON.idm = new String(idm);
                resultWAON.businessId = parameter.businessId;
                resultWAON.termIdentId = termIdentId;
                resultWAON.pointGrantType = "0";
                resultWAON.point = String.valueOf(value/200);
                resultWAON.totalPoint = "100";
                resultWAON.waonNum = "1234567890123456";
                resultWAON.time = "20" + parameter.time;
                if(parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_WAON_REFUND))) {
                    resultWAON.oldSlipNo = "1";
                    resultWAON.slipNo = "2";
                    resultWAON.cardThroughNum = "102";
                } else {
                    resultWAON.slipNo = "1";
                    resultWAON.cardThroughNum = "101";
                }

                if(parameter.businessId.equals(String.valueOf(iCASClient.BUSINESS_ID_WAON_REFUND))) {
                    if(refund > bRem) {
                        resultWAON.beforeBalance = "0";
                    } else {
                        resultWAON.beforeBalance = String.valueOf(bRem - refund);
                    }
                    resultWAON.balance = String.valueOf(rem);
                } else {
                    resultWAON.beforeBalance = String.valueOf(bRem);
                    resultWAON.balance = String.valueOf(rem);
                }
                resultWAON.sid = parameter.sid;
                if(!Objects.equals(parameter.businessId, String.valueOf(iCASClient.BUSINESS_ID_WAON_REMAIN)) && !bRemainErr) {
                    // 残高照会以外に入っているデータ
                } else if(Objects.equals(parameter.businessId, String.valueOf(iCASClient.BUSINESS_ID_WAON_REMAIN))) {
                    resultWAON.addInfo = new DeviceClient.AddInfo();
                    DeviceClient.HistoryData[] historyData = new DeviceClient.HistoryData[3];
                    historyData[0] = new DeviceClient.HistoryData();
                    historyData[0].balance = String.valueOf(bRem);
                    historyData[0].cardThroughNum = "3";
                    historyData[0].historyDate = "20210512";
                    historyData[0].historyTime = "1005";
                    historyData[0].terminalId = termIdentId;
                    historyData[0].terminalThroughNum = "17";
                    historyData[0].tradeTypeCode = "10";
                    historyData[0].chargeType = "0";
                    historyData[0].chargeValue = "0";
                    historyData[0].value = String.valueOf(bRem);
                    historyData[1] = new DeviceClient.HistoryData();
                    historyData[1].balance = "0";
                    historyData[1].cardThroughNum = "2";
                    historyData[1].historyDate = "20210512";
                    historyData[1].historyTime = "0955";
                    historyData[1].terminalId = termIdentId;
                    historyData[1].terminalThroughNum = "16";
                    historyData[1].tradeTypeCode = "01";
                    historyData[1].chargeType = "0";
                    historyData[1].chargeValue = "0";
                    historyData[1].value = String.valueOf(bRem);
                    historyData[2] = new DeviceClient.HistoryData();
                    historyData[2].balance = String.valueOf(bRem);
                    historyData[2].cardThroughNum = "1";
                    historyData[2].historyDate = "20210510";
                    historyData[2].historyTime = "1356";
                    historyData[2].terminalId = termIdentId;
                    historyData[2].terminalThroughNum = "15";
                    historyData[2].tradeTypeCode = "01";
                    historyData[2].chargeType = "0";
                    historyData[2].chargeValue = "0";
                    historyData[2].value = "300";
                    resultWAON.addInfo.historyData = historyData;
                }
                if(bRemainErr) {
                    resultWAON.result = "false";
                    resultWAON.balance = String.valueOf(bRem);
                    resultWAON.code = "817";
                }
                message.obj = resultWAON;
                break;
            case MONEY_QUICPAY:
                DeviceClient.ResultQUICPay resultQUICPay = new DeviceClient.ResultQUICPay();
                resultQUICPay.result = "true";
                resultQUICPay.businessId = parameter.businessId;
                resultQUICPay.sid = parameter.sid;
                resultQUICPay.value = ((BusinessParameter.QUICPay) (parameter.money)).value;
                resultQUICPay.termIdentId = termIdentId;
                resultQUICPay.cardCompMaskMembershipNum = "1234567890123***";
                resultQUICPay.memberMaskMembershipNum = "12345678901234567***";
                resultQUICPay.membershipNum = "1234567890123***";
                resultQUICPay.userMaskMembershipNum = "****************7890";
                resultQUICPay.time = "20" + parameter.time;
                if(Objects.equals(parameter.businessId, String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_REFUND))) {
                    resultQUICPay.oldSlipNo = "1";
                    resultQUICPay.slipNo = "2";
                } else {
                    resultQUICPay.slipNo = "1";
                }
//                resultQUICPay.addInfo.historyData = new DeviceClient.HistoryDataQUICPay[1];
//                resultQUICPay.addInfo.historyData[0] = new DeviceClient.HistoryDataQUICPay();
                //ADD-S BMT S.Oyama 2025/01/23 フタバ双方向向け改修
                resultQUICPay.acquierName = "デモ";
                //ADD-E BMT S.Oyama 2025/01/23 フタバ双方向向け改修
                message.obj = resultQUICPay;
                break;
            case MONEY_EDY:
                DeviceClient.ResultEdy resultEdy = new DeviceClient.ResultEdy();
                resultEdy.result = "true";
                resultEdy.businessId = parameter.businessId;
                resultEdy.sid = parameter.sid;
                resultEdy.value = ((BusinessParameter.Edy) (parameter.money)).value;
                resultEdy.termIdentId = termIdentId_edy;
                resultEdy.balance = String.valueOf(rem);
                resultEdy.saleHistories = new DeviceClient.SaleHistoryEdy[1];
                resultEdy.saleHistories[0] = new DeviceClient.SaleHistoryEdy();
                resultEdy.saleHistories[0].beforeBalance = String.valueOf(bRem);
                resultEdy.saleHistories[0].afterBalance = String.valueOf(rem);
                resultEdy.saleHistories[0].cardTransactionNo = "1";
                resultEdy.saleHistories[0].edyTransactionNo = "1";
                resultEdy.saleHistories[0].memberMaskMembershipNum = "************3456";
                resultEdy.saleHistories[0].userMaskMembershipNum = "************3456";

                if(bRemainErr) {
                    resultEdy.result = "false";
                    resultEdy.balance = String.valueOf(bRem);
                    resultEdy.code = "88";
                }
                message.obj = resultEdy;
                break;
            case MONEY_NANACO:
                DeviceClient.Resultnanaco resultnanaco = new DeviceClient.Resultnanaco();
                resultnanaco.result = "true";
                resultnanaco.businessId = parameter.businessId;
                resultnanaco.sid = parameter.sid;
                resultnanaco.value = ((BusinessParameter.nanaco) (parameter.money)).value;
                resultnanaco.termIdentId = termIdentId_nanaco;
                resultnanaco.balance = String.valueOf(rem);
                resultnanaco.saleHistories = new DeviceClient.SaleHistorynanaco[1];
                resultnanaco.saleHistories[0] = new DeviceClient.SaleHistorynanaco();
                resultnanaco.saleHistories[0].beforeBalance = String.valueOf(bRem);
                resultnanaco.saleHistories[0].afterBalance = String.valueOf(rem);
                resultnanaco.saleHistories[0].cardTransactionNo = "1";
                resultnanaco.saleHistories[0].slipNo = "1";
                resultnanaco.saleHistories[0].memberMaskNanacoNum = "123456789012****";
                resultnanaco.saleHistories[0].cardCompMaskNanacoNum = "123456789012****";
                resultnanaco.saleHistories[0].tenantMaskNanacoNum = "123456789012****";
                resultnanaco.saleHistories[0].userMaskNanacoNum = "************3456";
                resultnanaco.logfullFlg = "false";
                resultnanaco.nearfullFlg = "false";

                if(bRemainErr) {
                    resultnanaco.result = "false";
                    resultnanaco.balance = String.valueOf(bRem);
                    resultnanaco.code = "88";
                }
                message.obj = resultnanaco;
                break;
            default:
                return;
        }
        message.what = iCASClient.FC_MSG_TO_MAIN_ON_OPERATE_RESULT;
        _handlerToMainThread.sendMessage(message);

        // 8.OnFinish通知
        Timber.tag("iCAS Demo").d("Step:8");
        message = new Message();
        message.what = iCASClient.FC_MSG_TO_MAIN_ON_FINISH;
        message.obj = status;
        _handlerToMainThread.sendMessage(message);
    }

    public long OnStop(boolean bForced) {
        _bIsAborted = true;

        return FeliCaClient.FC_ERR_SUCCESS;
    }
}
