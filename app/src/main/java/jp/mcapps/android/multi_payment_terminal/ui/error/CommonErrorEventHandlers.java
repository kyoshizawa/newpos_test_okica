package jp.mcapps.android.multi_payment_terminal.ui.error;

import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;

public interface CommonErrorEventHandlers {
    void onPositiveClick(String errorCode);
    void onNegativeClick(String errorCode);
    void onNeutralClick(String errorCode);
    void onDismissClick(String errorCode);
}
