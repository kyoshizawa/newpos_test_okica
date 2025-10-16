package jp.mcapps.android.multi_payment_terminal.thread.emv;

import com.google.common.base.Strings;
import com.newpos.emvl2.EMV_ENUM;

public class RiskManagementParameter {
    private String brandSign;  // ブランド識別
    private String aid;  // AID
    private Integer floorLimit;  // フロアリミット
    private Integer maxTargetPercentage;  // 最大目標パーセンテージ
    private Integer targetParcentage;  // 目標パーセンテージ
    private Integer threshold;  // しきい値
    private Boolean brandEnableFlg;  // ブランド有効/無効フラグ
    private String defaultDDOL;  // デフォルトDDOL
    private Integer execPriority;  // 実行優先順位
    private String terminalActionCdDenial;  // 端末アクションコード・拒否
    private String terminalActionCdDefault;  // 端末アクションコード・デフォルト
    private String terminalActionCdOnline;  // 端末アクションコード・オンライン

    private String terminalActionCdDenialRefund;  // 端末アクションコード・拒否

    private String terminalActionCdDefaultRefund;  // 端末アクションコード・デフォルト

    private String terminalActionCdOnlineRefund;  // 端末アクションコード・オンライン
    private Integer acquirerOnlinePayType;  // アクワイアラ指定オンライン要求支払種別
    private String kameitenClassCd;  // 加盟店分類コード
    private String txnTypeCd;  // 取引分類コード
    private String chargeTypeCd;  // チャージタイプコード
    private String appPersonalInfo;  // AP個別情報
    private Integer kernelId;  // カーネル番号
    private Integer acquirerContactlessId;  // アクワイアラID
    private String combinationOptions;  // 端末オプション設定
    private Integer contactlessTransactionLimit;  // 非接触トランザクションリミット
    private Integer cvmRequiredLimit;  // 非接触CVMリミット
    private String merchantNameAndLocation;  // 加盟店名
    private Integer removableTimeout;  // 電文の戻り時間
    private String terminalCountryCode;  // 端末国コード
    private String terminalInterchangeProfile;  // 端末交換プロファイル
    private Integer terminalType;  // 端末タイプ
    private String transactionCurrencyCode;  // 取引国コード
    private Integer transactionCurrencyExponent;  // 取引通貨桁数
    private String defaultMdol;  // デフォルトMDOL

    public String getBrandSign() {
        return brandSign;
    }

