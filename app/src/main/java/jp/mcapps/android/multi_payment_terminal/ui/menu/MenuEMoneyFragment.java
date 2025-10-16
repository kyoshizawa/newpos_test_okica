package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuEmoneyBinding;

public class MenuEMoneyFragment extends BaseFragment {

    public static MenuEMoneyFragment newInstance() {
        return new MenuEMoneyFragment();
    }

    private final String SCREEN_NAME = "電マネメニュー";
    private SharedViewModel _sharedViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Fragment menuFragment = getParentFragment().getParentFragment();
        if(menuFragment == null) menuFragment = getParentFragment();

        final MenuViewModel menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        menuViewModel.setBodyType(MenuTypes.EMONEY);
        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final FragmentMenuEmoneyBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_emoney, container, false);

        binding.setViewModel(menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, menuViewModel));
        //ADD-S BMT S.Oyama 2024/09/12 フタバ双方向向け改修
        binding.setSharedViewModel(_sharedViewModel);
        //ADD-E BMT S.Oyama 2024/09/12 フタバ双方向向け改修

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

        final AppCompatActivity activity = (AppCompatActivity) view.getContext();

        if (activity != null) {
            _sharedViewModel.isActionBarLock(false);
            _sharedViewModel.setUpdatedFlag(true);
        }
    }
}