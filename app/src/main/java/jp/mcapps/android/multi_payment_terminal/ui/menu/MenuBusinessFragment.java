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
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuBusinessBinding;

public class MenuBusinessFragment extends BaseFragment {

    public static MenuBusinessFragment newInstance() {
        return new MenuBusinessFragment();
    }

    private final String SCREEN_NAME = "業務メニュー";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final Fragment menuFragment = getParentFragment().getParentFragment();
        final MenuViewModel menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        FragmentActivity activity = getActivity();
        final SharedViewModel sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);

        menuViewModel.setBodyType(MenuTypes.BUSINESS);

        final FragmentMenuBusinessBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_business, container, false);

        binding.setViewModel(menuViewModel);
        binding.setSharedViewModel(sharedViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, menuViewModel));

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
}