package jp.mcapps.android.multi_payment_terminal.ui.qr;

import android.view.View;

import androidx.fragment.app.Fragment;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuViewModel;

public interface QREventHandlers {
    void onCheckResultClick(View view);
    void onCancelClick(View view);
}
