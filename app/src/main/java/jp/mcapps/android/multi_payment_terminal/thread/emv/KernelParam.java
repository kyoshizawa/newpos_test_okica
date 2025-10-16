package jp.mcapps.android.multi_payment_terminal.thread.emv;

import android.os.Build;

import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.KernelID;
import timber.log.Timber;

public class KernelParam {
    private static final Param[] params = new Param[5];

    public static Param[] getParams() {
        return params;
    }

    public static class Param {
        public int kernelId;
        public byte[] data;
    }

    static {
        /*
         * 9F1E Interface Device (IFD) Serial Numberは自動で設定される
         * 端末シリアル番号から9210を除いたものをAsciiコード化した値
         */
        final StringBuilder common = new StringBuilder();

        /*
         * 9F33 Terminal Capabilities
         * Mastercardの場合別タグで値が構成される
         *
         * Byte 1: Card Data Input Capability Value 60(0110 0000)
         *     b8: 0 = Manual key entry not supported
         *     b7: 1 = Magnetic stripe supported
         *     b6: 1 = IC with contacts supported
         *     b5 - b1 RFU
         *
         * Byte 2: CVM Capability Value B8(1011 1000)
         *     b8: 1 = Plaintext PIN for ICC verification supported
         *     b7: 0 = Enciphered PIN for online verification not supported
         *     b6: 1 = Signature (paper) supported
         *     b5: 1 = Enciphered PIN for offline verification
         *     b4: 1 = No CVM Required
         *     b3 - b1 RFU
         *
         * Byte 3: Security Capability Value C8(1100 1000)
         *     b8: 1 = SDA supported
         *     b7: 1 = DDA supported
         *     b6: 0 = Card capture supported
         *     b5: 0 = RFU
         *     b4: 1 = CDA supported
         *     b3 - b1 RFU
         */
        common.append(String.format("%s%s%s", "9F33", "03", "60B8C8"));

        /*
         * 9F40 Additional Terminal Capabilities
         * Byte1 Transaction Type Capability Value 60(0110 0000)
         *     b8: 0 = Cash disabled
         *     b7: 1 = Goods enabled
         *     b6: 1 = Services disabled
         *     b5: 0 = Cashback disabled
         *     b4: 0 = Inquiry disabled
         *     b3: 0 = Transfer disabled
         *     b2: 0 = Payment disabled
         *     b1: 0 = Administrative disabled
         *
         * Byte2 Transaction Type Capability 00(0000 1000)
         *     b8: 0 = Cash Deposit disabled
         *     b7 - b1 RFU
         *
         * Byte3 Terminal Data Input Capability Value 00(0000 1000)
         *     b8: 0 = Numeric keys disabled
         *     b7: 0 = Alphabetic and special characters keys disabled
         *     b6: 0 = Command keys disabled
         *     b5: 0 = Function keys disabled
         *     b4 - b1 RFU
         *
         * Byte4 Terminal Data Output Capability Value A0(1010 1000)
         *     b8: 1 = Print, attendant ※ Book4より有人端末でプリンタが一つだけの場合はこちらのビットを立てる
         *     b7: 0 = Print, cardholder
         *     b6: 1 = Display, attendant ※ Book4より有人端末でディスプレイが一つだけの場合はこちらのビットを立てる
         *     b5: 0 = No display cardholder
         *     b4 - b3 RFU
         *     b2: 0 = don't use Code table 10
         *     b1: 0 = don't use Code table 9
         *
         *
         * Byte5 Terminal Data Output Capability Value 01(0000 1000)
         *     b8: 0 = don't use Code table 8
         *     b7: 0 = don't use Code table 7
         *     b6: 0 = don't use Code table 6
         *     b5: 0 = don't use Code table 5
         *     b4: 0 = don't use Code table 4
         *     b3: 0 = don't use Code table 3
         *     b2: 0 = don't use Code table 2
         *     b1: 1 = use Code table 1
         */
        common.append(String.format("%s%s%s", "9F40", "05", "600000A001"));

        /*
         * 9F15 Merchant Category Code
         *
         * https://neapay.com/post/mcc-codes-merchant-category-codes_93.html
         * Byte1 Value 5999(Miscellaneous and Specialty Retail Stores)
         */
        common.append(String.format("%s%s%s", "9F15", "02", "5999"));

        final StringBuilder JCB = new StringBuilder(common);

        /*
         * DF808061 Kernel Configuration ※ 内容がCombination Optionsとほとんど同じ
         * Byte1 Value D8(1101 1000)
         *     b8: 1 = Status check supported
         *     b7: 1 = Offline data authentication supported
         *     b6: 0 = Exception file check not supported
         *     b5: 1 = EMV supported
         *     b4: 1 = Legacy mode supported
         *     b3: 0 = Issuer update not supported
         */
        JCB.append(String.format("%s%s%s", "DF808061", "01", "D8"));

        /*
         * 9F52 Terminal Compatibility Indicator
         * Byte1 Value 02(0000 0010)
         *     b8 - b3 RFU
         *     b2: 1 = EMV Mode Supported (fixed to 1b)
         *     b1: 0 = fixed to 0b
         */
        JCB.append(String.format("%s%s%s", "9F52", "01", "02"));

        /*
         * 9F09 Application Version Number
         * JCB提供のリスク管理パラメータより設定
         */
        JCB.append(String.format("%s%s%s", "9F09", "02", "0200"));

        // JCB
        params[0] = new Param() {{
            kernelId = KernelID.JCB;
            data = ISOUtil.hex2byte(JCB.toString());
        }};

        final StringBuilder Amex = new StringBuilder(common);

        /*
         * 9F6D Contactless Reader Capabilities
         * Byte1 Value C0(1100 0000)
         *     00(0000 0000)    Deprecated
         *     08(0100 0000)    Not Available for Use
         *     40(0100 0000)    Contactless: Mag-Stripe – CVM Not Required (C-4 Version ≥ 2.2)
         *     48(0100 1000)    Contactless: Mag-Stripe – CVM Required (C-4 Version ≥ 2.2)
         *     80(1000 0000)    Deprecated – Contactless: EMV and Mag-Stripe (C-4 Version 2.1)
         *     88(1000 1000)    Not Available for Use
         *     C0(1100 0000)    Contactless: EMV and Mag-Stripe - CVM Not Required (C-4 Version ≥ 2.2)
         *     C8(1100 1000)    Contactless: EMV and Mag-Stripe - CVM Required (C-4 Version ≥ 2.2)
        */
        Amex.append(String.format("%s%s%s", "9F6D", "01", "C0"));

        /*
         * Tag 9F6E Enhanced Contactless Reader Capabilities
         *
         * Byte1 Value 98(1001 1000)
         *     b8: 1 = Contact mode supported
         *     b7: 0 = Contactless Mag-Stripe Mode supported
         *     b6: 0 = Contactless EMV full online mode not supported (full online mode is a legacy feature and is no longer supported)
         *     b5: 1 = Contactless EMV partial online mode supported
         *     b4: 1 = Contactless Mobile Supported
         *     b3: 0 = Try Another Interface before a decline.
         *     b2 - b1 RFU
         *
         * Byte2 Value A0(1011 0000)
         *     b8: 1 = Mobile CVM supported
         *     b7: 0 = Online PIN Not supported
         *     b6: 1 = Signature supported
         *     b5: 0 = Plaintext Offline PIN supported
         *     b4 - b1 RFU
         *
         * Byte3 Value 00(0000 0000)
         *     b8: 0 = Reader is not offline only
         *     b7: 0 = CVM not Required (セットしてもGPOの前に書き換えられる)
         *     b6 - b1 RFU
         *
         * Byte4 Value 03(0000 0011)
         *     b8: 0 = Terminal exempt from No CVM checks
         *     b7: 0 = Delayed Authorisation Terminal
         *     b6 - b4 RFU
         *     b3 - b1 011 = Kernel Version2.7
         *         001 = 2.2 - 2.3
         *         010 = 2.4 - 2.6
         *         011 = 2.7
         *         1xx = RFU
         */
        Amex.append(String.format("%s%s%s", "9F6E", "04", "98A00003"));

        /*
         * 9F09 Application Version Number
         */
        Amex.append(String.format("%s%s%s", "9F09", "02", "0001"));

        params[1] = new Param() {{
            kernelId = KernelID.Amex;
            data = ISOUtil.hex2byte(Amex.toString());
        }};

        // VISA Todo パラメータの値が適切か調べる
        final StringBuilder VISA = new StringBuilder(common);

        /*
         * DF808061 Visa Kernel Configure
         *
         * 確信はないけどTrack1ってワードが出ている当たりMSDは磁気ストライプモードの話っぽい
         * TTQから磁気ストライプモードはサポート外なので関係ないb6とb5はおそらく排他的な選択なのでb5にしておく
         * Byte 1: Value 10(0001 0000)
         *     b8: 0 = CVM17 not fallback TO MSD Legacy
         *     b7: 0 = Disable MSD CVM17
         *     b6: 0 = MSD Formatting Track2 Data
         *     b5: 1 = MSD Construction Track1 Data
         *     b4: 0 = Don't run DRL
         *     b3: 0 = Manual cash not check
         *     b2: 0 = Cashback not check
         *     b1: 0 = Exception file not check
         *     b6 - b1 RFU
         *
         * Byte 2: Value 00(0000 0000)
         *     b8: 0 = Key revocation(鍵の失効リストチェックはしない。そもそもTTQからオンライン要求時にオフラインデータ認証しないので関係ない)
         *     b7: 0 = Application Expiration don't check
         *     b6 - b1 RFU
         *
         * Byte 3: Value F8(0011 0010)
         *     b8: 1 = Status Check supported
         *     b7: 1 = Zero Amount Check supported
         *     b6: 1 = Contactless transaction limit Check supported
         *     b5: 1 = Contactless floor limit check supported
         *     b4: 1 = CVM Require Limit Check supported
         */
        VISA.append(String.format("%s%s%s", "DF808061", "03", "1000F8"));  // Visa Kernel Configure

        /*
         * 9F66 Terminal Transaction Qualifiers (TTQ)
         *
         * Byte 1: Value 32(0011 0010)
         *     b8: 0 = Mag-stripe mode not supported
         *     b7: RFU
         *     b6: 1 = EMV mode supported
         *     b5: 1 = EMV contact chip supported
         *     b4: 0 not Offline-only reader
         *     b3: 0 Online PIN not supported
         *     b2: 1 Signature supported
         *     b1: 0 Offline Data Authentication for Online Authorizations not supported
         *
         * Byte 2: Value 20(0010 0000)
         *     b8: 0 = Online cryptogram not required
         *     b7: 0 = CVM not required
         *     b6: 1 = (Contact Chip) Offline PIN supported
         *     b5 - b1 RFU
         *
         * Byte 3: Value 40(0100 0000)
         *     b8: 0 = Issuer Update Processing supported
         *     b7: 1 = Consumer Device CVM(CDCVM) supported
         *     b6 - b1 RFU
         *
         * Byte 4: Value 00(0000 0000)
         *     b8 - b1 RFU
         */
        VISA.append(String.format("%s%s%s", "9F66", "04", "32204000"));  // Terminal Transaction Qualifiers (TTQ)

        params[2] = new Param() {{
            kernelId = KernelID.VISA;
            data = ISOUtil.hex2byte(VISA.toString());
        }};

        // Mastercard
        final StringBuilder Mastercard = new StringBuilder(common);


        /*
         * DF8117 Card Data Input Capability
         * Byte1 Value 60(0110 0000)
         *     b8: 0 = Manual key entry not supported
         *     b7: 1 = Magnetic stripe not supported
         *     b6: 1 = IC with contacts supported
         *     b5 - b1 RFU
         */
        Mastercard.append(String.format("%s%s%s", "DF8117", "01", "60"));        // Card DataInput Capability

        /*
         * DF8118 CVM Capability – CVM Required
         * Byte1 Value F8(1111 1000)
         *     b8: 1 = Plaintext PIN for ICC verification
         *     b7: 0 = Enciphered PIN for online verification
         *     b6: 1 = Signature (paper)
         *     b5: 1 = Enciphered PIN for offline verification
         *     b4: 0 = No CVM required
         *     b3 - b1 RFU
         */
        Mastercard.append(String.format("%s%s%s", "DF8118", "01", "B0"));        // CVMCapability-CVMRequired

        /*
         * DF8119 CVM Capability – No CVM Required
         * Byte1 Value F8(1111 1000)
         *     b8: 0 = Plaintext PIN for ICC verification
         *     b7: 0 = Enciphered PIN for online verification
         *     b6: 0 = Signature (paper)
         *     b5: 0 = Enciphered PIN for offline verification
         *     b4: 1 = No CVM required
         *     b3 - b1 RFU
         */
        Mastercard.append(String.format("%s%s%s", "DF8119", "01", "08"));        // CVMCapability-NoCVMRequired

        /*
         * DF8119 CVM Capability – No CVM Required
         * 磁気ストライプモードをサポートしないので不要。コメントアウト
         * Byte1 Value F8(1111 1000)
         *     b8: 1 = Plaintext PIN for ICC verification
         *     b7: 1 = Enciphered PIN for online verification
         *     b6: 1 = Signature (paper)
         *     b5: 1 = Enciphered PIN for offline verification
         *     b4: 1 = No CVM required
         *     b3 - b1 RFU
         */
        // Mastercard.append(String.format("%s%s%s", "DF811A", "03", "9F6A04"));    // DefaultUDOL

        /*
         * DF810B DS Summary Status
         * IDSをサポートしないので不要。コメントアウト
         *
         * Byte1 Value F8(1111 1000)
         *     b8: x = Successful Read
         *     b7: x = Successful Write
         *     b6 - b1 RFU
         */
        // Mastercard.append(String.format("%s%s%s", "DF810B", "01", "20"));        // DSSummaryStatus

        /*
         * DF810C Kernel ID
         */
        Mastercard.append(String.format("%s%s%s", "DF810C", "01", "02"));        // KernelID

        /*
         * 9F6D Mag-stripe Application Version Number (Reader)
         * 磁気ストライプモードをサポートしないので不要。コメントアウト
         */
        // Mastercard.append(String.format("%s%s%s", "9F6D", "02", "0001"));        // Mag-stripeApplicationVersionNumber(Reader)

        /*
         * DF811E Mag-stripe CVM Capability – CVM Required
         * 磁気ストライプモードをサポートしないので不要。コメントアウト
         *
         * Byte1 Value F8(1111 1000)
         *     b8 - b5
         *         0000: NO CVM
         *         0001: OBTAIN SIGNATURE
         *         0010: ONLINE PIN
         *         1111: N/A
         *         Other values: RFU
         *     b4 - b1 RFU
         */
        // Mastercard.append(String.format("%s%s%s", "DF811E", "01", "10"));        // Mag-stripeCVMCapability-CVMRequired

        /*
         * DF812C Mag-stripe CVM Capability – No CVM Required
         * 磁気ストライプモードをサポートしないので不要。コメントアウト
         *
         * Byte1 Value F8(1111 1000)
         *     0000: NO CVM
         *     0001: OBTAIN SIGNATURE
         *     0010: ONLINE PIN
         *     1111: N/A
         *     Other values: RFU
         */
        // Mastercard.append(String.format("%s%s%s", "DF812C", "01", "00"));        // Mag-stripeCVMCapability-NoCVMRequired

        /*
         * DF811C Max Lifetime of Torn Transaction Log Record
         * IDSをサポートしないので不要。コメントアウト
         *
         * Maximum time, in seconds, that a record can remain in the Torn Transaction Log.
         */
        // Mastercard.append(String.format("%s%s%s", "DF811C", "02", "0000"));      // MaxLifetimeofTornTransactionLogRecord

        /*
         * DF811D Max Number of Torn Transaction Log Records
         * IDSをサポートしないので不要。コメントアウト
         *
         * Indicates the maximum number of records that can be stored in the Torn Transaction Log.
         */
        // Mastercard.append(String.format("%s%s%s", "DF811D", "01", "00"));        // MaxNumberofTornTransactionLogRecords

        /*
         * 9F7E Mobile Support Indicator
         *
         * Byte1 Value F8(1111 1000)
         *     b8 - b3 RFU
         *     b2 0 = OD-CVM Not Required
         *     b1 1 = Mobile supported
         */
        Mastercard.append(String.format("%s%s%s", "9F7E", "01", "01"));          // MobileSupportIndicator

        /*
         * DF811F Mobile Support Indicator
         *
         * Byte1 Value C8(1100 1000)
         *     b8 1 = SDA supported
         *     b7 1 = DDA supported
         *     b6 0 = Card capture not supported
         *     b5 RFU
         *     b4 1 = CDA supported
         *     b3 - b1 RFU
         */
        Mastercard.append(String.format("%s%s%s", "DF811F", "01", "C8"));        // SecurityCapability

        /*
         * DF811B Kernel Configuration
         *
         * Byte1 Value (1011 1000)
         *     b8 1 = Mag-stripe mode contactless transactions not supported  TSE設定よりサポート対象外
         *     b7 0 = EMV mode contactless transactions supported
         *     b6 1 = On device cardholder verification supported
         *     b5 1 = Relay resistance protocol supported
         *     b4 0 = Reserved for Payment system
         *     b3 0 = Read all records even when no CDA (CDAする)
         *     b2 - b1 RFU
         */
        Mastercard.append(String.format("%s%s%s", "DF811B", "01", "B0"));        // Kernel Configuration

        /*
         * Integrated Data Storage(IDS)をサポートしない場合オール0
         * PDOLに含まれることがあるためタグの定義がないと取引に失敗する
         */
        Mastercard.append(String.format("%s%s%s", "9F5C", "08", "0000000000000000")); // DS Requested Operator ID

        /*
         * 9F1D Terminal Risk Management Data
         * 2Byte目以外はTSE設定値より設定
         *
         * Byte1 Value 2C(0010 1100)
         *     b8 0 = Restart not supported
         *     b7 0 = Enciphered PIN verified online (Contactless) not supported
         *     b6 1 = Signature (paper) (Contactless) supported
         *     b5 0 = Enciphered PIN verification performed by ICC (Contactless) not supported
         *     b4 1 = No CVM required (Contactless) supported
         *     b3 1 = CDCVM (Contactless) supported
         *     b2 0 = Plaintext PIN verification performed by ICC (Contactless) not supported
         *     b1 0 = Present and Hold not supported
         *
         * Byte2 Value FF(1111 1111)
         *     b8 1 = CVM Limit exceeded
         *     b7 1 = Enciphered PIN verified online (Contactless)
         *     b6 1 = Signature (paper) (Contact)
         *     b5 1 = Enciphered PIN verification performed by ICC (Contact)
         *     b4 1 = No CVM required (Contact)
         *     b3 1 = CDCVM (Contact)
         *     b2 1 = Plaintext PIN verification performed by ICC (Contact)
         *     b1 RFU
         *
         * Byte3 Value 80(1000 0000)
         *     b8 1 = Mag-stripe mode contactless transactions not supported
         *     b7 0 = EMV mode contactless transactions supported
         *     b6 0 = CDCVM without CDA not supported
         *     b5 - b1 RFU
         *
         * Byte4 Value 00(0000 0000)
         *     b8 0 = CDCVM bypass not requested
         *     b7 0 = SCA not exempt
         *     b6 - b1 RFU
         *
         * Byte5 - Byte8 RFU
         */
        Mastercard.append(String.format("%s%s%s", "9F1D", "08", "2CFF800000000000"));

        params[3] = new Param() {{
            kernelId = KernelID.Mastercard;
            data = ISOUtil.hex2byte(Mastercard.toString());
        }};

        // Diners
        final StringBuilder Diners = new StringBuilder(common);

        /*
         * 9F66 Terminal Transaction Qualifiers (TTQ)
         *
         * Byte 1: Value 32(0011 0010)
         *     b8: 0 = Mag-stripe mode not supported
         *     b7: RFU
         *     b6: 1 = EMV mode supported
         *     b5: 1 = EMV contact chip supported
         *     b4: 0 not Offline-only reader
         *     b3: 0 Online PIN not supported
         *     b2: 1 Signature supported
         *     b1: 0 Offline Data Authentication for Online Authorizations not supported
         *
         * Byte 2: Value 20(0010 0000)
         *     b8: 0 = Online cryptogram not required
         *     b7: 0 = CVM not required
         *     b6: 1 = (Contact Chip) Offline PIN supported
         *     b5 - b1 RFU
         *
         * Byte 3: Value 40(0100 0000)
         *     b8: 0 = Issuer Update Processing supported
         *     b7: 1 = Consumer Device CVM(CDCVM) supported
         *     b6 - b1 RFU
         *
         * Byte 4: Value 00(0000 0000)
         *     b8 - b1 RFU
         */
        Diners.append(String.format("%s%s%s", "9F66", "04", "32204000"));  // Terminal Transaction Qualifiers (TTQ)

        /*
         * DF808028 Status check
         *
         * Byte1 Value 01(0000 0001)
         *     b8 - b2 RFU
         *     b1 1 = Status check enabled
         */
        Diners.append(String.format("%s%s%s", "DF808028", "01", "01"));  // Status Check

        /*
         * DF808029 Zero check
         *
         * Byte1 Value 01(0000 0001)
         *     b8 - b2 RFU
         *     b1 1 = Zero check enabled
         */
        Diners.append(String.format("%s%s%s", "DF808029", "01", "01"));  // Zero check

        params[4] = new Param() {{
            kernelId = KernelID.Diners;
            data = ISOUtil.hex2byte(Diners.toString());
        }};
    }
}
