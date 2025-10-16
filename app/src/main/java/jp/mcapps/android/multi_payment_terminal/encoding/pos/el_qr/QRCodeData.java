package jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr;

import java.util.Date;

public class QRCodeData {
    String taxItemNumber;               // 税目・料金番号
    Date filingDueDate;                 // 納期限日
    Date paymentDueDate;                // 支払期限日
    int paymentAmount;                  // 支払金額

    public String getTaxItemNumber() {
        return taxItemNumber;
    }

    public Date getFilingDueDate() {
        return filingDueDate;
    }

    public Date getPaymentDueDate() {
        return paymentDueDate;
    }

    public int getPaymentAmount() {
        return paymentAmount;
    }
}
