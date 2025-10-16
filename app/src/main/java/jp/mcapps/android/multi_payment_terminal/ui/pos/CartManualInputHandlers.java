package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.view.View;

import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;

public interface CartManualInputHandlers {
    //金額確定ボタン
    void onEnterBtn(View view , CartManualInputViewModel cartManualInputViewModel);
    //取消ボタン
    void onBackDelete(View view , CartManualInputViewModel cartManualInputViewModel);
    //クリアボタン
    void onClearBtn(View view , CartManualInputViewModel cartManualInputViewModel);
    //数字ボタン
    void onInputNumber(CartManualInputViewModel cartManualInputViewModel , String number);

    void onRedycedBtn(CartManualInputViewModel cartManualInputViewModel , ReducedTaxTypes reducedTaxTypes);
    void onIncludedBtn(CartManualInputViewModel cartManualInputViewModel , IncludedTaxTypes includedTaxTypes);
}
