package jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Chip {
    public static class Response {
        @SerializedName("chip_id")
        @Expose
        public String ChipId;

        @SerializedName("cpu_core")
        @Expose
        public Integer cpuCore;

        @SerializedName("cpu_freq")
        @Expose
        public String cpuFreq;

        @SerializedName("flash_size")
        @Expose
        public Integer flashSize;

        @SerializedName("flash_freq")
        @Expose
        public String flashFreq;

        @SerializedName("psram_size")
        @Expose
        public Integer psramSize;
    }
}
