package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import java.io.Serializable;

public class TicketGateQrScanResults implements Serializable {

    Boolean qrScanResult;                   // QR確認結果

    String errorCode;                       // エラーコード
    String errorMessage;                    // エラーメッセージ
    String errorMessageEnglish;             // エラーメッセージ（英語）

    Integer adultNumber;                    // 大人(人数)
    Integer childNumber;                    // 小人(人数)
    Integer babyNumber;                     // 乳幼児(人数)
    Integer adultDisabilityNumber;          // 障がい者 大人(人数)
    Integer childDisabilityNumber;          // 障がい者 小人(人数)
    Integer caregiverNumber;                // 介助者(人数)
    Integer totalPeoples;                   // 合計人数

}
