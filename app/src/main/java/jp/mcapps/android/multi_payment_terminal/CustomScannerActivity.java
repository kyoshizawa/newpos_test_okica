package jp.mcapps.android.multi_payment_terminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.ViewfinderView;

import java.lang.reflect.Field;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.QRLayouts;
import jp.mcapps.android.multi_payment_terminal.databinding.ActivityCustomScannerBinding;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import timber.log.Timber;

/**
 * Custom Scannner Activity extending from Activity to display a custom layout form scanner view.
 */
public class CustomScannerActivity extends AppCompatActivity {
    private CustomScannerViewModel _customScannerViewModel;
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private CommonHeadViewModel _commonHeadViewModel;
    private ViewfinderView viewfinderView;
    private String barcodeString = "";  // バーコードで読み取った文字列

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        final ActivityCustomScannerBinding binding = DataBindingUtil.setContentView(
                this, R.layout.activity_custom_scanner);

        _customScannerViewModel = new ViewModelProvider(this).get(CustomScannerViewModel.class);

        binding.setLifecycleOwner(this);
        binding.setActivity(this);
        binding.setLayout(intent.getIntExtra(QRLayouts.KEY, QRLayouts.PAYMENT));
        binding.setViewModel(_customScannerViewModel);

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);
        binding.setCommonHeadViewModel(_commonHeadViewModel);
        getLifecycle().addObserver(_commonHeadViewModel);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);

        // Viewにフォーカスを設定する（これをやらないと外付けバーコードリーダーで改行入力時、中止ボタンを押した動作になる）
        barcodeScannerView.setFocusableInTouchMode(true);
        barcodeScannerView.requestFocus();

        if (hasFlash() && _customScannerViewModel.useLight().getValue()) {
            barcodeScannerView.setTorchOn();
        }

        viewfinderView = findViewById(R.id.zxing_viewfinder_view);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            findViewById(R.id.toolbar_main_back).setOnClickListener(v -> { super.onBackPressed(); });

            actionBar.setDisplayShowTitleEnabled(false);
            applyActionBar();
        }

        hideNavigationBar();

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.setShowMissingCameraPermissionDialog(false);
        capture.decode();

        viewfinderView.setMaskColor(Color.argb(255, 255, 255, 255));

        for (int id: _customScannerViewModel.firstFlowIds) {
            ImageView view = createLogo(id, 230, 100);
            binding.constraintCustomScanner.addView(view);
            binding.flowWithoutAliplus.addView(view);
        }

        int cnt = _customScannerViewModel.secondFlowIds.size();
        for (int i = 0; i < cnt; i++) {
            int id = _customScannerViewModel.secondFlowIds.pop();
            ImageView view = (i == cnt - 1)
                    ? createLogo(id, 460, 200)
                    : createLogo(id, 230, 100);
            binding.constraintCustomScanner.addView(view);
            binding.flowInAliplus.addView(view);
        }
        if (cnt == 1) {
            binding.flowInAliplus.setWrapMode(Flow.WRAP_ALIGNED);
        }

        try {
            Field field = ViewfinderView.class.getDeclaredField("SCANNER_ALPHA");
            field.setAccessible(true);
            field.set(viewfinderView, new int[] {0});
        } catch (Exception e) {
            e.printStackTrace();
        }
        changeLaserVisibility(true);

        barcodeString = "";

        //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
        // ブロードキャストレシーバ登録
        LocalBroadcastManager.getInstance(this).registerReceiver(exitReceiver,
                new IntentFilter("CLOSE_QR_SCANNER"));
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

    }

    //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    private BroadcastReceiver exitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish(); // アクティビティを終了
        }
    };
    //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
        LocalBroadcastManager.getInstance(this).unregisterReceiver(exitReceiver);
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyEvent.getDevice() != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_TAB:
                    Timber.i("バーコードで読み取った文字列:%s", barcodeString);
                    // 外付けリーダーで読み取った値をエクストラデータで渡す
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("barcodeString", barcodeString);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    break;
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    // 大文字の場合、シフトキーも来るため破棄
                    break;
                case KeyEvent.KEYCODE_UNKNOWN:
                    // 連続で読み取っていると入る場合があったので破棄
                    break;
                default:
                    char pressedChar = (char) keyEvent.getUnicodeChar(keyEvent.getMetaState());
                    String pressedKey = Character.toString(pressedChar);
                    barcodeString += pressedKey;
                    break;
            }
        }
//        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
        return true;
    }

    public void changeLaserVisibility(boolean visible) {
        viewfinderView.setLaserVisibility(visible);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideNavigationBar();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Timber.v("onConfigurationChanged");
        // AndroidManifest.xmlでCustomScannerActivityのandroid:configChangesを設定し
        // 何も処理しないことで、外付けバーコードリーダーの接続/切断を行った際、システムがアクティビティを再作成することを防止
        super.onConfigurationChanged(newConfig);
    }

    public void onCancelClick(View view) {
        CommonClickEvent.RecordClickOperation("中止", "QR読取画面", false);
        super.onBackPressed();
    }

    public void onSwitchLight(View view) {
        final boolean b = !_customScannerViewModel.useLight().getValue();
        _customScannerViewModel.useLight(b);

        if (b) barcodeScannerView.setTorchOn();
        else barcodeScannerView.setTorchOff();
    }

    private void applyActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {

            findViewById(R.id.toolbar_main_demo_mode).setVisibility(
                    AppPreference.isDemoMode() ? View.VISIBLE : View.GONE);

            findViewById(R.id.toolbar_main_manual_mode).setVisibility(
                    IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) ? View.VISIBLE : View.GONE);

            if (AppPreference.isDemoMode()) {
                actionBar.setBackgroundDrawable(
                        getResources().getDrawable(R.color.design_default_color_error, null));
                actionBar.setDisplayShowTitleEnabled(true);
            } else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
                actionBar.setBackgroundDrawable(
                        getResources().getDrawable(R.color.orange_500, null));
                actionBar.setDisplayShowTitleEnabled(false);
            } else {
                actionBar.setBackgroundDrawable(
                        getResources().getDrawable(R.color.primary, null));
                actionBar.setDisplayShowTitleEnabled(false);
            }
        }
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private ImageView createLogo(@DrawableRes int id, int width, int height) {
        ImageView view = new ImageView(this);
        view.setImageResource(id);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(width, height);
        view.setLayoutParams(params);
        view.setId(View.generateViewId());
        return view;
    }
}
