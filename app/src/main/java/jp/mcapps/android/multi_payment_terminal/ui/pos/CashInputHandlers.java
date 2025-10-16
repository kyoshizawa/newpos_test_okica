package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.view.View;

public interface CashInputHandlers {
    //金額確定ボタン
    void onEnterBtn(View view , CashInputViewModel cashInputViewModel);
    //取消ボタン
    void onBackDelete(View view , CashInputViewModel cashInputViewModel);
    //クリアボタン
    void onClearBtn(View view , CashInputViewModel cashInputViewModel);
    //数字ボタン
    void onInputNumber(CashInputViewModel cashInputViewModel , String number);
}
