package jp.mcapps.android.multi_payment_terminal.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;

public class CancelConfirmDialog extends BaseDialogFragment {
    private static String TITLE = "title";
    private static String MESSAGE = "message";

    private PositiveClickListener _positiveClickListener = null;
    private NegativeClickListener _negativeClickListener = null;

    public static CancelConfirmDialog newInstance(
            String title,
            String message,
            PositiveClickListener positiveClickListener,
            NegativeClickListener negativeClickListener) {

        final CancelConfirmDialog fragment = new CancelConfirmDialog();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        fragment.setArguments(args);

        fragment._positiveClickListener = positiveClickListener;
        fragment._negativeClickListener = negativeClickListener;

        return fragment;
    }

    public static CancelConfirmDialog newInstance(String title, String message, PositiveClickListener positiveClickListener) {
        return newInstance(title, message, positiveClickListener, null);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final String title = getArguments().getString(TITLE);
        final String message = getArguments().getString(MESSAGE);

        TextView titleView = new TextView(getActivity());
        titleView.setText(title);
        titleView.setTextSize(24);
        titleView.setTextColor(Color.WHITE);
        titleView.setBackgroundColor(getResources().getColor(R.color.title_color, MainApplication.getInstance().getTheme()));
        titleView.setPadding(20, 20, 20, 20);
        titleView.setGravity(Gravity.CENTER);

        TextView msgView = new TextView(getActivity());
        msgView.setText(message);
        msgView.setTextSize(20);
        msgView.setTextColor(Color.BLACK);
        msgView.setPadding(20, 20, 20, 40);

        setCancelable(false);

        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setCustomTitle(titleView)
                .setView(msgView)
                .setNeutralButton("はい", (dialog, which) -> {
                    CommonClickEvent.RecordClickOperation("はい", title, true);
                    if (_positiveClickListener != null) {
                        _positiveClickListener.onClick();
                    }
                    dismiss();
                })
                .setPositiveButton("いいえ", (dialog, which) -> {
                    CommonClickEvent.RecordClickOperation("いいえ", title, true);
                    if (_negativeClickListener != null) {
                        _negativeClickListener.onClick();
                    }
                    dismiss();
                });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null) {
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.WHITE);
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundColor(getResources().getColor(R.color.ok_btn_color, MainApplication.getInstance().getTheme()));

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(getResources().getColor(R.color.cancel_btn_color, MainApplication.getInstance().getTheme()));
        }
    }
}
