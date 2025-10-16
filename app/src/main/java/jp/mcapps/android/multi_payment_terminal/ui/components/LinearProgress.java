package jp.mcapps.android.multi_payment_terminal.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.mcapps.android.multi_payment_terminal.R;

@SuppressLint("ViewConstructor")
public class LinearProgress extends LinearLayout {
    private final TextView[] items;
    private int idx = 0;
    private int nextIdx = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public LinearProgress(@NonNull Context context) {
        this(context, null);
    }

    public LinearProgress(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearProgress(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(getContext(), R.layout.linear_progress, this);
        items = new TextView[5];
        items[0] = findViewById(R.id.item1);
        items[1] = findViewById(R.id.item2);
        items[2] = findViewById(R.id.item3);
        items[3] = findViewById(R.id.item4);
        items[4] = findViewById(R.id.item5);

        items[0].setText("●");
        for (int i = 1; i < items.length; i++) {
            items[i].setText("〇");
        }

        next();
    }

    private void next() {
        handler.postDelayed(() -> {

            items[idx].setText("〇");
            items[nextIdx].setText("●");
            idx = nextIdx;
            nextIdx = (nextIdx + 1) % items.length;
            next();
        }, 300);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        handler.removeCallbacksAndMessages(null);
    }
}
