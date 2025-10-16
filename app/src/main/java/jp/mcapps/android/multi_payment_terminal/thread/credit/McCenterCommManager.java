package jp.mcapps.android.multi_payment_terminal.thread.credit;

import com.pos.device.SDKException;

import java.io.IOException;
import java.math.BigInteger;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.thread.credit.data.CreditResult;
import jp.mcapps.android.multi_payment_terminal.thread.emv.ParamManage;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CardAnalyze;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CreditGetKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Echo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuth;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuthCancel;
import timber.log.Timber;

public class McCenterCommManager {
    private final String LOGTAG = "McCenterCommManager";
    private McPosCenterApi _mcPosCenterApi = new McPosCenterApiImpl();
    private CreditSettlement _creditSettlement;

    public McCenterCommManager(CreditSettlement creditSettlement) {
        _creditSettlement = creditSettlement;
    }

    /* 認証用公開鍵情報 */
    private BigInteger _modulus;
    private BigInteger _exponent;
    private String _plainSessionKey;
    // 鍵種別
    private int _keyType;
    // 鍵バージョン
    private int _keyVer;

    /* カードデータ保護用公開鍵情報 */
    private BigInteger _modulusCredit;
    private BigInteger _exponentCredit;
    // 鍵種別
    private int _keyTypeCredit;
    // 鍵バージョン
    private int _keyVerCredit;

    // 端末操作処理ポート
    private int _terOpePort;

    public void setModulus(BigInteger modulus) {
        _modulus = modulus;
    }

    public void setExponent(BigInteger exponent) {
        _exponent = exponent;
    }

    public void setKeyType(int keyType) {
        _keyType = keyType;
    }

    public void setKeyVer(int keyVer) {
        _keyVer = keyVer;
    }

    public void setModulusCredit(BigInteger modulus) {
    _modulusCredit = modulus;
}

    public void setExponentCredit(BigInteger exponent) {
        _exponentCredit = exponent;
    }

    public void setKeyTypeCredit(int keyType) {
        _keyTypeCredit = keyType;
    }

    public void setKeyVerCredit(int keyVer) {
        _keyVerCredit = keyVer;
    }

    public void setTerOpePort(int port) {
        _terOpePort = port;
    }

    public void setPlainSessionKey(String key) {
        _plainSessionKey = key;
    }

    public BigInteger getModulus() {
        return _modulus;
    }

    public BigInteger getExponent() {
        return _exponent;
    }

    public int getKeyType() {
        return _keyType;
    }

    public int getKeyVer() {
        return _keyVer;
    }

    public BigInteger getModulusCredit() {
        return _modulusCredit;
    }

    public BigInteger getExponentCredit() {
        return _exponentCredit;
    }

    public int getKeyTypeCredit() {
        return _keyTypeCredit;
    }

    public int getKeyVerCredit() {
        return _keyVerCredit;
    }

    public int getTerOpePort() {
        return _terOpePort;
    }

    public String getSessionKey() {
        final String key = McUtils.bytesToHexString(
                Crypto.RSA.encrypt(McUtils.hexStringToBytes(_plainSessionKey), _modulus, _exponent));
        return key;
    }

    public String getEncryptData(String data) {
        String encryptData = "";
        int dataLen = (data.length() / 2);
        for (int i = 0; ; i++) {
            int st = (i * 373 * 2);
            int ed = (st + (373 * 2));
            if (dataLen > 373) {
                // 373Byteを超える場合、373Byteごとのブロックに分けて暗号化する
                encryptData += McUtils.bytesToHexString(
                        Crypto.RSA.encrypt(McUtils.hexStringToBytes(data.substring(st, ed)), _modulusCredit, _exponentCredit));
                dataLen -= 373;
            } else if (dataLen > 0) {
                encryptData += McUtils.bytesToHexString(
                        Crypto.RSA.encrypt(McUtils.hexStringToBytes(data.substring(st)), _modulusCredit, _exponentCredit));
                break;
            } else {
                break;
            }
        }
        return encryptData;
    }

