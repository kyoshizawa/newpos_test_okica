package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import android.view.View;

public interface EMoneyOkicaEventHandlers {
    void onCancelClick(View view);
    void onWithCashClick(View view);
    void onInversion(View view);
    void onHistoryPrintClick(View view);
}
