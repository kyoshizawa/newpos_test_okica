package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.view.View;

public interface CashConfirmHandlers {
    void onEnterBtn(View view, CashConfirmViewModel viewModel) ;
    void onCancelBtn(View view) ;
    void onHomeBtn(View view) ;
}
