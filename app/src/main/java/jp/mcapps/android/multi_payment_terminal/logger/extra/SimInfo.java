package jp.mcapps.android.multi_payment_terminal.logger.extra;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SimInfo {
    @Expose
    public String country;
    @SerializedName("operator_name")
    @Expose
    public String operatorName;
    @SerializedName("icc_id")
    @Expose
    public String iccId;
}
