package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemHistoryAggregateBinding;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;

public class HistoryAggregateAdapter extends ListAdapter<AggregateData, HistoryAggregateAdapter.ViewHolder> {
    public HistoryAggregateAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<AggregateData> DiffCallback = new DiffUtil.ItemCallback<AggregateData>() {
        @Override
        public boolean areItemsTheSame(@NonNull AggregateData oldItem, @NonNull AggregateData newItem) {
            return oldItem.aggregateStartDatetime.compareTo(newItem.aggregateStartDatetime) == 0;
        }

        @Override
        public boolean areContentsTheSame(@NonNull AggregateData oldItem, @NonNull AggregateData newItem) {
            return oldItem.aggregateStartDatetime.compareTo(newItem.aggregateStartDatetime) == 0;
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryAggregateBinding binding;

        ViewHolder(ItemHistoryAggregateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void bind(AggregateData item) {
            binding.setAggregateData(item);
            binding.btnAggregateReprinting.setOnClickListener(v -> {
                CommonClickEvent.RecordClickOperation("再印刷", String.format(Locale.JAPANESE, "%d回前の集計", item.aggregateHistoryOrder), false);

                //集計再印字
                PrinterManager printerManager = PrinterManager.getInstance();
                printerManager.print_Aggregate(v,item.aggregateHistoryOrder, AppPreference.isAggregateDetail());
            });
            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new HistoryAggregateAdapter.ViewHolder(ItemHistoryAggregateBinding.inflate(layoutInflater, parent, false));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }


}