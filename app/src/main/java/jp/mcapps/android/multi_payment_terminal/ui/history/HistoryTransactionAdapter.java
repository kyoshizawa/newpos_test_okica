package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.TransHead;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemHistoryTransactionBinding;

public class HistoryTransactionAdapter extends ListAdapter<TransHead, HistoryTransactionAdapter.ViewHolder> {
    public HistoryTransactionAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<TransHead> DiffCallback = new DiffUtil.ItemCallback<TransHead>() {
        @Override
        public boolean areItemsTheSame(@NonNull TransHead oldItem, @NonNull TransHead newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TransHead oldItem, @NonNull TransHead newItem) {
            return oldItem.id == newItem.id;
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryTransactionBinding binding;

        ViewHolder(ItemHistoryTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TransHead item) {
            binding.setTransHead(item);
            if (item.transResult == TransMap.RESULT_SUCCESS) {
                binding.setColorResource(MainApplication.getInstance().getResources().getColor(R.color.black, MainApplication.getInstance().getTheme()));
                binding.setResultVisible(View.GONE);
            } else {
                binding.setColorResource(MainApplication.getInstance().getResources().getColor(R.color.bar_emoney_red, MainApplication.getInstance().getTheme()));
                binding.setResultVisible(View.VISIBLE);
            }

            binding.btnTransHistoryDetail.setOnClickListener(v -> {
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable run = () -> {
                    SlipData slipData = DBManager.getSlipDao().getOneById(item.id);
                    handler.post(() -> {
                        String transName = String.format(Locale.JAPANESE, "機器通番 %d", slipData.termSequence);
                        CommonClickEvent.RecordClickOperation((String) ((Button) v).getText(), transName, true);

                        Bundle args = new Bundle();
                        args.putInt("SLIP_ID", slipData.id);

                        final Activity activity = (Activity) v.getContext();
                        if (activity == null) return;
                        v.post(() -> {
                            NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host,
                                    R.id.action_navigation_history_transaction_to_navigation_history_transaction_detail, args);
                        });
                    });
                };
                new Thread(run).start();
            });

            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public HistoryTransactionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new HistoryTransactionAdapter.ViewHolder(ItemHistoryTransactionBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryTransactionAdapter.ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}
