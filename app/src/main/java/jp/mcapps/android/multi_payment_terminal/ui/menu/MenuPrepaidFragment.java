package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuPrepaidBinding;

public class MenuPrepaidFragment extends BaseFragment {

    public static MenuPrepaidFragment newInstance() {
        return new MenuPrepaidFragment();
    }

    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "プリペイドメニュー";
    private final MainApplication _app = MainApplication.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final Fragment menuFragment = getParentFragment();
        final MenuViewModel menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        menuViewModel.setBodyType(MenuTypes.OTHERS);

        final FragmentMenuPrepaidBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_prepaid, container, false);

        binding.setViewModel(menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, menuViewModel));

        final FragmentActivity activity = getActivity();

        if (activity != null) {
            _sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);
            _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);
            binding.setSharedViewModel(_sharedViewModel);
        }

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }


//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修

}