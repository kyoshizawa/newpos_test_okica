package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.graphics.Typeface;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import kotlin.jvm.JvmStatic;
import timber.log.Timber;

public class CustomBindingAdapter {
    @BindingAdapter("customFont")
    @JvmStatic
    public static void setCustomFont(TextView textView, String dummy) {
        Typeface customTypeface = FontHelper.getFont();
        textView.setTypeface(customTypeface);
    }
}
