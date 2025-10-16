package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.epson.epos2.cashchanger.CashChanger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentCashChangerPaymentBinding;
import jp.mcapps.android.multi_payment_terminal.devices.CashChangerDispenseError;
import jp.mcapps.android.multi_payment_terminal.devices.GloryCashChanger;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import timber.log.Timber;

public class CashChangerPaymentFragment extends BaseFragment implements CashChangerPaymentHandlers {

    public static CashChangerPaymentFragment newInstance() {
        return new CashChangerPaymentFragment();
    }

    private CashChangerPaymentViewModel _cashChangerPaymentActiveViewModel;
    private CashConfirmViewModel _cashConfirmViewModel;

    private final String SCREEN_NAME = "現金支払い確認画面";
    private SharedViewModel _sharedViewModel;
    private Handler _handler = new Handler(Looper.getMainLooper());
    private SoundManager _soundManager = SoundManager.getInstance();
    private final MainApplication _app = MainApplication.getInstance();

    TransLogger _transLogger = null;

    private int _amount = 0;

    private boolean isRepay = false;

    private int _over = 0;

    private int _cash = 0;

    private int _connectStatus = 0;

    private Map<String, Integer> _count = null;

    private boolean retEndDeposit = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentCashChangerPaymentBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_cash_changer_payment, container, false);

        _transLogger = new TransLogger();

        _cashChangerPaymentActiveViewModel = new ViewModelProvider(this).get(CashChangerPaymentViewModel.class);

        final Bundle args = getArguments();
        assert args != null;

        isRepay = args.getBoolean("isRepay");

        if (!isRepay) { // 支払の場合
            MainApplication.getInstance().setBusinessType(BusinessType.PAYMENT);
        } else { // 取消の場合
            MainApplication.getInstance().setBusinessType(BusinessType.REFUND);
            final Integer slipId = args.getInt("slipId");
            _transLogger.setRefundParam(slipId);
            _amount = _transLogger.getRefundAmount();
            _cashChangerPaymentActiveViewModel.setTotalPrice(_amount);
        }

        _cashChangerPaymentActiveViewModel.isRepay(isRepay);
        _cashChangerPaymentActiveViewModel.setFinishedMessage("");
        _cashChangerPaymentActiveViewModel.isFinished(false);

        // data binding
        binding.setHandlers(this);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(_sharedViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        binding.setViewModel(_cashChangerPaymentActiveViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        new Thread(() -> {
            _handler.post(() -> {
                _sharedViewModel.setLoading(true);
            });

            _cashChangerPaymentActiveViewModel.start(Amount.getTotalAmount(), isRepay);

//            _connectStatus = _cashChangerPaymentActiveViewModel.getConnectStatus().getValue();

            _handler.post(() -> {
                _sharedViewModel.setLoading(false);
            });
        }).start();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEnterBtn(View view, CashChangerPaymentViewModel viewModel) {
        if (isRepay) {
            CommonClickEvent.RecordClickOperation("取消", true);
        } else {
            CommonClickEvent.RecordClickOperation("決済", true);
        }
        new Thread(() -> {
            _handler.post(() -> {
                _sharedViewModel.setLoading(true);
            });

            if (isRepay) {
                _amount = _cashChangerPaymentActiveViewModel.getTotalPrice().getValue();
            } else {
                _amount = _cashChangerPaymentActiveViewModel.getPaymentAmount().getValue();
            }
            _over = _cashChangerPaymentActiveViewModel.getChangeAmount().getValue();
            _cash = _cashChangerPaymentActiveViewModel.getCashAmount().getValue();
            int config = _over > 0 ? CashChanger.DEPOSIT_CHANGE : CashChanger.DEPOSIT_NOCHANGE;

            GloryCashChanger gloryCashChanger = GloryCashChanger.getInstance();
            if (gloryCashChanger != null) {
                final Bundle args = getArguments();
                assert args != null;
                if (isRepay){
                    String moneyBrand = args.getString("moneyBrand");
                    if (moneyBrand.equals(_app.getString(R.string.money_brand_cash))) {
                        gloryCashChanger.dispense(view, _cashChangerPaymentActiveViewModel, _sharedViewModel, _amount, _over, isRepay, _amount, _transLogger, _cash).getOposErrorCode();
                    }
                } else {
                    retEndDeposit = gloryCashChanger.endDeposit(config, view, _sharedViewModel);
                    _over = _cashChangerPaymentActiveViewModel.getCashChangePay();
                    _cash = _cashChangerPaymentActiveViewModel.getCashAmountPay();

                    if (retEndDeposit) {
                        Timber.tag("GloryCashChanger").i("反映入金額" + _cash + "円");
                        if (_over > 0) {
                            gloryCashChanger.dispense(view, _cashChangerPaymentActiveViewModel, _sharedViewModel, _amount, _over, isRepay, _over, _transLogger, _cash).getOposErrorCode();
                        } else {
                            gloryCashChanger.print(view, _cashChangerPaymentActiveViewModel, _amount, _over, isRepay, _transLogger);
                        }
                    } else {
                        _handler.post(() -> {
                            NavigationWrapper.navigateUp(this);
                        });
                    }
                }
            }

            _handler.post(() -> {
                _sharedViewModel.setLoading(false);
            });
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCancelBtn(View view) {
        CommonClickEvent.RecordClickOperation("中止", true);

        new Thread(() -> {
            _handler.post(() -> {
                _sharedViewModel.setLoading(true);
            });

            GloryCashChanger gloryCashChanger = GloryCashChanger.getInstance();
            if (gloryCashChanger != null) {
                if (!isRepay) {
                    retEndDeposit = gloryCashChanger.endDeposit(CashChanger.DEPOSIT_REPAY, view, _sharedViewModel);
                } else {
                    // 取消時は無条件に disconnect を行う
                    retEndDeposit = true;
                }

                if (retEndDeposit) {
                    if (gloryCashChanger.disconnect() == false) {
                        // 何かできることがあるか？
                    }
                }
            }

            _handler.post(() -> {
                NavigationWrapper.navigateUp(this);
                _sharedViewModel.setLoading(false);
            });
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onHomeBtn(View view) {
        new Thread(() -> {
            _handler.post(() -> {
                _sharedViewModel.setLoading(true);
            });

            GloryCashChanger gloryCashChanger = GloryCashChanger.getInstance();
            if (gloryCashChanger != null) {
//                gloryCashChanger.endDeposit(CashChanger.DEPOSIT_REPAY, view);
                if (gloryCashChanger.disconnect() == false) {
                    // 何かできることがあるか？
                }
            }

            _handler.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_global_navigation_menu);
                _sharedViewModel.setLoading(false);
            });

//        _cashChangerPaymentActiveViewModel.start(Amount.getTotalAmount());
        }).start();
    }

}