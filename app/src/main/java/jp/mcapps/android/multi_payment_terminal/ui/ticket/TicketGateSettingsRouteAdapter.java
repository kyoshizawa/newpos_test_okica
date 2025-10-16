package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import jp.mcapps.android.multi_payment_terminal.R;

public class TicketGateSettingsRouteAdapter extends ArrayAdapter<TicketGateRoute> {
    public TicketGateSettingsRouteAdapter(Context contest) {
        super(contest, R.layout.item_ticket_gate_settings_dropdown);
    }

    public interface OnItemClickListener {
        void onItemClick(TicketGateRoute item);
    }

    private static TicketGateSettingsRouteAdapter.OnItemClickListener clickListener;

    public void setOnItemClickListener(TicketGateSettingsRouteAdapter.OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView)super.getView(position, convertView, parent);
        textView.setText(getItem(position).routeName);
        return textView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView)super.getDropDownView(position, convertView, parent);
        textView.setText(getItem(position).routeName);
        return textView;
    }
}
