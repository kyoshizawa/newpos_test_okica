package jp.mcapps.android.multi_payment_terminal.service;

import androidx.lifecycle.ViewModel;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.TabletLinker;

public class WifiP2pServiceViewModel extends ViewModel {
    private final DeviceNetworkManager _deviceNetworkManager;
    private final IFBoxManager _ifBoxManager;
    private final TabletLinker _tabletLinker;

    public WifiP2pServiceViewModel(DeviceNetworkManager deviceNetworkManager, IFBoxManager ifBoxManager, TabletLinker tabletLinker) {
        _deviceNetworkManager = deviceNetworkManager;
        _ifBoxManager = ifBoxManager;
        _tabletLinker = tabletLinker;
    }

    public void start() {
        _deviceNetworkManager.start();

        _tabletLinker.start();
        if (AppPreference.getIFBoxOTAInfo() != null || AppPreference.getTabletLinkInfo() != null) {
            // IF-BOXのキッティングをしていない場合は起動しない
            // キッティング時に開始する
            _ifBoxManager.start();
        }
    }

    public void stop() {
        _deviceNetworkManager.stop();
        _ifBoxManager.stop();
        _tabletLinker.stop();
    }
}
