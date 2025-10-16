package jp.mcapps.android.multi_payment_terminal.util;

import java.security.SecureRandom;
import java.util.Random;

import jp.mcapps.android.multi_payment_terminal.AppPreference;

public class McUtils {
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null) return null;

        final byte[] bytes = new byte[hexString.length()/2];


        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(
                    hexString.substring(i*2, (i+1)*2), 16);
        }

        return bytes;
    }

    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) return null;

        final StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }

    public static String generateRandomString(int length) {
        final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        final Random rnd = new Random();
        final StringBuilder sb = new StringBuilder("");

        for (int i = 0; i < length; i++) {
            int charIdx = rnd.nextInt(chars.length());
            sb.append(chars.substring(charIdx, charIdx + 1));
        }

        return sb.toString();
    }

    public static byte[] generateRandomBytes(int length) {
        final SecureRandom sRmd = new SecureRandom();
        final byte[] rmdBytes = new byte[length];
        sRmd.nextBytes(rmdBytes);

        return rmdBytes;
    }

    // 利用可能な最大金額を超えているかチェックする(最小は見ない)
    public static Boolean isCheckMaxAmount(Integer amount) {
        // デフォルトは6桁まで利用可能
        int maxAmount = 999999;
        if (AppPreference.getMaxAmountType() == AppPreference.MaxAmountType.LARGE) {
            // 7桁まで利用可能
            maxAmount = 9999999;
        }

        // 利用可能な最大を超えてる場合はNG（最大しか見ないので最小の判定はしない）
        return amount <= maxAmount;
    }
}
