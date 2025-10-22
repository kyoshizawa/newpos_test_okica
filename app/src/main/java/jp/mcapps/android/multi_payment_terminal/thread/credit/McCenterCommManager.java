package jp.mcapps.android.multi_payment_terminal.thread.credit;

import com.pos.device.SDKException;

import java.io.IOException;
import java.math.BigInteger;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.data.CreditResult;
//import jp.mcapps.android.multi_payment_terminal.thread.emv.ParamManage;
//import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
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

}