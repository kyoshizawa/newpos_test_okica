package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemProductBinding;
import jp.mcapps.android.multi_payment_terminal.ui.pos.ProductCategorySelectModel;
import jp.mcapps.android.multi_payment_terminal.ui.pos.ProductSelectAdapter;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemTicketClassBinding;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketClass;

public class TicketClassAdapter extends ListAdapter<TicketClassData, TicketClassAdapter.ViewHolder> {
    public TicketClassAdapter() {
        super(DiffCallback);
    }

    public interface OnItemClickListener {
        void onItemClick(TicketClassData item);
    }

    private static TicketClassAdapter.OnItemClickListener clickListener;

    public void setOnItemClickListener(TicketClassAdapter.OnItemClickListener listener) {
        this.clickListener = listener;
    }

    private static final DiffUtil.ItemCallback<TicketClassData> DiffCallback = new DiffUtil.ItemCallback<TicketClassData>() {
        @Override
        public boolean areItemsTheSame(@NonNull TicketClassData oldItem, @NonNull TicketClassData newItem) {
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TicketClassData oldItem, @NonNull TicketClassData newItem) {
            return false;
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTicketClassBinding binding;

        ViewHolder(ItemTicketClassBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressWarnings("deprecation")
        private void setBackground(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.border_pin_input));
            } else {
                itemView.setBackgroundDrawable(ContextCompat.getDrawable(itemView.getContext(), R.drawable.border_pin_input));
            }
        }

        public void bind(TicketClassData item) {
            binding.setTicketClassData(item);

            setBackground();

            binding.itemTicketClassConstraint.setOnClickListener(v -> {
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
    public TicketClassAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final TicketClassAdapter.ViewHolder holder = new TicketClassAdapter.ViewHolder(ItemTicketClassBinding.inflate(layoutInflater, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TicketClassAdapter.ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}
