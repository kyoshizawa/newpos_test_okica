package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.content.Context;
import android.graphics.Typeface;

public class FontHelper {
    private static FontHelper instance;
    private Typeface font;

    private FontHelper(Context context) {
        font = Typeface.createFromAsset(context.getAssets(), "MPLUS1p-Bold.ttf");
    }

    public static void initialize(Context context) {
        if (instance == null) {
            instance = new FontHelper(context);
        }
    }

    public static Typeface getFont() {
        if (instance == null) {
            throw new IllegalStateException("FontHelper is not initialized. Call initialize() method first.");
        }
        return instance.font;
    }
}
