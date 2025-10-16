package jp.mcapps.android.multi_payment_terminal.util;

import android.content.Context;

import java.io.File;

public class CacheUtils {

    // アプリのキャッシュをクリアするメソッド
    public static void clearCache(Context context) {
        try {
            // コンテキストからキャッシュディレクトリを取得
            File cacheDir = context.getCacheDir();

            // キャッシュディレクトリ内のファイルをすべて削除
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDir(cacheDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ディレクトリ内のファイルを再帰的に削除するメソッド
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // ディレクトリ自体を削除
        return dir != null && dir.delete();
    }
}

