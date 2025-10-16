package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.google.gson.Gson;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import androidx.core.app.ActivityCompat;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CAKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Car;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CardAnalyze;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CreditGetKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Driver;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Echo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Exchange1;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Exchange2;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.GetKey;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuth;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuthCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.PostTerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.RiskParameterContactless;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPoint;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPointCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment.Payment;
import timber.log.Timber;

import com.pos.device.config.DevConfig;
import com.pos.device.config.DevConfig.CUSTOMER;

@SuppressLint("HardwareIds")
public class McPosCenterApiImpl implements McPosCenterApi {
    private final McHttpClient _httpClient = McHttpClient.getInstance();
    private final Map<String, String> _commonHeader = new HashMap<>();
    private final Map<String, String> _commonHeader2 = new HashMap<>();

    {
        String imei = null;
        try {
            final Application app = MainApplication.getInstance();
            final TelephonyManager tm = (TelephonyManager) app.getSystemService(Context.TELEPHONY_SERVICE);

            if (ActivityCompat.checkSelfPermission(app, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("権限エラーREAD_PHONE_STATE");
            } else {
                //imei = tm.getDeviceId();
                imei = CUSTOMER.ID;
            }

        } catch (Exception e) {
            Timber.e(e);
        }

        String sn = DeviceUtils.getSerial();

        // 相互認証前のヘッダ
        _commonHeader.put("X-Client-Model", Build.MODEL);
        _commonHeader.put("X-Client-SN", sn);

        // 相互認証後のヘッダ
        _commonHeader2.put("X-Client-Model", Build.MODEL);
        _commonHeader2.put("X-Client-SN", sn);
        _commonHeader2.put("X-Client-SessionKey", "");

        if (imei != null) {
            _commonHeader.put("X-Client-IMEINO", imei);
            _commonHeader2.put("X-Client-IMEINO", imei);
        }
    }

    public GetKey.Response getKey() throws IOException, HttpStatusException {
        final String path = "/Auth/GetKey";

        final GetKey.Request request = new GetKey.Request();

        final GetKey.Response response = _httpClient
                .post(makeUrl(path, null))
                .addHeader(_commonHeader)
                .setBody(request)
                .responseObject(GetKey.Response.class);

        return response;
    }

    public Exchange1.Response exchange1(String key1, String sessionKey1) throws IOException, HttpStatusException {
        final String path = "/Auth/Exchange1";

        final Exchange1.Request request = new Exchange1.Request();
        request.spec = CreditSettlement.getInstance()._mcCenterCommManager.getKeyType();
        request.ver = CreditSettlement.getInstance()._mcCenterCommManager.getKeyVer();
        request.key1 = key1;
        request.sessionKey1 = sessionKey1;

        final Exchange1.Response response = _httpClient
                .post(makeUrl(path, null))
                .addHeader(_commonHeader)
                .setBody(request)
                .responseObject(Exchange1.Response.class);

        return response;
    }

    public Exchange2.Response exchange2(String sessionKey2) throws IOException, HttpStatusException {
        final String path = "/Auth/Exchange2";

        final Exchange2.Request request = new Exchange2.Request();
        request.spec = CreditSettlement.getInstance()._mcCenterCommManager.getKeyType();
        request.ver = CreditSettlement.getInstance()._mcCenterCommManager.getKeyVer();
        request.sessionKey2 = sessionKey2;

        final Exchange2.Response response = _httpClient
                .post(makeUrl(path, null))
                .addHeader(_commonHeader)
                .setBody(request)
                .responseObject(Exchange2.Response.class);

        return response;
    }

    /**
     * 疎通確認
     */
    public Echo.Response echo(boolean detachJR, boolean detachQR) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");
        final String path = "/Term/Echo";

        final Echo.Request request = new Echo.Request();
        request.detachJR = detachJR;
        request.detachQR = detachQR;

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final Echo.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(Echo.Response.class);

        return response;
    }

    /**
     * カードデータ保護用公開鍵取得
     */
    public CreditGetKey.Response creditGetKey() throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Credit/GetKey";
        final int MODULUS_BITS = 384;
        final int EXPONENT_BITS = 3;

        final CreditGetKey.Request request = new CreditGetKey.Request();
        // 鍵種別（1：RSA-3072）
        CreditSettlement.getInstance()._mcCenterCommManager.setKeyTypeCredit(CreditSettlement.getInstance().k_KEYTYPE_RSA3072);
        request.keyType = CreditSettlement.getInstance()._mcCenterCommManager.getKeyTypeCredit();
        // 鍵バージョン（0：最新バージョンを取得）
        request.keyVersion = 0;

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final CreditGetKey.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(CreditGetKey.Response.class);

