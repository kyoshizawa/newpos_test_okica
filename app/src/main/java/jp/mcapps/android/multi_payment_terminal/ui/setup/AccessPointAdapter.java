package jp.mcapps.android.multi_payment_terminal.ui.setup;

import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.databinding.ItemAccessPointBinding;
import timber.log.Timber;

public class AccessPointAdapter extends RecyclerView.Adapter<AccessPointAdapter.ViewHolder> {
    private List<ScanResult> _list;
    private List<MutableLiveData<Boolean>> _checkStates;
    private Integer _checkPosition = null;
    private LifecycleOwner _owner;

    public AccessPointAdapter(List<ScanResult> list, LifecycleOwner owner) {
        _list = list;
        _checkStates = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            _checkStates.add(new MutableLiveData<>(false));
        }
        _owner = owner;
    }

    public interface onItemClickListener {
        void onClick(ScanResult scanResult);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAccessPointBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()), R.layout.item_access_point, parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult item = _list.get(position);

        holder._binding.setLifecycleOwner(_owner);
        holder._binding.setItem(item);
        holder._binding.setChecked(_checkStates.get(position));
        holder.itemView.setOnClickListener(v -> {
            _checkPosition = position;

            for (int i = 0; i < _checkStates.size(); i++) {
                _checkStates.get(i).setValue(i == position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return _list.size();
    }

    public ScanResult getCheckedItem() {
        return _checkPosition != null && _list.get(_checkPosition) != null
                ? _list.get(_checkPosition)
                : null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemAccessPointBinding _binding;

        public ViewHolder(ItemAccessPointBinding binding) {
            super(binding.getRoot());
            _binding = binding;
        }
    }
}
