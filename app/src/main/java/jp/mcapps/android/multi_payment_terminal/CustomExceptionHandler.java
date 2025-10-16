package jp.mcapps.android.multi_payment_terminal;

import android.util.Log;

import androidx.annotation.NonNull;

import jp.mcapps.android.multi_payment_terminal.logger.EventLogger;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler _defaultUeh;
    private boolean _isCrash = false;
    private final EventLogger _eventLogger;

    public CustomExceptionHandler(Thread.UncaughtExceptionHandler handler, EventLogger logger) {
        _defaultUeh = handler;
        _eventLogger = logger;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        //カスタムなエラー処理
        try {
            if (_isCrash) {
                return;
            }
            _isCrash = true;

            _eventLogger.appCrash(String.format("try catchされないExceptionを検出しました : %s", Log.getStackTraceString(e)));

        } finally {
            //(1)か(2)のパターンを行わないとANRが発生することに注意

            //パターン(1)デフォルトハンドラーのラッピング
            // デフォルトのエラー処理を発生させ終わりにする ダイアログが表示される
            _defaultUeh.uncaughtException(t, e);

            //パターン(2)下記処理を有効にすると、異常終了した旨のダイアログが非表示となる
            //Activityを強制終了 ライフサイクル無視
            //android.os.Process.killProcess(android.os.Process.myPid());
            //VM終了
            //System.exit(10);
        }
    }
}
