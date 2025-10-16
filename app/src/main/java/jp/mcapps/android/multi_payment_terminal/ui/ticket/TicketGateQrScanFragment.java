package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.TicketScannerActivity;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketGateQrScanBinding;
import jp.mcapps.android.multi_payment_terminal.service.PeriodicGateCheckService;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.BaseDialogFragment;
import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputEventHandlers;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketUpdateDynamicTicketStatus;
import java.util.UUID;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketPeople;
import timber.log.Timber;

public class TicketGateQrScanFragment extends BaseFragment implements TicketGateQrScanHandlers, PinInputEventHandlers {

    private final String SCREEN_NAME = "QRかざし待ち画面";
    private TicketGateQrScanViewModel _ticketGateQrScanViewModel;
    private SharedViewModel _sharedViewModel;
    private TicketGateQrScanResults _ticketGateQrScanResults;
    private Bundle _args;
    private int demo_count = 0;
    private final TicketSalesApi _ticketSalesApiClient = TicketSalesApiImpl.getInstance();
    private final TerminalDao _terminalDao = LocalDatabase.getInstance().terminalDao();
    private final GpsDao _gpsDao = LocalDatabase.getInstance().gpsDao();
    private final TicketGateSettingsDao _ticketGateSettingsDao = DBManager.getTicketGateSettingsDao();
    private String _serviceInstanceId = null;
    private String _qrCode = null;
    private static ProgressDialog _progressDialog;

    private int error_qrcode = 4018;       // QRコードのフォーマットが異なる
    private int error_network = 9999;      // ネットワークエラー
    private int error_system = 99999;      // システムエラー

    public static TicketGateQrScanFragment newInstance() {
        return new TicketGateQrScanFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final FragmentTicketGateQrScanBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_gate_qr_scan, container, false);

        _ticketGateQrScanViewModel = new ViewModelProvider(this).get(TicketGateQrScanViewModel.class);
        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        _args = getArguments();
//        List<TicketGateRoute> ticketGateRoutes;
//        ticketGateRoutes = (List<TicketGateRoute>) args.getSerializable("ticketRouteIds");
//        _ticketGateQrScanViewModel.setTicketGateRouteList(ticketGateRoutes);

//        _ticketGateQrScanViewModel.registerReceiver(requireActivity());
//        _ticketGateQrScanViewModel.fetch();

        binding.setViewModel(_ticketGateQrScanViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);
        binding.setHandlersPinInput(this);

        QrCodeScanStart();

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    private void navigateToMenuHome(View view) {

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // 自動更新サービスが起動していたら停止
        boolean foundGateCheckService = false;
        Activity mainActivity = (Activity) view.getContext();
        ActivityManager am = (ActivityManager) ((Activity) view.getContext()).getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfoList = am.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceInfoList) {
            //サービスが起動しているかを確認
            if (runningServiceInfo.service.getClassName().equals(PeriodicGateCheckService.class.getName())) {
                foundGateCheckService = true;
                break;
            }
        }
        if (foundGateCheckService) {
            Intent gateService = new Intent(mainActivity.getApplication(), PeriodicGateCheckService.class);
            mainActivity.stopService(gateService);
        }

