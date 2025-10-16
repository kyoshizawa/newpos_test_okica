package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.app.AlertDialog;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemHistoryErrorBinding;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import timber.log.Timber;

public class HistoryErrorAdapter extends ListAdapter<ErrorData, HistoryErrorAdapter.ViewHolder> {
    public HistoryErrorAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<ErrorData> DiffCallback = new DiffUtil.ItemCallback<ErrorData>() {
        @Override
        public boolean areItemsTheSame(@NonNull ErrorData oldItem, @NonNull ErrorData newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ErrorData oldItem, @NonNull ErrorData newItem) {
            return oldItem.id == newItem.id;
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryErrorBinding binding;

        ViewHolder(ItemHistoryErrorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void bind(ErrorData item) {
            binding.setErrorData(item);

            binding.btnErrorHistoryDetail.setOnClickListener(v -> {
                int iconId = item.level == 0
                        ? R.drawable.ic_error
                        : R.drawable.ic_warning;

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext())
                        .setIcon(iconId)
                        .setTitle(item.title)
                        .setMessage(item.message + "\n\n" + item.detail)
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
            });
            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public HistoryErrorAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new HistoryErrorAdapter.ViewHolder(ItemHistoryErrorBinding.inflate(layoutInflater, parent, false));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NonNull HistoryErrorAdapter.ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}