        if (response.result) {
            final String pubKey = response.keyData;
            // 鍵バージョン
            CreditSettlement.getInstance()._mcCenterCommManager.setKeyVerCredit(response.keyVersion);
            // 鍵本体
            // 16進数"文字列"なのでビットの倍でとる
            BigInteger modulus = new BigInteger(pubKey.substring(0, MODULUS_BITS * 2), 16);
            CreditSettlement.getInstance()._mcCenterCommManager.setModulusCredit(modulus);
            BigInteger exponent = new BigInteger(pubKey.substring(pubKey.length() - (EXPONENT_BITS * 2)), 16);
            CreditSettlement.getInstance()._mcCenterCommManager.setExponentCredit(exponent);
        }

        return response;
    }

    /**
     * カード判定
     */
    public CardAnalyze.Response cardAnalyze(CardAnalyze.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Credit/CardAnalyze";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final CardAnalyze.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(CardAnalyze.Response.class);

        return response;
    }

    /**
     * オンラインオーソリ
     */
    public OnlineAuth.Response onlineAuth(OnlineAuth.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Credit/OnlineAuth";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final OnlineAuth.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(OnlineAuth.Response.class);

        return response;
    }

    /**
     * オンラインオーソリ取消
     */
    public OnlineAuthCancel.Response onlineAuthCancel(OnlineAuthCancel.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Credit/OnlineAuthCancel";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final OnlineAuthCancel.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(OnlineAuthCancel.Response.class);

        return response;
    }

    /**
     * 端末情報取得
     */
    public TerminalInfo.Response getTerminalInfo() throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Term/GetInfo";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());
        TerminalInfo.Request request = new TerminalInfo.Request();

        final TerminalInfo.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(TerminalInfo.Response.class);

        return response;
    }

    /**
     * 端末稼働情報連携
     * @param type 送信種別
     *             0：電マネ開局(自動/手動)
     *             1：業務終了
     */
    public PostTerminalInfo.Response postTerminalInfo(int type) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Term/PostInfo";

        final PostTerminalInfo.Request request = new PostTerminalInfo.Request(type);
        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final PostTerminalInfo.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(PostTerminalInfo.Response.class);

        return response;
    }

    /**
     * 端末情報取得
     */
    public Driver.Response getDriver(int driverCode, boolean update, String driverName) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Term/SetDriver";

        final Driver.Request request = new Driver.Request();
        request.driverCd = driverCode;
        request.update = update;
        request.driverName = driverName;

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final Driver.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(Driver.Response.class);

        return response;
    }

    /**
     * 機器番号設定
     */
    public Car.Response setCar(int carId) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Term/SetCar";

        final Car.Request request = new Car.Request();
        request.carNo = carId;

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final Car.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(Car.Response.class);

        return response;
    }

    /**
     * 売上情報連携
     */
    public Payment.Response postPayment(Payment.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Term/Payment";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final Payment.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(Payment.Response.class);

        return response;
    }

    public CAKey.Response getCAKey() throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Credit/CAKey";

        final CAKey.Request request = new CAKey.Request();

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final CAKey.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(CAKey.Response.class);

        Gson gson = new Gson();
        Timber.d(gson.toJson(response));

        return response;
    }

    public Payment.Response postInvalidPayment(Payment.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Term/InvalidPayment";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final Payment.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(Payment.Response.class);

        return response;
    }

    @Override
    public RiskParameterContactless.Response getRiskParameterContactless(RiskParameterContactless.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Credit/GetRiskParameterContactless";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final RiskParameterContactless.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(RiskParameterContactless.Response.class);

        return response;
    }

    /**
     * ポイント付与
     */
    public WatariPoint.Response watariAdd(WatariPoint.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Point/AddPoint";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final WatariPoint.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(WatariPoint.Response.class);

        return response;
    }

    /**
     * ポイント取消
     */
    public WatariPointCancel.Response watariCancel(WatariPointCancel.Request request) throws IOException, HttpStatusException, IllegalStateException {
        if (!isExchangeOK()) throw new IllegalStateException("相互認証未成功");

        final String path = "/Point/CancelPoint";

        // ヘッダのセッションキー
        _commonHeader2.put("X-Client-SessionKey", CreditSettlement.getInstance()._mcCenterCommManager.getSessionKey());

        final WatariPointCancel.Response response = _httpClient
                .post(makeUrl(path, String.valueOf(CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort())))
                .addHeader(_commonHeader2)
                .setBody(request)
                .responseObject(WatariPointCancel.Response.class);

        return response;
    }

    private String makeUrl(String path, String port){
        String url;
        url = BuildConfig.POS_CENTER_BASE_URL;
        if (null == port) {
            url += BuildConfig.POS_CENTER_BASE_URL_SUB + path;
        } else {
            url += ":" + port + BuildConfig.POS_CENTER_BASE_URL_SUB + path;
        }
        return url;
    }

    private boolean isExchangeOK() {
        return CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort() > 0;
    }
}