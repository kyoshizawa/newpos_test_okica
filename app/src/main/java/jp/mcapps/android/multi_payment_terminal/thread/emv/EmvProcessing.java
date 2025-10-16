package jp.mcapps.android.multi_payment_terminal.thread.emv;

import android.os.Build;

import androidx.annotation.RequiresApi;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.ui.credit_card.CreditCardScanFragment;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;
import com.pos.device.SDKException;
import com.pos.device.emv.CandidateListApp;
import com.pos.device.emv.CoreParam;
import com.pos.device.emv.EMVHandler;
import com.pos.device.emv.IEMVCallback;
import com.pos.device.emv.IEMVHandler;
import com.pos.device.emv.TerminalMckConfigure;
import com.pos.device.ped.IccOfflinePinApdu;
import com.pos.device.ped.KeySystem;
import com.pos.device.ped.Ped;
import com.pos.device.ped.PedRetCode;
import com.pos.device.ped.PinBlockCallback;
import com.pos.device.ped.RsaPinKey;
import com.pos.device.icc.ContactCard;
import com.pos.device.icc.IccReader;
import com.pos.device.icc.OperatorMode;
import com.pos.device.icc.SlotType;
import com.pos.device.icc.VCC;
import com.secure.api.PadView;

import java.nio.ByteBuffer;

import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.TLVUtil;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;

public class EmvProcessing {

    private IEMVHandler mEmvHandler;
    private IccReader mIccReader;
    private ContactCard mContactCard;
    private CreditSettlement _creditSettlement;
    private InputStat _inputStat = new InputStat();
    private int currentLanguageType =0;//default is english(0),chinese is 1
    private int _appNum = 0;
    private int _appNumber = 0;
    private String[] _appList;
    private byte[] _resCode = null;
    private byte[] _authCode = null;
    private byte[] _authData = null;
    private byte[] _scriptData = null;
    private String _cafisErrorCd = null;
    private int _pinRetCode = 0;
    private byte[] _statusWord;
    private int _pinResult;

    private final String LOGTAG = "EMV処理";

    public static final int k_ICPROC_PIN_OK = 0;                    // 本人確認OK
    public static final int k_ICPROC_PINBYPASS = 1;                 // 本人確認OK（PINバイパス）
    public static final int k_ICPROC_COMBI_CVM = 2;                 // 本人確認OK（PINとサインの両方が必要）
    public static final int k_ICPROC_QPS = 3;                       // QPS（PIN不要、サイン不要）
    public static final int k_ICPROC_PINRETRY = 4;                  // PIN再入力
    public static final int k_ICPROC_OFFLINEDATAAUTH_NG = -1;       // オフラインデータ認証NG
    public static final int k_ICPROC_CHOLDER_VERIFY_NG = -2;        // 本人確認NG
    public static final int k_ICPROC_TERM_RISK_MANAGE_NG = -3;      // 端末リスク管理NG
    public static final int k_ICPROC_OFFLINE = -4;                  // オフライン判定
    public static final int k_ICPROC_CANCEL = -5;                   // キャンセル
    public static final int k_ICPROC_TIMEOUT = -6;                  // タイムアウト
    public static final int k_ICPROC_REMOVE_CARD = -7;              // カードが抜かれた
    public static final int k_ICPROC_PINBYPASS_DISABLE = -8;        // PINバイパス不可

    public static final int k_PIN_RESULT_NONE = 0;                  // PIN未入力
    public static final int k_PIN_RESULT_OK = 1;                    // PIN OK
    public static final int k_PIN_RESULT_NG = -1;                   // PIN NG
    public static final int k_PIN_RESULT_CANCEL = -2;               // PIN入力キャンセル
    public static final int k_PIN_RESULT_TIMEOUT = -3;              // PIN入力タイムアウト
    public static final int k_PIN_RESULT_REMOVE_CARD = -4;              // PIN入力中にカードが抜かれた

    public static final int k_CVMCODE_MASK = 0x1F;
    public static final int k_CVMCODE_PLAINPIN = 0x01;              // 平文PIN
    public static final int k_CVMCODE_PLAINPIN_SIGNATURE = 0x03;    // 平文PINとサイン
    public static final int k_CVMCODE_ENCPIN = 0x04;                // 暗号PIN
    public static final int k_CVMCODE_ENCPIN_SIGNATURE = 0x05;      // 暗号PINとサイン
    public static final int k_CVMCODE_SIGNATURE = 0x1E;             // サイン
    public static final int k_CVMCODE_NO_CVM_PERFORMED = 0x1F;      // PIN不要、サイン不要
    public static final int k_CVMCONDCODE_SUPPORT_CVM = 0x03;       // CVM type をサポート

    public static final int k_AUTH_DATA_LEN = 10;                   // AuthenticationData データ長

    public static EmvProcessing newInstance() {
        return new EmvProcessing();
    }

    public int init(CreditSettlement creditSettlement) throws SDKException {
        mEmvHandler = EMVHandler.getInstance();
        mIccReader = IccReader.getInstance(SlotType.USER_CARD);
        // ICカードとの接続確立
        try {
            mContactCard = mIccReader.connectCard(VCC.VOLT_5, OperatorMode.EMV_MODE);
        } catch (SDKException e) {
            Timber.tag(LOGTAG).e("SDKException connectCard");
            Timber.e(e);
            e.printStackTrace();
            return -1;
        }

        _creditSettlement = creditSettlement;

        // カーネル初期化
        if (false == initEmvKernel()) {
            return -1;
        }

        // EMV設定情報の登録
        ParamManage pm = new ParamManage();
        int ret = pm.loadParam(_creditSettlement._fragmentActivity);

        return ret;
    }

    /**
     * アプリケーション選択
     * @return 0：正常終了
     *         0以外：エラーコード
     */
    public int selectApp() {
        int ret = 0;

        Timber.tag(LOGTAG).d("selectApp");

        // アプリケーション選択
        _appNum = 0;
        ret = mEmvHandler.selectApp(0);
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail selectApp %d", ret);
            return -1;
        }

