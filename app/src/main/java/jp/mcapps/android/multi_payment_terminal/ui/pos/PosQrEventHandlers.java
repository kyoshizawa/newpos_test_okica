package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.view.View;

import androidx.fragment.app.Fragment;

public interface PosQrEventHandlers {
    void onReload(View view);
    void onCancelClick(View view);
}
