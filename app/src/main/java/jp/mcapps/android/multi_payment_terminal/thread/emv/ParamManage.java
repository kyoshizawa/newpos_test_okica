package jp.mcapps.android.multi_payment_terminal.thread.emv;

import androidx.fragment.app.FragmentActivity;
import android.content.res.AssetManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.pos.device.SDKException;
import com.pos.device.emv.CAPublicKey;
import com.pos.device.emv.CoreParam;
import com.pos.device.emv.EMVHandler;
import com.pos.device.emv.IEMVHandler;
import com.pos.device.emv.TerminalAidInfo;
import com.pos.device.emv.TerminalMckConfigure;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.thread.emv.ParamConfig;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CAKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CardAnalyze;
import timber.log.Timber;


public class ParamManage {
    private IEMVHandler emvHandler ;
    public ParamManage(){
        emvHandler =EMVHandler.getInstance();
    }

    private final String LOGTAG = "ParamManage";

    /**
     * CAPK（Certification Authority Public Key）および
     * EMV設定情報の登録
     * @param  creActivity：Activityインスタンス
     * @return 0：正常終了
     *         0以外：エラーコード
     */
    public int loadParam(FragmentActivity creActivity) throws SDKException {
        int ret =0;

        ret = terminalLoadEmvConf(creActivity);
        if (ret != ParamConfig.status.SUCCESS)
            return ret;
        return ParamConfig.status.SUCCESS;
    }

