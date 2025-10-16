package jp.mcapps.android.multi_payment_terminal.ui.others;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;

import com.pos.device.magcard.MagCardCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.error.OptionServiceErrorMap;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.model.Validator;
import jp.mcapps.android.multi_payment_terminal.model.WatariSettlement;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPoint;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPointCancel;
import timber.log.Timber;

@RequiresApi(api = Build.VERSION_CODES.M)
public class WatariViewModel extends ViewModel implements LifecycleObserver {
    public static final String successMessage = "ポイント付与が完了しました\n%s円相当を付与";
    public static final String successCancelMessage = "ポイント取消が完了しました\n%s円相当を取消";
    public static final String failureMessage = "ポイント付与が失敗しました\n\nお取り扱いできません";
    public static final String failureCancelMessage = "ポイント取消が失敗しました\n\nお取り扱いできません";
    public static final String unknownMessage = "結果が確認できません\nしばらくしてから\n再確認してください";

    private final WatariSettlement _watariSettlement = new WatariSettlement();
    private final List<Disposable> disposables = new ArrayList<>();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private String[] _tracks = null;
    private SoundManager _soundManager = SoundManager.getInstance();
    private MainApplication _app = MainApplication.getInstance();

    private enum Reasons {
        WatariDenialError("E1701", MainApplication.getInstance().getString(R.string.error_type_option_service_5902)),
        McDenialError("E1702", MainApplication.getInstance().getString(R.string.error_type_option_service_5903)),
        CancelWatariDenialError("E1801", MainApplication.getInstance().getString(R.string.error_type_option_service_5902)),
        CancelMcDenialError("E1802", MainApplication.getInstance().getString(R.string.error_type_option_service_5903));

        private String _reason;
        private String _errorCode;

        Reasons(String reason, String errorCode) {
            _reason = reason;
            _errorCode = errorCode;
        }

        public static String getErrorCode(String reason) {
            for (Reasons r : values()) {
                if (r._reason.equals(reason)) {
                    return r._errorCode;
                }
            }

            return MainApplication.getInstance().getString(
                    R.string.error_type_option_service_5903) + "@@@" + reason + "@@@";
        }
    }

    public enum UIStates {
        Read,
        ReadTimeout,
        Connecting,
        Finished,
        Success,
        Failure,
        Unknown,
    }

    public enum VolumeTypes {
        Payment,
        Guide,
    }

    private MutableLiveData<UIStates> _state = new MutableLiveData<>(UIStates.Read);
    public MutableLiveData<UIStates> getState() {
        return _state;
    }
    public void setState(UIStates state, int amount) {
        switch(state) {
            case Success:
                makeSound(R.raw.watari_ok, VolumeTypes.Payment);
                setResult(TransactionResults.SUCCESS);
                if (_isRefund) {
                    setFinishedMessage(String.format(successCancelMessage, amount));
                } else {
                    setFinishedMessage(String.format(successMessage, amount));
                }
                break;
            case Failure:
                makeSound(R.raw.watari_ng, VolumeTypes.Payment);
                setResult(TransactionResults.FAILURE);
                if (_isRefund) {
                    setFinishedMessage(failureCancelMessage);
                } else {
                    setFinishedMessage(failureMessage);
                }
                break;
            case Unknown:
                makeSound(R.raw.watari_check, VolumeTypes.Payment);
                setResult(TransactionResults.UNKNOWN);
                setFinishedMessage(unknownMessage);
                break;
            default:
                setResult(TransactionResults.None);
                setFinishedMessage("");
                break;
        }

        _handler.post(() -> {
            _state.setValue(state);
        });
    }
    private int _slipId = 0;
    public int getSlipId() { return _slipId; }
    public void setSlipId(int slipId) {
        _slipId = slipId;
    }

    private MutableLiveData<TransactionResults> _result = new MutableLiveData<>();
    public MutableLiveData<TransactionResults> getResult() {
        return _result;
    }

    private String _businessType;
    public String getBusinessType() { return _businessType; }
    public void setBusinessType(String type) { _businessType = type; }
    public void setResult(TransactionResults result) {
        _handler.post(() -> {
            _result.setValue(result);
        });
    }

    private boolean _isRefund = false;
    public void setIsRefund(boolean b) { _isRefund = b; }
    private int _refundSlipId = 0;
    public int getRefundSlipId() { return _refundSlipId; }
    public void setRefundSlipId(int slipId) {
        _refundSlipId = slipId;
    }

