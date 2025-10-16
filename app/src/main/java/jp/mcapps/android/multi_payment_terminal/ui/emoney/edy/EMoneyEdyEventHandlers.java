package jp.mcapps.android.multi_payment_terminal.ui.emoney.edy;

import android.view.View;

public interface EMoneyEdyEventHandlers {
    void onCancelClick(View view);
    void onConfirmationClick(View view);
    void onWithCashClick(View view);
    void onInversion(View view);
    void onForceBalance(View view);
}

