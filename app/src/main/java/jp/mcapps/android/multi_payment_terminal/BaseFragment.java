package jp.mcapps.android.multi_payment_terminal;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

//import jp.mcapps.android.multi_payment_terminal.ui.credit_card.CreditCardScanFragment;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.EMoneyOkicaFragment;
import jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal.InstallationOkicaFragment;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuHomeFragment;
import jp.mcapps.android.multi_payment_terminal.ui.others.ValidationCheckFragment;
//import jp.mcapps.android.multi_payment_terminal.ui.others.WatariFragment;
//import jp.mcapps.android.multi_payment_terminal.ui.pos.CashChangerPaymentFragment;
//import jp.mcapps.android.multi_payment_terminal.ui.pos.CashConfirmFragment;
//import jp.mcapps.android.multi_payment_terminal.ui.pos_activation.PosActivationFragment;
import jp.mcapps.android.multi_payment_terminal.ui.qr.QRPaymentFragment;
import jp.mcapps.android.multi_payment_terminal.ui.qr.QRRefundFragment;
//import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketGateQrScanFragment;
//import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketIssueFragment;
import timber.log.Timber;

public abstract class BaseFragment extends Fragment {
    // 戻るボタンを表示させないフラグメント
    static final List<String> hideBackButtonFragments = new ArrayList<String>() {{

//        add(EMoneySuicaFragment.class.getSimpleName());
//        add(EMoneyIdFragment.class.getSimpleName());
//        add(EMoneyWaonFragment.class.getSimpleName());
//        add(EMoneyNanacoFragment.class.getSimpleName());
//        add(EMoneyQuicPayFragment.class.getSimpleName());
//        add(EMoneyEdyFragment.class.getSimpleName());
        add(MenuHomeFragment.class.getSimpleName());

        add(EMoneyOkicaFragment.class.getSimpleName());

        add(QRPaymentFragment.class.getSimpleName());
        add(QRRefundFragment.class.getSimpleName());
        add(ValidationCheckFragment.class.getSimpleName());
        add(InstallationOkicaFragment.class.getSimpleName());
//        add(PosActivationFragment.class.getSimpleName());
//        add(CashConfirmFragment.class.getSimpleName());
//        add(TicketGateQrScanFragment.class.getSimpleName());
//        add(TicketIssueFragment.class.getSimpleName());
//        add(WatariFragment.class.getSimpleName());
//        add(CashChangerPaymentFragment.class.getSimpleName());
    }};

    private SharedViewModel _sharedViewModel;

    @Override
    public void onStart() {
        super.onStart();

        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        sharedViewModel.setBackVisibleFlag(!hideBackButtonFragments.contains(getClass().getSimpleName()));

        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long max = Runtime.getRuntime().maxMemory();

        Timber.tag(getClass().getSimpleName()).d("メモリ使用量: %sByte, メモリ残量: %sByte, フリーメモリ: %s", total, (max-total), free);
    }
}
