package jp.mcapps.android.multi_payment_terminal.util;

import android.util.Log;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import timber.log.Timber;

/**
 * カードない情報など本番運用でログ出力したくないデータ用のロガークラス
 */
public class DebugLog {
    private final String _tag;

    public DebugLog(String tag) {
        _tag = tag;
    }

    public void v(String format, Object... args) {
        print(Log.VERBOSE, format, args);
    }

    public void d(String format, Object... args) {
        print(Log.DEBUG, format, args);
    }

    public void i(String format, Object... args) {
        print(Log.INFO, format, args);
    }

    public void w(String format, Object... args) {
        print(Log.WARN, format, args);
    }

    public void e(String format, Object... args) {
        print(Log.ERROR, format, args);
    }

    public void wtf(String format, Object... args) {
        print(Log.ASSERT, format, args);
    }

    private void print(int priority, String format, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.println(priority, _tag, String.format(format, args));
        }
    }
}
