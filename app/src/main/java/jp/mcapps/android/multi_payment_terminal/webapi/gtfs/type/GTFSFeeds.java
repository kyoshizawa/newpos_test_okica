package jp.mcapps.android.multi_payment_terminal.webapi.gtfs.type;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketEmbarkNameI18n;

public class GTFSFeeds {

    @Expose
    public String id;

    @Expose
    public String service_instance_id;

    @Expose
    public String feed_env;

    @Expose
    public String feed_version;

    @Expose
    public String feed_start_date;

    @Expose
    public String feed_end_date;

    @Expose
    public String status;

    @Expose
    public Boolean is_current_version;

    @Expose
    public String file_url;

    @Expose
    public String module_url;

    @Expose
    public String feed_name;

    @Expose
    public String feed_desc;

    @Expose
    public String feed_starting_at;

    @Expose
    public String feed_ending_at;

    @Expose
    public String created_at;

    @Expose
    public String updated_at;
}
