package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemTicketClassBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemTicketEmbarkBinding;

public class TicketEmbarkAdapter extends ListAdapter<TicketEmbarkData, TicketEmbarkAdapter.ViewHolder> {
    public TicketEmbarkAdapter() {
        super(DiffCallback);
    }

    public interface OnItemClickListener {
        void onItemClick(TicketEmbarkData item);
    }

    private static TicketEmbarkAdapter.OnItemClickListener clickListener;

    public void setOnItemClickListener(TicketEmbarkAdapter.OnItemClickListener listener) {
        this.clickListener = listener;
    }

    private static final DiffUtil.ItemCallback<TicketEmbarkData> DiffCallback = new DiffUtil.ItemCallback<TicketEmbarkData>() {
        @Override
        public boolean areItemsTheSame(@NonNull TicketEmbarkData oldItem, @NonNull TicketEmbarkData newItem) {
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TicketEmbarkData oldItem, @NonNull TicketEmbarkData newItem) {
            return false;
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTicketEmbarkBinding binding;

        ViewHolder(ItemTicketEmbarkBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressWarnings("deprecation")
        private void setBackground() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.border_pin_input));
            } else {
                itemView.setBackgroundDrawable(ContextCompat.getDrawable(itemView.getContext(), R.drawable.border_pin_input));
            }
        }

        public void bind(TicketEmbarkData item) {
            binding.setTicketEmbarkData(item);

            setBackground();

            binding.itemTicketEmbarkConstraint.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onItemClick(getItem(position));
                    }
                }
            });
            binding.executePendingBindings();
        }
    }

    @NonNull
    @Override
    public TicketEmbarkAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final TicketEmbarkAdapter.ViewHolder holder = new TicketEmbarkAdapter.ViewHolder(ItemTicketEmbarkBinding.inflate(layoutInflater, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TicketEmbarkAdapter.ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}
