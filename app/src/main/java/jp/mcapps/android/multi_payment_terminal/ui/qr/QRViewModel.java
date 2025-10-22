package jp.mcapps.android.multi_payment_terminal.ui.qr;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RawRes;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeNameMap;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
//import jp.mcapps.android.multi_payment_terminal.model.OptionalTicketTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.QRSettlement;
import jp.mcapps.android.multi_payment_terminal.model.QRSettlement.ResultSummary;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import timber.log.Timber;

public class QRViewModel extends ViewModel {
    private final QRSettlement _qrSettlement = new QRSettlement();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private final MainApplication _app = MainApplication.getInstance();
    private SoundManager _soundManager = SoundManager.getInstance();
    private int _fee;
    private String _payName;

    private ResultSummary _summary;

    private int _slipId = 0;

    private TransLogger _transLogger_Err = null;

    public int getSlipId() {
        return _slipId;
    }

    private final MutableLiveData<Boolean> _isProcessing = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isProcessing() {
        return _isProcessing;
    }
    public void isProcessing(boolean b) {
        _handler.post(() -> {
            _isProcessing.setValue(b);
        });
    }

    private final MutableLiveData<Boolean> _isFinished = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isFinished() {
        return _isFinished;
    }
    public void isFinished(boolean b) {
        _handler.post(() -> {
            _isFinished.setValue(b);
        });
    }

    private final MutableLiveData<TransactionResults> _result = new MutableLiveData<>();
    public MutableLiveData<TransactionResults> getResult() {
        return _result;
    }
    public void setResult(TransactionResults result) {
        _handler.post(() -> {
            _result.setValue(result);
        });
    }

    private final MutableLiveData<String> _finishedMessage = new MutableLiveData<>("");
    public MutableLiveData<String> getFinishedMessage() {
        return _finishedMessage;
    }
    public void setFinishedMessage(String msg) {
        _handler.post(() -> {
            _finishedMessage.setValue(msg);
        });
    }

    SlipDao _slipDao = DBManager.getSlipDao();

    public void payment(String authCode) {
        final TransLogger transLogger = new TransLogger();

        _app.setBusinessType(BusinessType.PAYMENT);

        isProcessing(true);

        transLogger.setAntennaLevel();   //アンテナレベルを取得

        _summary = _qrSettlement.order(authCode);

        String msg;
        final String payTypeName = QRPayTypeNameMap.get(_summary.payType) != null
                ? QRPayTypeNameMap.get(_summary.payType)
                : _app.getString(R.string.money_brand_codetrans);

        if (_summary.result == TransactionResults.SUCCESS) {
            makeSound(R.raw.qr_ok);
            transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);   //取引結果
            msg = String.format("%s支払　%s円\n\nありがとうございました",
                    payTypeName, Converters.stringToNumberFormat(_summary.totalFee));
            Timber.tag("QR").i("QR支払 種別: %s,ウォレット: %s", _summary.payType, _summary.wallet);
        } else if (_summary.result == TransactionResults.FAILURE) {
            makeSound(R.raw.qr_ng);
            transLogger.setTransResult(TransMap.RESULT_ERROR, TransMap.DETAIL_AUTH_RESULT_NG);
            msg = String.format("%s支払　%s円\n\nお取扱いできません",
                    payTypeName, Converters.integerToNumberFormat(Amount.getFixedAmount()));
        } else {
            makeSound(R.raw.qr_check);
            transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_COMMUNICATION_FAILURE);
            msg = "支払結果が確認できません\nしばらくしてから\n結果確認をしてください";
        }

        if (_summary.result == TransactionResults.SUCCESS) {
            transLogger.qr(_summary);
            transLogger.setProcTime(_summary.procTime, 0);
            _slipId = transLogger.insert();

            if (AppPreference.isPosTransaction()) {
                // 通常の取引レコード以外の取引情報を作成する
                OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.QR);
                optionalTransFacade = transLogger.setDataForFacade(optionalTransFacade);
                optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                optionalTransFacade.CreateByUriData(); // DBにセット
            }