    /**
     * 疎通確認
     */
    public int echo() {
        int ret = 0;

        try {
            AppPreference.execMcEcho(); //echo実行フラグON
            Echo.Response res = _mcPosCenterApi.echo(AppPreference.isDetachJR(), AppPreference.isDetachQR());
            if (res.result) {
                ret = CreditSettlement.k_OK;
                AppPreference.setIsAvailable(res.useable); //利用許可状態をRAMに保持
            } else {
                Timber.tag(LOGTAG).e("Fail echo errorCode:%s", res.errorCode);
                _creditSettlement.setCreditError(CreditErrorCodes.T29);
                ret = CreditSettlement.k_ERROR_UNKNOWN;
            }
        } catch (IOException | HttpStatusException | IllegalStateException e) {
            //Timber.tag(LOGTAG).d("HttpStatusException echo StatusCode:%d", ((HttpStatusException) e).getStatusCode());
            Timber.e(e);
            _creditSettlement.setCreditError(CreditErrorCodes.T05);
            ret = CreditSettlement.k_ERROR_UNKNOWN;
        }
        return ret;
    }

    /**
     * カードデータ保護用公開鍵取得
     */
    public int creditGetKey() {
        int ret = 0;
        Timber.tag(LOGTAG).i("カードデータ保護用公開鍵取得要求");

        try {
            CreditGetKey.Response res = _mcPosCenterApi.creditGetKey();
            if (res.result) {
                ret = CreditSettlement.k_OK;
                Timber.tag(LOGTAG).i("カードデータ保護用公開鍵取得応答成功");
            } else {
                _creditSettlement.setCreditError(CreditErrorCodes.T91);
                ret = CreditSettlement.k_ERROR_UNKNOWN;
                Timber.tag(LOGTAG).e("カードデータ保護用公開鍵取得応答失敗（エラーコード:%s）", res.errorCode);
            }
        } catch (IOException | HttpStatusException | IllegalStateException e) {
            //Timber.tag(LOGTAG).d("HttpStatusException creditGetKey StatusCode:%d", ((HttpStatusException) e).getStatusCode());
            Timber.e(e);
            _creditSettlement.setCreditError(CreditErrorCodes.T08);
            ret = CreditSettlement.k_ERROR_UNKNOWN;
        }
        return ret;
    }

    /**
     * カード判定
     */
    public int cardAnalyze() {
        int ret = 0;

        try {
            String rsaData;

            final CardAnalyze.Request request = new CardAnalyze.Request();

            // 鍵種別（1：RSA-3072）
            request.keytype = getKeyTypeCredit();
            // 鍵バージョン
            request.keyVersion = getKeyVerCredit();
            // 暗号化データ
            rsaData = _creditSettlement.getTrackInfo();
            _creditSettlement.saveTrackData(rsaData);
            rsaData = getEncryptData(rsaData);
            request.rsaData = rsaData;

            Timber.tag(LOGTAG).i("カード判定要求");
            CardAnalyze.Response res = _mcPosCenterApi.cardAnalyze(request);
            if (res.result) {
                // パラメータ保存
                ParamManage pm = new ParamManage();
                pm.saveEmvConfig(res);
                // クレジット情報保存（売上、印刷用）
                _creditSettlement.saveCreditResult(res);
                ret = CreditSettlement.k_OK;
                Timber.tag(LOGTAG).i("カード判定応答成功");
            } else {
                _creditSettlement.setCreditError(CreditErrorCodes.T13);
                ret = CreditSettlement.k_ERROR_UNKNOWN;
                Timber.tag(LOGTAG).e("カード判定応答失敗（エラーコード:%s）", res.errorCode);
            }
        } catch (IOException | HttpStatusException | IllegalStateException | SDKException e) {
            //Timber.tag(LOGTAG).d("HttpStatusException cardAnalyze StatusCode:%d", ((HttpStatusException) e).getStatusCode());
            Timber.e(e);
            _creditSettlement.setCreditError(CreditErrorCodes.T09);
            ret = CreditSettlement.k_ERROR_UNKNOWN;
        }
        return ret;
    }

