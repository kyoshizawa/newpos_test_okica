package jp.mcapps.android.multi_payment_terminal.data;

public class QRLayouts {
    public static final String KEY = "layout";

    public static final int PAYMENT = assign();
    public static final int DEVICE_CHECK = assign();
    public static final int IFBOX_SETUP = assign();
    public static final int NETWORK_SETUP = assign();
    public static final int POS = assign();

    private static int bit = 0;
    private static int assign() {
        bit = bit != 0 ? bit << 1 : 1;
        return bit;
    }
}
