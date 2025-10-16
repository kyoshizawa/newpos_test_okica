package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.view.View;

public interface TicketSearchHandlers {
    void navigateToTicketClass(View view, String textName);
    void navigateToTicketEmbark(View view);
    void navigateToTicketDisembark(View view);
    void minusAdult(View view);
    void plusAdult(View view);
    void minusChild(View view);
    void plusChild(View view);
    void minusBaby(View view);
    void plusBaby(View view);
    void minusAdultDisability(View view);
    void plusAdultDisability(View view);
    void minusChildDisability(View view);
    void plusChildDisability(View view);
    void minusCaregiver(View view);
    void plusCaregiver(View view);
    void ticketSearch(View view);
}
