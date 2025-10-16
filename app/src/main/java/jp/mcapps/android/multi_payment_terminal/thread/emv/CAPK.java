package jp.mcapps.android.multi_payment_terminal.thread.emv;

public class CAPK {
    private String blandSign;
    private Integer caPublicKeyVersion;
    private String caPublicKeyIndex;
    private String caHashAlgorithmIndicator;
    private String caPublicKeyAlgorithmIndicator;
    private String caPublicKeyModulus;
    private String caPublicKeyExponent;
    private String caPublicKeyCheckSum;

    public String getBlandSign() {
        return blandSign;
    }

    public void setBlandSign(String blandSign) {
        this.blandSign = blandSign;
    }

    public Integer getCaPublicKeyVersion() {
        return caPublicKeyVersion;
    }

    public void setCaPublicKeyVersion(Integer caPublicKeyVersion) {
        this.caPublicKeyVersion = caPublicKeyVersion;
    }

    public String getCaPublicKeyIndex() {
        return caPublicKeyIndex;
    }

    public void setCaPublicKeyIndex(String caPublicKeyIndex) {
        this.caPublicKeyIndex = caPublicKeyIndex;
    }

    public String getCaHashAlgorithmIndicator() {
        return caHashAlgorithmIndicator;
    }

    public void setCaHashAlgorithmIndicator(String caHashAlgorithmIndicator) {
        this.caHashAlgorithmIndicator = caHashAlgorithmIndicator;
    }

    public String getCaPublicKeyAlgorithmIndicator() {
        return caPublicKeyAlgorithmIndicator;
    }

    public void setCaPublicKeyAlgorithmIndicator(String caPublicKeyAlgorithmIndicator) {
        this.caPublicKeyAlgorithmIndicator = caPublicKeyAlgorithmIndicator;
    }

    public String getCaPublicKeyModulus() {
        return caPublicKeyModulus;
    }

    public void setCaPublicKeyModulus(String caPublicKeyModulus) {
        this.caPublicKeyModulus = caPublicKeyModulus;
    }

    public String getCaPublicKeyExponent() {
        return caPublicKeyExponent;
    }

    public void setCaPublicKeyExponent(String caPublicKeyExponent) {
        this.caPublicKeyExponent = caPublicKeyExponent;
    }

    public String getCaPublicKeyCheckSum() {
        return caPublicKeyCheckSum;
    }

    public void setCaPublicKeyCheckSum(String caPublicKeyCheckSum) {
        this.caPublicKeyCheckSum = caPublicKeyCheckSum;
    }
}
