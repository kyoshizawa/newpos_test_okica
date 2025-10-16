package jp.mcapps.android.multi_payment_terminal.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationUtil {

    public static final int SERVICE_ID_WIFIP2P = 1;

    public static final int SERVICE_ID_LOGSEND = 2;

    public static Notification Create(Context context, String channelId, String channelName, int icon) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_MIN  // 最低レベルの通知（目立たない）
            );
            channel.setShowBadge(false);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        return new NotificationCompat.Builder(context, channelId)
                .setContentTitle("サービス起動中") // 通知に表示される内容
                .setContentText("")
                .setSmallIcon(icon)
                .setOngoing(true) // ユーザーが消せない通知
                .build();
    }
}
