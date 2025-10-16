package jp.mcapps.android.multi_payment_terminal.ui.device_check;

import android.view.View;

public interface DeviceCheckEventHandlers {
    void checkGps(View view);

    void checkSim(View view);

    void checkWifi(View view);

    void clearResult(View view);

    void checkCamera(View view);

    void checkFelica(View view);

    void checkTerminal(View view);

    void checkPrinter(View view);

    void checkIc(View view);

    void checkContactless(View view);

    void checkMs(View view);

    void checkMeter(View view);

    void checkSam(View view);
}