        // HOME画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_ticketGateQrScanFragment_to_navigation_menu);
    }

    private void navigateToTicketQrScanResults(View view) {

        Activity activity = (Activity) view.getContext();
        if (activity == null) return;
/*
        // テストデータ
        _ticketGateQrScanResults = new TicketGateQrScanResults();
        _ticketGateQrScanResults.qrScanResult = false;
        _ticketGateQrScanResults.adultNumber = 0;
        _ticketGateQrScanResults.childNumber = 1;
        _ticketGateQrScanResults.adultDisabilityNumber = 2;
        _ticketGateQrScanResults.childDisabilityNumber = 3;
        _ticketGateQrScanResults.caregiverNumber = 4;
        _ticketGateQrScanResults.totalPeoples = 10;

        _ticketGateQrScanResults.errorCode = "E2";
        _ticketGateQrScanResults.errorMessage= "チケットの出発時間をご確認ください";
        _ticketGateQrScanResults.errorMessageEnglish = "Please check the departure time on your ticket";
*/
        final Bundle args = new Bundle();
        args.putSerializable("QrScanResults", _ticketGateQrScanResults);
        // QR結果画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_qr_scan_results, args);
    }

    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordClickOperation(number,"パスワード入力画面", false);
        _ticketGateQrScanViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordClickOperation("CLEAR","パスワード入力画面", false);
        _ticketGateQrScanViewModel.correct();
    }

    @Override
    public void onEnter(View view) {
        CommonClickEvent.RecordClickOperation("ENTER","パスワード入力画面", false);

        if (getChildFragmentManager().getFragments().size() >= 1) return;

        if (!_ticketGateQrScanViewModel.enter()) {
            _ticketGateQrScanViewModel.correct();
            new TicketGateQrScanFragment.PinErrorDialogFragment().show(getChildFragmentManager(), null);
        } else {
            _ticketGateQrScanViewModel.correct();
            navigateToMenuHome(view);
        }
    }

    @Override
    public void onCancel(View view) {
        CommonClickEvent.RecordClickOperation("CANCEL","パスワード入力画面", false);
        _ticketGateQrScanViewModel.correct();
        _sharedViewModel.setTopBarView(false);
        _ticketGateQrScanViewModel.isPinInput(false);

        QrCodeScanStart();
    }

    static public class PinErrorDialogFragment extends BaseDialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            // 画面外のタップを無効化
            this.setCancelable(false);

            Timber.e("エラー:パスワードが間違っています。再度、入力してください。");
            final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle("エラー")
                    .setMessage("パスワードが間違っています。\n再度、入力してください。")
                    .setPositiveButton("閉じる", (dialog, which) -> {
                        CommonClickEvent.RecordClickOperation("閉じる", "エラー", false);
                        dialog.dismiss(); });

            return builder.create();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        _qrCode = null;
        if (data != null && result != null) {
            if (result.getContents() != null) {
                // 背面カメラで読み取った値を設定
                _qrCode = result.getContents();
                Timber.i("QRコード読取成功：%s", _qrCode);

                _ticketGateQrScanResults = new TicketGateQrScanResults();
                _sharedViewModel.setTopBarView(false);
                if (null != _qrCode) {
                    // QRコード長さ（36桁）をチェック
                    if (36 != _qrCode.length()) {
                        Timber.e("QRコード異常：%s", _qrCode);
                        _ticketGateQrScanResults.qrScanResult = false;
                        errorMessageSet(error_qrcode);
                        navigateToTicketQrScanResults(getView());
                        return;
                    }
                }

                showConnectingDialog();

                // スキャンの結果を処理
                performNetworkOperation();
            } else {
                Timber.e("result.getContents()：null");
            }
        } else {
            _sharedViewModel.setTopBarView(true);
            _ticketGateQrScanViewModel.isPinInput(true);
            return;
        }

        dismissConnectingDialog();
        navigateToTicketQrScanResults(getView());
    }

    public TicketUpdateDynamicTicketStatus.Request setDynamicTicketStatus() {
        TicketUpdateDynamicTicketStatus.Request request = new TicketUpdateDynamicTicketStatus.Request();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -9);
        Date date = calendar.getTime();
        TicketGateSettingsData ticketGateSettingsData = _ticketGateSettingsDao.getTicketGateSettingsLatest();

        request.reservation_date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        request.trip_id = ticketGateSettingsData != null ? ticketGateSettingsData.trip_id : null;
        request.special_trip_id = null;
        request.tap_method = "one";
        request.event_type = "entrance";
        request.terminal_tid = AppPreference.getMcTermId();
        request.station_no = String.valueOf(AppPreference.getMcCarId());
        request.param_id = null;
        request.telegram_id = UUID.randomUUID().toString();
        request.telegram_datetime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(date);
        Timber.d("%s", request.telegram_datetime);
        request.production = BuildConfig.DEBUG ? "test" : "production";
        request.event_location = setLocation();
        request.transit_type = "hover";
        request.stop_id = ticketGateSettingsData != null ? ticketGateSettingsData.stop_id : null;
        request.route_id = ticketGateSettingsData != null ? ticketGateSettingsData.route_id : null;
        request.account_media = "ticket";

        return request;
    }

    public TicketUpdateDynamicTicketStatus.Location setLocation() {
        TicketUpdateDynamicTicketStatus.Location location = new TicketUpdateDynamicTicketStatus.Location();
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location1 -> {
                        if (location1 != null) {
                            location.latitude = location1.getLatitude();
                            location.longitude = location1.getLongitude();
                            location.sampled_at = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
                            Timber.i("位置情報取得日時:%s 緯度:%s 経度:%s", location.sampled_at, location.latitude, location.longitude);
                        }
                    });
        }

        return location;
    }

