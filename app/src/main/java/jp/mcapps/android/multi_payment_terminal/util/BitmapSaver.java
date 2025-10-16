package jp.mcapps.android.multi_payment_terminal.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;

public class BitmapSaver {

    public static void saveReceipt(Bitmap bitmap) {
        // デバッグビルドの場合のみ保存する
        if (BuildConfig.DEBUG) {
            String filename = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.JAPAN).format(new Date()) + "_receipt" + ".png";

            Bitmap whiteBackgroundBitmap = convertWhiteBackground(bitmap);
            saveBitmapToInternalDir(whiteBackgroundBitmap, "receipts", filename);
        }
    }
    public static void saveLogo(Bitmap bitmap) {
        // デバッグビルドの場合のみ保存する
        if (BuildConfig.DEBUG) {
            String filename = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.JAPAN).format(new Date()) + "_logo" + ".png";

            Bitmap whiteBackgroundBitmap = convertWhiteBackground(bitmap);
            saveBitmapToInternalDir(whiteBackgroundBitmap, "receipts", filename);
        }
    }

    public static void deleteReceipts() {
        // デバッグビルドの場合のみ実施する
        if (BuildConfig.DEBUG) {
            File internalDir = new File(MainApplication.getInstance().getFilesDir(), "receipts");
            if (internalDir.exists()) {
                File[] files = internalDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        }
    }

    private static void saveBitmapToInternalDir(Bitmap bitmap, String folderName, String fileName) {
        File internalDir = new File(MainApplication.getInstance().getFilesDir(), folderName);
        if (!internalDir.exists()) {
            // 画像を保存するための新しいフォルダを作成する
            internalDir.mkdirs();
        }

        // 画像ファイルを作成する
        File file = new File(internalDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            // 画像の形式に応じて圧縮してファイルに書き込む
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 背景を白に変換
    private static Bitmap convertWhiteBackground(Bitmap originalBitmap) {
        final int margin = 36;
        // 元のbitmapと同じサイズの白背景のbitmapを作成
        Bitmap whiteBackgroundBitmap = Bitmap.createBitmap(
                originalBitmap.getWidth() + (margin * 2),
                originalBitmap.getHeight() + (margin * 2),
                Bitmap.Config.ARGB_8888);

        // Canvasを使って白背景のbitmapに描画
        Canvas canvas = new Canvas(whiteBackgroundBitmap);
        canvas.drawColor(Color.WHITE);

        // 元のbitmapを白背景のbitmapに描画
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(originalBitmap, margin, margin, paint);

        return whiteBackgroundBitmap;
    }
}
