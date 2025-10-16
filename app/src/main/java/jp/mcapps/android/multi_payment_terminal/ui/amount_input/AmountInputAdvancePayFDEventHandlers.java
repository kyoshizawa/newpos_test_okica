package jp.mcapps.android.multi_payment_terminal.ui.amount_input;
//ADD-S BMT S.Oyama 2024/08/27 フタバ双方向向け改修

import android.view.View;

public interface AmountInputAdvancePayFDEventHandlers {
    void onInputNumber(View view, String number);

    void onCorrection(View view);

    void onEnter(View view);

    void onAdvancepayFlatRate(View view);

    void onCancel(View view);

    void onReset(View view);

    void onChangeBack(View view);

    void onApply(View view);

    void onSeparation(View view);

    void onClear(View view);
}
//ADD-E BMT S.Oyama 2024/08/27 フタバ双方向向け改修
