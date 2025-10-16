package jp.mcapps.android.multi_payment_terminal.ui.emoney.quicpay;

import android.view.View;

public interface EMoneyQuicPayEventHandlers {
    void onCancelClick(View view);
    void onConfirmationClick(View view);
    void onWithCashClick(View view);
    void onInversion(View view);
}

