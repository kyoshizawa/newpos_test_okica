package jp.mcapps.android.multi_payment_terminal.ui.ticket;


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.databinding.ItemTicketInfoBinding;

public class TicketSearchResultsAdapter extends ListAdapter<TicketCategoryData, TicketSearchResultsAdapter.ViewHolder> {

    public TicketSearchResultsAdapter() {
        super(DiffCallback);
    }

    private static final DiffUtil.ItemCallback<TicketCategoryData> DiffCallback = new DiffUtil.ItemCallback<TicketCategoryData>() {
        @Override
        public boolean areItemsTheSame(@NonNull TicketCategoryData oldItem, @NonNull TicketCategoryData newItem) {
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TicketCategoryData oldItem, @NonNull TicketCategoryData newItem) {
            return false;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final TicketSearchResultsAdapter.ViewHolder holder = new TicketSearchResultsAdapter.ViewHolder(ItemTicketInfoBinding.inflate(layoutInflater, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private final ItemTicketInfoBinding binding;

        ViewHolder(ItemTicketInfoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TicketCategoryData item) {
            binding.setTicketCategoryData(item);
            binding.executePendingBindings();
        }
    }
}
