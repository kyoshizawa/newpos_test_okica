package jp.mcapps.android.multi_payment_terminal;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioManager;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.pos.device.SDKManager;
import com.pos.device.sys.SystemManager;
import com.pos.device.telephony.ApnInfo;
import com.pos.device.telephony.ApnItemInfo;
import com.pos.device.telephony.PosTelephonyManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.TimeZone;

import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaExceptions;
import jp.mcapps.android.multi_payment_terminal.logger.EventLogTree;
import jp.mcapps.android.multi_payment_terminal.thread.emv.CAPK;
import jp.mcapps.android.multi_payment_terminal.thread.emv.RiskManagementParameter;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.AssetsUtil;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.util.SimUtils;
import timber.log.Timber;

public class MainApplication extends Application {
    private static MainApplication _instance;
    private static Typeface _typeface;
    private static ViewModelFactory _viewModelFactory;

    public static MainApplication getInstance() {
        return _instance;
    }
    public static ViewModelFactory getViewModelFactory() {
        return _viewModelFactory;
    }

    private ScreenData _screenData;
    public ScreenData getScreenData() {
        return _screenData;
    }

    public static Typeface get_typeface() {
        return _typeface;
    }
	
    private boolean _isAppFinishCheck = false;
    public boolean isAppFinishCheck() { return _isAppFinishCheck; }
    public void setAppFinishCheck(boolean b) { _isAppFinishCheck = b; }

    private BusinessType _businessType;
    public BusinessType getBusinessType() { return _businessType; }
    public void setBusinessType(BusinessType type) { _businessType = type; }

    private byte[] _idPinAesKye = new byte[16];
    public byte[] getIdPinAesKye() { return _idPinAesKye; }
    public void setIdPinAesKye(byte[] key) { _idPinAesKye = key; }

    private String _errorCode = null;
    public String getErrorCode() { return _errorCode; }
    public void setErrorCode(String errorCode) { _errorCode = errorCode; }

    private int _cashValue = 0;
    public int getCashValue() { return _cashValue; }
    public void setCashValue(int cashValue) { _cashValue = cashValue; }

    private int _qrEnabledFlags = -1;  // -1は未認証
    public int getQREnabledFlags() {
        return _qrEnabledFlags;
    }
    public void setQREnabledFlags(int flags) {
        _qrEnabledFlags = flags;
    }
    public void addQREnabledFlags(int flag) {
        _qrEnabledFlags |= flag;
    }

    private boolean _isMcAuthSuccess = false;
    public boolean isMcAuthSuccess() {
        return _isMcAuthSuccess;
    }
    public void isMcAuthSuccess(boolean b) {
        _isMcAuthSuccess = b;
    }

    private boolean _isSDKInit = false;
    public boolean isSDKInit() {
        return _isSDKInit;
    }

    private byte[] _roomAesKye = new byte[16];
    public byte[] getRoomAesKye() { return _roomAesKye; }

    private OptionService _optionService;
    public OptionService getOptionService() {
        return _optionService;
    }
    public void setOptionService(OptionService service) {
        _optionService = service;
    }

    private boolean _isInitFeliCaSAM = false;
    public boolean isInitFeliCaSAM() {
        return _isInitFeliCaSAM;
    }
    public void isInitFeliCaSAM(boolean b) {
        _isInitFeliCaSAM = b;
    }

    private ICMaster _okicaICMaster = null;
    public ICMaster getOkicaICMaster() {
        return _okicaICMaster;
    }
    public void setOkicaICMaster(ICMaster master) {
        _okicaICMaster = master;
    }

    private CAPK[] _capk = null;
    public CAPK[] getCAPK() {
        return _capk;
    }
    public void setCAPK(CAPK[] capk) {
        _capk = capk;
    }

    private RiskManagementParameter[] _riskManagementParameter = null;
    public RiskManagementParameter[] getRiskManagementParameter() {
        return _riskManagementParameter;
    }
    public void setRiskManagementParameter(RiskManagementParameter[] p) {
        _riskManagementParameter = p;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        _instance = this;
        _viewModelFactory = new ViewModelFactory();

        AppPreference.initialize(this);

        // Google Fonts [Kosugi Maru] のダウンロードフォント指定
        _typeface = Typeface.createFromAsset(getAssets(), "KosugiMaru-Regular.ttf");

        // Timber.plant(new Timber.DebugTree());
        Timber.plant(new EventLogTree(_viewModelFactory._eventLogger));

        //try catchされていない例外を補足
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(handler, _viewModelFactory._eventLogger));

        initAmplify(); //Amplify初期化

        initSDKManager(); //SDKManager初期化

