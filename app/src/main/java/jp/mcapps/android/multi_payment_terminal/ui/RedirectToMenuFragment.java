package jp.mcapps.android.multi_payment_terminal.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import timber.log.Timber;

/**
 * POSモード時のメニュー -> メニューの画面遷移で
 * NavigationのpopUpToInclusiveが期待通りに動作しないため
 * 古い画面が残り続ける
 * スタックを消去するためリダイレクト画面を挟む
 */
public class RedirectToMenuFragment extends Fragment {
    public static RedirectToMenuFragment newInstance() {
        return new RedirectToMenuFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NavigationWrapper.navigate(this, R.id.action_redirect_to_menu, getArguments());
        Timber.v("onCreate");
    }
}
