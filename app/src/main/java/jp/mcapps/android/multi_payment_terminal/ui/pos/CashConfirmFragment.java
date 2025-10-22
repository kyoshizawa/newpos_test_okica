package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.pos.device.uart.SerialPort;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentCashConfirmBinding;
import jp.mcapps.android.multi_payment_terminal.model.CashChecker;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
//import jp.mcapps.android.multi_payment_terminal.model.OptionalTicketTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedConfirm;
import timber.log.Timber;

public class CashConfirmFragment  extends BaseFragment implements CashConfirmHandlers {
    public static CashConfirmFragment newInstance(){
        return new CashConfirmFragment();
    }
    private final String SCREEN_NAME = "現金支払/取消の確認";
    private final String SCREEN_NAME_POSTAL_ORDER = "為替類支払/取消の確認";
    private SharedViewModel _sharedViewModel;
    private Handler _handler = new Handler(Looper.getMainLooper());
    private SoundManager _soundManager = SoundManager.getInstance();

    TransLogger _transLogger = null;

    private int _amount = 0;

    private boolean isRepay = false;

    private boolean isFixedAmountPostalOrder = false;

    private int _over = 0;

    private String _purchasedTicketDealId = null;

    private boolean _isABTCancelSuccess = false;
    private static int _errorCode = 0;
    private boolean _isProcessing = false;

