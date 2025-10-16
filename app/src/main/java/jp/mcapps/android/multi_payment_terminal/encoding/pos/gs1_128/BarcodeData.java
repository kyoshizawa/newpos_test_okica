package jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128;

import java.util.Date;

public class BarcodeData {
    String invoiceCompanyCode;          // 請求書発行企業コード
    String freeField;                   // 自由使用欄
    int reissueCount;                   // 再発行区分 (再発行回数)
    Date dueDate;                       // 支払期限日
    int stampFlag;                      // 印紙フラグ
    int paymentAmount;                  // 支払金額

    public String getInvoiceCompanyCode() {
        return invoiceCompanyCode;
    }

    public String getFreeField() {
        return freeField;
    }

    public int getReissueCount() {
        return reissueCount;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public int getStampFlag() {
        return stampFlag;
    }

    public int getPaymentAmount() {
        return paymentAmount;
    }
}
