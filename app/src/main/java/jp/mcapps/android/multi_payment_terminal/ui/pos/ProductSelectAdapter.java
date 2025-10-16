package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemProductBinding;


public class ProductSelectAdapter extends ListAdapter<ProductCategorySelectModel, ProductSelectAdapter.ViewHolder> {
        private Typeface _typeFace;

        public ProductSelectAdapter(Typeface typeface) {
            super(DiffCallback);
            _typeFace = typeface;
        }

        public interface OnItemClickListener {
            void onItemClick(ProductCategorySelectModel item);
        }

        private static OnItemClickListener clickListener;

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.clickListener = listener;
        }

        private static final DiffUtil.ItemCallback<ProductCategorySelectModel> DiffCallback = new DiffUtil.ItemCallback<ProductCategorySelectModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull ProductCategorySelectModel oldItem, @NonNull ProductCategorySelectModel newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull ProductCategorySelectModel oldItem, @NonNull ProductCategorySelectModel newItem) {
                return oldItem.id == newItem.id;
            }
        };

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemProductBinding binding;

            ViewHolder(ItemProductBinding binding) {
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

            public void bind(ProductCategorySelectModel item) {
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
                binding.executePendingBindings();
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            final ViewHolder holder = new ViewHolder(ItemProductBinding.inflate(layoutInflater, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(getItem(position));
    }
}
