package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CAKey {
    public static class Request {
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public CAPublicKeys[] caKeys;
    }

    public static class CAPublicKeys {
        @Expose
        public String blandSign;

        @Expose
        public Integer caPublicKeyVersion;

        @Expose
        public String caPublicKeyIndex;

        @Expose
        @SerializedName("caHashAlgolithmIndicator")
        public String caHashAlgorithmIndicator;

        @Expose
        @SerializedName("caPublicKeyAlgolithmIndicator")
        public String caPublicKeyAlgorithmIndicator;

        @Expose
        public String caPublicKeyModulus;

        @Expose
        public String caPublicKeyExponent;

        @Expose
        public String caPublicKeyCheckSum;
    }
}
