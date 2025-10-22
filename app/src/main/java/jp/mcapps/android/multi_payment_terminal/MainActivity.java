package jp.mcapps.android.multi_payment_terminal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.DialogPreference;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.databinding.ActivityMainBinding;
import jp.mcapps.android.multi_payment_terminal.devices.DiscoverDevice;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
//import jp.mcapps.android.multi_payment_terminal.httpserver.events.EventBroker;
import jp.mcapps.android.multi_payment_terminal.model.DiscountInfo;
import jp.mcapps.android.multi_payment_terminal.model.DiscountMenuInfo;
import jp.mcapps.android.multi_payment_terminal.service.GetGpsService;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
//import jp.mcapps.android.multi_payment_terminal.service.PeriodicErrorCheckService;
//import jp.mcapps.android.multi_payment_terminal.service.WifiP2pService;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuDiscountFragment;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuHomeFragment;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private SharedViewModel _sharedViewModel;
    private ActivityMainBinding _binding;
    private List<Disposable> _disposables = new ArrayList<>();

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.i("業務開始");

        Intent it = getIntent();
        String str = it.getStringExtra("startType");
        if (str != null) {
            if(str.equals("prepaidApp")) {
                AppPreference.setPrepaidSlipId((ArrayList<Long>) it.getSerializableExtra("slipId"));
            }
        }

        //業務開始日時の設定
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        new Thread(() -> {
            String date = dateFmt.format((new Date()));
            DBManager.getAggregateDao().insertAggregateStart(date);
        }).start();

        hideNavigationBar();
        _binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(_binding.getRoot());

        _sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        _binding.setViewModel(_sharedViewModel);

        _sharedViewModel.getUpdatedFlag().observe(this, value -> {
            if (value) {
                if (!_sharedViewModel.isActionBarLock()) {
                    applyActionBar();
                } else {
                    _sharedViewModel.isActionBarLock(false);
                }
            }
        });


        _sharedViewModel.getBackVisibleFlag().observe(this, value -> {
            ActionBar actionBar = getSupportActionBar();

            if (actionBar != null) {
                findViewById(R.id.toolbar_main_back).setVisibility(value ? View.VISIBLE : View.GONE);
            }
        });

        _sharedViewModel.getActionBarColor().observe(this, color -> {
            if (color == null) return;

            ActionBar actionBar = getSupportActionBar();

            if (actionBar != null) {
                actionBar.setBackgroundDrawable(getResources().getDrawable(color, null));
            }
        });

        // `MainPosEnum`を制御するためのリスナーを追加
        _sharedViewModel.getActionBarMode().observe(this, pos -> {
            Timber.v("getActionBarMode observe: %s", String.valueOf(pos));
            if (pos == SharedViewModel.ActionBarMode.POS) {
                // Posの場合の処理
                findViewById(R.id.app_main_bar).setVisibility(View.GONE);
//                findViewById(R.id.app_pos_bar).setVisibility(View.VISIBLE);
            } else {
                _sharedViewModel.isTopBarView().observe(this, value -> {
                    if (value) {
                        // Mainの場合の処理
                        findViewById(R.id.app_main_bar).setVisibility(View.VISIBLE);
//                      findViewById(R.id.app_pos_bar).setVisibility(View.GONE);
                    } else {
                        // TopBarを非表示
                        findViewById(R.id.app_main_bar).setVisibility(View.GONE);
                    }
                });
            }
        });
        // NavHostFragment navHostFragment = (NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_main_nav_host);
        // assert navHostFragment != null;
        // navHostFragment.getNavController().addOnDestinationChangedListener((controller, destination, arguments) -> {
        //     SharedViewModel.ActionBarMode pos = _sharedViewModel.getActionBarMode().getValue();
        //     Timber.v("addOnDestinationChangedListener: %s", pos);
        //     if (pos == SharedViewModel.ActionBarMode.POS) {
        //         // Posの場合の処理
        //         findViewById(R.id.app_main_bar).setVisibility(View.GONE);
        //     } else {
        //         // Mainの場合の処理
        //         findViewById(R.id.app_main_bar).setVisibility(View.VISIBLE);
        //     }
        // });

        // `MainPosEnum`の値を変更して、AppBarの表示を制御する
        _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN); // Mainの場合
        // viewModel.setMainPos(MainPosEnum.POS); // Posの場合

        // ...

        findViewById(R.id.loading).setOnClickListener(v -> {
            // 下にあるレイアウトにクリックイベントが伝播してほしくないので何もしない
        });

        // 乗務員コード入力を受け付けていない状態(乗務員コード入力画面以外)で
        // タブレットが出庫になった場合、エラーをスタッキングする
