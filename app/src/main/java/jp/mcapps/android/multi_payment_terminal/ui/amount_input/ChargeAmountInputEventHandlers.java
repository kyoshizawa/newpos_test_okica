package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import android.view.View;

public interface ChargeAmountInputEventHandlers {
    void onInputNumber(View view, String number);

    void onCorrection(View view);

    void onEnter(View view);
}
