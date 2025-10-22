package jp.mcapps.android.multi_payment_terminal;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.logger.EventLogger;
import jp.mcapps.android.multi_payment_terminal.model.CreditChecker;
import jp.mcapps.android.multi_payment_terminal.model.EmoneyChecker;
import jp.mcapps.android.multi_payment_terminal.model.OkicaChecker;
import jp.mcapps.android.multi_payment_terminal.model.QRChecker;
import jp.mcapps.android.multi_payment_terminal.model.WatariChecker;
//import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.Updater;
import jp.mcapps.android.multi_payment_terminal.service.LogSendServiceViewModel;
//import jp.mcapps.android.multi_payment_terminal.service.PeriodicErrorCheckServiceViewModel;
//import jp.mcapps.android.multi_payment_terminal.service.PeriodicGateCheckServiceViewModel;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputAdvancePayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputLt27ViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.auto_daily_report.AutoDailyReportFuelViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.device_check.DeviceCheckViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.discount.DiscountFutabaDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.discount.DiscountJobFutabaDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.driver_code.DriverCodeViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal.InstallationAndRemovalViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.setup.IFBoxSetupViewModel;
//import jp.mcapps.android.multi_payment_terminal.ui.setup.TabletLinkSetupViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.update.UpdateViewModel;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;

@SuppressWarnings("unchecked")
public class ViewModelFactory implements ViewModelProvider.Factory {
    //private final DeviceNetworkManager _deviceNetworkManager = new DeviceNetworkManager();
//    private final IFBoxManager _ifBoxManager = new IFBoxManager(_deviceNetworkManager);
    protected final EventLogger _eventLogger = new EventLogger();
    private final Updater _updater = new Updater();
//    private final TabletLinker _tabletLinker = new TabletLinker(_deviceNetworkManager);

    // PrinterからIFBoxManagerを共有するための処理
    {
//        PrinterProc printerProc = PrinterProc.getInstance() ;
//        printerProc.setIFBoxManager(_ifBoxManager);
//
//        PrinterManager printerManager = PrinterManager.getInstance() ;
//        printerManager.setIFBoxManager(_ifBoxManager);
//
//        CreditChecker creditChecker = CreditChecker.getInstance() ;
//        creditChecker.setIFBoxManager(_ifBoxManager);
//
//        EmoneyChecker emoneyChecker = EmoneyChecker.getInstance() ;
//        emoneyChecker.setIFBoxManager(_ifBoxManager);
//
//        QRChecker qrChecker = QRChecker.getInstance();
//        qrChecker.setIFBoxManager(_ifBoxManager);
//
//        OkicaChecker okicaChecker = OkicaChecker.getInstance() ;
//        okicaChecker.setIFBoxManager(_ifBoxManager);
//
//        WatariChecker watariChecker = WatariChecker.getInstance();
//        watariChecker.setIFBoxManager(_ifBoxManager);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom((StartViewModel.class))) {
            return (T) new StartViewModel(_eventLogger, _updater);
        }
//        if (modelClass.isAssignableFrom((WifiP2pServiceViewModel.class))) {
//            return (T) new WifiP2pServiceViewModel(_deviceNetworkManager, _ifBoxManager, _tabletLinker);
//        }
        else if (modelClass.isAssignableFrom((IFBoxSetupViewModel.class))) {
//            return (T) new IFBoxSetupViewModel(_deviceNetworkManager, _ifBoxManager, _updater);
        }
        else if (modelClass.isAssignableFrom((UpdateViewModel.class))) {
            return (T) new UpdateViewModel(_updater);
        }
//        else if (modelClass.isAssignableFrom((TabletLinkSetupViewModel.class))) {
//            return (T) new TabletLinkSetupViewModel(_deviceNetworkManager, _ifBoxManager, _tabletLinker);
//        }
        else if (modelClass.isAssignableFrom((DeviceCheckViewModel.class))) {
//            return (T) new DeviceCheckViewModel(_deviceNetworkManager);
        }
        else if (modelClass.isAssignableFrom((AmountInputViewModel.class))) {
            return (T) new AmountInputViewModel();
        }
        else if (modelClass.isAssignableFrom((MenuViewModel.class))) {
            return (T) new MenuViewModel(_eventLogger);
        }
//        else if (modelClass.isAssignableFrom((PeriodicErrorCheckServiceViewModel.class))) {
//            return (T) new PeriodicErrorCheckServiceViewModel(_eventLogger);
//        }
        else if (modelClass.isAssignableFrom((InstallationAndRemovalViewModel.class))) {
            return (T) new InstallationAndRemovalViewModel();
        }
        else if (modelClass.isAssignableFrom((AmountInputLt27ViewModel.class))) {
            return (T) new AmountInputLt27ViewModel();
        }
        else if (modelClass.isAssignableFrom((DriverCodeViewModel.class))) {
            return (T) new DriverCodeViewModel();
        }
        else if (modelClass.isAssignableFrom((LogSendServiceViewModel.class))) {
            return (T) new LogSendServiceViewModel(_eventLogger);
        }
//        else if (modelClass.isAssignableFrom((PeriodicGateCheckServiceViewModel.class))) {
//            return (T) new PeriodicGateCheckServiceViewModel();
//        }
//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
        else if (modelClass.isAssignableFrom((AmountInputAdvancePayFDViewModel.class))) {
            return (T) new AmountInputAdvancePayFDViewModel();
        }
        else if (modelClass.isAssignableFrom((AmountInputSeparationPayFDViewModel.class))) {
            return (T) new AmountInputSeparationPayFDViewModel();
        }
        else if (modelClass.isAssignableFrom((DiscountJobFutabaDViewModel.class))) {
            return (T) new DiscountJobFutabaDViewModel();
        }
        else if (modelClass.isAssignableFrom((DiscountFutabaDViewModel.class))) {
            return (T) new DiscountFutabaDViewModel();
        }
        else if (modelClass.isAssignableFrom((AutoDailyReportFuelViewModel.class))) {
            return (T) new AutoDailyReportFuelViewModel();
        }
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修

        return null;
    }
}
