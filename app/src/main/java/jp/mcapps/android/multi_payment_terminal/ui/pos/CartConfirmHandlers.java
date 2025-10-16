package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.view.View;

import jp.mcapps.android.multi_payment_terminal.SharedViewModel;

public interface CartConfirmHandlers {
    void navigateToPayment(View view , CartConfirmViewModel cartConfirmViewModel , SharedViewModel sharedViewModel);
    void navigateToManual(View view);
}
