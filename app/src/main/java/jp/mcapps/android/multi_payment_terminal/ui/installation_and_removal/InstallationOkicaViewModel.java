package jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Strings;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.okica.Constants;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetAccessToken;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInstallation;
import timber.log.Timber;

public class InstallationOkicaViewModel extends ViewModel {
    public enum States {
        None,
        Expired,
        Requesting,
        InstallRequestSuccess,
        InstallRequestFailure,
        TerminalInfoRequestSuccess,
        TerminalInfoRequestFailure,
        ICMasterRequestSuccess,
        ICMasterRequestFailure,
        AccessKeyRequestSuccess,
        AccessKeyRequestFailure,
        NegaRequestSuccess,
        NegaRequestFailure,

        Finished,
    }


    private final MainApplication _app = MainApplication.getInstance();

    private final Handler _handler = new Handler(Looper.getMainLooper());
    private final McOkicaCenterApi _api = new McOkicaCenterApiImpl();
    private final OkicaMasterControl _okicaMasterCtrl = new OkicaMasterControl();
    private final ExecutorService _pool = Executors.newSingleThreadExecutor();

    private String _url = "";
    public String getUrl() {
        return _url;
    }

    private long _expiredTime = 0;
    private final Timer _countdownTimer = new Timer();
    private final Timer _pollingTimer = new Timer();

    private final MutableLiveData<States> _state = new MutableLiveData<>(States.None);
    public MutableLiveData<States> getState() {
        return _state;
    }
    public void setState(States state) {
        Timber.d("設置状態:%s => %s", _state.getValue(), state);
        _handler.post(() -> {
            _state.setValue(state);
        });
    }

    private final MutableLiveData<String> _activationCode = new MutableLiveData<>("");
    public final MutableLiveData<String> getActivationCode() {
        return _activationCode;
    }
    public final void setActivationCode(String code) {
        _handler.post(() -> {
            _activationCode.setValue(code);
        });
    }

    private final MutableLiveData<String> _timeLimit = new MutableLiveData<>("");
    public MutableLiveData<String> getTimeLimit() {
        return _timeLimit;
    }
    public void setTimeLimit(long timeLimit) {
        _handler.post(() -> {
            _timeLimit.setValue(Long.toString(timeLimit));
        });
    }

    public void start() {
        _countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long timeLimit = _expiredTime - getCurrentTime();

                if (timeLimit > 0) {
                    setTimeLimit(timeLimit);
                } else {
                    setTimeLimit(0);
                    if (_state.getValue() == States.InstallRequestSuccess) {
                        setState(States.Expired);
                    }
                }
            }
        }, 0, 1000);

        _pollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final String code = _activationCode.getValue();
                if (_state.getValue() == States.InstallRequestSuccess && !Strings.isNullOrEmpty(code)) {
                    final GetAccessToken.Response response = _api.getAccessToken(Constants.TERMINAL_INSTALL_ID, code);

                    if (response.result) {
                        AppPreference.setOkicaAccessToken(response.accessToken);

                        getTerminalInfo();
                        if (_state.getValue() == States.TerminalInfoRequestFailure) {
                            return;
                        }

                        getICMaster();
                        if (_state.getValue() == States.ICMasterRequestFailure) {
                            return;
                        }

                        getAccessKey();
                        if (_state.getValue() == States.AccessKeyRequestFailure) {
                            return;
                        }

                        getNega();
                        if (_state.getValue() == States.NegaRequestFailure) {
                            return;
                        }

                        setState(States.Finished);
                    }
                }
            }
        }, 5000, 5000);
    }

    public void stop() {
        _countdownTimer.cancel();
        _pollingTimer.cancel();
    }

    /**
     * 端末設置要求を行います
     */
    public void requestInstallation() {
        _pool.submit(() -> {
            setState(States.Requesting);

            final TerminalInstallation.Response response = _api.installTerminal(Constants.TERMINAL_INSTALL_ID);

            if (response.result) {

                _url = response.url;
                _expiredTime = response.expiredTime;

                AppPreference.setOkicaAuthCode(response.code);
                setActivationCode(response.code);

                setState(States.InstallRequestSuccess);
            } else {
                setState(States.InstallRequestFailure);
            }
        });
    }

    private void getTerminalInfo() {
        setState(States.Requesting);
        final TerminalInfo.Response response = _api.getTerminalInfo();
        if (response.result) {
            setState(States.TerminalInfoRequestSuccess);
            AppPreference.setOkicaTerminalInfo(response);
        } else {
            setState(States.TerminalInfoRequestFailure);
        }
    }

    private void getICMaster() {
        setState(States.Requesting);
        if ( _okicaMasterCtrl.getICMaster() ) {
            _app.setOkicaICMaster(ICMaster.load());
            setState(States.ICMasterRequestSuccess);
        } else {
            setState(States.ICMasterRequestFailure);
        }
    }

    private void getAccessKey() {
        setState(States.Requesting);
        if (_okicaMasterCtrl.getAccessKey()) {
            setState(States.AccessKeyRequestSuccess);
        } else {
            setState(States.AccessKeyRequestFailure);
        }
    }

    private void getNega() {
        setState(States.Requesting);
        if (_okicaMasterCtrl.okicaGetNega()) {
            setState(States.NegaRequestSuccess);
        } else {
            setState(States.NegaRequestFailure);
        }
    }

    private int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }
}
