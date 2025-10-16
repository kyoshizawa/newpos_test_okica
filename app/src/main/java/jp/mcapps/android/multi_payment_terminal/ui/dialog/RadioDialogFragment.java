package jp.mcapps.android.multi_payment_terminal.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class RadioDialogFragment extends DialogFragment {
    public interface RadioDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onRadioButtonClick(DialogFragment dialog, int which);
        default void onDialogNegativeClick(DialogFragment dialog) {
            // 何もしない
        }
    }

    RadioDialogListener listener;
    static boolean _cancelButton = false;

    public static RadioDialogFragment newInstance(String title, String[] strings, String buttonText, int checked, boolean cancelButton) {

        _cancelButton = cancelButton;

        final RadioDialogFragment fragment = new RadioDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putStringArray("strings", strings);
        args.putString("buttonText", buttonText);
        args.putInt("checked", checked);
        fragment.setArguments(args);

        return fragment;
    }

    public static RadioDialogFragment newInstance(String title, String[] strings) {
        return newInstance(title, strings, "OK", 0, false);
    }
    public static RadioDialogFragment newInstance(String title, String[] strings, boolean cancelButton) {
        return newInstance(title, strings, "OK", 0, true);
    }
    public static RadioDialogFragment newInstance(String title, String[] strings, String buttonText) {
        return newInstance(title, strings, buttonText, 0, false);
    }
    public static RadioDialogFragment newInstance(String title, String[] strings, int checked) {
        return newInstance(title, strings, "OK", checked, false);
    }
    public static RadioDialogFragment newInstance(String title, ArrayList<String> strings) {
        return newInstance(title, strings.toArray(new String[0]));
    }
    public static RadioDialogFragment newInstance(String title, ArrayList<String> strings, String buttonText) {
        return newInstance(title, strings.toArray(new String[0]), buttonText, 0, false);
    }
    public static RadioDialogFragment newInstance(String title, ArrayList<String> strings, int checked) {
        return newInstance(title, strings.toArray(new String[0]), "OK", checked, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final String title = getArguments().getString("title");
        final String[] strings = getArguments().getStringArray("strings");
        final String buttonText = getArguments().getString("buttonText");
        final int checked = getArguments().getInt("checked");

        // 画面外のタップを無効化
        this.setCancelable(false);

        if (_cancelButton == true) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setSingleChoiceItems(strings, checked, (dialog, which) -> {
                        listener.onRadioButtonClick(this, which);
                    })
                    .setPositiveButton(buttonText, (dialog, which) -> {
                        listener.onDialogPositiveClick(this);
                    })
                    .setNegativeButton("CANCEL", (dialog, which) -> {
                        listener.onDialogNegativeClick(this);
                    });
            return builder.create();
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setSingleChoiceItems(strings, checked, (dialog, which) -> {
                        listener.onRadioButtonClick(this, which);
                    })
                    .setPositiveButton(buttonText, (dialog, which) -> {
                        listener.onDialogPositiveClick(this);
                    });
            return builder.create();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (RadioDialogListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("must implement RadioDialogListener");
        }
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
