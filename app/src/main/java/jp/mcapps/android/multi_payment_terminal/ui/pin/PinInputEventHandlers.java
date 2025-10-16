package jp.mcapps.android.multi_payment_terminal.ui.pin;

import android.view.View;

public interface PinInputEventHandlers {
    void onInputNumber(View view, String number);

    void onCorrection(View view);

    void onEnter(View view);

    void onCancel(View view);
}
