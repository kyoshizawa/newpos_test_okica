package jp.mcapps.android.multi_payment_terminal.thread.emv;


import androidx.annotation.IntDef;

import com.newpos.emvl2.EMV_ENUM;
import com.newpos.emvl2.EMV_ERROR_VALUE;
import com.newpos.emvl2.EMV_L2_FUNC;
import com.pos.device.SDKException;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CardManager;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.util.DebugLog;
import timber.log.Timber;

/**
 * 非接触カード処理クラス
 */
public class EmvCLProcess {
    private static final String LOGTAG = "EmvCLProcess";
    @IntDef({EMV_ENUM.EMV_TRANS_PURCHASE, EMV_ENUM.EMV_TRANS_REFUND})
    public @interface TransType {}

    public EmvCLProcess(CreditSettlement creditSettlement){
        _creditSettlement = creditSettlement;
    }
    private static final MainApplication _app = MainApplication.getInstance();
    private static boolean isInit = false;
    private static final EMV_L2_FUNC _emvL2Func = new EMV_L2_FUNC();
    private static final EmvCLCallBackHandle CallBack = new EmvCLCallBackHandle();
    private static final DebugLog _debugLog = new DebugLog(EmvCLProcess.class.getSimpleName());
    private final CreditSettlement _creditSettlement;
    public byte getCVM() {
        return _emvL2Func.EmvGetCVM();
    }
    public int getKernelId() {
        byte[] kernelId = new byte[2];
        _emvL2Func.EmvEpCurrentKernelIdGet(kernelId);

        return kernelId[0];
    }

    /**
     * カーネルデータベースからAIDを取得します
     * 値は次の事前処理が行われるまで保持されます
     *
     * @return AID
     */
    public byte[] getAID() {
        byte[] aid = _emvL2Func.EmvDatabaseValueGetEx(0x4F);

        if (aid != null) {
            Timber.tag(LOGTAG).d("AID from Tag'0x4F'");
            return aid;
        }

        aid = _emvL2Func.EmvDatabaseValueGetEx(0x84);

        if (aid != null) {
            Timber.tag(LOGTAG).d("AID from Tag'0x84'");
            return aid;
        }

        Timber.tag(LOGTAG).d("AID does not exist in either Tag'0x4F' or 'Tag'0x84'");

        return null;
    }

    /**
     * カーネルデータベースからトラック2相当のデータを取得します
     * 値は次の事前処理が行われるまで保持されます
     *
     * @return トラック2相当データ
     */
    public byte[] getTrack2() {
        byte[] track2 = _emvL2Func.EmvDatabaseValueGetEx(0x57);
        if(track2 == null) {
            Timber.tag(LOGTAG).d("Track2(Tag'57') is null");
            track2 = _emvL2Func.EmvDatabaseValueGetEx(0x9F6B);
            if (track2 == null) {
                Timber.tag(LOGTAG).d("Track2(Tag'9F6B') is null");
                track2 = new byte[] {0};
            }
        }

        return track2;
    }

