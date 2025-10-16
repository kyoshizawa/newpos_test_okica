package jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;

import com.google.common.base.Strings;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverDao;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.JremActivator;
import jp.mcapps.android.multi_payment_terminal.model.JremOpener;
import jp.mcapps.android.multi_payment_terminal.model.QRSettlement;
//import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputViewModel;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.JremActivationApi;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.JremActivationApiImpl;

public class InstallationAndRemovalViewModel extends PinInputViewModel {
    private final JremActivationApi _apiClient = new JremActivationApiImpl();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private final String _activateId = AppPreference.getJremActivateId();
    private final String _activatePassword = AppPreference.getJremPassword();
    private final JremActivator _activator = new JremActivator();
    private final JremOpener _opener = new JremOpener();
    private final QRSettlement _qr = new QRSettlement();

//    private final DeviceNetworkManager _deviceNetworkManager;
//    private final IFBoxManager _ifBoxManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    {
        _pinDigits = 10;
    }

    public InstallationAndRemovalViewModel() {
        //_deviceNetworkManager = deviceNetworkManager;
        //_ifBoxManager = ifBoxManager;
    }

    private final MutableLiveData<Boolean> _hasError = new MutableLiveData<>(false);

    public MutableLiveData<Boolean> hasError() {
        return _hasError;
    }
    public final void hasError(boolean b) {
        _hasError.setValue(b);
    }

    private String _errorCode = "";


    public String getErrorCode() {
        return _errorCode;
    }

    public void setErrorCode(String errorCode) {
        _handler.post(() -> {
            _errorCode = errorCode;
            _hasError.setValue(true);
        });
    }

    public enum DeviceInterLocking {
        None,
        IFBox,
        Tablet,
    }

    private MutableLiveData<Boolean> _isPinSuccess = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isPinSuccess() {
        return _isPinSuccess;
    }
    public void isPinSuccess(boolean b) {
        _isPinSuccess.setValue(b);
    }

    private MutableLiveData<Boolean> _isEdyOpeningFinished = new MutableLiveData<>(EmoneyOpeningInfo.getEdy() != null);
    public MutableLiveData<Boolean> isEdyOpeningFinished() {
        return _isEdyOpeningFinished;
    }

    private MutableLiveData<Boolean> _isEdyInitCommunicated = new MutableLiveData<>(
            EmoneyOpeningInfo.getEdy() != null && !EmoneyOpeningInfo.getEdy().initCommunicationFlg);
    public MutableLiveData<Boolean> isEdyInitCommunicated() {
        return _isEdyInitCommunicated;
    }
    public void isEdyInitCommunicated(boolean b) {
        _handler.post(() -> {
            _isEdyInitCommunicated.setValue(b);
        });
    }

    private MutableLiveData<DeviceInterLocking> _deviceInterlocking = new MutableLiveData<>(DeviceInterLocking.None);
    public MutableLiveData<DeviceInterLocking> getDeviceInterlocking() {
        return _deviceInterlocking;
    }
    public void setDeviceInterlocking() {
        _deviceInterlocking.setValue(AppPreference.getTabletLinkInfo() != null
                ? DeviceInterLocking.Tablet
                : AppPreference.getIFBoxOTAInfo() != null
                ? DeviceInterLocking.IFBox
                : DeviceInterLocking.None);
    }

    private MutableLiveData<Boolean> _isOkicaInstalled = new MutableLiveData<>(!Strings.isNullOrEmpty(AppPreference.getOkicaAccessToken()));
    public MutableLiveData<Boolean> isOkicaInstalled() {
        return _isOkicaInstalled;
    }
    public void isOkicaInstalled(boolean b) {
        _isOkicaInstalled.setValue(b);
    }

    public boolean install() {
        final String errCode = _activator.install();
        if (errCode != null) {
            setErrorCode(errCode);
        }
        return errCode == null;
    }

    public String uninstall() {
        return _activator.uninstall();
    }

    public String uninstallQR() {

        return _qr.uninstall();
    }

    private final MutableLiveData<String> _display = new MutableLiveData<>("暗証番号を入力してください。");
    public final MutableLiveData<String> getDisplay() {
        return _display;
    }
    public final void setDisplay(String message) {
        _display.setValue(message);
    }

    private MutableLiveData<Boolean> _removeBtnEnabled = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> getRemoveBtnEnabled() { return _removeBtnEnabled; }

    public void fetchDrivers(DriverDao dao) {
        final Runnable run = () -> {
            final List<DriverDao.Driver> drivers = dao.getDrivers();
            handler.post(() -> {
                if (drivers.size() > 0) {
                    _removeBtnEnabled.setValue(true);
                } else {
                    _removeBtnEnabled.setValue(false);
                }
            });
        };

        new Thread(run).start();
    }

    @Override
    public boolean enter() {
        enterBeep();    //効果音
        final boolean ret = _pin.equals("8888888888");
        _isPinSuccess.setValue(ret);

        return ret;
    }

    @Override
    public void cancel() {
    }

    public void edyInitCommunication() {
        isEdyInitCommunicated(_opener.edyInitCommunication());
    }

    public void edyRemove() {
        isEdyInitCommunicated(!_opener.edyRemove());
    }

    public void disconnectIFBox() {
//        _ifBoxManager.stop();
    }

//    public Completable removeWifiP2pGroup() {
////        return _deviceNetworkManager.deletePersistentGroup();
//    }

    private MutableLiveData<Boolean> _isJremActivateIdEnabled = new MutableLiveData<>(AppPreference.getJremActivateId() != null && !AppPreference.getJremActivateId().equals(""));
    public MutableLiveData<Boolean> isJremActivateIdEnabled() { return _isJremActivateIdEnabled; }
    public void setJremActivateIdEnabled(String JremActivateId) {
        if (JremActivateId == null || JremActivateId.equals("")) {
            _isJremActivateIdEnabled.setValue(false);
        } else {
            _isJremActivateIdEnabled.setValue(true);
        }
    }

    private MutableLiveData<Boolean> _isDemoEnabled = new MutableLiveData<>(AppPreference.isDemoMode());
    public MutableLiveData<Boolean> isDemoEnabled() { return _isDemoEnabled; }
    public void setDemoEnabled(boolean b) { _isDemoEnabled.setValue(b); }

    private MutableLiveData<Boolean> _isPosActivate = new MutableLiveData<>(AppPreference.isServicePos());
    public MutableLiveData<Boolean> isPosActivate() {
        return _isPosActivate;
    }
    public void setPosActivate(boolean b) { _isPosActivate.setValue(b); }

    private MutableLiveData<Boolean> _isTicketActivate = new MutableLiveData<>(AppPreference.isServiceTicket());
    public MutableLiveData<Boolean> isTicketActivate() {
        return _isTicketActivate;
    }
    public void setTicketActivate(boolean b) { _isTicketActivate.setValue(b); }

    private MutableLiveData<Boolean> _isExternal = new MutableLiveData<>(AppPreference.getIsOnCradle());
    public MutableLiveData<Boolean> isExternal() {
        return _isExternal;
    }
    public void setExternal(boolean b) { _isExternal.setValue(b); }
}
