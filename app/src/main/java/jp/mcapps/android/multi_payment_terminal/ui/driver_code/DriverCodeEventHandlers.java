package jp.mcapps.android.multi_payment_terminal.ui.driver_code;

import android.view.View;

import androidx.fragment.app.Fragment;

public interface DriverCodeEventHandlers {
    void onInputNumber(String number);

    void onCorrection(View view);

    void onEnter(View view);

    void onPrevClick(View view);

    void onNextClick(View view);
}
