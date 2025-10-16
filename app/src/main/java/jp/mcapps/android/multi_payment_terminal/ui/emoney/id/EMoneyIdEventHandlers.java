package jp.mcapps.android.multi_payment_terminal.ui.emoney.id;

import android.view.View;

import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputEventHandlers;

public interface EMoneyIdEventHandlers extends PinInputEventHandlers {
    void onCancelClick(View view);
    void onConfirmationClick(View view);
    void onWithCashClick(View view);
    void onInversion(View view);
}