    /**
     * オンラインオーソリ
     */
    public int onlineAuth() {
        int ret = 0;

        try {
            String iccData;
            String rsaData;

            final OnlineAuth.Request request = new OnlineAuth.Request();
            final CreditResult.Result creditResult = _creditSettlement._creditResult;
            /* 端末通番をインクリメント */
            AppPreference.termSequenceIncrement();

            // 端末番号
            request.terminalNo = AppPreference.getMcTermId();
            // 仕向け先コード
            request.deliveryCode = creditResult.deliveryCode;
            // クレジットアクワイアラID
            request.acquirerId = creditResult.acquirerId;
            // マネー区分
            request.moneyKbn = _creditSettlement.getMoneyKbn();
            // 磁気・IC区分
            request.msICKbn = _creditSettlement.getMSICKbn();
            // 使用ストライプ情報
            request.cardKbn = creditResult.cardKbn;
            // 操作日時（全て0でセンター採番）
            request.authDateTime = "00000000000000";
            // 売上金額
            request.fare = Amount.getFixedAmount();
            // 通番（伝票番号）
            request.terminalProcNo = AppPreference.getTermSequence();
            // 支払方法
            request.payMethod = _creditSettlement.getPayMethod();
            // 商品コード
            request.productCd = _creditSettlement.getProductCd();
            // 乗務員コード
            request.driverCd = AppPreference.getMcDriverId();
            // AID
            request.aid = _creditSettlement.get_fullAID();
            // POSエントリモード
            request.posEntryMode = _creditSettlement.getPosEntryMode();
            // PANシーケンスナンバー
            request.panSeqNo = _creditSettlement.getPanSeqNo();
            // IC端末対応フラグ
            request.icTerminalFlg = _creditSettlement.getIcTerminalFlg();
            // ブランド識別
            request.brandSign = creditResult.brandSign;
            // 鍵種別（1：RSA-3072）
            request.keyType = getKeyTypeCredit();
            // 鍵バージョン
            request.keyVersion = getKeyVerCredit();
            // 暗号化データに含めるトラック情報
            rsaData = _creditSettlement.loadTrackData();
            // 暗号化データに含める暗証番号
            rsaData += _creditSettlement.getFixedPinNo(request.aid);
            // 暗号化データに含めるICC関連データ
            if (CreditSettlement.k_MSIC_KBN_IC == _creditSettlement.getMSICKbn()) {
                iccData = _creditSettlement.getEmvProcInstance().emvGetSomeTagValues(_creditSettlement.getTagsOnlineAuthTable());
                rsaData += iccData;
            } else if (CreditSettlement.k_MSIC_KBN_CONTACTLESS_IC == _creditSettlement.getMSICKbn()) {
                byte[] tlv = _creditSettlement.getEmvCLProcInstance().getICCDataForAuth();

                iccData = String.format("%06X", tlv.length);
                iccData += ISOUtil.byte2hex(tlv);
                rsaData += iccData;
            } else {
                // ICC関連データなし（データ長0）
                iccData = "000000";
                rsaData += "000000";
            }
            // 暗号化
            rsaData = getEncryptData(rsaData);
            request.rsaData = rsaData;

            // 売上用暗号化データを保存
            iccData = getEncryptData(iccData);
            _creditSettlement.setRsaDataForPayment(iccData);

            // クレジット情報保存（売上、印刷用）
            _creditSettlement.saveCreditResult(request);
            _creditSettlement.setCreditResultValidFlg(true);

            Timber.tag(LOGTAG).i("オンラインオーソリ要求");
            OnlineAuth.Response res = _mcPosCenterApi.onlineAuth(request);
            if (res.result) {
                // クレジット情報保存（売上、印刷用）
                _creditSettlement.saveCreditResult(res);
                ret = CreditSettlement.k_OK;
                Timber.tag(LOGTAG).i("オンラインオーソリ応答成功");
            } else {
                Timber.tag(LOGTAG).e("オンラインオーソリ応答失敗（エラーコード:%s）", res.errorCode);
                _creditSettlement.setOnlineAuthErrorReason(CreditSettlement.AuthErrorReason.RES_NG);
                _creditSettlement.setCreditError(CreditErrorCodes.T14);
                // クレジット情報保存（拒否売上用）
                _creditSettlement.saveCreditResult(res);
                ret = CreditSettlement.k_ERROR_UNKNOWN;
            }
        } catch (IOException | HttpStatusException | IllegalStateException e) {
            //Timber.tag(LOGTAG).d("HttpStatusException onlineAuth StatusCode:%d", ((HttpStatusException) e).getStatusCode());
            Timber.e(e);
            _creditSettlement.setOnlineAuthErrorReason(CreditSettlement.AuthErrorReason.WAITRES_TOUT);
            _creditSettlement.setCreditError(CreditErrorCodes.T10);
            ret = CreditSettlement.k_ERROR_UNKNOWN;
        }
        return ret;
    }

