package jp.mcapps.android.multi_payment_terminal.ui.discount;
//ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修

import android.view.View;

import jp.mcapps.android.multi_payment_terminal.SharedViewModel;

public interface DiscountJobFutabaDEventHandlers {
    void onOkClick(View view , DiscountJobFutabaDViewModel discountJobFutabaDViewModel, SharedViewModel sharedViewModel);
    void onCancelClick(View view , DiscountJobFutabaDViewModel discountJobFutabaDViewModel, SharedViewModel sharedViewModel);
}
//ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修
