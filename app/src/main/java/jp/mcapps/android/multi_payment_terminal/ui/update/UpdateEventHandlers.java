package jp.mcapps.android.multi_payment_terminal.ui.update;

import android.view.View;

public interface UpdateEventHandlers {
    void onVersionClick(View view);
    void onOnlineUpdate(View view, boolean isPreUpdate);
    void onPrepaidAppOnlineUpdate(View view, boolean isPreUpdate);
    void onUSBUpdate(View view);
    void onClose(View view);
    void onVersionDetailClick(View view);
    void onCancelClick(View view);
    void onUpdateClick(View view);
}
