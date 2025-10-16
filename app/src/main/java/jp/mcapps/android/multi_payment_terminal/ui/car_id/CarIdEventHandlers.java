package jp.mcapps.android.multi_payment_terminal.ui.car_id;

import android.view.View;

public interface CarIdEventHandlers {
    void onInputNumber(String number);

    void onCorrection(View view);

    void onEnter(View view);
}
