package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemDeveloperHistoryRadioBinding;

public class DeveloperHistoryRadioAdapter extends ListAdapter<RadioData, DeveloperHistoryRadioAdapter.ViewHolder> {
    public DeveloperHistoryRadioAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<RadioData> DiffCallback = new DiffUtil.ItemCallback<RadioData>() {
        @Override
        public boolean areItemsTheSame(@NonNull RadioData oldItem, @NonNull RadioData newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull RadioData oldItem, @NonNull RadioData newItem) {
            return oldItem.id == newItem.id;
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDeveloperHistoryRadioBinding binding;

        ViewHolder(ItemDeveloperHistoryRadioBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RadioData item) {
            binding.setRadioData(item);
            binding.setIsLTE(item.networkType.equals("LTE"));
            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(ItemDeveloperHistoryRadioBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}
