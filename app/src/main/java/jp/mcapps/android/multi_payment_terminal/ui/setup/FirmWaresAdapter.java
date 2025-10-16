package jp.mcapps.android.multi_payment_terminal.ui.setup;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.FirmWareInfo;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemFirmwaresBinding;

public class FirmWaresAdapter extends RecyclerView.Adapter<FirmWaresAdapter.ViewHolder> {
    private List<FirmWareInfo> _list;
    private onItemClickListener _listener;

    public FirmWaresAdapter(List<FirmWareInfo> list) {
        _list = list;
    }

    public interface onItemClickListener {
        void onClick(FirmWareInfo firmWareInfo);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFirmwaresBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()), R.layout.item_firmwares, parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FirmWareInfo item = _list.get(position);

        holder._binding.setItem(item);
        holder.itemView.setOnClickListener(v -> {
            _listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return _list.size();
    }

    public void setItemClickListener(onItemClickListener listener) {
        _listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemFirmwaresBinding _binding;

        public ViewHolder(ItemFirmwaresBinding binding) {
            super(binding.getRoot());
            _binding = binding;
        }
    }
}