    private String _msg = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final FragmentCashConfirmBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_cash_confirm, container, false);
        _transLogger = new TransLogger();

        // viewModel from viewModelProvider
        CashConfirmViewModel _cashConfirmViewModel = new ViewModelProvider(requireActivity()).get(CashConfirmViewModel.class);

        final Bundle args = getArguments();

        Integer deposit = 0;

        assert args != null;
        isRepay = args.getBoolean("isRepay");
        isFixedAmountPostalOrder = args.getBoolean("isFixedAmountPostalOrder", false);

        if (!isRepay) { // 支払の場合
            MainApplication.getInstance().setBusinessType(BusinessType.PAYMENT);
            deposit = args.getInt("deposit");
            _over = args.getInt("over");
            _amount = Amount.getTotalAmount();
        } else { // 取消の場合
            MainApplication.getInstance().setBusinessType(BusinessType.REFUND);
            final Integer slipId = args.getInt("slipId");
            _purchasedTicketDealId = args.getString("purchasedTicketDealId");
            _transLogger.setRefundParam(slipId);
            _amount = _transLogger.getRefundAmount();
        }

        _cashConfirmViewModel.setDeposit(deposit);
        _cashConfirmViewModel.setOver(_over);
        // _cashConfirmViewModel.fetchTotalPrice(); // 最新取引の金額をとってくるため修正
        _cashConfirmViewModel.setTotalPrice(_amount);
        _cashConfirmViewModel.isRepay(isRepay);

        _cashConfirmViewModel.setFinishedMessage("");
        _cashConfirmViewModel.isFinished(false);

        // data binding
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_cashConfirmViewModel);
        binding.setHandlers(this);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(_sharedViewModel);

        if(isFixedAmountPostalOrder) {
            ScreenData.getInstance().setScreenName(SCREEN_NAME_POSTAL_ORDER);
        } else {
            ScreenData.getInstance().setScreenName(SCREEN_NAME);
        }

        return binding.getRoot();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEnterBtn(View view, CashConfirmViewModel viewModel) {
        if (isRepay) {
            CommonClickEvent.RecordClickOperation("取消", true);
        } else {
            CommonClickEvent.RecordClickOperation("決済", true);
        }

        if(AppPreference.getIsCashDrawerTypeDonly() || AppPreference.getIsCashDrawerTypeAll()) {
            SerialPort serialPort = null;
            try {
                serialPort = SerialPort.getInstance("9600,8,n,1", SerialPort.TTY_USB0); //USB
//                serialPort = SerialPort.getInstance("9600,8,n,1", SerialPort.TTY_ACM0); //LAN
            } catch (Exception e) {

            }
            if (serialPort == null) {
                Log.e("Drawer", "null");
            } else {
                OutputStream os = serialPort.getOutputStream();
                if (os != null) {
                    try {
                        os.write("U".getBytes());
                        os.flush();
                        Timber.tag("Drawer").d("send data");
                    } catch (IOException e) {
                        e.printStackTrace();

                        Timber.tag("Drawer").d("send error data");
                    }
                } else {
                    Log.e("Drawer", "OutputStream == null");
                }
                serialPort.release();
            }
        }


        Log.d("enter--------", "came");
        Log.d("_isProcessing", String.valueOf(_isProcessing));
        if (_isProcessing) return;
        Date exDate = new Date();   // 2．日付（今回は現在の日時）を取得
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        String payTime = dateFmt.format(exDate);

        _msg = "";

        Log.d("_isProcessing", "->true");
        _isProcessing = true;

        if (AppPreference.isTicketTransaction() && isRepay) {
            _isABTCancelSuccess = false;
            _errorCode = 0;

            if (_purchasedTicketDealId == null || _purchasedTicketDealId.equals("")) {
                // チケット購入IDが存在しない場合、取消確認不要
                Timber.i("チケット取消確認不要(%s)", _purchasedTicketDealId);
            } else {
                // チケットの取消時はABTセンターで取消ができるか確認が必要
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final TicketSalesApi ticketSalesApiClient = TicketSalesApiImpl.getInstance();
                        final TerminalDao terminalDao = LocalDatabase.getInstance().terminalDao();
                        final TerminalData terminalData = terminalDao.getTerminal();

                        try {
                            // ABTに取消確認実施
                            TicketPurchasedConfirm.Response confirmResponse = ticketSalesApiClient.TicketPurchasedConfirm(terminalData.service_instance_abt, _purchasedTicketDealId);
                            _isABTCancelSuccess = true;

                        } catch (TicketSalesStatusException e) {
                            Timber.e(e);
                            _errorCode = e.getCode();
                            if (404 == _errorCode || 4007 == _errorCode) {
                                _isABTCancelSuccess = true;
                            }
                        } catch (HttpStatusException e) {
                            Timber.e(e);
                            _errorCode = e.getStatusCode();
                            if (404 == _errorCode || 4007 == _errorCode) {
                                _isABTCancelSuccess = true;
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                            _errorCode = 99999;
                        }
                    }
                });
                thread.start();

                try {
                    thread.join();

                    if (!_isABTCancelSuccess) {
                        Resources resources = MainApplication.getInstance().getResources();
                        if (4008 == _errorCode) {
                            _msg = resources.getString(R.string.error_message_ticket_8161) + "\n" + resources.getString(R.string.error_detail_ticket_8161);
                        } else if (4009 == _errorCode) {
                            _msg = resources.getString(R.string.error_message_ticket_8162) + "\n" + resources.getString(R.string.error_detail_ticket_8162);
                        } else if (4010 == _errorCode) {
                            _msg = resources.getString(R.string.error_message_ticket_8163) + "\n" + resources.getString(R.string.error_detail_ticket_8163);
                        } else if (99999 == _errorCode) {
                            _msg = resources.getString(R.string.error_message_ticket_8097) + "\n" + resources.getString(R.string.error_detail_ticket_8097);
                        } else {
                            _msg = resources.getString(R.string.error_message_ticket_8160) + "\n" + String.format(resources.getString(R.string.error_detail_ticket_8160), String.valueOf(_errorCode));
                        }
                        viewModel.setFinishedMessage(_msg);
                        viewModel.isFinished(true);
                        viewModel.setResult(TransactionResults.FAILURE);
                        return;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                    Resources resources = MainApplication.getInstance().getResources();
                    _msg = resources.getString(R.string.error_message_ticket_8097) + "\n" + resources.getString(R.string.error_detail_ticket_8097);
                    viewModel.setFinishedMessage(_msg);
                    viewModel.isFinished(true);
                    viewModel.setResult(TransactionResults.FAILURE);
                    return;
                }
            }
        }

        _transLogger.cash(payTime, _amount, isFixedAmountPostalOrder);
        int slipId = _transLogger.insert();
        _transLogger.updateCancelFlg();

        String termSequence = String.valueOf(AppPreference.getTermSequence());
        new Thread(() -> {
            if (AppPreference.isPosTransaction()) {
                OptionalTransFacade _optionalTransFacade = null;
                if (isFixedAmountPostalOrder) {
                    _optionalTransFacade = new OptionalTransFacade(MoneyType.POSTALORDER); // 為替類の場合
                    _optionalTransFacade = _transLogger.setDataForFacade(_optionalTransFacade);
                    _optionalTransFacade.CreateReceiptData(slipId, payTime, termSequence, _amount, _over); // 取引明細書、取消票、領収書のデータを作成
                    _optionalTransFacade.CreatePostalOrder(isRepay, payTime, termSequence);
                } else {
                    _optionalTransFacade = new OptionalTransFacade(MoneyType.CASH); // 現金の場合
                    _optionalTransFacade = _transLogger.setDataForFacade(_optionalTransFacade);
                    _optionalTransFacade.CreateReceiptData(slipId, payTime, termSequence, _amount, _over); // 取引明細書、取消票、領収書のデータを作成
                    _optionalTransFacade.CreateCash(isRepay, payTime, termSequence);
                }
            }

//            if (AppPreference.isTicketTransaction()) {
//                // チケット販売時の取引情報を作成する
//                OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.CASH);
//                optionalTicketTransFacade = _transLogger.setTicketDataForFacade(optionalTicketTransFacade);
//                optionalTicketTransFacade.CreateReceiptData(slipId, payTime, termSequence, _amount, _over); // 取引明細書、取消票、領収書のデータを作成
//            }
        }).start();
        makeSound(R.raw.credit_auth_ok);
        String msg = "";
        if (!isRepay) {
            msg = "決済処理が完了しました\nおつり " + String.format("%,d", _over) + "円";
            // 売上明細書の印刷
            PrinterManager.getInstance().print_trans_cash(view, slipId);
        } else {
            msg = "取消処理が完了しました";
            // 取消票の印刷
            PrinterManager.getInstance().print_cancel_ticket(view, slipId);

            // チケット購入取消送信
            if (AppPreference.isTicketTransaction()) {
                Thread postTicketCancelThread = new Thread(() -> {
                    new McTerminal().postTicketCancel();
                });
                postTicketCancelThread.start();

                try {
                    postTicketCancelThread.join();
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }
        Timber.i("現金取引結果："+ msg);
        viewModel.setFinishedMessage(msg);
        viewModel.isFinished(true);
        viewModel.setResult(TransactionResults.SUCCESS);
    }

    @Override
    public void onCancelBtn(View view) {
        CommonClickEvent.RecordClickOperation("中止", true);
        NavigationWrapper.navigateUp(this);
    }

    @Override
    public void onHomeBtn(View view) {
    	CommonClickEvent.RecordClickOperation("確認", true);

        if (AppPreference.isTicketTransaction() && !isRepay) {
            // チケット販売時の初回現金決済の取引明細書を印刷完了後、QR発行画面に遷移する
            _handler.post(() -> {
//                NavigationWrapper.navigate(view, R.id.action_navigation_menu_to_fragment_ticket_issue);
            });
        } else {
            _handler.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_global_navigation_menu);
            });
        }
    }

    public void makeSound(@RawRes int id) {
        float volume = 0f;
        _soundManager.load(MainApplication.getInstance(), id, 1);

        if (id == R.raw.qr_start) {
            volume =  AppPreference.getSoundGuidanceVolume() / 10f;
        } else {
            volume =  AppPreference.getSoundPaymentVolume() / 10f;
        }

        float leftVolume = volume;
        float rightVolume = volume;
        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            soundPool.play(soundId, leftVolume, rightVolume, 1, 0, 1);
        });
    }
}
