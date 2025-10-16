package jp.mcapps.android.multi_payment_terminal.ui.setup;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.CustomScannerActivity;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.ViewModelFactory;
import jp.mcapps.android.multi_payment_terminal.data.QRLayouts;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTabletLinkSetupBinding;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;
import timber.log.Timber;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TabletLinkSetupFragment extends Fragment implements TabletLinkSetupEventHandlers {
    private final String SCREEN_NAME = "タブレット連動";
    private final int REQUEST_CODE_WIFI_SETTINGS = 1001;

    private String address = null;
    private String ssid = null;
    private int port = -1;

    private FragmentTabletLinkSetupBinding _binding;
    private TabletLinkSetupViewModel _viewModel;
    private SharedViewModel _sharedViewModel;
    private List<Disposable> disposables = new ArrayList<>();

    public static TabletLinkSetupFragment newInstance() {
        return new TabletLinkSetupFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_tablet_link_setup, container, false);

        final ViewModelFactory factory = MainApplication.getViewModelFactory();

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        _viewModel = new ViewModelProvider(this, factory).get(TabletLinkSetupViewModel.class);
        _binding.setLifecycleOwner(getViewLifecycleOwner());
        _binding.setHandlers(this);
        _binding.setViewModel(_viewModel);

        openQRReader();

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
    public void onDestroy() {
        super.onDestroy();
        for (Disposable d : disposables) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WIFI_SETTINGS) {
            _viewModel.isExecuteFailure(false);
            _viewModel.setStatus(TabletLinkSetupViewModel.Status.STARTED);

            connectP2p();
            return;
        }

        final IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        final String contents = result.getContents();
        _viewModel.setStatus(TabletLinkSetupViewModel.Status.STARTED);

        if (contents != null) {
            final String[] params = contents.split(",");

            for (String p : params) {
                String[] kv = p.trim().split("=");
                if (kv.length != 2) {
                    continue;
                }

                String key = kv[0];
                String value = kv[1];

                switch (key) {
                    case "address":
                        Timber.i("タブレット連動: キッティング種別 deviceAddress");

                        final Pattern ptn = Pattern.compile("^(([0-9]|[A-F]|[a-f]){2}:){5}([0-9]|[A-F]|[a-f]){2}$");
                        final Matcher matcher = ptn.matcher(value);

                        if (matcher.find()) {
                            address = value;
                        } else {
                            Timber.e("タブレット連動: デバイスアドレスの形式が不正です %s", value);
                        }
                        break;
                    case "ssid":
                        Timber.i("タブレット連動: キッティング種別 SSID");
                        ssid = value;
                        break;
                    case "port":
                        try {
                            port = Integer.parseInt(value);
                        } catch (Exception e) {
                            Timber.e("タブレット連動: ポートの形式が不正です %s", value);
                        }
                        break;
                    default:
                        break;
                }
            }
            connectP2p();
        } else {
            NavigationWrapper.popBackStack(this);
        }
    }

    @SuppressLint("CheckResult")
    private void connectP2p() {
        if (address != null && port > 0) {
            _viewModel.connectP2p(address, port)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(disposables::add)
                    .subscribe(() -> {
                    }, e -> {
                        if (e instanceof TabletLinkSetupViewModel.WifiNotEnabledException) showWifiEnableDialog();
                        _viewModel.isExecuteFailure(true);
                        Timber.e(e);
                    });
        } else if (ssid != null && port > 0) {
            _viewModel.connectP2pBySSID(ssid, port)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .doOnSubscribe(disposables::add)
                    .subscribe(() -> {
                    }, e -> {
                        if (e instanceof TabletLinkSetupViewModel.WifiNotEnabledException) showWifiEnableDialog();
                        _viewModel.isExecuteFailure(true);
                        Timber.e(e);
                    });
        } else {
            Toast.makeText(requireContext(), "有効なコードではありません", Toast.LENGTH_SHORT).show();
            NavigationWrapper.popBackStack(this);
        }
    }

    private void showWifiEnableDialog() {
        CommonClickEvent.RecordClickOperation("Wi-Fi有効化", "Wi-Fi設定確認", false);
        ConfirmDialog.newInstance(
                "【Wi-Fi設定確認】",
                "Wi-Fiが無効になっています。Wi-Fiを有効にしますか？",
                () -> {
                    CommonClickEvent.RecordClickOperation("はい", "Wi-Fi設定確認", false);
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_WIFI_SETTINGS);
                },
                () -> {
                    CommonClickEvent.RecordClickOperation("いいえ", "Wi-Fi設定確認", false);
                }
        ).show(getChildFragmentManager(), null);
    }

    private void openQRReader() {
        final IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("");
        integrator.setCaptureActivity(CustomScannerActivity.class)
                .addExtra(QRLayouts.KEY, QRLayouts.NETWORK_SETUP)
                .setBeepEnabled(true)
                .initiateScan();
    }
}