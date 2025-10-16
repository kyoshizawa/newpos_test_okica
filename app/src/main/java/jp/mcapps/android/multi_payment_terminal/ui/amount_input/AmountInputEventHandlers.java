package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import android.view.View;

public interface AmountInputEventHandlers {
    void onInputNumber(View view, String number);

    void onCorrection(View view);

    void onEnter(View view);

    void onIncrease(View view);

    void onDecrease(View view);

    void onFlatRate(View view);

    void onCancel(View view);

    void onReset(View view);

    void onChangeBack(View view);

    void onApply(View view);
}
