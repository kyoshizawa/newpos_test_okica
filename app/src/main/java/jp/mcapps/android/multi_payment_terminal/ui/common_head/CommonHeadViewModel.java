package jp.mcapps.android.multi_payment_terminal.ui.common_head;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import timber.log.Timber;

public class CommonHeadViewModel extends ViewModel implements LifecycleObserver {
    private final Application _app = MainApplication.getInstance();
    private final int[] radioImageResources = {R.drawable.ic_radio_level_low, R.drawable.ic_radio_level_middle, R.drawable.ic_radio_level_high, R.drawable.ic_airplane_mode};
    private final BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setRadioImageResource(radioImageResources[CurrentRadio.getImageLevel()]);
        }
    };

    private final MutableLiveData<Integer> _amount = new MutableLiveData<>(Amount.getFixedAmount());
    public MutableLiveData<Integer> getAmount() { return _amount; }
    public void setAmount(int amount) {
        _amount.setValue(amount);
    }

    private MutableLiveData<Drawable> _warningImage = new MutableLiveData<Drawable>(null);
    public MutableLiveData<Drawable> getWarningImage() {
        return _warningImage;
    }
    public void setWarningImage() {
        _warningImage.setValue(MainApplication.getInstance().getDrawable(R.drawable.ic_warning));
    }
    public void setErrorImage() {
        _warningImage.setValue(MainApplication.getInstance().getDrawable(R.drawable.ic_error));
    }
    public void resetWarningImage() {
        _warningImage.setValue(null);
    }

    private final MutableLiveData<Integer> _radioImageResource = new MutableLiveData<>();
    public MutableLiveData<Integer> getRadioImageResource() {
        return _radioImageResource;
    }
    public void setRadioImageResource(int radioImageResource) {
        _radioImageResource.setValue(radioImageResource);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        Timber.d("on resume");
        if (!AppPreference.isDemoMode()) {
            IntentFilter intentFilter = new IntentFilter("CHANGE_RADIO_LEVEL_IMAGE");
            LocalBroadcastManager.getInstance(_app).registerReceiver(_receiver, intentFilter); //通常モードのみ電波レベルの変更通知を受け取る
            setRadioImageResource(radioImageResources[CurrentRadio.getImageLevel()]); //電波レベルの初期画像
        } else {
            setRadioImageResource(radioImageResources[2]); //デモモードは強固定
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (!AppPreference.isDemoMode()) {
            LocalBroadcastManager.getInstance(_app).unregisterReceiver(_receiver); //通常モードのみ電波レベルの変更通知を受け取るためここで解除
        }
    }

    {
        if (AppPreference.isDemoMode()) {
            setRadioImageResource(radioImageResources[2]); //デモモードは強固定
        } else {
            setRadioImageResource(radioImageResources[CurrentRadio.getImageLevel()]); //電波レベルの初期画像
        }
    }
}
