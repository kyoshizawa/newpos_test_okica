package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.view.View;
import androidx.fragment.app.Fragment;

import jp.mcapps.android.multi_payment_terminal.SharedViewModel;

public interface PosEventHandlers {
    // メイン画面へ移動
    void navigateMain(View view, SharedViewModel sharedViewModel);

    // 商品選択画面へ移動
    void navigateToProductSelect(View view);

    // QRスキャン画面へ移動
    void navigateToQR(View view);

    // カート確認画面へ移動
    void navigateToCartConfirm(View view);

    void navigateFromManualToCartConfirm(View view);

    // カート内数値入力画面（数量の変更）へ移動
    void navigateToInputCartCount(Fragment fragment, Integer product_id, Integer count);

    // 単価または数量変更選択画面へ移動
    void navigateToSelectInputType(Fragment fragment, CartModel item);

    // 商品選択画面から手動明細画面へ移動
    void navigateFromProductSelectToManualInput(View view);

    void navigateUp(View view);
}
