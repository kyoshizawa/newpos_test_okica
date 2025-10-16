package jp.mcapps.android.multi_payment_terminal.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AlertDialogFragment extends BaseDialogFragment {
    private static String TITLE = "title";
    private static String MESSAGE = "message";

    private PositiveClickListener _positiveClickListener = null;

    public static AlertDialogFragment newInstance(
            String title,
            String message,
            PositiveClickListener positiveClickListener) {

        final AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        fragment.setArguments(args);

        fragment._positiveClickListener = positiveClickListener;

        return fragment;
    }

    public static AlertDialogFragment newInstance(String message, PositiveClickListener positiveClickListener) {
        return newInstance("", message, positiveClickListener);
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
                });

        return builder.create();
    }
}
