package jp.mcapps.android.multi_payment_terminal.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class DiscountInfo {
    private String transactionDateTime;
    private Integer discountType;
    private String expiredDateFrom;
    private String expiredDateTo;

    public DiscountInfo(String transactionDateTime, Integer discountType, String expiredDateFrom, String expiredDateTo) {
        this.transactionDateTime = transactionDateTime;
        this.discountType = discountType;
        this.expiredDateFrom = expiredDateFrom;
        this.expiredDateTo = expiredDateTo;
    }

    public String getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(String transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public Integer getDiscountType() {
        return discountType;
    }

    public void setDiscountType(Integer discountType) {
        this.discountType = discountType;
    }

    public String getExpiredDateFrom() {
        return expiredDateFrom;
    }

    public void setExpiredDateFrom(String expiredDateFrom) {
        this.expiredDateFrom = expiredDateFrom;
    }

    public String getExpiredDateTo() {
        return expiredDateTo;
    }

    public void setExpiredDateTo(String expiredDateTo) {
        this.expiredDateTo = expiredDateTo;
    }

    @NonNull
    @Override
    public String toString() {
        return (new Gson()).toJson(this);
    }
}
