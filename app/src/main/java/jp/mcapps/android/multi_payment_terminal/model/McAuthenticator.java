package jp.mcapps.android.multi_payment_terminal.model;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Driver;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Exchange1;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Exchange2;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.GetKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.TerminalInfo;
import timber.log.Timber;

public class McAuthenticator {
    private static final int MAX_RETRY_COUNT = 3;
    private McPosCenterApi _apiClient = new McPosCenterApiImpl();
    private BigInteger _modulus;
    private BigInteger _exponent;
    private byte[] _aesKey;
    private byte[] _aesIV;
    private String _plainSessionKey1;
    private String _encodedSessionKey2;

    public String authenticate() {
        Timber.i("MC認証実行");
        String errCode;
        try {
            errCode = getKey();
        } catch (Exception e) {
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        if (errCode != null) {
            Timber.e("MC認証失敗 -> 公開鍵取得失敗(エラーコード:%s)", errCode);
            return errCode;
        }
        Timber.i("MC認証 -> 公開鍵取得成功");

        try {
            errCode = exchange1();
        } catch (Exception e) {
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
        if (errCode != null) {
            Timber.e("MC認証失敗 -> 相互認証1失敗(エラーコード:%s)", errCode);
            return errCode;
        }
        Timber.i("MC認証 -> 相互認証1成功");

        try {
            errCode = exchange2();
        } catch (Exception e) {
            errCode = McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
        if (errCode != null) {
            Timber.e("MC認証失敗 -> 相互認証2失敗(エラーコード:%s)", errCode);
            return errCode;
        }
        Timber.i("MC認証 -> 相互認証2成功");
        Timber.i("MC認証成功");

        final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        final Date currentDate = new Date();

        AppPreference.setDatetimeAuthenticationMc(dateFmt.format(currentDate));
        MainApplication.getInstance().isMcAuthSuccess(true);

        return null;
    }

    private String getKey() {
        final int MODULUS_BITS = 384;
        final int EXPONENT_BITS = 3;

        try {
            GetKey.Response response = _apiClient.getKey();

            if (!response.result) {
                return McPosCenterErrorMap.get(response.errorCode);
            }

            CreditSettlement.getInstance()._mcCenterCommManager.setKeyType(response.spec);
            CreditSettlement.getInstance()._mcCenterCommManager.setKeyVer(response.ver);

            final String pubKey = response.data;

            // 16進数"文字列"なのでビットの倍でとる
            _modulus = new BigInteger(pubKey.substring(0, MODULUS_BITS*2), 16);
            CreditSettlement.getInstance()._mcCenterCommManager.setModulus(_modulus);
            _exponent = new BigInteger(pubKey.substring(pubKey.length()-(EXPONENT_BITS*2)), 16);
            CreditSettlement.getInstance()._mcCenterCommManager.setExponent(_exponent);

            return null;
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            return MainApplication.getInstance().getString(R.string.error_type_startup_comm_reception);
        } catch (IOException | HttpStatusException e) {
            Timber.e(e);
            Timber.d("getKey Exception %d", ((HttpStatusException)e).getStatusCode());
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
    }

    private String exchange1() {
        final byte[][] keyAndIv = Crypto.AES256.generateKeyAndIV();

        if (keyAndIv == null) {
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        _aesKey = keyAndIv[0];
        _aesIV = keyAndIv[1];

        final String plainKey1 =
                McUtils.bytesToHexString(_aesKey) + McUtils.bytesToHexString(_aesIV);

        final String key1 = McUtils.bytesToHexString(
                Crypto.RSA.encrypt(McUtils.hexStringToBytes(plainKey1) , _modulus, _exponent));

        if (key1 == null) {
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        _plainSessionKey1 = UUID.randomUUID().toString().replace("-", "");

        final String sessionKey1 = McUtils.bytesToHexString(
                Crypto.RSA.encrypt(McUtils.hexStringToBytes( _plainSessionKey1), _modulus, _exponent));

        if (sessionKey1 == null) {
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }

        try {
            final Exchange1.Response response = _apiClient.exchange1(key1, sessionKey1);

            if (!response.result) {
                return McPosCenterErrorMap.get(response.errorCode);
            }

            final String decoded = McUtils.bytesToHexString(
                    Crypto.AES256.decrypt(McUtils.hexStringToBytes(response.sessionKey1), _aesKey, _aesIV));

            if (!decoded.toUpperCase().equals(_plainSessionKey1.toUpperCase())) {
                Timber.e("セッションキーが一致しない");
                return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
            }

            CreditSettlement.getInstance()._mcCenterCommManager.setPlainSessionKey(_plainSessionKey1);

            _encodedSessionKey2 = response.sessionKey2;
            return null;
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            return MainApplication.getInstance().getString(R.string.error_type_startup_comm_reception);
        } catch (IOException e) {
            Timber.e(e);
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.d("exchange1 Exception %d", e.getStatusCode());
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
    }

    private String exchange2() {
        final byte[] decoded = Crypto.AES256.decrypt(
                McUtils.hexStringToBytes(_encodedSessionKey2), _aesKey, _aesIV);

        final String sessionKey2 = McUtils.bytesToHexString(
                Crypto.RSA.encrypt(decoded, _modulus, _exponent));

        try {
            final Exchange2.Response response = _apiClient.exchange2(sessionKey2);

            if (!response.result) {
                return McPosCenterErrorMap.get(response.errorCode);
            } else {
                CreditSettlement.getInstance()._mcCenterCommManager.setTerOpePort(response.forward);
            }

            return null;
        } catch (UnknownHostException | SocketTimeoutException e) {
            Timber.e(e);
            return MainApplication.getInstance().getString(R.string.error_type_startup_comm_reception);
        } catch (IOException e) {
            Timber.e(e);
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        } catch (HttpStatusException e) {
            Timber.d("exchange2 Exception %d", ((HttpStatusException)e).getStatusCode());
            return McPosCenterErrorMap.INTERNAL_ERROR_CODE;
        }
    }
}
