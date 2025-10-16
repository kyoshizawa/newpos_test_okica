package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import android.view.View;

public interface AmountInputLt27EventHandlers {
    void onInputNumber(View view, String number);

    void onPayment(View view);

    void onCash(View view);

    void onReset(View view);

    void onChangeBack(View view);

}
