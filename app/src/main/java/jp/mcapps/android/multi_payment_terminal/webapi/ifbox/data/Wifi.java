package jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data;

import java.util.HashMap;
import java.util.Map;

public class Wifi {
    public static class Request {
        public String ssid;
        public String passphrase;

        public Map<String, String> toMap() {
            return new HashMap<String, String>() {{
                put("ssid", ssid);
                put("passphrase", passphrase);
            }};
        }
    }
}
