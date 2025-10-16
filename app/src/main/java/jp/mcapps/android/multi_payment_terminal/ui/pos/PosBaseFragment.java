package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import timber.log.Timber;

public abstract class PosBaseFragment extends Fragment {
    //デフォルトが表示なので非表示をいれる
    static final List<String> HideHomeButtonFragments = new ArrayList<String>() {{
    }};
    //デフォルトは非表示
    static final List<String> VisibleQRScanButtonFragments = new ArrayList<String>() {{
        add(CartConfirmFragment.class.getSimpleName());
        add(ProductSelectFragment.class.getSimpleName());
    }};
    static final List<String> VisibleSearchButtonFragments = new ArrayList<String>() {{
    }};
    static final List<String> VisibleCartConfirmButtonFragments = new ArrayList<String>() {{
        add(CartManualInputFragment.class.getSimpleName());
    }};
    static final List<String> HiddenNavUpButtonFragments = new ArrayList<String>() {{
        add(ProductSelectFragment.class.getSimpleName());
        add(CartConfirmFragment.class.getSimpleName());
    }};

    PosViewModel posViewModel;
    SharedViewModel sharedViewModel;


    @Override
    public void onStart() {
        super.onStart();
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        posViewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);
        sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.POS);
        posViewModel.setHomeVisible(!HideHomeButtonFragments.contains(getClass().getSimpleName()));
        posViewModel.setQRScanVisible(VisibleQRScanButtonFragments.contains(getClass().getSimpleName()));
        posViewModel.setSearchVisible(VisibleSearchButtonFragments.contains(getClass().getSimpleName()));
        posViewModel.setCartConfirmVisible(VisibleCartConfirmButtonFragments.contains(getClass().getSimpleName()));
        posViewModel.setNavigateUpVisible(!HiddenNavUpButtonFragments.contains(getClass().getSimpleName()));

        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long max = Runtime.getRuntime().maxMemory();

        Timber.tag(getClass().getSimpleName()).d("メモリ使用量: %sByte, メモリ残量: %sByte, フリーメモリ: %s", total, (max-total), free);
    }
}
