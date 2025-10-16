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
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuBalanceBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuChargeBinding;

public class MenuChargeFragment extends BaseFragment {

    public static MenuChargeFragment newInstance() {
        return new MenuChargeFragment();
    }

    private final String SCREEN_NAME = "チャージメニュー";
    private MenuViewModel _menuViewModel;
    private SharedViewModel _sharedViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Fragment menuFragment = getParentFragment().getParentFragment();
        if(menuFragment == null) menuFragment = getParentFragment();
        _menuViewModel = new ViewModelProvider(menuFragment, MainApplication.getViewModelFactory()).get(MenuViewModel.class);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        _menuViewModel.setBodyType(MenuTypes.CHARGE);

        final FragmentMenuChargeBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_charge, container, false);

        binding.setViewModel(_menuViewModel);
        binding.setHandlers(new MenuEventHandlersImpl(this, _menuViewModel));

        _menuViewModel.setChargeMenu(true);

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

        if (activity != null && !AppPreference.isServicePos()) {
            _sharedViewModel.isActionBarLock(false);
            final ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setBackgroundDrawable(
                    getResources().getDrawable(R.color.menu_charge, null));
            _sharedViewModel.isActionBarLock(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        _menuViewModel.setChargeMenu(false);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            final SharedViewModel sharedViewModel = new ViewModelProvider(activity).get(SharedViewModel.class);
            sharedViewModel.setUpdatedFlag(true);
        }
    }
}