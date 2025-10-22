package jp.mcapps.android.multi_payment_terminal.ui.device_check;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.pos.device.printer.Printer;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.CustomScannerActivity;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.QRLayouts;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeviceCheckBinding;
import jp.mcapps.android.multi_payment_terminal.devices.SamRW;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CardInfo;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CardListener;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CardManager;
import jp.mcapps.android.multi_payment_terminal.thread.felica.FelicaManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.util.SimUtils;
import timber.log.Timber;
//
//import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_IC;
//import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_MAG;
//import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_NFC;

public class DeviceCheckFragment extends BaseFragment implements DeviceCheckEventHandlers {
    public static DeviceCheckFragment newInstance() {
        return new DeviceCheckFragment();
    }

    private final String SCREEN_NAME = "デバイスチェック";
    private DeviceCheckViewModel _deviceCheckViewModel;
    private TextView _resultTextView;
    private final static int k_DEVICE_CHECK_TIMEOUT = 15000;  // デバイスチェックのタイムアウト（ms）
    @SuppressWarnings("deprecation")
    private static ProgressDialog _progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentDeviceCheckBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_device_check, container, false);

        _deviceCheckViewModel =
                new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(DeviceCheckViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);
        binding.setViewModel(_deviceCheckViewModel);

        _resultTextView = binding.textDeviceCheckResult;
        _deviceCheckViewModel.getResultText().observe(getViewLifecycleOwner(), _resultTextView::setText);
        _resultTextView.setMovementMethod(new ScrollingMovementMethod()); //スクロール設定

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void checkGps(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        String resultText = "◆ＧＰＳ　　　：";

        //アプリの権限をチェック Android5での確認用にバージョンチェックも
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(view.getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //アプリの権限なし
            resultText += "✕　権限なし";
        } else {
            //位置情報ユーザ設定チェック
            LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //ユーザ設定でGPSがオフ
                resultText += "✕　オフ";
            } else {
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    resultText += "〇　オン\n[詳細] 未測位";
                } else {
                    int satellites = location.getExtras().getInt("satellites");
                    resultText += satellites <= 2 ? "〇　オン\n[詳細] 2D測位" : "〇　オン\n[詳細] 3D測位";
                }
            }
        }
        _deviceCheckViewModel.appendResultText(resultText);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.isRunning(false);
    }

    @Override
    public void checkSim(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        String resultText = "◆ＳＩＭ　　　：";
        switch (tm.getSimState()) {
            //SIM未接続
            case TelephonyManager.SIM_STATE_ABSENT:
                resultText += "✕　なし";
                break;

            //SIMロック、ネットワークPINが必要
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                resultText += "✕　あり\n[詳細] SIMロック(NetWorkPIN必要)";
                break;

            //SIMロック、ユーザPINが必要
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                resultText += "✕　あり\n[詳細] SIMロック(UserPIN必要)";
                break;

            //SIMロック、ユーザPUKが必要
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                resultText += "✕　あり\n[詳細] SIMロック(UserPUK必要)";
                break;

            //準備完了
            case TelephonyManager.SIM_STATE_READY:
                resultText += "〇　あり";
                break;

            //不明、状態移行中
            case TelephonyManager.SIM_STATE_UNKNOWN:
                resultText += "✕　あり\n[詳細] SIM状態移行中(再確認必要)";
                break;

            //8以降で追加される定数　ここから下には来ないはず

            //カードエラー、カードはあるが使えない状態
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                resultText += "✕　あり\n[詳細] SIMカードエラー";
                break;

            //SIM制限　キャリアによる制限があり使えない状態
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                resultText += "✕　あり\n[詳細] SIM制限中";
                break;

            //準備完了前の状態？
            case TelephonyManager.SIM_STATE_NOT_READY:
                resultText += "✕　あり\n[詳細] SIM使用不可";
                break;

            //カードエラー、恒久的な無効状態？
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                resultText += "✕　あり\n[詳細] SIM無効";
                break;
        }
        _deviceCheckViewModel.appendResultText( resultText);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.isRunning(false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void checkWifi(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        WifiManager wm = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        String resultText = "◆ＷｉＦｉ　　：";
        switch (wm.getWifiState()) {
            case WifiManager.WIFI_STATE_DISABLING :
                //無効化中の状態 すぐ無効になると思われるのでNG
            case WifiManager.WIFI_STATE_DISABLED :
                //無効の状態
            case WifiManager.WIFI_STATE_ENABLING :
                //有効化中の状態 この時点では無効と思われるのでNG
            case WifiManager.WIFI_STATE_UNKNOWN :
                //不明の状態 有効化または無効化中にエラーが発生した場合
                resultText += "✕　オフ";
                break;
            case WifiManager.WIFI_STATE_ENABLED :
                //有効の状態
                resultText += wm.getConnectionInfo().getNetworkId() != -1
                        ? "〇　オン\n[詳細] 接続\n<SSID> " + wm.getConnectionInfo().getSSID()
                            .replaceFirst("[\"]", "")
                            .replaceFirst("[\"]", "") //SSID取得 先頭と末尾に「"」がつくので置換
                        : "〇　オン\n[詳細] 未接続";
                break;
        }
        _deviceCheckViewModel.appendResultText(resultText);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.isRunning(false);
    }

    @Override
    public void clearResult(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _deviceCheckViewModel.clearResultText();
        _resultTextView.scrollTo(0, 0); //先頭にスクロール
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    @Override
    public void checkTerminal(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.appendResultText("◆端末情報　　：〇　取得完了\n[詳細]");
        //アプリの権限をチェック Android5での確認用にバージョンチェックも
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(view.getContext(),
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            _deviceCheckViewModel.appendResultText("<IMEI    > 権限なし");
            _deviceCheckViewModel.appendResultText("<IMSI    > 権限なし");
            _deviceCheckViewModel.appendResultText("<ICCID   > 権限なし");
        } else {
            //TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            //String imsi = tm.getSubscriberId() == null ? "SIMエラー" : tm.getSubscriberId();
            //String iccid = tm.getSimSerialNumber() == null ? "SIMエラー" : tm.getSimSerialNumber();
            String imei = SimUtils.getImei(getContext());
            String imsi = SimUtils.getImsi(getContext());
            String iccid = SimUtils.getIccId(getContext());

            //_deviceCheckViewModel.appendResultText("<IMEI    > " + tm.getDeviceId());
            _deviceCheckViewModel.appendResultText("<IMEI    > " + imei);
            _deviceCheckViewModel.appendResultText("<IMSI    > " + (TextUtils.isEmpty(imsi) ? "SIMエラー" : imsi));
            _deviceCheckViewModel.appendResultText("<ICCID   > " + (TextUtils.isEmpty(iccid) ? "SIMエラー" : iccid));
        }

        if(AppPreference.isServicePos()) {
            // POSアクティベート状態の時、決済システム側のデータではなくPOSのデータを表示する
            String merchantName = AppPreference.getPosMerchantName() == null ? "－" : AppPreference.getPosMerchantName();
            String merchantOffice = AppPreference.getPosMerchantOffice() == null ? "－" : AppPreference.getPosMerchantOffice();
            _deviceCheckViewModel.appendResultText("<会社名  > " + merchantName);
            _deviceCheckViewModel.appendResultText("<営業所名> " + merchantOffice);
        } else {
            _deviceCheckViewModel.appendResultText("<会社名  > " + AppPreference.getMerchantName());
            _deviceCheckViewModel.appendResultText("<営業所名> " + AppPreference.getMerchantOffice());
        }
        _deviceCheckViewModel.appendResultText("<号機番号> " + String.valueOf(AppPreference.getMcCarId()));
        _deviceCheckViewModel.appendResultText("<端末番号> " + String.valueOf(AppPreference.getMcTermId()));
        _deviceCheckViewModel.appendResultText("<モデル名> " + Build.MODEL);
        _deviceCheckViewModel.appendResultText("<SerialNo> " + DeviceUtils.getSerial());
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.isRunning(false);
    }

    @Override
    public void checkFelica(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        String Msg = "【FeliCaチェック開始】\n" + String.valueOf(k_DEVICE_CHECK_TIMEOUT/1000) + "秒以内にカードをタッチ\nしてください";
        showProgressDialog(view,Msg);

        FelicaManager fm = new FelicaManager();

        long startTime = System.currentTimeMillis();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable run = () -> {

            String idm = null;
            /* FeliCaカードを検出するまで、または、15秒経過するまで */
            /* ポーリングコマンドを送信し続ける */
            while(idm == null || idm.equals("0000000000000000")) {
                idm = fm.deviceCheck();

                if (System.currentTimeMillis() - startTime >= k_DEVICE_CHECK_TIMEOUT) {
                    Timber.d("time out");
                    break;
                }
            }

            String finalIdm = idm;
            handler.post(() -> {
                String result = "";
                if(finalIdm != null && !finalIdm.equals("0000000000000000")) {
                    /* FeliCaカードを検出 */
                    result = "◆ＦｅｌｉＣａ：〇　検出成功\n[詳細]\n<IDm> " + finalIdm;
                } else {
                    result += "◆ＦｅｌｉＣａ：✕　検出失敗";
                }
                dismissProgressDialog();
                _deviceCheckViewModel.appendResultText(result);
                _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
                _deviceCheckViewModel.isRunning(false);
            });
        };
        new Thread(run).start();
    }

    @Override
    public void checkIc(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        String Msg = "【接触ICチェック開始】\n" + String.valueOf(k_DEVICE_CHECK_TIMEOUT/1000) + "秒以内にカードを挿入して\nください";
        showProgressDialog(view,Msg);

        // 接触ICカードの検出開始
//        CardManager cardManager = CardManager.getInstance(INMODE_IC.getVal());
//        cardManager.getCard(k_DEVICE_CHECK_TIMEOUT, new CardListener() {
//            @Override
//            public void callback(CardInfo cardInfo) {
//                String resultText = "◆接触ＩＣ　　：✕　検出失敗";
//                if (INMODE_IC == cardInfo.getCardType()) {
//                    if (true == cardInfo.isResultFalg()) {
//                        resultText = "◆接触ＩＣ　　：〇　検出成功";
//                    }
//                }
//                dismissProgressDialog();
//                _deviceCheckViewModel.appendResultText(resultText);
//                _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
//                _deviceCheckViewModel.isRunning(false);
//            }
//        });
    }

    @Override
    public void checkContactless(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        String Msg = "【非接触ICチェック開始】\n" + String.valueOf(k_DEVICE_CHECK_TIMEOUT/1000) + "秒以内にカードをタッチ\nしてください";
        showProgressDialog(view,Msg);

        // 非接触ICカードの検出開始
//        CardManager cardManager = CardManager.getInstance(INMODE_NFC.getVal());
//        cardManager.getCard(k_DEVICE_CHECK_TIMEOUT, new CardListener() {
//            @Override
//            public void callback(CardInfo cardInfo) {
//                cardManager.stopPICC();
//                String resultText = "◆非接触ＩＣ　：✕　検出失敗";
//                if (INMODE_NFC == cardInfo.getCardType()) {
//                    if (true == cardInfo.isResultFalg()) {
//                        resultText = "◆非接触ＩＣ　：〇　検出成功";
//                    }
//                }
//                dismissProgressDialog();
//                _deviceCheckViewModel.appendResultText(resultText);
//                _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
//                _deviceCheckViewModel.isRunning(false);
//            }
//        });
    }

    @Override
    public void checkCamera(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("");
        integrator.setTimeout(k_DEVICE_CHECK_TIMEOUT);  // 単位はミリ秒
        integrator.setCaptureActivity(CustomScannerActivity.class)
                .addExtra(QRLayouts.KEY, QRLayouts.DEVICE_CHECK)
                .setBeepEnabled(true)
                .initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        if (result.getContents() != null) {
            _deviceCheckViewModel.appendResultText("◆カメラ　　　：〇　読取成功\n[詳細]\n<コード> " + result.getContents());
        } else {
            _deviceCheckViewModel.appendResultText("◆カメラ　　　：✕　読取失敗");
        }

        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.isRunning(false);
    }

    @Override
    public void checkMs(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        String Msg = "【磁気Rチェック開始】\n" + String.valueOf(k_DEVICE_CHECK_TIMEOUT/1000) + "秒以内にカードを通して\nください";
        showProgressDialog(view,Msg);

        // 磁気Rカードの検出開始
//        CardManager cardManager = CardManager.getInstance(INMODE_MAG.getVal());
//        cardManager.getCard(k_DEVICE_CHECK_TIMEOUT, new CardListener() {
//            @Override
//            public void callback(CardInfo cardInfo) {
//                String resultText = "◆磁気Ｒ　　　：✕　検出失敗";
//                if (INMODE_MAG == cardInfo.getCardType()) {
//                    if (true == cardInfo.isResultFalg()) {
//                        resultText = "◆磁気Ｒ　　　：〇　検出成功";
//                    }
//                }
//                dismissProgressDialog();
//                _deviceCheckViewModel.appendResultText(resultText);
//                //Activity遷移の都合でここではスクロールできない。onResumeでスクロールを行う
//                //_resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
//                _deviceCheckViewModel.isRunning(false);
//            }
//        });
    }

    private int getResultTextBottom() {
        //結果のテキストがテキストビューからはみ出す場合は一番下の位置を返す
        //はみ出さない場合は先頭の位置(0)を返す

        return _resultTextView.getLayout() != null  //onResumeで呼ぶためnullの可能性あり
                ? Math.max(_resultTextView.getLayout().getLineTop(_resultTextView.getLineCount()) - _resultTextView.getHeight(), 0)
                : 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void checkPrinter(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        PrinterManager printerManager = new PrinterManager();
        boolean isPrinterOK = false;

        String resultText = "◆プリンター　：";

        // ここからはじめる
        if (AppPreference.getIsExternalPrinter()){
            // 自動釣銭機のプリンターチェックあれば

        } else {
            int isPrinterSts = Printer.getInstance().getStatus();
            switch (isPrinterSts) {
                case Printer.PRINTER_OK: // 正常状態（0）
                    resultText += "〇　状態正常";
                    isPrinterOK = true;
                    break;
                case Printer.PRINTER_STATUS_BUSY: // ビジー状態（-1）
                    resultText += "✕　状態異常\n[詳細] ビジー";
                    break;
                case Printer.PRINTER_STATUS_HIGHT_TEMP: // 高温状態（-2）
                    resultText += "✕　状態異常\n[詳細] 高温";
                    break;
                case Printer.PRINTER_STATUS_PAPER_LACK: // 用紙切れ状態 (-3)
                    resultText += "✕　状態異常\n[詳細] 用紙切れ";
                    break;
                case Printer.PRINTER_STATUS_NO_BATTERY: // バッテリー残量不足状態（-4）
                    resultText += "✕　状態異常\n[詳細] バッテリー残量不足";
                    break;
                case Printer.PRINTER_STATUS_FEED: // 用紙送り状態（-5）
                    resultText += "✕　状態異常\n[詳細] 用紙送り";
                    break;
                case Printer.PRINTER_STATUS_PRINT: // 印刷状態（-6）
                    resultText += "✕　状態異常\n[詳細] 印刷";
                    break;
                case Printer.PRINTER_STATUS_FORCE_FEED: // 強制用紙送り状態（-7）
                    resultText += "✕　状態異常\n[詳細] 強制用紙送り";
                    break;
                case Printer.PRINTER_STATUS_POWER_ON: // 電源ON処理中状態（-8）
                    resultText += "✕　状態異常\n[詳細] 電源ON処理中";
                    break;
                case Printer.PRINTER_TASKS_FULL: // 処理満載状態（-9）
                    resultText += "✕　状態異常\n[詳細] 処理満載";
                    break;
                default:
                    resultText += "✕　状態異常\n[詳細] 不明(" + isPrinterSts + ")";
                    break;
            }
        }
            _deviceCheckViewModel.appendResultText(resultText);
            _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
            _deviceCheckViewModel.isRunning(false);

        if(isPrinterOK == true){
            printerManager.setPrintData_DeviceCheckResult(view, _resultTextView.getText().toString());
        }
    }

    @Override
    public void checkMeter(View view) {
        _deviceCheckViewModel.isRunning(true);
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.checkMeter().subscribe((response, error) -> {
            if (error != null) {
                _deviceCheckViewModel.appendResultText(String.format("◆メーター　　：✕　情報取得失敗"));
            } else {
                if (AppPreference.getIFBoxVersionInfo() != null) {
                    _deviceCheckViewModel.appendResultText(String.format("◆メーター　　：〇　情報取得成功\n[詳細]\n<SerialNo> %s\n<種別> %s\n<Version> %s\n<状態> %s\n<料金> %s",
                            AppPreference.getIFBoxVersionInfo().mcSerial, AppPreference.getIFBoxVersionInfo().appModel, AppPreference.getIFBoxVersionInfo().appVersion, response.status, response.fare));
                } else {
                    _deviceCheckViewModel.appendResultText(String.format("◆メーター　　：〇　情報取得成功\n[詳細]\n<SerialNo> %s\n<種別> %s\n<Version> %s\n<状態> %s\n<料金> %s",
                            "", "", "", response.status, response.fare));
                }
            }
            _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
            _deviceCheckViewModel.isRunning(false);
        });
    }

    @Override
    public void checkSam(View view) {

        SamRW.OpenResult Result = SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal);
        if (Result == SamRW.OpenResult.SAM_NOT_FOUND) {
            _deviceCheckViewModel.appendResultText(String.format("◆ＳＡＭ　　　：✕　なし"));
        } else if (Result == SamRW.OpenResult.SUCCESS) {
            _deviceCheckViewModel.appendResultText(String.format("◆ＳＡＭ　　　：〇　あり"));
        } else {
            _deviceCheckViewModel.appendResultText(String.format("◆ＳＡＭ　　　：✕　あり\n[詳細] SAMカードエラー"));
        }

        _resultTextView.scrollTo(0, getResultTextBottom()); //自動スクロール
        _deviceCheckViewModel.isRunning(false);
    }

    // ダイアログ表示
    @SuppressWarnings("deprecation")
    private void showProgressDialog(View view, String Message){
        _progressDialog = new ProgressDialog(view.getContext());
        _progressDialog.setMessage(Message);                               // 内容(メッセージ)設定
        _progressDialog.setCancelable(false);                              // キャンセル無効
        _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    // スタイル設定
        _progressDialog.show();                                            // ダイアログを表示
    }

    // ダイアログ閉じる
    private void dismissProgressDialog(){
        _progressDialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        //カメラチェック後のスクロール用
        _resultTextView.scrollTo(0, getResultTextBottom());
    }
}
