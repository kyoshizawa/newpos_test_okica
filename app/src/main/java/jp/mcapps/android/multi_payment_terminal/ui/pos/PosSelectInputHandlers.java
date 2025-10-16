package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.fragment.app.Fragment;

public interface PosSelectInputHandlers {
    // 数量変更ボタン
    void clickCountChange(PosSelectInputViewModel posSelectInputViewModel);

    // 単価変更ボタン
    void clickUnitPriceChange(PosSelectInputViewModel posSelectInputViewModel);

    // 単価リセットボタン
    void clickUnitPriceReset(PosSelectInputViewModel posSelectInputViewModel);

    // 商品削除ボタン
    void clickUnitDeleteProduct(PosSelectInputViewModel posSelectInputViewModel);

    // 商品名表示
    String getProductName(PosSelectInputViewModel posSelectInputViewModel);
}
