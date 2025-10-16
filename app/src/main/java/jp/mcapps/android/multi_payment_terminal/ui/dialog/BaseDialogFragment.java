package jp.mcapps.android.multi_payment_terminal.ui.dialog;

import android.view.View;

import java.util.Objects;

import androidx.fragment.app.DialogFragment;

public abstract class BaseDialogFragment extends DialogFragment {
    public interface PositiveClickListener {
        void onClick();
    }

    public interface NegativeClickListener {
        void onClick();
    }

    @Override
    public void onResume() {
        super.onResume();
        hideNavigationBar();
    }

    private void hideNavigationBar() {
        View decorView = Objects.requireNonNull(getDialog()).getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }
}