    /**
     * オンラインオーソリ取消
     */
    public int onlineAuthCancel(SlipData slipData) {
        int ret = 0;

        try {
            String iccData;
            String rsaData;

            final OnlineAuthCancel.Request request = new OnlineAuthCancel.Request();
            final CreditResult.Result creditResult = _creditSettlement._creditResult;
            /* 端末通番をインクリメント */
            AppPreference.termSequenceIncrement();

            // 端末番号
            request.terminalNo = AppPreference.getMcTermId();
            // 仕向け先コード
            request.deliveryCode = creditResult.deliveryCode;
            // クレジットアクワイアラID
            request.acquirerId = creditResult.acquirerId;
            // マネー区分
            request.moneyKbn = _creditSettlement.getMoneyKbn();
            // 磁気・IC区分
            request.msICKbn = _creditSettlement.getMSICKbn();
            // 使用ストライプ情報
            request.cardKbn = creditResult.cardKbn;
            // 操作日時（全て0でセンター採番）
            request.authDateTime = "00000000000000";
            // 取消対象支払日時
            String dateTime = slipData.transDate;
            dateTime = dateTime.replace("/", "");
            dateTime = dateTime.replace(" ", "");
            dateTime = dateTime.replace(":", "");
            request.authCancelDateTime = dateTime;
            // 売上金額
            request.fare = slipData.transAmount;
            // 通番（伝票番号）
            request.terminalProcNo = AppPreference.getTermSequence();
            // 取消対象支払通番
            request.terminalCancelProcNo = slipData.termSequence;
            // 支払方法
            request.payMethod = _creditSettlement.getPayMethod();
            // 商品コード
            request.productCd = _creditSettlement.getProductCd();
            // 乗務員コード
            request.driverCd = AppPreference.getMcDriverId();
            // AID
            request.aid = creditResult.aid;
            // POSエントリモード
            request.posEntryMode = _creditSettlement.getPosEntryMode();
            // PANシーケンスナンバー
            request.panSeqNo = _creditSettlement.getPanSeqNo();
            // IC端末対応フラグ
            request.icTerminalFlg = _creditSettlement.getIcTerminalFlg();
            // ブランド識別
            request.brandSign = creditResult.brandSign;
            // 鍵種別（1：RSA-3072）
            request.keyType = getKeyTypeCredit();
            // 鍵バージョン
            request.keyVersion = getKeyVerCredit();
            // 暗号化データに含めるトラック情報
            rsaData = _creditSettlement.loadTrackData();
            // 暗号化データに含める暗証番号
            rsaData += _creditSettlement.getFixedPinNo(request.aid);
            // 暗号化データに含めるICC関連データ（ICC関連データなし（データ長0））
            iccData = "000000";
            rsaData += iccData;
            // 暗号化
            rsaData = getEncryptData(rsaData);
            request.rsaData = rsaData;

            // 売上用暗号化データを保存
            iccData = getEncryptData(iccData);
            _creditSettlement.setRsaDataForPayment(iccData);

            // クレジット取消情報保存（売上、印刷用）
            _creditSettlement.saveCreditResult(request);

            Timber.tag(LOGTAG).i("オンラインオーソリ取消要求");
            OnlineAuthCancel.Response res = _mcPosCenterApi.onlineAuthCancel(request);
            if (res.result) {
                // クレジット取消情報保存（売上、印刷用）
                _creditSettlement.saveCreditResult(res);
                ret = CreditSettlement.k_OK;
                Timber.tag(LOGTAG).i("オンラインオーソリ取消応答成功");
            } else {
                Timber.tag(LOGTAG).e("オンラインオーソリ取消応答失敗（エラーコード:%s）", res.errorCode);
                _creditSettlement.setCreditError(CreditErrorCodes.T15);
                ret = CreditSettlement.k_ERROR_UNKNOWN;
            }
        } catch (IOException | HttpStatusException | IllegalStateException e) {
            //Timber.tag(LOGTAG).d("HttpStatusException onlineAuthCancel StatusCode:%d", ((HttpStatusException) e).getStatusCode());
            Timber.e(e);
            _creditSettlement.setCreditError(CreditErrorCodes.T11);
            ret = CreditSettlement.k_ERROR_UNKNOWN;
        }
        return ret;
    }
}