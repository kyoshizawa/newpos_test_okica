package jp.mcapps.android.multi_payment_terminal.ui.qr;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.ActionBarColors;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentQrBinding;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import timber.log.Timber;

public class QRRefundFragment extends BaseFragment implements QREventHandlers {
    private final String SCREEN_NAME = "QR返金";
    private static final int MESSAGE_DISPLAY_TIME_MS = 5000;

    private MainApplication _app = MainApplication.getInstance();
    private QRViewModel _qrViewModel;
    private SharedViewModel _sharedViewModel;
    private CommonHeadViewModel _commonHeadViewModel;
    private Handler _handler = new Handler(Looper.getMainLooper());

    public static QRRefundFragment newInstance() {
        return new QRRefundFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        final FragmentQrBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_qr, container, false);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        _qrViewModel = new ViewModelProvider(this).get(QRViewModel.class);

        binding.setViewModel(_qrViewModel);
        binding.setSharedViewModel(_sharedViewModel);
        binding.setHandlers(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);
        binding.setCommonHeadViewModel(_commonHeadViewModel);
        getLifecycle().addObserver(_commonHeadViewModel);

        _qrViewModel.setResult(TransactionResults.None);
        _qrViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == TransactionResults.None) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
            }
            else if (result == TransactionResults.SUCCESS) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Success);
            }
            else if (result == TransactionResults.FAILURE) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Error);
            }
            else if (result == TransactionResults.UNKNOWN) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Unknown);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();

        int slipId = args.getInt("slipId");

        final SlipData[] slipData = {null};
        Thread thread = new Thread(() -> {
            slipData[0] = DBManager.getSlipDao().getOneById(slipId);
        });
        thread.start();

        try {
            thread.join();

            String orderId = slipData[0].codetransOrderId;
            final int fee = slipData[0].transAmount;
            final String payName = slipData[0].transBrand;

            _commonHeadViewModel.setAmount(fee);

            _qrViewModel.getFinishedMessage().observe(getViewLifecycleOwner(), msg -> {
                if (msg != null && !msg.equals("")) {
                    _handler.removeCallbacksAndMessages(null);

                    final Bundle params = new Bundle();
                    params.putInt("slipId", _qrViewModel.getSlipId());

                    if (_qrViewModel.getResult().getValue() != TransactionResults.UNKNOWN) {
                        _handler.postDelayed(() -> {
                            _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                            NavigationWrapper.navigate(this, R.id.action_navigation_qr_refund_to_navigation_menu, params);
                        }, MESSAGE_DISPLAY_TIME_MS);
                    }
                }
            });
            new Thread(() -> {
                _qrViewModel.refund(orderId, fee, payName, slipId);
            }).start();

        } catch (InterruptedException e) {
            Timber.e(e); }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCheckResultClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        final Bundle args = getArguments();
        int oldSlipId = args.getInt("slipId");

        new Thread(() -> {
            _qrViewModel.checkRefundResult(oldSlipId);
        }).start();
    }

    @Override
    public void onCancelClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        _app.setErrorCode(_app.getString(R.string.error_type_qr_3320));

        // 処理未了の売上データ生成と印刷
        _qrViewModel.createUnfinishedTransLogger();
        final Bundle params = new Bundle();
        params.putInt("slipId", _qrViewModel.getSlipId());

        _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
        NavigationWrapper.navigate(this, R.id.action_navigation_qr_refund_to_navigation_menu, params);
    }
}