package jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentInstallationOkicaBinding;
import jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal.InstallationOkicaViewModel.States;
import timber.log.Timber;

public class InstallationOkicaFragment extends BaseFragment {
    private final String SCREEN_NAME = "OKICA設置";

    public static InstallationOkicaFragment newInstance() {
        return new InstallationOkicaFragment();
    }

    private final MainApplication _app = MainApplication.getInstance();
    private InstallationOkicaViewModel _installationOkicaViewModel;
    private SharedViewModel _sharedViewModel;
    private FragmentInstallationOkicaBinding _binding = null;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_installation_okica, container, false);

        _installationOkicaViewModel =
                new ViewModelProvider(this).get(InstallationOkicaViewModel.class);

        _binding.setLifecycleOwner(getViewLifecycleOwner());

        _binding.setViewModel(_installationOkicaViewModel);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _installationOkicaViewModel.requestInstallation();
        _installationOkicaViewModel.start();

        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        _installationOkicaViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == States.Requesting) {
                _sharedViewModel.setLoading(true);
            } else {
                _sharedViewModel.setLoading(false);
            }

            switch (state) {
                case InstallRequestSuccess:
                    try {
                        final String url = _installationOkicaViewModel.getUrl();

                        final BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap bitmap = barcodeEncoder.encodeBitmap(url, BarcodeFormat.QR_CODE, 200, 200);
                        _binding.okicaInstallationQr.setImageBitmap(bitmap);
                        Timber.i("【設置要求】QRコードが表示されました。");
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    break;
                case Finished:
                    Timber.i("【設置完了】設置成功しました。");
                    Toast.makeText(_app, "【設置完了】\n" +
                            "設置成功しました。", Toast.LENGTH_LONG).show();
                    NavigationWrapper.popBackStack(this);
                    break;
                case InstallRequestFailure:
                    Timber.e("【設置失敗】管理者に連絡してください。");
                    Toast.makeText(_app, "【設置失敗】\n" +
                            "管理者に連絡してください。", Toast.LENGTH_LONG).show();
                    NavigationWrapper.popBackStack(this);
                    break;
                case TerminalInfoRequestFailure:
                    Timber.e("【端末情報取得失敗】管理者に連絡してください。");
                    Toast.makeText(_app, "【端末情報取得失敗】\n" +
                            "管理者に連絡してください。", Toast.LENGTH_LONG).show();
                    NavigationWrapper.popBackStack(this);
                    break;
                case ICMasterRequestFailure:
                    Timber.e("【IC運用マスタ取得失敗】管理者に連絡してください。");
                    Toast.makeText(_app, "【IC運用マスタ取得失敗】\n" +
                            "管理者に連絡してください。", Toast.LENGTH_LONG).show();
                    NavigationWrapper.popBackStack(this);
                    break;
                case AccessKeyRequestFailure:
                    Timber.e("【アクセスキー取得失敗】管理者に連絡してください。");
                    Toast.makeText(_app, "【アクセスキー取得失敗】\n" +
                            "管理者に連絡してください。", Toast.LENGTH_LONG).show();
                    NavigationWrapper.popBackStack(this);
                    break;
                case NegaRequestFailure:
                    Timber.e("【ネガ取得失敗】管理者に連絡してください。");
                    Toast.makeText(_app, "【ネガ取得失敗】\n" +
                            "管理者に連絡してください。", Toast.LENGTH_LONG).show();
                    NavigationWrapper.popBackStack(this);
                    break;
                case Expired:
                    Timber.e("【認証期限切れ】管理者に連絡してください。");
                    Toast.makeText(_app, "【認証期限切れ】\n" +
                            "管理者に連絡してください。", Toast.LENGTH_LONG).show();
                    NavigationWrapper.popBackStack(this);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        _installationOkicaViewModel.stop();
        _sharedViewModel.setLoading(false);

        super.onDestroyView();
    }
}