    /**
     * EMV設定情報の登録
     * @param  creActivity：Activityインスタンス
     * @return 0：正常終了
     *         0以外：エラーコード
     */
    private int terminalLoadEmvConf(FragmentActivity creActivity) throws SDKException {
        Timber.tag(LOGTAG).d("terminalLoadEmvConf");
        int ret = 0;

        // EMV設定情報ファイルオープン
        InputStream inputStream = openAssetFile("EmvConfig.json", creActivity);
        if (null == inputStream) {
            Timber.tag(LOGTAG).d("Fail config file open");
            return ParamConfig.status.LOAD_AID_FAIL;
        }

        // JSON形式のEMV設定情報を読み込む
        Gson gson = new Gson();
        JsonReader jsonReader = null;
        JsonObject jsonObj = null;
        try {
            jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            jsonObj = gson.fromJson(jsonReader, JsonObject.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (null == jsonObj) {
            Timber.tag(LOGTAG).d("Fail fromJson in terminalLoadEmvConf");
            return ParamConfig.status.LOAD_AID_FAIL;
        }

        // EMV設定情報の"CoreParam"を登録
        for (int i=0; i<ParamConfig.emvConfigCore.length; i++) {
            String parentKeyName = ParamConfig.emvConfigCore[i];  // 親のキー名
            JsonElement jsonElement = jsonObj.get(parentKeyName);
            if (null != jsonElement) {
                JsonObject jsonObjEmvConfig = jsonElement.getAsJsonObject();
                // jsonObjEmvConfigで親キー以下の値を取り出し、EMV設定情報として登録する
                extractEmvConfCore(parentKeyName, jsonObjEmvConfig);
            }
        }
        // ブランド別のEMV設定情報を登録
        for (int i=0; i<ParamConfig.emvConfigBrand.length; i++) {
            String parentKeyName = ParamConfig.emvConfigBrand[i];  // 親のキー名
            JsonElement jsonElement = jsonObj.get(parentKeyName);
            if (null != jsonElement) {
                JsonObject jsonObjEmvConfig = jsonElement.getAsJsonObject();
                // jsonObjEmvConfigで親キー以下の値を取り出し、EMV設定情報として登録する
                ret = extractEmvConfBrand(parentKeyName, jsonObjEmvConfig);
            }
        }
        return ret;
    }

    /**
     * EMV設定情報の"CoreParam"をファイルから取り出す
     * @param  parentKeyName：EMV設定情報の親キー
     *         jsonObj：EMV設定情報のJsonObjectインスタンス
     */
    private void extractEmvConfCore(String parentKeyName, JsonObject jsonObj) throws SDKException {
        CoreParam coreParam = new CoreParam();
        byte[] bTemp;
        String sTemp;

        Set<Map.Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        for(Map.Entry<String, JsonElement> entry : entrySet) {
            String keyName = entry.getKey();  // キー名
            switch(keyName) {
                // 端末カントリーコード
                case ParamConfig.k_TERMINAL_COUNTRY_CODE:
                    sTemp = jsonObj.get(ParamConfig.k_TERMINAL_COUNTRY_CODE).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    coreParam.setTerminalCountryCode(bTemp);
                    break;
                // 取引通貨コード
                case ParamConfig.k_TRANSACTION_CURRENCY_CODE:
                    sTemp = jsonObj.get(ParamConfig.k_TERMINAL_COUNTRY_CODE).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    coreParam.setTransactionCurrencyCode(bTemp);
                    break;
                // 基準通貨コード
                case ParamConfig.k_REFER_CURRENCY_CODE:
                    sTemp = jsonObj.get(ParamConfig.k_REFER_CURRENCY_CODE).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    coreParam.setReferCurrencyCode(bTemp);
                    break;
                // 取引通貨指数
                case ParamConfig.k_TRANSACTION_CURRENCY_EXPONENT:
                    sTemp = jsonObj.get(ParamConfig.k_TRANSACTION_CURRENCY_EXPONENT).getAsString();
                    coreParam.setTransactionCurrencyExponent(Integer.valueOf(sTemp));
                    break;
                // 基準通貨指数
                case ParamConfig.k_REFER_CURRENCY_EXPONENT:
                    sTemp = jsonObj.get(ParamConfig.k_REFER_CURRENCY_EXPONENT).getAsString();
                    coreParam.setReferCurrencyExponent(Integer.valueOf(sTemp));
                    break;
                // 基準通貨係数
                case ParamConfig.k_REFER_CURRENCY_COEFFICIENT:
                    sTemp = jsonObj.get(ParamConfig.k_REFER_CURRENCY_COEFFICIENT).getAsString();
                    coreParam.setReferCurrencyCoefficient(Integer.valueOf(sTemp));
                    break;
                // 取引タイプ
                case ParamConfig.k_TRANSACTION_TYPE:
                    sTemp = jsonObj.get(ParamConfig.k_TRANSACTION_TYPE).getAsString();
                    SetTransactionType(sTemp, coreParam);
                    break;
                // TerminalMckConfigure
                case ParamConfig.k_TERMINAL_MCK_CONFIGURE:
                    JsonElement jsonElement = jsonObj.get(ParamConfig.k_TERMINAL_MCK_CONFIGURE);
                    JsonObject jsonObjectChild = jsonElement.getAsJsonObject();
                    SetTerminalMckConfigure(jsonObjectChild);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * ブランド別のEMV設定情報をファイルから取り出す
     * @param  parentKeyName：EMV設定情報の親キー
     *         jsonObj：EMV設定情報のJsonObjectインスタンス
     */
    private int extractEmvConfBrand(String parentKeyName, JsonObject jsonObj){
        int ret = 0;

        Set<Map.Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        for(Map.Entry<String, JsonElement> entry : entrySet) {
            String keyName = entry.getKey();  // キー名
            switch(keyName) {
                // TerminalAidInfo
                case ParamConfig.k_TERMINAL_AID_INFO:
                    JsonElement jsonElement = jsonObj.get(ParamConfig.k_TERMINAL_AID_INFO);
                    JsonObject jsonObjectChild = jsonElement.getAsJsonObject();
                    ret = SetTerminalAidInfo(jsonObjectChild);
                    break;
                default:
                    break;
            }
        }
        return ret;
    }

    private void SetTransactionType(String str, CoreParam coreParam) {
        switch (str) {
            case "ADMIN":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_ADMIN);
                break;
            case "CASH":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_CASH);
                break;
            case "CASHBACK":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_CASHBACK);
                break;
            case "GOODS":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_GOODS);
                break;
            case "INQUIRY":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_INQUIRY);
                break;
            case "PAYMENT":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_PAYMENT);
                break;
            case "SERVICE":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_SERVICE);
                break;
            case "TRANSFER":
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_TRANSFER);
                break;
            default:
                coreParam.setTransactionType(EMVHandler.EMVTransType.EMV_SERVICE);
                break;
        }
    }

    private int SetTerminalAidInfo(JsonObject jsonObj) {
        int ret = 0;
        byte[] bTemp;
        String sTemp;
        TerminalAidInfo aidInfo = CreditSettlement.getInstance()._aidInfo;
        Set<Map.Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        for(Map.Entry<String, JsonElement> entry : entrySet) {
            String keyName = entry.getKey();  // キー名
            switch(keyName) {
                // AID
                case ParamConfig.k_AID:
                    sTemp = jsonObj.get(ParamConfig.k_AID).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    aidInfo.setAId(bTemp);
                    aidInfo.setAIDdLength(bTemp.length);
                    break;
                // 端末フロアリミット
                case ParamConfig.k_TERMINAL_FLOOR_LIMIT:
                    sTemp = jsonObj.get(ParamConfig.k_TERMINAL_FLOOR_LIMIT).getAsString();
                    aidInfo.setTerminalFloorLimit(Integer.valueOf(sTemp));
                    break;
                // アプリケーションバージョン
                case ParamConfig.k_APPLICATION_VERSION:
                    sTemp = jsonObj.get(ParamConfig.k_APPLICATION_VERSION).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    aidInfo.setApplicationVersion(bTemp);
                    break;
                // PartialAIDSelectサポート
                case ParamConfig.k_SUPPORT_PARTIAL_AID_SELECT:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_PARTIAL_AID_SELECT).getAsString();
                    aidInfo.setSupportPartialAIDSelect(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // TAC Default
                case ParamConfig.k_TAC_DEFAULT:
                    sTemp = jsonObj.get(ParamConfig.k_TAC_DEFAULT).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    aidInfo.setTerminalActionCodeDefault(bTemp);
                    break;
                // TAC Online
                case ParamConfig.k_TAC_ONLINE:
                    sTemp = jsonObj.get(ParamConfig.k_TAC_ONLINE).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    aidInfo.setTerminalActionCodeOnline(bTemp);
                    break;
                // TAC Denial
                case ParamConfig.k_TAC_DENIAL:
                    sTemp = jsonObj.get(ParamConfig.k_TAC_DENIAL).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    aidInfo.setTerminalActionCodeDenial(bTemp);
                    break;
                // しきい値
                case ParamConfig.k_THRESHOLD_VALUE:
                    sTemp = jsonObj.get(ParamConfig.k_THRESHOLD_VALUE).getAsString();
                    aidInfo.setThresholdValue(Integer.valueOf(sTemp));
                    break;
                // 最大目標パーセンテージ
                case ParamConfig.k_MAXIMUM_TARGET_PERCENTAGE:
                    sTemp = jsonObj.get(ParamConfig.k_MAXIMUM_TARGET_PERCENTAGE).getAsString();
                    aidInfo.setMaximumTargetPercentage(Integer.valueOf(sTemp));
                    break;
                // 目標パーセンテージ
                case ParamConfig.k_TARGET_PERCENTAGE:
                    sTemp = jsonObj.get(ParamConfig.k_TARGET_PERCENTAGE).getAsString();
                    aidInfo.setTargetPercentage(Integer.valueOf(sTemp));
                    break;
                // デフォルトDDOL
                case ParamConfig.k_DEFAULT_DDOL:
                    sTemp = jsonObj.get(ParamConfig.k_DEFAULT_DDOL).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    aidInfo.setDefaultDDOL(bTemp);
                    aidInfo.setLenOfDefaultDDOL(bTemp.length);
                    break;
                // デフォルトTDOL
                case ParamConfig.k_DEFAULT_TDOL:
                    sTemp = jsonObj.get(ParamConfig.k_DEFAULT_TDOL).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    aidInfo.setDefaultTDOL(bTemp);
                    aidInfo.setLenOfDefaultTDOL(bTemp.length);
                    break;
                default:
                    break;
            }
        }

        if (aidInfo != null) {
            ret = emvHandler.addAidInfo(aidInfo);
            if (ret != 0) {
                Timber.tag(LOGTAG).d("Fail addAidInfo-1 %d", ret);
                return ParamConfig.status.LOAD_AID_FAIL;
            }
        }

        return ret;
    }

    private void SetTerminalMckConfigure(JsonObject jsonObj) throws SDKException {
        byte[] bTemp;
        String sTemp;
        IEMVHandler mEmvHandler = EMVHandler.getInstance();
        TerminalMckConfigure configure = mEmvHandler.getMckConfigure();
        Set<Map.Entry<String, JsonElement>> entrySet = jsonObj.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String keyName = entry.getKey();  // キー名
            switch (keyName) {
                // 端末タイプ
                case ParamConfig.k_TERMINAL_TYPE:
                    sTemp = jsonObj.get(ParamConfig.k_TERMINAL_TYPE).getAsString();
                    configure.setTerminalType(Integer.valueOf(sTemp));
                    break;
                // 端末能力
                case ParamConfig.k_TERMINAL_CAPABILITIES:
                    if (AppPreference.isPinLessEnabled()) {
                        // PINレス機能有効
                        if (Amount.getFixedAmount() > AppPreference.getPinLessLimitFare()) {
                            // PINレス金額を超える場合
                            sTemp = jsonObj.get(ParamConfig.k_TERMINAL_CAPABILITIES).getAsString();
                            bTemp = getBcdFromStr(sTemp);
                            Timber.tag(LOGTAG).i("PINレス機能が有効かつ閾値を超えるため、接触ICで暗証番号入力が必要");
                        } else {
                            // PINレス金額以下の場合
                            sTemp = jsonObj.get(ParamConfig.k_TERMINAL_CAPABILITIES_PINLESS).getAsString();
                            bTemp = getBcdFromStr(sTemp);
                            Timber.tag(LOGTAG).i("PINレス機能が有効かつ閾値以下のため、接触ICで暗証番号入力不要");
                        }
                    } else {
                        // PINレス機能無効
                        sTemp = jsonObj.get(ParamConfig.k_TERMINAL_CAPABILITIES).getAsString();
                        bTemp = getBcdFromStr(sTemp);
                        Timber.tag(LOGTAG).i("PINレス機能が無効のため、接触ICで暗証番号入力が必要");
                    }

                    configure.setTerminalCapabilities(bTemp);
                    break;
                // 追加端末能力
                case ParamConfig.k_ADDITIONAL_TERMINAL_CAPABILITIES:
                    sTemp = jsonObj.get(ParamConfig.k_ADDITIONAL_TERMINAL_CAPABILITIES).getAsString();
                    bTemp = getBcdFromStr(sTemp);
                    configure.setAdditionalTerminalCapabilities(bTemp);
                    break;
                // 音声照会処理サポート
                case ParamConfig.k_SUPPORT_CARDINITIATED_VOICEREFERRALS:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_CARDINITIATED_VOICEREFERRALS).getAsString();
                    configure.setSupportCardInitiatedVoiceReferrals(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // 強制承認機能サポート
                case ParamConfig.k_SUPPORT_FORCED_ACCEPTANCE_CAPABILITY:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_FORCED_ACCEPTANCE_CAPABILITY).getAsString();
                    configure.setSupportForcedAcceptanceCapability(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // 強制オンライン指示機能サポート
                case ParamConfig.k_SUPPORT_FORCED_ONLINE_CAPABILITY:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_FORCED_ONLINE_CAPABILITY).getAsString();
                    configure.setSupportForcedOnlineCapability(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // POSエントリモード
                case ParamConfig.k_POS_ENTRY_MODE:
                    sTemp = jsonObj.get(ParamConfig.k_POS_ENTRY_MODE).getAsString();
                    configure.setPosEntryMode(Integer.valueOf(sTemp));
                    break;
                // ExceptionFileサポート
                case ParamConfig.k_SUPPORT_EXCEPTION_FILE:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_EXCEPTION_FILE).getAsString();
                    configure.setSupportExceptionFile(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // PIN試行カウンタ取得サポート
                case ParamConfig.k_SUPPORT_GET_PIN_TRY_COUNTER:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_GET_PIN_TRY_COUNTER).getAsString();
                    configure.setSupportGetPinTryCounter(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // PSE（PaymentSystemEnvironment）選択
                case ParamConfig.k_SUPPORT_PSE_SELECTION_METHOD:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_PSE_SELECTION_METHOD).getAsString();
                    configure.setSupportPSESelectionMethod(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // カード所有者確認サポート
                case ParamConfig.k_SUPPORT_CARDHOLDER_CONFIRMATION:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_CARDHOLDER_CONFIRMATION).getAsString();
                    configure.setSupportCardholderConfirmation(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // フロアリミットチェックサポート
                case ParamConfig.k_SUPPORT_FLOOR_LIMIT_CHECKING:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_FLOOR_LIMIT_CHECKING).getAsString();
                    configure.setSupportFloorLimitChecking(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // アドバイス機能サポート
                case ParamConfig.k_SUPPORT_ADVICES:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_ADVICES).getAsString();
                    configure.setSupportAdvices(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // BatchDataCapture
                case ParamConfig.k_SUPPORT_BATCH_DATA_CAPTURE:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_BATCH_DATA_CAPTURE).getAsString();
                    configure.setSupportBatchDataCapture(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // PINバイパスサポート
                case ParamConfig.k_SUPPORT_BYPASS_PIN_ENTRY:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_BYPASS_PIN_ENTRY).getAsString();
                    configure.setSupportByPassPinEntry(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // デフォルトDDOLサポート
                case ParamConfig.k_SUPPORT_DEFAULT_DDOL:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_DEFAULT_DDOL).getAsString();
                    configure.setSupportDefaultDDOL(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // デフォルトTDOLサポート
                case ParamConfig.k_SUPPORT_DEFAULT_TDOL:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_DEFAULT_TDOL).getAsString();
                    configure.setSupportDefaultTDOL(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                // 多言語サポート
                case ParamConfig.k_SUPPORT_MULTI_LANGUAGE:
                    sTemp = jsonObj.get(ParamConfig.k_SUPPORT_MULTI_LANGUAGE).getAsString();
                    configure.setSupportMultiLanguage(Integer.valueOf(sTemp) == 1 ? true : false);
                    break;
                default:
                    break;
            }
        }
        int ret = mEmvHandler.setMckConfigure(configure);
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail setMckConfigure %d", ret);
        }
    }

    /**
     * 端末情報取得で受信した端末番号を設定
     */
    public void saveTerminalNo(String terminalNo) {
        CoreParam coreParam = new CoreParam();
        coreParam.setTerminalId(terminalNo.getBytes());
    }

    /**
     * クレジットCA公開鍵DLで受信したCA公開鍵を設定
     */
    public void saveCAPublicKey(CAKey.Response res) {
        CAPublicKey capk = new CAPublicKey();
        byte[] bTemp;
        String sTemp;
        String blandSign;
        int capkDatLen = res.caKeys.length;

        for (int i=0; i<capkDatLen; i++) {
            // RID
            sTemp = "";
            blandSign = res.caKeys[i].blandSign;
            switch(blandSign) {
                case "35":  // JCB
                    sTemp = "A000000065";
                    break;
                case "36":  // DINERS
                    sTemp = "A000000152";
                    break;
                case "37":  // AMEX
                    sTemp = "A000000025";
                    break;
                case "40":  // VISA
                    sTemp = "A000000003";
                    break;
                case "50":  // MASTER
                    sTemp = "A000000004";
                    break;
                default:
                    break;
            }

            if (0 < sTemp.length()) {
                bTemp = getBcdFromStr(sTemp);
                capk.setRID(bTemp);

                // IDX
                sTemp = res.caKeys[i].caPublicKeyIndex;
                bTemp = getBcdFromStr(sTemp);
                capk.setIndex(ISOUtil.byte2int(bTemp));

                // EXP DATE
                sTemp = "991231";
                bTemp = getBcdFromStr(sTemp);
                capk.setExpirationDate(bTemp);

                // Exponent
                sTemp = res.caKeys[i].caPublicKeyExponent;
                bTemp = getBcdFromStr(sTemp);
                capk.setLenOfExponent(bTemp.length);
                capk.setExponent(bTemp);

                // Modulus
                sTemp = res.caKeys[i].caPublicKeyModulus;
                bTemp = getBcdFromStr(sTemp);
                capk.setLenOfModulus(bTemp.length);
                capk.setModulus(bTemp);

                // SHA
                bTemp = new byte[0];
                try {
                    byte[] rid = capk.getRID();
                    byte[] index = ISOUtil.int2byte(capk.getIndex());
                    byte[] mod = capk.getModulus();
                    byte[] exponent = capk.getExponent();
                    byte[] gm_in = new byte[rid.length + index.length + mod.length + exponent.length];
                    int gmoffset = 0;
                    System.arraycopy(rid, 0, gm_in, gmoffset, rid.length);
                    gmoffset += rid.length;
                    System.arraycopy(index, 0, gm_in, gmoffset, index.length);
                    gmoffset += index.length;
                    System.arraycopy(mod, 0, gm_in, gmoffset, mod.length);
                    gmoffset += mod.length;
                    System.arraycopy(exponent, 0, gm_in, gmoffset, exponent.length);
                    bTemp = MessageDigest.getInstance("SHA-1").digest(gm_in);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                capk.setChecksum(bTemp);
                capk.setLenOfExponent(capk.getExponent().length);

                int ret = emvHandler.addCAPublicKey(capk);
                if (ret != 0) {
                    Timber.tag(LOGTAG).d("Fail addCAPublicKey %d", ret);
                }
            }
        }
    }

    /**
     * カード判定で受信したパラメータの設定
     */
    public void saveEmvConfig(CardAnalyze.Response res) throws SDKException {
        byte[] bTemp;
        CoreParam coreParam = new CoreParam();
        TerminalMckConfigure configure = new TerminalMckConfigure();
        TerminalAidInfo aidInfo = CreditSettlement.getInstance()._aidInfo;

        // AID
        bTemp = getBcdFromStr(res.aid);
        aidInfo.setAId(bTemp);
        aidInfo.setAIDdLength(bTemp.length);

        IEMVHandler mEmvHandler = EMVHandler.getInstance();
        configure = mEmvHandler.getMckConfigure();

        // PINバイパスサポート
        if (0 == res.pinBypassInstruction) {
            configure.setSupportByPassPinEntry(false);
        } else {
            configure.setSupportByPassPinEntry(true);
        }

        int ret = mEmvHandler.setMckConfigure(configure);
        if (ret != 0) {
            Timber.tag(LOGTAG).e("Fail setMckConfigure %d", ret);
        }

        // フォールバック有効フラグ
        if (0 == res.pinSkipFBFlg) {
            CreditSettlement.setPinSkipFBFlg(false);
        } else {
            CreditSettlement.setPinSkipFBFlg(true);
        }

        // PINレスはカード読み取り前にセレクタブルカーネルで対応する必要があるため、この設定は使えない
        // PINレス取扱い可否フラグ
        if (0 == res.pinLessFlg) {
            CreditSettlement.setPinLessFlg(false);
        } else {
            CreditSettlement.setPinLessFlg(true);
        }

        // PINレス限度額
        CreditSettlement.setPinLessLimit(res.pinLessLimit);

        // 端末フロアリミット
        aidInfo.setTerminalFloorLimit(res.floorLimit);

        // 最大目標パーセンテージ
        aidInfo.setMaximumTargetPercentage(res.maxTargetPercentage);

        // 目標パーセンテージ
        aidInfo.setTargetPercentage(res.targetPercentage);

        // しきい値
        aidInfo.setThresholdValue(res.threshold);

        // MS移行フラグ
        CreditSettlement.setMsAvailableFlg(res.msAvailableFlg);

        // ブランド有効/無効フラグ
        CreditSettlement.setBrandEnableFlg(res.brandEnableFlg);

        // デフォルトDDOL
        bTemp = getBcdFromStr(res.defaultDDOL);
        aidInfo.setDefaultDDOL(bTemp);
        aidInfo.setLenOfDefaultDDOL(bTemp.length);

        // 実行優先順位
        aidInfo.setApplicationPriority(res.execPriority);

        // TAC Denial
        bTemp = getBcdFromStr(res.terminalActionCdDenial);
        aidInfo.setTerminalActionCodeDenial(bTemp);

        // TAC Default
        bTemp = getBcdFromStr(res.terminalActionCdDefault);
        aidInfo.setTerminalActionCodeDefault(bTemp);

        // TAC Online
        bTemp = getBcdFromStr(res.terminalActionCdOnline);
        aidInfo.setTerminalActionCodeOnline(bTemp);

        // アクワイアラ指定オンライン要求支払種別
        CreditSettlement.setAcquirerOnlinePayType(res.acquirerOnlinePayType);

        // 加盟店分類コード
        coreParam.setMerchantCateCode(res.kameitenClassCd.getBytes());

        // 取引分類コード
        CreditSettlement.setTxnTypeCd(res.txnTypeCd);

        // チャージタイプコード
        CreditSettlement.setChargeTypeCd(res.chargeTypeCd);

        // AP個別情報
        CreditSettlement.setAppPersonalInfo(res.appPersonalInfo);

        ret = emvHandler.addAidInfo(aidInfo);
        if (ret != 0) {
            Timber.tag(LOGTAG).d("Fail addAidInfo in saveEmvConfig %d", ret);
        }
    }

    /**
     * assetsフォルダのファイルオープン
     * @return InputStreamインスタンス
     *         null：ファイルオープン失敗
     */
    public InputStream openAssetFile(String fname, FragmentActivity creActivity){
        AssetManager asset = creActivity.getResources().getAssets();
        InputStream inputStream = null;
        try {
            inputStream = asset.open(fname);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    private byte[] getBcdFromStr(String str) {
        byte[] bcd;
        if (0 == (str.length() % 2)) {
            bcd = new byte[str.length() / 2];
            ISOUtil.str2bcd(str, false, bcd, 0);
        } else {
            bcd = new byte[(str.length() / 2) + 1];
            ISOUtil.str2bcd(str, false, bcd, 0);
        }
        return bcd;
    }
}