//            if (AppPreference.isTicketTransaction()) {
//                // チケット販売時の取引情報を作成する
//                OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.QR);
//                optionalTicketTransFacade = transLogger.setTicketDataForFacade(optionalTicketTransFacade);
//                optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
//            }

            // クリア前に現金分割分を現金併用金額に加算
            MainApplication.getInstance().setCashValue(MainApplication.getInstance().getCashValue() + Amount.getCashAmount());
            Amount.reset();
        } else {
            _transLogger_Err = new TransLogger();
            _transLogger_Err = transLogger;
        }

        setResult(_summary.result);
        isProcessing(false);
        isFinished(true);

        if (_summary.code != null) {
            _app.setErrorCode(_summary.code);
        }

        // Fragmentでobserveしてるので最後にセットする
        setFinishedMessage(msg);
    }

    public void refund(String orderId, int fee, String payName, int oldSlipId) {
        final TransLogger transLogger = new TransLogger();

        transLogger.setRefundParam(oldSlipId);

        _fee = fee;
        _payName = payName;

        _app.setBusinessType(BusinessType.REFUND);

        isProcessing(true);

        transLogger.setAntennaLevel();   //アンテナレベルを取得

        _summary = _qrSettlement.refund(orderId, fee);

        String msg;
        final String payTypeName = QRPayTypeNameMap.get(_summary.payType) != null
                ? QRPayTypeNameMap.get(_summary.payType)
                : _payName;

        if (_summary.result == TransactionResults.SUCCESS) {
            makeSound(R.raw.qr_ok);
            transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);
            msg = String.format("%s取消　%s円\n\nありがとうございました",
                    payTypeName, Converters.stringToNumberFormat(_summary.totalFee));
            Timber.tag("QR").i("QR取消 種別: %s,ウォレット: %s", _summary.payType, _summary.wallet);
        } else if (_summary.result == TransactionResults.FAILURE) {
            makeSound(R.raw.qr_ng);
            transLogger.setTransResult(TransMap.RESULT_ERROR, TransMap.DETAIL_AUTH_RESULT_NG);
            msg = String.format("%s取消　%s円\n\nお取扱いできません", payTypeName, fee);
        } else {
            makeSound(R.raw.qr_check);
            transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_COMMUNICATION_FAILURE);
            msg = "取消結果が確認できません\nしばらくしてから\n結果確認をしてください";
        }

        if (_summary.result == TransactionResults.SUCCESS) {
            transLogger.qr(_summary);
            transLogger.setProcTime(_summary.procTime, 0);
            _slipId = transLogger.insert();

            if (AppPreference.isPosTransaction()) {
                // 通常の取引レコード以外の取引情報を作成する
                OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.QR);
                optionalTransFacade = transLogger.setDataForFacade(optionalTransFacade);
                optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                optionalTransFacade.CreateByUriData(); // DBにセット
            }

//            if (AppPreference.isTicketTransaction()) {
//                // チケット販売時の取引情報を作成する
//                OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.QR);
//                optionalTicketTransFacade = transLogger.setTicketDataForFacade(optionalTicketTransFacade);
//                optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
//            }

            // POSサービス有効の時は取消成功時・取消未了時に取消不可にする
            if(AppPreference.isServicePos()) {
                transLogger.updateCancelFlg();
            }
        } else {
            _transLogger_Err = new TransLogger();
            _transLogger_Err = transLogger;
        }
        setResult(_summary.result);
        isProcessing(false);
        isFinished(true);

        if (_summary.code != null) {
            _app.setErrorCode(_summary.code);
        }

        // Fragmentでobserveしてるので最後にセットする
        setFinishedMessage(msg);
    }

    // 決済結果確認
    public void checkPaymentResult() {
        final TransLogger transLogger = new TransLogger();

        _app.setBusinessType(BusinessType.PAYMENT);

        isProcessing(true);

        transLogger.setAntennaLevel();   //アンテナレベルを取得

        _summary = _qrSettlement.checkOrder(_summary);

        String msg;
        final String payTypeName = QRPayTypeNameMap.get(_summary.payType) != null
                ? QRPayTypeNameMap.get(_summary.payType)
                : _app.getString(R.string.money_brand_codetrans);

        if (_summary.result == TransactionResults.SUCCESS) {
            makeSound(R.raw.qr_ok);
            transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_COMMUNICATION_FAILURE);   //取引結果
            msg = String.format("%s支払　%s円\n\nありがとうございました",
                    payTypeName, Converters.stringToNumberFormat(_summary.totalFee));
        } else if (_summary.result == TransactionResults.FAILURE) {
            makeSound(R.raw.qr_ng);
            transLogger.setTransResult(TransMap.RESULT_ERROR, TransMap.DETAIL_COMMUNICATION_FAILURE);
            msg = String.format("%s支払　%s円\n\nお取扱いできません",
                    payTypeName, Converters.integerToNumberFormat(Amount.getFixedAmount()));
        } else {
            makeSound(R.raw.qr_check);
            transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_COMMUNICATION_FAILURE);
            msg = "支払結果が確認できません\nしばらくしてから\n結果確認をしてください";
        }

        if (_summary.result == TransactionResults.SUCCESS) {
            transLogger.qr(_summary);
            transLogger.setProcTime(_summary.procTime, 0);
            _slipId = transLogger.insert();

            if (AppPreference.isPosTransaction()) {
                // 通常の取引レコード以外の取引情報を作成する
                OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.QR);
                optionalTransFacade = transLogger.setDataForFacade(optionalTransFacade);
                optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                optionalTransFacade.CreateByUriData(); // DBにセット
            }

