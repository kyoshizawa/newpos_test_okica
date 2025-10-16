package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemHistoryOperationBinding;
import timber.log.Timber;

public class HistoryOperationAdapter extends ListAdapter<OperationData, HistoryOperationAdapter.ViewHolder> {
    public HistoryOperationAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<OperationData> DiffCallback = new DiffUtil.ItemCallback<OperationData>() {
        @Override
        public boolean areItemsTheSame(@NonNull OperationData oldItem, @NonNull OperationData newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull OperationData oldItem, @NonNull OperationData newItem) {
            return oldItem.id == newItem.id;
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryOperationBinding binding;

        ViewHolder(ItemHistoryOperationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OperationData item) {
            binding.setOperationData(item);

            //ダイアログ名の確認
            if (item.dialogName != null) {
                binding.valueHistoryOperationDialogName.setVisibility(View.VISIBLE);
            } else {
                binding.valueHistoryOperationDialogName.setVisibility(View.GONE);
            }

            //操作種別によってラベル名を変更
            if(item.operationType == 1) {
                binding.groupOperationDetail.setVisibility(View.VISIBLE);
                binding.labelHistoryOperationDetail2.setText("入力内容");
                binding.valueHistoryOperationDetail2.setText(item.inputData);
            } else if (item.operationType == 2){
                binding.groupOperationDetail.setVisibility(View.VISIBLE);
                binding.labelHistoryOperationDetail2.setText("入力桁数");
                binding.valueHistoryOperationDetail2.setText(String.valueOf(item.inputDigitNumber));
            } else {
                binding.groupOperationDetail.setVisibility(View.GONE);
            }

            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new HistoryOperationAdapter.ViewHolder(ItemHistoryOperationBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryOperationAdapter.ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}