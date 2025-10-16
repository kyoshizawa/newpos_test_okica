package jp.mcapps.android.multi_payment_terminal.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;

import jp.mcapps.android.multi_payment_terminal.R;

public class ErrorDialog {
    public static void show(Context context, String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_error)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("閉じる", null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        alertDialog.show();

        // これをshow()の前でやるとエラーになる
        alertDialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        alertDialog.getWindow().
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    public static void show(Context context, String message) {
        show(context, "異常終了", message);
    }
}