    public void setBrandSign(String brandSign) {
        this.brandSign = brandSign;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public Integer getFloorLimit() {
        return floorLimit;
    }

    public void setFloorLimit(Integer floorLimit) {
        this.floorLimit = floorLimit;
    }

    public Integer getMaxTargetPercentage() {
        return maxTargetPercentage;
    }

    public void setMaxTargetPercentage(Integer maxTargetPercentage) {
        this.maxTargetPercentage = maxTargetPercentage;
    }

    public Integer getTargetParcentage() {
        return targetParcentage;
    }

    public void setTargetParcentage(Integer targetParcentage) {
        this.targetParcentage = targetParcentage;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Boolean getBrandEnableFlg() {
        return brandEnableFlg;
    }

    public void setBrandEnableFlg(Boolean brandEnableFlg) {
        this.brandEnableFlg = brandEnableFlg;
    }

    public String getDefaultDDOL() {
        return defaultDDOL;
    }

    public void setDefaultDDOL(String defaultDDOL) {
        this.defaultDDOL = defaultDDOL;
    }

    public Integer getExecPriority() {
        return execPriority;
    }

    public void setExecPriority(Integer execPriority) {
        this.execPriority = execPriority;
    }

    public String getTerminalActionCdDenial() {
        return terminalActionCdDenial;
    }

    public void setTerminalActionCdDenial(String terminalActionCdDenial) {
        this.terminalActionCdDenial = terminalActionCdDenial;
    }

    public String getTerminalActionCdDefault() {
        return terminalActionCdDefault;
    }

    public void setTerminalActionCdDefault(String terminalActionCdDefault) {
        this.terminalActionCdDefault = terminalActionCdDefault;
    }

    public String getTerminalActionCdOnline() {
        return terminalActionCdOnline;
    }

    public void setTerminalActionCdOnline(String terminalActionCdOnline) {
        this.terminalActionCdOnline = terminalActionCdOnline;
    }

    public String getTerminalActionCdDenialRefund() {
        return terminalActionCdDenialRefund;
    }

    public void setTerminalActionCdDenialRefund(String terminalActionCdDenialRefund) {
        this.terminalActionCdDenialRefund = terminalActionCdDenialRefund;
    }

    public String getTerminalActionCdDefaultRefund() {
        return terminalActionCdDefaultRefund;
    }

    public void setTerminalActionCdDefaultRefund(String terminalActionCdDefaultRefund) {
        this.terminalActionCdDefaultRefund = terminalActionCdDefaultRefund;
    }

    public String getTerminalActionCdOnlineRefund() {
        return terminalActionCdOnlineRefund;
    }

    public void setTerminalActionCdOnlineRefund(String terminalActionCdOnlineRefund) {
        this.terminalActionCdOnlineRefund = terminalActionCdOnlineRefund;
    }

    public Integer getAcquirerOnlinePayType() {
        return acquirerOnlinePayType;
    }

    public void setAcquirerOnlinePayType(Integer acquirerOnlinePayType) {
        this.acquirerOnlinePayType = acquirerOnlinePayType;
    }

    public String getKameitenClassCd() {
        return kameitenClassCd;
    }

    public void setKameitenClassCd(String kameitenClassCd) {
        this.kameitenClassCd = kameitenClassCd;
    }

    public String getTxnTypeCd() {
        return txnTypeCd;
    }

    public void setTxnTypeCd(String txnTypeCd) {
        this.txnTypeCd = txnTypeCd;
    }

    public String getChargeTypeCd() {
        return chargeTypeCd;
    }

    public void setChargeTypeCd(String chargeTypeCd) {
        this.chargeTypeCd = chargeTypeCd;
    }

    public String getAppPersonalInfo() {
        return appPersonalInfo;
    }

    public void setAppPersonalInfo(String appPersonalInfo) {
        this.appPersonalInfo = appPersonalInfo;
    }

    public Integer getKernelId() {
        return kernelId;
    }

    public void setKernelId(Integer kernelId) {
        this.kernelId = kernelId;
    }

    public Integer getAcquirerContactlessId() {
        return acquirerContactlessId;
    }

    public void setAcquirerContactlessId(Integer acquirerContactlessId) {
        this.acquirerContactlessId = acquirerContactlessId;
    }

    public String getCombinationOptions() {
        return combinationOptions;
    }

    public void setCombinationOptions(String combinationOptions) {
        this.combinationOptions = combinationOptions;
    }

    public Integer getContactlessTransactionLimit() {
        return contactlessTransactionLimit;
    }

    public void setContactlessTransactionLimit(Integer contactlessTransactionLimit) {
        this.contactlessTransactionLimit = contactlessTransactionLimit;
    }

    public Integer getCvmRequiredLimit() {
        return cvmRequiredLimit;
    }

    public void setCvmRequiredLimit(Integer cvmRequiredLimit) {
        this.cvmRequiredLimit = cvmRequiredLimit;
    }

    public String getMerchantNameAndLocation() {
        return merchantNameAndLocation;
    }

    public void setMerchantNameAndLocation(String merchantNameAndLocation) {
        this.merchantNameAndLocation = merchantNameAndLocation;
    }

    public Integer getRemovableTimeout() {
        return removableTimeout;
    }

    public void setRemovableTimeout(Integer removableTimeout) {
        this.removableTimeout = removableTimeout;
    }

    public String getTerminalCountryCode() {
        return terminalCountryCode;
    }

    public void setTerminalCountryCode(String terminalCountryCode) {
        this.terminalCountryCode = terminalCountryCode;
    }

    public String getTerminalInterchangeProfile() {
        return terminalInterchangeProfile;
    }

    public void setTerminalInterchangeProfile(String terminalInterchangeProfile) {
        this.terminalInterchangeProfile = terminalInterchangeProfile;
    }

    public Integer getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(Integer terminalType) {
        this.terminalType = terminalType;
    }

    public String getTransactionCurrencyCode() {
        return transactionCurrencyCode;
    }

    public void setTransactionCurrencyCode(String transactionCurrencyCode) {
        this.transactionCurrencyCode = transactionCurrencyCode;
    }

    public Integer getTransactionCurrencyExponent() {
        return transactionCurrencyExponent;
    }

    public void setTransactionCurrencyExponent(Integer transactionCurrencyExponent) {
        this.transactionCurrencyExponent = transactionCurrencyExponent;
    }

    public String getDefaultMdol() {
        return defaultMdol;
    }

    public void setDefaultMdol(String defaultMdol) {
        this.defaultMdol = defaultMdol;
    }

    public String toTLV(int transType) {
        final StringBuilder tlv = new StringBuilder();

        // 共通タグ
        tlv.append(makeTLV("9F01", "06", acquirerContactlessId));
        tlv.append(makeTLV("9F35", "01", terminalType));
        tlv.append(makeTLV("9F1A", "02", terminalCountryCode));
        tlv.append(makeTLV("5F2A", "02", transactionCurrencyCode));
        tlv.append(makeTLV("5F36", "01", transactionCurrencyExponent));

        // ブランド固有タグ

        if (brandSign.equals(Constants.BrandSign.Amex)) {
            tlv.append(makeTLV("DF808023", "01", targetParcentage));
            tlv.append(makeTLV("DF808024", "01", maxTargetPercentage));
            tlv.append(makeTLV("DF808025", "06", threshold));
            tlv.append(makeTLV("DF808026", "03", defaultDDOL));
        }
        else if (brandSign.equals(Constants.BrandSign.JCB)) {
            tlv.append(makeTLV("DF808600", "02", combinationOptions));
            tlv.append(makeTLV("DF808601", "03", terminalInterchangeProfile));
            tlv.append(makeTLV("DF808602", "01", removableTimeout));
            tlv.append(makeVarLengthTLV("9F5C", defaultMdol));
        }

        // リミットセットとTACはMastercard以外同じタグ
        if (brandSign.equals(Constants.BrandSign.Mastercard)) {
            tlv.append(makeTLV("DF8123", "06", floorLimit));
            tlv.append(makeTLV("DF8126", "06", cvmRequiredLimit));
            tlv.append(makeTLV("DF8124", "06", contactlessTransactionLimit)); // NoOn-deviceCVM
            tlv.append(makeTLV("DF8125", "06", contactlessTransactionLimit)); // On-deviceCVM

            if (transType == EMV_ENUM.EMV_TRANS_PURCHASE) {
                tlv.append(makeTLV("DF8120", "05", terminalActionCdDefault));
                tlv.append(makeTLV("DF8121", "05", terminalActionCdDenial));
                tlv.append(makeTLV("DF8122", "05", terminalActionCdOnline));
            } else if(transType == EMV_ENUM.EMV_TRANS_REFUND) {
                tlv.append(makeTLV("DF8120", "05", terminalActionCdDefaultRefund));
                tlv.append(makeTLV("DF8121", "05", terminalActionCdDenialRefund));
                tlv.append(makeTLV("DF8122", "05", terminalActionCdOnlineRefund));
            }
        } else {
            tlv.append(makeTLV("DF80802B", "06", floorLimit));
            tlv.append(makeTLV("DF80802C", "06", cvmRequiredLimit));
            tlv.append(makeTLV("DF80802A", "06", contactlessTransactionLimit));
            if (transType == EMV_ENUM.EMV_TRANS_PURCHASE) {
                tlv.append(makeTLV("DF808020", "05", terminalActionCdDenial));
                tlv.append(makeTLV("DF808021", "05", terminalActionCdDefault));
                tlv.append(makeTLV("DF808022", "05", terminalActionCdOnline));
            } else if(transType == EMV_ENUM.EMV_TRANS_REFUND) {
                tlv.append(makeTLV("DF808020", "05", terminalActionCdDenialRefund));
                tlv.append(makeTLV("DF808021", "05", terminalActionCdDefaultRefund));
                tlv.append(makeTLV("DF808022", "05", terminalActionCdOnlineRefund));
            }
        }

        return tlv.toString();
    }

    private String makeTLV(String t, String l, String v) {
        if (Strings.isNullOrEmpty(v)) return "";

        return String.format("%s%s%s", t, l, v);
    }

    private String makeTLV(String t, String l, Integer v) {
        if (v == null) return "";

        int strLength = Integer.parseInt(l, 16) * 2;

        final StringBuilder value = new StringBuilder(String.valueOf(v));
        int padNum = strLength - value.length();

        while (padNum-- > 0) {
            value.insert(0, "0");
        }

        return String.format("%s%s%s", t, l, value);
    }

    private String makeVarLengthTLV(String t, String v) {
        if (Strings.isNullOrEmpty(v)) return "";

        long length = v.length() / 2;

        return String.format("%s%02X%s", t, v.length()/2, v);
    }
}