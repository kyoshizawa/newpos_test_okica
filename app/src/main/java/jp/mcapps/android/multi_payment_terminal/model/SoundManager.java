package jp.mcapps.android.multi_payment_terminal.model;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.room.Room;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;

public class SoundManager {
    private static SoundManager _instance;
    private MainApplication _app = MainApplication.getInstance();
    private SoundPool _soundPool;

    private SoundManager() {
        final AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        _soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(1)
                .build();
    }

    public static SoundManager getInstance() {
        if (_instance == null) {
            synchronized (SoundManager.class) {
                if (_instance == null) {
                    _instance = new SoundManager();
                }
            }
        }
        return _instance;
    }

    public int load(Context context, int resId, int priority) {
        setVolume();
        return _soundPool.load(_app, resId, 1);
    }

    public boolean unload(int var1) {
        return _soundPool.unload(var1);
    };

    public int play(int soundID, float leftVolume, float rightVolume, int priority, int loop, float rate) {
        return _soundPool.play(soundID, leftVolume, rightVolume, priority, loop, rate);
    }

    public void setOnLoadCompleteListener(SoundPool.OnLoadCompleteListener listener) {
        _soundPool.setOnLoadCompleteListener(listener);
    }

    public void autoPause() {
        _soundPool.autoPause();
    }

    public void stop(int var1) {
        _soundPool.stop(var1);
    };

    private void setVolume() {
        AudioManager audioManager = (AudioManager) _app.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (int)(maxVolume * 0.7), 0);
    }
}
