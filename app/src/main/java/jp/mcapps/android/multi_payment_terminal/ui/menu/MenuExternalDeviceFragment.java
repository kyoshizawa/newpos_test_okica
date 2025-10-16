package jp.mcapps.android.multi_payment_terminal.ui.menu;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuBusinessBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuExternalDeviceBinding;
import jp.mcapps.android.multi_payment_terminal.ui.history.HistoryEventHandlersImpl;

public class MenuExternalDeviceFragment extends BaseFragment implements ExternalDeviceEventHandlers{
    private final String SCREEN_NAME = "外部機器メニュー";

    public static MenuExternalDeviceFragment newInstance() { return new MenuExternalDeviceFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final FragmentMenuExternalDeviceBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu_external_device, container, false);

        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onClickCashChanger(View view){
        CommonClickEvent.RecordButtonClickOperation(view, false);
        NavigationWrapper.navigate(this, R.id.action_navigation_menu_to_external_device_cash_changer);
    }
}