package jp.mcapps.android.multi_payment_terminal.ui.ticket_activation;

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
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketActivationBinding;

public class TicketActivationFragment extends BaseFragment {
    private TicketActivationViewModel _ticketActivationViewModel;

    public static TicketActivationFragment newInstance() {
        return new TicketActivationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final FragmentTicketActivationBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_activation, container, false);

        _ticketActivationViewModel = new ViewModelProvider(this).get(TicketActivationViewModel.class);

        binding.setViewModel(_ticketActivationViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        _ticketActivationViewModel.start();

        binding.setHandlers(new TicketActivationEventHandlersImpl(this));

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
        _ticketActivationViewModel.stop();
    }
}
