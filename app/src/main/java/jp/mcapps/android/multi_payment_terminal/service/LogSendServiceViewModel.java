package jp.mcapps.android.multi_payment_terminal.service;

import java.util.concurrent.TimeUnit;

import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.logger.EventLogger;
import timber.log.Timber;

public class LogSendServiceViewModel extends ViewModel {
    private final EventLogger _eventLogger;
    private Disposable _disposable;
    private boolean isSending = false;

    public LogSendServiceViewModel(EventLogger eventLogger) {
        _eventLogger = eventLogger;
    }

    public void start() {
        if (_disposable == null) {
            Timber.i("定期ログ送信起動");
            _disposable = Observable
                    .interval(1, TimeUnit.MINUTES)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(t -> {
                        if (!isSending) {
                            isSending = true;
                            _eventLogger.submit();
                            isSending = false;
                        }
                    });
        }
    }

    public void stop() {
        if (_disposable != null && !_disposable.isDisposed()) {
            Timber.i("定期ログ送信停止");
            _disposable.dispose();
        }
    }
}
