package jp.mcapps.android.multi_payment_terminal.thread.emv;

/**
 * Created by vincent on 2017/12/28.
 */

public class ParamConfig {

    //content(TVL:tag+value+length)
    /**
     * Below the directory named assets,the file named aid.properties is the set of aid.
     * This file's format must be DataIndex(AID)=content(TVL).If you want to add another
     * AID in this file ,you should add the DataIndex(AID) in this field as well.Or the
     * program can't find the message you add.
     * Note:about the TVL ,you can refer to the common EMV tag used in AID information.
     * */
    public static final String[] aids ={"A0000000031010","A0000000032010",
    "A0000000033010","A0000000041010","A0000000043060","A0000000651010",
    "A000000333010101","A000000333010102","A000000333010103",
    "A000000333010106","A0000001523010","A00000000310100"};



    //RID IDX ExpirationDate Exponent Modulus
    /**
     * Below the directory assets, the file ,capk.properties is set of capk ,
     * This file's format must be DataIndex(RID_IDX)=RID_IDX_ExpirationDate_Exponent_Modulus.
     * If you add capk in this file ,you are supported to add the RID_IDX in this field as well.
     * Or the program can't find the capk you add.
     * */
    public static final String[] capkRid={
            "A000000003_95", "A000000003_92", "A000000003_94",
            "A000000065_0F", "A000000065_11", "A000000065_13",
            "A000000025_C8", "A000000025_C9", "A000000025_CA",
            "A000000152_5A", "A000000152_5B", "A000000152_5C", "A000000152_5D",
            "A000000004_FA", "A000000004_F1", "A000000004_EF", "A000000004_FE",
            "A000000004_F8", "A000000004_F3",
            "A000000003_08", "A000000003_09",
            "A000000065_10", "A000000065_12", "A000000065_14",
            "A000000025_0F", "A000000025_10",
            "A000000152_03", "A000000152_04", "A000000152_05",
            "A000000004_04", "A000000004_05", "A000000004_06"
    };

    public static final String[] emvConfigCore = {
            "CoreParam"
    };

    public static final String[] emvConfigBrand = {
            "JCB", "AMEX", "DINERS", "VISA", "MASTER"
    };

    // EMV設定情報の"CoreParam"
    public static final String k_TERMINAL_COUNTRY_CODE = "TerminalCountryCode";
    public static final String k_TRANSACTION_CURRENCY_CODE = "TransactionCurrencyCode";
    public static final String k_REFER_CURRENCY_CODE = "ReferCurrencyCode";
    public static final String k_TRANSACTION_CURRENCY_EXPONENT = "TransactionCurrencyExponent";
    public static final String k_REFER_CURRENCY_EXPONENT = "ReferCurrencyExponent";
    public static final String k_REFER_CURRENCY_COEFFICIENT = "ReferCurrencyCoefficient";
    public static final String k_TRANSACTION_TYPE = "TransactionType";
    public static final String k_TERMINAL_AID_INFO = "TerminalAidInfo";
    public static final String k_TERMINAL_MCK_CONFIGURE = "TerminalMckConfigure";
    public static final String k_TERMINAL_TYPE = "Terminal Type";
    public static final String k_TERMINAL_CAPABILITIES = "Terminal Capabilities";
    public static final String k_TERMINAL_CAPABILITIES_PINLESS = "Terminal Capabilities Pinless";
    public static final String k_ADDITIONAL_TERMINAL_CAPABILITIES = "Additional Terminal Capabilities";
    public static final String k_SUPPORT_CARDINITIATED_VOICEREFERRALS = "Support CardInitiated VoiceReferrals";
    public static final String k_SUPPORT_FORCED_ACCEPTANCE_CAPABILITY = "Support Forced Acceptance Capability";
    public static final String k_SUPPORT_FORCED_ONLINE_CAPABILITY = "Support Forced Online Capability";
    public static final String k_POS_ENTRY_MODE = "Pos Entry Mode";
    public static final String k_SUPPORT_EXCEPTION_FILE = "Support Exception File";
    public static final String k_SUPPORT_GET_PIN_TRY_COUNTER = "Support Get Pin Try Counter";
    public static final String k_SUPPORT_PSE_SELECTION_METHOD = "Support PSE Selection Method";
    public static final String k_SUPPORT_CARDHOLDER_CONFIRMATION = "Support Cardholder Confirmation";
    public static final String k_SUPPORT_FLOOR_LIMIT_CHECKING = "Support Floor Limit Checking";
    public static final String k_SUPPORT_ADVICES = "Support Advices";
    public static final String k_SUPPORT_BATCH_DATA_CAPTURE = "Support Batch Data Capture";
    public static final String k_SUPPORT_BYPASS_PIN_ENTRY = "Support ByPass Pin Entry";
    public static final String k_SUPPORT_DEFAULT_DDOL = "Support Default DDOL";
    public static final String k_SUPPORT_DEFAULT_TDOL = "Support Default TDOL";
    public static final String k_SUPPORT_MULTI_LANGUAGE = "Support Multi Language";