    private MutableLiveData<String> _finishedMessage = new MutableLiveData<>("");
    public MutableLiveData<String> getFinishedMessage() {
        return _finishedMessage;
    }
    public void setFinishedMessage(String message) {
        _handler.post(() -> {
            _finishedMessage.setValue(message);
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        cleanup();
    }

    public void start() {
        makeSound(R.raw.watari_start, VolumeTypes.Guide);
        searchCard();
    }

    public void searchCard() {
        cleanup();

        Disposable subscribe = _watariSettlement.searchCard()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposables::add)
                .subscribe((tracks, error) -> {
                    if (error != null) {
                        if (error instanceof WatariSettlement.WatariException) {
                            int reason = ((WatariSettlement.WatariException) error).getReason();
                            switch (reason) {
                                case MagCardCallback.TIMEOUT_ERROR:
                                    setState(UIStates.ReadTimeout, 0);
                                    _app.setErrorCode(_app.getString(R.string.error_type_option_service_5901));
                                    return;
                                case MagCardCallback.USER_CANCEL:
                                    return;
                                case WatariSettlement.WatariCardError.READ_ERROR:
                                default:
                                    makeSound(R.raw.watari_read_error, VolumeTypes.Guide);
                                    break;
                            }

                            _handler.post(this::searchCard);
                        }
                        return;
                    }
                    _tracks = tracks;

                    // ここでインクリメント（状態確認の時に同じ通番を使いたいので
                    int slipNo = AppPreference.getSlipNoWatari();
                    slipNo = slipNo < 999
                            ? slipNo + 1
                            : 1;
                    AppPreference.setSlipNoWatari(slipNo);

                    if (_isRefund) {
                        if (AppPreference.isDemoMode()) {
                            pointCancelDemo();
                        } else {
                            pointCancel();
                        }
                        return;
                    }

                    if (AppPreference.isDemoMode()) {
                        pointAddDemo();
                    } else {
                        pointAdd();
                    }
                });
    }

    public void pointAdd() {
        cleanup();
        setState(UIStates.Read, 0);

        if (_tracks == null) {
            Timber.e("track info is null");
            return;
        }

        Disposable subscribe = _watariSettlement.sendAddApi(_tracks)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> {
                    setState(UIStates.Connecting, 0);
                    disposables.add(d);
                })
                .subscribe((result, unhandledError) -> {
                    if (unhandledError != null) {
                        Timber.d("カード読み取り時エラー");
                        Timber.e(unhandledError);
                        setState(UIStates.Unknown, 0);
                    }

                    if (result.ex != null) {
                        if (result.ex instanceof HttpStatusException) {
                            int status = ((HttpStatusException) result.ex).getStatusCode();

                            // オプションサービス共通のエラー
                            _app.setErrorCode(OptionServiceErrorMap.getCommunicationError(status));

                            setState(UIStates.Failure, 0);
                            Amount.reset();
                        } else {
                            setState(UIStates.Unknown, 0);
                        }
                    } else {
                        if (result.addRes.result) {
                            final TransLogger transLogger = new TransLogger();
                            transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);
                            transLogger.point(result);
                            _slipId = transLogger.insert();

                            setState(UIStates.Success, result.addReq.fare);
                        } else {
                            _app.setErrorCode(WatariViewModel.Reasons.getErrorCode(result.addRes.errorCode));
                            setState(UIStates.Failure, 0);
                            Amount.reset();
                        }
                    }
                });
    }

    public void pointAddDemo() {
        cleanup();
        setState(UIStates.Read, 0);

        if (_tracks == null) {
            Timber.e("track info is null");
            return;
        }
        int slipNo = AppPreference.getSlipNoWatari();

        final WatariPoint.Request request = new WatariPoint.Request();
        request.moneyKbn = 0; // マネー区分（0:和多利）
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
        request.authDateTime = dateFmt.format(new Date()); // 操作日時
        request.fare = Amount.getFixedAmount(); // 売上金額
        request.terminalProcNo = slipNo; // 通番（伝票番号）
        request.payMethod = "10"; // 支払方法（固定）
        request.productCd = "0000240"; // 商品コード
        request.driverCd = 999; // 乗務員コード

        final WatariPoint.Response response = new WatariPoint.Response();
        response.result = true;
        response.errorCode = "";
        response.terminalNo = "3080600099999"; // 端末番号
        response.cardCampany = ""; // カード発行会社
        response.authDateTime = request.authDateTime; // 売上日時
        response.maskedMemberNo = ""; // マスクされたカード番号
        response.approvalNo = ""; // 承認番号
        response.terminalProcNo = request.terminalProcNo; // 伝票番号
        response.name = "MC TARO"; // 名前
        response.fare = request.fare; // 売上金額
        response.transactionType = 1; // 取引内容
        response.productCd = "0000240"; // 商品コード
        response.addPoint = 2; // 加算ポイント
        response.sumPoint = 192; // ポイント残高
        response.calidityPeriod = "2412"; // 有効期限

        WatariSettlement.WatariResult result = new WatariSettlement.WatariResult() {
            {
                addReq = request;
                addRes = response;
                ex = null;
                payTime = response.authDateTime;
                amount = request.fare;
            }
        };

        final TransLogger transLogger = new TransLogger();
        transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);
        transLogger.point(result);
        _slipId = transLogger.insert();

        setState(UIStates.Success, result.addReq.fare);
    }

    public void pointCancel() {
        cleanup();
        setState(UIStates.Read, 0);

        if (_tracks == null) {
            Timber.e("track info is null");
            return;
        }
        // 先に取っておく
        final TransLogger transLogger = new TransLogger();
        transLogger.setRefundParam(_refundSlipId);
        RefundParam refundParam = transLogger.getRefundParam();

        Disposable subscribe = _watariSettlement.sendCancelApi(_tracks, refundParam.oldTransAmount, refundParam.oldTransDate, refundParam.oldSlipNumber)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> {
                    setState(UIStates.Connecting, 0);
                    disposables.add(d);
                })
                .subscribe((result, unhandledError) -> {
                    if (unhandledError != null) {
                        setState(UIStates.Unknown, 0);
                    }

                    if (result.ex != null) {
                        if (result.ex instanceof HttpStatusException) {
                            int status = ((HttpStatusException) result.ex).getStatusCode();

                            // オプションサービス共通のエラー
                            _app.setErrorCode(OptionServiceErrorMap.getCommunicationError(status));

                            setState(UIStates.Failure, 0);
                            Amount.reset();
                        } else {
                            setState(UIStates.Unknown, 0);
                        }
                    } else {
                        if (result.cancelRes.result) {
                            transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);
                            transLogger.point(result);
                            _slipId = transLogger.insert();

                            setState(UIStates.Success, result.cancelReq.fare);
                        } else {
                            _app.setErrorCode(WatariViewModel.Reasons.getErrorCode(result.cancelRes.errorCode));
                            setState(UIStates.Failure, 0);
                            Amount.reset();
                        }
                    }
                });
    }

    public void pointCancelDemo() {
        cleanup();
        setState(UIStates.Read, 0);

        if (_tracks == null) {
            Timber.e("track info is null");
            return;
        }
        int slipNo = AppPreference.getSlipNoWatari();
        // 先に取っておく
        TransLogger transLogger = new TransLogger();
        transLogger.setRefundParam(_refundSlipId);
        RefundParam refundParam = transLogger.getRefundParam();

        final WatariPointCancel.Request request = new WatariPointCancel.Request();
        request.moneyKbn = 0; // マネー区分（0:和多利）
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
        request.authDateTime = dateFmt.format(new Date()); // 操作日時
        request.fare = refundParam.oldTransAmount; // 売上金額
        request.terminalProcNo = slipNo; // 通番（伝票番号）
        request.payMethod = "10"; // 支払方法（固定）
        request.productCd = "0000240"; // 商品コード
        request.driverCd = 999; // 乗務員コード
        request.cancelAuthDateTime = refundParam.oldTransDate;
        request.cancelProcNo = refundParam.oldSlipNumber;

        final WatariPointCancel.Response response = new WatariPointCancel.Response();
        response.result = true;
        response.errorCode = "";
        response.terminalNo = "3080600099999"; // 端末番号
        response.cardCampany = ""; // カード発行会社
        response.authDateTime = request.authDateTime; // 売上日時
        response.maskedMemberNo = ""; // マスクされたカード番号
        response.cancelAuthDateTime = request.cancelAuthDateTime; // 取消対象取引日時
        response.approvalNo = ""; // 承認番号
        response.terminalProcNo = request.terminalProcNo; // 伝票番号
        response.name = "MC TARO"; // 名前
        response.fare = request.fare; // 売上金額
        response.transactionType = 1; // 取引内容
        response.productCd = "0000240"; // 商品コード
        response.addPoint = 2; // 加算ポイント
        response.sumPoint = 192; // ポイント残高
        response.calidityPeriod = "2412"; // 有効期限

        WatariSettlement.WatariResult result = new WatariSettlement.WatariResult() {
            {
                cancelReq = request;
                cancelRes = response;
                ex = null;
                payTime = response.authDateTime;
                amount = request.fare;
            }
        };

        transLogger = new TransLogger();
        transLogger.setRefundParam(_refundSlipId);
        transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);
        transLogger.point(result);
        _slipId = transLogger.insert();

        setState(UIStates.Success, result.cancelReq.fare);
    }

    public void makeSound(@RawRes int id, VolumeTypes type) {
        _soundManager.load(MainApplication.getInstance(), id, 1);

        final float volume = type == VolumeTypes.Payment
                ? AppPreference.getSoundPaymentVolume() / 10f
                : AppPreference.getSoundGuidanceVolume() / 10f;

        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            soundPool.play(soundId, volume, volume, 1, 0, 1);
        });
    }

    public void stopSerchCard() {
        _watariSettlement.stopSearchCard();
    }

    public Completable suspend() {
        return Completable.create(CompletableEmitter::onComplete);
    }

    private void cleanup() {
        for (Disposable d : disposables) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }

        disposables.clear();
    }
}
