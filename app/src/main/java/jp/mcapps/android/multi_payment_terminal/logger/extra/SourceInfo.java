package jp.mcapps.android.multi_payment_terminal.logger.extra;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SourceInfo {
    @SerializedName("product_name")
    @Expose
    public String productName;
    @SerializedName("user_id")
    @Expose
    public String userId;
    @SerializedName("user_name")
    @Expose
    public String userName;
    @SerializedName("device_id")
    @Expose
    public String deviceId;
    @SerializedName("car_id")
    @Expose
    public String carId;
    @SerializedName("driver_id")
    @Expose
    public String driverId;
    @Expose
    public String serial;
    @SerializedName("organization_id")
    @Expose
    public String organizationId;
    @SerializedName("org_id")
    @Expose
    public String orgId;
}
