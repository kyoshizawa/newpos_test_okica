package jp.mcapps.android.multi_payment_terminal.logger;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import jp.mcapps.android.multi_payment_terminal.logger.extra.AppInfo;
import jp.mcapps.android.multi_payment_terminal.logger.extra.DeviceInfo;
import jp.mcapps.android.multi_payment_terminal.logger.extra.NetworkInfo;
import jp.mcapps.android.multi_payment_terminal.logger.extra.SimInfo;
import jp.mcapps.android.multi_payment_terminal.logger.extra.SourceInfo;

public class EventLog {
    @SerializedName("@timestamp")
    @Expose
    public String timestamp;
    @Expose
    public String level;
    @Expose
    public String tag;
    @Expose
    public String message;
    @Expose
    public String location;
    @Expose
    public AppInfo app;
    @Expose
    public DeviceInfo device;
    @Expose
    public SimInfo sim;
    @Expose
    public SourceInfo source;
    @Expose
    public NetworkInfo network;

    public EventLog(String timestamp, Level level, String tag, String message, String location, DeviceInfo device, SimInfo sim, SourceInfo source) {
        this.timestamp = timestamp;
        this.level = level.name();
        this.tag = tag;
        this.message = message;
        this.location = location;
        app = new AppInfo();
        this.device = device;
        this.sim = sim;
        this.source = source;
        network = new NetworkInfo();
    }

    public EventLog(String timestamp, Level level, String tag, String message, String location, SourceInfo source) {
        this.timestamp = timestamp;
        this.level = level.name();
        this.tag = tag;
        this.message = message;
        this.location = location;
        app = new AppInfo();
        this.source = source;
        network = new NetworkInfo();
    }

    enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL,
    }
}
