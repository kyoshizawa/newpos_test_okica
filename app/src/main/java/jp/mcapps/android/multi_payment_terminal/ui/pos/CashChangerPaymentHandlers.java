package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.view.View;

public interface CashChangerPaymentHandlers {
    void onEnterBtn(View view, CashChangerPaymentViewModel viewModel) ;
    void onCancelBtn(View view) ;
    void onHomeBtn(View view) ;
//    void onConfirmClick(View view);
}
