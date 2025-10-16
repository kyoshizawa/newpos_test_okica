package jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr;

import java.nio.charset.StandardCharsets;

public class CRCGenerator {
    // CRC-16-CCITT
    // https://www.soumu.go.jp/main_content/000807095.pdf

    final String code;

    public CRCGenerator(String code) {
        this.code = code;
    }

    public int generate() {
        return computeCRC(code);
    }

    private static int computeCRC(String code) {

        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001 (x16+x12+x5+1)

        byte[] bytes = code.getBytes(StandardCharsets.UTF_8); // UTF-8

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;

        return crc;
    }
}
