package jp.mcapps.android.multi_payment_terminal.thread.printer;

public class PrinterConst {

    /* 伝票タイプ */
    public static final int SlipType_Trans = 0;                         // 取引
    public static final int SlipType_TransHistory_Waon = 1;             // WAON履歴照会
    public static final int SlipType_Aggregate = 2;                     // 集計
    public static final int SlipType_DeviceCheck = 3;                   // デバイスチェック
    public static final int SlipType_TransHistory_Okica = 4;            // OKICA残高履歴
    public static final int SlipType_DetailStatement = 5;               // 取引明細書
    public static final int SlipType_CancelTicket = 6;                  // 取消票
    public static final int SlipType_Receipt = 7;                       // 領収書
    public static final int SlipType_QRTicket = 8;                      // QR券
    public static final int SlipType_TransCash_DetailStatement = 9;     // 取引明細書（現金決済）
    public static final int SlipType_CashHistory = 10;                  // 自動釣銭機機内残高印刷
    public static final int SlipType_Prepaid = 11;                      // プリペイド取引
    //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
    public static final int SlipType_AggregateFutabaD = 12;             // 集計印字（フタバD専用）
    //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修

    /* 伝票控え */
    public static final int SlipCopy_Merchant = 1;                      // 加盟店控え
    public static final int SlipCopy_Customer = 2;                      // お客様控え

    /* 集計 */
    public static final int AggregateType_NoDetail = 0;                 // 取引・処理未了
    public static final int AggregateType_Detail = 1;                   // 取引・処理未了・明細

    /* 取引結果 */
    public static final Integer TransResult_OK = 0;                     // 成功
    public static final Integer TransResult_NG = 1;                     // 失敗
    public static final Integer TransResult_UnFinished = 2;             // 未了
    public static final Integer TransResult_Other = 9;                  // その他

    /* 取引結果詳細 */
    public static final Integer TransDetail_Normal = 0;                 // 正常
    public static final Integer TransDetail_Auth_NG = 1;                // オーソリ結果NG
    public static final Integer TransDetail_AuthVerification_NG = 2;    // オーソリ検証結果NG
    public static final Integer TransDetail_UnFinished = 3;             // 処理未了
    public static final Integer TransDetail_Communication_Failure = 4;  // 通信障害

    /* 印刷状態 */
    public static final int PrintStatus_IDLE = 0;                       // 印刷可能状態
    public static final int PrintStatus_PRINTING = 1;                   // 印刷中状態
    public static final int PrintStatus_PRINTWAITING = 2;               // 印刷待ち状態
    public static final int PrintStatus_PAPERLACKING = 3;               // 用紙切れ状態
    public static final int PrintStatus_ERROR = 4;                      // 異常状態（用紙切れ以外）
    public static final int PrintStatus_UPDATING = 5;                   // 更新中状態

    /* 双方向印刷結果詳細コード */
    public static final int DuplexPrintStatus_OK = 0;                   // 印刷正常終了
    public static final int DuplexPrintStatus_PAPERLACKING = 1;         // 用紙切れ
    public static final int DuplexPrintStatus_PRINTCHECK = 2;           // プリンタ確認
    public static final int DuplexPrintStatus_PRINTBUSY = 3;            // 印字中
    public static final int DuplexPrintStatus_DENY = 8;                 // 印字拒否
    public static final int DuplexPrintStatus_ERROR = 9;                // 印字不能
    public static final int DuplexPrintStatus_CMDSTOP = 100;            // データ送信不可
    public static final int DuplexPrintStatus_CMDERR = 101;             // データ送信中止
    public static final int DuplexPrintStatus_DATAERROR = 200;          // 受信データ異常
    public static final int DuplexPrintStatus_DISCON = 900;             // メーター通信不可
    public static final int DuplexPrintStatus_TIMEOUT = 901;            // メーター通信タイムアウト
    public static final int DuplexPrintStatus_IFBOX_PRINTERROR = -1;    // 印刷失敗、再開用
    public static final int DuplexPrintStatus_METERSTSERROR = -2;       // メーター状態異常
    public static final int DuplexPrintStatus_IFBOXERROR = -3;          // IM-A820通信エラー（WS送信前）
    public static final int DuplexPrintStatus_IFBOXERROR_TIMEOUT = -4;  // IM-A820通信エラー（WS送信後、応答待ちタイムアウト）
    //ADD-S BMT S.Oyama 2024/10/18 フタバ双方向向け改修
    public static final int DuplexPrintStatus_SDCARD_NOTFOUND = -5;     // SDカード未挿入
    //public static final int DuplexPrintStatus_METER_ERR = -6;           // メータ発のエラー(文字列エラー)
    public static final int DuplexPrintStatus_DISCOUNT_REJECT = -7;     // 割引処理拒否
    public static final int DuplexPrintStatus_MONEYRECEIPTKEY_ERR = -8;   // 領収書発行エラー
    public static final int DuplexPrintStatus_TICKETRECEIPTKEY_ERR = -9;  // チケット伝票発行エラー
    public static final int DuplexPrintStatus_OUTOFPAPER_NORESTART = -10; // 用紙切れ検出で再印刷なし
    public static final int DuplexPrintStatus_PROCESSCODE_TEISEIREQ = -11; // メータエラー時訂正キー要求
    public static final int DuplexPrintStatus_PROCESSCODE_ERRRETURN = -12; // メータエラー時異常処理コード画面戻り要求
    //ADD-E BMT S.Oyama 2024/10/18 フタバ双方向向け改修

    /* 双方向印刷タイマ値 */
    public static final int DuplexPrintWaitTimer = 20;                  // 印字完了待ちタイマ
    public static final int DuplexPrintResponseTimer = 25 * 1000;       // 820応答待ちタイマ
    public static final int DuplexPrintResponseTimerAggregate = 25 * 1000;       // 820応答待ちタイマ-日計
//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    public static final int DuplexPrintResponseTimerSec = DuplexPrintResponseTimer / 1000;       // 820応答待ちタイマ(秒換算)
    public static final int DuplexMeterSendWaitTimerFutabaD = 10;                  // メータデータ通知時待ちタイマ
    public static final int DuplexMeterSendWaitTimerShortFutabaD = 1;              // メータデータ通知時待ちタイマ(ACKが返ってこない場合の短い待ち時間)
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修

    /* オカベ双方向　印刷エラーコード */
    /* ここでは、メーターから受信するエラーコードを定義 */
    /* 印刷エラー処理時は、上記の「双方向印刷結果詳細コード」に変換する */
    public static final int DuplexOkabePrintStatus_PAPERLACKING = 4;    // 用紙切れ
    public static final int DuplexOkabePrintStatus_PRINTERROR = 5;      // プリンタ異常
}
