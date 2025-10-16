package jp.mcapps.android.multi_payment_terminal.ui.ticket;

public class TicketGateLocation {
    public String[] stopIds;
    public String stopName;

    public TicketGateLocation(String[] ids, String name) {
        stopIds = ids;
        stopName = name;
    }

    public String GetName() {
        return stopName;
    }
}
