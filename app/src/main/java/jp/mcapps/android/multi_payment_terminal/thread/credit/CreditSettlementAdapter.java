package jp.mcapps.android.multi_payment_terminal.thread.credit;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;

import androidx.annotation.RequiresApi;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import timber.log.Timber;

public abstract class CreditSettlementAdapter implements CreditSettlement.CreditSettlementListener {
    private SoundManager _soundManager = SoundManager.getInstance();
    private float _soundVolume = AppPreference.getSoundPaymentVolume() / 10f;

    @Override
    public void OnProcStart() {
    }

    @Override
    public void OnProcEnd() {
    }

    @Override
    public void OnError(String errorCode) {
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OnSound(int id) {
        Timber.d("OnSound");
        _soundManager.load(MainApplication.getInstance(), id, 1);
        _soundVolume = getVolumeCredit(id);
        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            soundPool.play(soundId, _soundVolume, _soundVolume, 1, 0, 1);
        });
    }

    @Override
    public void selectApplication(String[] applications) {
    }

    @Override
    public void timeoutWaitCard(String errorCode) {
    }

    @Override
    public void cancelPin() {
    }

    protected float getVolumeCredit(Integer soundResource) {
        float vol = 0f;

        /* 案内音の場合は案内音の音量、それ以外は決済音の音量に */
        if (soundResource == R.raw.credit_signature
                ||  soundResource == R.raw.credit_start
                ||  soundResource == R.raw.credit_input_pin
                || soundResource == R.raw.remove_card) {
            vol =  AppPreference.getSoundGuidanceVolume() / 10f;
        } else {
            vol =  AppPreference.getSoundPaymentVolume() / 10f;
        }
        return vol;
    }
}
