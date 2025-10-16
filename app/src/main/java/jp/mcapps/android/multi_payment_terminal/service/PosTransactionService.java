package jp.mcapps.android.multi_payment_terminal.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.model.pos.TransactionUploader;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import timber.log.Timber;

public class PosTransactionService extends Service {

    public static void startService() {
        Timber.d("startService is kicked");
        final MainApplication app = MainApplication.getInstance();
        app.startService(new Intent(app, PosTransactionService.class));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final CompositeDisposable disposables = new CompositeDisposable();

    private final PublishSubject<Long> manualSubject = PublishSubject.create();

    @Override
    public void onCreate() {
        super.onCreate();
        // onCreateはサービス開始時に一度だけ呼ばれる
        Timber.d("onCreate");

        Scheduler scheduler = Schedulers.single();

        // 15分の定期リトライ
        Observable<Long> intervalObservable = Observable.interval(15, TimeUnit.MINUTES)
                .observeOn(scheduler)
                .doOnNext(n -> uploadTransactions());

        // 手動
        Observable<Long> manualObservable = manualSubject
                .observeOn(scheduler)
                .doOnNext(n -> uploadTransactions())
                .switchMap(ignored -> Observable.error(new RuntimeException("manually uploaded")));

        // 開始
        disposables.add(
                Observable.merge(intervalObservable, manualObservable)
                        .retry()
                        .subscribe()
        );
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        int flag = super.onStartCommand(intent, flags, startId);
        // onStartCommandはstartServiceが呼ばれる度に呼ばれる
        Timber.d("onStartCommand");

        // 手動開始をemitする
        manualSubject.onNext(System.currentTimeMillis());
        return flag;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");

        // 後片付け
        disposables.dispose();
    }

    private void uploadTransactions() {
        Timber.d("uploadTransactions on thread: %s", Thread.currentThread().getName());
        TransactionUploader uploader = new TransactionUploader();
        try {
            uploader.uploadTransactions();
        } catch (HttpStatusException e) {
            Timber.e(e, "(paypf) Error on uploading transactions: (%s) %s", e.getStatusCode(), e.getBody());
        } catch (IOException e) {
            Timber.e(e, "(paypf) Error on uploading transactions (io)");
        } catch (Exception e) {
            Timber.e(e, "(paypf) Error on uploading transactions (unknown)");
        }
    }
}