    // ブランド別のEMV設定情報
    public static final String k_AID = "AID";
    public static final String k_TERMINAL_FLOOR_LIMIT = "Terminal Floor Limit";
    public static final String k_APPLICATION_VERSION = "Application Version";
    public static final String k_SUPPORT_PARTIAL_AID_SELECT = "Support Partial AID Select";
    public static final String k_TAC_DEFAULT = "TAC Default";
    public static final String k_TAC_ONLINE = "TAC Online";
    public static final String k_TAC_DENIAL = "TAC Denial";
    public static final String k_THRESHOLD_VALUE = "Threshold Value";
    public static final String k_MAXIMUM_TARGET_PERCENTAGE = "Maximum Target Percentage";
    public static final String k_TARGET_PERCENTAGE = "Target Percentage";
    public static final String k_DEFAULT_DDOL = "Default DDOL";
    public static final String k_DEFAULT_TDOL = "Default TDOL";

    public class status{
        public static final int SUCCESS = 0;
        public static final int LOAD_AID_FAIL= 1;
        public static final int LOAD_CAPK_FAIL = 2;
        public static final int OTHER_ERROR=3;
    }

    public static final int TagsOnlineAuth_Default[] = {
            0       // 終了フラグ
    };

    /**
     * ICC関連データ
     * （JCBオンラインオーソリ）
     **/
    public static final int TagsOnlineAuth_JCB[] = {
        0x9F26, // AC (Application Cryptogram)
        0x9F27, // CID
        0x9F10, // IAD (Issuer Application Data)
        0x9F37, // Unpredicatable Number
        0x9F36, // ATC (Application Transaction Counter)
        0x95,   // TVR
        0x9A,   // Transaction Date
        0x9C,   // Transaction Type
        0x9F02, // Amount Authorised
        0x5F2A, // Transaction Currency Code
        0x82,   // AIP (Application Interchange Profile)
        0x9F1A, // Terminal Country Code
        0x9F03, // Amount Other
        //0x9F33, // Terminal Capabilities
        0x9F34, // CVM Result
        //0x9F35, // Terminal Type
        //0x9F1E, // IFD Serial Number
        //0x5F25, // Application Effective Date
        0x5A,   // Application Primary Account Number (PAN)
        0x5F24, // Application Expiration Date
        0x9F21, // Transaction Time
        0x9F07, // Application Usage Control
        0x9F0D, // Issuer Action Code – Default
        0x9F0E, // Issuer Action Code – Denial
        0x9F0F, // Issuer Action Code – Online
        0x9F09, // Application Version Number
        0x9F08, // Application Version Number
        //0x8E,   // Cardholder Verification Method (CVM) List
        //0x9F41, // Transaction Sequence Counter
        //0x9F53, // Transaction Classification Code
        //0x84,   // Dedicated File Name
        //0x5F34, // PAN Sequence Number
        0       // 終了フラグ
    };

