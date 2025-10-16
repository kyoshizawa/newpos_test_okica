package jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Meter {
    public static class Response {
        @SerializedName("status")
        @Expose
        public String status;

        @SerializedName("fare")
        @Expose
        public Integer fare;

        @SerializedName("fare_split")
        @Expose
        public Integer fare_split;

        @SerializedName("eigyo_num")
        @Expose
        public Integer eigyo_num;

        @SerializedName("sampled_at")
        @Expose
        public Long sampledAt;

        // 以降、meter/v3 で使用
        @SerializedName("fare_discount")
        @Expose
        public Integer fare_discount;

        @SerializedName("fare_etc")
        @Expose
        public Integer fare_etc;

        @SerializedName("fare_other")
        @Expose
        public Integer fare_other;

        @SerializedName("car_id")
        @Expose
        public Integer car_id;

        // 20231031 t.wada タリフの判定は、820から来る料金通知 status_serial 追加
        @SerializedName("status_serial")
        @Expose
        public String status_serial;

        // 20231124 t.wada 820から来る支払済料金 追加
        @SerializedName("fare_PAID")
        @Expose
        public Integer fare_paid;
    }
    public static class ResponseYazaki {
        @SerializedName("status")
        @Expose
        public String status;

        @SerializedName("continue")
        @Expose
        public Integer cont;

        @SerializedName("err_cd")
        @Expose
        public Integer err_cd;

        @SerializedName("sampled_at")
        @Expose
        public Long sampledAt;
    }

//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    public static class ResponseFutabaD {
        @SerializedName("meter_sub_cmd")
        @Expose
        public Integer meter_sub_cmd;

        @SerializedName("status")
        @Expose
        public String status;

        @SerializedName("trans_brand")
        @Expose
        public String trans_brand;

        @SerializedName("trans_type")
        @Expose
        public Integer trans_type;

        @SerializedName("trans_amount")
        @Expose
        public Integer trans_amount;

        @SerializedName("zangaku_mode")
        @Expose
        public Integer zangaku_mode;

        @SerializedName("id_code")
        @Expose
        public String id_code;

        @SerializedName("if_ver")
        @Expose
        public Integer if_ver;

        @SerializedName("meter_ver")
        @Expose
        public String meter_ver;

        @SerializedName("car_id")
        @Expose
        public Integer car_id;

        @SerializedName("proc_code")
        @Expose
        public String proc_code;

        @SerializedName("line_1")
        @Expose
        public String line_1;

        @SerializedName("line_2")
        @Expose
        public String line_2;

        @SerializedName("line_3")
        @Expose
        public String line_3;

        //ADD-S BMT S.Oyama 2025/03/27 フタバ双方向向け改修
        @SerializedName("line_41")
        @Expose
        public String line_41;
        @SerializedName("line_42")
        @Expose
        public String line_42;
        @SerializedName("line_43")
        @Expose
        public String line_43;
        //ADD-E BMT S.Oyama 2025/03/27 フタバ双方向向け改修

        @SerializedName("sound_no")
        @Expose
        public Integer sound_no;

        @SerializedName("zandaka_flg")
        @Expose
        public Integer zandaka_flg;

        public int     separate_flg;
    }

    public static class ResponseFutabaDPrintEnd {
        @SerializedName("status")
        @Expose
        public String status;

        @SerializedName("continue")
        @Expose
        public Integer cont;

        @SerializedName("err_cd")
        @Expose
        public Integer err_cd;

        @SerializedName("sampled_at")
        @Expose
        public Long sampledAt;

        @SerializedName("err_cd_str")
        @Expose
        public String err_cd_str;
    }
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修

    public static class ResponseStatus {
        @SerializedName("info")
        @Expose
        public String info;

        @SerializedName("version")
        @Expose
        public int version;
    }

//ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
    public static class ResponseStatusFutabaD {
        @SerializedName("info")
        @Expose
        public String info;

        @SerializedName("version")
        @Expose
        public Integer version;

        @SerializedName("meter_status")
        @Expose
        public Integer meter_status;

        @SerializedName("meter_fare")
        @Expose
        public Integer meter_fare;

        @SerializedName("tatekae")
        @Expose
        public Integer tatekae;

        @SerializedName("eigyo_num")
        @Expose
        public Integer eigyo_num;
    }
//ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

    public static class AliveData {
        @SerializedName("name")
        @Expose
        public String name;
        @SerializedName("model")
        @Expose
        public String model;
        @SerializedName("version")
        @Expose
        public String version;
        @SerializedName("base_version")
        @Expose
        public String baseVersion;
    }
}
