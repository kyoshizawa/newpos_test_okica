package jp.mcapps.android.multi_payment_terminal.ui.driver_code;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverDao;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverData;
import jp.mcapps.android.multi_payment_terminal.model.McAuthenticator;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import jp.mcapps.android.multi_payment_terminal.model.TabletLinker;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.SignedIn;
import timber.log.Timber;

public class DriverCodeViewModel extends ViewModel {
    private static final int MAX_DRIVER_CODE_DIGITS = 6;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DriverDao _dao = DBManager.getDriverDao();
    private List<DriverDao.Driver> _drivers;
    private int _driverIdx = 0;
    private McTerminal _terminal = new McTerminal();
    private final TabletLinker _tabletLinker;

    public DriverCodeViewModel(TabletLinker tabletLinker) {
        _tabletLinker = tabletLinker;
    }

    private final MutableLiveData<String> _driverCode = new MutableLiveData<String>("");
    public MutableLiveData<String> getDriverCode() { return _driverCode; }
    public void setDriverCode(String driverCode) { _driverCode.setValue(driverCode); }

    private final MutableLiveData<String> _driverName = new MutableLiveData<String>("");
    public MutableLiveData<String> getDriverName() { return _driverName; }
    public void setDriverName(String driverName) { _driverName.setValue(driverName); }

    private final ObservableBoolean _prevBtnEnabled = new ObservableBoolean(false);
    public ObservableBoolean getPrevBtnEnabled() { return _prevBtnEnabled; }

    private final ObservableBoolean _nextBtnEnabled = new ObservableBoolean(false);
    public ObservableBoolean getNextBtnEnabled() { return _nextBtnEnabled; }

    private boolean _isTabletLinkSuccess = false;
    public boolean isTabletLinkSuccess() {
        return _isTabletLinkSuccess;
    }
    public void isTabletLinkSuccess(boolean b) {
        _isTabletLinkSuccess = b;
    }

    public void fetchDrivers() {
        final Runnable run = () -> {
            final List<DriverDao.Driver> drivers = _dao.getDrivers();
            handler.post(() -> {
                _drivers = drivers;
                if (!drivers.isEmpty()) {
                    setDriver(drivers.get(0));
                    _prevBtnEnabled.set(drivers.size() >= 2);
                }
            });
        };

        new Thread(run).start();
    }

    public Single<SignedIn.Response> getTabletSignedInDriver() {
        return _tabletLinker.getSignedIn();
    }

    public boolean isHistoryExists() {
        return _drivers.size() > 0;
    }

    public DriverDao.Driver getLatestDriver() {
        return _drivers.get(0);
    }

    public void addDrivers(DriverData driverData) {
        new Thread(() -> {
            Timber.d("ドライバー追加");
            _dao.addDriverHistory(driverData);
        }).start();
    }

    public void inputNumber(String number) {
        if (_driverCode.getValue().length() >= MAX_DRIVER_CODE_DIGITS) {
            Timber.d("桁数オーバー");
            return;
        }

        if (_driverName.getValue() != "") {
            clearDriver();
        }

        final String driverCode =_driverCode.getValue() + number;
        _driverCode.setValue(driverCode);
    }

    public void correct() {
        clearDriver();
    }

    public String enter() {
        String errorCode;
        if (isTabletLinkSuccess()) {
            errorCode = _terminal.getDriver(
                    Integer.parseInt(Objects.requireNonNull(_driverCode.getValue())), true, _driverName.getValue());
        } else {
            errorCode = _terminal.getDriver(
                    Integer.parseInt(Objects.requireNonNull(_driverCode.getValue())), false, "");
        }


        final DriverData driverData = new DriverData();

        driverData.driverCode = AppPreference.getDriverCode();
        driverData.driverName = AppPreference.getDriverName();
        driverData.createdAt = new Date();
        addDrivers(driverData);

        if (errorCode == null) {
            //売上情報連携
            _terminal.postPayment();    // 現状、乗務員コード入力時の売上送信エラーは表示していない
        }

        return errorCode;
    }

    public void prevDriver() {
        if (_driverIdx >= -1 && _driverIdx < _drivers.size()-1) {
            _driverIdx += 1;
            _prevBtnEnabled.set(_driverIdx < _drivers.size()-1);
            _nextBtnEnabled.set(_driverIdx > 0);
            setDriver(_drivers.get(_driverIdx));
        }
    }

    public void nextDriver() {
        if (_driverIdx <= _drivers.size()-1 && _driverIdx > 0) {
            _driverIdx -= 1;
            _nextBtnEnabled.set(_driverIdx > 0);
            _prevBtnEnabled.set(true);
            setDriver(_drivers.get(_driverIdx));
        }
    }

    private void setDriver(DriverDao.Driver driver) {
        _driverCode.setValue(driver.code);
        _driverName.setValue(driver.name);
    }

    private void clearDriver() {
        _driverIdx = -1;
        _nextBtnEnabled.set(false);
        _prevBtnEnabled.set(_drivers.size() >= 1);
        _driverCode.setValue("");
        _driverName.setValue("");
    }
}
