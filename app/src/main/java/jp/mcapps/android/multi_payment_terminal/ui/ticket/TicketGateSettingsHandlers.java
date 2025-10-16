package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.view.View;
import android.widget.AdapterView;

public interface TicketGateSettingsHandlers {
    void navigateToTicketGateQrScanByChange(View view);
    void navigateToTicketGateQrScan(View view);
    void selectTripOne(View view);
    void selectTripTwo(View view);
    void selectTripThree(View view);
    void arrowUp(View view);
    void arrowDown(View view);
}
