package jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr;

import java.util.ArrayList;
import java.util.List;

public class TLV {

    public static List<Part> splitString(String code) {
        List<Part> parts = new ArrayList<>();

        do {
            String id = code.substring(0, 2); // タグ
            String len = code.substring(2, 4); // 長さ
            int length = Integer.parseInt(len, 10);
            String value = code.substring(4, 4 + length); // 値
            code = code.substring(4 + length);
            Part part = new Part(id, length, value);
            parts.add(part);
        } while (!code.isEmpty());

        return parts;
    }

    public static class Part {
        public final String tag;
        public final int length;
        public final String value;

        public Part(String tag, int length, String value) {
            this.tag = tag;
            this.length = length;
            this.value = value;
        }
    }
}
