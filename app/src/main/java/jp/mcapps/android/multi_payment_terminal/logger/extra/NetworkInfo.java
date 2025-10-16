package jp.mcapps.android.multi_payment_terminal.logger.extra;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;

public class NetworkInfo {
    @SerializedName("network_type")
    @Expose
    public String networkType;
    @SerializedName("network_level")
    @Expose
    public Integer networkLevel;

    public NetworkInfo() {
        RadioData data = CurrentRadio.getData();
        if (data != null) {
            networkType = data.networkType;
            networkLevel = data.level;
        }
    }
}
