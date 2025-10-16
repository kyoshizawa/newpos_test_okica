package jp.mcapps.android.multi_payment_terminal.ui.emoney.waon;

import android.view.View;

public interface EMoneyWaonEventHandlers {
    void onCancelClick(View view);
    void onConfirmationClick(View view);
    void onWithCashClick(View view);
    void onInversion(View view);
    void onPrintHistory(View view);
}

