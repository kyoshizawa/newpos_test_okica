package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import java.text.ParseException;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeCodes;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeNameMap;
import jp.mcapps.android.multi_payment_terminal.data.TransHead;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentHistoryTransactionDetailBinding;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import timber.log.Timber;

public class HistoryTransactionDetailFragment extends BaseFragment implements CreditSettlement.CreditSettlementListener {
    private static final String ARGS_NAME = "SLIP_ID";
    private final String SCREEN_NAME = "取引履歴詳細";
    private final CreditSettlement _creditSettlement = CreditSettlement.getInstance();
    private HistoryTransactionDetailViewModel _viewModel;
    private SoundManager _soundManager = SoundManager.getInstance();
    private float _soundVolume = AppPreference.getSoundPaymentVolume() / 10f;
    private CreditSettlement.CreditSettlementListener _listener;

    public static HistoryTransactionDetailFragment newInstance() {
        return new HistoryTransactionDetailFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentHistoryTransactionDetailBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_history_transaction_detail, container, false);
        binding.setHandlers(new HistoryEventHandlersImpl(this));
        binding.setLifecycleOwner(getViewLifecycleOwner());

        _viewModel = new ViewModelProvider(this).get(HistoryTransactionDetailViewModel.class);
        binding.setViewModel(_viewModel);

        binding.setSharedViewModel(new ViewModelProvider(requireActivity()).get(SharedViewModel.class));

