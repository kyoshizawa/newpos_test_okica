package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.CustomScannerActivity;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.ActionBarColors;
import jp.mcapps.android.multi_payment_terminal.data.QRLayouts;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentPosQrBinding;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import timber.log.Timber;

public class PosQRFragment extends PosBaseFragment implements PosQrEventHandlers {
    private final String SCREEN_NAME = "QR読み取り";
    private static final int MESSAGE_DISPLAY_TIME_MS = 1000;
    private PosQRViewModel _posQrViewModel;
    private SharedViewModel _sharedViewModel;
    private CommonHeadViewModel _commonHeadViewModel;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    private final LocalDatabase _db = LocalDatabase.getInstance();

    public static PosQRFragment newInstance() {
        return new PosQRFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        final FragmentPosQrBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pos_qr, container, false);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        _posQrViewModel = new ViewModelProvider(this).get(PosQRViewModel.class);

        binding.setViewModel(_posQrViewModel);
        binding.setSharedViewModel(_sharedViewModel);
        binding.setHandlers(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);
        binding.setCommonHeadViewModel(_commonHeadViewModel);
        getLifecycle().addObserver(_commonHeadViewModel);

        initiateScan();

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
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        String qrcode = null;
        if(data != null) {
            if(result.getContents() != null) {
                // 背面カメラで読み取った値を設定
                qrcode = result.getContents();
                Timber.i("スキャンで読み取った文字列：%s", qrcode);
            } else if(data.getStringExtra("barcodeString") != null) {
                // 外付けバーコードリーダーで読み取った値を設定
                qrcode = data.getStringExtra("barcodeString");
            }
        }
        final String contents = qrcode;

        if (contents != null) {
            _posQrViewModel.getResult().observe(getViewLifecycleOwner(), isOk -> {
                if (isOk) {
                    _handler.removeCallbacksAndMessages(null);
                    _handler.postDelayed(() -> {
                        _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                        // カート画面に遷移
                        NavigationWrapper.navigate(this, R.id.action_navigation_pos_qr_to_cart_confirm_fragment);
                    }, MESSAGE_DISPLAY_TIME_MS);
                }
            });
            new Thread(() -> {
                _posQrViewModel.setProduct(contents, _db.productDao(), _db.cartDao());
            }).start();
        } else {
            // カメラ画面で戻るボタンが押されたとき メニュー画面に戻す
            requireActivity().runOnUiThread(() -> {
                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                NavigationWrapper.popBackStack(this);
            });
        }
    }

    @Override
    public void onReload(View view) {
        CommonClickEvent.RecordClickOperation("再読み込み", "QR読取失敗確認画面", true);
        initiateScan();
    }

    private void initiateScan() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("");
        integrator.setCaptureActivity(CustomScannerActivity.class)
                .addExtra(QRLayouts.KEY, QRLayouts.POS)
                .setBeepEnabled(false)
                .initiateScan();
    }

    @Override
    public void onCancelClick(View view) {
        CommonClickEvent.RecordClickOperation("中止", "QR読取失敗確認画面", true);
        _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
        NavigationWrapper.navigateUp(this);
    }
}
