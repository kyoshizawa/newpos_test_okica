package jp.mcapps.android.multi_payment_terminal.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FirmWareInfo {
    @Expose
    public Integer id;

    @Expose
    @SerializedName("created_at")
    public String createdAt;

    @Expose
    @SerializedName("updated_at")
    public String updatedAt;

    @Expose
    @SerializedName("product_name")
    public String productName;

    @Expose
    @SerializedName("model_name")
    public String modelName;

    @Expose
    @SerializedName("version_name")
    public String versionName;

    @Expose
    @SerializedName("version_code")
    public Integer versionCode;

    @Expose
    public Boolean visibility;

    @Expose
    @SerializedName("bin_path")
    public String binPath;

    @Expose
    @SerializedName("bin_key")
    public String binKey;

    @Expose
    @SerializedName("tags")
    public List<String> tags;

    @Expose
    public Boolean isForceUpdate = false;

    public boolean downloadSuccessful;
}
