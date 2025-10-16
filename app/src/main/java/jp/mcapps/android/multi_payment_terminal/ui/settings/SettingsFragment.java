package jp.mcapps.android.multi_payment_terminal.ui.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.pos.device.sys.SystemManager;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import timber.log.Timber;

@RequiresApi(api = Build.VERSION_CODES.M)
public class SettingsFragment extends PreferenceFragmentCompat {
    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "設定";
    private ListPreference _screenTimeout; //画面オフ時間の設定　デモモードのみ
    private ListPreference _screenLock; //画面ロック時間の設定　デモモードのみ
    private SeekBarPreference _soundPayment;
    private SeekBarPreference _soundGuidance;
    private SeekBarPreference _brightness;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        if (AppPreference.isDemoMode()) {
            setPreferencesFromResource(R.xml.demo_preferences, rootKey);
            _screenTimeout = findPreference(getString(R.string.setting_key_demo_screen_timeout));
            _screenTimeout.setSummary(_screenTimeout.getEntry());
            _screenLock = findPreference(getString(R.string.setting_key_demo_screen_lock));
            _screenLock.setSummary(_screenLock.getEntry());
            SwitchPreferenceCompat screenLockEnabled = findPreference(getString(R.string.setting_key_screenlock_enabled));
            screenLockEnabled.setDefaultValue(AppPreference.isScreenlockEnabled());
            screenLockEnabled.setSummary("パスワード：" + getString(R.string.setting_default_screenlock_password));
        } else {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        _soundPayment = findPreference(getString(R.string.confirm_key_sound_payment));
        _soundGuidance = findPreference(getString(R.string.confirm_key_sound_guidance));
        _brightness = findPreference(getString(R.string.confirm_key_brightness));

        Preference backButton = (Preference) getPreferenceManager()
                .findPreference(getString(R.string.setting_key_btn_back));

        FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);

            if (backButton != null) {
                backButton.setOnPreferenceClickListener(v -> {
                    activity.runOnUiThread(() -> {
                        NavigationWrapper.popBackStack(activity, R.id.fragment_main_nav_host);
                    });
                    return true;
                });
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
//        AppPreference.load();
        return super.onPreferenceTreeClick(preference);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener _listener = (sharedPreferences, key) -> {
        if (AppPreference.isDemoMode()) {
            //現在の設定値を表示させたいので変更のタイミングでセット
            if (key.equals(getString(R.string.setting_key_demo_screen_timeout))) _screenTimeout.setSummary(_screenTimeout.getEntry());
            else if (key.equals(getString(R.string.setting_key_demo_screen_lock))) _screenLock.setSummary(_screenLock.getEntry());
        }

        if (key.equals(_soundPayment.getKey())) testSound(0, _soundPayment.getValue());
        else if (key.equals(_soundGuidance.getKey())) testSound(1, _soundGuidance.getValue());
        else if (key.equals(_brightness.getKey())) {
            int value;    // 0〜255の値を設定
            // 輝度設定
            value = (int)(_brightness.getValue()/10.0f * 255);
            Timber.i("brightness %d %%", value);
            Settings.System.putInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);

            // スクリーンの輝度のみ変えるパターン
//            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//            lp.screenBrightness = _brightness.getValue()/10.0f;
//            ((Activity)getContext()).getWindow().setAttributes(lp);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(_listener);
        // BaseFragmentを継承できないのでここで戻るボタンの表示を制御する
        _sharedViewModel.setBackVisibleFlag(true);
        // 輝度を取得して設定値に設定
        int value;
        try {
            value = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            Timber.i("brightness %d %%", value);
            _brightness.setValue(value/25);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        AppPreference.load();
        if (AppPreference.isDemoMode() && MainApplication.getInstance().isSDKInit()) {
            SystemManager.setScreenTimeOut(Integer.parseInt(_screenTimeout.getValue()));
            if (AppPreference.isScreenlockEnabled()) {
                SystemManager.setPinLockScreenPassword(getString(R.string.setting_default_screenlock_password));
                SystemManager.setLockTimeOut(Integer.parseInt(_screenLock.getValue()));
            } else {
                SystemManager.cancelLockScreen();
            }
        }
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(_listener);
        _sharedViewModel.setUpdatedFlag(true);
    }

    private final SoundManager _soundManager = SoundManager.getInstance();

    /*
        type: 0 -> 決済音, 1 -> 案内音
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testSound(int type, int testVolume) {
        @RawRes int id = type == 0 ? R.raw.completeover1000 : R.raw.emoney_touch_only;
        float volume = testVolume / 10f;

        _soundManager.load(MainApplication.getInstance(), id, 1);
        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> soundPool.play(soundId, volume, volume, 1, 0, 1));
    }
}