/*
    @Override
    public void onDestroy() {
        super.onDestroy();
        _ticketGateQrScanViewModel.unregisterReceiver();
    }
 */

    // センター通信中のダイアログ表示
    private void showConnectingDialog() {

        if (_progressDialog == null) {
            _progressDialog = new ProgressDialog(getContext());
            _progressDialog.setMessage("センター通信中 ・・・ ");
            _progressDialog.setCancelable(false);
            _progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        _progressDialog.show();
        Timber.i("ダイヤログ表示「センター通信中 ・・・ 」");
    }

    // センター通信中のダイアログ閉じる
    private void dismissConnectingDialog() {
        if (_progressDialog != null && _progressDialog.isShowing()) {
            _progressDialog.dismiss();
            Timber.i("ダイヤログ閉じる「センター通信中 ・・・ 」");
        }
    }

    // QRコード読取再開
    private void restartQrCodeReading() {
        Timber.i("QRコード読取再開");
        _sharedViewModel.setTopBarView(false);

        QrCodeScanStart();
    }

    // QRコードスキャン開始
    private void QrCodeScanStart() {

        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setCameraId(0)                                       // デフォルトカメラを使用
                .setOrientationLocked(false)                            // 画面の向き固定（オフ）
                .setBeepEnabled(false)                                  // ビープ音（オフ）
                .setBarcodeImageEnabled(true)                           // 画像処理の最適化（オン）
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)     // QRコード読込対象
                .setPrompt("")
//                .setTimeout(30000)                                      // タイムアウト設定（なし）
                .setCaptureActivity(TicketScannerActivity.class)
                .addExtra("TICKET_ROUTE_IDS", _args)
                .initiateScan();
    }

    private void errorMessageSet(int errorCode) {

        switch (errorCode) {
            case 4008 :
                // 払戻済み
                _ticketGateQrScanResults.errorCode = "E5";
                _ticketGateQrScanResults.errorMessage = "チケットは払戻済みです";
                _ticketGateQrScanResults.errorMessageEnglish = "Ticket has already refunded";
                break;
            case 4013 :
                // 使用済み
                _ticketGateQrScanResults.errorCode = "E3";
                _ticketGateQrScanResults.errorMessage = "チケットは使用済みです";
                _ticketGateQrScanResults.errorMessageEnglish = "Ticket has been used";
                break;
            case 4018 :
                // QRコードのフォーマットが異なる
                _ticketGateQrScanResults.errorCode = "E1";
                _ticketGateQrScanResults.errorMessage = "チケットのQRコードをかざしてください";
                _ticketGateQrScanResults.errorMessageEnglish = "Please hold up the QR code on your ticket";
                break;
            case 4024 :
                // 便が異なる場合
                _ticketGateQrScanResults.errorCode = "E2";
                _ticketGateQrScanResults.errorMessage = "チケットの出発時間をご確認ください";
                _ticketGateQrScanResults.errorMessageEnglish = "Please check the departure time on your ticket";
                break;
            case 9999 :
                // ネットワークエラー
                _ticketGateQrScanResults.errorCode = "E4";
                _ticketGateQrScanResults.errorMessage = "ネットワークエラーのため判定に失敗しました";
                _ticketGateQrScanResults.errorMessageEnglish = "Judgment failed due to network error";
                break;
            default :
                // システムエラー※上記以外のエラー
                _ticketGateQrScanResults.errorCode = "E9";
                _ticketGateQrScanResults.errorMessage = "システムエラーにより判定に失敗しました";
                _ticketGateQrScanResults.errorMessageEnglish = "Judgment failed due to system error";
                break;
        }
    }

    private void performNetworkOperation() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // ネットワーク操作をここに実装
                try {
                    // リクエストデータ設定
                    TicketUpdateDynamicTicketStatus.Request request = setDynamicTicketStatus();

                    TerminalData terminalData = _terminalDao.getTerminal();
                    if (terminalData != null)
                        _serviceInstanceId = terminalData.service_instance_abt;
                    TicketUpdateDynamicTicketStatus.Response response = _ticketSalesApiClient.TicketUpdateDynamicTicketStatus(_serviceInstanceId, _qrCode, request);

                    // センターへの問い合わせ結果を格納
                    _ticketGateQrScanResults.qrScanResult = true;

                    _ticketGateQrScanResults.errorCode = "";
                    _ticketGateQrScanResults.errorMessage = "";
                    _ticketGateQrScanResults.errorMessageEnglish = "";

                    if (response != null && response.data != null && response.data.peoples != null) {
                        for (TicketPeople people : response.data.peoples) {
                            if (people == null) continue;
                            switch (people.category_type) {
                                case "unknown":
                                    _ticketGateQrScanResults.adultNumber = people.num;
                                    break;
                                case "child":
                                    _ticketGateQrScanResults.childNumber = people.num;
                                    break;
                                case "disabled":
                                    _ticketGateQrScanResults.adultDisabilityNumber = people.num;
                                    break;
                                case "child_disabled":
                                    _ticketGateQrScanResults.childDisabilityNumber = people.num;
                                    break;
                                case "carer":
                                    _ticketGateQrScanResults.caregiverNumber = people.num;
                                    break;
                                case "baby":
                                    _ticketGateQrScanResults.babyNumber = people.num;
                                    break;
                            }
                        }
                        _ticketGateQrScanResults.totalPeoples = response.data.total_num;
                    }

                } catch (TicketSalesStatusException e) {
                    Timber.e(e);
                    _ticketGateQrScanResults.qrScanResult = false;
                    int errorCode = ((TicketSalesStatusException) e).getCode();
                    errorMessageSet(errorCode);
                } catch (HttpStatusException e) {
                    Timber.e(e);
                    _ticketGateQrScanResults.qrScanResult = false;
                    errorMessageSet(error_system);
                } catch (Exception e) {
                    Timber.e(e);
                    _ticketGateQrScanResults.qrScanResult = false;
                    errorMessageSet(error_network);
                }
            }
        });
        thread.start();

        try {
            thread.join();
        } catch (Exception e) {
            Timber.e(e);
            _ticketGateQrScanResults.qrScanResult = false;
            errorMessageSet(error_system);
        }
    }
}