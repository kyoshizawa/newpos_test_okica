package jp.mcapps.android.multi_payment_terminal.ui.discount;
//ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修

import android.view.View;

public interface DiscountFutabaDEventHandlers {
    void onTestClick(View view , DiscountFutabaDViewModel discountFutabaDViewModel);
    void onCancelClick(View view , DiscountFutabaDViewModel discountFutabaDViewModel);
}
//ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修
