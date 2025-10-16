package jp.mcapps.android.multi_payment_terminal.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ConfirmDialog extends BaseDialogFragment {
    private static String TITLE = "title";
    private static String MESSAGE = "message";

    private PositiveClickListener _positiveClickListener = null;
    private NegativeClickListener _negativeClickListener = null;

    public static ConfirmDialog newInstance(
            String title,
            String message,
            PositiveClickListener positiveClickListener,
            NegativeClickListener negativeClickListener) {

        final ConfirmDialog fragment = new ConfirmDialog();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        fragment.setArguments(args);

        fragment._positiveClickListener = positiveClickListener;
        fragment._negativeClickListener = negativeClickListener;

        return fragment;
    }

    public static ConfirmDialog newInstance(String title, String message, PositiveClickListener positiveClickListener) {
        return newInstance(title, message, positiveClickListener, null);
    }

    public static ConfirmDialog newInstance(String message, PositiveClickListener positiveClickListener) {
        return newInstance("確認", message, positiveClickListener, null);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final String title = getArguments().getString(TITLE);
        final String message = getArguments().getString(MESSAGE);

        setCancelable(false);

        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("はい", (dialog, which) -> {
                    if (_positiveClickListener != null) {
                        _positiveClickListener.onClick();
                    }
                    dismiss();
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    if (_negativeClickListener != null) {
                        _negativeClickListener.onClick();
                    }
                    dismiss();
                });

        return builder.create();
    }
}
