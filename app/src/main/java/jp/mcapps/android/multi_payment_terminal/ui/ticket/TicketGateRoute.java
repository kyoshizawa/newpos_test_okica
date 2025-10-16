package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import java.io.Serializable;

public class TicketGateRoute implements Serializable {
    public String routeId;
    public String routeName;

    public TicketGateRoute(String id, String name) {
        routeId = id;
        routeName = name;
    }
}