    /**
     * ICC関連データ
     * （AMEXオンラインオーソリ）
     **/
    public static final int TagsOnlineAuth_AMEX[] = {
            0x9F26, // AC (Application Cryptogram)
            0x9F27, // CID
            0x9F10, // IAD (Issuer Application Data)
            0x9F37, // Unpredicatable Number
            0x9F36, // ATC (Application Transaction Counter)
            0x95,   // TVR
            0x9A,   // Transaction Date
            0x9C,   // Transaction Type
            0x9F02, // Amount Authorised
            0x5F2A, // Transaction Currency Code
            0x82,   // AIP (Application Interchange Profile)
            0x9F1A, // Terminal Country Code
            0x9F03, // Amount Other
            //0x9F33, // Terminal Capabilities
            0x9F34, // CVM Result
            //0x9F35, // Terminal Type
            //0x9F1E, // IFD Serial Number
            //0x5F25, // Application Effective Date
            0x5A,   // Application Primary Account Number (PAN)
            0x5F24, // Application Expiration Date
            0x9F21, // Transaction Time
            0x9F07, // Application Usage Control
            0x9F0D, // Issuer Action Code – Default
            0x9F0E, // Issuer Action Code – Denial
            0x9F0F, // Issuer Action Code – Online
            0x9F09, // Application Version Number
            0x9F08, // Application Version Number
            //0x8E,   // Cardholder Verification Method (CVM) List
            0x9F41, // Transaction Sequence Counter
            //0x9F53, // Transaction Classification Code
            //0x84,   // Dedicated File Name
            0x5F34, // PAN Sequence Number
            0       // 終了フラグ
    };

    /**
     * ICC関連データ
     * （DINERSオンラインオーソリ）
     **/
    public static final int TagsOnlineAuth_DINERS[] = {
            0x9F26, // AC (Application Cryptogram)
            //0x9F27, // CID
            0x9F10, // IAD (Issuer Application Data)
            0x9F37, // Unpredicatable Number
            0x9F36, // ATC (Application Transaction Counter)
            0x95,   // TVR
            0x9A,   // Transaction Date
            0x9C,   // Transaction Type
            0x9F02, // Amount Authorised
            0x5F2A, // Transaction Currency Code
            0x82,   // AIP (Application Interchange Profile)
            0x9F1A, // Terminal Country Code
            //0x9F03, // Amount Other
            0x9F33, // Terminal Capabilities
            //0x9F34, // CVM Result
            //0x9F35, // Terminal Type
            //0x9F1E, // IFD Serial Number
            //0x5F25, // Application Effective Date
            //0x5A,   // Application Primary Account Number (PAN)
            //0x5F24, // Application Expiration Date
            //0x9F21, // Transaction Time
            //0x9F07, // Application Usage Control
            //0x9F0D, // Issuer Action Code – Default
            //0x9F0E, // Issuer Action Code – Denial
            //0x9F0F, // Issuer Action Code – Online
            //0x9F09, // Application Version Number
            //0x9F08, // Application Version Number
            //0x8E,   // Cardholder Verification Method (CVM) List
            //0x9F41, // Transaction Sequence Counter
            //0x9F53, // Transaction Classification Code
            //0x84,   // Dedicated File Name
            //0x5F34, // PAN Sequence Number
            0       // 終了フラグ
    };