        Bundle args = getArguments();
        if (args != null) {
            SharedViewModel sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            sharedViewModel.setLoading(true);

            boolean isLt27 = IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D);
            boolean isOkabe_MS70 = IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D);
            //ADD-S BMT S.Oyama 2024/09/27 フタバ双方向向け改修
            boolean isFutabaD = IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D);
            //ADD-E BMT S.Oyama 2024/09/27 フタバ双方向向け改修
            boolean isFutabaDManual = IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL);
            final boolean[] isLt27Reprinting = {true};

            int slipId = args.getInt(ARGS_NAME);

            final SlipData[] slipData = {null, null};
            final ReceiptData[] receiptData = {new ReceiptData()};
            final TicketReceiptData[] ticketReceiptData = { new TicketReceiptData()};

            Thread thread = new Thread(() -> {
                SlipDao dao = DBManager.getSlipDao();
                slipData[0] = dao.getOneById(slipId);
                AppPreference.setCancelTargetSlipId(slipData[0].id);

                //POS有効の場合のみ取得する
                if(AppPreference.isServicePos()) {
                    ReceiptDao dao1 = DBManager.getReceiptDao();
                    receiptData[0] = dao1.getReceiptsBySlipId(slipId);

                    if (receiptData[0] == null && slipData[0] != null) {
                        // 見つからない場合は通番と取引日時で探す
                        receiptData[0] = dao1.getReceiptsByTermSequenceAndTransDate(slipData[0].termSequence, slipData[0].transDate);

                        // これをやると影響範囲が大きくなるので一旦保留
//                        if (receiptData[0] != null) {
//                            // 見つかったら元のSlipDataを取り直す（取消のため）
//                            slipData[0] = dao.getOneById(receiptData[0].slip_id);
//                            AppPreference.setCancelTargetSlipId(slipData[0].id);
//                        }
                    }
                }

                // チケットサービス有効な場合、チケットレシートデータ取得
                if (AppPreference.isServiceTicket()) {
                    TicketReceiptDao ticketReceiptDao = DBManager.getTicketReceiptDao();
                    ticketReceiptData[0] = ticketReceiptDao.getReceiptsBySlipId(slipId);

                    if (ticketReceiptData[0] == null && slipData[0] != null) {
                        // 見つからない場合は通番と取引日時で探す
                        ticketReceiptData[0] = ticketReceiptDao.getReceiptsByTermSequenceAndTransDate(slipData[0].termSequence, slipData[0].transDate);

                        // これをやると影響範囲が大きくなるので一旦保留
//                        if (ticketReceiptData[0] != null) {
//                            // 見つかったら元のSlipDataを取り直す（取消のため）
//                            slipData[0] = dao.getOneById(ticketReceiptData[0].slip_id);
//                            AppPreference.setCancelTargetSlipId(slipData[0].id);
//                        }
                    }
                }

                //ADD-S BMT S.Oyama 2024/09/27 フタバ双方向向け改修
                //if (isLt27 || isOkabe_MS70) {
                if (isLt27 || isOkabe_MS70 || isFutabaD || isFutabaDManual) {
                //ADD-E BMT S.Oyama 2024/09/27 フタバ双方向向け改修
                    if (isOkabe_MS70) {
                        // 岡部メーター双方向の場合は直前取引も取得
                        slipData[1] = dao.getLatestOne();
                        if (slipData[1].transDate.compareTo(AppPreference.getDatetimeLt27Printable()) <= 0) {
                            // 直前取引が再印字不可になっている場合、取消も不可にする
                            if (slipData[1].cancelFlg != null) {
                                dao.updateCancelUriId(slipData[1].id);
                            }
                            isLt27Reprinting[0] = false;
                        }
                    } else {
                        slipData[1] = dao.getLatestOne();
                        // LT-27双方向は直近の取引も取得
                        // ポイント・決済それぞれで直近のものは取消可
                        Thread thread1 = new Thread(() -> {
                            List<TransHead> transHeadList = dao.getTransHead();
                            for(TransHead head : transHeadList){
                                if(slipData[0].id == head.id){
                                    if (slipData[0].transDate.compareTo(AppPreference.getDatetimeLt27Printable()) <= 0) {
                                        // 直前取引が再印字不可になっている場合、取消も不可にする
                                        if (slipData[0].cancelFlg != null) {
                                            dao.updateCancelUriId(slipData[0].id);
                                        }
                                            isLt27Reprinting[0] = false;
                                    }
                                    break;
                                } else {
                                    slipData[1] = dao.getOneById(head.id);

                                    if(slipData[1].transBrand.equals(getString(R.string.point_brand_watari))
                                    && slipData[0].transBrand.equals(getString(R.string.point_brand_watari))){
                                        if (slipData[1].transDate.compareTo(AppPreference.getDatetimeLt27Printable()) <= 0) {
                                            // 直近取引が再印字不可になっている場合、取消も不可にする
                                            if (slipData[1].cancelFlg != null) {
                                                dao.updateCancelUriId(slipData[1].id);
                                            }
                                            isLt27Reprinting[0] = false;
                                        }
                                        break;
                                    } else if(!slipData[1].transBrand.equals(getString(R.string.point_brand_watari))
                                           && !slipData[0].transBrand.equals(getString(R.string.point_brand_watari))){
                                        if (slipData[1].transDate.compareTo(AppPreference.getDatetimeLt27Printable()) <= 0) {
                                            // 直近取引が再印字不可になっている場合、取消も不可にする
                                            if (slipData[1].cancelFlg != null) {
                                                dao.updateCancelUriId(slipData[1].id);
                                            }
                                            isLt27Reprinting[0] = false;
                                        }
                                        break;
                                    }
                                }
                            }
                        });
                        thread1.start();
                        try {
                            thread1.join();
                        } catch (InterruptedException e) {
                            // 例外処理
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();

            try {
                thread.join();
                binding.setSlipData(slipData[0]);

                if (slipData[0].transBrand.equals(getString(R.string.money_brand_credit))) {
                    _viewModel.setIsCredit(true);
                } else if (slipData[0].transBrand.equals(getString(R.string.money_brand_codetrans))) {
                    _viewModel.setIsQr(true);
                    if (slipData[0].codetransPayTypeName == null || slipData[0].codetransPayTypeName.equals(""))
                    {
                        _viewModel.setIsNoQrBrandName(true);
                    } else {
                        _viewModel.setIsNoQrBrandName(false);
                    }
                } else if (slipData[0].transBrand.equals(getString(R.string.money_brand_cash))) {
                    _viewModel.setIsCash(true);
                } else if (slipData[0].transBrand.equals(getString(R.string.money_brand_postal_order))) {
                    _viewModel.setIsPostalOrder(true);
                } else if (slipData[0].transBrand.equals(getString(R.string.point_brand_watari))) {
                    _viewModel.setIsWatari(true);
                }

                // transTypeCode見て手動決済モードだったら手動決済表示フラグ建てる
                if(slipData[0].transTypeCode != null && (slipData[0].transTypeCode.equals(PrinterProc.MANUALMODE_TRANS_TYPE_CODE_SALES) || slipData[0].transTypeCode.equals(PrinterProc.MANUALMODE_TRANS_TYPE_CODE_CANCEL))) {
                    _viewModel.setIsManual(true);
                }

                if (slipData[0].transBrand.equals(getString(R.string.money_brand_credit))
                        || slipData[0].transBrand.equals(getString(R.string.money_brand_id))
                        || slipData[0].transBrand.equals(getString(R.string.money_brand_qp))
                        || slipData[0].transBrand.equals(getString(R.string.money_brand_codetrans))
                        || slipData[0].transBrand.equals(getString(R.string.money_brand_cash))
                        || slipData[0].transBrand.equals(getString(R.string.money_brand_postal_order))
                        || slipData[0].transBrand.equals(getString(R.string.point_brand_watari))) {
                    if (slipData[0].transBrand.equals(getString(R.string.money_brand_id)) && slipData[0].transAfterBalance != null) {
                        _viewModel.setHasBalance(true);
                    } else if (slipData[0].transBrand.equals(getString(R.string.money_brand_codetrans))
                            && slipData[0].codetransPayTypeName != null
                            && slipData[0].codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                        _viewModel.setHasBalance(true);
                    } else {
                        _viewModel.setHasBalance(false);
                    }
                } else {
                    _viewModel.setHasBalance(true);
                }

                if (slipData[0].transCashTogetherAmount != null) {
                    String cash = Converters.integerToNumberFormat(slipData[0].transCashTogetherAmount) + getString(R.string.yen);
                    binding.valueCashTogetherAmount.setText(cash);
                }

                if (slipData[0].transResult == TransMap.RESULT_SUCCESS) {
                    _viewModel.setIsSuccess(true);
                    if (slipData[0].transAfterBalance != null) {
                        String balance = Converters.longToNumberFormat(slipData[0].transAfterBalance) + getString(R.string.yen);
                        binding.valueTransDetailBalance.setText(balance);
                    }
                } else if (slipData[0].transResult == TransMap.RESULT_UNFINISHED) {
                    _viewModel.setIsUnFinished(true);
                    if (slipData[0].transBeforeBalance != null) {
                        String balance = Converters.longToNumberFormat(slipData[0].transBeforeBalance) + getString(R.string.yen);
                        binding.valueTransDetailBalance.setText(balance);
                    }
                }

                if (slipData[0].cancelFlg == null || slipData[0].transBrand.equals(getString(R.string.money_brand_edy))) {
                    _viewModel.setCancelable(false);
                }

                // プリペイドのカード発売はレシート再印字も不可
                if (slipData[0].transBrand.equals(getString(R.string.prepaid_brand)) && slipData[0].transType == TransMap.TYPE_PREPAID_CARDBUY) {
                    _viewModel.setCancelable(false);
                    _viewModel.setIsSuccess(false);
                }

                if (slipData[0].cancelFlg != null && slipData[0].transactionTerminalType == AppPreference.TerminalType.Pos.ordinal()) {
                    // POS機能：クレジット・iD・QUICPay・QR・現金・為替類 かつ 24時間以内の取引であれば取消可
                    if(slipData[0].transBrand.equals(getString(R.string.money_brand_credit))
                            || slipData[0].transBrand.equals(getString(R.string.money_brand_id))
                            || slipData[0].transBrand.equals(getString(R.string.money_brand_qp))
                            || slipData[0].transBrand.equals(getString(R.string.money_brand_codetrans))
                            || slipData[0].transBrand.equals(getString(R.string.money_brand_cash))
                            || slipData[0].transBrand.equals(getString(R.string.money_brand_postal_order))){

                        try {
                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.HOUR, -24); // 24時間前まで取消可
                            Date transDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE).parse(slipData[0].transDate);
                            if (transDate.after(calendar.getTime())) {
                                _viewModel.setCancelable(true);
                            }
                            else {
                                _viewModel.setCancelable(false);
                                // 取引時刻から24時間経過した場合、取消フラグをnullに更新
                                Thread thread_pos = new Thread(() -> {
                                    DBManager.getSlipDao().updateCancelUriId(slipData[0].id);
                                });
                                thread_pos.start();
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    // 直前のみ取消可能な決済種別は直前ではない取引の取消フラグをnullに更新する
                    else{
                        Thread thread1 = new Thread(() -> {
                            SlipDao dao = DBManager.getSlipDao();
                            slipData[1] = dao.getLatestOne();
                            if(slipData[0].id != slipData[1].id){
                                _viewModel.setCancelable(false);
                                DBManager.getSlipDao().updateCancelUriId(slipData[0].id);
                            }
                        });
                        thread1.start();
                    }
                } else if (slipData[0].cancelFlg != null && slipData[0].transactionTerminalType == AppPreference.TerminalType.Ticket.ordinal()) {
                    // チケットの場合、当日中のみ取消可とする仕様
                    try {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        Date transDate = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE).parse(slipData[0].transDate);

                        String nowDate = calendar.getTime().toString();
                        String slipDate = transDate.toString();

                        Timber.d("nowDate:%s slipDate:%s", nowDate, slipDate);

                        if (nowDate.equals(slipDate)) {
                            _viewModel.setCancelable(true);
                        } else {
                            _viewModel.setCancelable(false);
                            Thread thread_ticket = new Thread(() -> {
                                DBManager.getSlipDao().updateCancelUriId(slipData[0].id);
                            });
                            thread_ticket.start();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if(slipData[0].cancelFlg != null) {
                    // ポイント・決済それぞれで直近のものは取消可
                    Thread thread1 = new Thread(() -> {
                        SlipDao dao = DBManager.getSlipDao();
                        List<TransHead> transHeadList = dao.getTransHead();
                        slipData[1] = dao.getLatestOne();
                        for(TransHead head : transHeadList){
                            if(slipData[0].id == head.id){
                                break;
                            } else {
                                slipData[1] = dao.getOneById(head.id);

                                if(slipData[1].transBrand.equals(getString(R.string.point_brand_watari))
                                        && slipData[0].transBrand.equals(getString(R.string.point_brand_watari))){
                                    _viewModel.setCancelable(false);
                                    _viewModel.setIsSuccess(false);
                                    break;
                                } else if(!slipData[1].transBrand.equals(getString(R.string.point_brand_watari))
                                        && !slipData[0].transBrand.equals(getString(R.string.point_brand_watari))){
                                    _viewModel.setCancelable(false);
                                    _viewModel.setIsSuccess(false);
                                    break;
                                }
                            }
                        }
                    });
                    thread1.start();
                }

                if (isOkabe_MS70) {
                    // LT-27双方向、岡部メーター双方向は再印字可能な取引を制限
                    if (slipId != slipData[1].id) {
                        // 直前取引以外は再印字不可
                        _viewModel.setIsSuccess(false);
                    } else if (!isLt27Reprinting[0]) {
                        // 直前取引でも再印字不可になっているものは取消も不可にする
                        _viewModel.setIsSuccess(false);
                        _viewModel.setCancelable(false);
                    }
                //CHG-S BMT S.Oyama 2024/10/08 フタバ双方向向け改修
                //} else if (isLt27) {
                } else if (isLt27 || isFutabaD || isFutabaDManual) {
                //CHG-E BMT S.Oyama 2024/10/08 フタバ双方向向け改修
                    // LT-27双方向、岡部メーター双方向は再印字可能な取引を制限
                    if (slipId != slipData[1].id) {
                        // 直前取引以外は再印字不可
                        _viewModel.setIsSuccess(false);
                    }
                    if (!isLt27Reprinting[0]) {
                        // 再印字不可になっているものは取消も不可にする
                        _viewModel.setIsSuccess(false);
                        _viewModel.setCancelable(false);
                    }
                }

                sharedViewModel.setLoading(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(slipData[0].transactionTerminalType == AppPreference.TerminalType.Pos.ordinal()) {
                // POS機能：売上ならマスタ設定に応じて領収書発行ボタン表示
                if(receiptData[0] != null) { // クレジットが拒否売上の場合、レシートは印字されない
                    if (slipData[0].transType == TransMap.TYPE_SALES && receiptData[0].canceled_trans == 0) {
                        final ServiceFunctionData[] serviceFunctionData = {new ServiceFunctionData()};
                        boolean _is_pos_receipt = false;
                        Thread thread_pos = new Thread(() -> {
                            serviceFunctionData[0] = DBManager.getServiceFunctionDao().getServiceFunction();
                        });
                        thread_pos.start();
                        try {
                            thread_pos.join();
                            if (serviceFunctionData[0] != null) {
                                _is_pos_receipt = serviceFunctionData[0].is_pos_receipt;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        _viewModel.setIsVisibility(_is_pos_receipt);
                    }
                }
            } else if (slipData[0].transactionTerminalType == AppPreference.TerminalType.Ticket.ordinal()) {
                // チケット販売時の領収書ボタン表示制御
                if (ticketReceiptData[0] != null) {
                    if (0 == slipData[0].transType && 0 == slipData[0].transResult && 0 == ticketReceiptData[0].canceled_trans) {
                        // 決済成功且つ取消可能な場合のみ、領収書ボタンを表示
                        _viewModel.setIsVisibility(true);
                    } else {
                        // その他の場合、領収書ボタンを非表示
                        _viewModel.setIsVisibility(false);
                    }
                } else {
                    Timber.e("ticketReceiptData = null");
                }
            }
        }

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _creditSettlement.setListener(this);

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
    public void onDestroyView() {
        super.onDestroyView();
        if (_creditSettlement.isSameListener(this)) {
            _creditSettlement.setListener(null);
        }
    }

    @Override
    public void selectApplication(String[] applications) {
    }

    @Override
    public void OnProcStart() {
    }

    @Override
    public void OnProcEnd() {
    }

    @Override
    public void OnError(String errorCode) {
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OnSound(int id) {
        Timber.d("OnSound");
        _soundManager.load(MainApplication.getInstance(), id, 1);

        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            soundPool.play(soundId, _soundVolume, _soundVolume, 1, 0, 1);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //画面オフを有効化 取消を中止した場合はここに戻るため
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void timeoutWaitCard(String errorCode) {
    }

    @Override
    public void cancelPin() {
    }
}