    /**
     * カーネルデータベースからPANシーケンス番号を取得します
     * 値は次の事前処理が行われるまで保持されます
     *
     * @return 0埋め3桁のPANシーケンス番号
     */
    public String getPanSeqNo() {
        byte[] panSeqNo = _emvL2Func.EmvDatabaseValueGetEx(0x5F34);
        if(panSeqNo != null) {
            if (panSeqNo.length == 1) {
                return ISOUtil.padleft(ISOUtil.bcd2int(panSeqNo, 0, panSeqNo.length) + "", 3, '0');
            }
        }

        return null;
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
    public String getATC() {
        String result ;
        byte[] val = _emvL2Func.EmvDatabaseValueGetEx(0x9f36);
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
     * カーネルデータベースからアプリケーションラベルを取得します
     * 値は次の事前処理が行われるまで保持されます
     *
     * @return アプリケーションラベル
     */
    public byte[] getApplicationLabel() {
        byte[] val = _emvL2Func.EmvDatabaseValueGetEx(0x50);
        if (null == val) {
            val = new byte[1];
            val[0] = 0;
        }
        return val;
    }
    /**
     * カーネルデータベースからオーソリ用のICC関連データを取得します
     * 値は次の事前処理が行われるまで保持されます
     *
     * @return ICC関連データ
     */
    public byte[] getICCDataForAuth() {
        int[] tags = Constants.CL_AUTH_TAGS.get(getKernelId());

        byte[] buf = new byte[1024];
        byte[] iccData;
        int iccDataLen = 0;

        if (tags != null) {
            iccDataLen = packTags(tags, buf);
        }

        iccData = Arrays.copyOf(buf, iccDataLen);

        //_debugLog.i("オーソリICC関連データ=%s", ISOUtil.byte2hex(iccData));

        return Arrays.copyOf(iccData, iccDataLen);
    }

    /**
     * 売上用のICC関連データを取得します
     * 値は次の事前処理が行われるまで保持されます
     * 拒否売上にも使用されます
     *
     * @return ICC関連データ
     */
    public byte[] getICCDataForSales() {
        int[] tags = Constants.CL_SALES_TAGS.get(getKernelId());

        byte[] buf = new byte[1024];
        byte[] iccData;
        int iccDataLen = 0;

        if (tags != null) {
            iccDataLen = packTags(tags, buf);
        }

        iccData = Arrays.copyOf(buf, iccDataLen);

        //_debugLog.d("売上ICC関連データ=%s", ISOUtil.byte2hex(iccData));

        return iccData;
    }

    /**
     * カーネルデータベースからCAFIS電文データ部9-7-3を取得します
     * 値は次の事前処理が行われるまで保持されます
     * 拒否売上とアドバイス電文を同時に送信できないため未使用です
     *
     * @return ICC関連データ
     */
    public byte[] getICCDataForAdvice() {
        int[] tags = Constants.CL_ADVICE_TAGS.get(getKernelId());

        byte[] buf = new byte[1024];
        byte[] iccData;
        int iccDataLen = 0;

        if (tags != null) {
            iccDataLen = packTags(tags, buf);
        }

        iccData = Arrays.copyOf(buf, iccDataLen);

        //_debugLog.d("アドバイスICC関連データ=%s", ISOUtil.byte2hex(iccData));

        return iccData;
    }

    private byte[] _resCode;
    private byte[] _authCode;
    private byte[] _authData;
    private byte[] _scriptData;
    private String _cafisErrorCd;

    private CountDownLatch counter;


    /**
     * 非接触EMVの初期化を行います
     * 初期化が行われるのは1回のみであり2回目以降の呼び出しは何もせずにreturnします
     *
     * @return 初期化成功/失敗
     */
    public static boolean emvInit(){
        if (isInit) return true;

        Timber.tag(LOGTAG).i("非接触EMV初期化処理実行");

        final String path = _app.getFilesDir().getPath()+"/";

        // メーカの回答よりDiscoverのカーネルは2回呼び出しが必要とのこと
        final String[] modules = new String[] {
                path+"libPaywave.so",
                path+"libPaypass.so",
                path+"libJCB.so",
                path+"libAMEX.so",
                path+"libDiscover.so",
                path+"libDiscover.so"
        };

        final String [] libName = new String[] {
                "paywave",
                "paypass",
                "jcb",
                "amex",
                "discover",
                "discover"
        };

        if (!EMV_L2_FUNC.EmvInit()) {
            Timber.tag(LOGTAG).e("EmvInitに失敗");
            return false;
        }

        if (!_emvL2Func.EmvFwInit(CallBack, false)) {
            Timber.tag(LOGTAG).e("EmvFwInitに失敗");
            return false;
        }

        _emvL2Func.EmvLoadSysparam(1);

        if (!_emvL2Func.EmvLoadKernel(modules, libName)) {
            Timber.tag(LOGTAG).e("EmvLoadKernelに失敗");
            return false;
        }

        if (!loadKernelParams()) {
            Timber.tag(LOGTAG).e("loadKernelParamsに失敗");
            return false;
        }

        // カードタイプのセット 0: 接触 1: 非接触
        // CardTypeによってコールバックが接触/非接触で切り替わる
        // 接触ではEMV_L2_FUNCクラスを使用しないため非接触決め打ち
        if (_emvL2Func.EmvCmdCardTypeSet(1) != 0) {
            Timber.tag(LOGTAG).e("EmvCmdCardTypeSetに失敗");
            return false;
        }

        Timber.tag(LOGTAG).i("非接触EMV初期化処理成功");

        isInit = true;

        return true;
    }

    public static void emvUnInit() {
        EMV_L2_FUNC.EmvUnint();
    }

    private static boolean loadKernelParams() {
        for (KernelParam.Param p : KernelParam.getParams()) {
            final int ret = _emvL2Func.EmvEpKernelParamSet((byte) p.kernelId, p.data, p.data.length);

            if (ret != 0) {
                Timber.tag(LOGTAG).e("カーネルパラメータ不正 カーネルID = %s", p.kernelId);
                return false;
            }
        }

        return true;
    }

    /**
     * カードとの取引を行います
     * EMVの仕様では事前処理の後にポーリング処理を行いますが
     * ポーリング後に呼び出される処理であるためこのメソッドではポーリングを行いません
     *
     * @param cm CardManager
     * @param amount 取引金額
     * @param transType 取引種別(購入: 0x00, 取消: 0x20)
     *
     * @return 取引結果
     */
    public int startTransaction(CardManager cm, int amount, @TransType byte transType) {
        Timber.tag(LOGTAG).d("非接触EMVカード処理実行");

        if (transType != EMV_ENUM.EMV_TRANS_PURCHASE && transType != EMV_ENUM.EMV_TRANS_REFUND) {
            Timber.tag(LOGTAG).e("取引種別異常 :%s", transType);
            cm.stopPICC();
            return -1;
        }

        int outcome;

        CallBack.init(cm);

        _emvL2Func.EmvInitTransaction(true,true,(byte)0xFF);

        // 事前処理実行
        if ( (outcome = transactionSetData(amount, transType)) != 0) {
            Timber.tag(LOGTAG).e("事前処理失敗 outcome=%s", outcome);
            cm.stopPICC();
            return outcome;
        }

        // コンビネーション選択
        if ( ( outcome = _emvL2Func.EmvEpContactlessBuildCombination()) != 0) {
            Timber.tag(LOGTAG).d("EmvEpContactlessBuildCombinationに失敗 outcome=%s", outcome);
            cm.stopPICC();

            if (outcome == EMV_ERROR_VALUE.EMV_TRY_AGAIN
                    || outcome == EMV_ERROR_VALUE.EMV_PRESENT_CARD_AGAIN) {
                _creditSettlement.getCLState().onNext(CLState.ReadAgain);
            }
            else if (outcome == EMV_ERROR_VALUE.EMV_TRY_AGAIN_SEEPHONE) {
                _creditSettlement.playSound(R.raw.look_at_device);
                _creditSettlement.getCLState().onNext(CLState.SeePhone);
                try {
                    // VISAの仕様だと画面表示して1000ms - 2000msほど非接触のIFを停止する
                    Thread.sleep(2000);
                } catch (InterruptedException ignore) {
                }
                _creditSettlement.getCLState().onNext(CLState.ReadAgain);
            }

            return outcome;
        }

        // カーネル起動処理 ～ アプリケーションデータ読み込み処理
        // SELECT NEXTが発生した場合は他のアプリケーションでの処理を行うためもう一度実行する
        while ( (outcome = _emvL2Func.EmvEpContactlessTransaction(true)) == EMV_ERROR_VALUE.EMV_SELECT_NEXT) {
            Timber.tag(LOGTAG).i("SELECT NEXT 次のアプリケーションを選択します");
        }

        if (outcome == EMV_ERROR_VALUE.EMV_TRY_AGAIN
                || outcome == EMV_ERROR_VALUE.EMV_PRESENT_CARD_AGAIN) {
            cm.stopPICC();
            _creditSettlement.getCLState().onNext(CLState.ReadAgain);
            return outcome;
        }
        else if (outcome == EMV_ERROR_VALUE.EMV_TRY_AGAIN_SEEPHONE) {
            cm.stopPICC();
            _creditSettlement.playSound(R.raw.look_at_device);
            _creditSettlement.getCLState().onNext(CLState.SeePhone);
            try {
                // VISAの仕様だと画面表示して1000ms - 2000msほど非接触のIFを停止する
                Thread.sleep(2000);
            } catch (InterruptedException ignore) {
            }
            _creditSettlement.getCLState().onNext(CLState.ReadAgain);
            return outcome;
        }

        // カードの読取に成功した場合は音声でカード除去を案内する
        _creditSettlement.playSound(R.raw.remove_card);

        try {
            cm.mEmvContactlessCard.deactive(i -> {
                counter.countDown();
            });
        } catch (SDKException e) {
            e.printStackTrace();
        }

        counter = new CountDownLatch(1);

        // 画面にカード除去の案内を表示
        _creditSettlement.getCLState().onNext(CLState.RemoveCard);

        // カードが除去されるまで待つ
        try{
            counter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cm.stopPICC();

        Timber.tag(LOGTAG).d("EmvEpContactlessTransaction=%s", outcome);

        return outcome;
    }

    public int completecontactless(byte[]auth_resp_code,int auth_resp_code_len, byte[]issuer_auth_data,int issuer_auth_data_len,byte[]auth_code,int auth_code_len,byte[]script,int script_len)
    {
//        Tparam.tapCard(CardManager.TYPE_NFC);
//        ParamEmvL2 paramEmvL2=new ParamEmvL2();
//        paramEmvL2.Contactless=true;
//        paramEmvL2.Contact=false;
//        paramEmvL2.Magstripe=false;
//        int detectcrd=detects.DetectCards(paramEmvL2);//contactless only
//        if(detectcrd!=0)
//            return 0;
//        return emv_l2_func.EmvEpContactlessTransactionCompletion(0,auth_resp_code,auth_resp_code_len,issuer_auth_data,issuer_auth_data_len,
//                auth_code,auth_code_len,script,script_len);
        return -1; // Todo イシュアスクリプトの実装の時に考える
    }

    /**
     * 取引金額を設定して事前処理を行います
     *
     * @param amount 取引金額
     * @param transType 取引種別(購入: 0x00, 取消: 0x20)
     * @return 事前処理結果
     */
    private int transactionSetData(int amount, @TransType byte transType){
        byte[] transData = new byte[32];
        int transDataLen = 0;
        transData[transDataLen++] = (byte) 0x9C;//transaction type
        transData[transDataLen++] = 0x01;
        transData[transDataLen++] = transType;

        transData[transDataLen++] = (byte) 0x9F;//transaction amount
        transData[transDataLen++] = 0x02;
        transData[transDataLen++] = 0x06;

        byte[] amountBCD = ISOUtil.str2bcd(String.valueOf(amount),true);
        System.arraycopy(amountBCD, 0, transData, transDataLen+6-amountBCD.length, amountBCD.length);
        transDataLen += 6;

        transData[transDataLen++] = (byte) 0x9F;//transaction other amount
        transData[transDataLen++] = 0x03;
        transData[transDataLen++] = 0x06;
        byte[] otherAmount = ISOUtil.str2bcd(String.valueOf(0),false);
        System.arraycopy(otherAmount, 0, transData, transDataLen, otherAmount.length);
        transDataLen += 6;

        return _emvL2Func.EmvEpPreTransaction(transData, transDataLen, false);
    }

    /**
     * カーネルデータベースからオーソリゼーション報告コードを取得します
     *
     * @return オーソリゼーション報告コード
     */
    public byte[] getAuthCode() {
        return _emvL2Func.EmvDatabaseValueGetEx(0x8A);
    }

    /**
     * カーネルデータベースにオーソリゼーション報告コード(ARC)をセットします
     *
     * @param authCode オーソリゼーション報告コード
     */
    public void setAuthCode(byte[] authCode) {
        /*
         * カーネルSDKマニュアルよりおそらく使い方はあっているはず
         * arg1: データソース 0x01: ICC, 0x02: Terminal, 0x04: Issuer
         * arg2: テンプレート ARCにテンプレートはないため0で問題ない？
         * arg3: タグ
         * arg4: 値
         * arg5: レングス
         * arg6: 上書きフラグ
         */
        if (authCode != null) {
            _emvL2Func.EmvDatabaseValueSet((byte) 0x04, 0, 0x8A, authCode, authCode.length, false);
        }
    }

    /**
     * オーソリゼーション報告コードから承認されなかった取引の結果を取得します
     *
     * @return 取引結果
     */
    public int getNotApprovalOutcomeFromARC() {
        if (getAuthCode() == null) return -1;

        final String code = new String(getAuthCode());

        Timber.tag(LOGTAG).e("Auth Code: %s", code);

        int kernelId = getKernelId();

        if (kernelId == KernelID.Mastercard) {
            if (code.equals("65")) {
                return EMV_ERROR_VALUE.EMV_OTHER_INTERFACE;
            }
            return EMV_ERROR_VALUE.EMV_DECLIEND;
        }
        else if (kernelId == KernelID.Amex) {
            switch (code) {
                case "12":
                case "13":  // Book C-4よりOnline PINをサポートしていない場合はTry Another Interfaceとする
                    return EMV_ERROR_VALUE.EMV_OTHER_INTERFACE;
                default:
                    return EMV_ERROR_VALUE.EMV_DECLIEND;
            }
        }

        return EMV_ERROR_VALUE.EMV_DECLIEND;
    }

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

    /**
     * カーネルデータベースから指定したタグリストのデータを取得します
     *
     * @param tags タグリスト
     * @param dest データ格納用オブジェクト
     *
     * @return レングス
     */
    private int packTags(int[] tags, byte[] dest) {
        int i, tagLen, len;
        byte[] Tag = new byte[2];
        int offset = 0;
        byte[] ptr = new byte[256];

        i = 0;
        while (tags[i] != 0) {

            if (tags[i] < 0x100) {
                tagLen = 1;
                Tag[0] = (byte) tags[i];
            } else {
                tagLen = 2;
                Tag[0] = (byte) (tags[i] >> 8);
                Tag[1] = (byte) tags[i];
            }

            len = getTLVDataFromKernel(tags[i], ptr);

            if (len > 0) {
                System.arraycopy(Tag, 0, dest, offset, tagLen);// 拷标签
                offset += tagLen;

                if (len < 128) {
                    dest[offset++] = (byte) len;
                } else if (len < 256) {
                    dest[offset++] = (byte) 0x81;
                    dest[offset++] = (byte) len;
                }
                System.arraycopy(ptr, 0, dest, offset, len);
                offset += len;
            }

            i++;
        }

        return offset;
    }

    /**
     * カーネルデータベースからデータを取得します
     *
     * @param tag TLVのタグ
     * @param data データ格納用オブジェクト
     *
     * @return データレングス
     */
    private  int getTLVDataFromKernel(int tag, byte[] data) {
        byte[] result = _emvL2Func.EmvDatabaseValueGetEx(tag);

        if(result!=null) {
            System.arraycopy(result, 0, data, 0, result.length);
        }

        return result == null ? 0 : result.length;
    }
}