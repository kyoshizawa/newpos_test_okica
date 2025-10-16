package jp.mcapps.android.multi_payment_terminal.data.sam;

public class SyntaxErrorReason {
    public static final int SYNTAX_ERROR = 0x1000;
    public static final int READ_ERROR = 0x1001;
    public static final int WRITE_ERROR = 0x1002;
    public static final int WRONG_RWSAM_ID = 0x1003;
    public static final int KEY_NOT_FOUND = 0x1005;
    public static final int KEY_EXISTS = 0x1006;
    public static final int NO_SPACE_LEFT = 0x100C;
    public static final int ERR_CARD_AUTH_FAILED = 0x100F;
    public static final int RWSAM_AUTH_FAILED = 0x1010;
    public static final int PACKAGE_PARITY_WRONG = 0x1011;
    public static final int SELFTEST_FAILED = 0x1012;
    public static final int WRONG_PACKAGE_TYPE = 0x1015;
    public static final int WRONG_KEY_TYPE = 0x1016;
    public static final int INVALID_PACKAGE = 0x1017;
    public static final int FW_UPDATE_ERROR = 0x1018;
    public static final int HW_ERROR = 0x1019;
    public static final int FL_S_MAC_ERROR = 0x101A;
    public static final int TOP_UP_AMOUNT_TOTAL_EXISTS = 0x101B;
    public static final int TOP_UP_AMOUNT_TOTAL_NOT_FOUND = 0x101C;
    public static final int TOP_UP_AMOUNT_ERROR = 0x101D;
    public static final int NUMBER_OF_KEY_USE_TOTAL_EXISTS = 0x101E;
    public static final int NUMBER_OF_KEY_USE_TOTAL_NOT_FOUND = 0x101F;
    public static final int NUMBER_OF_KEY_USE_ERROR = 0x1020;
    public static final int BLOCK_LIST_ERROR = 0x1021;
    public static final int PURSE_DATA_IS_NOT_BEING_READ = 0x1022;
    public static final int FP_SERVICE_CERTIFICATION_ERROR = 0x2000;
    public static final int FP_SERVICE_NOT_FOUND = 0x2001;
    public static final int FP_SERVICE_PASSCODE_ERROR = 0x2002;
    public static final int CARD_ID_INFO_ERROR = 0x2003;
    public static final int NO_EMPTY_POCKET = 0x2004;
    public static final int FP_SERVICE_NUMBER_ERROR = 0x2005;
    public static final int DECRYPT_POCKET_INFO_FAILED = 0x2006;
    public static final int INVALID_MODE = 0xFFFE;
}
