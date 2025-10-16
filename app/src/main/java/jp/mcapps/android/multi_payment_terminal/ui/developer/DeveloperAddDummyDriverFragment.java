package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeveloperAddDummyDriverBinding;
import jp.mcapps.android.multi_payment_terminal.receiver.PickedDateTimeReceiver;

public class DeveloperAddDummyDriverFragment extends BaseFragment {
    private final String SCREEN_NAME = "ダミー乗務員追加";
    private DeveloperAddDummyDriverViewModel _viewModel;
    private PickedDateTimeReceiver _receiver;

    public static DeveloperAddDummyDriverFragment newInstance() {
        return new DeveloperAddDummyDriverFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        FragmentDeveloperAddDummyDriverBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_developer_add_dummy_driver, container, false);
        binding.setHandlers(new DeveloperEventHandlersImpl());

        _viewModel = new ViewModelProvider(this).get(DeveloperAddDummyDriverViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_viewModel);

        _receiver = new PickedDateTimeReceiver() {
            @Override
            protected void onPickedDateTimeReceive(Date pickedDateTime) {
                if (pickedDateTime != null) {
                    _viewModel.setCreatedAt(pickedDateTime);
                }
            }
        };

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        //BroadcastReceiverの解除
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(_receiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("SEND_PICKED_DATE");
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(_receiver, intentFilter);
    }
}
