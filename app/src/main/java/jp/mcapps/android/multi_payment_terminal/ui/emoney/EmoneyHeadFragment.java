package jp.mcapps.android.multi_payment_terminal.ui.emoney;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentEmoneyHeadBinding;

public class EmoneyHeadFragment extends Fragment {

    public static EmoneyHeadFragment newInstance() {
        return new EmoneyHeadFragment();
    }

    private EmoneyHeadViewModel _emoneyHeadViewModel;
    private BroadcastReceiver _receiver;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _emoneyHeadViewModel = new ViewModelProvider(this).get(EmoneyHeadViewModel.class);

        final FragmentEmoneyHeadBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_emoney_head, container, false);

        binding.setViewModel(_emoneyHeadViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            final SharedViewModel sharedViewModel =
                    new ViewModelProvider(activity).get(SharedViewModel.class);

            binding.setSharedViewModel(sharedViewModel);
        }

        int[] radioImageResources = {R.drawable.ic_radio_level_low, R.drawable.ic_radio_level_middle, R.drawable.ic_radio_level_high, R.drawable.ic_airplane_mode};
        _emoneyHeadViewModel.setRadioImageResource(AppPreference.isDemoMode()
                ? radioImageResources[2]                                //デモモードは強固定
                : radioImageResources[CurrentRadio.getImageLevel()]);   //電波レベルの初期画像
        _receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                _emoneyHeadViewModel.setRadioImageResource(radioImageResources[CurrentRadio.getImageLevel()]);
            }
        };

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

    @Override
    public void onPause() {
        if (!AppPreference.isDemoMode()){
            LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(_receiver); //通常モードのみ電波レベルの変更通知を受け取るためここで解除
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (!AppPreference.isDemoMode()) {
            IntentFilter intentFilter = new IntentFilter("CHANGE_RADIO_LEVEL_IMAGE");
            LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(_receiver, intentFilter); //通常モードのみ電波レベルの変更通知を受け取る
        }
        super.onResume();
    }
}