//        EventBroker.signIn
//                .doOnSubscribe(_disposables::add)
//                .subscribeOn(Schedulers.newThread())
//                .subscribe(signIn -> {
//                    Timber.i("タブレットからの乗務員コードを受信: %s", signIn);
//
//                    final boolean needErrorStack = !_sharedViewModel.allowDriverSignIn()
//                            && !signIn.driverCode.equals(AppPreference.getDriverCode());
//
//                    if (needErrorStack) {
//                        ErrorManage errorManage = ErrorManage.getInstance();
//                        errorManage.stackingError(getString(R.string.error_type_payment_system_2015));
//                    }
//                });


        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            findViewById(R.id.toolbar_main_back).setOnClickListener(v -> {
                // ボタン押しっぱなしで非表示になってから指を離すと画面が戻ってしまうので
                // ボタンが非表示のときは処理しない
                if (!_sharedViewModel.getBackVisibleFlag().getValue()) {
                    return;
                }

                final Runnable backAction = _sharedViewModel.getBackAction();
                Timber.d("backAction exists: %s", backAction != null);
                if (backAction != null) {
                    backAction.run();
                } else {
                    try {
                        CommonClickEvent.RecordClickOperation(getString(R.string.btn_back_text), true);
                        NavBackStackEntry previous = Navigation.findNavController(
                                this, R.id.fragment_menu_body_nav_host).getPreviousBackStackEntry();

                        if (previous == null) return;
                    } catch (Exception ex) {
                        NavBackStackEntry previous = Navigation.findNavController(
                                this, R.id.fragment_main_nav_host).getPreviousBackStackEntry();

                        if (previous == null) return;
                    }
                    super.onBackPressed();
                }
            });

            findViewById(R.id.toolbar_cancel).setOnClickListener(view -> {

                _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);
                _sharedViewModel.setCashMenu(false);

                try {
                    CommonClickEvent.RecordClickOperation(getString(R.string.btn_issue_cancel), true);

                    NavBackStackEntry previous = Navigation.findNavController(
                            this, R.id.fragment_main_nav_host).getPreviousBackStackEntry();

                    if (previous == null) return;
                } catch (Exception ex) {
                    NavBackStackEntry previous = Navigation.findNavController(
                            this, R.id.fragment_main_nav_host).getPreviousBackStackEntry();

                    if (previous == null) return;
                }
                super.onBackPressed();
            });

            actionBar.setDisplayShowTitleEnabled(false);
        }

        applyActionBar();

//        Intent periodicService = new Intent(this, PeriodicErrorCheckService.class);
//        startService(periodicService);

        //電波情報取得処理
        Intent radioService = new Intent(getApplication(), GetRadioService.class);
        radioService.putExtra("INTERVAL", AppPreference.getAntlogTime());
        startService(radioService);

        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            //権限があればgps取得サービス起動
            Intent gpsService = new Intent(getApplication(), GetGpsService.class);
            gpsService.putExtra("TIME_INTERVAL", AppPreference.getGpslogTime());
            gpsService.putExtra("DISTANCE_INTERVAL", AppPreference.getGpslogDistance());
            startService(gpsService);
        }

        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (permissions.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), 1000);
        }

        final Intent intent = getIntent();
        final String[] errors = intent.getStringArrayExtra("errors");

        if (errors != null && errors.length != 0) {
            ErrorManage errorManage = ErrorManage.getInstance();

            for (String errCode : errors) {
                errorManage.stackingError(errCode);
            }
        }

        // バッテリー状態のチェック
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, intentFilter);
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        Timber.i("battery %d %%", batteryLevel);

//        // 機内モード状態のチェック
//        this.registerReceiver(new AirPlaneModeChangedReceiver(), new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
//        if (1 == Settings.System.getInt(getContentResolver(),
//                Settings.Global.AIRPLANE_MODE_ON, 0)) {
//            Timber.e("機内モード状態：ON");
//            CurrentRadio.setAirplaneMode();
//        }


    }

    @Override
    protected void onDestroy() {
        for (Disposable d : _disposables) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }

        _disposables.clear();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideNavigationBar();
    }

    @Override
    public void onBackPressed() {
        // ナビゲーションバーの戻るボタンは無効化するため何もしない
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Timber.v("onConfigurationChanged");
        // AndroidManifest.xmlでMainActivityのandroid:configChangesを設定し
        // 何も処理しないことで、外付けバーコードリーダーの接続/切断を行った際、システムがアクティビティを再作成することを防止
        super.onConfigurationChanged(newConfig);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    //位置情報使用を許可された場合はgps取得サービス起動
                    Intent gpsService = new Intent(getApplication(), GetGpsService.class);
                    startService(gpsService);

//                    Intent wifiP2pService = new Intent(this, WifiP2pService.class);
//                    startService(wifiP2pService);
                } else {
                    //拒否された場合
                    Toast toast = Toast.makeText(this, "位置情報は取得できません", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == MenuDiscountFragment.DISCOUNT_MENU_INFO_REQUEST_CODE && data != null && data.getExtras() != null) {
                String a = data.getExtras().getString("discountMenuInfo");
                Gson gson = new Gson();
                List<DiscountMenuInfo> discountMenuInfo = gson.fromJson(data.getExtras().getString("discountMenuInfo"), new TypeToken<List<DiscountMenuInfo>>(){}.getType());
                FragmentManager fragmentManager = getSupportFragmentManager();
                MenuDiscountFragment menuDiscountFragment = findFragmentInFragmentManager(fragmentManager, MenuDiscountFragment.class);
                if (menuDiscountFragment != null) {
                    menuDiscountFragment.setDiscountMenuInfo(discountMenuInfo);
                }
            }
            if (requestCode == MenuDiscountFragment.DISCOUNT_INFO_REQUEST_CODE && data != null && data.getExtras() != null) {
                Gson gson = new Gson();
                DiscountInfo discountInfo = gson.fromJson(data.getExtras().getString("discountInfo"), DiscountInfo.class);
                FragmentManager fragmentManager = getSupportFragmentManager();
                MenuDiscountFragment menuDiscountFragment = findFragmentInFragmentManager(fragmentManager, MenuDiscountFragment.class);
                if (menuDiscountFragment != null) {
                    menuDiscountFragment.setDiscountInfo(discountInfo);
                }
            }
        }
    }

    private <T extends Fragment> T findFragmentInFragmentManager(FragmentManager fragmentManager, Class<T> fragmentClass) {
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragmentClass.isInstance(fragment)) {
                return fragmentClass.cast(fragment);
            }
            if (fragment != null) {
                FragmentManager childFragmentManager = fragment.getChildFragmentManager();
                T childFragment = findFragmentInFragmentManager(childFragmentManager, fragmentClass);
                if (childFragment != null) {
                    return childFragment;
                }
            }
        }
        return null;
    }
}