        TimeZone timeZone = TimeZone.getTimeZone("Asia/Tokyo");
        TimeZone.setDefault(timeZone);

        initVolume();

        copyAssetsToFileDir();

        try {
            //ScreenDataのインスタンス生成
            Constructor<ScreenData> constructor = ScreenData.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            _screenData = constructor.newInstance();
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            final BufferedInputStream inputStream =
                    new BufferedInputStream(getAssets().open(BuildConfig.ID_PIN_AES_KEY));

            byte[] buf = new byte[32];

            inputStream.read(buf);
            _idPinAesKye = McUtils.hexStringToBytes(new String(buf));
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            final BufferedInputStream inputStream =
                    new BufferedInputStream(getAssets().open(BuildConfig.ROOM_COLUMN_AES_KEY));

            byte[] buf = new byte[32];

            inputStream.read(buf);
            _roomAesKye = McUtils.hexStringToBytes(new String(buf));
        } catch (IOException e) {
            Timber.e(e);
        }
    }

    //Amplify初期化
    private void initAmplify() {
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSS3StoragePlugin());
            Amplify.addPlugin(new AWSApiPlugin());

            AmplifyConfiguration config = AmplifyConfiguration.builder(getApplicationContext())
                    .devMenuEnabled(false)
                    .build();
            Amplify.configure(config, getApplicationContext());
        } catch (AmplifyException e) {
            Timber.e(e, "Could not initialize Amplify");
        }
    }

    private void initSDKManager() {
        SDKManager.init(this, () -> {
            _isSDKInit = true;

            //
            SimUtils.SetInitSDK(true);
            DeviceUtils.SetInitSDK(true);

            if (!BuildConfig.DEBUG) {
                //センター配信に関係のない設定はここでしておく
                SystemManager.setPowerKeyDisable(true); //決済中に押されたら困るため、電源ボタン押下での画面sleepを禁止
                SystemManager.setDisableStatusBar(true);   //ステータスバーの無効化
                SystemManager.setHomeRecentAppKeyEnable(false, false);  //ホームキー、アプリ切替キーの無効化
                SystemManager.setDefaultLauncher(getPackageName(), "jp.mcapps.android.multi_payment_terminal.StartActivity");   //ホームアプリの設定
            }

            if(AppPreference.isDemoMode()) {
                //デモモードの場合は次回起動時以降NoSIMのダイアログを表示しない
                //TODO:wifi運用の場合もダイアログを表示しない予定
                SystemManager.setNoSimDialogVisibility(false);
            } else {
                SystemManager.setNoSimDialogVisibility(true);
            }

            PosTelephonyManager ptm = PosTelephonyManager.getInstance();
            List<ApnItemInfo> apnList = ptm.getApnList("440", "03"); //設定済のAPNを取得 iij
            // 未設定(初回起動)の場合のみAPNを設定
            if (apnList == null || apnList.size() == 0) {
                ApnInfo apn = new ApnInfo();
                apn.setName("iij");
                apn.setApn("iijmobile.biz");
                apn.setUserName("mobile@iij");
                apn.setPassword("iij");
                apn.setMcc("440");
                apn.setMnc("03");
                apn.setAuthType(ApnInfo.APN_AUTHENTICATION_TYPE_PAP_CHAP);

                ptm.addNewApn(apn); //APN設定
                Timber.i("APN setting");
            }

            apnList = ptm.getApnList("440", "20"); //設定済みのAPNを取得 SoftBank
            // 未設定の場合(初回起動)の場合のみAPNを設定
            if (apnList == null || apnList.size() == 0) {
                ApnInfo apn = new ApnInfo();
                apn.setName("softbank");
                apn.setApn("plus.4g");
                apn.setUserName("plus");
                apn.setPassword("4g");
                apn.setMcc("440");
                apn.setMnc("20");

                ptm.addNewApn(apn); //APN設定
                Timber.i("softbank APN setting");
            }
        });
    }

    private void initVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //決済音・決済案内音
        int alertMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (int)(alertMaxVolume * 0.7), 0);

        //業務終了時の音声
        int mediaMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(mediaMaxVolume * 0.4), 0);

        //ボタン音
        int ringMaxvolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, (int)(ringMaxvolume * 0.5), 0);
    }

    private void copyAssetsToFileDir() {
        AssetsUtil.init(this);
        AssetsUtil.copyAssetsToData("libEMVL2.so");
        AssetsUtil.copyAssetsToData("libPaypass.so");
        AssetsUtil.copyAssetsToData("libPaywave.so");
        AssetsUtil.copyAssetsToData("libJCB.so");
        AssetsUtil.copyAssetsToData("libAMEX.so");
        AssetsUtil.copyAssetsToData("libDiscover.so");
    }
}
