package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeCodes;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeNameMap;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentReceiptIssueBinding;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;

public class HistoryReceiptIssueFragment extends BaseFragment{

    private final String SCREEN_NAME = "領収書選択画面";

    private HistoryReceiptIssueViewModel _receiptIssueViewModel;

    private HistoryEventHandlers _historyEventHandlers;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentReceiptIssueBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_receipt_issue, container, false);

        _historyEventHandlers = new HistoryEventHandlersImpl(this);
        binding.setHandlers(_historyEventHandlers);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        _receiptIssueViewModel =  new ViewModelProvider(this).get(HistoryReceiptIssueViewModel.class);
        _receiptIssueViewModel.setIsDetail(false);
        binding.setViewModel(_receiptIssueViewModel);

        boolean isLt27 = IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D);
        final boolean[] isLt27Reprinting = {true};

        final Bundle args = getArguments();
        Integer slipId = args.getInt("slipId");

        final SlipData[] slipData = {null, null};
        Thread thread = new Thread(() -> {
            SlipDao dao = DBManager.getSlipDao();
            slipData[0] = dao.getOneById(slipId);
            if (isLt27) {
                // LT-27双方向の場合は直前取引も取得
                slipData[1] = dao.getLatestOne();
                if (slipData[1].transDate.compareTo(AppPreference.getDatetimeLt27Printable()) <= 0) {
                    // 直前取引が再印字不可になっている場合、取消も不可にする
                    if (slipData[1].cancelFlg != null) {
                        dao.updateCancelUriId(slipData[1].id);
                    }
                    isLt27Reprinting[0] = false;
                }
            }
        });
        thread.start();

        try {
            thread.join();
            binding.setSlipData(slipData[0]);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }
}
