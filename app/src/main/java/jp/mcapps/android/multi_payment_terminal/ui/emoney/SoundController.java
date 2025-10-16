package jp.mcapps.android.multi_payment_terminal.ui.emoney;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;

import androidx.annotation.RequiresApi;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import timber.log.Timber;

public class SoundController {
    private float _volume;
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private final SoundManager _soundManager = SoundManager.getInstance();
    private final HashMap<Integer, Integer> _soundMap;
    private int _soundId;

    public SoundController(HashMap<Integer, Integer> soundMap) {
        _volume = AppPreference.getSoundPaymentVolume() / 10f;
        _soundMap = soundMap;
    }

    public void pause() {
        _soundManager.autoPause();
    }

    public void cleanup() {
        _soundManager.autoPause();
    }

    public float getVolumeEmoney(Integer soundResource) {
        float vol = 0f;

        /* 案内音の場合は案内音の音量、それ以外は決済音の音量に */
        if (soundResource == R.raw.emoney_touch_default
        ||  soundResource == R.raw.emoney_touch_only) {
            vol =  AppPreference.getSoundGuidanceVolume() / 10f;
        } else {
            vol =  AppPreference.getSoundPaymentVolume() / 10f;
        }
        return vol;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void setStatus(int controlCode, boolean endlessRepeat) {
        Integer soundResource = _soundMap.get(controlCode);
        Timber.d("sound control code: %s", controlCode);

        if (soundResource != null) {
            _soundId = _soundManager.load(MainApplication.getInstance(), soundResource, 1);
            _volume = getVolumeEmoney(soundResource);
            _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
                if (soundId == _soundId) {
                    soundPool.play(soundId, _volume, _volume, 1, endlessRepeat ? -1 : 0, 1);;
                }

                soundPool.unload(soundId);
            });
        } else {
            _soundManager.stop(_soundId);
        }
    }
}
