package jp.mcapps.android.multi_payment_terminal.ui.setup;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.CustomScannerActivity;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.ViewModelFactory;
import jp.mcapps.android.multi_payment_terminal.data.FirmWareInfo;
import jp.mcapps.android.multi_payment_terminal.data.QRLayouts;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentIfboxSetupBinding;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.BaseDialogFragment;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes;
import timber.log.Timber;

import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.*;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.DEVICE_CONNECTING;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.DEVICE_MENU;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.FIRMWARE_SELECT;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.PARAMETER_MENU;
import static jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel.DisplayTypes.TOP_MENU;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class IFBoxSetupFragment extends Fragment implements IFBoxSetupEventHandlers {
    private final String SCREEN_NAME = "IM-A820";

    private FragmentIfboxSetupBinding _binding;
    private IFBoxSetupViewModel _viewModel;
    private SharedViewModel _sharedViewModel;
    private AccessPointAdapter _accessPointAdapter;
    private List<Disposable> disposables = new ArrayList<>();
    private Runnable disposeAction = () -> {
        for (Disposable disposable : disposables) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        }

        disposables.clear();
    };

    public static IFBoxSetupFragment newInstance() {
        return new IFBoxSetupFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
        Timber.i("[FUTABA-D] class IFBoxSetupFragment");
        //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ifbox_setup, container, false);

        final ViewModelFactory factory = MainApplication.getViewModelFactory();

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        _viewModel = new ViewModelProvider(this, factory).get(IFBoxSetupViewModel.class);
        _binding.setLifecycleOwner(getViewLifecycleOwner());
        _binding.setHandlers(this);
        _binding.setViewModel(_viewModel);

        _viewModel.getAccessPoints().observe(getViewLifecycleOwner(), (list -> {
            _accessPointAdapter = new AccessPointAdapter(list, getViewLifecycleOwner());
            _binding.recyclerViewAccessPoint.setAdapter(_accessPointAdapter);
        }));

        _viewModel.wifiScan();

        _sharedViewModel.setBackAction(() -> {
            final DisplayTypes type = _viewModel.getDisplayType().getValue();


            switch (type) {
                case TOP_MENU:
                    NavigationWrapper.popBackStack(this);
                    _sharedViewModel.setBackAction(null);
                    disposeAction.run();
                    _viewModel.cleanup();
                    break;
                case FIRMWARE_SELECT:
                    CommonClickEvent.RecordClickOperation("戻る", "ファームウェア設定画面", false);
                    if (AppPreference.getIFBoxOTAInfo() == null) {
                        showCancelConfirmDialog();
                    } else {
                        _viewModel.setDisplayType(TOP_MENU);
                        disposeAction.run();
                    }
                    break;
                case FIRMWARE_UPLOADING:
                    CommonClickEvent.RecordClickOperation("戻る", "ファームウェア書き込み画面", false);
                    if (_viewModel.getUploadStatus().getValue() >= UploadStatus.VERSION_CHECKED || _viewModel.isExecuteFailure().getValue()) {
                        _viewModel.setDisplayType(TOP_MENU);
                        disposeAction.run();
                    } else {
                        showCancelConfirmDialog();
                    }
                    break;
                case DEVICE_CONNECTING:
                    CommonClickEvent.RecordClickOperation("戻る", "ファームウェア情報取得中", false);
                    if (_viewModel.isExecuteFailure().getValue()) {
                        _viewModel.setDisplayType(DEVICE_MENU);
                        disposeAction.run();
                    } else {
                        showCancelConfirmDialog(DEVICE_MENU);
                    }
                    break;
                case PARAMETER_MENU:
                case DEVICE_MENU:
                default:
                    if (type == DEVICE_MENU) {
                        CommonClickEvent.RecordClickOperation("戻る", "デバイス設定画面", false);
                    } else if (type == PARAMETER_MENU) {
                        CommonClickEvent.RecordClickOperation("戻る", "パラメータ設定画面", false);
                    } else {
                        CommonClickEvent.RecordClickOperation("戻る", "?", false);
                    }
                    _viewModel.setDisplayType(TOP_MENU);
                    disposeAction.run();
                    break;
            }
        });

        return _binding.getRoot();
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
    public void onResume() {
        super.onResume();
        _viewModel.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        _viewModel.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onConnectionClick(View view) {
        CommonClickEvent.RecordClickOperation("接続", "デバイス設定画面", false);
        ScanResult item = _accessPointAdapter.getCheckedItem();
        if (item != null) {
            getFirmwareInfo(item.SSID);
        }
    }

    @Override
    public void onQRClick(View view) {
        CommonClickEvent.RecordClickOperation("QR読取", "デバイス設定画面", false);

        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("");
        integrator.setCaptureActivity(CustomScannerActivity.class)
                .addExtra(QRLayouts.KEY, QRLayouts.IFBOX_SETUP)
                .setBeepEnabled(true)
                .initiateScan();
    }

    @Override
    public void onFirmwareClick() {
        CommonClickEvent.RecordClickOperation("ファームウェア設定「詳細」", "IM-A820設定画面", false);
//        if (AppPreference.getIFBoxOTAInfo() == null || _viewModel.getIFBoxUrl() == null) {
//        if (_viewModel.getIFBoxUrl() == null) {
//            new SetupAlertDialog().show(getChildFragmentManager(), null);
//        } else {
//            final Disposable disposable = _viewModel.getFirmwares().subscribe((firmwares, error) -> {
//                if (error != null) {
//                    Toast.makeText(requireContext(), "ファームウェア情報取得失敗", Toast.LENGTH_SHORT).show();
//                } else {
//                    final FirmWaresAdapter adapter = new FirmWaresAdapter(firmwares);
//                    _binding.recyclerViewFirmwares.setAdapter(adapter);
//
//                    adapter.setItemClickListener(firmWareInfo -> {
//                        if (AppPreference.isIFBoxSetupFinished()) {
//                            ConfirmDialog.newInstance("【書換確認】", confirmMessage(firmWareInfo), () -> {
//                                CommonClickEvent.RecordClickOperation("はい", "書換確認", false);
//                                firmwareUpdate(firmWareInfo);
//                            },() ->{
//                                CommonClickEvent.RecordClickOperation("いいえ", "書換確認", false);
//                            }).show(getChildFragmentManager(), null);
//                        } else {
//                            firmwareUpdate(firmWareInfo);
//                        }
//                    });
//
//                    _viewModel.setDisplayType(FIRMWARE_SELECT);
//                }
//            });
//        }
    }

    @Override
    public void onConfigurationClick() {
        CommonClickEvent.RecordClickOperation("パラメータ設定「詳細」", "IM-A820設定画面", false);
//        if (AppPreference.getIFBoxOTAInfo() == null || _viewModel.getIFBoxUrl() == null) {
//        if (_viewModel.getIFBoxUrl() == null) {
//            new SetupAlertDialog().show(getChildFragmentManager(), null);
//        } else {
//            _viewModel.setDisplayType(PARAMETER_MENU);
//            WebView webView = requireActivity().findViewById(R.id.web_view);
//            webView.loadUrl(_viewModel.getIFBoxUrl() + "/setting/v1");
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        final String contents = result.getContents();

        if (contents != null) {
            final Pattern ssidPattern = Pattern.compile("S:.*;");
            final Matcher matcher = ssidPattern.matcher(contents);

            final String ssid = matcher.find()
                    ? matcher.group().replace("S:", "").split(";")[0]
                    : null;

            if (ssid != null) {
                getFirmwareInfo(ssid);
            } else {
                Toast.makeText(requireContext(), "有効なコードではありません", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onWifiScanClick(View view) {
        CommonClickEvent.RecordClickOperation("検索", "デバイス設定画面", false);
        _viewModel.wifiScan();
    }

    private void getFirmwareInfo(String ssid) {
        Timber.i("ファームウェア情報取得開始");
        _viewModel.setDisplayType(DEVICE_CONNECTING);
        _viewModel.isExecuteFailure(false);

        final Disposable disposable = _viewModel.getFirmwares().subscribe((firmwares, error) -> {
            if (error != null) {
                _viewModel.isExecuteFailure(true);
                Timber.e("ファームウェア情報取得失敗");
//                Toast.makeText(requireContext(), "ファームウェア情報取得失敗", Toast.LENGTH_SHORT).show();
            } else {
                final FirmWaresAdapter adapter = new FirmWaresAdapter(firmwares);
                _binding.recyclerViewFirmwares.setAdapter(adapter);
                adapter.setItemClickListener(this::firmwareUpdate);

                Timber.i("ファームウェア情報取得成功");
                _viewModel.setConnectionStatus(ConnectionStatus.FIEMWARE_INFO_CHECKED);
                connectWifiP2p(ssid);
            }
        });

        disposables.add(disposable);
    }

    private void connectWifiP2p(String ssid) {
        Timber.i("接続確認開始");
        _viewModel.isExecuteFailure(false);

//        disposables.add(_viewModel.removeWifiP2pGroup()
//                .subscribeOn(Schedulers.io())
//                .doFinally(() -> {
//                    disposables.add(_viewModel.connectWifiP2p(ssid)
//                            .subscribe((success, error) -> {
//                                if (error != null) {
//                                    Timber.e("接続確認失敗");
//                                    _viewModel.isExecuteFailure(true);
////                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
//                                }
//                            }));
//                })
//                .subscribe(() -> {}, e -> {}));
    }

    private void firmwareUpdate(FirmWareInfo firmWareInfo) {
        _viewModel.isExecuteFailure(false);

        final Disposable disposable = _viewModel.firmwareUpdate(firmWareInfo)
                .subscribe(() -> {
                    Toast.makeText(requireContext(), "設定が完了しました", Toast.LENGTH_SHORT).show();
                }, error -> {
                    _viewModel.isExecuteFailure(true);
//            Toast.makeText(requireContext(), "ファームウェアの更新に失敗しました", Toast.LENGTH_SHORT).show();
                });

        disposables.add(disposable);
    }

    private String confirmMessage(FirmWareInfo selected) {
        final FirmWareInfo current = AppPreference.getIFBoxOTAInfo();
        Timber.i("書換確認:現在のファームウェアは「%s %s」です。「%s %s」に書き換えを行ってもよろしいですか？", current.modelName, current.versionName, selected.modelName, selected.versionName);
        return String.format("現在のファームウェアは\n" +
                "「%s %s」です。\n" +
                "「%s %s」に書き換えを行ってもよろしいですか？",
                current.modelName, current.versionName,
                selected.modelName, selected.versionName);
    }

    private void showCancelConfirmDialog() {
        showCancelConfirmDialog(TOP_MENU);
    }

    private void showCancelConfirmDialog(DisplayTypes display) {
        Timber.i("中断確認:ファームウェア設定を中断すると接続設定は完了しません。中断してよろしいですか？");
        final String message =
                "ファームウェア設定を中断すると接続設定は完了しません。\n中断してよろしいですか？";

        ConfirmDialog.newInstance("【中断確認】",message, () -> {
            CommonClickEvent.RecordClickOperation("はい", "中断確認", false);
            _viewModel.setDisplayType(display);
            disposables.clear();
        },() ->{
            CommonClickEvent.RecordClickOperation("いいえ", "中断確認", false);
        }).show(getChildFragmentManager(), null);
    }

    static public class SetupAlertDialog extends BaseDialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            // 画面外のタップを無効化
            this.setCancelable(false);

            final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setMessage("接続設定が完了していません")
                    .setPositiveButton("閉じる", (dialog, which) -> { dialog.dismiss(); });

            return builder.create();
        }
    }
}