package jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WebSocketPayload {
    @SerializedName("type")
    @Expose
    public String type;

    @SerializedName("data")
    @Expose
    public Object data;

    @SerializedName("cmd")
    @Expose
    public Object cmd;
}
