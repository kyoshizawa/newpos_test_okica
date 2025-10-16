package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.pos.device.beeper.Beeper;

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
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentEmoneyOkicaBinding;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.ui.PostPaymentProcess;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.ActionBarController;
import timber.log.Timber;

public class EMoneyOkicaFragment extends BaseFragment implements EMoneyOkicaEventHandlers {
    public static EMoneyOkicaFragment newInstance() {
        return new EMoneyOkicaFragment();
    }

    private final String SCREEN_NAME = "OKICA";

    private MainApplication app = MainApplication.getInstance();
    private Handler _handler = new Handler(Looper.getMainLooper());
    private BaseEMoneyOkicaViewModel _eMoneyOkicaViewModel;
    private SharedViewModel _sharedViewModel;
    private ActionBarController _actionBarController;
    private boolean _isNoEndError = false;   // 処理未了フラグ

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        MainApplication.getInstance().setErrorCode(null);

        final FragmentEmoneyOkicaBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_emoney_okica, container, false);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            _sharedViewModel =
                    new ViewModelProvider(activity).get(SharedViewModel.class);

            binding.setSharedViewModel(_sharedViewModel);
        }

        _eMoneyOkicaViewModel = AppPreference.isDemoMode()
            ? new ViewModelProvider(this).get(DemoEMoneyOkicaViewModel.class)
            : new ViewModelProvider(this).get(EMoneyOkicaViewModel.class);

        binding.setViewModel(_eMoneyOkicaViewModel);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        //
        ((EMoneyOkicaViewModel)_eMoneyOkicaViewModel).getToastMessage().observe(getViewLifecycleOwner(), message -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            try {
                Beeper.getInstance().beepNonBlock(2700, 40);
            } catch (Exception x) {
                x.printStackTrace();
            }
        });

        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        _actionBarController = new ActionBarController(this, _sharedViewModel);
        _actionBarController.setStatus(
                ActionBarController.ControlCodes.OFF,
                ActionBarController.ColorCodes.NONE,
                0);
        _actionBarController.setShowIndicatorArrow(true);

        if (app.getBusinessType() == BusinessType.PAYMENT) {
            final String amount = Converters.integerToNumberFormat(Amount.getFixedAmount());
            ((TextView) requireView().findViewById(R.id.text_emoney_head_amount)).setText(
                    amount + MainApplication.getInstance().getString(R.string.yen));
            _eMoneyOkicaViewModel.withdrawal();
        }
        else if (app.getBusinessType() == BusinessType.BALANCE) {
            _eMoneyOkicaViewModel.balance();
        }
        else if (app.getBusinessType() == BusinessType.REFUND) {
            final Bundle args = getArguments();
            int slipId = args.getInt("slipId");

            final SlipData[] slipData = {null};
            Thread thread = new Thread(() -> {
                slipData[0] = DBManager.getSlipDao().getOneById(slipId);
            });
            thread.start();

            try {
                thread.join();
                final String amount = Converters.integerToNumberFormat(slipData[0].transAmount);
                ((TextView) requireView().findViewById(R.id.text_emoney_head_amount)).setText(
                        amount + MainApplication.getInstance().getString(R.string.yen));
                _eMoneyOkicaViewModel.refund(slipId);
            } catch (InterruptedException e) {
                Timber.e(e);
            }
        }
        else if (app.getBusinessType() == BusinessType.CHARGE) {
            final Bundle args = getArguments();
            int amount = args.getInt("amount");

            ((TextView) requireView().findViewById(R.id.text_emoney_head_amount)).setText(
                    Converters.integerToNumberFormat(amount) + MainApplication.getInstance().getString(R.string.yen));

            _eMoneyOkicaViewModel.charge(amount);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _eMoneyOkicaViewModel.cleanup();
        _actionBarController.cleanup();

        // アクションバーの状態戻すため
        _sharedViewModel.setUpdatedFlag(true);
        _sharedViewModel.setScreenInversion(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle params = new Bundle();
        _eMoneyOkicaViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case None:
                    _sharedViewModel.setLoading(true);
                case Waiting:
                    _sharedViewModel.setLoading(false);
                case Unprocessed:
                    _actionBarController.setStatus(0x02, 0x01, 0x00);
                    _actionBarController.setShowIndicatorArrow(true);
                    break;
                case Processing:
                    _actionBarController.setStatus(0x01, 0x01, 0x00);
                    _actionBarController.setShowIndicatorArrow(false);
                    break;
                case Success:
                    _actionBarController.setStatus(0x01, 0x01, 0x00);
                    _actionBarController.setShowIndicatorArrow(false);
//                    _handler.postDelayed(() -> {
//                        if (_eMoneyOkicaViewModel.getSlipId() != null) {
//                            // ないとは思うけど5秒以内にインサートが終わらないと伝票が出力されない
//                            params.putInt("slipId", _eMoneyOkicaViewModel.getSlipId());
//                        }
//                        NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu, params);
//                    }, 5_000);

                    // もっかいやる！
                    _eMoneyOkicaViewModel.withdrawal();

                    break;
                case SuccessBalance:
                    _actionBarController.setStatus(0x01, 0x01, 0x00);
                    _actionBarController.setShowIndicatorArrow(false);

                    // もっかいやる！
                    _eMoneyOkicaViewModel.balance();

                    break;
                case Error:
                    _actionBarController.setStatus(0x01, 0x02, 0x00);
                    _actionBarController.setShowIndicatorArrow(false);
                    _handler.postDelayed(() -> {
                        // ないとは思うけど5秒以内にインサートが終わらないと伝票が出力されない
                        if (_eMoneyOkicaViewModel.getSlipId() != null) {
                            params.putInt("slipId", _eMoneyOkicaViewModel.getSlipId());
                            _isNoEndError = true;
                        }
                        // NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu, params);
                        navigateToMenuOrPopBack(params);
                    }, 5_000);
                    break;
                case NoOpTimeout:
                    _actionBarController.setShowIndicatorArrow(false);
                    // NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu);
                    navigateToMenuOrPopBack(null);
                    break;
                case Canceling:
                    _sharedViewModel.setLoading(true);
                    break;
                case Canceled:
                    _sharedViewModel.setLoading(false);
                    _actionBarController.setShowIndicatorArrow(false);
                    //ADD-S BMT S.Oyama 2024/09/20 フタバ双方向向け改修
                    //PrinterManager printerManager2 = PrinterManager.getInstance();
                    //printerManager2.send820_FunctionCodeErrorResult(IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY , false);      //820へ決済中止を通知
                    //
                    //if(_eMoneyOkicaViewModel.isfromSeparationAmount().getValue() == false) {                          // 分別払い画面から遷移していない場合
                    //    Amount.setFlatRateAmount(0);        // 分別払いからの遷移でないときは定額金額をクリア
                    NavigationWrapper.popBackStack(this);
                    //}
                    //else                                                            // 分別払い画面から遷移している場合
                    //{
                    //    backSeparation();
                    //}
                    //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修
                    break;
                case InsufficientBalance:
                case WithCashOrCancel:
                    _actionBarController.setStatus(0x01, 0x02, 0x00);
                    _actionBarController.setShowIndicatorArrow(false);
                    break;
                case HistoryInquiry:
                    BaseEMoneyOkicaViewModel.HistoryData historyData = _eMoneyOkicaViewModel.getHitoryData();

                    PrinterManager printerManager = PrinterManager.getInstance();
                    printerManager.print_trans_history_okica(view, historyData);
                    // NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu);
                    navigateToMenuOrPopBack(null);
                    break;
                case HistoryInquiryCancel:
                case InsufficientBalanceCancel:
                    // NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu);
                    navigateToMenuOrPopBack(null);
                    break;
                default:
                    break;
            }
        });

    }

    //ADD-S BMT S.Oyama 2024/09/20 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  画面を戻す処理（前画面が分別モード時向け）
     * @note   画面を戻す処理　前画面が分別モード時向け
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void backSeparation() {
        NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu);
    }
    //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修


    @Override
    public void onCancelClick(View view) {
        // かざし待ちか現金併用選択以外でコールされた場合は何もしない (ボタン押しっぱなし対策)
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _eMoneyOkicaViewModel.cancel();
    }

    @Override
    public void onWithCashClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        int cardAmount = 0;
        if (AppPreference.isDemoMode()) {
            cardAmount = _eMoneyOkicaViewModel.getBalance();
        } else {
            cardAmount = AppPreference.isWishcash1yenEnabled()
                    ?_eMoneyOkicaViewModel.getBalance()
                    :_eMoneyOkicaViewModel.getBalance()/10*10;
        }

        final int cashAmount = Amount.getFixedAmount() - _eMoneyOkicaViewModel.getBalance();

        ((TextView) requireView().findViewById(R.id.text_emoney_head_amount)).setText(
                Converters.integerToNumberFormat(cardAmount) + MainApplication.getInstance().getString(R.string.yen));

        _eMoneyOkicaViewModel.withdrawal();
    }

    @Override
    public void onInversion(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        if (_sharedViewModel != null) {
            _sharedViewModel.inverseScreen();
        }
    }

    @Override
    public void onHistoryPrintClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _eMoneyOkicaViewModel.setState(BaseEMoneyOkicaViewModel.States.HistoryInquiry);
    }

    private void navigateToMenuOrPopBack(@Nullable Bundle params) {
        if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {

            if (AppPreference.isTicketTransaction() && _isNoEndError) {
                // チケット購入の決済または取消時の処理未了発生した場合、トップメニュー画面に遷移
                NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu, params);
                return;
            }

            // 伝票印刷
            Integer slipId = null;
            if (params != null && params.containsKey("slipId")) {
                slipId = params.getInt("slipId");
            }
            PostPaymentProcess.getInstance().execute(requireActivity(), slipId);

            // POSの時は、直前の画面に戻る
            NavigationWrapper.popBackStack(this);
        } else {
            NavigationWrapper.navigate(this, R.id.action_navigation_emoney_okica_to_navigation_menu, params);
        }
    }
}
