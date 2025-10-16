package jp.mcapps.android.multi_payment_terminal.model;

public class DiscountMenuInfo {
    private String discountName;
    private Integer discountType;

    public DiscountMenuInfo(String discountName, Integer discountType) {
        this.discountName = discountName;
        this.discountType = discountType;
    }

    public String getDiscountName() { return discountName; }

    public void setDiscountName(String discountName) {
        this.discountName = discountName;
    }

    public Integer getDiscountType() { return discountType; }

    public void setDiscountType(Integer discountType) {
        this.discountType = discountType;
    }
}
