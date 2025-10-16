package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemDeveloperHistoryGpsBinding;

public class DeveloperHistoryGpsAdapter extends ListAdapter<GpsData, DeveloperHistoryGpsAdapter.ViewHolder> {
    public DeveloperHistoryGpsAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<GpsData> DiffCallback = new DiffUtil.ItemCallback<GpsData>() {
        @Override
        public boolean areItemsTheSame(@NonNull GpsData oldItem, @NonNull GpsData newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull GpsData oldItem, @NonNull GpsData newItem) {
            return oldItem.id == newItem.id;
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDeveloperHistoryGpsBinding binding;

        ViewHolder(ItemDeveloperHistoryGpsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(GpsData item) {
            binding.setGpsData(item);
            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new DeveloperHistoryGpsAdapter.ViewHolder(ItemDeveloperHistoryGpsBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}