//            if (AppPreference.isTicketTransaction()) {
//                // チケット販売時の取引情報を作成する
//                OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.QR);
//                optionalTicketTransFacade = transLogger.setTicketDataForFacade(optionalTicketTransFacade);
//                optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
//            }

            Amount.reset();
        } else {
            _transLogger_Err = new TransLogger();
            _transLogger_Err = transLogger;
        }

        setResult(_summary.result);
        isProcessing(false);
        isFinished(true);

        if (_summary.code != null) {
            _app.setErrorCode(_summary.code);
        }

        // Fragmentでobserveしてるので最後にセットする
        setFinishedMessage(msg);
    }

    // 返金結果確認
    public void checkRefundResult(int oldSlipId) {
        final TransLogger transLogger = new TransLogger();
        transLogger.setRefundParam(oldSlipId);

        _app.setBusinessType(BusinessType.REFUND);

        isProcessing(true);

        transLogger.setAntennaLevel();   //アンテナレベルを取得

        _summary = _qrSettlement.checkRefund(_summary);

        String msg;
        final String payTypeName = QRPayTypeNameMap.get(_summary.payType) != null
                ? QRPayTypeNameMap.get(_summary.payType)
                : _payName;

        if (_summary.result == TransactionResults.SUCCESS) {
            makeSound(R.raw.qr_ok);
            transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_COMMUNICATION_FAILURE);
            msg = String.format("%s取消　%s円\n\nありがとうございました",
                    payTypeName, Converters.stringToNumberFormat(_summary.totalFee));
        } else if (_summary.result == TransactionResults.FAILURE) {
            makeSound(R.raw.qr_ng);
            transLogger.setTransResult(TransMap.RESULT_ERROR, TransMap.DETAIL_COMMUNICATION_FAILURE);
            msg = String.format("%s取消　%s円\n\nお取扱いできません", payTypeName, _fee);
        } else {
            makeSound(R.raw.qr_check);
            transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_COMMUNICATION_FAILURE);
            msg = "取消結果が確認できません\nしばらくしてから\n結果確認をしてください";
        }

        if (_summary.result == TransactionResults.SUCCESS) {
            transLogger.qr(_summary);
            transLogger.setProcTime(_summary.procTime, 0);
            _slipId = transLogger.insert();

            if (AppPreference.isPosTransaction()) {
                // 通常の取引レコード以外の取引情報を作成する
                OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.QR);
                optionalTransFacade = transLogger.setDataForFacade(optionalTransFacade);
                optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                optionalTransFacade.CreateByUriData(); // DBにセット
            }

//            if (AppPreference.isTicketTransaction()) {
//                // チケット販売時の取引情報を作成する
//                OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.QR);
//                optionalTicketTransFacade = transLogger.setTicketDataForFacade(optionalTicketTransFacade);
//                optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
//            }

            // POSサービス有効の時は取消成功時・取消未了時に取消不可にする
            if(AppPreference.isServicePos()) {
                transLogger.updateCancelFlg();
            }
        } else {
            _transLogger_Err = new TransLogger();
            _transLogger_Err = transLogger;
        }

        setResult(_summary.result);
        isProcessing(false);
        isFinished(true);

        if (_summary.code != null) {
            _app.setErrorCode(_summary.code);
        }

        // Fragmentでobserveしてるので最後にセットする
        setFinishedMessage(msg);
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

    // 処理未了時の売上データ生成
    public void createUnfinishedTransLogger() {
        if (null != _transLogger_Err) {
            _transLogger_Err.qr(_summary);
            _transLogger_Err.setProcTime(_summary.procTime, 0);
            _slipId = _transLogger_Err.insert();
            // POSサービス有効の時は取消成功時・取消未了時に取消不可にする
            if(AppPreference.isServicePos()) {
                _transLogger_Err.updateCancelFlg();
            }

            if (AppPreference.isPosTransaction()) {
                // 通常の取引レコード以外の取引情報を作成する(未了)
                OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.QR);
                optionalTransFacade = _transLogger_Err.setDataForFacade(optionalTransFacade);
                optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、領収書のデータを作成
                optionalTransFacade.CreateByUriData(); // DBにセット
            }

//            if (AppPreference.isTicketTransaction()) {
//                // チケット販売時の取引情報を作成する(未了)
//                OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.QR);
//                optionalTicketTransFacade = _transLogger_Err.setTicketDataForFacade(optionalTicketTransFacade);
//                optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
//            }
        }
    }

    //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
    private MutableLiveData<AmountInputSeparationPayFDViewModel> _amountInputSeparationPayFDViewModel = new MutableLiveData<>(null);
    public MutableLiveData<AmountInputSeparationPayFDViewModel> getAmountInputSeparationPayFDViewModel() {
        return _amountInputSeparationPayFDViewModel;
    }
    public void setAmountInputSeparationPayFDViewModel(AmountInputSeparationPayFDViewModel viewModel) {
        _amountInputSeparationPayFDViewModel.setValue( viewModel);
    }
    //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修


}
