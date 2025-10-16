package jp.mcapps.android.multi_payment_terminal.ui.emoney.nanaco;

import android.view.View;

public interface EMoneyNanacoEventHandlers {
    void onCancelClick(View view);
    void onConfirmationClick(View view);
    void onWithCashClick(View view);
    void onInversion(View view);
}

