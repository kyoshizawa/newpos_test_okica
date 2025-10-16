package jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal;

import android.view.View;

import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputEventHandlers;

public interface InstallationAndRemovalEventHandlers extends PinInputEventHandlers {
    void onRegisterCarNumberClick(View view);
    void onActivateClick(View view);
    void onDeactivateClick(View view);
    void onDeactivateQRClick(View view);
    void onEdyActivation(View view);
    void onEdyDeactivation(View view);
    void onOkicaActivation(View view);
    void onOkicaDeactivation(View view);
    void onIFBoxSetupClick(View view);
    void onDisconnectIFBox(View view);
    void onTabletLinkSetupClick(View view);
    void onTabletUnlinkClick(View view);
    void onCashChangerSetupClick(View view);
    void onDisconnectCashChanger(View view);
    void onRemoveDriverCodeClick(View view);
    void onSettingsClick(View view);
    void onEnableDemoClick(View view);
    void onDisableDemoClick(View view);
    void onTerminalInfoClick(View view);
    void onOkicaInitializeClick(View view);
    void onPosAuthenticationClick(View view);
    void onPosClearClick(View view);
    void onTicketAuthenticationClick(View view);
    void onTicketClearClick(View view);
}
