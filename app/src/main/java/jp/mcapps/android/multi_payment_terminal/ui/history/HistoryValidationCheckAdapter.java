package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckHistoryData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemHistoryValidationCheckBinding;

public class HistoryValidationCheckAdapter extends ListAdapter<ValidationCheckHistoryData, HistoryValidationCheckAdapter.ViewHolder> {
    public HistoryValidationCheckAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<ValidationCheckHistoryData> DiffCallback = new DiffUtil.ItemCallback<ValidationCheckHistoryData>() {
        @Override
        public boolean areItemsTheSame(@NonNull ValidationCheckHistoryData oldItem, @NonNull ValidationCheckHistoryData newItem) {
            return oldItem.historyId == newItem.historyId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ValidationCheckHistoryData oldItem, @NonNull ValidationCheckHistoryData newItem) {
            return oldItem.historyId == newItem.historyId;
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryValidationCheckBinding binding;

        ViewHolder(ItemHistoryValidationCheckBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ValidationCheckHistoryData item) {
            binding.setHistory(item);

            //成功を黒字、失敗/未確認を赤字で表示するように変更
            if (item.termResult == 0) {
                binding.setColorResource(MainApplication.getInstance().getResources().getColor(R.color.black, MainApplication.getInstance().getTheme()));
            } else {
                binding.setColorResource(MainApplication.getInstance().getResources().getColor(R.color.bar_emoney_red, MainApplication.getInstance().getTheme()));
            }
            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new HistoryValidationCheckAdapter.ViewHolder(ItemHistoryValidationCheckBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}