    /**
     * ICC関連データ
     * （VISAオンラインオーソリ）
     **/
    public static final int TagsOnlineAuth_VISA[] = {
            0x9F26, // AC (Application Cryptogram)
            0x9F27, // CID
            0x9F10, // IAD (Issuer Application Data)
            0x9F37, // Unpredicatable Number
            0x9F36, // ATC (Application Transaction Counter)
            0x95,   // TVR
            0x9A,   // Transaction Date
            0x9C,   // Transaction Type
            0x9F02, // Amount Authorised
            0x5F2A, // Transaction Currency Code
            0x82,   // AIP (Application Interchange Profile)
            0x9F1A, // Terminal Country Code
            0x9F03, // Amount Other
            0x9F33, // Terminal Capabilities
            0x9F34, // CVM Result
            //0x9F35, // Terminal Type
            0x9F1E, // IFD Serial Number
            //0x5F25, // Application Effective Date
            0x5A,   // Application Primary Account Number (PAN)
            0x5F24, // Application Expiration Date
            0x9F21, // Transaction Time
            0x9F07, // Application Usage Control
            0x9F0D, // Issuer Action Code – Default
            0x9F0E, // Issuer Action Code – Denial
            0x9F0F, // Issuer Action Code – Online
            0x9F09, // Application Version Number
            0x9F08, // Application Version Number
            //0x8E,   // Cardholder Verification Method (CVM) List
            //0x9F41, // Transaction Sequence Counter
            //0x9F53, // Transaction Classification Code
            //0x84,   // Dedicated File Name
            //0x5F34, // PAN Sequence Number
            0       // 終了フラグ
    };

    /**
     * ICC関連データ
     * （MASTERオンラインオーソリ）
     **/
    public static final int TagsOnlineAuth_MASTER[] = {
            0x9F26, // AC (Application Cryptogram)
            0x9F27, // CID
            0x9F10, // IAD (Issuer Application Data)
            0x9F37, // Unpredicatable Number
            0x9F36, // ATC (Application Transaction Counter)
            0x95,   // TVR
            0x9A,   // Transaction Date
            0x9C,   // Transaction Type
            0x9F02, // Amount Authorised
            0x5F2A, // Transaction Currency Code
            0x82,   // AIP (Application Interchange Profile)
            0x9F1A, // Terminal Country Code
            0x9F03, // Amount Other
            0x9F33, // Terminal Capabilities
            0x9F34, // CVM Result
            0x9F35, // Terminal Type
            0x9F1E, // IFD Serial Number
            //0x5F25, // Application Effective Date
            0x5A,   // Application Primary Account Number (PAN)
            0x5F24, // Application Expiration Date
            0x9F21, // Transaction Time
            0x9F07, // Application Usage Control
            0x9F0D, // Issuer Action Code – Default
            0x9F0E, // Issuer Action Code – Denial
            0x9F0F, // Issuer Action Code – Online
            0x9F09, // Application Version Number
            0x9F08, // Application Version Number
            //0x8E,   // Cardholder Verification Method (CVM) List
            0x9F41, // Transaction Sequence Counter
            0x9F53, // Transaction Classification Code
            0x84,   // Dedicated File Name
            //0x5F34, // PAN Sequence Number
            0       // 終了フラグ
    };

    /**
     * ICC関連データ
     * （オンラインオーソリ応答検証）
     **/
    public static final int TagsOnlineAuthVerification[] = {
            0x9F26, // AC (Application Cryptogram)
            0x9F27, // CID
            0x9F10, // IAD (Issuer Application Data)
            0x9F37, // Unpredicatable Number
            0x9F36, // ATC (Application Transaction Counter)
            0x95,   // TVR
            0x9A,   // Transaction Date
            0x9C,   // Transaction Type
            0x9F02, // Amount Authorised
            0x5F2A, // Transaction Currency Code
            0x82,   // AIP (Application InterchangeProfile)
            0x9F1A, // Terminal Country Code
            0x9F03, // Amount Other
            0x9F33, // Terminal Capabilities
            0x9F34, // CVM Result
            0x9F35, // Terminal Type
            0x8A,   // Authorisation Response Code
            0x9F1E, // IFD Serial Number
            0x5F25, // Application Effective Date
            0x5A,   // Application Primary Account Number (PAN)
            0xDF7F, // Issuer Script Results
            0x5F34, // PAN Sequence Number
            0x5F24, // Application Expiration Date
            0x9F21, // Transaction Time
            0x9F07, // Application Usage Control
            0x9F0D, // Issuer Action Code – Default
            0x9F0E, // Issuer Action Code – Denial
            0x9F0F, // Issuer Action Code – Online
            0x9F09, // Application Version Number
            0x9F08, // Application Version Number
            0x84,   // Dedicated File Name
            0       // 終了フラグ
    };
}
