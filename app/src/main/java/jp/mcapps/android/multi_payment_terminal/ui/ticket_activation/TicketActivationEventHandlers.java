package jp.mcapps.android.multi_payment_terminal.ui.ticket_activation;

import android.view.View;

public interface TicketActivationEventHandlers {
    void onCloseClick(View view , TicketActivationViewModel ticketActivationViewModel);
    void onCancelClick(View view , TicketActivationViewModel ticketActivationViewModel);
}
