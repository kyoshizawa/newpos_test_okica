package jp.mcapps.android.multi_payment_terminal.ui.pos_activation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentPosActivationBinding;

public class PosActivationFragment extends BaseFragment{

    private PosActivationViewModel _posActivationViewModel;

    public static PosActivationFragment newInstance() {
        return new PosActivationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final FragmentPosActivationBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pos_activation, container, false);

        _posActivationViewModel = new ViewModelProvider(this).get(PosActivationViewModel.class);

        binding.setViewModel(_posActivationViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        _posActivationViewModel.start();

        binding.setHandlers(new PosActivationEventHandlersImpl(this));

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
        _posActivationViewModel.stop();
    }
}
