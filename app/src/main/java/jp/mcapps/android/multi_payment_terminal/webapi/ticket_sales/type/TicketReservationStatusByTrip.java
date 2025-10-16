package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketReservationStatusByTrip {

    @Expose
    public TicketTripInfo trip_info;

    @Expose
    public String transport_equipment_id;

    @Expose
    public String transport_equipment_name;

    @Expose
    public TicketTransportEquipmentNameI18n[] transport_equipment_name_i18n;

    @Expose
    public TicketTransportReservationSlot[] transport_reservation_slots;
}