        return ret;
    }

    private int selectApplication() {
        int[] numData = new int[1];
        CandidateListApp[] listApp = new CandidateListApp[32];
        try {
            listApp = mEmvHandler.getCandidateList();
        } catch (SDKException e) {
            Timber.tag(LOGTAG).e("SDKException getCandidateList");
            Timber.e(e);
            e.printStackTrace();
        }
        int ret = 0;
        if (listApp == null) {
            Timber.tag(LOGTAG).e("Fail listApp is null");
            return -1;
        }
        numData[0] = listApp.length;
        _appNum = numData[0];
        if (listApp.length > 0) {
            ret = numData[0];
            //handling multiple application
            StringBuilder sb = new StringBuilder();
            String[] list = new String[numData[0]];
            for (int i = 0; i < numData[0]; i++) {
                String name = new String(listApp[i].gettCandAppName());
                Timber.tag(LOGTAG).i("アプリケーションリスト[%d]：%s", i, name);
                list[i] = name;
                String aid = new String(listApp[i].getAID());
                String temp = name + "," + aid + "," + listApp[i].getPriority() + "," + listApp[i].getSupportPriority();
                if (i == 0)
                    sb.append(temp);
                else
                    sb.append(";").append(temp);
            }

            _appList = list;

            if (numData[0] == 1) {
                return 0;
            } else {
                // アプリケーション選択画面表示
                _creditSettlement.startSelectApplication(list);
                return ret;
            }
        } else {
            Timber.tag(LOGTAG).e("Fail Applist length");
            return -1;
        }
    }

    public void setApplication(int appNumber)
    {
        Timber.tag(LOGTAG).i("アプリケーション[%d]を選択", appNumber);
        _appNumber = appNumber;
        // アプリケーション選択待ち解除
        endWaitInput();
    }

    /**
     * ICカードデータ読み出し
     * @return 0：正常終了
     *         0以外：エラーコード
     */
    public int emvReadData() {
        int ret = 0;

        Timber.tag(LOGTAG).d("emvReadData");

        // ICカードデータ読み込み
        try {
            ret = mEmvHandler.readAppData();
        } catch (SDKException e) {
            Timber.tag(LOGTAG).e("SDKException readAppData");
            Timber.e(e);
            e.printStackTrace();
        }
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail readAppData %d", ret);
            return -1;
        }

        // ＩＣカード対応 ＰＯＳ ガイドライン １．４ 版 Ｐ９
        // 「カードホルダー確認が必要（カード内のApplication Priority Indicatorのビット８がON）の場合は、
        // カードホルダーによるアプリケーション選択が必要となる」に従い、
        // アプリケーションが1個でも、カードホルダー確認が必要な場合はアプリケーション選択を表示する
        byte[] val = getTagValue(new byte[]{(byte) 0x95});
        val = getTagValue(new byte[]{(byte) 0x87});
        if (null != val) {
            if ((1 == _appNum) && (0 != (val[0] & (byte)0x80))) {
                // アプリケーション選択画面表示
                Timber.tag(LOGTAG).i("Select app by API bit 8 ON");
                _creditSettlement.startSelectApplication(_appList);
                // アプリケーション選択待ち
                startWaitInput();

                // アプリ選択後の setApplication() で endWaitInput() によって入力待ち解除
            }
        }

        return 0;
    }

    /**
     * ICカード処理（PIN入力まで）
     * @return 0：正常終了
     *         k_ICPROC_OFFLINEDATAAUTH_NG：オフラインデータ認証NG
     *         k_ICPROC_CHOLDER_VERIFY_NG ：本人確認NG
     *         k_ICPROC_CANCEL ：キャンセル
     *         k_ICPROC_TIMEOUT：タイムアウト
     */
    public int emvIcProc() {
        int ret = 0;
        int cardHolderVerifyResult = 0;

        // オフラインデータ認証
        ret = emvOfflineDataAuthentication();
        if (0 != ret) {
            return k_ICPROC_OFFLINEDATAAUTH_NG;
        }

        // 処理制限
        mEmvHandler.processRestriction();

        byte[] val = getTagValue(new byte[]{(byte) 0x95});
        if (null != val) { Timber.tag(LOGTAG).d("After processRestriction TVR(TAG_95):%02x %02x %02x %02x %02x", val[0], val[1], val[2], val[3], val[4]); }

        // 本人確認
        _pinResult = k_PIN_RESULT_NONE;
        while(true) {
            try {
                cardHolderVerifyResult = mEmvHandler.cardholderVerify();

                // Terminal Verification Results（ターミナル検証結果）取得
                byte[] terminalVerificationResults = getTagValue(new byte[]{(byte) 0x95});
                if (null == terminalVerificationResults) {
                    Timber.tag(LOGTAG).e("Fail get TVR");
                    return k_ICPROC_CHOLDER_VERIFY_NG;
                }

                // 本人確認結果のチェック
                cardHolderVerifyResult = emvCheckCardholderVerifyResult(cardHolderVerifyResult, terminalVerificationResults, _pinResult, _pinRetCode, _statusWord);
                if (k_ICPROC_PINRETRY == cardHolderVerifyResult) {
                    // PIN再入力
                    continue;
                }
                if (k_ICPROC_CANCEL == cardHolderVerifyResult || k_ICPROC_TIMEOUT == cardHolderVerifyResult || k_ICPROC_REMOVE_CARD == cardHolderVerifyResult) {
                    // キャンセル、タイムアウト
                    return cardHolderVerifyResult;
                }
                if (k_ICPROC_CHOLDER_VERIFY_NG == cardHolderVerifyResult || k_ICPROC_PINBYPASS_DISABLE == cardHolderVerifyResult) {
                    return cardHolderVerifyResult;
                }
                break;
            } catch (SDKException e) {
                Timber.tag(LOGTAG).e("Exception cardholderVerify");
                e.printStackTrace();
                cardHolderVerifyResult = k_ICPROC_CHOLDER_VERIFY_NG;
                return cardHolderVerifyResult;
            }
        }

        val = getTagValue(new byte[]{(byte) 0x95});
        if (null != val) { Timber.tag(LOGTAG).d("After cardholderVerify TVR(TAG_95):%02x %02x %02x %02x %02x", val[0], val[1], val[2], val[3], val[4]); }

        // PIN認証結果の設定
        ret = setPinAuthResult();

        return ret;
    }

    /**
     * 本人確認結果のチェック
     * @return PIN認証結果
     *         k_ICPROC_PIN_OK   ：PIN OK
     *         k_ICPROC_PINBYPASS：PINバイパス
     *         k_ICPROC_CANCEL   ：PIN入力キャンセル
     *         k_ICPROC_TIMEOUT  ：PIN入力待ちタイムアウト
     *         k_ICPROC_CHOLDER_VERIFY_NG：本人確認NG
     */
    private int emvCheckCardholderVerifyResult(int cardHolderVerifyResult, byte[] terminalVerificationResults, int pinResult, int pinRetCode, byte[] statusWord) throws SDKException {
        int ret = 0;

        switch(pinResult) {
            // PIN OK、PIN NG、PINバイパス
            case k_PIN_RESULT_OK:
                switch(cardHolderVerifyResult) {
                    case 0:
                        if ((0 == pinRetCode) && (null != statusWord) && ((byte) 0x90 == statusWord[0]) && (0x00 == statusWord[1])) {
                            // PIN OK
                            Timber.tag(LOGTAG).i("PIN認証成功");
                            ret = k_ICPROC_PIN_OK;
                        } else if (PedRetCode.NO_PIN == pinRetCode) {
                            // PINバイパス
                            IEMVHandler iemvHandler = EMVHandler.getInstance();
                            if(iemvHandler.getMckConfigure().getSupportByPassPinEntry()) {
                                Timber.tag(LOGTAG).i("PINバイパス");
                                ret = k_ICPROC_PINBYPASS;
                            } else {
                                Timber.tag(LOGTAG).e("PINバイパス不可");
                                ret = k_ICPROC_PINBYPASS_DISABLE;
                            }
                        } else {
                            if (0 != (terminalVerificationResults[2] & 0x20)) {
                                // PIN試行回数を超過
                                Timber.tag(LOGTAG).e("PIN認証失敗：入力回数超過");
                                ret = k_ICPROC_CHOLDER_VERIFY_NG;
                            } else {
                                // PIN再入力
                                Timber.tag(LOGTAG).e("PIN認証失敗：再入力案内");
                                ret = k_ICPROC_PINRETRY;
                            }
                        }
                        break;
                    default:
                        Timber.tag(LOGTAG).e("Fail cardholderVerify %d", ret);
                        ret = k_ICPROC_CHOLDER_VERIFY_NG;
                        break;
                }
                break;
            // PIN入力自体がなかった場合
            case k_PIN_RESULT_NONE:
                if (0 != (terminalVerificationResults[2] & 0x20)) {
                    // PIN試行回数を超過
                    Timber.tag(LOGTAG).i("PIN入力案内（無）：入力回数超過");
                    ret = k_ICPROC_CHOLDER_VERIFY_NG;
                } else {
                    // PINバイパスの扱い
                    Timber.tag(LOGTAG).i("PIN入力案内（無）：PINバイパス扱い");
                    ret = k_ICPROC_PINBYPASS;
                }
                break;
            // PIN入力キャンセル
            case k_PIN_RESULT_CANCEL:
                Timber.tag(LOGTAG).i("PIN入力キャンセル");
                ret = k_ICPROC_CANCEL;
                break;
            // PIN入力待ちタイムアウト
            case k_PIN_RESULT_TIMEOUT:
                Timber.tag(LOGTAG).i("PIN入力待ちタイムアウト");
                ret = k_ICPROC_TIMEOUT;
                break;
            // PIN入力中にカードが抜かれた場合
            case k_PIN_RESULT_REMOVE_CARD:
                Timber.tag(LOGTAG).e("PIN入力中に接触ICカード抜かれ");
                ret = k_ICPROC_REMOVE_CARD;
                break;
            default:
                Timber.tag(LOGTAG).e("Unknown pinResult");
                ret = k_ICPROC_CHOLDER_VERIFY_NG;
                break;
        }

        return ret;
    }

    /**
     * PIN認証結果の設定
     */
    private int setPinAuthResult() {
        int ret = 0;
        byte[] val;

        // POSエントリモード：PINなし
        _creditSettlement.setPosEntryMode(null, CreditSettlement.k_POSEMODE_PIN_NOTEXISTS);
        // サイン必要
        _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_NECESSARY);
        ret = k_ICPROC_CHOLDER_VERIFY_NG;

        // CVM結果を判定
        // EMV 4.3 Book 3 「C3 Cardholder Verification Rule Format」「Table 39: CVM Codes」参照

        // CVM Results（TAG 9F34）を確認
        val = getTagValue(new byte[]{(byte) 0x9F, 0x34});
        if (null != val) {
            Timber.tag(LOGTAG).d("CVM Results(TAG_9F34) %02x", val[0]);
            int cvmCode = val[0] & k_CVMCODE_MASK;
            // PIN認証結果をPOSエントリモードに反映
            switch (cvmCode) {
                case k_CVMCODE_PLAINPIN:            // 平文PIN
                case k_CVMCODE_ENCPIN:              // 暗号PIN
                    if (cvmCode == k_CVMCODE_PLAINPIN) {
                        Timber.tag(LOGTAG).i("PIN認証結果：平文PINあり、サイン不要");
                    } else {
                        Timber.tag(LOGTAG).i("PIN認証結果：暗号PINあり、サイン不要");
                    }

                    // POSエントリモード：PINあり
                    _creditSettlement.setPosEntryMode(null, CreditSettlement.k_POSEMODE_PIN_EXISTS);
                    // サイン不要
                    _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_UNNECESSARY);
                    ret = 0;
                    break;
                case k_CVMCODE_PLAINPIN_SIGNATURE:  // 平文PINとサイン
                case k_CVMCODE_ENCPIN_SIGNATURE:    // 暗号PINとサイン
                    if (cvmCode == k_CVMCODE_PLAINPIN_SIGNATURE) {
                        Timber.tag(LOGTAG).i("PIN認証結果：平文PINあり、サイン必要");
                    } else {
                        Timber.tag(LOGTAG).i("PIN認証結果：暗号PINあり、サイン必要");
                    }

                    // POSエントリモード：PINあり
                    _creditSettlement.setPosEntryMode(null, CreditSettlement.k_POSEMODE_PIN_EXISTS);
                    // サイン必要
                    _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_NECESSARY);
                    ret = 0;
                    break;
                case k_CVMCODE_SIGNATURE:           // サイン
                    Timber.tag(LOGTAG).i("PIN認証結果：PINなし、サイン必要");
                    // POSエントリモード：PINなし
                    _creditSettlement.setPosEntryMode(null, CreditSettlement.k_POSEMODE_PIN_NOTEXISTS);
                    // サイン必要
                    _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_NECESSARY);
                    ret = 0;
                    break;
                case k_CVMCODE_NO_CVM_PERFORMED:
                    // EMV 4.3 Book 4「A4 CVM Results」
                    // CVM が実行されていない場合、Byte 1 は 3F になる
                    // QPS の場合、このパターンになる
                    Timber.tag(LOGTAG).i("PIN認証結果：PINなし、サイン不要");
                    // POSエントリモード：PINなし
                    _creditSettlement.setPosEntryMode(null, CreditSettlement.k_POSEMODE_PIN_NOTEXISTS);
                    // 署名欄なし
                    _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_SPACE_NONE);
                    ret = 0;
                    break;
                default:
                    break;
            }
        }

        return ret;
    }

    /**
     * オフラインデータ認証
     * @return 0：正常終了
     *         0以外：エラーコード
     */
    private int emvOfflineDataAuthentication() {
        int ret = 0;

        Timber.tag(LOGTAG).i("emvOfflineDataAuthentication");

        // オフラインデータ認証
        try {
            ret = mEmvHandler.offlineDataAuthentication();
        } catch (SDKException e) {
            Timber.tag(LOGTAG).e("SDKException offlineDataAuthentication");
            Timber.e(e);
            e.printStackTrace();
        }
        // オフラインデータ認証後のAIP(Application Interchange Profile)／TerminalCapabilities／TVR(Terminal Verification Results)を取得
        Timber.tag(LOGTAG).d("After offlineDataAuthentication");
        byte[] val = getTagValue(new byte[]{(byte) 0x95});
        if (null != val) { Timber.tag(LOGTAG).d("TVR(TAG_95):%02x %02x %02x %02x %02x", val[0], val[1], val[2], val[3], val[4]); }
        val = getTagValue(new byte[]{(byte) 0x82});
        if (null != val) { Timber.tag(LOGTAG).d("AIP(TAG_82):%02x %02x", val[0], val[1]); }
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail offlineDataAuthentication %d", ret);
            return -1;
        }

        return 0;
    }

    /**
     * PIN入力後のICカード処理
     * @return 0：オンライン判定
     *         k_ICPROC_TERM_RISK_MANAGE_NG：端末リスク管理NG
     *         k_ICPROC_OFFLINE：オフライン判定
     */
    public int emvAfterInputPin() {
        int ret;

        Timber.tag(LOGTAG).d("emvAfterInputPin");

        // 端末リスク管理
        ret = 0;
        try {
            ret = mEmvHandler.terminalRiskManage();
        } catch (SDKException e) {
            Timber.tag(LOGTAG).e("SDKException terminalRiskManage");
            Timber.e(e);
            e.printStackTrace();
        }
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail terminalRiskManage %d", ret);
            return k_ICPROC_TERM_RISK_MANAGE_NG;
        }

        // 端末アクション分析
        boolean isNeedOnline = false;
        try {
            isNeedOnline = mEmvHandler.terminalActionAnalysis();
        } catch (SDKException e) {
            Timber.tag(LOGTAG).e("SDKException terminalActionAnalysis");
            Timber.e(e);
            e.printStackTrace();
        }

        Timber.tag(LOGTAG).d("After terminalActionAnalysis");
        byte[] val = getTagValue(new byte[]{(byte) 0x95});
        if (null != val) { Timber.tag(LOGTAG).d("TVR(TAG_95):%02x %02x %02x %02x %02x", val[0], val[1], val[2], val[3], val[4]); }
        val = getTagValue(new byte[]{(byte) 0x8A});
        if (null != val) { Timber.tag(LOGTAG).d("ARC(TAG_8A):%02x %02x", val[0], val[1]); }

        // 端末アクション分析後のCID(Cryptogram Information Data)を取得
        ret = k_ICPROC_OFFLINE;
        val = getTagValue(new byte[]{(byte) 0x9f,0x27});
        if (null != val) {
            Timber.tag(LOGTAG).d("CID(TAG_9F27):%02x", val[0]);
            if ((val[0]&0x80)==0x80) {
                // オンライン処理
                Timber.tag(LOGTAG).d("Card request to online");
                if (isNeedOnline) {
                    ret = 0;
                }
            } else if ((val[0]&0x40)==0x40) {
                // オフライン承認
                Timber.tag(LOGTAG).d("Offline OK");
                ret = k_ICPROC_OFFLINE;
            } else {
                // オフライン拒否
                Timber.tag(LOGTAG).d("Offline NG");
                ret = k_ICPROC_OFFLINE;
            }
        } else {
            // オフライン拒否
            Timber.tag(LOGTAG).d("Offline NG (TAG_9F27 is NULL)");
            ret = k_ICPROC_OFFLINE;
        }

        return ret;
    }

    /**
     * オンラインオーソリ応答検証
     * @return 0：正常終了
     *         0以外：エラーコード
     */
    public int emvOnlineAuthVerification()
    {
        int ret = -1;

        Timber.tag(LOGTAG).i("2nd Generate AC start");

        // 2nd Generate AC
        try {
            mEmvHandler.onlineTransaction();
            // 2nd Generate AC後のCID(Cryptogram Information Data)を取得
            Timber.tag(LOGTAG).d("After onlineTransaction");
            byte[] val = getTagValue(new byte[]{(byte) 0x95});
            if (null != val) { Timber.tag(LOGTAG).d("TVR(TAG_95):%02x %02x %02x %02x %02x", val[0], val[1], val[2], val[3], val[4]); }
            val = getTagValue(new byte[]{(byte) 0x8A});
            if (null != val) { Timber.tag(LOGTAG).d("ARC(TAG_8A):%02x %02x", val[0], val[1]); }
            val = getTagValue(new byte[]{(byte) 0x9B});
            if (null != val) { Timber.tag(LOGTAG).d("TSI(TAG_9B):%02x %02x", val[0], val[1]); }

            val = getTagValue(new byte[]{(byte) 0x9f,0x27});
            if (null != val) {
                Timber.tag(LOGTAG).d("CID(TAG_9F27):%02x", val[0]);
                if ((val[0]&0x40)==0x40) {
                    // オンライン承認
                    Timber.tag(LOGTAG).i("EMVオンラインオーソリ応答検証：オンライン承認");
                    ret = 0;
                } else {
                    // オンライン拒否
                    Timber.tag(LOGTAG).e("EMVオンラインオーソリ応答検証：オンライン拒否");
                    ret = -1;
                }
            } else {
                // オンライン拒否
                Timber.tag(LOGTAG).e("EMVオンラインオーソリ応答検証：オンライン拒否 (TAG_9F27 is NULL)");
                ret = -1;
            }
        } catch (SDKException e) {
            Timber.tag(LOGTAG).e("SDKException onlineTransaction");
            Timber.e(e);
            e.printStackTrace();
            ret = -1;
        }

        return ret;
    }

    /**
     * オンラインオーソリ応答のレスポンスコード設定
     * @param resCode :response code
     * @param authCode:authentication code(TAG8A)
     * @param authData:authentication data(TAG91)
     * @param scriptData:script data(TAG71or72)
     *        ※存在しないパラメータはnullを設定
     */
    public void emvSetAuthResCode(byte[] resCode, byte[] authCode, byte[] authData, byte[] scriptData, String cafisErrorCd) {
        // response code
        if (null != resCode) {
            _resCode = resCode;
        } else {
            _resCode = null;
        }

        // authentication code
        if (null != authCode) {
            _authCode = authCode;
        } else {
            _authCode = null;
        }

        // authentication data
        if (null != authData) {
            _authData = authData;
        } else {
            _authData = null;
        }

        // script data
        if (null != scriptData) {
            _scriptData = scriptData;
        } else {
            _scriptData = null;
        }

        // オーソリエラーコード
        _cafisErrorCd = cafisErrorCd;
    }

    private IEMVCallback.EMVInitListener emvInitListener = new IEMVCallback.EMVInitListener() {
        @Override
        public int candidateAppsSelection() {
            int ret;
            //LogUtil.d("--------------------candidateAppsSelection------------------");
            //return ret;//return a application's number

            // アプリケーション選択
            ret = selectApplication();

            if ( 0 < ret ) {
                // アプリケーション選択待ち
                startWaitInput();

                // アプリ選択後の setApplication() で endWaitInput() によって入力待ち解除

                // 選択されたアプリケーションの番号を返す
                return _appNumber;
            } else {
                // アプリケーションが1つ（ret:0）、またはエラー（ret:-1）
                return ret;
            }
        }

        @Override
        public void multiLanguageSelection() {
            //LogUtil.d("---------------multiLanguageSelection----------------" );
            // table of terminal supported: en EN zh ZH
            //byte[] gl_TmSupLang = {0x65, 0x6E, 0x45, 0x4E, 0x7A, 0x68, 0x5A, 0x48};
            byte[][] lg={{0x65,0x6E},{0x45,0x4E},{0x7A, 0x68},{0x5A, 0x48}};
            //727565736465656E
            byte[] tag = new byte[]{0x5F, 0x2D};
            byte[] lang = new byte[0];
            try {
                 lang=mEmvHandler.getDataElement(tag);
            } catch (SDKException e) {
                Timber.tag(LOGTAG).e("SDKException getDataElement");
                Timber.e(e);
                e.printStackTrace();
            }
            //LogUtil.d("read support language in card: "+ ISOUtil.byte2hex(lang));
            if (lang==lg[2]||lang==lg[3]){//chinese
                currentLanguageType = 1;
                ///mListener.debugInfo("card uses Chinese");
            }else {//default is english (situation of lang==lg[0]||lang==lg[1] is also english)
                currentLanguageType =0;
            }
        }

        @Override
        public int getAmount(int[] transAmount, int[] cashBackAmount) {
            //LogUtil.d("---------------getAmount-------------");
            transAmount[0] = Amount.getTotalAmount();
            cashBackAmount[0] = 0;
            //LogUtil.d("getAmount: transAmount("+transAmount[0]+") cashBackAmount("+cashBackAmount[0]+")");

            return 0;
        }

        /**
         * @deprecated
         * */
        @Override
        public int getPin(int[] pinLen, byte[] cardPin) {
            //LogUtil.d("-------------getOfflinePin-------------");
            return 0;
        }

        //offline pin
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public int getOfflinePin(int i, RsaPinKey rsaPinKey, byte[] recvLen, byte[] recvData) {
            //LogUtil.d("--------------getOfflinePin----------------");
            int ret = 0;

            KeySystem ks;
            Ped ped = Ped.getInstance();
            int fd = ped.getIccSlot(SlotType.USER_CARD);
            IccOfflinePinApdu apdu = new IccOfflinePinApdu();
            String pinLenLimmit = "0, 4, 5, 6, 7, 8, 9, 10, 11, 12";

            apdu.setCla(0x00);
            apdu.setIns(0x20);
            apdu.setLe(0x00);
            apdu.setLeflg(0x00);
            apdu.setP1(0x00);
            if (1 == i) {
                ks = KeySystem.ICC_CIPHER;
                apdu.setRsakey(rsaPinKey);
                apdu.setP2(0x88);
            } else {
                ks = KeySystem.ICC_PLAIN;
                apdu.setP2(0x80);
            }

            PadView padView = new PadView();

            // PIN入力
            if (k_PIN_RESULT_NONE == _pinResult) {
                // PIN入力初回
                Timber.tag(LOGTAG).d("PIN start");
                padView.setPinTips("暗証番号を入力してください");
                // PIN入力の音声案内
                _creditSettlement.startInputPin();
                // PIN入力開始時間記録
                _creditSettlement.setPinStartTime();
            } else {
                // PIN再入力
                if ((0x63 == _statusWord[0]) && ((byte)0xC1 == _statusWord[1])) {
                    // PIN試行回数：残り1回
                    padView.setPinTips("【残り１回】再入力してください");
                    // 暗証番号入力間違いなのでエラー履歴に残す
                    CommonErrorDialog commonErrorDialog = new CommonErrorDialog();
                    commonErrorDialog.ShowErrorMessage(_creditSettlement._fragmentActivity, "3026");
                } else {
                    // PIN試行回数：残り2回以上
                    padView.setPinTips("【エラー】再入力してください");
                    // 暗証番号入力間違いなのでエラー履歴に残す
                    CommonErrorDialog commonErrorDialog = new CommonErrorDialog();
                    commonErrorDialog.ShowErrorMessage(_creditSettlement._fragmentActivity, "3025");
                }
            }

            ped.setPinPadView(padView);

            ped.getOfflinePin(ks, fd, pinLenLimmit, apdu, new PinBlockCallback() {
                @Override
                public void onPinBlock(int i, byte[] aByte) {
                    _pinRetCode = i;
                    _statusWord = aByte;
                    // PIN入力待ち解除
                    endWaitInput();
                }
            });

            // PIN入力待ち
            startWaitInput();

            // PIN入力のコールバック getOfflinePin() で endWaitInput() によって入力待ち解除

            ret = 0;
            recvLen[0] = 0;
            switch(_pinRetCode) {
                case PedRetCode.NO_PIN:
                    // PINバイパス
                    Timber.tag(LOGTAG).d("PIN Bypass");
                    _pinResult = k_PIN_RESULT_OK;
                    ret = PedRetCode.NO_PIN;
                    break;
                case PedRetCode.ENTER_CANCEL:
                    // キャンセル
                    Timber.tag(LOGTAG).d("PIN Cancel");
                    ScreenData.getInstance().setScreenName("クレジットPIN入力"); //PIN入力画面での定義ができないためここでセット ホームメニューに戻るだけなので再セットは必要なし
                    CommonClickEvent.RecordClickOperation("CANCEL", null, true);
                    _pinResult = k_PIN_RESULT_CANCEL;
                    ret = PedRetCode.ENTER_CANCEL;
                    break;
                case PedRetCode.TIMEOUT:
                    // タイムアウト
                    Timber.tag(LOGTAG).d("PIN Timeout");
                    _pinResult = k_PIN_RESULT_TIMEOUT;
                    break;
                default:
                    // PIN OK または PIN NG
                    // PIN OKの場合 _statusWord が 0x90,0x00 になる
                    if (null == _statusWord) {
                        Timber.tag(LOGTAG).d("REMOVE CARD");
                        _pinResult = k_PIN_RESULT_REMOVE_CARD;
                    } else if (((byte) 0x90 == _statusWord[0]) && (0x00 == _statusWord[1])) {
                        // PIN OK
                        Timber.tag(LOGTAG).d("PIN OK");
                        _pinResult = k_PIN_RESULT_OK;
                    } else {
                        // PIN NG
                        Timber.tag(LOGTAG).d("PIN NG");
                        _pinResult = k_PIN_RESULT_NG;
                    }
                    if (null != _statusWord) {
                        int len = _statusWord.length;
                        System.arraycopy(_statusWord, 0, recvData, 0, len);
                        recvLen[0] = (byte) len;
                    }
                    break;
            }

            // PIN入力終了時間記録
            _creditSettlement.setPinEndTime();

            return ret;
        }

        //pin verify result
        @Override
        public int pinVerifyResult(int tryCount) {
            //LogUtil.d("--------------pinVerifyResult------------" + tryCount);
            boolean isTryAgain = true;

            if (isTryAgain){
                return 0;
            }else {
                return -1;
            }
        }

        //online PIN
        @Override
        public int checkOnlinePIN() {
            return 0;
        }

        /** 証明書の確認 **/
        @Override
        public int checkCertificate() {
            //LogUtil.d("=====checkCertificate====");
            int ret = -1;
            byte[] ucCertType = new byte[0];
            byte[] szCertNo = new byte[0];
            try {
                ucCertType = mEmvHandler.getDataElement(new byte[]{(byte) 0x9f,0x62});
                szCertNo = mEmvHandler.getDataElement(new byte[]{(byte) 0x9f,0x61});
            } catch (SDKException e) {
                Timber.tag(LOGTAG).e("SDKException getDataElement");
                Timber.e(e);
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public int onlineTransactionProcess(byte[] brspCode, byte[] bauthCode,
                                            int[] authCodeLen, byte[] bauthData, int[] authDataLen,
                                            byte[] script, int[] scriptLen, byte[] bonlineResult) {
            //LogUtil.d("==onlineTransactionProcess========");
            //here, you should handle script sent from issue's server.pass these parameters to kernel
            //and if it's required,the result data of handling script data in kernel should be send to
            //issue's server .
            //there are another task in this process ,which is you should send and receive data from
            //issue's server.But you can handle online data transform before this function, too.
            //LogUtil.d("onlineTransactionProcess return_exit 0.");

            // response code
            if (null != _resCode) {
                Timber.tag(LOGTAG).d("ResponseCode:%s", McUtils.bytesToHexString(_resCode));
                System.arraycopy(_resCode, 0, brspCode, 0, _resCode.length);
            } else {
                Timber.tag(LOGTAG).d("ResponseCode None");
            }

            if (0 < _cafisErrorCd.length()) {
                Timber.tag(LOGTAG).d("CafisErrorCd:%s", _cafisErrorCd);
            } else {
                Timber.tag(LOGTAG).d("CafisErrorCd None");
            }

            // EMV 4.3 Book 4「A6 Authorisation Response Code」に従い
            // オンライン不可でオフライン拒否の場合は "Z3" を設定する
            // JCB でオンライン不可の場合、_resCode も _cafisErrorCd も空なので (null == _resCode) で判定
            // AMEX でオンライン不可の場合、_cafisErrorCd が "G30" なので (_cafisErrorCd.equals("G30")) で判定
            if (null == _resCode || _cafisErrorCd.equals("G30")){
                Timber.tag(LOGTAG).i("Update ResponseCode To \"Z3\"");
                brspCode[0] = 'Z';
                brspCode[1] = '3';
            }

            // authentication code（TAG8A）
            if (null != _authCode) {
                Timber.tag(LOGTAG).d("AuthenticationCode(TAG8A)::%s", McUtils.bytesToHexString(_authCode));
                System.arraycopy(_authCode, 0, bauthCode, 0, _authCode.length);
                authCodeLen[0] = _authCode.length;
            } else {
                Timber.tag(LOGTAG).d("AuthenticationCode(TAG8A) None");
            }

            // authentication data（TAG91）
            if (null != _authData) {
                Timber.tag(LOGTAG).d("AuthenticationData(TAG91) Len:%d %s", _authData.length, McUtils.bytesToHexString(_authData));
                System.arraycopy(_authData, 0, bauthData, 0, _authData.length);
                authDataLen[0] = _authData.length;
            } else {
                Timber.tag(LOGTAG).d("AuthenticationData(TAG91) None");
            }

            // script data（TAG71 or TAG72）
            if (null != _scriptData) {
                Timber.tag(LOGTAG).d("ScriptData Len:%d", _scriptData.length);
                System.arraycopy(_scriptData, 0, script, 0, _scriptData.length);
                scriptLen[0] = _scriptData.length;
            } else {
                Timber.tag(LOGTAG).d("ScriptData None");
            }

            // online transaction process result
            if (null != _resCode && 0x30 == _resCode[0] && 0x30 == _resCode[1]) {
                Timber.tag(LOGTAG).d("OnlineResult OK");
                bonlineResult[0] = 0;
            } else {
                Timber.tag(LOGTAG).d("OnlineResult NG");
                bonlineResult[0] = 1;
            }

            return 0;
        }

        /**
         * exception card
         */
        @Override
        public int checkExceptionFile(int panLen, byte[] pan, int panSN) {
            return -1;
        }
        @Override
        public int issuerReferralProcess() {
            //LogUtil.d("----------------issuerReferralProcess---------------");
            return 0;
        }
        @Override
        public int adviceProcess(int firstFlg) {
            //LogUtil.d("--------------adviceProcess------------");
            try {
                TerminalMckConfigure configure = mEmvHandler.getMckConfigure();
                if(configure.getSupportAdvices()){
                    return 0;
                }
            } catch (SDKException e) {
                Timber.tag(LOGTAG).e("SDKException getMckConfigure");
                Timber.e(e);
                e.printStackTrace();
            }
            return 0;
        }
        @Override
        public int checkRevocationCertificate(int caPublicKeyID, byte[] RID,
                                              byte[] destBuf) {
            //LogUtil.d("---------checkRevocationCertificate----------------");
            return -1;
        }
        @Override
        public int getTransactionLogAmount(int panLen, byte[] pan, int panSN) {
            //LogUtil.d("--------------getTransactionLogAmount-------------");
            return 0;
        }
    };

    private IEMVCallback.ApduExchangeListener apduExchangeListener = new IEMVCallback.ApduExchangeListener() {
        @Override
        public int apduExchange(byte[] sendData, int[] recvLen, byte[] recvData) {
            //LogUtil.d("==apduExchangeListener===");
                 int len = 0;
                try {
                    if (mContactCard == null || mIccReader == null) {
                        return -1;
                    } else {
                        byte[] rawData = mIccReader.transmit(mContactCard, sendData);
                        if (rawData != null) {
                            len = rawData.length;
                        }
                        if (len <= 0) {
                            return -1;
                        }
                        System.arraycopy(rawData, 0, recvData, 0, rawData.length);
                    }
                } catch (SDKException e) {
                    Timber.tag(LOGTAG).e("SDKException transmit");
                    Timber.e(e);
                    e.printStackTrace();
                }
                if (len >= 0) {
                    recvLen[0] = len;
                    return 0;
                }
                return -1;
        }
    };

    private class InputStat {
        private boolean _input;
        public void setStat(boolean stat) {
            _input = stat;
        }
        public boolean getStat() {
            return _input;
        }
    }

    private InputStat getInputStatInstance() {
        return _inputStat;
    }

    public void startWaitInput() {
        getInputStatInstance().setStat(false);
        try {
            synchronized (getInputStatInstance()) {
                while (false == getInputStatInstance().getStat()) {
                    getInputStatInstance().wait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endWaitInput() {
        try {
            synchronized (getInputStatInstance()) {
                getInputStatInstance().setStat(true);
                getInputStatInstance().notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * initial Kernel
     **/
    private boolean initEmvKernel() {
        mEmvHandler.setKernelType(EMVHandler.KERNEL_TYPE_EMV);
        mEmvHandler.initDataElement();
        mEmvHandler.setEMVInitCallback(emvInitListener);
        mEmvHandler.setApduExchangeCallback(apduExchangeListener);
        mEmvHandler.setDataElement(new byte[]{(byte) 0x9c}, new byte[]{0x00});

        TerminalMckConfigure configure = new TerminalMckConfigure();
        //terminal type: Attended – Online only
        configure.setTerminalType(0x21);
        //terminal capability: input type of card(manual/mag/ic)、CVM capability(plaintext pin/signature/Enciphered PIN)、security capability(SDA/DDA/CDA)
        configure.setTerminalCapabilities(new byte[]{(byte) 0xE0, (byte) 0xB0, (byte) 0xC8});
        /**terminal additional capability:
         * Transaction Type Capability: 0x60, 0x00 Goods、Services
         * Terminal Data Input Capability: 0xF0 Numeric、Alphabetic and special characters、Command、Function keys
         * Term. Data Output Capability: 0xA0 0x01 Print attendant、Display attendant、Code table 1
         * */
        configure.setAdditionalTerminalCapabilities(new byte[]{0x60, 0x00, (byte) 0xF0, (byte) 0xA0, 0x01});
        //Set whether supported card initiated voice referrals or not
        configure.setSupportCardInitiatedVoiceReferrals(false);
        //Set whether supported forced acceptance capability or not
        configure.setSupportForcedAcceptanceCapability(false);
        //Set whether supported force online capabilities or not
        configure.setSupportForcedOnlineCapability(false);
        //POS entry mode: IC card(contact card)
        configure.setPosEntryMode(0x05);
        //Set whether supported exception file or not
        configure.setSupportExceptionFile(false);
        //Set whether supported get PIN try counter
        configure.setSupportGetPinTryCounter(true);
        //Set whether supported PSE selection method or not
        configure.setSupportPSESelectionMethod(true);
        //Set whether supported cardholder confirmation or not
        configure.setSupportCardholderConfirmation(true);
        //Set whether supported floor limit checking or not
        configure.setSupportFloorLimitChecking(true);
        //Set whether supported advices or not
        configure.setSupportAdvices(false);
        //Set whether supported batch data capture or not
        configure.setSupportBatchDataCapture(false);
        //Set whether supported PIN entry or not
        configure.setSupportByPassPinEntry(true);
        //Set whether supported default DDOL(Dynamic Data Authentication Data Object List) or not
        configure.setSupportDefaultDDOL(true);
        //Set whether supported multi-language or not
        configure.setSupportMultiLanguage(false);
        //Set whether supported terminal action code or not
        configure.setSupportTerminalActionCodes(true);

        int ret = mEmvHandler.setMckConfigure(configure);
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail setMckConfigure %d", ret);
            return false;
        }

        CoreParam coreParam = new CoreParam();
        coreParam.setMerchantCateCode(new byte[]{0x00, 0x01});// type code of merchant
        // カントリーコード／通貨コードは0392：日本（円）
        coreParam.setTerminalCountryCode(new byte[]{0x03,(byte)0x92});
        coreParam.setTransactionCurrencyCode(new byte[]{0x03,(byte)0x92});
        coreParam.setReferCurrencyCode(new byte[]{0x03,(byte)0x92});
        // 日本円のexponentは0
        coreParam.setTransactionCurrencyExponent(0x00); // currency exponent
        coreParam.setReferCurrencyExponent(0x00);// refer currency exponent
        coreParam.setReferCurrencyCoefficient(0); //
        coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_SERVICE); //transaction type

        ret = mEmvHandler.setCoreInitParameter(coreParam);
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail setCoreInitParameter %d", ret);
            return false;
        }

        return true;
    }

    private byte[] getTagValue(byte[] tag){
        byte[] val = null;
        try {
            val =  mEmvHandler.getDataElement(tag);
        } catch (SDKException e) {
            Timber.tag(LOGTAG).d("SDKException getDataElement");
            //e.printStackTrace();
        }
        return val;
    }

    /**
     * 複数タグの値取得
     * @param tags:TAGテーブル（終端は0である事）
     * @return String:データ長＋データ
     *                データはTAGテーブルで指定されたTAGとTAG値
     */
    public String emvGetSomeTagValues(int[] tags) {
        String tag = "";
        String tagLen = "";
        String tagValues = "";
        String totalData = "";
        int nTag;
        byte[] bTag = null;
        byte[] bTagVal = null;

        for (int i = 0; ; i++) {
            nTag = tags[i];
            if (0 == nTag) {
                break;
            }

            // TAGの値を取得
            if (nTag <= 0xFF) {
                // TAGが1バイト
                bTag = new byte[1];
                bTag[0] = (byte) nTag;
                bTagVal = getTagValue(bTag);
            } else if (nTag > 0xFF && nTag <= 0xFFFF) {
                // TAGが2バイト
                bTag = new byte[2];
                bTag[0] = (byte) (nTag >> 8);
                bTag[1] = (byte) (nTag & 0x00FF);
                bTagVal = getTagValue(bTag);
            } else {
                // TAGが3バイト以上は未対応
                bTagVal = null;
            }

            if (null != bTagVal) {
                tag = ISOUtil.hexString(bTag);
                if (bTagVal.length <= 0xFF) {
                    // TAGの値サイズが1バイト
                    tagLen = String.format("%02X", bTagVal.length & 0xFF);
                } else if (bTagVal.length > 0xFF && bTagVal.length <= 0xFFFF) {
                    // TAGの値サイズが2バイト
                    tagLen = String.format("%04X", bTagVal.length & 0xFFFF);
                } else if (bTagVal.length > 0xFFFF && bTagVal.length <= 0xFFFFFF) {
                    // TAGの値サイズが3バイト
                    tagLen = String.format("%06X", bTagVal.length & 0xFFFFFF);
                } else if (bTagVal.length > 0xFFFFFF && bTagVal.length <= 0xFFFFFFFF) {
                    // TAGの値サイズが4バイト
                    tagLen = String.format("%08X", bTagVal.length & 0xFFFFFFFF);
                } else {
                    tagLen = "";
                }

                if (tagLen.length() > 0) {
                    // 「TAG＋TAGの値サイズ＋TAGの値」のStringを生成
                    tagValues += (tag + tagLen);
                    tagValues += ISOUtil.hexString(bTagVal);
                }
            }
        }

        totalData = String.format("%06X", (tagValues.length() / 2) & 0xFFFFFF);
        totalData += tagValues;
        return totalData;
    }

    /**
     * カード情報取得
     */
    public byte[] emvGetTrack1Info() {
        byte[] val = getTagValue(new byte[]{(byte) 0x56});
        if (null == val) {
            val = new byte[1];
            val[0] = 0;
        }
        return val;
    }
    public byte[] emvGetTrack2Info() {
        byte[] val = getTagValue(new byte[]{(byte) 0x57});
        if (null == val) {
            val = new byte[1];
            val[0] = 0;
        }
        return val;
    }

    /**
     * アプリケーションラベル取得
     */
    public byte[] emvGetApplicationLabel() {
        byte[] val = getTagValue(new byte[]{(byte) 0x50});
        if (null == val) {
            val = new byte[1];
            val[0] = 0;
        }
        return val;
    }

    /**
     * AID取得
     */
    public byte[] emvGetAID() {
        byte[] val = getTagValue(new byte[]{(byte) 0x9F, 0x06});
        if (null == val) {
            val = new byte[1];
            val[0] = 0;
        }
        return val;
    }

    /**
     * PANシーケンスナンバー取得
     */
    public byte[] emvGetPanSeqNo() {
        byte[] val = getTagValue(new byte[]{(byte) 0x5F, 0x34});
        if (null == val) {
            val = new byte[1];
            val[0] = -1;
        }
        return val;
    }

    //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  ATC取得
     * @note   ATCを取得する(TAG:9F36)
     * @param [in] なし
     * @retval なし
     * @return String ATC情報 0~65535までの数値を5桁詰めで返す
     * @private
     */
    /******************************************************************************/
    public String emvGetATC() {
        String result ;
        byte[] val = getTagValue(new byte[]{(byte) 0x9F, 0x36});
        if (null == val) {
            result = "     ";           //無効時は５個のスペース
        }
        else {
            if (val.length == 2) {
                int v = (val[0] & 0xFF) << 8 | (val[1] & 0xFF);
                if (v >= 65536)             // ushort型の最大値を超えた場合(あり得ないが念の為)
                {
                    result = "     ";           //無効時は５個のスペース
                }
                else
                {
                    result = ISOUtil.padleft(String.valueOf(v), 5, '0');
                }
            }
            else
            {
                result = "     ";           //無効時は５個のスペース
            }
        }
        return result;
    }
    //ADD-E BMT S.Oyama 2024/10/03 フタバ双方向向け改修


    /**
     * POSエントリモード設定・取得
     */
    public void emvSetPosEntryMode(String posEntryMode) {
        TerminalMckConfigure configure = new TerminalMckConfigure();
        int val = Integer.parseInt(posEntryMode, 16);
        configure.setPosEntryMode(val);
    }

    /**
     * get card number :emv tag 5A
     * @return card number string
     */
    /*
    public String getCardNo() {
        byte[] temp = new byte[256];
        int len = TLVUtil.getTlvDataKernel(0x5A, temp);
        return ISOUtil.trimf(ISOUtil.byte2hex(temp, 0, len));
    }
    */

    public int inputPinForDemo() {
        int ret = 0;
        KeySystem ks;
        Ped ped = Ped.getInstance();
        int fd = ped.getIccSlot(SlotType.USER_CARD);
        IccOfflinePinApdu apdu = new IccOfflinePinApdu();
        String pinLenLimmit = "0, 4, 5, 6, 7, 8, 9, 10, 11, 12";

        apdu.setCla(0x00);
        apdu.setIns(0x20);
        apdu.setLe(0x00);
        apdu.setLeflg(0x00);
        apdu.setP1(0x00);
        ks = KeySystem.ICC_PLAIN;
        apdu.setP2(0x80);

        PadView padView = new PadView();

        Timber.tag(LOGTAG).d("PIN start");
        padView.setPinTips("暗証番号を入力してください");
        // PIN入力の音声案内
        _creditSettlement.startInputPin();

        ped.setPinPadView(padView);

        ped.getOfflinePin(ks, fd, pinLenLimmit, apdu, new PinBlockCallback() {
            @Override
            public void onPinBlock(int i, byte[] aByte) {
                _pinRetCode = i;
                _statusWord = aByte;
                // PIN入力待ち解除
                endWaitInput();
            }
        });

        // PIN入力待ち
        startWaitInput();

        // PIN入力のコールバック getOfflinePin() で endWaitInput() によって入力待ち解除

        switch(_pinRetCode) {
            case PedRetCode.NO_PIN:
            case PedRetCode.ENTER_CANCEL:
            case PedRetCode.TIMEOUT:
                // PINバイパスの扱い
                Timber.tag(LOGTAG).d("PIN BYPASS");
                _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_NECESSARY);
                ret = k_PIN_RESULT_NONE;
                break;
            default:
                // PIN OK の扱い
                Timber.tag(LOGTAG).d("PIN OK");
                _creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_UNNECESSARY);
                ret = k_PIN_RESULT_OK;
                break;
        }

        return(ret);
    }
}
