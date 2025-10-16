package jp.mcapps.android.multi_payment_terminal.ui.setup;

import android.view.View;

public interface IFBoxSetupEventHandlers {
    void onConnectionClick(View view);
    void onQRClick(View view);
    void onFirmwareClick();
    void onConfigurationClick();
    void onWifiScanClick(View view);
}
