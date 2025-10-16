package jp.mcapps.android.multi_payment_terminal.ui.device_check;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Date;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
//import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Meter;

public class DeviceCheckViewModel extends ViewModel {
    private final String DEFAULT_STRING =
            "デバイスチェック開始";
    private final Handler _handler = new Handler(Looper.getMainLooper());

//    private final DeviceNetworkManager _deviceNetworkManager;

    public DeviceCheckViewModel() {
//        _deviceNetworkManager = deviceNetworkManager;
    }

    //初期値 DEFAULT_STRING + 現在日時
    private final MutableLiveData<String> _resultText = new MutableLiveData<>(DEFAULT_STRING + "\n" + Converters.dateToString(new Date()));
    public MutableLiveData<String> getResultText () {
        return _resultText;
    }

    public void appendResultText(String appendText) {
        String resultText = _resultText.getValue();
        resultText = resultText + "\n" + appendText;
        _resultText.setValue(resultText);
    }

    public void clearResultText () {
        _resultText.setValue(DEFAULT_STRING);
        appendResultText(Converters.dateToString(new Date()));
    }

    private MutableLiveData<Boolean> _isRunning = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isRunning() {
        return _isRunning;
    }
    public void isRunning(boolean b) {
        _handler.post(() ->{
            _isRunning.setValue(b);
        });
    }

    public Single<Meter.Response> checkMeter() {
        Single<Meter.Response> single = Single.create(emitter -> {
            final IFBoxApi apiClient = new IFBoxApiImpl();
            apiClient.setBaseUrl("http://dum");
            try {
                emitter.onSuccess(apiClient.getMeter());
            } catch (Exception e) {
                emitter.onError(new Throwable());
            }
        });

        return single.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
    }
}
