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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.error.OptionServiceErrorMap;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.Validator;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data.CardValidation;
import timber.log.Timber;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RequiresApi(api = Build.VERSION_CODES.M)
public class ValidationCheckViewModel extends ViewModel implements LifecycleObserver {
    public static final String successMessage = "有効性が確認できました\n\nありがとうございました";
    public static final String failureMessage = "有効性が確認できませんでした\n\nお取り扱いできません";
    public static final String unknownMessage = "結果が確認できません\nしばらくしてから\n再確認してください";

    private final Validator _validator = new Validator();
    private final List<Disposable> disposables = new ArrayList<>();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private String[] _tracks = null;
    private SoundManager _soundManager = SoundManager.getInstance();
    private MainApplication _app = MainApplication.getInstance();

    private enum Reasons {
        EncodeError("0001", MainApplication.getInstance().getString(R.string.error_type_option_service_5102)),
        Unregistered("0002", MainApplication.getInstance().getString(R.string.error_type_option_service_5103));

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
                    R.string.error_type_option_service_5109) + "@@@" + reason + "@@@";
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
    private Validator.ValidationResult _validationResult;

    private MutableLiveData<UIStates> _state = new MutableLiveData<>(UIStates.Read);
    public MutableLiveData<UIStates> getState() {
        return _state;
    }
    public void setState(UIStates state) {
        switch(state) {
            case Success:
                makeSound(R.raw.validation_ok, VolumeTypes.Payment);
                setResult(TransactionResults.SUCCESS);
                setFinishedMessage(successMessage);
                break;
            case Failure:
                makeSound(R.raw.validation_ng, VolumeTypes.Payment);
                setResult(TransactionResults.FAILURE);
                setFinishedMessage(failureMessage);
                break;
            case Unknown:
                makeSound(R.raw.validation_check, VolumeTypes.Payment);
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


    private MutableLiveData<TransactionResults> _result = new MutableLiveData<>();
    public MutableLiveData<TransactionResults> getResult() {
        return _result;
    }

    public String getDisplayName() {
        final String defaultName = _app.getString(R.string.btn_other_validation);
        final OptionService s = _app.getOptionService();

        if (s == null) return defaultName;

        int i = s.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION);
        return i >= 0 ? s.getFunc(i).getDisplayName() : defaultName;
    }
    public void setResult(TransactionResults result) {
        _handler.post(() -> {
            _result.setValue(result);
        });
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
        makeSound(R.raw.validation_start, VolumeTypes.Guide);
        searchCard();
    }

    public void searchCard() {
        cleanup();

        _validator.searchCard()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposables::add)
                .subscribe((tracks, error) -> {
                    if (error != null) {
                        if (error instanceof Validator.ValidatorException) {
                            int reason = ((Validator.ValidatorException) error).getReason();
                            switch (reason) {
                                case MagCardCallback.TIMEOUT_ERROR:
                                    setState(UIStates.ReadTimeout);
                                    _app.setErrorCode(_app.getString(R.string.error_type_option_service_5101));
                                    return;
                                case MagCardCallback.USER_CANCEL:
                                    return;
                                default:
                                    makeSound(R.raw.validation_read_error, VolumeTypes.Guide);
                                    break;
                            }

                            _handler.post(this::searchCard);
                        }
                        return;
                    }
                    _tracks = tracks;
                    if (AppPreference.isDemoMode()) {
                        validateDemo();
                    } else {
                        validate();
                    }
                });
    }

    public void validate() {
        cleanup();
        setState(UIStates.Read);

        if (_tracks == null) {
            Timber.e("track info is null");
            return;
        }

        _validator.validate(_tracks)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> {
                    setState(UIStates.Connecting);
                    disposables.add(d);
                })
                .subscribe((result, unhandledError) -> {
                    if (unhandledError != null) {
                        setState(UIStates.Unknown);
                    }

                    _validationResult = result;

                    if (result.ex != null) {
                        if (result.ex instanceof HttpStatusException) {
                            int status = ((HttpStatusException) result.ex).getStatusCode();

                            _validator.addRecord(result, false).subscribeOn(Schedulers.io())
                                    .doOnSubscribe(disposables::add)
                                    .doFinally(() -> {
                                        _app.setErrorCode(OptionServiceErrorMap.getCommunicationError(status));
                                        setState(UIStates.Failure);
                                        Amount.reset();
                                    })
                                    .subscribe(() -> { }, Timber::e);
                        } else {
                            setState(UIStates.Unknown);
                        }
                    } else {
                        CardValidation.Response response = result.res;

                        _validator.addRecord(result, false).subscribeOn(Schedulers.io())
                                .doOnSubscribe(disposables::add)
                                .doFinally(() -> {
                                    if (!response.result) {
                                        _app.setErrorCode(OptionServiceErrorMap.getCommunicationError(response.errorCode));
                                        setState(UIStates.Failure);
                                        Amount.reset();
                                    }
                                    else if (response.mediaResultInfo.isValid) {
                                        setState(UIStates.Success);
                                    } else {
                                        _app.setErrorCode(Reasons.getErrorCode(response.mediaResultInfo.reason));
                                        setState(UIStates.Failure);
                                        Amount.reset();
                                    }
                                })
                                .subscribe(() -> { }, Timber::e);
                    }
                });
    }

    public void validateDemo() {
        cleanup();
        setState(UIStates.Read);

        if (_tracks == null) {
            Timber.e("track info is null");
            return;
        }
        Validator.ValidationResult result = null;
        disposables.add(_validator.addRecord(result, true).subscribeOn(Schedulers.io()).subscribe(() -> {
            setState(UIStates.Success);
        }, e -> {
            setState(UIStates.Failure);
            Amount.reset();
        }));
    }

    public Completable sendResult() {
        if (AppPreference.isDemoMode()) {
            return Completable.create(emitter -> {
                emitter.onComplete();
            });
        } else {
            return _validator.sendResult();
        }
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
        _validator.stopSearchCard();
    }

    public Completable suspend() {
        return Completable.create(emitter -> {
            if (_validationResult == null) {
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            }

            _validator.addRecord(_validationResult, AppPreference.isDemoMode()).subscribeOn(Schedulers.io()).subscribe(() -> {
                sendResult().subscribeOn(Schedulers.io()).subscribe(() -> {
                    if (!emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                }, e -> {
                    Timber.e(e);
                    if (!emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                });
            }, e -> {
                Timber.e(e);
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            });
        });
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
