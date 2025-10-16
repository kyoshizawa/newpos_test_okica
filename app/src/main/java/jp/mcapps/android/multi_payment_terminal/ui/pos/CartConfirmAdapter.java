package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemCartBinding;


public class CartConfirmAdapter extends ListAdapter<CartModel, CartConfirmAdapter.ViewHolder> {
        public CartConfirmAdapter() {
            super(DiffCallback);
        }

        public interface OnItemClickListener {
            void onItemClick(CartModel item);
            void onInputClick(CartModel item);
            void onSelectInputTypeClick(CartModel item);
        }

        private static OnItemClickListener clickListener;

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.clickListener = listener;
        }

        private static final DiffUtil.ItemCallback<CartModel> DiffCallback = new DiffUtil.ItemCallback<CartModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull CartModel oldItem, @NonNull CartModel newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull CartModel oldItem, @NonNull CartModel newItem) {
                return oldItem.id == newItem.id;
            }
        };

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemCartBinding binding;

            ViewHolder(ItemCartBinding binding) {
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

            public void bind(CartModel item) {
                binding.setProduct(item);

                setBackground();

                binding.itemProductConstraint.setOnClickListener( v-> {
                    if (clickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            clickListener.onItemClick(getItem(position));
                        }
                    }
                });

                binding.posInputButton.setOnClickListener(v ->{
                    if (clickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            clickListener.onInputClick(getItem(position));
                        }
                    }
                });

                binding.posSelectInputType.setOnClickListener(v -> {
                    if(clickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            clickListener.onSelectInputTypeClick(getItem(position));
                        }
                    }
                });

                // 単価変更後は色を変更
                if (item.isCustomPrice) {
                    binding.setColorResource(MainApplication.getInstance().getResources().getColor(R.color.text_pos_custom_price, MainApplication.getInstance().getTheme()));
                } else {
                    binding.setColorResource(MainApplication.getInstance().getResources().getColor(R.color.black, MainApplication.getInstance().getTheme()));
                }

                binding.executePendingBindings();
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            final ViewHolder holder = new ViewHolder(ItemCartBinding.inflate(layoutInflater, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(getItem(position));
    }
}
