package jp.mcapps.android.multi_payment_terminal.logger;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class EventLogTree extends Timber.Tree {
    private final EventLogger _eventLogger;
    private final ExecutorService _logPool = Executors.newCachedThreadPool();

    public EventLogTree(EventLogger logger) {
        _eventLogger = logger;
    }

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        String logTag = getTag(tag);
        _logPool.submit(() -> {
            //ログ送信レベル設定　とりあえずINFO以上
            EventLog.Level level;
            switch (priority) {
                case Log.INFO :
                    level = EventLog.Level.INFO;
                    break;
                case Log.WARN :
                    level = EventLog.Level.WARN;
                    break;
                case Log.ERROR :
                    level = EventLog.Level.ERROR;
                    break;
                default:
                    return;
            }
            _eventLogger.log(level, logTag, message, t);
        });
    }

    private String getTag(String tag) {
        if (tag != null) {
            return tag;
        }

        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= 6) {
            return null;
        }

        String[] packageList = stackTrace[6].getClassName().split("\\.");
        return packageList[packageList.length - 1];
    }
}
