package jp.mcapps.android.multi_payment_terminal.thread.emv;

import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_IC;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_MAG;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_NFC;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static class KernelID {
        public static final int JCB = 5;
        public static final int Diners = 6;
        public static final int Amex = 4;
        public static final int VISA = 3;
        public static final int Mastercard = 2;
    }

    public static class BrandSign {
        public static final String JCB = "35";
        public static final String Diners = "36";
        public static final String Amex = "37";
        public static final String VISA = "40";
        public static final String Mastercard = "50";
    }

    public static class RID {
        public static final String JCB = "A000000065";
        public static final String Diners = "A000000152";
        public static final String Amex = "A000000025";
        public static final String VISA = "A000000003";
        public static final String Mastercard = "A000000004";
    }

    // NEW POS Technology Limited EMV Level2 Kernel-C SDK User Manual V1.2より抜粋
    public static class OutcomeCVM {
        public static final byte NO_CVM = (byte) 0x00;
        public static final byte OBTAIN_SIGNATURE = (byte) 0x10;
        public static final byte ONLINE_PIN = (byte) 0x20;
        public static final byte CONFIRMATION_CODE_VERIFIED = (byte) 0x30;
        public static final byte CVM_NA = (byte) 0xF0;
    }

    public enum CLState {
        None(null),
        CardHold("カードを離さないでください"),
        RemoveCard("カードを離してください"),
        OnlineRequest("オンライン処理中です"),
        SecondTouch("もう一度カードをかざしてください"),
        ReadAgain(null),
        SeePhone(null),
        ;

        private final String message;
        public String getMessage() {
            return this.message;
        }

        CLState(String message) {
            this.message = message;
        }
    }

    public enum ActivateIF {
        None(0),
        MSIC(INMODE_MAG.getVal() | INMODE_IC.getVal()),  // 磁気とIC
        CL(INMODE_NFC.getVal()),  // 非接触
        ALL(INMODE_MAG.getVal() | INMODE_IC.getVal() | INMODE_NFC.getVal()),  // すべて
        ;

        private int mode;
        public int getMode() {
            return mode;
        }

        ActivateIF(int mode) {
            this.mode = mode;
        }
    }

    /*
     * オーソリのICC関連データ
     */
    public static Map<Integer, int[]> CL_AUTH_TAGS = new HashMap<Integer, int[]>() {{
        /*
         * Mastercard
         * CAFIS 接続条件設計書 加盟店ショッピング業務・基本接続編第3.9版を参考に定義
         */
        put(KernelID.Mastercard, new int[] {
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Result
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount,Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount,Other
                0x9F33,  // Terminal Capabilities
                0x9F34,  // CVM Results
                0x9F35,  // Terminal Type
                0x9F1E,  // IFD Serial Number
                0x5A,    // Application PAN
                0x5F24,  // Application Expiration Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code – Default
                0x9F0E,  // Issuer Action Code – Denial
                0x9F0F,  // Issuer Action Code – Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // IC Card Application Version Number
                0x9F41,  // Transaction Sequence Counter
                0x9F53,  // Transaction Category Code
                0x9F6E,  // Third Party Data
                0,  // 終了タグ
        });

        /*
         * VISA
         * CAFIS 接続条件設計書 加盟店ショッピング業務・基本接続編第3.9版を参考に定義
         */
        put(KernelID.VISA, new int[] {
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Result
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount,Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount,Other
                0x9F33,  // Terminal Capabilities
                0x9F34,  // CVM Results
                0x9F1E,  // IFD Serial Number
                0x5A,    // Application PAN
                0x5F24,  // Application Expiration Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code – Default
                0x9F0E,  // Issuer Action Code – Denial
                0x9F0F,  // Issuer Action Code – Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // IC Card Application Version Number
                0x9F7C,  // Customer Exclusive Data
                0x9F6E,  // Form Factor Indicator
                0,  // 終了タグ
        });

        /*
         * JCB
         * ACQ00074 アクワイアラ接続試験テストケースV3.85有人ファイルを参考に定義
         */
        put(KernelID.JCB, new int[] {
                0x4F,    // Application ID (AID)
                0x84,    // DF Name
                0x5F34,  // Application PAN Sequence Number
                0x5A,    // Application PAN(下4桁のみ記載他はマスク)
                0x9F02,  // Amount, Authorized
                0x9F03,  // Amount, Other
                0x9F1A,  // Terminal Country Code
                0x95,    // Terminal Verification Results (TVR)
                0x5F2A,  // Transaction Currency Code
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F37,  // Unpredictable Number
                0x82,    // Application Interchange Profile (AIP)
                0x9F36,  // Application Transaction Counter (ATC)
                0x9F10,  // Issuer Application Data
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F34,  // CVM Results
                0x9F35,  // Terminal Type
                0x5F25,  // Application Effective Date
                0x5F24,  // Application Expiration Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code - Default
                0x9F0E,  // Issuer Action Code - Denial
                0x9F0F,  // Issuer Action Code - Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // Application Version Number
                0x9F33,  // Terminal Capabilities
                0x9F1E,  // Interface Device Serial Number
                0x9F41,  // Transaction Sequence Counter
                0x8E,    // CVM List
                0x9F7C,  // Partner Discretionary Data
                0x9F6E,  // Device Information
                0  // 終了タグ
        });

        /*
         * Amex
         * CAFIS 接続条件設計書 加盟店ショッピング業務・基本接続編第3.9版を参考に定義
         */
        put(KernelID.Amex, new int[] {
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Result
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount,Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount,Other
                0x9F34,  // CVM Results
                0x9F35,  // Terminal Type
                0x5A,    // Application PAN
                0x5F24,  // Application Expiration Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code – Default
                0x9F0E,  // Issuer Action Code – Denial
                0x9F0F,  // Issuer Action Code – Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // IC Card Application Version Number
                0,  // 終了タグ
        });

        /*
         * Diners
         * ACQ00074 アクワイアラ接続試験テストケースV3.85有人ファイルを参考に定義
         */
        put(KernelID.Diners, new int[]{
                0x9F02,  // Amount Authorized
                0x82,    // Application Interchange Profile (AIP)
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x9F26,  // Application Cryptogram (ARQC)
                0x9F10,  // Issuer Application Data
                0x9F33,  // Terminal Capabilities
                0x9F1A,  // Terminal Country Code
                0x95,    // Terminal Verification Results (TVR)
                0x9A,    // Transaction Data
                0x9C,    // Transaction Type
                0x5F2A,  // Transaction Currency Code
                0,       // 終了タグ
        });
    }};

    public static Map<Integer, int[]> CL_SALES_TAGS = new HashMap<Integer, int[]>() {{
        /*
         * Mastercard
         * 非接触 EMV 対応 POS ガイドライン 取引処理編(1.4版)を参考に定義
         */
        put(KernelID.Mastercard, new int[] {
                0x84,    // Dedicated File Name
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Results
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x9F33,  // Terminal Capabilities
                0x9F35,  // Terminal Type
                0x9F34,  // CVM Results
                0x9F6E,  // Third Party Data
                0,       // 終了タグ
        });

        /*
         * VISA
         * 非接触 EMV 対応 POS ガイドライン 取引処理編(1.4版)を参考に定義
         */
        put(KernelID.VISA, new int[] {
                0x84,    // Dedicated File Name
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Results
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x8A,    // Authorization Response Code
                0x9F33,  // Terminal Capabilities
                0x9F35,  // Terminal Type
                0x5A,    // Application PAN
                0x9F21,  // Transaction Time
                0x5F34,  // Application PAN Sequence Number
                0x9F1E,  // IFD Serial Number
                0xDF7F,  // Issuer Script Results
                0x9F6E,  // Form Factor Indicator (FFI)
                0,       // 終了タグ
        });

        /*
         * Amex
         * 非接触 EMV 対応 POS ガイドライン 取引処理編(1.4版)を参考に定義
         */
        put(KernelID.Amex, new int[] {
                0x84,    // Dedicated File Name
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Results
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x8A,    // Authorization Response Code
                0x9F33,  // Terminal Capabilities
                0x9F35,  // Terminal Type
                0x9F34,  // CVM Results
                0x5A,    // Application PAN
                0x9F21,  // Transaction Time
                0x5F34,  // Application PAN Sequence Number
                0x9F1E,  // IFD Serial Number
                0x5F25,  // Application Effective Date
                0xDF7F,  // Issuer Script Results
                0x5F24,  // Application Expiration Date
                0x9F09,  // Application Version Number
                0x9F41,  // Transaction Sequence Counter
                0,       // 終了タグ
        });

        /*
         * JCB
         * 非接触 EMV 対応 POS ガイドライン 取引処理編(1.4版)を参考に定義
         */
        put(KernelID.JCB, new int[] {
                0x84,    // Dedicated File Name
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Results
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x8A,    // Authorization Response Code
                0x9F33,  // Terminal Capabilities
                0x9F35,  // Terminal Type
                0x9F34,  // CVM Results
                0x5A,    // Application PAN
                0x9F21,  // Transaction Time
                0x5F34,  // Application PAN Sequence Number
                0x9F1E,  // IFD Serial Number
                0x5F25,  // Application Effective Date
                0xDF7F,  // Issuer Script Results
                0x9F6E,  // Device Information
                0,       // 終了タグ
        });

        /*
         * Diners
         * 非接触 EMV 対応 POS ガイドライン 取引処理編(1.4版)を参考に定義
         */
        put(KernelID.Diners, new int[] {
                0x84,    // Dedicated File Name
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,    // Terminal Verification Results
                0x9A,    // Transaction Date
                0x9C,    // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,    // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x9F33,  // Terminal Capabilities
                0x9F35,  // Terminal Type
                0x9F34,  // CVM Results
                0x5F34,  // Application PAN Sequence Number
                0x9F1E,  // IFD Serial Number
                0xDF7F,  // Issuer Script Results
                0x9F07,  // Application Usage Control
                0,       // 終了タグ
        });
    }};

    /*
     * 障害取消のICC関連データ
     * CAFIS データ部９－７－３ ＩＣ関連データ （アドバイス要求）
     */
    public static Map<Integer, int[]> CL_ADVICE_TAGS = new HashMap<Integer, int[]>() {{
        /*
         * Mastercard
         * CAFIS 接続条件設計書 加盟店ショッピング業務・基本接続編第3.9版を参考に定義
         */
        put(KernelID.Mastercard, new int[] {
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,  // Terminal Verification Result
                0x9A,  // Transaction Date
                0x9C,  // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,  // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x9F33,  // Terminal Capabilities
                0x9F34,  // CVM Results
                0x9F35,  // Terminal Type
                0x8A,  // Authorization Response Code
                0x9F1E,  // IFD Serial Number
                0x5F25,  // Application Effective Date
                0x5A,  // Application PAN
                0xDF7F,  // Issuer Script Result
                0x5F34,  // Application PAN Sequence Counter
                0x5F24,  // Application Effective Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code – Default
                0x9F0E,  // Issuer Action Code – Denial
                0x9F0F,  // Issuer Action Code – Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // IC Card Application Version Number
                0,  // 終了タグ
        });

        /*
         * VISA
         * CAFIS 接続条件設計書 加盟店ショッピング業務・基本接続編第3.9版を参考に定義
         */
        put(KernelID.VISA, new int[] {
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,  // Terminal Verification Result
                0x9A,  // Transaction Date
                0x9C,  // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,  // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x9F33,  // Terminal Capabilities
                0x9F34,  // CVM Results
                0x9F35,  // Terminal Type
                0x8A,  // Authorization Response Code
                0x9F1E,  // IFD Serial Number
                0x5F25,  // Application Effective Date
                0x5A,  // Application PAN
                0xDF7F,  // Issuer Script Result
                0x5F34,  // Application PAN Sequence Counter
                0x5F24,  // Application Effective Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code – Default
                0x9F0E,  // Issuer Action Code – Denial
                0x9F0F,  // Issuer Action Code – Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // IC Card Application Version Number
                0,  // 終了タグ
        });

        /*
         * Amex
         * CAFIS 接続条件設計書 加盟店ショッピング業務・基本接続編第3.9版を参考に定義
         */
        put(KernelID.Amex, new int[] {
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,  // Terminal Verification Result
                0x9A,  // Transaction Date
                0x9C,  // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,  // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x9F33,  // Terminal Capabilities
                0x9F34,  // CVM Results
                0x9F35,  // Terminal Type
                0x8A,  // Authorization Response Code
                0x9F1E,  // IFD Serial Number
                0x5F25,  // Application Effective Date
                0x5A,  // Application PAN
                0xDF7F,  // Issuer Script Result
                0x5F34,  // Application PAN Sequence Counter
                0x5F24,  // Application Effective Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code – Default
                0x9F0E,  // Issuer Action Code – Denial
                0x9F0F,  // Issuer Action Code – Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // IC Card Application Version Number
                0,  // 終了タグ
        });


        /*
         * JCB
         * ACQ00074 アクワイアラ接続試験テストケースV3.85有人ファイルを参考に定義
         */
        put(KernelID.JCB, new int[] {
                0x4F,		//	Application ID (AID)
                0x84,		//	DF Name
                0x5F34,		//	Application PAN Sequence Number
                0x5A,		//	Application PAN(下4桁のみ記載他はマスク)
                0x9F02,		//	Ammount, Authorized
                0x9F03,		//	Ammount, Other
                0x9F1A,		//	Terminal Country Code
                0x95,		//	Terminal Verification Results (TVR)
                0x5F2A,		//	Transaction Currency Code
                0x9A,		//	Transaction Date
                0x9C,		//	Transaction Type
                0x9F37,		//	Unredictable Number
                0x82,		//	Application Interchange Profile (AIP)
                0x9F36,		//	Application Transaction Counter (ATC)
                0x9F10,		//	Issuer Application Data
                0x9F26,		//	Application Cryptogram
                0x9F27,		//	Cryptogram Information Data
                0x9F34,		//	CVM Results
                0x9F35,		//	Terminal Type
                0x5F25,		//	Application Effective Date
                0x5F24,		//	Application Expiration Date
                0x9F21,		//	Transaction Time
                0x9F07,		//	Application Usage Control
                0x9F0D,		//	Issuer Action Code - Default
                0x9F0E,		//	Issuer Action Code - Denial
                0x9F0F,		//	Issuer Action Code - Online
                0x9F09,		//	Terminal Application Version Number
                0x9F08,		//	Application Version Number
                0x9F33,		//	Terminal Capabilities
                0x9F1E,		//	Interface Device Serial Number
                0x9F7C,		//	Partner Discretionary Data
                0x9F6E,		//	Device Information
                0x8A,		//	Authorization Response Code
                0x9F60,		//	Issuer Script Results
                0		//	終了タグ
        });

        /*
         * Amex
         * CAFIS 接続条件設計書 加盟店ショッピング業務・基本接続編第3.9版を参考に定義
         */
        put(KernelID.Diners, new int[] {
                0x9F26,  // Application Cryptogram
                0x9F27,  // Cryptogram Information Data
                0x9F10,  // Issuer Application Data
                0x9F37,  // Unpredictable Number
                0x9F36,  // Application Transaction Counter
                0x95,  // Terminal Verification Result
                0x9A,  // Transaction Date
                0x9C,  // Transaction Type
                0x9F02,  // Amount, Authorized
                0x5F2A,  // Transaction Currency Code
                0x82,  // Application Interchange Profile
                0x9F1A,  // Terminal Country Code
                0x9F03,  // Amount, Other
                0x9F33,  // Terminal Capabilities
                0x9F34,  // CVM Results
                0x9F35,  // Terminal Type
                0x8A,  // Authorization Response Code
                0x9F1E,  // IFD Serial Number
                0x5F25,  // Application Effective Date
                0x5A,  // Application PAN
                0xDF7F,  // Issuer Script Result
                0x5F34,  // Application PAN Sequence Counter
                0x5F24,  // Application Effective Date
                0x9F21,  // Transaction Time
                0x9F07,  // Application Usage Control
                0x9F0D,  // Issuer Action Code – Default
                0x9F0E,  // Issuer Action Code – Denial
                0x9F0F,  // Issuer Action Code – Online
                0x9F09,  // Terminal Application Version Number
                0x9F08,  // IC Card Application Version Number
                0,  // 終了タグ
        });
    }};
}
