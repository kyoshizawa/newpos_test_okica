package jp.mcapps.android.multi_payment_terminal;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.ViewfinderView;

import java.lang.reflect.Field;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.QRLayouts;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.databinding.ActivityTicketQrScannerBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketGateQrScanBinding;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketGateQrScanViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketGateRoute;
import timber.log.Timber;

/**
 * Custom Scannner Activity extending from Activity to display a custom layout form scanner view.
 */
public class TicketScannerActivity extends AppCompatActivity {
    private TicketScannerViewModel _ticketScannerViewModel;
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private ViewfinderView viewfinderView;
    private String barcodeString = "";  // バーコードで読み取った文字列
    private int _tapCount = 0;
    private Bundle _savedInstanceState;
    private final int REQUEST_CAMERA_PERMISSION = 100;
    private List<TicketGateRoute> _ticketGateRoutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        Bundle args = (Bundle) bundle.getParcelable("TICKET_ROUTE_IDS");
        _ticketGateRoutes = (List<TicketGateRoute>) args.getSerializable("ticketRouteIds");
        _savedInstanceState = savedInstanceState;

        final ActivityTicketQrScannerBinding binding = DataBindingUtil.setContentView(
                this, R.layout.activity_ticket_qr_scanner);

        _ticketScannerViewModel = new ViewModelProvider(this).get(TicketScannerViewModel.class);
        _ticketScannerViewModel.setTicketGateRouteList((List<TicketGateRoute>) args.getSerializable("ticketRouteIds"));

        binding.setViewModel(_ticketScannerViewModel);
        binding.setLifecycleOwner(this);
        binding.setActivity(this);
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);

        barcodeScannerView.setFocusableInTouchMode(true);
        barcodeScannerView.requestFocus();

        checkCameraPermission();

        initializeCaptureManager();

        _ticketScannerViewModel.useLight(AppPreference.ticketGateScanUseLight());
        if (hasFlash() && Boolean.TRUE.equals(_ticketScannerViewModel.useLight().getValue())) {
            barcodeScannerView.setTorchOn();
        }

        viewfinderView = findViewById(R.id.zxing_viewfinder_view);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            findViewById(R.id.toolbar_main_back).setOnClickListener(v -> super.onBackPressed());

            actionBar.setDisplayShowTitleEnabled(false);
            applyActionBar();
        }

        hideNavigationBar();

        viewfinderView.setMaskColor(Color.argb(128, 128, 128, 128));

        try {
            Field field = ViewfinderView.class.getDeclaredField("SCANNER_ALPHA");
            field.setAccessible(true);
            field.set(viewfinderView, new int[]{0});
        } catch (Exception e) {
            Timber.e(e, "Failed to set SCANNER_ALPHA field.");
        }
        changeLaserVisibility(true);

//        _ticketScannerViewModel.setCapture(capture);
//        _ticketScannerViewModel.setTicketGateRouteList(_ticketGateRoutes);
        _ticketScannerViewModel.registerReceiver(this);
        _ticketScannerViewModel.fetch();

        barcodeString = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (capture != null) {
            capture.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
        _ticketScannerViewModel.unregisterReceiver();
    }

    private void initializeCaptureManager() {
        if (capture != null) {
            capture.onDestroy();
        }
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), _savedInstanceState);
        capture.setShowMissingCameraPermissionDialog(false);
        capture.decode();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    public void changeLaserVisibility(boolean visible) {
        viewfinderView.setLaserVisibility(visible);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.i("Camera permission granted.");
                initializeCaptureManager();
            } else {
                Timber.e("Camera permission denied.");
            }
        }
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
        super.onBackPressed();
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
        final boolean b = !AppPreference.ticketGateScanUseLight();
        AppPreference.ticketGateScanUseLight(b);
        _ticketScannerViewModel.useLight(b);

        if (b) {
            barcodeScannerView.setTorchOn();
            CommonClickEvent.RecordClickOperation("ライト点灯", "QR読取画面", false);
        } else {
            barcodeScannerView.setTorchOff();
            CommonClickEvent.RecordClickOperation("ライト消灯", "QR読取画面", false);
        }
    }

    private void applyActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {

            findViewById(R.id.toolbar_main_demo_mode).setVisibility(
                    AppPreference.isDemoMode() ? View.VISIBLE : View.GONE);

            if (AppPreference.isDemoMode()) {
                actionBar.setBackgroundDrawable(
                        getResources().getDrawable(R.color.design_default_color_error, null));
                actionBar.setDisplayShowTitleEnabled(true);
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

    public void popBackStackToTicketGateSettings(View view) {
        CommonClickEvent.RecordClickOperation("タップ", "設置場所名", true);

        if (_tapCount == 0) countDownTimerStart();

        // タップ回数
        _tapCount += 1;
        if (_tapCount < 5) return;
        _tapCount = 0;

        // 改札設定画面に戻す
        super.onBackPressed();
    }

    private void countDownTimerStart() {

        long waitingTimeMillisecond = 3000;
        CountDownTimer countDownTimer = new CountDownTimer(waitingTimeMillisecond, 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                _tapCount = 0;
            }
        }.start();
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                initializeCaptureManager();
            }
        } else {
            initializeCaptureManager();
        }
    }
}
