package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketTransportReservationSlot {
    @Expose
    public String transport_reservation_slot_id;
    @Expose
    public String transport_reservation_slot_name;
    @Expose
    public TicketTransportReservationSlotNameI18n[] transport_reservation_slot_name_i18n;
    @Expose
    public Integer amount;
    @Expose
    public Integer remaining_amount;
}
