package jp.mcapps.android.multi_payment_terminal.thread.printer;


import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.amazonaws.services.pinpoint.model.DeleteSegmentRequest;
import com.epson.epos2.Epos2CallbackCode;
import com.epson.epsonio.usb.AoaUsb;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.pos.device.printer.PrintCanvas;
import com.pos.device.printer.PrintTask;
import com.pos.device.printer.Printer;
import com.pos.device.printer.PrinterCallback;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeCodes;
import jp.mcapps.android.multi_payment_terminal.data.QRPayTypeNameMap;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardBrand;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptProductDetail;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptSubtotalDetail;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptDetail;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.auto_daily_report.AutoDailyReportFuelFragment;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.BaseEMoneyOkicaViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.util.BitmapSaver;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.DynamicTicketItem;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuCashChangerViewModel;
import timber.log.Timber;

public class PrinterProc {
    private static PrinterProc _instance = null;
    private Printer _printer;
    private PrintTask _printTask;
    private PrintCanvas _printCanvas;
    private Bitmap _printBitmap;
    private Paint _paint;
    private Resources _printDataRes;
    private SlipData _slipData;
    private ReceiptData _receiptData;
    private List<SlipData> _slipDataList;
    private List<SlipData> _slipDataList_Filter;
    private AggregateData _aggregateData;
    private ServiceFunctionData _serviceFunctionData;
    private TicketReceiptData _ticketReceiptData;
    private DynamicTicketItem _dynamicTicketItem;
//ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
    //private IFBoxManager.SendMeterData_FutabaD _sendMeterData_FutabaD;
//ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修

    private final int PaintSize_Normal = 0;
    private final int PaintSize_Medium = 1;
    private final int PaintSize_Big = 2;

    private final int SlipKind_Receipt = 0b00000001;
    private final int SlipKind_Customer = 0b00000010;
    private final int SlipKind_Merchant = 0b00000100;
    private final int SlipKind_CardCompany = 0b00001000;

    private static Integer isTransResult;
    private static String isMaskCardId;
    private static Integer isTransType;
    private static boolean isRePrinter;

    private int isSlipDataList_Size;
    private int isSlipDataId;
    private int isFontHalfSize;
    private int isAggregateOrder;

    private int isTransFinished_TransCnt;
    private long isTransFinished_TotalAmount;
    private int isTransUnfinished_TransCnt;
    private long isTransUnfinished_TotalAmount;

    private int isAggregateType;
    private int isPrinterSts;
    private int isQRTicketPrintSts;

    private enum QRTicketPrintSts {
        UNKNOWN,
        INFO,
        QRCODE,
        CATEGORY
    }

    private static boolean isCreditAnnounceSignature;
    private boolean isTransFinished_TotalAmount_NA_flg;

    private String Log_ErrTitle;
    private String Log_BrandName;
    private String Log_SlipName;
    private String Log_CopyTypeName;
    private String Log_Amounts;
    private String Log_ServicePos;
    private String Log_InvoiceNo;
    private String Log_InvoiceTax;
    private String Log_transDate;
    private String Log_QRTicket;

    private boolean isPT750_Print;

    private boolean isRecovery = false;

//    private static IFBoxManager _ifBoxManager;
//
//    public void setIFBoxManager(IFBoxManager ifBoxManager) {
//        _ifBoxManager = ifBoxManager;
//    }
//
//    //ADD-S BMT S.Oyama 2024/10/11 フタバ双方向向け改修
//    public IFBoxManager getIFBoxManager() {
//        return _ifBoxManager;
//    }

    private String _DuplexComm_BlandName = "";              //双方向時，通信実施中のブランド名を保持

    public String getDuplexComm_BlandName() {
        return _DuplexComm_BlandName;
    }

    private int _DuplexComm_SlipIDBackup = 0;               //双方向時，通信実施中のスリップIDを保持

    public int getDuplexComm_SlipIDBackup() {
        return _DuplexComm_SlipIDBackup;
    }
    //ADD-E BMT S.Oyama 2024/10/11 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/10/17 フタバ双方向向け改修
    //private DeviceClient.ResultWAON _resultWAONBackup = null;   //双方向時，通信実施中のWAON結果を保持
    //ADD-E BMT S.Oyama 2024/10/17 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/12/02 フタバ双方向向け改修
    private static Disposable meterDataV4ErrorDisposable = null;
    private static Disposable meterDataV4InfoDisposable = null;
    //ADD-E BMT S.Oyama 2024/12/02 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/04/24 フタバ双方向向け改修
    private String _DuplexPrint_BlandName = "";              //双方向時，印字中のブランド名を保持
    public String getDuplexPrint_BlandName() {
        return _DuplexPrint_BlandName;
    }
    //ADD-E BMT S.Oyama 2025/04/24 フタバ双方向向け改修

    // 手動決済モード時の取引種別
    public static final String MANUALMODE_TRANS_TYPE_CODE_SALES = "20";
    public static final String MANUALMODE_TRANS_TYPE_CODE_CANCEL = "21";

    public static PrinterProc getInstance() {
        if (_instance == null) {
            _instance = new PrinterProc();
        }
        return _instance;
    }

    // 現金併用分の金額
    private int togetherCashSales;
    private int togetherCashCancel;

    // Google Fonts [Kosugi] フォント
    private void setPaintFont() {
        Typeface typeface = MainApplication.get_typeface();
        _paint.setTypeface(typeface);
    }

    private void setPaintSize(int PaintSize_Type) {

        switch (PaintSize_Type) {
            case PaintSize_Normal:
                // 通常（半角32文字/全角16文字）
                _paint.setTextSize(24F);
                isFontHalfSize = 32;
                break;
            case PaintSize_Medium:
                // 取引金額用（半角24文字/全角12文字）
                _paint.setTextSize(32F);
                isFontHalfSize = 24;
                break;
            case PaintSize_Big:
                // 通常倍角（半角16文字/全角8文字）
                _paint.setTextSize(48F);
                isFontHalfSize = 16;
                break;
            default:
                break;
        }
    }

    // 中央揃え
    private void setAlign_Mid(String str, int paint_size) {
        StringBuilder sb = new StringBuilder();
        int space_len = isFontHalfSize;

        setPaintSize(paint_size);

        if (str != null) {
            space_len = (isFontHalfSize - HalfFont_Size(str)) / 2;
            sb.append(HalfSpace_Fill(space_len));
            sb.append(str);
        } else {
            sb.append(HalfSpace_Fill(space_len));
        }

        _printCanvas.drawText(sb.toString(), _paint);
    }

    // 左揃え
    private void setAlign_Left(String str, int paint_size) {
        StringBuilder sb = new StringBuilder();
        int space_len = isFontHalfSize;

        setPaintSize(paint_size);

        if (str != null) {
            space_len = isFontHalfSize - HalfFont_Size(str);
            sb.append(str);
        }
        sb.append(HalfSpace_Fill(space_len));

        _printCanvas.drawText(sb.toString(), _paint);
    }

    // 右揃え
    private void setAlign_Right(String str, int paint_size) {
        StringBuilder sb = new StringBuilder();
        int space_len = isFontHalfSize;

        setPaintSize(paint_size);

        if (str != null) {
            space_len = isFontHalfSize - HalfFont_Size(str);
            sb.append(HalfSpace_Fill(space_len));
            sb.append(str);
        } else {
            sb.append(HalfSpace_Fill(space_len));
        }

        _printCanvas.drawText(sb.toString(), _paint);
    }

    // 左、右揃え
    private void setAlign_LR(String str1, int int2, int paint_size) {
        setAlign_LR(str1, String.valueOf(int2), paint_size);
    }

    private void setAlign_LR(String str1, String str2, int paint_size) {
        StringBuilder sb = new StringBuilder();
        int space_len = isFontHalfSize;

        setPaintSize(paint_size);

        if (str1 != null && str2 != null) {
            int Total_HalfFont_Size = HalfFont_Size(str1) + HalfFont_Size(str2);
            if (Total_HalfFont_Size > isFontHalfSize) {
                // 一行の半角32文字（MAX）印刷超過
                setAlign_Left(str1, paint_size);
                setAlign_Right(str2, paint_size);
            } else {
                space_len = isFontHalfSize - Total_HalfFont_Size;
                sb.append(str1);
                sb.append(HalfSpace_Fill(space_len));
                sb.append(str2);
                _printCanvas.drawText(sb.toString(), _paint);
            }
        } else if (str1 != null) {
            setAlign_Left(str1, paint_size);
        } else if (str2 != null) {
            setAlign_Right(str2, paint_size);
        } else {
            sb.append(HalfSpace_Fill(space_len));
            _printCanvas.drawText(sb.toString(), _paint);
        }
    }

    // 左、中、右揃え
    private void setAlign_LMR(String str1, String str2, String str3, int paint_size) {
        StringBuilder sb = new StringBuilder();

    }

    // 点線ライン
    private void setLine_dotted() {
        setPaintSize(PaintSize_Normal);
        _printCanvas.drawText(_printDataRes.getStringArray(R.array.print_line)[0], _paint);
    }

    // 実線ライン
    private void setLine() {
        setPaintSize(PaintSize_Normal);
        _printCanvas.drawText(_printDataRes.getStringArray(R.array.print_line)[1], _paint);
    }

    // 記入ライン
    private void setLine_fill_in() {
        setPaintSize(PaintSize_Normal);
        _printCanvas.drawText(_printDataRes.getStringArray(R.array.print_line)[2], _paint);
    }

    // 強調ライン
    private void setLine_emphasize() {
        setPaintSize(PaintSize_Medium);
        _printCanvas.drawText(_printDataRes.getStringArray(R.array.print_line)[3], _paint);
    }

    // 改行
    private void setLF(int LF_cnt, int paint_size) {

        setPaintSize(paint_size);

        for (int i = 0; i < LF_cnt; i++) {
            _printCanvas.drawText(" ", _paint);
        }
    }

    // 半角スペース埋め
    private String HalfSpace_Fill(int space_len) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < space_len; i++) {
            sb.append(" ");
        }

        return sb.toString();
    }

    // 半角サイズ
    private int HalfFont_Size(String str) {
        int size = 0;
        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++) {

            char c = chars[i];
            if ((c <= '\u007e') || (c == '\u00a5') || (c == '\u203e') || (c >= '\uff61' && c <= '\uff9f')) {
                size += 1;
            } else {
                size += 2;
            }
        }
        return size;
    }

    // 金額（￥,付き）
    private String trans_amount(int amount) {
        return String.format("\\%,d", amount);
    }

    // 金額（￥,付き）
    private String trans_amount(long amount) {
        return String.format("\\%,d", amount);
    }

    // 増減額（+-,付き）
    private String adj_amount(int amount) {
        if (amount > 0) {
            return String.format("+%,d", amount);
        } else {
            return String.format("%,d", amount);
        }
    }

    // ブランド毎の総額（集計用）
    private String aggregate_brand_total_amount(long amount) {
        if (amount > 0) {
            return String.format("%,d円", amount);
        } else {
            return String.format("%,d円", amount).replace("-", "△");
        }
    }

    // 総額（集計用）
    private String aggregate_total_amount(long amount) {
        return String.format("%,d円", amount);
    }

    // 明細の売上金額（集計用）
    private String aggregate_payment_amount(int amount) {
        return String.format("%,d円", amount);
    }

    // 明細の取消金額（集計用）
    private String aggregate_cancel_amount(int amount) {
        return String.format("△%,d円", amount);
    }

    // 明細のチャージ金額（集計用）
    private String aggregate_charge_amount(int amount) {
        return String.format("%,d円", amount);
    }

    // ポイント付与
    private String trans_pint(int amount) {
        return String.format("%,dP", amount);
    }

    // カード番号（区切り）
    private String card_id_separated(String CardId) {
        if (_slipData.transBrand != null) {
            if ((_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) &&
                    CardId.length() == 17) {
                /* 交通系電子マネー、OKICA(5 4 4 4) */
                return String.format("%s %s %s %s", CardId.substring(0, 5), CardId.substring(5, 9), CardId.substring(9, 13), CardId.substring(13, 17));
            } else if ((_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id))) &&
                    CardId.length() == 16) {
                /* WAON、楽天Edy、nanaco、iD(4 4 4 4) */
                return String.format("%s %s %s %s", CardId.substring(0, 4), CardId.substring(4, 8), CardId.substring(8, 12), CardId.substring(12, 16));
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay)) && CardId.length() == 20) {
                /* QUICPay(4 4 4 4 4) */
                return String.format("%s %s %s %s %s", CardId.substring(0, 4), CardId.substring(4, 8), CardId.substring(8, 12), CardId.substring(12, 16), CardId.substring(16, 20));
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))) {
                /* クレジット */
                return CardId;
            } else {
                /* その他の場合 */
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：card_id_separated->_slipData.transBrand <%s> CardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand, CardId.length());
                return null;
            }
        } else {
            /* 区切りの判別ができないため、そのまま */
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：card_id_separated->_slipData.transBrand <%s> CardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand, CardId.length());
            return null;
        }
    }

    // カード番号（マスク）
    private void card_id_masked(String CardId) {
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica))) {
                /* 交通系電子マネー（上位2桁、下位4桁以外マスク） */
                if (CardId.length() == 17) {
                    isMaskCardId = String.format("%s***********%s", CardId.substring(0, 2), CardId.substring(13, 17));
                } else {
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：card_id_masked->_slipData.transBrand <%s> CardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand, CardId.length());
                    isMaskCardId = "*****************";
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                /* 楽天Edy（下位4桁以外マスク） */
                if (CardId.length() == 16) {
                    isMaskCardId = String.format("************%s", CardId.substring(12, 16));
                } else {
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：card_id_masked->_slipData.transBrand <%s> CardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand, CardId.length());
                    isMaskCardId = "****************";
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanaco（上位12桁以外マスク） */
                if (CardId.length() == 16) {
                    isMaskCardId = String.format("%s****", CardId.substring(0, 12));
                } else {
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：card_id_masked->_slipData.transBrand <%s> CardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand, CardId.length());
                    isMaskCardId = "****************";
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICA（下位4桁以外マスク） */
                if (CardId.length() == 17) {
                    isMaskCardId = String.format("*************%s", CardId.substring(13, 17));
                } else {
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：card_id_masked->_slipData.transBrand <%s> CardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand, CardId.length());
                    isMaskCardId = "*****************";
                }
            } else {
                /* その他 */
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：card_id_masked->_slipData.transBrand <%s> CardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand, CardId.length());
                isMaskCardId = "****************";
            }
        }
    }

    // 履歴照会（実施日時）
    private String history_time(String HistoryTime) {
        if (HistoryTime.length() == 14) {
            return String.format("%s/%s/%s              %s:%s:%s", HistoryTime.substring(0, 4), HistoryTime.substring(4, 6), HistoryTime.substring(6, 8), HistoryTime.substring(8, 10), HistoryTime.substring(10, 12), HistoryTime.substring(12, 14));
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：history_time->HistoryTime <%s>", _printDataRes.getString(R.string.printLog_printDataError), HistoryTime);
            return null;
        }
    }

    // 履歴照会（カード番号）
    private String history_card_id_separated(String HistoryCardId) {
        /* WAON */
        if (HistoryCardId.length() == 16) {
            return String.format("%s %s %s %s", HistoryCardId.substring(0, 4), HistoryCardId.substring(4, 8), HistoryCardId.substring(8, 12), HistoryCardId.substring(12, 16));
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：history_card_id_separated->HistoryCardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), HistoryCardId.length());
            return null;
        }
    }

    // 履歴照会（決済日時）
    private String history_trans_time(String HistoryData, String HistoryTime) {
        if (HistoryData.length() == 8 && HistoryTime.length() == 4) {
            return String.format("%s/%s/%s %s:%s", HistoryData.substring(0, 4), HistoryData.substring(4, 6), HistoryData.substring(6, 8), HistoryTime.substring(0, 2), HistoryTime.substring(2, 4));
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：history_trans_time->HistoryData <%s> HistoryTime <%s>", _printDataRes.getString(R.string.printLog_printDataError), HistoryData, HistoryTime);
            return null;
        }
    }

    // 決済種別名（WAON専用）
    private String history_trade_type_name(String TradeTypeCode, String ChargeType) {

        if (TradeTypeCode.equals("01")) {
            // 支払(01)
            return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[1]);
        } else if (TradeTypeCode.equals("02")) {
            // 返品(02)
            return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[2]);
        } else if (TradeTypeCode.equals("03") || TradeTypeCode.equals("04")) {
            if (ChargeType.equals("4")) {
                // ポイントチャージ(03)、(04)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[3]);
            } else {
                // 現金チャージ(03)、(04)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[4]);
            }
        } else if (TradeTypeCode.equals("05")) {
            // チャージ取消(05)
            return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[5]);
        } else if (TradeTypeCode.equals("07") || TradeTypeCode.equals("08")) {
            // 支払＋オートチャージ(07)、(08)
            return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[6]);
        } else if (TradeTypeCode.equals("10")) {
            // 支払取消(10)
            return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[7]);
        } else if (TradeTypeCode.equals("12") || TradeTypeCode.equals("13")) {
            // オートチャージ(12)、(13)
            return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[8]);
        } else {
            // その他(XX)
            return String.format("%s", _printDataRes.getStringArray(R.array.print_history_trade_type)[0]);
        }
    }

    // 残高履歴（カード番号）
    private String okica_history_card_id_separated(String HistoryCardId) {
        /* OKICA（下位4桁以外マスク） */
        if (HistoryCardId.length() == 17) {
            return String.format("***** **** **** %s", HistoryCardId.substring(13, 17));
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：okica_history_card_id_separated->HistoryCardId.length() <%d>", _printDataRes.getString(R.string.printLog_printDataError), HistoryCardId.length());
            return null;
        }
    }

    // 残高履歴（日付）
    private String history_date(int Year, int Month, int Day) {
        if ((Year >= 0 && Year <= 99) &&
                (Month >= 1 && Month <= 12) &&
                (Day >= 1 && Day <= 31)) {
            return String.format("20%02d/%02d/%02d", Year, Month, Day);
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：history_date->Year <%d> Month <%d> Day <%d>", _printDataRes.getString(R.string.printLog_printDataError), Year, Month, Day);
            return null;
        }
    }

    // 種別名（OKICA専用）
    private String okica_history_process_type_name(int ProcessType) {

        switch (ProcessType) {
            case 0: // 入場(0)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[0]);
            case 1: // 出場(1)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[1]);
            case 2: // SFチャージ(2)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[2]);
            case 3: // 乗車券購入(3)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[3]);
            case 4: // 精算(金券利用)(4)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[4]);
            case 5: // 乗越し精算(5)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[5]);
            case 6: // 窓口精算機出場(6)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[6]);
            case 7: // 新規チャージ(7)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[7]);
            case 8: // 控除(8)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[8]);
            case 15: // バス出場(15)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[9]);
            case 16: // 紛失再発行(16)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[10]);
            case 17: // 障害再発行(17)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[11]);
            case 20: // SF入場＆SFチャージ(20)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[12]);
            case 23: // バス出場＆SFチャージ(23)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[13]);
            case 29: // バス精算(金券利用)(29)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[14]);
            case 31: // バス＆SFチャージ(31)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[15]);
            case 32: // バス控除(32)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[16]);
            case 34: // バス入場キャンセル(34)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[17]);
            case 35: // バスチケット購入(35)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[18]);
            case 36: // バスチケット控除(36)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[19]);
            case 70: // 物販利用(70)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[20]);
            case 72: // ポイントチャージ(72)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[21]);
            case 73: // 物販チャージ(73)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[22]);
            case 76: // 物販利用取消(76)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[23]);
            case 77: // ポイント処理(77)
                return String.format("%s", _printDataRes.getStringArray(R.array.print_okica_history_process_type)[24]);
            default: // 予備
                return null;
        }
    }

    // プリンター初期化
    private void print_init() {
        _printer = Printer.getInstance();
        _printTask = new PrintTask();
        _printTask.setGray(130);
        _printCanvas = new PrintCanvas();
        _printBitmap = null;
        _paint = new Paint();
        _printDataRes = MainApplication.getInstance().getResources();
        setPaintFont();
        setPaintSize(PaintSize_Normal);
        isCreditAnnounceSignature = false;
        isTransResult = null;
        isMaskCardId = null;
        isTransType = null;
        isRePrinter = false;
        isQRTicketPrintSts = QRTicketPrintSts.UNKNOWN.ordinal();

        Log_ErrTitle = null;
        Log_BrandName = null;
        Log_SlipName = null;
        Log_CopyTypeName = null;
        Log_Amounts = null;
        Log_ServicePos = null;
        Log_InvoiceNo = null;
        Log_InvoiceTax = null;
        Log_transDate = null;
        Log_QRTicket = null;

        isPT750_Print = true;
    }

    private String convertSjisString(String sourceStr) throws UnsupportedEncodingException {
        String str = sourceStr;
        String destStr = "";
        // SJISに文字コードを指定
        if (null != sourceStr) {
            byte[] bytes = str.getBytes("SJIS");
            for (int i = 0; i < bytes.length; i++) {
                destStr += String.format("%02x", bytes[i]);
            }
        }
        return destStr;
    }

    private void sendWsPrintdata() {
        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        Object alipayPlusCode;
        Object JCoinPayCode; // J-Coin
        Object AEONPayCode; // AEON Pay
        try {
            alipayPlusCode = "01";
            JCoinPayCode = "06";
            AEONPayCode = "07";
            _sendData.put("type", "/printdata/v1");
            _sendData.put("cmd", "print_start");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
//            _slipData = DBManager.getSlipDao().getOneById(id);
            _params.put("trans_brand", _slipData.transBrand);
            _params.put("trans_type", _slipData.transType);
            if (_slipData.transBrand.equals("コード決済")) {
                if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Wechat))) {
                    _params.put("trans_type_code", "02");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Alipay))) {
                    _params.put("trans_type_code", "01");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Docomo))) {
                    _params.put("trans_type_code", "11");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.auPAY))) {
                    _params.put("trans_type_code", "16");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.PayPay))) {
                    _params.put("trans_type_code", "10");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.LINEPay))) {
                    _params.put("trans_type_code", "04");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.RakutenPay))) {
                    _params.put("trans_type_code", "09");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.GinkoPay))) {
                    _params.put("trans_type_code", "17");       // フタバにはない
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.merpay))) {
                    _params.put("trans_type_code", "15");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                    _params.put("trans_type_code", "08");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.AlipayPlus))) {
                    _params.put("trans_type_code", alipayPlusCode);     // 暫定的にAlipayと同じコードを送信
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.JCoinPay))) {
                    _params.put("trans_type_code", JCoinPayCode);
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.AEONPay))) {
                    _params.put("trans_type_code", AEONPayCode);
                } else {
                    _params.put("trans_type_code", "00");
                }
            } else {
                _params.put("trans_type_code", _slipData.transTypeCode);
            }
            _params.put("trans_result", _slipData.transResult);
            _params.put("trans_result_detail", _slipData.transResultDetail);
            _params.put("print_cnt", _slipData.printCnt);
            _params.put("old_aggregate_order", _slipData.oldAggregateOrder);
            _params.put("encrypt_type", _slipData.encryptType);
            _params.put("cancel_flg", _slipData.cancelFlg);
            _params.put("trans_id", _slipData.transId);
            _params.put("merchant_name", convertSjisString(_slipData.merchantName));
            _params.put("merchant_office", convertSjisString(_slipData.merchantOffice));
            _params.put("merchant_telnumber", _slipData.merchantTelnumber);
            _params.put("car_id", _slipData.carId);
            _params.put("driver_id", _slipData.driverId);
            _params.put("term_id", _slipData.termId);
            _params.put("term_sequence", _slipData.termSequence);
            _params.put("trans_date", _slipData.transDate);
            _params.put("card_company", convertSjisString(_slipData.cardCompany));
            _params.put("card_id_merchant", _slipData.cardIdMerchant);
            _params.put("card_id_customer", _slipData.cardIdCustomer);
            _params.put("card_exp_date", _slipData.cardExpDate);
            _params.put("card_trans_number", _slipData.cardTransNumber);
            _params.put("nanaco_slip_number", _slipData.nanacoSlipNumber);
            _params.put("edy_trans_number", _slipData.edyTransNumber);
            _params.put("slip_number", _slipData.slipNumber);
            _params.put("old_slip_number", _slipData.oldSlipNumber);
            _params.put("auth_id", _slipData.authId);
            _params.put("auth_sequence_number", _slipData.authSequenceNumber);
            _params.put("commodity_code", _slipData.commodityCode);
            _params.put("installment", _slipData.installment);
            _params.put("point", _slipData.point);
            _params.put("point_grant_type", _slipData.pointGrantType);
            _params.put("point_grant_msg_one", _slipData.pointGrantMsgOne);
            _params.put("point_grant_msg_two", _slipData.pointGrantMsgTwo);
            _params.put("term_ident_id", _slipData.termIdentId);
            _params.put("trans_amount", _slipData.transAmount);
            _params.put("trans_specified_amount", _slipData.transSpecifiedAmount);
            _params.put("trans_meter_amount", _slipData.transMeterAmount);
            _params.put("trans_adj_amount", _slipData.transAdjAmount);
            _params.put("trans_cash_together_amount", _slipData.transCashTogetherAmount);
            _params.put("trans_other_amount_one_type", _slipData.transOtherAmountOneType);
            _params.put("trans_other_amount_one", _slipData.transOtherAmountOne);
            _params.put("trans_other_amount_two_type", _slipData.transOtherAmountTwoType);
            _params.put("trans_other_amount_two", _slipData.transOtherAmountTwo);
            _params.put("free_count_one", _slipData.freeCountOne);
            _params.put("free_count_two", _slipData.freeCountTwo);
            _params.put("trans_before_balance", _slipData.transBeforeBalance);
            _params.put("trans_after_balance", _slipData.transAfterBalance);
            _params.put("common_name", _slipData.commonName);
            _params.put("credit_type", _slipData.creditType);
            _params.put("credit_arc", _slipData.creditArc);
            _params.put("credit_aid", _slipData.creditAid);
            _params.put("credit_apl", _slipData.creditApl);
            _params.put("credit_signature_flg", _slipData.creditSignatureFlg);
            _params.put("codetrans_order_id", _slipData.codetransOrderId);
            _params.put("codetrans_pay_type_name", convertSjisString(_slipData.codetransPayTypeName));
            _params.put("auth_id_str", _slipData.printingAuthId);

            // パラメータの格納
            _sendData.put("data", _params);
            //_ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintdata->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    /* ADD-S N.Sasaki 2024/04/18 ヤザキLT27双方向 LANSポイント対応 */
    private void sendWsPrintdata_watari() {
        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        try {
            _sendData.put("type", "/printpoint/v1");
            _sendData.put("cmd", "print_start");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
            _params.put("trans_brand", _slipData.transBrand);
            _params.put("trans_type", _slipData.transType);
            _params.put("trans_type_code", _slipData.transTypeCode);
            _params.put("print_cnt", _slipData.printCnt);
            _params.put("term_id", _slipData.termId);
            _params.put("trans_amount", _slipData.transAmount);
            _params.put("trans_meter_amount", _slipData.transMeterAmount);
            _params.put("point", _slipData.watariPoint);
            _params.put("point_total", _slipData.watariSumPoint);
            // パラメータの格納
            _sendData.put("data", _params);
            //_ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintdata_watari->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }
    /* ADD-E N.Sasaki 2024/04/18 ヤザキLT27双方向 LANSポイント対応 */

    private void sendWsPrintNext() {
        JSONObject _sendData = new JSONObject();
        try {
            _sendData.put("type", "/printdata/v1");
            _sendData.put("cmd", "print_next");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
            // パラメータの格納
            //_ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintNext->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    private void sendWsPrintAggregate() {
        JSONObject _sendData = new JSONObject();
        try {
            _sendData.put("type", "/printdata/v1");
            _sendData.put("cmd", "print_nikkei");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
            //_ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimerAggregate);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintAggregate->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    private void sendWsPrintdata_okabe() {
        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        Object alipayPlusCode;
        Object JCoinPayCode; // J-Coin
        Object AEONPayCode; // AEON Pay
        try {
            alipayPlusCode = "18";
            JCoinPayCode = "06";
            AEONPayCode = "07";
            _sendData.put("type", "/printdata/v2");
            _sendData.put("cmd", "print_start");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
//            _slipData = DBManager.getSlipDao().getOneById(id);
            _params.put("trans_brand", _slipData.transBrand);
            _params.put("trans_type", _slipData.transType);
            if (_slipData.transBrand.equals("コード決済")) {
                if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Wechat))) {
                    _params.put("trans_type_code", "02");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Alipay))) {
                    _params.put("trans_type_code", "01");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Docomo))) {
                    _params.put("trans_type_code", "11");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.auPAY))) {
                    _params.put("trans_type_code", "16");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.PayPay))) {
                    _params.put("trans_type_code", "10");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.LINEPay))) {
                    _params.put("trans_type_code", "04");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.RakutenPay))) {
                    _params.put("trans_type_code", "09");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.GinkoPay))) {
                    _params.put("trans_type_code", "17");       // フタバにはない
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.merpay))) {
                    _params.put("trans_type_code", "15");
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                    _params.put("trans_type_code", "08");       // フタバにはない
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.AlipayPlus))) {
                    _params.put("trans_type_code", alipayPlusCode);
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.JCoinPay))) {
                    _params.put("trans_type_code", JCoinPayCode);
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.AEONPay))) {
                    _params.put("trans_type_code", AEONPayCode);
                } else {
                    _params.put("trans_type_code", "00");
                }
            } else {
                _params.put("trans_type_code", _slipData.transTypeCode);
            }
            _params.put("trans_result", _slipData.transResult);
            _params.put("trans_result_detail", _slipData.transResultDetail);
            _params.put("print_cnt", _slipData.printCnt);
            _params.put("old_aggregate_order", _slipData.oldAggregateOrder);
            _params.put("encrypt_type", _slipData.encryptType);
            _params.put("cancel_flg", _slipData.cancelFlg);
            _params.put("trans_id", _slipData.transId);
            _params.put("merchant_name", convertSjisString(_slipData.merchantName));
            _params.put("merchant_office", convertSjisString(_slipData.merchantOffice));
            _params.put("merchant_telnumber", _slipData.merchantTelnumber);
            _params.put("car_id", _slipData.carId);
            _params.put("driver_id", _slipData.driverId);
            _params.put("term_id", _slipData.termId);
            _params.put("term_sequence", _slipData.termSequence);
            _params.put("trans_date", _slipData.transDate);
            _params.put("card_company", convertSjisString(_slipData.cardCompany));
            _params.put("card_id_merchant", _slipData.cardIdMerchant);
            _params.put("card_id_customer", _slipData.cardIdCustomer);
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) || _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))) {
                /* iD、クレカ */
                _params.put("card_exp_date", _slipData.cardExpDate);
            } else {
                /* iD、クレカ以外*/
                _params.put("card_exp_date", "");
            }
            /* nanacoの場合、「取引通番」に伝票番号の値を設定（JREM指摘事項）*/
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {  //nanacoの時
                _params.put("card_trans_number", _slipData.nanacoSlipNumber);
            } else {
                //nanaco以外
                _params.put("card_trans_number", _slipData.cardTransNumber);
            }
            _params.put("nanaco_slip_number", _slipData.nanacoSlipNumber);
            _params.put("edy_trans_number", _slipData.edyTransNumber);
            _params.put("slip_number", _slipData.slipNumber);
            _params.put("old_slip_number", _slipData.oldSlipNumber);
            _params.put("auth_id", _slipData.authId);
            _params.put("auth_sequence_number", _slipData.authSequenceNumber);
            _params.put("commodity_code", _slipData.commodityCode);
            _params.put("installment", _slipData.installment);
            _params.put("point", _slipData.point);
            _params.put("point_grant_type", _slipData.pointGrantType);
            /*CHG-S k.Fukumitsu  2023/11/30  処理未了または通信障害の時nullを返すように修正　(未了、障害時はポイント関連内容を出さない為)*/
            _params.put("point_grant_msg_one", null);
            _params.put("point_grant_msg_two", null);
            /*CHG-E k.Fukumitsu  2023/11/30  処理未了または通信障害の時nullを返すように修正　(未了、障害時はポイント関連内容を出さない為)*/
            _params.put("term_ident_id", _slipData.termIdentId);
            _params.put("trans_amount", _slipData.transAmount);
            _params.put("trans_specified_amount", _slipData.transSpecifiedAmount);
            _params.put("trans_meter_amount", _slipData.transMeterAmount);
            _params.put("trans_adj_amount", _slipData.transAdjAmount);
            _params.put("trans_cash_together_amount", _slipData.transCashTogetherAmount);
            _params.put("trans_other_amount_one_type", _slipData.transOtherAmountOneType);
            _params.put("trans_other_amount_one", _slipData.transOtherAmountOne);
            _params.put("trans_other_amount_two_type", _slipData.transOtherAmountTwoType);
            _params.put("trans_other_amount_two", _slipData.transOtherAmountTwo);
            _params.put("free_count_one", _slipData.freeCountOne);
            _params.put("free_count_two", _slipData.freeCountTwo);
            _params.put("trans_before_balance", _slipData.transBeforeBalance);
            _params.put("trans_after_balance", _slipData.transAfterBalance);
            _params.put("common_name", _slipData.commonName);
            _params.put("credit_type", _slipData.creditType);
            _params.put("credit_arc", _slipData.creditArc);
            _params.put("credit_aid", _slipData.creditAid);
            _params.put("credit_apl", _slipData.creditApl);
            _params.put("credit_signature_flg", _slipData.creditSignatureFlg);
            _params.put("codetrans_order_id", _slipData.codetransOrderId);
            _params.put("codetrans_pay_type_name", convertSjisString(_slipData.codetransPayTypeName));
            _params.put("auth_id_str", _slipData.printingAuthId);
            setWsPrintdataV2(_params);
            // パラメータの格納
            _sendData.put("data", _params);
            //_ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：printTrans->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    private void sendWsReprintdata_FutabaD() {
        Timber.i("[FUTABA-D]sendWsReprintdata_FutabaD");
//
//        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)) {              //フタバD以外は処理しない
//            return ;
//        }
//
//        if (_ifBoxManager.getIsConnected820() == false)             //820未接続の場合
//        {
//            PrinterManager.getInstance().PrinterDuplexError(PrinterConst.DuplexPrintStatus_DISCON);
//            Printing_end();
//            return ;
//        }
//
//        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
//        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.IsLoopBreakOut = false;
//        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.ErrorCodeExt1 = 0;
//
//        meterDataV4InfoDisposable = _ifBoxManager.getMeterDataV4().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
//            Timber.i("[FUTABA-D]sendWsReprintdata_FutabaD:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//            if (meter.meter_sub_cmd == 5 || meter.sound_no == IFBoxManager.Send820Status_JobReq_FutabaD.JOBREQ_V3_PRINTSTART_RECV) {           //処理コード要求　かつ，V3系print_start print_end受信
//                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//            }
//        });
//
//        meterDataV4ErrorDisposable = _ifBoxManager.getMeterDataV4Error().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//            Timber.e("[FUTABA-D]sendWsReprintdata_FutabaD:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//            tmpSend820Info.StatusCode = error.ErrorCode;
//            tmpSend820Info.ErrorCode820 = error.ErrorCode820;
//
//        });
//
//        tmpSend820Info.ErrorCodeExt1 = 999999;               //通信等でエラー発生時は999999以外のエラーコードをセットする
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                _ifBoxManager.send820_Reprint_KeyCode();
//
//                for(int i = 0; i < (DuplexPrintResponseTimerSec + 1) * 10; i++)        //最大26秒ほど待ってみる
//                {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                    }
//
//                    if (tmpSend820Info.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
//                    {
//                        tmpSend820Info.IsLoopBreakOut = true;
//                        break;
//                    }
//                }
//            }
//        });
//        thread.start();
//
//        try {
//            thread.join();
//
//            if (meterDataV4InfoDisposable != null) {       //コールバック系を後始末
//                meterDataV4InfoDisposable.dispose();
//                meterDataV4InfoDisposable = null;
//            }
//
//            if (meterDataV4ErrorDisposable != null) {      //コールバック系を後始末
//                meterDataV4ErrorDisposable.dispose();
//                meterDataV4ErrorDisposable = null;
//            }
//
//            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
//                tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//            }
//            else
//            {
//                switch(tmpSend820Info.StatusCode)                       //ステータスコードのチェック
//                {
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                        Timber.e("[FUTABA-D](demo)820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
//                        if (tmpSend820Info.ErrorCode820 == IFBoxManager.Send820Status_Error_FutabaD.ERROR_STATUS820_PAPERLACKING)       //用紙無しエラー
//                        {
//                            tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART;                     //用紙なしの場合はエラーコードを入れる
//                        } else {
//                            tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        }
//                        break;
//                    default:
//                        //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
//                        break;
//                }
//            }
//
//        } catch (Exception e) {
//            Timber.e(e);
//            tmpSend820Info.ErrorCodeExt1 =  PrinterConst.DuplexPrintStatus_DATAERROR;
//        }
//
//        if (tmpSend820Info.ErrorCodeExt1 != 999999){           //エラーコードが設定されている場合
//            Handler handler= new Handler(Looper.getMainLooper());
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    if(tmpSend820Info.ErrorCodeExt1 == PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART) {           //用紙なしエラーの場合
//                        PrinterManager.getInstance().PrinterDuplexError(tmpSend820Info.ErrorCodeExt1);
//                    }
//                    else {                                                                              //その他のエラーの場合
//                        PrinterManager.getInstance().PrinterDuplexError(PrinterConst.DuplexPrintStatus_DISCON);
//                    }
//                    PrinterManager.getInstance().dismissPrintingDialogExt();
//                    PrintDataError();
//                    //Printing_end();
//                }
//            });
//        }
    }

//ADD-S BMT S.Oyama 2024/09/3 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  決済データ通知（フタバ双方向用）
     * @note   PT750->IM820への決済データ通知 /printdata/v3:print_start
     * @param [in] boolean tmpKessaiKakuninFl:False 通常 true 決済確認モード
     * @param [in] String tmpStatusCode:決済確認時ステータスコード　決済確認のみセット　それ以外は空文字
     * @param [in] boolean tmpSeparationMainSendFL 分別時の2回目の送信実施時フラグ
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    private void sendWsPrintdata_FutabaD(boolean tmpKessaiKakuninFl, String tmpStatusCode , boolean tmpSeparationMainSendFL) {

        Timber.i("[FUTABA-D]sendWsPrintdata_FutabaD()  KessaiKakuninFl:%d StatusCode:%s", tmpKessaiKakuninFl ? 1:0, tmpStatusCode);

        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        Object alipayPlusCode;
        Object JCoinPayCode; // J-Coin
        Object AEONPayCode; // AEON Pay
        String tmpDateTimeStrConvert;
        String tmpQRSettlmentNameStr = "";

        boolean isCommFailure = (isTransResult == PrinterConst.TransResult_UnFinished && _slipData.transResultDetail == PrinterConst.TransDetail_Communication_Failure);
        boolean isSuica = _slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_suica));
        boolean isWaon = _slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_waon));
        boolean isEdy = _slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_edy));
        boolean isNanaco = _slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_nanaco));
        //ADD-S BMT S.Oyama 2025/04/24 フタバ双方向向け改修
        _DuplexPrint_BlandName = _slipData.transBrand;
        //ADD-E BMT S.Oyama 2025/04/24 フタバ双方向向け改修

        try {
            alipayPlusCode = "03";
            JCoinPayCode = "06";
            AEONPayCode = "07";
            _sendData.put("type", "/printdata/v3");
            _sendData.put("cmd", "print_start");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
//            _slipData = DBManager.getSlipDao().getOneById(id);
            _params.put("trans_brand", _slipData.transBrand);
            _params.put("trans_type", _slipData.transType);
            if (_slipData.transBrand.equals("コード決済")) {
                if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Wechat))) {
                    _params.put("trans_type_code", "02");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.WECHAT);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Alipay))) {
                    _params.put("trans_type_code", "01");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.ALIPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Docomo))) {
                    _params.put("trans_type_code", "11");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.DOCOMO);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.auPAY))) {
                    _params.put("trans_type_code", "16");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.AUPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.PayPay))) {
                    _params.put("trans_type_code", "10");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.PAYPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.LINEPay))) {
                    _params.put("trans_type_code", "04");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.LINEPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.RakutenPay))) {
                    _params.put("trans_type_code", "09");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.RAKUTENPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.GinkoPay))) {
                    _params.put("trans_type_code", "17");       // フタバにはない
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.GINKOPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.merpay))) {
                    _params.put("trans_type_code", "15");
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.MERPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                    _params.put("trans_type_code", "08");       // フタバにはない
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.QUOPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.AlipayPlus))) {
                    _params.put("trans_type_code", alipayPlusCode);
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.ALIPAYPLUS);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.JCoinPay))) {
                    _params.put("trans_type_code", JCoinPayCode);
                } else if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.AEONPay))) {
                    _params.put("trans_type_code", AEONPayCode);
                    tmpQRSettlmentNameStr = CardBrand.QR.ConvertToName(CardBrand.AEONPAY);          //ADD BMT S.Oyama 2025/03/8 フタバ双方向向け改修
                } else {
                    _params.put("trans_type_code", "00");
                }
            } else {
                _params.put("trans_type_code", _slipData.transTypeCode);
            }

            //_params.put("trans_type_name", _slipData.trans_type_name);                                  //取引種別名

            if (_slipData.transBrand.equals("クレジット") == true) {                                             //クレジット系
                _params.put("status", "000");                                                       //応答ｽﾃｰﾀｽ 000で仮設定
            } else if (_slipData.transBrand.equals("コード決済") == true) {                                      //QR系
                _params.put("status", "000");                                                       //応答ｽﾃｰﾀｽ 000で仮設定
            } else {                                                                                           //電子マネー系
                if (isTransResult == PrinterConst.TransResult_UnFinished) {
                    if (_slipData.transBrand.equals("交通系電子マネー")) {
                        _params.put("status", "v1B0");
                    } else if (_slipData.transBrand.equals("WAON")) {
                        if (_slipData.transResultDetail == PrinterConst.TransDetail_Communication_Failure) {
                            _params.put("status", "T98");
                        } else {
                            _params.put("status", "u90 ");
                        }
                    } else if (_slipData.transBrand.equals("nanaco")) {
                        if (_slipData.transResultDetail == PrinterConst.TransDetail_Communication_Failure) {
                            _params.put("status", "D108");
                        } else {
                            _params.put("status", "D106");
                        }
                    } else if (_slipData.transBrand.equals("楽天Edy")) {
                        _params.put("status", "D005");
                    } else {
                        _params.put("status", "u90 ");
                    }
                } else {
                    _params.put("status", "0000");                                                      //応答ｽﾃｰﾀｽ 0000で仮設定
                }
            }

            _params.put("trans_result", _slipData.transResult);
            _params.put("trans_result_detail", _slipData.transResultDetail);
            //_params.put("slip_kind", _slipData.slip_kind);                                              //伝票種別指示
            _params.put("print_cnt", _slipData.printCnt);
            _params.put("old_aggregate_order", _slipData.oldAggregateOrder);
            _params.put("encrypt_type", _slipData.encryptType);
            _params.put("cancel_flg", _slipData.cancelFlg);
            _params.put("trans_id", _slipData.transId);
            //_params.put("pay_separation", _slipData.pay_separation);                                    //分別払い
            _params.put("merchant_name", convertSjisString(_slipData.merchantName));
            _params.put("merchant_office", convertSjisString(_slipData.merchantOffice));
            _params.put("merchant_telnumber", _slipData.merchantTelnumber);
            _params.put("car_id", _slipData.carId);
            _params.put("driver_id", _slipData.driverId);
            _params.put("term_id", _slipData.termId);
            _params.put("term_sequence", _slipData.termSequence);
            _params.put("trans_date", _slipData.transDate);
            _params.put("card_company", convertSjisString(_slipData.cardCompany));
            //ADD-S BMT S.Oyama 2025/03/15 フタバ双方向向け改修
            if (isCommFailure && (isSuica || isWaon || isEdy || isNanaco)) {
                // 通信障害
                _params.put("card_id_merchant", "--");
                _params.put("card_id_customer", "--");
            } else {
                _params.put("card_id_merchant", _slipData.cardIdMerchant);
                _params.put("card_id_customer", _slipData.cardIdCustomer);
            }
            //ADD-E BMT S.Oyama 2025/03/15 フタバ双方向向け改修
            _params.put("card_id_card_company", _slipData.card_id_card_company);                        //カード番号※暗号化対象
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) || _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit)) || _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay))) {
                /* iD、クレカ QUICPAY * */
                _params.put("card_exp_date", _slipData.cardExpDate);
            } else {
                /* iD、クレカ QUICPAY以外*/
                _params.put("card_exp_date", "");
            }
            _params.put("card_exp_date_merchant", _slipData.card_exp_date_merchant);                    //カード有効期限（加盟店）
            _params.put("card_exp_date_card_company", _slipData.card_exp_date_card_company);            //カード有効期限（カード会社）
            /* nanacoの場合、「取引通番」に伝票番号の値を設定（JREM指摘事項）*/
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {  //nanacoの時
                if (isCommFailure) {
                    _params.put("card_trans_number", "--");
                } else {
                    _params.put("card_trans_number", _slipData.nanacoSlipNumber);
                }
            } else {
                //nanaco以外
                _params.put("card_trans_number", _slipData.cardTransNumber);
            }
            _params.put("nanaco_slip_number", _slipData.nanacoSlipNumber);
            _params.put("edy_trans_number", _slipData.edyTransNumber);

            if (isCommFailure & isWaon) {
                _params.put("slip_number", "--");
            } else {
                if (_slipData.slipNumber != null) {
                    _params.put("slip_number", _slipData.slipNumber);
                } else {
                    _params.put("slip_number", _slipData.cardTransNumber);
                }
            }
            _params.put("old_slip_number", _slipData.oldSlipNumber);
            _params.put("auth_id", _slipData.authId);
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))) {
                if (_slipData.printingAuthId != null)
                {
                    _params.put("auth_id_str", _slipData.printingAuthId);                                      //承認番号（string型）      //メンバauth_id_strは使用しない　元から定義してある「printingAuthId」を使用のこと
                }
                else
                {
                    _params.put("auth_id_str", "-------");                                      //承認番号（string型）      //メンバauth_id_strは使用しない　元から定義してある「printingAuthId」を使用のこと
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id))) {
                if ((_slipData.printingAuthId != null) && (_slipData.off_on_type.equals("1")))
                {
                    _params.put("auth_id_str", _slipData.printingAuthId);                                      //承認番号（string型）      //メンバauth_id_strは使用しない　元から定義してある「printingAuthId」を使用のこと
                }
                else
                {
                    _params.put("auth_id_str", "-------");                                      //承認番号（string型）      //メンバauth_id_strは使用しない　元から定義してある「printingAuthId」を使用のこと
                }

                if ((_slipData.authSequenceNumber != null) && (_slipData.off_on_type.equals("1")))
                {
                    _params.put("auth_sequence_number", _slipData.authSequenceNumber);
                }
                else
                {
                    _params.put("auth_sequence_number", "-------");
                }
            }
            _params.put("commodity_code", _slipData.commodityCode);
            _params.put("installment", _slipData.installment);
            _params.put("point", _slipData.point);
            _params.put("point_grant_type", _slipData.pointGrantType);
            /*CHG-S k.Fukumitsu  2023/11/30  処理未了または通信障害の時nullを返すように修正　(未了、障害時はポイント関連内容を出さない為)*/
            _params.put("point_grant_msg_one", null);
            _params.put("point_grant_msg_two", null);
            /*CHG-E k.Fukumitsu  2023/11/30  処理未了または通信障害の時nullを返すように修正　(未了、障害時はポイント関連内容を出さない為)*/
            _params.put("term_ident_id", _slipData.termIdentId);
            _params.put("trans_amount", _slipData.transAmount);
            _params.put("trans_specified_amount", _slipData.transSpecifiedAmount);
            _params.put("trans_meter_amount", _slipData.transMeterAmount);
            _params.put("trans_adj_amount", _slipData.transAdjAmount);
            _params.put("trans_cash_together_amount", _slipData.transCashTogetherAmount);
            //_params.put("trans_complete_amount", _slipData.transCompleteAmount);                        //支払済み金額
            _params.put("trans_other_amount_one_type", _slipData.transOtherAmountOneType);
            _params.put("trans_other_amount_one", _slipData.transOtherAmountOne);
            _params.put("trans_other_amount_two_type", _slipData.transOtherAmountTwoType);
            _params.put("trans_other_amount_two", _slipData.transOtherAmountTwo);
            _params.put("free_count_one", _slipData.freeCountOne);
            _params.put("free_count_two", _slipData.freeCountTwo);
            //ADD-S BMT S.Oyama 2025/03/15 フタバ双方向向け改修
            if (isCommFailure && (isSuica || isWaon || isEdy || isNanaco)) {
                // 通信障害
                _params.put("trans_before_balance", "--");
            } else {
                _params.put("trans_before_balance", _slipData.transBeforeBalance);
            }
            //ADD-E BMT S.Oyama 2025/03/15 フタバ双方向向け改修
            if (isCommFailure && (isSuica || isWaon || isEdy || isNanaco)) {
                // 通信障害
                _params.put("trans_after_balance", "--");
            } else if (isTransResult == PrinterConst.TransResult_UnFinished && isWaon) {
                _params.put("trans_after_balance", _slipData.transBeforeBalance);
            } else {
                _params.put("trans_after_balance", _slipData.transAfterBalance);
            }
            _params.put("common_name", _slipData.commonName);
            _params.put("credit_type", _slipData.creditType);
            _params.put("credit_arc", _slipData.creditArc);
            _params.put("credit_aid", _slipData.creditAid);
            _params.put("credit_apl", _slipData.creditApl);
            //_params.put("credit_kid", _slipData.creditKid);                                             //KID

            int tmpcredit_signature_flg;
            if (_slipData.creditType != null) {                         //クレジットカード種
                if (_slipData.creditType.equals("MS") == true)          //磁気カードの場合
                {
                    tmpcredit_signature_flg = 1;                        //磁気カードの場合は署名あり
                } else {
                    if (_slipData.creditSignatureFlg != null) {         //サイン有効時
                        switch (_slipData.creditSignatureFlg) {
                            case 0:  // サイン不要
                                tmpcredit_signature_flg = 2;
                                break;
                            case 1:  // サイン必要
                                tmpcredit_signature_flg = 1;
                                break;
                            case 2:  // 署名欄なし
                                tmpcredit_signature_flg = 0;            //署名なし
                                break;
                            default:
                                tmpcredit_signature_flg = 1;            //署名あり
                                break;
                        }

                    } else {
                        tmpcredit_signature_flg = 1;                    //署名あり
                    }
                }
            }
            else
            {
                tmpcredit_signature_flg = 1;                            //署名あり
            }
            _params.put("credit_signature_flg", tmpcredit_signature_flg);

            _params.put("codetrans_order_id", _slipData.codetransOrderId);
            _params.put("codetrans_pay_type_name", convertSjisString(_slipData.codetransPayTypeName));
            _params.put("cat_dual_type", _slipData.cat_dual_type);                                      //0:CAT型、1:DUAL型
//            if (_slipData.card_seq_no != null) {
//                if (_slipData.card_seq_no.equals("-1") == true) {
//                    _params.put("card_seq_no", "  ");                                                 //カードシーケンス番号  初期値の場合はSPACE2個
//                } else {
//                    _params.put("card_seq_no", _slipData.card_seq_no);                                      //カードシーケンス番号
//                }
//            }
//            else
//            {
//                _params.put("card_seq_no", "");                                                    //カードシーケンス番号 null時は空文字
//            }
            //ADD-S BMT S.Oyama 2025/03/6 フタバ双方向向け改修
            // _params.put("card_seq_no", "  ");                                                             //カードシーケンス番号  SPACE2個
            if (_slipData.transBrand.equals("クレジット") == true) {                                           //クレジット系
                _params.put("card_seq_no", "  ");                                                 //カードシーケンス番号  SPACE2個
            } else {
                if (_slipData.card_seq_no == null || _slipData.card_seq_no.isEmpty() == true) {
                    //ADD-S BMT S.Oyama 2025/03/15 フタバ双方向向け改修
                    if (isCommFailure && isEdy) {
                        // 通信障害
                        _params.put("card_seq_no", "--");                                           //カードシーケンス番号  SPACE2個
                    } else {
                        _params.put("card_seq_no", "  ");                                             //カードシーケンス番号  SPACE2個
                    }
                    //ADD-E BMT S.Oyama 2025/03/15 フタバ双方向向け改修
                } else {
                    _params.put("card_seq_no", _slipData.card_seq_no);                                  //カードシーケンス番号
                }
            }
            //ADD-E BMT S.Oyama 2025/03/6 フタバ双方向向け改修
            _params.put("atc", _slipData.atc);                                                          //ATC
            _params.put("rw_id", _slipData.rw_id);                                                      //RWID
            _params.put("sprw_id", _slipData.sprw_id);                                                  //SPRWID
            _params.put("off_on_type", _slipData.off_on_type);                                          //0:オフライン、1:オンライン
            _params.put("card_type", _slipData.card_type);                                              //カード区分
            if (isCommFailure && isWaon) {
                // 通信障害
                _params.put("card_id", "--");                                                  //カードID
                _params.put("point_yuko_msg", "--");                                    //ポイント利用不可時のメッセージ
            } else {
                _params.put("card_id", _slipData.card_id);                                                  //カードID
                _params.put("point_yuko_msg", _slipData.point_yuko_msg);                                    //ポイント利用不可時のメッセージ
            }
            _params.put("point_marchant", _slipData.point_marchant);                                    //加盟店ポイント
            int point = 0;
            if (_slipData.point_total != null && !_slipData.point_total.equals("")) {
                if (_slipData.point_total.contains("-")) {
                    point = Integer.parseInt(_slipData.point_total.substring(1));
                    _params.put("point_total", String.format("-%07d", point));                                          //累計ポイント
                } else {
                    point = Integer.parseInt(_slipData.point_total);
                    _params.put("point_total", String.format("%08d", point));                                          //累計ポイント
                }
            } else {
                _params.put("point_total", _slipData.point_total);                                          //累計ポイント
            }
            _params.put("point_exp_date", _slipData.point_exp_date);                                    //ポイント有効期限
            _params.put("point_exp", _slipData.point_exp);                                              //期限ポイント
            if (isCommFailure && isWaon) {
                //WAON取引種別コード
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            _params.put("waon_trans_type_code", "1");                              //WAON取引種別コード
                            break;
                        case 1: // 取引種別：取消
                            _params.put("waon_trans_type_code", "10");                             //WAON取引種別コード
                            break;
                        default:
                            _params.put("waon_trans_type_code", "--");
                            break;
                    }
                } else {
                    _params.put("waon_trans_type_code", "--");
                }
                _params.put("card_slip_no", "--");                                                 //カード通番
            } else {
                _params.put("waon_trans_type_code", _slipData.waon_trans_type_code);                    //WAON取引種別コード
                _params.put("card_slip_no", _slipData.card_slip_no);                                    //カード通番
            }
            _params.put("lid", _slipData.lid);                                                          //端末シリアル番号
            _params.put("service_name", _slipData.service_name);                                        //サービス名
            _params.put("card_trans_number_str", _slipData.card_trans_number_str);                      //取引通番
            _params.put("pay_id", _slipData.pay_id);                                                    //支払ID
            _params.put("ic_no", _slipData.ic_no);                                                      //IC通番
            _params.put("old_ic_no", _slipData.old_ic_no);                                              //元IC通番
            _params.put("terminal_no", _slipData.terminal_no);                                          //端末番号
            _params.put("terminal_seq_no", _slipData.terminal_seq_no);                                  //端末通番
            _params.put("unique_id", _slipData.uniqueId);                                               //ユニークID
            _params.put("terminal_id", _slipData.terminal_id);                                          //上位端末ID
            //ADD-S BMT S.Oyama 2025/03/15 フタバ双方向向け改修
            if (isCommFailure && isEdy) {
                // 通信障害
                _params.put("edy_seq_no", "--");                                                    //Edy取引通番
            } else {
                _params.put("edy_seq_no", _slipData.edy_seq_no);                                            //Edy取引通番
            }
            //ADD-E BMT S.Oyama 2025/03/15 フタバ双方向向け改修
            _params.put("input_kingaku", _slipData.input_kingaku);                                      //入力金額
            //_params.put("auth_id_str", _slipData.printingAuthId);
            setWsPrintdataV2(_params);

            _params.put("status_code", tmpStatusCode);                                                    //応答ｽﾃｰﾀｽコード（決済確認時のみセット）

            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) == true) {                                                    //iDのみ　端末番号　機器番号を分ける（他は同一）
                _params.put("term_id", _slipData.termId);
                _params.put("term_ident_id", _slipData.termIdentId);
            }

            //ADD-S BMT S.Oyama 2025/03/8 フタバ双方向向け改修
            _params.put("qr_settlement_namestr", convertSjisString(tmpQRSettlmentNameStr));                                                    //QR決済名称
            //ADD-E BMT S.Oyama 2025/03/8 フタバ双方向向け改修

            boolean tmpuseNewSendSystenFL = false;
            if (_slipData.transBrand.equals("分別チケット")) {                                            //分別チケットの場合
                //_ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.SENDING);
                //_ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.SEPARATION_TIKECT);
            } else if (_slipData.transBrand.equals("プリペイド")) {                                        //プリペイドの場合
                int tmpTranType = _slipData.transType;              //transe_typeにプリペイド処理の挙動が記載される
                int tmpSettlementSelectMode = 0;

                //-- 以下   プリペイド向け決済選択の送信処理
                switch(tmpTranType)
                {
//                    case TransMap.TYPE_SALES :                      //売上
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_PAY;
//                        break;
//                    case TransMap.TYPE_CANCEL :                     //取消
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_PAYREFUND;
//                        break;
//                    case TransMap.TYPE_POINT :                      //ポイント付与
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTADD;
//                        break;
//                    case TransMap.TYPE_POINT_CANCEL :               //ポイント取消
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTREFUND;
//                        break;
//                    case TransMap.TYPE_CACHE_CHARGE :               //現金チャージ
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CACHECHARGE;
//                        break;
//                    case TransMap.TYPE_CACHE_CHARGE_CANCEL :        //現金チャージ取消
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CHCHECHARGEREFUND;
//                        break;
//                    case TransMap.TYPE_POINT_CHARGE :               //ポイントチャージ
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTCHARGE;
//                        break;
//                    case TransMap.TYPE_PREPAID_CARDBUY  :           //プリペイド発売
//                        tmpSettlementSelectMode = IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CARDBUY;
//                        break;
                    default:
                        return;
                }

//                boolean tmpSendOKNg = send820PrepaidSettlementSelectMode(tmpSettlementSelectMode);      //プリペイド向け決済選択の送信処理
//
//                if (tmpSendOKNg == false) {     //送信失敗時
//                    return;
//                }

                //-- 以上   プリペイド向け決済選択の送信処理
                //-- 以下   プリペイド向け決済情報の送信処理準備

//                _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.SENDING);
//
//                switch(tmpTranType)
//                {
//                    case TransMap.TYPE_SALES :                      //売上
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_PAY);
//                        break;
//                    case TransMap.TYPE_CANCEL :                     //取消
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_PAYREFUND);
//                        break;
//                    case TransMap.TYPE_POINT :                      //ポイント付与
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_POINTADD);
//                        break;
//                    case TransMap.TYPE_POINT_CANCEL :               //ポイント取消
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_POINTREFUND);
//                        break;
//                    case TransMap.TYPE_CACHE_CHARGE :               //現金チャージ
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_CACHECHARGE);
//                        break;
//                    case TransMap.TYPE_CACHE_CHARGE_CANCEL :        //現金チャージ取消
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_CHCHECHARGEREFUND);
//                        break;
//                    case TransMap.TYPE_POINT_CHARGE :               //ポイントチャージ
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_POINTCHARGE);
//                        break;
//                    case TransMap.TYPE_PREPAID_CARDBUY  :           //プリペイド発売
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.PREPAID_CARDBUY);
//                        break;
//                }
                //-- 以上   プリペイド向け決済情報の送信処理準備
            } else {                                                                                    //その他の場合
//                if (_slipData.transCashTogetherAmount > 0) {                                            //分別時の場合
//                    if (tmpSeparationMainSendFL == false) {                                             //分別1度目送信時
//                        if (_slipData.transBrand.equals("クレジット") == true) {                          //クレジットの場合
//                            _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.SENDING);
//                            _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.SEPARATION_CREDITCASH_FIRST);
//                        } else if (_slipData.transBrand.equals("交通系電子マネー") == true) {              //交通系電子マネーの場合
//                            _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.SENDING);
//                            _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.SEPARATION_SUICACASH_FIRST);
//                        } else if (_slipData.transBrand.equals("コード決済") == true) {                  //QR決済の場合
//                            _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.SENDING);
//                            _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.SEPARATION_QRCASH_FIRST);
//                        } else {
//                            _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);
//                            _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);
//                        }
//
//                        _DuplexComm_BlandName = _slipData.transBrand;               //送信するブランド名（送信確認で使用）
//                        _DuplexComm_SlipIDBackup = isSlipDataId;                    //送信するスリップID（送信確認で使用）
//
//                        tmpuseNewSendSystenFL = true;                               //新送信システムを使用
//                    } else {                                                                            //分別2度目送信時
//                        _params.put("pay_separation", 0);               // 分別払い有無
//                        _params.put("input_kingaku", 0);                //入力金額
//                        _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);
//                        _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);
//                    }
//                } else {
//                    _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);
//                    _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.NONE);
//                }
            }

            if (tmpKessaiKakuninFl == false) {                              //通常送信時(決済確認以外)
                _DuplexComm_BlandName = _slipData.transBrand;               //送信するブランド名（送信確認で使用）
                _DuplexComm_SlipIDBackup = isSlipDataId;                    //送信するスリップID（送信確認で使用）
            }

            // パラメータの格納
            _sendData.put("data", _params);

//            if (tmpuseNewSendSystenFL == false)             //新送信システムを使用しない場合
//            {
//                _ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
//            } else {                                        //新送信システムを使用する場合
//                _ifBoxManager.sendFutabaDExt(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
//            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：printTrans->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

//ADD-E BMT S.Oyama 2024/09/3 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/02/10 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  決済データ通知[エラー応答：中止ボタン，スキャンタイムアウト等]（フタバ双方向用）
     * @note   PT750->IM820への決済データ通知 /printdata/v3:print_start
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */

    /******************************************************************************/
    private void sendWsPrintdata_FutabaDErrorAck() {

        Timber.i("[FUTABA-D]sendWsPrintdata_FutabaDErrorAck()  ");

        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        Object alipayPlusCode;
        Object JCoinPayCode; // J-Coin
        Object AEONPayCode; // AEON Pay
        String tmpDateTimeStrConvert;
        try {
            alipayPlusCode = "03";
            JCoinPayCode = "06";
            AEONPayCode = "07";
            _sendData.put("type", "/printdata/v3");
            _sendData.put("cmd", "print_start");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
//            _slipData = DBManager.getSlipDao().getOneById(id);
            _params.put("trans_brand", _slipData.transBrand);
            _params.put("trans_type", _slipData.transType);
            if (_slipData.transBrand.equals("コード決済")) {
                _params.put("trans_type_code", "00");
            } else {
                _params.put("trans_type_code", _slipData.transTypeCode);
            }

            if (_slipData.transBrand.equals("クレジット") == true) {                                             //クレジット系
                _params.put("status", "x59");                                                       //応答ｽﾃｰﾀｽ "x59"で設定
            } else if (_slipData.transBrand.equals("コード決済") == true) {                                      //QR系
                _params.put("status", "x59");                                                       //応答ｽﾃｰﾀｽ "x59"で設定
            } else {                                                                                           //電子マネー系
                _params.put("status", "x59 ");                                                      //応答ｽﾃｰﾀｽ "x59 "で仮設定
            }
            _params.put("trans_result", _slipData.transResult);
            _params.put("trans_result_detail", _slipData.transResultDetail);
            //_params.put("slip_kind", _slipData.slip_kind);                                              //伝票種別指示
            _params.put("print_cnt", _slipData.printCnt);
            _params.put("old_aggregate_order", _slipData.oldAggregateOrder);
            _params.put("encrypt_type", _slipData.encryptType);
            _params.put("cancel_flg", _slipData.cancelFlg);
            _params.put("trans_id", _slipData.transId);
            //_params.put("pay_separation", _slipData.pay_separation);                                    //分別払い
            _params.put("merchant_name", convertSjisString(_slipData.merchantName));
            _params.put("merchant_office", convertSjisString(_slipData.merchantOffice));
            _params.put("merchant_telnumber", _slipData.merchantTelnumber);
            _params.put("car_id", _slipData.carId);
            _params.put("driver_id", _slipData.driverId);
            _params.put("term_id", _slipData.termId);
            _params.put("term_sequence", _slipData.termSequence);
            _params.put("trans_date", _slipData.transDate);
            _params.put("card_company", convertSjisString(_slipData.cardCompany));
            _params.put("card_id_merchant", _slipData.cardIdMerchant);
            _params.put("card_id_customer", _slipData.cardIdCustomer);
            _params.put("card_id_card_company", _slipData.card_id_card_company);                        //カード番号※暗号化対象
            _params.put("card_exp_date", "");
            _params.put("card_exp_date_merchant", _slipData.card_exp_date_merchant);                    //カード有効期限（加盟店）
            _params.put("card_exp_date_card_company", _slipData.card_exp_date_card_company);            //カード有効期限（カード会社）
            _params.put("card_trans_number", _slipData.cardTransNumber);
            _params.put("nanaco_slip_number", _slipData.nanacoSlipNumber);
            _params.put("edy_trans_number", _slipData.edyTransNumber);
            _params.put("slip_number", _slipData.slipNumber);
            _params.put("old_slip_number", _slipData.oldSlipNumber);
            _params.put("auth_id", _slipData.authId);
            _params.put("auth_id_str", "-------");                                      //承認番号（string型）      //メンバauth_id_strは使用しない　元から定義してある「printingAuthId」を使用のこと
            _params.put("commodity_code", _slipData.commodityCode);
            _params.put("installment", _slipData.installment);
            _params.put("point", _slipData.point);
            _params.put("point_grant_type", _slipData.pointGrantType);
            _params.put("point_grant_msg_one", null);
            _params.put("point_grant_msg_two", null);
            _params.put("term_ident_id", _slipData.termIdentId);
            _params.put("trans_amount", _slipData.transAmount);
            _params.put("trans_specified_amount", _slipData.transSpecifiedAmount);
            _params.put("trans_meter_amount", _slipData.transMeterAmount);
            _params.put("trans_adj_amount", _slipData.transAdjAmount);
            _params.put("trans_cash_together_amount", _slipData.transCashTogetherAmount);
            //_params.put("trans_complete_amount", _slipData.transCompleteAmount);                        //支払済み金額
            _params.put("trans_other_amount_one_type", _slipData.transOtherAmountOneType);
            _params.put("trans_other_amount_one", _slipData.transOtherAmountOne);
            _params.put("trans_other_amount_two_type", _slipData.transOtherAmountTwoType);
            _params.put("trans_other_amount_two", _slipData.transOtherAmountTwo);
            _params.put("free_count_one", _slipData.freeCountOne);
            _params.put("free_count_two", _slipData.freeCountTwo);
            _params.put("trans_before_balance", _slipData.transBeforeBalance);
            _params.put("trans_after_balance", _slipData.transAfterBalance);
            _params.put("common_name", _slipData.commonName);
            _params.put("credit_type", _slipData.creditType);
            _params.put("credit_arc", _slipData.creditArc);
            _params.put("credit_aid", _slipData.creditAid);
            _params.put("credit_apl", _slipData.creditApl);
            //_params.put("credit_kid", _slipData.creditKid);                                             //KID
            _params.put("credit_signature_flg", 0);
            _params.put("codetrans_order_id", _slipData.codetransOrderId);
            _params.put("codetrans_pay_type_name", convertSjisString(_slipData.codetransPayTypeName));
            _params.put("cat_dual_type", _slipData.cat_dual_type);                                      //0:CAT型、1:DUAL型
            _params.put("card_seq_no", "  ");                                                      //カードシーケンス番号  SPACE2個
            _params.put("atc", _slipData.atc);                                                          //ATC
            _params.put("rw_id", _slipData.rw_id);                                                      //RWID
            _params.put("sprw_id", _slipData.sprw_id);                                                  //SPRWID
            _params.put("off_on_type", _slipData.off_on_type);                                          //0:オフライン、1:オンライン
            _params.put("card_type", _slipData.card_type);                                              //カード区分
            _params.put("card_id", _slipData.card_id);                                                  //カードID
            _params.put("point_yuko_msg", _slipData.point_yuko_msg);                                    //ポイント利用不可時のメッセージ
            _params.put("point_marchant", _slipData.point_marchant);                                    //加盟店ポイント
            _params.put("point_total", _slipData.point_total);                                          //累計ポイント
            _params.put("point_exp_date", _slipData.point_exp_date);                                    //ポイント有効期限
            _params.put("point_exp", _slipData.point_exp);                                              //期限ポイント
            _params.put("waon_trans_type_code", _slipData.waon_trans_type_code);                        //WAON取引種別コード
            _params.put("card_slip_no", _slipData.card_slip_no);                                        //カード通番
            _params.put("lid", _slipData.lid);                                                          //端末シリアル番号
            _params.put("service_name", _slipData.service_name);                                        //サービス名
            _params.put("card_trans_number_str", _slipData.card_trans_number_str);                      //取引通番
            _params.put("pay_id", _slipData.pay_id);                                                    //支払ID
            _params.put("ic_no", _slipData.ic_no);                                                      //IC通番
            _params.put("old_ic_no", _slipData.old_ic_no);                                              //元IC通番
            _params.put("terminal_no", _slipData.terminal_no);                                          //端末番号
            _params.put("terminal_seq_no", _slipData.terminal_seq_no);                                  //端末通番
            _params.put("unique_id", _slipData.uniqueId);                                               //ユニークID
            _params.put("terminal_id", _slipData.terminal_id);                                          //上位端末ID
            _params.put("edy_seq_no", _slipData.edy_seq_no);                                            //Edy取引通番
            _params.put("input_kingaku", _slipData.input_kingaku);                                      //入力金額
            //_params.put("auth_id_str", _slipData.printingAuthId);
            //setWsPrintdataV2(_params);

            _params.put("trans_type_name", "");  // 取引種別名
            _params.put("slip_kind", 0);  // 伝票種別指示
            _params.put("pay_separation", 0);  // 分別払い有無
            _params.put("credit_kid", _slipData.creditKid);  // KID
            _params.put("trans_complete_amount", _slipData.transCompleteAmount);
            _params.put("input_kingaku", 0);                                      //入力金額
            _params.put("point", _slipData.prepaidAddPoint);
            _params.put("point_total", _slipData.prepaidTotalPoint);
            _params.put("point_exp_date", "FFFFFF");
            _params.put("point_exp", _slipData.prepaidNextExpiredPoint);
            _params.put("service_name", convertSjisString(_slipData.prepaidServiceName));

            _params.put("term_ident_id", _slipData.termId);


            _params.put("term_id", _slipData.termId);
            _params.put("term_ident_id", _slipData.termIdentId);

            //ADD-S BMT S.Oyama 2025/03/8 フタバ双方向向け改修
            _params.put("qr_settlement_namestr", "");                                                    //QR決済名称
            //ADD-E BMT S.Oyama 2025/03/8 フタバ双方向向け改修

            // パラメータの格納
            _sendData.put("data", _params);

            //_ifBoxManager.sendFutabaDExt(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：printTrans->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }
    //ADD-E BMT S.Oyama 2025/02/10 フタバ双方向向け改修

    private void setWsPrintdataV2(JSONObject params) {
        // type：printdata/v2、cmd：print_start で必要なパラメータを設定
        int slip_kind = SlipKind_Customer | SlipKind_Merchant | SlipKind_Receipt;  // お客様控え＋加盟店控え＋領収書
        int pay_separation = 0;
        //ADD-S BMT S.Oyama 2024/10/08 フタバ双方向向け改修
        int tmpInputKingaku = 0;
        //ADD-E BMT S.Oyama 2024/10/08 フタバ双方向向け改修
        try {

            if (_slipData.transCashTogetherAmount > 0) {
                pay_separation = 1;        //7.分別払い    2ビット目：現金(1)　0ビット目：分別払い(1)
            }
            //ADD-S BMT S.Oyama 2024/11/11 フタバ双方向向け改修
            else
            {
                if (_slipData.transBrand.equals("分別チケット") == true ) {                                            //分別チケットの場合
                    if ((_slipData.transOtherAmountOne != null) && (_slipData.transOtherAmountOne > 0))
                    {
                        pay_separation = 1;        //7.分別払い
                        tmpInputKingaku = _slipData.transAmount;
                    }
                }
            }
            //ADD-E BMT S.Oyama 2024/11/06 フタバ双方向向け改修

            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) && _slipData.transTypeCode != null) {
                /*CHG-S k.Fukumitsu  2023/11/30  trans_type_name(取引種別名)SJISに変換して値をセットするように修正　　※取引種別コード　"01"：支払　"10"：支払取消*/
                params.put("trans_type_name", convertSjisString(history_trade_type_name(_slipData.transTypeCode, null)));  // 取引種別名
                /*CHG-E k.Fukumitsu  2023/11/30  trans_type_name(取引種別名)SJISに変換して値をセットするように修正　　※取引種別コード　"01"：支払　"10"：支払取消*/
            } else {
                params.put("trans_type_name", "");  // 取引種別名
            }

            if (_slipData.transType == 1) {

                slip_kind &= ~SlipKind_Receipt;     //ビット反転
            }

            params.put("slip_kind", slip_kind);  // 伝票種別指示
            params.put("pay_separation", pay_separation);  // 分別払い有無
            params.put("credit_kid", _slipData.creditKid);  // KID
            params.put("trans_complete_amount", _slipData.transCompleteAmount);
            //ADD-S BMT S.Oyama 2024/10/08 フタバ双方向向け改修
            params.put("input_kingaku", tmpInputKingaku);                                      //入力金額
            //ADD-E BMT S.Oyama 2024/10/08 フタバ双方向向け改修
            //ADD-S BMT S.Oyama 2024/12/17 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
                if (_slipData.transBrand.equals("プリペイド") == true) {                                 //プリペイド時特殊処理の場合

                    String tmpPointExpDateY = "FFFF";
                    String tmpPointExpDateM = "FF";
                    if (_slipData.prepaidNextExpired.length() == 7)         //YYYY/MM 仕様
                    {
                        tmpPointExpDateY = _slipData.prepaidNextExpired.substring(0, 4);
                        tmpPointExpDateM = _slipData.prepaidNextExpired.substring(5);
                    }

                    params.put("point", _slipData.prepaidAddPoint);
                    params.put("point_total", _slipData.prepaidTotalPoint);
                    params.put("point_exp_date", tmpPointExpDateY + tmpPointExpDateM);
                    params.put("point_exp", _slipData.prepaidNextExpiredPoint);
                    params.put("service_name", convertSjisString(_slipData.prepaidServiceName));

                    params.put("term_ident_id", _slipData.termId);

                }
            }
            //ADD-E BMT S.Oyama 2024/12/17 フタバ双方向向け改修
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：setWsPrintdataV2->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    private void sendWsPrintRestart() {
        // メーターへ印刷再開を送信
        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        try {
            _sendData.put("type", "/printdata/v1");
            _sendData.put("cmd", "print_restart");
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
            //_ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintRestart->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    // 取引伝票印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTrans(int id, int SlipCopy) {
        isSlipDataId = id;
        print_init();
        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _slipData = DBManager.getSlipDao().getOneById(isSlipDataId);
            }
        });
        thread.start();

        try {
            thread.join();

            if (_slipData != null) {
                isTransResult = _slipData.transResult;
                isTransType = _slipData.transType;
                if (_slipData.printCnt > 0) isRePrinter = true;

                if (isTransResult != null) {
                    if (isTransResult == PrinterConst.TransResult_OK) {
                        // 取引結果：成功
                        if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D)) {
                            // ヤザキLT27双方向
                            isPT750_Print = false;
                            setPrintData_trans_ok(SlipCopy);
                            // 双方向用にデータをWS送信
                            if (SlipCopy == PrinterConst.SlipCopy_Merchant) {
                                /* CHG-S N.Sasaki 2024/04/18 ヤザキLT27双方向 LANSポイント対応 */
//                                sendWsPrintdata();
                                // ブランド名
                                if (_slipData.transBrand != null) {
                                    // 和多利だったら専用のレシート
                                    if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_watari))) {
                                        sendWsPrintdata_watari();
                                    } else {
                                        sendWsPrintdata();
                                    }
                                } else {
                                    sendWsPrintdata();
                                }
                                /* CHG-E N.Sasaki 2024/04/18 ヤザキLT27双方向 LANSポイント対応 */
                            } else {
                                sendWsPrintNext();
                            }
                        } else if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                            // オカベMS70双方向
                            isPT750_Print = false;
                            setPrintData_trans_ok(SlipCopy);
                            if (PrinterManager.getInstance().getPrintStatus() == PrinterConst.PrintStatus_PAPERLACKING) {
                                // 紙切れ後の印刷再開をWS送信
                                sendWsPrintRestart();
                            } else {
                                // 双方向用にデータをWS送信
                                if (SlipCopy == PrinterConst.SlipCopy_Merchant) {
                                    sendWsPrintdata_okabe();
                                } else {
                                    sendWsPrintNext();  /*Add k.Fukumitsu  2024/1/12  レシート印字2枚目以降の継続印刷要求*/
                                }
                            }
                            //ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                        } else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) && (_slipData.transTypeCode == null || !_slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_SALES)) && (_slipData.transTypeCode == null || !_slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_CANCEL))) {
                            // フタバ双方向
                            isPT750_Print = false;
                            setPrintData_trans_ok(SlipCopy);
                            if (PrinterManager.getInstance().getPrintStatus() == PrinterConst.PrintStatus_PAPERLACKING) {
                                // 紙切れ後の印刷再開をWS送信
                                sendWsPrintRestart();
                            } else {
                                // 双方向用にデータをWS送信
                                if (SlipCopy == PrinterConst.SlipCopy_Merchant) {
                                    if (!isRePrinter) {
                                        sendWsPrintdata_FutabaD(false, "", false);
                                    } else {
                                        sendWsReprintdata_FutabaD();
                                    }
                                } else {
                                    sendWsPrintNext();  /*Add k.Fukumitsu  2024/1/12  レシート印字2枚目以降の継続印刷要求*/
                                }
                            }
                            //ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                        } else {
                            setPrintData_trans_ok(SlipCopy);
                        }
                    } else if (isTransResult == PrinterConst.TransResult_UnFinished) {
                        // 取引結果：未了
                        if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) || _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay))) {
                            /* iD、QUICPay */
                            // 処理未了伝票の印刷対象外
                            PrintDataError();
                        } else {
                            //CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                            //if(IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                            if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
                                //CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                                isPT750_Print = false;
                                setPrintData_trans_unfinished(SlipCopy);
                                // 双方向用にデータをWS送信
                                if (!_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                                    if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                                        sendWsPrintdata_okabe();
                                    }
                                    //ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                                    else if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) && !_slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_SALES) && !_slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_CANCEL)) {
                                        sendWsPrintdata_FutabaD(false, "", false);
                                        //ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                                    } else {
                                        sendWsPrintdata();
                                    }
                                } else {
                                    // 印字無しで終わらせる
                                    PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, PrinterConst.TransResult_UnFinished, Printer.PRINTER_OK, isTransType);
                                    Printing_end();
                                }
                            } else {
                                setPrintData_trans_unfinished(SlipCopy);
                            }
                        }
                    } else {
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：printTrans->_slipData.transResult <%d>", _printDataRes.getString(R.string.printLog_printDataError), isTransResult);
                        PrintDataError();
                    }
                } else {
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTrans->_slipData.transResult <%d>", _printDataRes.getString(R.string.printLog_printDataError), isTransResult);
                    PrintDataError();
                }
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTrans->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
                PrintDataError();
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：printTrans->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    //ADD-S BMT S.Oyama 2024/10/04 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820への送信処理：分別チケットおよびプリペイド向け(フタバD)
     * @note　 820への送信処理：分別チケットおよびプリペイド向け 伝票の発生はなし
     * @param [in] int id       SlipID
     * @retval なし
     * @return　なし
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTransFutabaD(int id) {
        Timber.i("[FUTABA-D]printTransFutabaD() slipid:%d", id);

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)           // フタバD以外は処理しない
        {
            Timber.e("[FUTABA-D]printTransFutabaD():not FUTABA-D Mode");
            return;
        }

        isSlipDataId = id;
        print_init();
        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _slipData = DBManager.getSlipDao().getOneById(isSlipDataId);
            }
        });
        thread.start();

        try {
            thread.join();
            if (_slipData != null) {
                isTransResult = _slipData.transResult;
                sendWsPrintdata_FutabaD(false, "", false);              //820への送信処理　フタバD：分別チケットおよびプリペイド向け
            } else {
                // 印刷データ異常（想定外）
                Timber.e("[FUTABA-D]%s：printTransFutabaD->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
                //PrintDataError();
            }
        } catch (Exception e) {
            Timber.e("[FUTABA-D]%s：printTransFutabaD->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            //PrintDataError();
            e.printStackTrace();
        }
    }
    //ADD-E BMT S.Oyama 2024/10/04 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/11/12 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820への送信処理：分別クレカ　SUICA　QR　2度目の送信向け(フタバD)
     * @note　 820への送信処理：分別クレカ　SUICA　QR　2度目の送信向け
     * @param [in] int id       SlipID
     * @retval なし
     * @return　なし
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTransFutabaD_Separation2ndSend() {
        Timber.i("[FUTABA-D]printTransFutabaD_Separation2ndSend() slipid:%d", isSlipDataId);

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)           // フタバD以外は処理しない
        {
            Timber.e("[FUTABA-D]printTransFutabaD():not FUTABA-D Mode");
            return;
        }

        print_init();
        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _slipData = DBManager.getSlipDao().getOneById(isSlipDataId);
            }
        });
        thread.start();

        try {
            thread.join();
            if (_slipData != null) {
                isTransResult = _slipData.transResult;
                sendWsPrintdata_FutabaD(false, "", true);              //820への送信処理　フタバD：分別クレカ　SUIA　QR　2度目の送信向け
            } else {
                // 印刷データ異常（想定外）
                Timber.e("[FUTABA-D]%s：printTransFutabaD->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
                //PrintDataError();
            }
        } catch (Exception e) {
            Timber.e("[FUTABA-D]%s：printTransFutabaD->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            //PrintDataError();
            e.printStackTrace();
        }
    }
    //ADD-S BMT S.Oyama 2024/11/12 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/10/15 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820への送信処理：決済確認送信(フタバD)
     * @note　 820への送信処理：決済確認送信 伝票の発生はなし
     * @param なし
     * @retval なし
     * @return　なし
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTransFutabaD_KessaiKakunin() {

        Timber.i("[FUTABA-D]printTransFutabaD_KessaiKakunin()");

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)           // フタバD以外は処理しない
        {
            Timber.e("[FUTABA-D]printTransFutabaD_KessaiKakunin():not FUTABA-D Mode");
            return;
        }

        if ((_DuplexComm_SlipIDBackup == 0) || (_DuplexComm_BlandName.equals("") == true)) {                //通信が完了している場合
            // 別スレッド：伝票印刷関連データ取得
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _slipData = DBManager.getSlipDao().getLatestOne();           //最後の取引を取得
                }
            });
            thread.start();

            try {
                thread.join();
                if (_slipData == null) {
                    // 印刷データ異常（想定外）
                    Timber.e("[FUTABA-D]%s：printTransFutabaD_KessaiKakunin(1)->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
                    return;
                }

                isTransResult = _slipData.transResult;
                _slipData.transBrand = "決済確認";
                _slipData.status = "0001";          // 売上データなし
                sendWsPrintdata_FutabaD(true, "000", false);              //820への送信処理　フタバD：決済確認

            } catch (Exception e) {
                Timber.e("[FUTABA-D]%s：printTransFutabaD->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
                //PrintDataError();
                e.printStackTrace();
            }

            return;
        }

        //print_init();
        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _slipData = DBManager.getSlipDao().getOneById(_DuplexComm_SlipIDBackup);           //送信中スリップID
            }
        });
        thread.start();

        try {
            thread.join();
            if (_slipData == null) {
                // 印刷データ異常（想定外）
                Timber.e("[FUTABA-D]%s：printTransFutabaD_KessaiKakunin(2)->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
                return;
            }

            String tmpStatusCode = "";
            if ((_slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_suica)) == true) ||
                    (_slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_id)) == true) ||
                    (_slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_qp)) == true) ||
                    (_slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_waon)) == true))              //SUICA, iD, QuicPay, WAON時
            {
                _slipData.transBrand = "決済確認";
                _slipData.status = "0000";          // 売上データあり

                if ((_slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_suica)) == true))     //SUICA時
                {
                    tmpStatusCode = "923";
                } else if (_slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_id)) == true)           //iD時
                {
                    tmpStatusCode = "920";
                } else if (_slipData.transBrand.equals(MainApplication.getInstance().getString(R.string.money_brand_qp)) == true)       //QuicPay時
                {
                    tmpStatusCode = "921";
                } else {                                                            //WAON時
                    tmpStatusCode = "928";
                }
            } else {
                _slipData.transBrand = "決済確認";
                _slipData.status = "0001";          // 売上データなし
                tmpStatusCode = "000";
            }

            sendWsPrintdata_FutabaD(true, tmpStatusCode, false);              //820への送信処理　フタバD：決済確認

        } catch (Exception e) {
            Timber.e("[FUTABA-D]%s：printTransFutabaD->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            //PrintDataError();
            e.printStackTrace();
        }
    }
    //ADD-E BMT S.Oyama 2024/10/04 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/02/10 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820への送信処理：決済取り消し情報発砲(フタバD)
     * @note　 820への送信処理：決済取り消し情報発砲 伝票の発生はなし
     * @param なし
     * @retval なし
     * @return　なし
     * @private
     */

    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTransFutabaD_SettlementAbort(int tmpPhase, int tmpSettlementMode) {
        Timber.i("[FUTABA-D]printTransFutabaD_SettlementAbort()");

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)           // フタバD以外は処理しない
        {
            Timber.e("[FUTABA-D]printTransFutabaD_SettlementAbort():not FUTABA-D Mode");
            return;
        }

        Date exDate = new Date();   // 取引時間
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        String payTime = dateFmt.format(exDate);

        try {
            SlipData tmpslipData = new SlipData(payTime);              // 伝票データ初期化(中身空のデータ)
            switch (MainApplication.getInstance().getBusinessType()) {
                case PAYMENT:
                    tmpslipData.transType = TransMap.TYPE_SALES;      //取引種別：決済
                    break;
                case REFUND:
                    tmpslipData.transType = TransMap.TYPE_CANCEL;      //取引種別：返金
                    break;
                default:
                    Timber.e("[FUTABA-D]printTransFutabaD_SettlementAbort():not Settlment Type");
                    return;
            }

//            switch(tmpPhase) {
//                case IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_CREDIT:
//                    tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_credit);
//                    break;
//                case IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY:
//                    switch(tmpSettlementMode)
//                    {
//                        case IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY:
//                            tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_edy);
//                            break;
//                        case IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_ID:
//                            tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_id);
//                            break;
//                        case IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO:
//                            tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_nanaco);
//                            break;
//                        case IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QUICPAY:
//                            tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_qp);
//                            break;
//                        case IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA:
//                            tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_suica);
//                            break;
//                        case IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_WAON:
//                            tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_waon);
//                            break;
//                        default:
//                            Timber.e("[FUTABA-D]%s：printTransFutabaD_SettlementAbort->tmpSettlementMode <%s>", _printDataRes.getString(R.string.printLog_printDataError), tmpSettlementMode);
//                            return;
//                    }
//                    break;
//                case IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_QR:
//                    tmpslipData.transBrand = MainApplication.getInstance().getString(R.string.money_brand_codetrans);
//                    break;
//                default:
//                    Timber.e("[FUTABA-D]%s：printTransFutabaD_SettlementAbort->tmpPhase <%s>", _printDataRes.getString(R.string.printLog_printDataError), tmpPhase);
//                    return;
//            }
                                                         //0123456789A123456789B12345    //*** 820側文字列変数処理が単純にmemcpy左詰めしている結果，構造体にゴミが入るため，その抑止として空白や0を入れる
            tmpslipData.termId                          = "                ";            // 端末ID系は空白15個入れる
            tmpslipData.termIdentId                     = "                ";            // 端末識別ID系は空白15個入れる
            tmpslipData.commodityCode                   = "                ";            // 商品コードは空白16個入れる
            tmpslipData.cardExpDate                     = "        ";                    //カード有効期限
            tmpslipData.card_exp_date_merchant          = "        ";                    //カード有効期限（加盟店控）
            tmpslipData.card_exp_date_card_company      = "        ";                    //カード有効期限（カード会社控）
            tmpslipData.card_seq_no                     = "        ";                    //カード番号
            tmpslipData.atc                             = "        ";                    //ATC
            tmpslipData.rw_id                           = "                    ";        //RWID
            tmpslipData.card_id                         = "                    ";        //カードID
            tmpslipData.point_yuko_msg                  = "0";                           //ポイント利用不可時メッセージ番号
            tmpslipData.point_total                     = "00000000";                    //ポイント合計
            tmpslipData.point_exp_date                  = "        ";                    //ポイント有効期限
            tmpslipData.cardIdMerchant                  = "                    ";        //カード番号（加盟店控）nanacoはマスクされていない番号がないので処理未了時もマスクされたものを保存
            tmpslipData.cardIdCustomer                  = "                    ";        //カード番号(お客様控え用)
            tmpslipData.terminal_id                     = "                ";            //端末ID(EDY)

            _slipData = tmpslipData;                                 // 伝票データをセット

            sendWsPrintdata_FutabaDErrorAck();              //820への送信処理　フタバD：取り消しキー代替エラー応答
        } catch (Exception e) {
            Timber.e("[FUTABA-D]%s：printTransFutabaD->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
    }
    //ADD-E BMT S.Oyama 2025/02/10 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean printPrepaidTrans(int[] ids, int SlipCopy) {
        print_init();

        List<SlipData> slipDataList = new ArrayList<>();

        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < ids.length; i++) {
                    SlipData slipData = DBManager.getSlipDao().getOneById(ids[i]);

                    synchronized (slipDataList) {
                        slipDataList.add(slipData);
                    }
                }
            }
        });
        thread.start();

        try {
            // スレッドが完了するのを待つ
            thread.join();
            _slipDataList = slipDataList;
            // 共通部は最新の1件を使うことにする　※チャージ＋支払の場合に支払の伝票を印字するため
            _slipData = slipDataList.get(slipDataList.size()-1);

            if (_slipData.printCnt > 0) isRePrinter = true;

            //ADD-S BMT S.Oyama 2025/03/21 フタバ双方向向け改修
            //if ((_slipData.transBrand.equals("プリペイド") == true) &&
            //        (_slipData.transType == TransMap.TYPE_SALES ) )        // プリペイドの場合 かつ　売上の場合
            //{
                //Amount.getMeterCharge()
                //int tmpMeterAmount = Amount.getFixedAmount();           //メータ側でスキャン中に金額変更されてないか確認し，変更されていたら，エラーを出して抜ける
                //if (_slipData.transAmount != tmpMeterAmount) {
                //    PrinterManager.getInstance().prepaid_MeterAmountChangeErr();
                //    return false;
                //}
            //}
            //ADD-E BMT S.Oyama 2025/03/21 フタバ双方向向け改修
            isTransResult = _slipData.transResult;

            // todo: メータ連携時の条件分岐

            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {              //フタバD時はprintTrans()を呼び出し，820通信を実施
                //ADD-S BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                PrinterManager.getInstance().setprepaid_print_trans_fix_IsSlipDataId(_slipData.id);
                //ADD-E BMT S.Oyama 2025/02/18 フタバ双方向向け改修
                printTrans(_slipData.id, SlipCopy);
                if (_slipData.transType == TransMap.TYPE_PREPAID_CARDBUY) {
                    // カード発売の時はtrueで返す
                    return true;
                }
            } else if (_slipData.transType != TransMap.TYPE_PREPAID_CARDBUY) {
                setPrintData_trans_ok(SlipCopy);
            } else {
                // メーター連動していない場合、カード発売では何もしないので終了処理だけやっとく
                PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, PrinterConst.TransResult_Other, Printer.PRINTER_OK, 0);
                Printing_end();
                return true;
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：printPrepaidTrans->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
        return false;
    }

    //ADD-S BMT S.Oyama 2025/03/25 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  プリペイドスリップ群中，ターゲットの取引種別を返す（フタバ双方向用）
     * @note   プリペイドスリップ群中，ターゲットの取引種別を返す
     * @param [in] int[] slip_data主キー
     * @retval なし
     * @return　プリペイド取引モード
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public int printPrepaidTransGetTargetJobMode(int[] ids) {
        int tmpResult = -1;  //0はTranseMap.TYPE_SALESで使っているので指定しないこと

        List<SlipData> slipDataList = new ArrayList<>();

        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < ids.length; i++) {
                    SlipData slipData = DBManager.getSlipDao().getOneById(ids[i]);

                    synchronized (slipDataList) {
                        slipDataList.add(slipData);
                    }
                }
            }
        });
        thread.start();

        try {
            // スレッドが完了するのを待つ
            thread.join();

            if (slipDataList.size() == 0)
            {
                return tmpResult;
            }

            // 共通部は最新の1件を使うことにする　※チャージ＋支払の場合に支払の伝票を印字するため
            SlipData tmpslipData = slipDataList.get(slipDataList.size()-1);

            if (tmpslipData.transBrand.equals("プリペイド") == true) {          //プリペイドの場合
                tmpResult = tmpslipData.transType;              //transe_typeにプリペイド処理の挙動が記載される
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：printPrepaidTransGetTargetJObMode->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }

        return tmpResult;
    }
    //ADD-E BMT S.Oyama 2025/03/25 フタバ双方向向け改修

    // 取引正常伝票（印刷データセット）
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPrintData_trans_ok(int copy_type) {

        // ブランド名
        if (_slipData.transBrand != null) {
            // 和多利だったら専用のレシート
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_watari))) {
                setPrintData_watari_ok(copy_type);
                return;
            }

            // プリペイドは専用のレシート
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.prepaid_brand))) {
                setPrintData_prepaid_ok(copy_type);
                return;
            }

            if(_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))){
                /* クレジット */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_credit), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica))) {
                /* 交通系電子マネー */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_suica), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAON */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_waon), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                /* 楽天Edy */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_edy_rakute), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanaco */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_nanaco), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id))) {
                /* iD */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_id), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay))) {
                /* QUICPay */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_quicpay), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICA */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_okica), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済 */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_codetrans), PaintSize_Big);
            } else {
                setAlign_Mid(null, PaintSize_Big);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transBrand <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand);
            }
            Log_BrandName = _slipData.transBrand;
        } else {
            setAlign_Mid(null, PaintSize_Big);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transBrand <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand);
        }

        // デモモード確認
        PrintDemoCheck();

        // 票名
        if (_slipData.transType != null && _slipData.printCnt != null) {
            switch (_slipData.transType) {
                case 0: // 取引種別：売上
                    String print_title = _printDataRes.getString(R.string.print_payment_slip);  // デフォルトのタイトル名

                    // 加盟店控えについてはPOSサービスのアクティベート状態とマスタデータに応じて印字内容を変更する
                    if (copy_type == PrinterConst.SlipCopy_Merchant) {
                        if (AppPreference.isPosTransaction()) {
                            // マスタデータが未設定の場合に印字するタイトル「取引明細書」を設定
                            print_title = _printDataRes.getString(R.string.print_detail_statement);

                            Thread thread = new Thread(() -> {
                                _serviceFunctionData = DBManager.getServiceFunctionDao().getServiceFunction();
                            });
                            thread.start();
                            try {
                                thread.join();
                                if (_serviceFunctionData != null) {
                                    Timber.tag("printer").d("slip_title = %s", _serviceFunctionData.slip_title);
                                    // マスタに登録されているタイトルが空文字でない場合
                                    if (!_serviceFunctionData.slip_title.isEmpty()) {
                                        print_title = _serviceFunctionData.slip_title;
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (_slipData.printCnt > 0) {
                        /* 売上票[再] */
                        setAlign_Mid(print_title + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        Log_SlipName = print_title + _printDataRes.getString(R.string.print_slip_again);

                    } else {
                        /* 売上票 */
                        setAlign_Mid(print_title, PaintSize_Medium);
                        Log_SlipName = print_title;
                    }
                    break;
                case 1: // 取引種別：取消
                    if (_slipData.printCnt > 0) {
                        /* 取消票[再] */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_cancel_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_cancel_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* 取消票 */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_cancel_slip), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_cancel_slip);
                    }
                    break;
                case 4: // 取引種別：チャージ
                    if (_slipData.printCnt > 0) {
                        /* チャージ票[再] */
                        setAlign_Mid(_printDataRes.getString(R.string.print_charge_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_charge_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* チャージ票 */
                        setAlign_Mid(_printDataRes.getString(R.string.print_charge_slip), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_charge_slip);
                    }
                    break;
                default:
                    setAlign_Mid(null, PaintSize_Medium);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                    break;
            }
        } else {
            setAlign_Mid(null, PaintSize_Medium);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transType <%d> _slipData.printCnt <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType, _slipData.printCnt);
        }

        // 控名
        if (copy_type == PrinterConst.SlipCopy_Merchant) {
            /* 加盟店控え */
            setAlign_Mid(_printDataRes.getString(R.string.print_merchant), PaintSize_Normal);
            Log_CopyTypeName = _printDataRes.getString(R.string.print_merchant);
        } else if (copy_type == PrinterConst.SlipCopy_Customer) {
            /* お客様控え */
            setAlign_Mid(_printDataRes.getString(R.string.print_customer), PaintSize_Normal);
            Log_CopyTypeName = _printDataRes.getString(R.string.print_customer);
        } else {
            // その他※必要な場合は追加
        }

        // 端末情報（加盟店名、営業所名・号機番号（車番）、電話番号、係員番号、機器番号、機器通番）
        setTermInfo();

        // 取引日時
        if (_slipData.transDate != null && _slipData.transDate.length() == 19) {
            setAlign_LR(_slipData.transDate.substring(0, 10), _slipData.transDate.substring(11, 19), PaintSize_Normal);
        } else {
            setAlign_LR(null, null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transDate <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transDate);
        }

        // カード会社
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))) {
                /* クレジットのみ */
                if (_slipData.cardCompany != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_company), _slipData.cardCompany, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_company), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.cardCompany <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.cardCompany);
                }
            }
        }

        // カード番号
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* クレジット、交通系電子マネー、WAON、楽天Edy、nanaco、iD、QUICPay、OKICA */
                if (copy_type == PrinterConst.SlipCopy_Merchant) {
                    /* 加盟店控え */
                    if (_slipData.cardIdMerchant != null) {
                        setAlign_LR(_printDataRes.getString(R.string.print_card_id), card_id_separated(_slipData.cardIdMerchant), PaintSize_Normal);
                    } else {
                        setAlign_LR(_printDataRes.getString(R.string.print_card_id), null, PaintSize_Normal);
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.cardIdMerchant <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.cardIdMerchant);
                    }
                } else if (copy_type == PrinterConst.SlipCopy_Customer) {
                    /* お客様控え */
                    if (_slipData.cardIdCustomer != null) {
                        setAlign_LR(_printDataRes.getString(R.string.print_card_id), card_id_separated(_slipData.cardIdCustomer), PaintSize_Normal);
                    } else {
                        setAlign_LR(_printDataRes.getString(R.string.print_card_id), null, PaintSize_Normal);
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.cardIdCustomer <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.cardIdCustomer);
                    }
                } else {
                    // その他※必要な場合は追加
                }
            }
        }

        // カード通番
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAONの場合 */
                if (_slipData.cardTransNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_sequence_number), String.valueOf(_slipData.cardTransNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_sequence_number), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.cardTransNumber <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.cardTransNumber);
                }
            }
        }

        // 有効期限
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit)) || _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id))) {
                /* クレジット、iD */
                if (_slipData.cardExpDate != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_exp_date), _slipData.cardExpDate, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_exp_date), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.cardExpDate <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.cardExpDate);
                }
            }
        }

        // 伝票番号
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* クレジット、WAON、iD、QUICPay、OKICAの場合 */
                if (_slipData.slipNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), String.valueOf(_slipData.slipNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.slipNumber <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.slipNumber);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.codetransOrderId != null && _slipData.codetransOrderId.length() == 20) {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), _slipData.codetransOrderId, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.codetransOrderId <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.codetransOrderId);
                }
            }
        }

        // 取消番号（元伝票番号）※取消対象
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* クレジット、WAON、OKICAの場合 */
                if (_slipData.transType != null) {
                    if (_slipData.transType == 1) {
                        // 取消時
                        if (_slipData.oldSlipNumber != null) {
                            setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), String.valueOf(_slipData.oldSlipNumber), PaintSize_Normal);
                        } else {
                            setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.oldSlipNumber <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.oldSlipNumber);
                        }
                    }
                }
            }
        }

        // 承認番号
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))) {
                /* クレジット */
                if (_slipData.printingAuthId != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_auth_id), _slipData.printingAuthId, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_auth_id), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.printingAuthId null", _printDataRes.getString(R.string.printLog_printDataError));
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id))) {
                /* iD※オンラインの場合のみ */
                if (_slipData.printingAuthId != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_auth_id), _slipData.printingAuthId, PaintSize_Normal);
                }
            }
        }

        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))) {
                /* クレジット */
                // 取引内容
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            setAlign_LR(_printDataRes.getString(R.string.print_trans_detail), _printDataRes.getString(R.string.print_trans_payment), PaintSize_Normal);
                            break;
                        case 1: // 取引種別：取消
                            setAlign_LR(_printDataRes.getString(R.string.print_trans_detail), _printDataRes.getString(R.string.print_trans_cancel), PaintSize_Normal);
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_trans_detail), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_detail), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }

                // 支払区分
                if (_slipData.installment != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_installment), _slipData.installment, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_installment), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.installment <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.installment);
                }

                // 商品区分
                if (_slipData.commodityCode != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_commodity_code), _slipData.commodityCode.replaceFirst("^0+", ""), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_commodity_code), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.commodityCode <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.commodityCode);
                }

                // IC/MS/CL
                if (_slipData.creditType != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_credit_type), _slipData.creditType, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_credit_type), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.creditType <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.creditType);
                }

                // ICCデータ（ICチップ取引）
                if (_slipData.creditType != null) {
                    if (_slipData.creditType.equals(_printDataRes.getString(R.string.print_credit_ic))
                            || _slipData.creditType.equals(_printDataRes.getString(R.string.print_credit_cl))) {
                        // ARC
                        if (_slipData.creditArc != null) {
                            setAlign_LR(_printDataRes.getString(R.string.print_credit_arc), _slipData.creditArc, PaintSize_Normal);
                        } else {
                            setAlign_LR(_printDataRes.getString(R.string.print_credit_arc), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.creditArc <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.creditArc);
                        }

                        // AID
                        if (_slipData.creditAid != null) {
                            setAlign_LR(_printDataRes.getString(R.string.print_credit_aid), _slipData.creditAid, PaintSize_Normal);
                        } else {
                            setAlign_LR(_printDataRes.getString(R.string.print_credit_aid), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.creditAid <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.creditAid);
                        }

                        // APL
                        if (_slipData.creditApl != null) {
                            setAlign_LR(_printDataRes.getString(R.string.print_credit_apl), _slipData.creditApl, PaintSize_Normal);
                        } else {
                            setAlign_LR(_printDataRes.getString(R.string.print_credit_apl), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.creditApl <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.creditApl);
                        }
                    }
                }
            }
        }

        // 処理通番
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id))) {
                /* iD※オンラインの場合のみ */
                if (_slipData.authSequenceNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_auth_sequence_number), String.valueOf(_slipData.authSequenceNumber), PaintSize_Normal);
                }
            }
        }

        // 決済種別
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) || _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay))) {
                /* iD、QUICPayの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 種別：売上
                            setAlign_LR(_printDataRes.getString(R.string.print_trans_type), _printDataRes.getString(R.string.print_trans_payment), PaintSize_Normal);
                            break;
                        case 1: // 種別：取消
                            setAlign_LR(_printDataRes.getString(R.string.print_trans_type), _printDataRes.getString(R.string.print_trans_cancel), PaintSize_Normal);
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_trans_type), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.codetransPayTypeName != null) {
                    if (_slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Wechat)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Alipay)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.Docomo)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.auPAY)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.PayPay)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.LINEPay)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.RakutenPay)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.GinkoPay)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.merpay)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay)) ||
                            _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.AlipayPlus))) {

                        setAlign_LR(_printDataRes.getString(R.string.print_trans_type), _slipData.codetransPayTypeName, PaintSize_Normal);
                    } else {
                        setAlign_LR(_printDataRes.getString(R.string.print_trans_type), null, PaintSize_Normal);
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.codetransPayTypeName <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.codetransPayTypeName);
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.codetransPayTypeName <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.codetransPayTypeName);
                }
            }
        }

        // 取引通番
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                /* 交通系電子マネー、楽天Edyの場合 */
                if (_slipData.cardTransNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), String.valueOf(_slipData.cardTransNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.cardTransNumber <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.cardTransNumber);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanacoの場合、「取引通番」に伝票番号の値を設定（JREM指摘事項）*/
                if (_slipData.nanacoSlipNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), String.valueOf(_slipData.nanacoSlipNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.nanacoSlipNumber <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.nanacoSlipNumber);
                }
            }
        }

        // 端末番号
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay))) {
                /* 交通系電子マネー、WAON、楽天Edy、nanaco、iD、QUICPayの場合 */
                if (_slipData.termIdentId != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), _slipData.termIdentId, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.termIdentId <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.termIdentId);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICAの場合 */
                if (_slipData.termIdentId != null && _slipData.termIdentId.length() == 17) {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_term_ident_id), _slipData.termIdentId, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_term_ident_id), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.termIdentId <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.termIdentId);
                }
            }
        }

        // ユニークID
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanaco */
                if (_slipData.commonName != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_common_name), _slipData.commonName, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_common_name), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.commonName <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.commonName);
                }
            }
        }

        // 決済内容
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 支払
                            setAlign_LR(_printDataRes.getString(R.string.print_codetrans_detail), _printDataRes.getString(R.string.print_codetrans_payment), PaintSize_Normal);
                            break;
                        case 1: // 取消
                            setAlign_LR(_printDataRes.getString(R.string.print_codetrans_detail), _printDataRes.getString(R.string.print_codetrans_cancel), PaintSize_Normal);
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_codetrans_detail), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            break;
                    }
                }
            }
        }
        /*インボイス対応*/
        //運賃表示
        if (_slipData.transAmount != null && Tax_FareOutputConfirm() && AppPreference.judgeInvoice(false) && !AppPreference.settlementPosChk()) {
            setAlign_LR(_printDataRes.getString(R.string.print_total_fare), trans_amount(_slipData.transAmount + _slipData.transCashTogetherAmount), PaintSize_Normal);
        } else {
            // 印字データ対象外
            Timber.tag("Printer").i("%s：setPrintData_trans_ok->_slipData.transAmount <%d> _slipData.transCashTogetherAmount <%d>", _printDataRes.getString(R.string.printLog_printfareError), _slipData.transAmount, _slipData.transCashTogetherAmount);
        }
        // 合計金額 or 入金金額
        if(_slipData.transAmount != null && _slipData.transType == 4) {
            /* チャージの場合 */
            setAlign_LR(_printDataRes.getString(R.string.print_charge_amount), trans_amount(_slipData.transAmount), PaintSize_Big);
        }else if(_slipData.transAmount != null && _slipData.transCashTogetherAmount != null){
            /* 現金併用決済の場合 */
            setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_slipData.transAmount + _slipData.transCashTogetherAmount), PaintSize_Big);
        } else if (_slipData.transAmount != null) {
            /* 通常決済の場合 */
            setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_slipData.transAmount), PaintSize_Big);
        } else {
            setAlign_LR(_printDataRes.getString(R.string.print_total_amount), null, PaintSize_Big);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transAmount <%d> _slipData.transCashTogetherAmount <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAmount, _slipData.transCashTogetherAmount);
        }
        /*インボイス対応*/
        // 対象消費税
        if ((Tax_FareOutputConfirm() && AppPreference.judgeInvoice(false)) && !AppPreference.settlementPosChk()) {
            String taxRate = AppPreference.getreceiptTax();
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_tax_h) + taxRate + _printDataRes.getString(R.string.print_invoice_tax_e), null, PaintSize_Normal);
        }
        Log_InvoiceTax = AppPreference.getreceiptTax();

        // 増減額
        if (_slipData.transType != null) {
            if (_slipData.transType == 0) {
                // 支払時のみ
                if (_slipData.transAdjAmount != null && _slipData.transAdjAmount != 0) {
                    /* 増減額あり*/
                    setAlign_LR(_printDataRes.getStringArray(R.array.print_adj_amount)[0], adj_amount(_slipData.transAdjAmount) + _printDataRes.getStringArray(R.array.print_adj_amount)[1], PaintSize_Normal);
                }
            }
        }

        if(_slipData.transBrand != null){
            if(_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* 交通系電子マネー、WAON、楽天Edy、nanaco、OKICAの場合 */
                // 実線ライン
                setLine();
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.codetransPayTypeName != null && _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                    /* QUOカードPayの場合のみ */
                    // 実線ライン
                    setLine();
                }
            }
        }

        // 現金
        if (_slipData.transCashTogetherAmount != null && _slipData.transCashTogetherAmount != 0) {
            // 現金額
            setAlign_LR(_printDataRes.getString(R.string.print_trans_cash_together_amount), trans_amount(_slipData.transCashTogetherAmount), PaintSize_Normal);
        }

        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit))) {
                /* クレジットの場合 */
                // 改行
                setLF(1, PaintSize_Normal);

                // ご署名欄
                if (copy_type == PrinterConst.SlipCopy_Merchant) {
                    /* 加盟店控え時のみ */
                    if (_slipData.creditSignatureFlg != null) {
                        switch (_slipData.creditSignatureFlg) {
                            case 0: // サイン省略
                                setLine_fill_in();
                                // ご署名
                                setAlign_Mid(_printDataRes.getString(R.string.print_credit_signature), PaintSize_Normal);
                                setLine_fill_in();
                                // 省略内容
                                setAlign_Mid(_printDataRes.getStringArray(R.array.print_pin_verified)[0], PaintSize_Normal);
                                setAlign_Mid(_printDataRes.getStringArray(R.array.print_pin_verified)[1], PaintSize_Normal);
                                setLine_fill_in();
                                break;
                            case 1: // サイン必要
                                setLine_fill_in();
                                // ご署名
                                setAlign_Mid(_printDataRes.getString(R.string.print_credit_signature), PaintSize_Normal);
                                setLine_fill_in();
                                // 改行
                                setLF(6, PaintSize_Normal);
                                isCreditAnnounceSignature = true;
                                setLine_fill_in();
                                break;
                            case 2: // 署名欄なし
                            //ADD-S BMT S.Oyama 2024/10/25 フタバ双方向向け改修
                            case 98:        //PINなし
                            case 99:        //PINあり
                            //ADD-E BMT S.Oyama 2024/10/25 フタバ双方向向け改修
                                break;
                            default:
                                /* 想定外の場合、サイン必要 */
                                setLine_fill_in();
                                // ご署名
                                setAlign_Mid(_printDataRes.getString(R.string.print_credit_signature), PaintSize_Normal);
                                setLine_fill_in();
                                // 改行
                                setLF(6, PaintSize_Normal);
                                isCreditAnnounceSignature = true;
                                setLine_fill_in();
                                // 印刷データ異常（想定外）
                                Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.creditSignatureFlg <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.creditSignatureFlg);
                                break;
                        }
                    } else {
                        /* 想定外の場合、サイン必要 */
                        setLine_fill_in();
                        // ご署名
                        setAlign_Mid(_printDataRes.getString(R.string.print_credit_signature), PaintSize_Normal);
                        setLine_fill_in();
                        // 改行
                        setLF(6, PaintSize_Normal);
                        isCreditAnnounceSignature = true;
                        setLine_fill_in();
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.creditSignatureFlg <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.creditSignatureFlg);
                    }
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica))) {
                /* 交通系電子マネーの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // 交通系 支払
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 1: // 取引種別：取消
                            // 交通系 取消
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }

                // 交通系 残高
                if (_slipData.transAfterBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_after_balance), trans_amount(_slipData.transAfterBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_after_balance), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transAfterBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAfterBalance);
                }

            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAONの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // WAON 支払
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 1: // 取引種別：取消
                            // WAON 取消
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }

                // WAON 残高
                if (_slipData.transAfterBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_after_balance), trans_amount(_slipData.transAfterBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_after_balance), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transAfterBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAfterBalance);
                }

                // 改行
                setLF(1, PaintSize_Normal);

                // ポイント付与区分
                if (_slipData.pointGrantType != null) {
                    switch (_slipData.pointGrantType) {
                        case 0: // ポイント付与が可能なカード
                            if (copy_type == PrinterConst.SlipCopy_Customer) {
                                // ポイント関連内容※お客様控えのみ
                                setLine_fill_in();
                                setAlign_Mid(_printDataRes.getStringArray(R.array.print_pint_grant_msg)[0], PaintSize_Normal);
                                setAlign_Mid(_printDataRes.getStringArray(R.array.print_pint_grant_msg)[1], PaintSize_Normal);
                                setAlign_Mid(_printDataRes.getStringArray(R.array.print_pint_grant_msg)[2], PaintSize_Normal);
                                setLine_fill_in();
                            }
                            break;
                        case 1: // ポイント付与が不可のカード（イオンＪＭＢカード）
                        case 2: // ポイント付与が不可のカード（イオンＪＭＢカード以外）
                            if (_slipData.pointGrantMsgOne != null && _slipData.pointGrantMsgTwo != null) {
                                if (copy_type == PrinterConst.SlipCopy_Customer) {
                                    // ポイント関連内容※お客様控えのみ
                                    setLine_fill_in();
                                    setAlign_Mid(_slipData.pointGrantMsgOne, PaintSize_Normal);
                                    setAlign_Mid(_slipData.pointGrantMsgTwo, PaintSize_Normal);
                                    setLine_fill_in();
                                }
                            } else {
                                /* 想定外の場合、何も印刷しない */
                                // 印刷データ異常（想定外）
                                Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.pointGrantMsgOne <%s> _slipData.pointGrantMsgTwo <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.pointGrantMsgOne, _slipData.pointGrantMsgTwo);
                            }
                            break;
                        default:
                            /* 想定外の場合、何も印刷しない */
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.pointGrantType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.pointGrantType);
                            break;
                    }
                } else {
                    /* 想定外の場合、何も印刷しない */
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.pointGrantType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.pointGrantType);
                }

            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                /* 楽天Edyの場合 */
                // 改行
                setLF(1, PaintSize_Normal);
                /* 取引レシートタイトル(Edy専用) */
                setAlign_Mid(" " + _printDataRes.getString(R.string.print_edy_trans_title), PaintSize_Normal);
                // 改行
                setLF(1, PaintSize_Normal);

                // Edy 決済前残高
                if (_slipData.transBeforeBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_before_balance), trans_amount(_slipData.transBeforeBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_before_balance), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transBeforeBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBeforeBalance);
                }

                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // Edy 支払
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }

                            // Edy 残高
                            if (_slipData.transAfterBalance != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_after_balance), trans_amount(_slipData.transAfterBalance), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_after_balance), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）
                                Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transAfterBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAfterBalance);
                            }

                            // Edy 取引通番
                            if (_slipData.edyTransNumber != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_number), String.valueOf(_slipData.edyTransNumber), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_number), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）
                                Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.edyTransNumber <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.edyTransNumber);
                            }
                            break;
                        default:
                            /* 想定外の場合、何も印刷しない */
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                } else {
                    /* 想定外の場合、何も印刷しない */
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanacoの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // nanaco 支払
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }

                            // nanaco 残高
                            if (_slipData.transAfterBalance != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_trans_after_balance), trans_amount(_slipData.transAfterBalance), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_trans_after_balance), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）
                                Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transAfterBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAfterBalance);
                            }
                            break;
                        default:
                            /* 想定外の場合、何も印刷しない */
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                } else {
                    /* 想定外の場合、何も印刷しない */
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_id))) {
                /* iDの場合 */
                // iD 残高※残高設定されている場合のみ
                if (_slipData.transAfterBalance != null) {
                    setLine();
                    setAlign_LR(_printDataRes.getString(R.string.print_iD_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_after_balance), trans_amount(_slipData.transAfterBalance), PaintSize_Normal);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICAの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // 物販利用
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 1: // 取引種別：取消
                            // 物販利用取消
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_cancel), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_cancel), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 4: // 取引種別：チャージ
                            // 物販チャージ
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_charge), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_charge), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(null, null, PaintSize_Normal);
                            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            break;
                    }
                } else {
                    setAlign_LR(null, null, PaintSize_Normal);
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }

                // 残高
                if (_slipData.transAfterBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_trans_after_balance), trans_amount(_slipData.transAfterBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_trans_after_balance), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transAfterBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAfterBalance);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.codetransPayTypeName != null && _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                    /* QUOカードPayの場合のみ */
                    // 決済金額※現金併用時のみ
                    if (_slipData.transAmount != null && _slipData.transCashTogetherAmount != null && _slipData.transCashTogetherAmount != 0) {
                        setAlign_LR(_printDataRes.getString(R.string.print_codetrans_payment_amount), trans_amount(_slipData.transAmount), PaintSize_Big);
                    }

                    // 残高
                    if (_slipData.transAfterBalance != null) {
                        setAlign_LR(_printDataRes.getString(R.string.print_codetrans_after_amount), trans_amount(_slipData.transAfterBalance), PaintSize_Normal);
                    } else {
                        setAlign_LR(_printDataRes.getString(R.string.print_codetrans_after_amount), null, PaintSize_Normal);
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transAfterBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAfterBalance);
                    }
                }
            }
        }

        // 改行
        setLF(2, PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s%s%s", _printDataRes.getString(R.string.printLog_printDataSet), Log_BrandName, Log_SlipName, Log_CopyTypeName);
        LogAmounts();
        /*インボイス対応*/
        LogInvoice();
        // 印刷
        if (isPT750_Print == true) {
            Printing(_printCanvas);
        }
    }

    // 取引正常伝票（印刷データセット）和多利洋
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPrintData_watari_ok(int copy_type) {

        setAlign_Mid(_printDataRes.getString(R.string.print_brand_watari), PaintSize_Big);
        Log_BrandName = _slipData.transBrand;

        // デモモード確認
        PrintDemoCheck();

        // 票名
        if (_slipData.transType != null && _slipData.printCnt != null) {
            switch (_slipData.transType) {
                case 2: // 取引種別：ポイント付与
                    String print_title = _printDataRes.getString(R.string.print_point_add_slip);  // デフォルトのタイトル名

                    if (_slipData.printCnt > 0) {
                        /* ポイント付与票[再] */
                        setAlign_Mid(print_title + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        Log_SlipName = print_title + _printDataRes.getString(R.string.print_slip_again);

                    } else {
                        /* ポイント付与票 */
                        setAlign_Mid(print_title, PaintSize_Medium);
                        Log_SlipName = print_title;
                    }
                    break;
                case 3: // 取引種別：ポイント取消
                    if (_slipData.printCnt > 0) {
                        /* 取消票[再] */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_cancel_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_point_cancel_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* 取消票 */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_cancel_slip), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_point_cancel_slip);
                    }
                    break;
                default:
                    setAlign_Mid(null, PaintSize_Medium);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_watari_ok->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                    break;
            }
        } else {
            setAlign_Mid(null, PaintSize_Medium);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_watari_ok->_slipData.transType <%d> _slipData.printCnt <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType, _slipData.printCnt);
        }

        // 控名
        if (copy_type == PrinterConst.SlipCopy_Merchant) {
            /* 加盟店控え */
            setAlign_Mid(_printDataRes.getString(R.string.print_merchant), PaintSize_Normal);
            Log_CopyTypeName = _printDataRes.getString(R.string.print_merchant);
        } else if (copy_type == PrinterConst.SlipCopy_Customer) {
            /* お客様控え */
            setAlign_Mid(_printDataRes.getString(R.string.print_customer), PaintSize_Normal);
            Log_CopyTypeName = _printDataRes.getString(R.string.print_customer);
        } else {
            // その他※必要な場合は追加
        }

        // 端末情報（加盟店名、営業所名・号機番号（車番）、電話番号、係員番号、機器番号、機器通番）
        setTermInfo();

        // 取引日時
        if (_slipData.transDate != null && _slipData.transDate.length() == 19) {
            setAlign_LR(_slipData.transDate.substring(0, 10), _slipData.transDate.substring(11, 19), PaintSize_Normal);
        } else {
            setAlign_LR(null, null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_watari_ok->_slipData.transDate <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transDate);
        }

        // 伝票番号（和多利用）
        if (_slipData.slipNumber != null) {
            setAlign_LR(_printDataRes.getString(R.string.print_slip_number), String.valueOf(_slipData.slipNumber), PaintSize_Normal);
        } else {
            setAlign_LR(_printDataRes.getString(R.string.print_slip_number), _printDataRes.getString(R.string.print_slip_number_null), PaintSize_Normal);
        }

        // 元伝票番号※取消対象
        if (_slipData.transType != null && _slipData.transType == 3) {
            if (_slipData.oldSlipNumber != null) {
                setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), String.valueOf(_slipData.oldSlipNumber), PaintSize_Normal);
            } else {
                setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), _printDataRes.getString(R.string.print_old_slip_number_null), PaintSize_Normal);
            }
        }

        // 改行
        setLF(1, PaintSize_Normal);
        setAlign_Left(_printDataRes.getStringArray(R.array.print_point_detail)[0], PaintSize_Normal);
        setAlign_Left(_slipData.transAmount + _printDataRes.getStringArray(R.array.print_point_detail)[1], PaintSize_Normal);
        if (_slipData.transType == 2) {
            setAlign_Left(_printDataRes.getString(R.string.print_point_detail_add), PaintSize_Normal);
        } else if (_slipData.transType == 3) {
            setAlign_Left(_printDataRes.getString(R.string.print_point_detail_cancel), PaintSize_Normal);
        }
        setLF(1, PaintSize_Normal);
        setAlign_LR(_printDataRes.getString(R.string.print_point_sum), String.valueOf(_slipData.watariSumPoint), PaintSize_Normal);

        // お客様向け注意書き（付与の時だけ）
        if (copy_type == PrinterConst.SlipCopy_Customer && _slipData.transType == 2) {
            setLF(2, PaintSize_Normal);
            setAlign_Left(_printDataRes.getStringArray(R.array.print_point_note)[0], PaintSize_Normal);
            setAlign_Left(_printDataRes.getStringArray(R.array.print_point_note)[1], PaintSize_Normal);
        }

        // 改行
        setLF(2, PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s%s%s", _printDataRes.getString(R.string.printLog_printDataSet), Log_BrandName, Log_SlipName, Log_CopyTypeName);
        LogAmounts();
        // 印刷
        if (isPT750_Print == true) {
            Printing(_printCanvas);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPrintData_prepaid_ok(int copy_type) {

        // デモモード確認
        PrintDemoCheck();

        // サービス名
        setAlign_Mid(_slipData.prepaidServiceName, PaintSize_Medium);


        if (_slipDataList.size() > 1) {
            // 印字する明細が複数あるときはチャージ売上票かチャージ票の2種類のみ
            boolean isCharge = true;
            for (SlipData data : _slipDataList) {
                // 売上があるかどうかで判断
                if (data.transType == 0) {
                    isCharge = false;
                }
            }
            // 複数同時のレシートは再印字がない（再印字は個別になる）
            if (isCharge) {
                // チャージ票
                setAlign_Mid(_printDataRes.getString(R.string.print_charge_slip), PaintSize_Medium);
                Log_SlipName = _printDataRes.getString(R.string.print_charge_slip);
            } else {
                // チャージ売上票
                setAlign_Mid(_printDataRes.getString(R.string.print_charge_payment_slip), PaintSize_Medium);
                Log_SlipName = _printDataRes.getString(R.string.print_charge_payment_slip);
            }
        } else {
            switch (_slipData.transType) {
                case 0: // 取引種別：売上
                    String print_title = _printDataRes.getString(R.string.print_payment_slip);  // デフォルトのタイトル名

                    // 加盟店控えについてはPOSサービスのアクティベート状態とマスタデータに応じて印字内容を変更する
                    if (copy_type == PrinterConst.SlipCopy_Merchant) {
                        if (AppPreference.isPosTransaction()) {
                            // マスタデータが未設定の場合に印字するタイトル「取引明細書」を設定
                            print_title = _printDataRes.getString(R.string.print_detail_statement);

                            Thread thread = new Thread(() -> {
                                _serviceFunctionData = DBManager.getServiceFunctionDao().getServiceFunction();
                            });
                            thread.start();
                            try {
                                thread.join();
                                if (_serviceFunctionData != null) {
                                    Timber.tag("printer").d("slip_title = %s", _serviceFunctionData.slip_title);
                                    // マスタに登録されているタイトルが空文字でない場合
                                    if (!_serviceFunctionData.slip_title.isEmpty()) {
                                        print_title = _serviceFunctionData.slip_title;
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (_slipData.printCnt > 0) {
                        /* 売上票[再] */
                        setAlign_Mid(print_title + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        Log_SlipName = print_title + _printDataRes.getString(R.string.print_slip_again);

                    } else {
                        /* 売上票 */
                        setAlign_Mid(print_title, PaintSize_Medium);
                        Log_SlipName = print_title;
                    }
                    break;
                case 1: // 取引種別：取消
                    if (_slipData.printCnt > 0) {
                        /* 取消票[再] */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_cancel_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_cancel_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* 取消票 */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_cancel_slip), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_cancel_slip);
                    }
                    break;
                case 2:
                    if (_slipData.printCnt > 0) {
                        /* ポイント付与[再] */
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_add_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_point_add_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* ポイント付与 */
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_add_slip), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_point_add_slip);
                    }
                    break;
                case 3:
                    if (_slipData.printCnt > 0) {
                        /* ポイント付与取消[再] */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_cancel_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_point_cancel_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* ポイント付与取消 */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_cancel_slip), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_point_cancel_slip);
                    }
                    break;
                case 4: // 取引種別：チャージ
                    if (_slipData.printCnt > 0) {
                        /* チャージ票[再] */
                        setAlign_Mid(_printDataRes.getString(R.string.print_charge_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_charge_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* チャージ票 */
                        setAlign_Mid(_printDataRes.getString(R.string.print_charge_slip), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_charge_slip);
                    }
                    break;
                case 5: // 取引種別：チャージ取消
                    if (_slipData.printCnt > 0) {
                        /* チャージ取消票[再] */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_charge_cancel_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_charge_cancel_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* チャージ取消票 */
                        setLine_emphasize();
                        setAlign_Mid(_printDataRes.getString(R.string.print_charge_cancel_slip), PaintSize_Medium);
                        setLine_emphasize();
                        Log_SlipName = _printDataRes.getString(R.string.print_charge_cancel_slip);
                    }
                    break;
                case 6: //　取引種別：ポイントチャージ
                    if (_slipData.printCnt > 0) {
                        /* ポイントチャージ票[再] */
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_charge_slip) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_point_charge_slip) + _printDataRes.getString(R.string.print_slip_again);
                    } else {
                        /* ポイントチャージ票 */
                        setAlign_Mid(_printDataRes.getString(R.string.print_point_charge_slip), PaintSize_Medium);
                        Log_SlipName = _printDataRes.getString(R.string.print_point_charge_slip);
                    }
                    break;
                default:
                    setAlign_Mid(null, PaintSize_Medium);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                    break;
            }
        }

        // 控名
        if(copy_type == PrinterConst.SlipCopy_Merchant){
            /* 加盟店控え */
            setAlign_Mid(_printDataRes.getString(R.string.print_merchant),PaintSize_Normal);
            Log_CopyTypeName = _printDataRes.getString(R.string.print_merchant);
        }else if(copy_type == PrinterConst.SlipCopy_Customer){
            /* お客様控え */
            setAlign_Mid(_printDataRes.getString(R.string.print_customer),PaintSize_Normal);
            Log_CopyTypeName = _printDataRes.getString(R.string.print_customer);
        }else{
            // その他※必要な場合は追加
        }

        // 端末情報（加盟店名、営業所名・号機番号（車番）、電話番号、係員番号、機器番号、機器通番）
        setTermInfo();

        // カード番号は一つだけ
        setAlign_LR(_printDataRes.getString(R.string.print_card_id), _slipData.card_id_card_company, PaintSize_Normal);

        // 以下、取引の詳細
        for (int i = 0; i < _slipDataList.size(); i++) {
            // 取引日時
            if(_slipDataList.get(i).transDate != null && _slipDataList.get(i).transDate.length() == 19){
                setAlign_LR(_slipDataList.get(i).transDate.substring(0,10), _slipDataList.get(i).transDate.substring(11,19),PaintSize_Normal);
            }else{
                setAlign_LR(null,null,PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_prepaid_ok->_slipDataList.get(i).transDate <%s>",_printDataRes.getString(R.string.printLog_printDataError), _slipDataList.get(i).transDate);
            }

            // 合計金額 or 入金金額
            if(_slipDataList.get(i).transAmount != null && _slipDataList.get(i).transType == 4) {
                /* チャージの場合 */
                setAlign_LR(_printDataRes.getString(R.string.print_charge_amount), trans_amount(_slipDataList.get(i).transAmount), PaintSize_Big);
            }else if(_slipDataList.get(i).transAmount != null && _slipDataList.get(i).transType == 6) {
                /* ポイントチャージの場合 */
                setAlign_LR(_printDataRes.getString(R.string.print_charge), trans_pint(_slipDataList.get(i).transAmount), PaintSize_Big);
            }else if(_slipDataList.get(i).transAmount != null && (_slipDataList.get(i).transType == 1 || _slipDataList.get(i).transType == 5)) {
                /* 決済取消、チャージ取消の場合 */
                setAlign_LR(_printDataRes.getString(R.string.print_total_pay_cancel_amount), trans_amount(_slipDataList.get(i).transAmount), PaintSize_Big);
            }else if(_slipDataList.get(i).transAmount != null && (_slipDataList.get(i).transType == 2 || _slipDataList.get(i).transType == 3)) {
                /* ポイント付与、ポイント付与取消の場合 */
                setAlign_LR(_printDataRes.getString(R.string.print_total_other_amount), trans_amount(_slipDataList.get(i).transAmount), PaintSize_Big);
            } else if (_slipDataList.get(i).transAmount != null && _slipDataList.get(i).transCashTogetherAmount != null) {
                /* 現金併用の場合 */
                setAlign_LR(_printDataRes.getString(R.string.print_total_pay_amount), trans_amount(_slipDataList.get(i).transAmount + _slipDataList.get(i).transCashTogetherAmount), PaintSize_Big);
            }else if(_slipDataList.get(i).transAmount != null){
                /* 通常決済の場合 */
                setAlign_LR(_printDataRes.getString(R.string.print_total_pay_amount), trans_amount(_slipDataList.get(i).transAmount),PaintSize_Big);
            }else{
                setAlign_LR(_printDataRes.getString(R.string.print_total_other_amount), null,PaintSize_Big);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_trans_ok->_slipDataList.get(i).transAmount <%d> _slipDataList.get(i).transCashTogetherAmount <%d>",_printDataRes.getString(R.string.printLog_printDataError), _slipDataList.get(i).transAmount, _slipDataList.get(i).transCashTogetherAmount);
            }

            if (_slipDataList.get(i).transType == 0) {
                // 支払い
                setAlign_LR(_printDataRes.getString(R.string.print_prepaid_pay), trans_amount(_slipDataList.get(i).transAmount), PaintSize_Normal);
                setAlign_LR(_printDataRes.getString(R.string.print_prepaid_balance), trans_amount(_slipDataList.get(i).transAfterBalance), PaintSize_Normal);
                if (_slipDataList.get(i).prepaidAddPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_this_time_point), _slipDataList.get(i).prepaidAddPoint + "P", PaintSize_Normal);
                }
                if (_slipDataList.get(i).prepaidTotalPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_point_sum), _slipDataList.get(i).prepaidTotalPoint + "P", PaintSize_Normal);
                }
                if (_slipDataList.get(i).prepaidNextExpired != null && !_slipDataList.get(i).prepaidNextExpired.isEmpty() && _slipDataList.get(i).prepaidNextExpiredPoint != null) {
                    setAlign_LR(_printDataRes.getStringArray(R.array.print_prepaid_next_expired)[0] + _slipDataList.get(i).prepaidNextExpired + _printDataRes.getStringArray(R.array.print_prepaid_next_expired)[1], _slipDataList.get(i).prepaidNextExpiredPoint + "P", PaintSize_Normal);
                }
            } else if (_slipDataList.get(i).transType == 1) {
                // 支払い取消
                setAlign_LR(_printDataRes.getString(R.string.print_prepaid_pay_cancel), trans_amount(_slipDataList.get(i).transAmount), PaintSize_Normal);
                if (_slipDataList.get(i).transCashTogetherAmount != null && _slipDataList.get(i).transCashTogetherAmount != 0) {
                    // 現金額
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_cash_together_amount), trans_amount(_slipDataList.get(i).transCashTogetherAmount), PaintSize_Normal);
                }
                setAlign_LR(_printDataRes.getString(R.string.print_prepaid_balance), trans_amount(_slipDataList.get(i).transAfterBalance), PaintSize_Normal);
                if (_slipDataList.get(i).prepaidAddPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_cancel_point), _slipDataList.get(i).prepaidAddPoint + "P", PaintSize_Normal);
                }
                if (_slipDataList.get(i).prepaidTotalPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_point_sum), _slipDataList.get(i).prepaidTotalPoint + "P", PaintSize_Normal);
                }
            } else if (_slipDataList.get(i).transType == 2) {
                // ポイント付与
                if (_slipDataList.get(i).prepaidAddPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_this_time_point), _slipDataList.get(i).prepaidAddPoint + "P", PaintSize_Normal);
                }
                if (_slipDataList.get(i).prepaidTotalPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_point_sum), _slipDataList.get(i).prepaidTotalPoint + "P", PaintSize_Normal);
                }
                if (_slipDataList.get(i).prepaidNextExpired != null && !_slipDataList.get(i).prepaidNextExpired.isEmpty() && _slipDataList.get(i).prepaidNextExpiredPoint != null) {
                    setAlign_LR(_printDataRes.getStringArray(R.array.print_prepaid_next_expired)[0] + _slipDataList.get(i).prepaidNextExpired + _printDataRes.getStringArray(R.array.print_prepaid_next_expired)[1], _slipDataList.get(i).prepaidNextExpiredPoint + "P", PaintSize_Normal);
                }
            } else if (_slipDataList.get(i).transType == 3) {
                // ポイント付与取消
                if (_slipDataList.get(i).prepaidAddPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_cancel_point), _slipDataList.get(i).prepaidAddPoint + "P", PaintSize_Normal);
                }
                if (_slipDataList.get(i).prepaidTotalPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_point_sum), _slipDataList.get(i).prepaidTotalPoint + "P", PaintSize_Normal);
                }
            } else if (_slipDataList.get(i).transType == 4) {
                // 現金チャージ
                if (_slipDataList.get(i).transAmount != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_prepaid_charge_amount), trans_amount(_slipDataList.get(i).transAmount), PaintSize_Normal);
                }
                setAlign_LR(_printDataRes.getString(R.string.print_prepaid_balance), trans_amount(_slipDataList.get(i).transAfterBalance), PaintSize_Normal);
                if (_slipDataList.get(i).prepaidTotalPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_point_sum), _slipDataList.get(i).prepaidTotalPoint + "P", PaintSize_Normal);
                }
                if (_slipDataList.get(i).prepaidNextExpired != null && !_slipDataList.get(i).prepaidNextExpired.isEmpty() && _slipDataList.get(i).prepaidNextExpiredPoint != null) {
                    setAlign_LR(_printDataRes.getStringArray(R.array.print_prepaid_next_expired)[0] + _slipDataList.get(i).prepaidNextExpired + _printDataRes.getStringArray(R.array.print_prepaid_next_expired)[1], _slipDataList.get(i).prepaidNextExpiredPoint + "P", PaintSize_Normal);
                }
            } else if (_slipDataList.get(i).transType == 5) {
                // 現金チャージ取消
                if (_slipDataList.get(i).transAmount != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_prepaid_charge_amount_cancel), trans_amount(_slipDataList.get(i).transAmount), PaintSize_Normal);
                }
                setAlign_LR(_printDataRes.getString(R.string.print_prepaid_balance), trans_amount(_slipDataList.get(i).transAfterBalance), PaintSize_Normal);
                if (_slipDataList.get(i).prepaidTotalPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_point_sum), _slipDataList.get(i).prepaidTotalPoint + "P", PaintSize_Normal);
                }
            } else if (_slipDataList.get(i).transType == 6) {
                // ポイントチャージ
                if (_slipDataList.get(i).prepaidAddPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_prepaid_charge_point), _slipDataList.get(i).prepaidAddPoint + "P", PaintSize_Normal);
                }
                setAlign_LR(_printDataRes.getString(R.string.print_prepaid_balance), trans_amount(_slipDataList.get(i).transAfterBalance), PaintSize_Normal);
                if (_slipDataList.get(i).prepaidTotalPoint != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_point_sum), _slipDataList.get(i).prepaidTotalPoint + "P", PaintSize_Normal);
                }
            }

            if(i != _slipDataList.size() - 1) {
                // 点線ライン
                setLine_dotted();
            }
        }

        // 改行
        setLF(2,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s%s%s",_printDataRes.getString(R.string.printLog_printDataSet),Log_BrandName,Log_SlipName,Log_CopyTypeName);
        LogAmounts();
        // 印刷
        if (isPT750_Print == true) {
            Printing(_printCanvas);
        }
    }

    // 端末情報（加盟店名、営業所名・号機番号（車番）、電話番号、係員番号、機器番号、機器通番）
    private void setTermInfo() {

        // 点線ライン
        setLine_dotted();

        // 加盟店名
        if (_slipData.merchantName != null) {
            setAlign_Left(_slipData.merchantName, PaintSize_Normal);
        } else {
            setAlign_Left(null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setTermInfo->_slipData.merchantName <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.merchantName);
        }

        // 営業所名・号機番号（車番）
        if (_slipData.merchantOffice != null && _slipData.carId != null) {
            setAlign_LR(_slipData.merchantOffice, String.valueOf(_slipData.carId) + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);
        } else if (_slipData.merchantOffice != null) {
            setAlign_LR(_slipData.merchantOffice, _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setTermInfo->_slipData.carId <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.carId);
        } else if (_slipData.carId != null) {
            setAlign_LR(null, String.valueOf(_slipData.carId) + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);
            if (AppPreference.isServicePos() == false) {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setTermInfo->_slipData.merchantOffice <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.merchantOffice);
            }
        } else {
            setAlign_LR(null, _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setTermInfo->_slipData.merchantOffice <%s> _slipData.carId <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.merchantOffice, _slipData.carId);
        }

        // 電話番号
        if (_slipData.merchantTelnumber != null) {
            setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), _slipData.merchantTelnumber, PaintSize_Normal);
        } else {
            setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setTermInfo->_slipData.merchantTelnumber <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.merchantTelnumber);
        }

        //インボイス登録番号
        if ((invoicePaymentType() && AppPreference.judgeInvoice(true)) && !AppPreference.settlementPosChk()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), AppPreference.getInvoiceNo(), PaintSize_Normal);
        }
        Log_ServicePos = String.format("%s", AppPreference.settlementPosChk());
        Log_InvoiceNo = AppPreference.getInvoiceNo();

        // 係員番号
        if (_slipData.driverId != null) {
            if (AppPreference.isServicePos() && !AppPreference.isDriverCodeInput()) {
                // POS機能有効で乗務員コード入力なしの場合、係員番号は空欄で印字
                setAlign_LR(_printDataRes.getString(R.string.print_driver_id), null, PaintSize_Normal);
            } else {
                setAlign_LR(_printDataRes.getString(R.string.print_driver_id), String.valueOf(_slipData.driverId), PaintSize_Normal);
            }
        } else {
            setAlign_LR(_printDataRes.getString(R.string.print_driver_id), null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setTermInfo->_slipData.driverId <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.driverId);
        }

        // 機器番号
        if (_slipData.termId != null) {
            setAlign_LR(_printDataRes.getString(R.string.print_term_id), _slipData.termId, PaintSize_Normal);
        } else {
            setAlign_LR(_printDataRes.getString(R.string.print_term_id), null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setTermInfo->_slipData.termId <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.termId);
        }

        // 機器通番
        if (_slipData.termSequence != null) {
            setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), String.valueOf(_slipData.termSequence), PaintSize_Normal);
        } else {
            setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setTermInfo->_slipData.termSequence <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.termSequence);
        }

        // 点線ライン
        setLine_dotted();
    }

    // 取引未了伝票（印刷データセット）
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPrintData_trans_unfinished(int copy_type) {

        // 強調ライン
        setLine_emphasize();
        // 改行
        setLF(1, PaintSize_Normal);

        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) && _slipData.transResultDetail == PrinterConst.TransDetail_Communication_Failure) {
                /* WAONの通信障害のみ */
                // 障害
                setAlign_Mid(_printDataRes.getString(R.string.print_failure), PaintSize_Big);
                Log_ErrTitle = _printDataRes.getString(R.string.print_failure);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) && _slipData.transResultDetail == PrinterConst.TransDetail_Communication_Failure) {
                /* nanacoの通信障害のみ */
                // 決済未完了
                setAlign_Mid(_printDataRes.getString(R.string.print_trans_unfinished), PaintSize_Big);
                Log_ErrTitle = _printDataRes.getString(R.string.print_trans_unfinished);
            } else {
                if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                    /* 交通系電子マネー、WAON、楽天Edy、nanaco、OKICA、コード決済 */
                    // 処理未了
                    setAlign_Mid(_printDataRes.getString(R.string.print_unfinished), PaintSize_Big);
                    Log_ErrTitle = _printDataRes.getString(R.string.print_unfinished);
                } else {
                    setAlign_Mid(null, PaintSize_Big);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transBrand <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand);
                }
            }
        } else {
            setAlign_Mid(null, PaintSize_Big);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transBrand <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBrand);
        }

        // 改行
        setLF(1, PaintSize_Normal);
        // 強調ライン
        setLine_emphasize();
        // 改行
        setLF(1, PaintSize_Normal);

        // 処理未了の印刷内容
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* 交通系電子マネー、WAON、楽天Edy、nanaco、OKICA */
                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[0], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[1], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[2], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[3], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[4], PaintSize_Medium);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[5], PaintSize_Normal);
                setLF(2, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[6], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[7], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[8], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[9], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[10], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[11], PaintSize_Normal);
                setLF(2, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[12], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[13], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[14], PaintSize_Normal);
                setLF(2, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[15], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[16], PaintSize_Normal);
                setLF(2, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_unfinished_content)[17], PaintSize_Normal);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済 */
                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[0], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[1], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[2], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[3], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[4], PaintSize_Medium);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[5], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[6], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[7], PaintSize_Normal);
                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[8], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[9], PaintSize_Normal);
                setLF(2, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[10], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[11], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[12], PaintSize_Normal);
                setLF(2, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[13], PaintSize_Normal);
                setLF(1, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[14], PaintSize_Normal);
                setLF(2, PaintSize_Normal);

                setAlign_Left(_printDataRes.getStringArray(R.array.print_codetrans_unfinished_content)[15], PaintSize_Normal);
            } else {
                /* その他 */
                // 改行
                setLF(1, PaintSize_Normal);
            }
        }

        // 改行
        setLF(1, PaintSize_Normal);
        // 強調ライン
        setLine_emphasize();
        // 改行
        setLF(1, PaintSize_Normal);

        // ブランド名
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica))) {
                /* 交通系電子マネー */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_suica), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAON */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_waon), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                /* 楽天Edy */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_edy_rakute), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanaco */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_nanaco), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICA */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_okica), PaintSize_Big);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済 */
                setAlign_Mid(_printDataRes.getString(R.string.print_brand_codetrans), PaintSize_Big);
            } else {
                setAlign_Mid(null, PaintSize_Big);
                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
            }
        }
        Log_BrandName = _slipData.transBrand;

        // デモモード確認
        PrintDemoCheck();

        // 票名
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) && _slipData.transResultDetail == PrinterConst.TransDetail_Communication_Failure) {
                /* WAONの通信障害のみ */
                /* 障害票 */
                setAlign_Mid(_printDataRes.getString(R.string.print_failure_slip), PaintSize_Medium);
                Log_SlipName = _printDataRes.getString(R.string.print_failure_slip);
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) && _slipData.transResultDetail == PrinterConst.TransDetail_Communication_Failure) {
                /* nanacoの通信障害のみ */
                /* 決済未完了票 */
                setAlign_Mid(_printDataRes.getString(R.string.print_trans_unfinished_slip), PaintSize_Medium);
                Log_SlipName = _printDataRes.getString(R.string.print_trans_unfinished_slip);
            } else {
                if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica)) ||
                        _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                    /* 交通系電子マネー、WAON、楽天Edy、nanaco、OKICA、コード決済 */
                    /* 処理未了票 */
                    setAlign_Mid(_printDataRes.getString(R.string.print_unfinished_slip), PaintSize_Medium);
                    Log_SlipName = _printDataRes.getString(R.string.print_unfinished_slip);
                } else {
                    setAlign_Mid(null, PaintSize_Medium);
                    // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                }
            }
        } else {
            setAlign_Mid(null, PaintSize_Medium);
            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
        }

        // 控名
        if (copy_type == PrinterConst.SlipCopy_Merchant) {
            /* 加盟店控え */
            setAlign_Mid(_printDataRes.getString(R.string.print_merchant), PaintSize_Normal);
            Log_CopyTypeName = _printDataRes.getString(R.string.print_merchant);
        } else {
            // その他※必要な場合は追加
        }

        // 端末情報（加盟店名、営業所名・号機番号（車番）、電話番号、係員番号、機器番号、機器通番）
        setTermInfo();

        // 取引日時
        if (_slipData.transDate != null && _slipData.transDate.length() == 19) {
            setAlign_LR(_slipData.transDate.substring(0, 10), _slipData.transDate.substring(11, 19), PaintSize_Normal);
        } else {
            setAlign_LR(null, null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transDate <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transDate);
        }

        // カード番号
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* 交通系電子マネー、WAON、楽天Edy、nanaco、OKICA */

                if (copy_type == PrinterConst.SlipCopy_Merchant) {
                    /* 加盟店控え */
                    if (_slipData.cardIdMerchant != null) {
                        setAlign_LR(_printDataRes.getString(R.string.print_card_id), card_id_separated(_slipData.cardIdMerchant), PaintSize_Normal);
                        card_id_masked(_slipData.cardIdMerchant);
                    } else {
                        if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica))) {
                            /* 交通系電子マネー（17桁） */
                            setAlign_LR(_printDataRes.getString(R.string.print_card_id), card_id_separated(_printDataRes.getString(R.string.print_suica_card_id_null)), PaintSize_Normal);
                        } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                                _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                                _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                            /* WAON、楽天Edy、nanaco（16桁） */
                            setAlign_LR(_printDataRes.getString(R.string.print_card_id), card_id_separated(_printDataRes.getString(R.string.print_card_id_null)), PaintSize_Normal);
                        } else {
                            setAlign_LR(_printDataRes.getString(R.string.print_card_id), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                        }
                    }
                } else {
                    // その他※必要な場合は追加
                }
            }
        }

        // カード通番
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAON */
                if (_slipData.cardTransNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_sequence_number), String.valueOf(_slipData.cardTransNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_sequence_number), _printDataRes.getString(R.string.print_card_trans_number_null), PaintSize_Normal);
                }
            }
        }

        // 伝票番号・元伝票番号（取消番号）
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAONの場合 */
                // 伝票番号
                if (_slipData.slipNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), String.valueOf(_slipData.slipNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), _printDataRes.getString(R.string.print_slip_number_null), PaintSize_Normal);
                }

                // 元伝票番号※取消対象
                if (_slipData.transType != null && _slipData.transType == 1) {
                    if (_slipData.oldSlipNumber != null) {
                        setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), String.valueOf(_slipData.oldSlipNumber), PaintSize_Normal);
                    } else {
                        setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), _printDataRes.getString(R.string.print_old_slip_number_null), PaintSize_Normal);
                    }
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICAの場合 */
                // 伝票番号
                if (_slipData.slipNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), String.valueOf(_slipData.slipNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.slipNumber <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.slipNumber);
                }

                // 元伝票番号※取消対象
                if (_slipData.transType != null && _slipData.transType == 1) {
                    if (_slipData.oldSlipNumber != null) {
                        setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), String.valueOf(_slipData.oldSlipNumber), PaintSize_Normal);
                    } else {
                        setAlign_LR(_printDataRes.getString(R.string.print_old_slip_number), null, PaintSize_Normal);
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.oldSlipNumber <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.oldSlipNumber);
                    }
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.codetransOrderId != null && _slipData.codetransOrderId.length() == 20) {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), _slipData.codetransOrderId, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_slip_number), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.codetransOrderId <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.codetransOrderId);
                }
            }
        }

        // 取引通番
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                /* 楽天Edyの場合 */
                if (_slipData.cardTransNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), String.valueOf(_slipData.cardTransNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), _printDataRes.getString(R.string.print_card_trans_number_null), PaintSize_Normal);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanacoの場合、「取引通番」に伝票番号の値を設定（JREM指摘事項）*/
                if (_slipData.nanacoSlipNumber != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), String.valueOf(_slipData.nanacoSlipNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_card_trans_number), _printDataRes.getString(R.string.print_card_trans_number_null), PaintSize_Normal);
                }
            }
        }

        // 決済種別番号・決済種別名
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAON */
                if (_slipData.transTypeCode != null && !_slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_SALES) && !_slipData.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_CANCEL)) {
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_code), _slipData.transTypeCode, PaintSize_Normal);
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_name), history_trade_type_name(_slipData.transTypeCode, null), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_code), _printDataRes.getString(R.string.print_trans_type_code_null), PaintSize_Normal);
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_name), _printDataRes.getString(R.string.print_trans_type_name_null), PaintSize_Normal);
                }
            }
        }

        // 端末番号
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* 交通系電子マネー、WAON、楽天Edy、nanaco */

                if (_slipData.termIdentId != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), _slipData.termIdentId, PaintSize_Normal);
                } else {
                    if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                            _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                        /* 交通系電子マネー、WAON（13桁固定）*/
                        setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), _printDataRes.getString(R.string.print_suica_term_ident_id_null), PaintSize_Normal);
                    } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                        /* 楽天Edy（8桁固定）*/
                        setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), _printDataRes.getString(R.string.print_edy_rakute_term_ident_id_null), PaintSize_Normal);
                    } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                        /* nanaco（20桁固定）*/
                        setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), _printDataRes.getString(R.string.print_nanaco_term_ident_id_null), PaintSize_Normal);
                    } else {
                        setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), null, PaintSize_Normal);
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.termIdentId <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.termIdentId);
                    }
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICA */
                if (_slipData.termIdentId != null && _slipData.termIdentId.length() == 17) {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_term_ident_id), _slipData.termIdentId, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_term_ident_id), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.termIdentId <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.termIdentId);
                }
            }
        }

        // ユニークID
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanaco */
                if (_slipData.commonName != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_common_name), _slipData.commonName, PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_common_name), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.commonName <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.commonName);
                }
            }
        }

        // 決済内容
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 支払
                            setAlign_LR(_printDataRes.getString(R.string.print_codetrans_detail), _printDataRes.getString(R.string.print_codetrans_payment), PaintSize_Normal);
                            break;
                        case 1: // 取消
                            setAlign_LR(_printDataRes.getString(R.string.print_codetrans_detail), _printDataRes.getString(R.string.print_codetrans_cancel), PaintSize_Normal);
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_codetrans_detail), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                }
            }
        }

        /*インボイス対応*/
        //運賃表示
        if (_slipData.transAmount != null && Tax_FareOutputConfirm() && AppPreference.judgeInvoice(false) && !AppPreference.settlementPosChk()) {
            setAlign_LR(_printDataRes.getString(R.string.print_total_fare), trans_amount(_slipData.transAmount + _slipData.transCashTogetherAmount), PaintSize_Normal);
        } else {
            // 印字データ対象外
            Timber.tag("Printer").i("%s：setPrintData_trans_ok->_slipData.transAmount <%d> _slipData.transCashTogetherAmount <%d>", _printDataRes.getString(R.string.printLog_printfareError), _slipData.transAmount, _slipData.transCashTogetherAmount);
        }

        // 合計金額 or 入金金額
        if (_slipData.transAmount != null && _slipData.transType == 4) {
            /* チャージの場合 */
            setAlign_LR(_printDataRes.getString(R.string.print_charge_amount), trans_amount(_slipData.transAmount), PaintSize_Big);
        } else if (_slipData.transAmount != null && _slipData.transCashTogetherAmount != null) {
            /* 現金併用決済の場合 */
            setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_slipData.transAmount + _slipData.transCashTogetherAmount), PaintSize_Big);
        } else if (_slipData.transAmount != null) {
            /* 通常決済の場合 */
            setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_slipData.transAmount), PaintSize_Big);
        } else {
            setAlign_LR(_printDataRes.getString(R.string.print_total_amount), null, PaintSize_Big);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transAmount <%d> _slipData.transCashTogetherAmount <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transAmount, _slipData.transCashTogetherAmount);
        }
        /*インボイス対応*/
        //消費税率
        if ((Tax_FareOutputConfirm() && AppPreference.judgeInvoice(false)) && !AppPreference.settlementPosChk()) {
            String taxRate = AppPreference.getreceiptTax();
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_tax_h) + taxRate + _printDataRes.getString(R.string.print_invoice_tax_e), null, PaintSize_Normal);
        }
        Log_InvoiceTax = AppPreference.getreceiptTax();
        // 増減額
        if (_slipData.transType != null) {
            if (_slipData.transType == 0) {
                // 支払時のみ
                if (_slipData.transAdjAmount != null && _slipData.transAdjAmount != 0) {
                    /* 増減額あり*/
                    setAlign_LR(_printDataRes.getStringArray(R.array.print_adj_amount)[0], adj_amount(_slipData.transAdjAmount) + _printDataRes.getStringArray(R.array.print_adj_amount)[1], PaintSize_Normal);
                }
            }
        }

        // 実線ライン
        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) ||
                    _slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* 交通系電子マネー、WAON、楽天Edy、nanaco、OKICA */
                setLine();
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.codetransPayTypeName != null && _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                    /* QUOカードPayの場合のみ */
                    // 実線ライン
                    setLine();
                }
            }
        }

        // 現金
        if (_slipData.transCashTogetherAmount != null && _slipData.transCashTogetherAmount != 0) {
            // 現金額
            setAlign_LR(_printDataRes.getString(R.string.print_trans_cash_together_amount), trans_amount(_slipData.transCashTogetherAmount), PaintSize_Normal);
        }

        if (_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica))) {
                /* 交通系電子マネーの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // 交通系 支払
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 1: // 取引種別：取消
                            // 交通系 取消
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                }

                // 交通系 決済前残高
                if (_slipData.transBeforeBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_before_balance), trans_amount(_slipData.transBeforeBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_suica_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_before_balance), _printDataRes.getString(R.string.print_trans_before_balance_null), PaintSize_Normal);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon))) {
                /* WAONの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // WAON 支払(01)
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 1: // 取引種別：取消
                            // WAON 取消(10)
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_cancel), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                }

                // WAON 決済前残高
                if (_slipData.transBeforeBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_before_balance), trans_amount(_slipData.transBeforeBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_waon_xxx) + " " + _printDataRes.getString(R.string.print_xxx_trans_before_balance), _printDataRes.getString(R.string.print_trans_before_balance_null), PaintSize_Normal);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute))) {
                /* 楽天Edyの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // Edy 支払
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                }

                // Edy 決済前残高
                if (_slipData.transBeforeBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_before_balance), trans_amount(_slipData.transBeforeBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_before_balance), _printDataRes.getString(R.string.print_trans_before_balance_null), PaintSize_Normal);
                }

                // Edy 取引通番
                if (_slipData.transAfterBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_number), String.valueOf(_slipData.edyTransNumber), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_edy_rakute_xxx) + _printDataRes.getString(R.string.print_xxx_trans_number), _printDataRes.getString(R.string.print_edy_rakute_trans_number_null), PaintSize_Normal);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco))) {
                /* nanacoの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // nanaco 支払
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx), null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                }

                // nanaco 決済前残高
                if (_slipData.transBeforeBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_trans_before_balance), trans_amount(_slipData.transBeforeBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_nanaco_xxx) + _printDataRes.getString(R.string.print_xxx_trans_before_balance), _printDataRes.getString(R.string.print_trans_before_balance_null), PaintSize_Normal);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
                /* OKICAの場合 */
                if (_slipData.transType != null) {
                    switch (_slipData.transType) {
                        case 0: // 取引種別：売上
                            // 物販利用
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_payment), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_payment), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 1: // 取引種別：取消
                            // 物販利用取消
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_cancel), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_cancel), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        case 4: // 取引種別：チャージ
                            // 物販チャージ
                            if (_slipData.transAmount != null) {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_charge), trans_amount(_slipData.transAmount), PaintSize_Normal);
                            } else {
                                setAlign_LR(_printDataRes.getString(R.string.print_okica_charge), null, PaintSize_Normal);
                                // 印刷データ異常（想定外）※先の方でログ残しているため、ログ不要
                            }
                            break;
                        default:
                            setAlign_LR(null, null, PaintSize_Normal);
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                            break;
                    }
                } else {
                    setAlign_LR(null, null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transType <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transType);
                }

                // 決済前残高
                if (_slipData.transBeforeBalance != null) {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_trans_before_balance), trans_amount(_slipData.transBeforeBalance), PaintSize_Normal);
                } else {
                    setAlign_LR(_printDataRes.getString(R.string.print_okica_trans_before_balance), null, PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：setPrintData_trans_unfinished->_slipData.transBeforeBalance <%d>", _printDataRes.getString(R.string.printLog_printDataError), _slipData.transBeforeBalance);
                }
            } else if (_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans))) {
                /* コード決済の場合 */
                if (_slipData.codetransPayTypeName != null && _slipData.codetransPayTypeName.equals(QRPayTypeNameMap.get(QRPayTypeCodes.QUOPay))) {
                    /* QUOカードPayの場合のみ */
                    // 決済金額※現金併用時のみ
                    if (_slipData.transAmount != null && _slipData.transCashTogetherAmount != null && _slipData.transCashTogetherAmount != 0) {
                        setAlign_LR(_printDataRes.getString(R.string.print_codetrans_payment_amount), trans_amount(_slipData.transAmount), PaintSize_Big);
                    }
                }
            }
        }

        // 改行
        setLF(2, PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s %s%s%s", _printDataRes.getString(R.string.printLog_printDataSet), Log_ErrTitle, Log_BrandName, Log_SlipName, Log_CopyTypeName);
        LogAmounts();

        /*インボイス対応*/
        LogInvoice();

        // 印刷
        if (isPT750_Print == true) {
            Printing(_printCanvas);
        }
    }

    private void sendWsPrintHistryWaon(DeviceClient.ResultWAON resultWAON) {
        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        try {
            _sendData.put("type", "/printdata/v1");
            if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                _sendData.put("cmd", "print_hist_general");
            } else {
                _sendData.put("cmd", "print_hist_waon");
            }
            _sendData.put("timer", PrinterConst.DuplexPrintWaitTimer);
            if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                String datetime = convertDatetime(resultWAON.time);
                _params.put("time", datetime);                              // 照会日時
            } else {
                _params.put("time", resultWAON.time);                       // 照会日時
            }
            _params.put("user_mask_waon_num", resultWAON.userMaskWaonNum());    // カード番号
            _params.put("term_ident_id", resultWAON.termIdentId);           // 端末番号
            _params.put("mc_term_id", AppPreference.getMcTermId());         // 機器番号

            if (resultWAON.addInfo != null) {
                _params.put("hist_num", resultWAON.addInfo.historyData.length); // 履歴個数
                // 過去決済履歴
                for (int i = 0; i < resultWAON.addInfo.historyData.length; i++) {
                    JSONObject _history = new JSONObject();
                    String HistName = "hist" + (i + 1);
                    // 決済日時
                    if (resultWAON.addInfo.historyData[i].historyDate != null) {
                        if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                            String date = convertDate(resultWAON.addInfo.historyData[i].historyDate);
                            _history.put("date", date);
                        } else {
                            _history.put("date", resultWAON.addInfo.historyData[i].historyDate);
                        }
                    } else {
                        _history.put("date", "");
                    }
                    // 決済時刻
                    if (resultWAON.addInfo.historyData[i].historyTime != null) {
                        if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                            String time = convertTime(resultWAON.addInfo.historyData[i].historyTime);
                            _history.put("time", time);
                        } else {
                            _history.put("time", resultWAON.addInfo.historyData[i].historyTime);
                        }
                    } else {
                        _history.put("time", "");
                    }
                    // カード通番(決済通番)
                    if (resultWAON.addInfo.historyData[i].cardThroughNum != null) {
                        _history.put("card_through_num", resultWAON.addInfo.historyData[i].cardThroughNum);
                    } else {
                        _history.put("card_through_num", "");
                    }
                    // 決済種別番号
                    if (resultWAON.addInfo.historyData[i].tradeTypeCode != null) {
                        _history.put("trade_type_code", resultWAON.addInfo.historyData[i].tradeTypeCode);
                    } else {
                        _history.put("trade_type_code", "");
                    }
                    // 決済種別名
                    if (resultWAON.addInfo.historyData[i].tradeTypeCode != null && resultWAON.addInfo.historyData[i].chargeType != null) {
                        _history.put("trade_type_name", convertSjisString(history_trade_type_name(resultWAON.addInfo.historyData[i].tradeTypeCode, resultWAON.addInfo.historyData[i].chargeType)));
                    } else {
                        _history.put("trade_type_name", "");
                    }
                    // チャージ金額
                    if (resultWAON.addInfo.historyData[i].chargeValue != null) {
                        _history.put("charge_value", resultWAON.addInfo.historyData[i].chargeValue);
                    } else {
                        _history.put("charge_value", "");
                    }
                    // 決済金額
                    if (resultWAON.addInfo.historyData[i].value != null) {
                        _history.put("value", resultWAON.addInfo.historyData[i].value);
                    } else {
                        _history.put("value", "");
                    }
                    // 決済後残高
                    if (resultWAON.addInfo.historyData[i].balance != null) {
                        _history.put("balance", resultWAON.addInfo.historyData[i].balance);
                    } else {
                        _history.put("balance", "");
                    }
                    // 端末番号
                    if (resultWAON.addInfo.historyData[i].terminalId != null) {
                        _history.put("terminal_id", resultWAON.addInfo.historyData[i].terminalId);
                    } else {
                        _history.put("terminal_id", "");
                    }
                    // 伝票番号
                    if (resultWAON.addInfo.historyData[i].terminalThroughNum != null) {
                        _history.put("terminal_through_num", resultWAON.addInfo.historyData[i].terminalThroughNum);
                    } else {
                        _history.put("terminal_through_num", "");
                    }
                    _params.put(HistName, _history);
                }

            } else {
                _params.put("hist_num", 0); // 履歴個数
            }

            if (_sendData.get("cmd") == "print_hist_general") {
                setWsPrintHistryGeneral(_params, resultWAON);
            }

            // パラメータの格納
            _sendData.put("data", _params);
//            _ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintHistryWaon->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    private void setWsPrintHistryGeneral(JSONObject params, DeviceClient.ResultWAON resultWAON) {
        // type：printdata/v1、cmd：print_hist_general で必要なパラメータを設定
        try {
            params.put("brand", convertSjisString(_printDataRes.getString(R.string.print_brand_waon)));  // ブランド名
            params.put("title", convertSjisString(_printDataRes.getString(R.string.print_history_slip)));  // タイトル
//ADD-S BMT S.Oyama 2024/11/05 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {              // フタバ双方向向け
                String tmpuser_mask_card_num = resultWAON.userMaskWaonNum();
                tmpuser_mask_card_num = tmpuser_mask_card_num.replace("*", "X");
                params.put("user_mask_card_num", tmpuser_mask_card_num);        // カード番号
            }
            else
            {
                params.put("user_mask_card_num", resultWAON.userMaskWaonNum()); // カード番号
            }
//ADD-E BMT S.Oyama 2024/11/05 フタバ双方向向け改修
            params.put("term_ident_id_title", convertSjisString(_printDataRes.getString(R.string.print_term_ident_id)));  // 端末番号タイトル
            params.put("mc_term_id_title", convertSjisString(_printDataRes.getString(R.string.print_term_id)));  // 機器番号タイトル

            if (resultWAON.addInfo != null) {
                if (resultWAON.addInfo.historyData[0].balance != null) {
                    params.put("balance", resultWAON.addInfo.historyData[0].balance);  // 残高
                } else {
                    params.put("balance", "");
                }

//ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {              // フタバ双方向向け
                    if (resultWAON.addInfo.historyData[0].sprw_id != null) {
                        params.put("sprw_id", resultWAON.addInfo.historyData[0].sprw_id);  // sprwid
                    } else {
                        params.put("sprw_id", "             ");
                    }
                }
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：setWsPrintHistryGeneral->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    //ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief メータに対して履歴データ通知（フタバ双方向用）
     * @note  メータに対して履歴データ通知:前段処理
     * @param [in] DeviceClient.ResultWAON resultWAON   : Waon関連情報クラス
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
//    private void sendWsPrintHistryFutabaD(DeviceClient.ResultWAON resultWAON) {
//        Timber.i("[FUTABA-D]sendWsPrintHistryFutabaD()");
//
//        _resultWAONBackup = resultWAON;
//
//        if(_ifBoxManager == null){
//            Timber.e("[FUTABA-D]sendWsPrintHistryFutabaD():_ifBoxManager is null");
//            return;
//        }
//
//        _ifBoxManager.send820_WaonHistoryStart();           // WAON履歴通知：フタバ双方向向け
//    }

    /******************************************************************************/
    /*!
     * @brief メータに対して履歴データ通知（フタバ双方向用）
     * @note  メータに対して履歴データ通知:後段処理
     * @param [in] DeviceClient.ResultWAON resultWAON   : Waon関連情報クラス
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
//    public void sendWsPrintHistryFutabaDAfterJob(){
//
//        Timber.i("[FUTABA-D]sendWsPrintHistryFutabaDAfterJob()  ");
//
//        if(_resultWAONBackup  == null){
//            Timber.e("[FUTABA-D]sendWsPrintHistryFutabaDAfterJob():_resultWAONBackup is null");
//            return;
//        }
//
//        sendWsPrintHistryFutabaDCore(_resultWAONBackup);
//        _resultWAONBackup = null;
//    }

    /******************************************************************************/
    /*!
     * @brief メータに対して履歴データ通知（フタバ双方向用）
     * @note  メータに対して履歴データ通知：本処理
     * @param [in] DeviceClient.ResultWAON resultWAON   : Waon関連情報クラス
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void sendWsPrintHistryFutabaDCore(DeviceClient.ResultWAON resultWAON) {
        Timber.i("[FUTABA-D]sendWsPrintHistryFutabaDCore()  ");

        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        String tmpDateTimeStrConvert;

        try {
            _sendData.put("type","/printdata/v2");
            _sendData.put("cmd", "print_hist_general");
            _sendData.put("timer",PrinterConst.DuplexPrintWaitTimer);
            _sendData.put("status", "000");                                // ステータス

            tmpDateTimeStrConvert = convertDatetime(resultWAON.time);
            _params.put("time", tmpDateTimeStrConvert);                     // 処理日時 yyyy/mm/dd/ hh:mm:ss

//ADD-S BMT S.Oyama 2024/11/05 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {              // フタバ双方向向け
                String tmpuser_mask_waon_num = resultWAON.userMaskWaonNum();
                tmpuser_mask_waon_num  = tmpuser_mask_waon_num.replace("*", "X");
                _params.put("user_mask_waon_num", tmpuser_mask_waon_num);  // カード番号
            }
            else
            {
                _params.put("user_mask_waon_num", resultWAON.userMaskWaonNum());    // カード番号
            }
//ADD-E BMT S.Oyama 2024/11/05 フタバ双方向向け改修

            _params.put("term_ident_id", resultWAON.termIdentId);           // 端末番号
            _params.put("mc_term_id", AppPreference.getMcTermId());         // 機器番号

            if(resultWAON.addInfo != null) {
                _params.put("hist_num", resultWAON.addInfo.historyData.length); // 履歴個数
                // 過去決済履歴
                for (int i = 0; i < resultWAON.addInfo.historyData.length; i++) {
                    JSONObject _history = new JSONObject();
                    String HistName = "hist" + (i + 1);
                    // 決済日時
                    if (resultWAON.addInfo.historyData[i].historyDate != null) {
                        _history.put("date", resultWAON.addInfo.historyData[i].historyDate);            //yymmdd(820->メータ通信仕様に合わせる)
                    } else{
                        _history.put("date", "");
                    }
                    // 決済時刻
                    if (resultWAON.addInfo.historyData[i].historyTime != null) {
                        tmpDateTimeStrConvert = convertTimeNotSecAndColon(resultWAON.addInfo.historyData[i].historyTime);
                        _history.put("time", tmpDateTimeStrConvert);            //hhmm(820->メータ通信仕様に合わせる)
                    }else{
                        _history.put("time", "");
                    }
                    // カード通番(決済通番)
                    if (resultWAON.addInfo.historyData[i].cardThroughNum != null) {
                        _history.put("card_through_num", resultWAON.addInfo.historyData[i].cardThroughNum);
                    }else{
                        _history.put("card_through_num", "");
                    }
                    // 決済種別番号
                    if (resultWAON.addInfo.historyData[i].tradeTypeCode != null) {
                        _history.put("trade_type_code", "0" + resultWAON.addInfo.historyData[i].tradeTypeCode);
                    }else{
                        _history.put("trade_type_code", "");
                    }
                    // 決済種別名
                    if (resultWAON.addInfo.historyData[i].tradeTypeCode != null && resultWAON.addInfo.historyData[i].chargeType != null) {
                        _history.put("trade_type_name", convertSjisString(history_trade_type_name(resultWAON.addInfo.historyData[i].tradeTypeCode, resultWAON.addInfo.historyData[i].chargeType)));
                    }else{
                        _history.put("trade_type_name", "");
                    }
                    // チャージ金額
                    if (resultWAON.addInfo.historyData[i].chargeValue != null) {
                        _history.put("charge_value", resultWAON.addInfo.historyData[i].chargeValue);
                    }else{
                        _history.put("charge_value", "");
                    }
                    // 決済金額
                    if (resultWAON.addInfo.historyData[i].value != null) {
                        _history.put("value", resultWAON.addInfo.historyData[i].value);
                    }else{
                        _history.put("value", "");
                    }
                    // 決済後残高
                    if (resultWAON.addInfo.historyData[i].balance != null) {
                        _history.put("balance", resultWAON.addInfo.historyData[i].balance);
                    }else{
                        _history.put("balance", "");
                    }
                    // 端末番号
                    if (resultWAON.addInfo.historyData[i].terminalId != null) {
                        _history.put("terminal_id", resultWAON.addInfo.historyData[i].terminalId);
                    }else{
                        _history.put("terminal_id", "");
                    }
                    // 伝票番号
                    if (resultWAON.addInfo.historyData[i].terminalThroughNum != null) {
                        _history.put("terminal_through_num", resultWAON.addInfo.historyData[i].terminalThroughNum);
                    }else{
                        _history.put("terminal_through_num", "");
                    }

                    // SPRWID
                    if (resultWAON.addInfo.historyData[i].sprw_id != null) {
                        _history.put("sprw_id", resultWAON.addInfo.historyData[i].sprw_id);
                    }else{
                        _history.put("sprw_id", "             ");
                    }

                    _params.put(HistName, _history);
                }

            }else{
                _params.put("hist_num", 0); // 履歴個数
            }

            setWsPrintHistryGeneral(_params, resultWAON);

            _params.put("brand", _printDataRes.getString(R.string.print_brand_waon));  // ブランド名

            // パラメータの格納
            _sendData.put("data", _params);

//            _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.SENDING);
//            _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.WAON_HISTORYJOBCORE);          //WAON歴出力本体
//
//            _ifBoxManager.sendFutabaDExt(_sendData.toString(), PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              //フタバD専用送出を利用


            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000); // 10秒(1万ミリ秒)間だけ処理を止める
                    } catch (InterruptedException e) {
                    }

                    PrinterManager printerManager = PrinterManager.getInstance();
                    printerManager.dismissPrintingDialogExt();
                }
            });

            thread.start();

            thread.join();

            Printing_end();
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintHistryFutabD->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }



    }
    //ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief メータに対して履歴データ通知 エラー通知（フタバ双方向用）
     * @note  メータに対して履歴データ通知　エラー通知：本処理
     * @param [in] DeviceClient.ResultWAON resultWAON   : Waon関連情報クラス
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void sendWsPrintHistryFutabaDCoreErrorAck() {
        Timber.i("[FUTABA-D]sendWsPrintHistryFutabaDCore()  ");

        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();

        try {
            _sendData.put("type","/printdata/v2");
            _sendData.put("cmd", "print_hist_general");
            _sendData.put("timer",PrinterConst.DuplexPrintWaitTimer);
            _sendData.put("status", "x59");                                // ステータス エラー通知

            _params.put("time", "2001/01/01 00:00:00");                     // 処理日時 yyyy/mm/dd/ hh:mm:ss

            _params.put("user_mask_waon_num", "XXXXXXXXXXXX0000");  // カード番号

            _params.put("term_ident_id", "0000000000000");           // 端末番号
            _params.put("mc_term_id", AppPreference.getMcTermId());         // 機器番号

            _params.put("hist_num",3); // 履歴個数
            // 過去決済履歴
            for (int i = 0; i < 3; i++) {
                JSONObject _history = new JSONObject();
                String HistName = "hist" + (i + 1);
                // 決済日時
                _history.put("date", "20010101");
                // 決済時刻
                _history.put("time", "000000");
                // カード通番(決済通番)
                _history.put("card_through_num", "0");
                // 決済種別番号
                _history.put("trade_type_code", "0");
                // 決済種別名
                _history.put("trade_type_name", "0");
                // チャージ金額
                _history.put("charge_value", "0");
                // 決済金額
                _history.put("value", "0");
                // 決済後残高
                _history.put("balance", "0");
                // 端末番号
                _history.put("terminal_id", "0000000000000");
                // 伝票番号
                _history.put("terminal_through_num", "0");

                // SPRWID
                _history.put("sprw_id", "             ");

                _params.put(HistName, _history);
            }

            _params.put("brand", _printDataRes.getString(R.string.print_brand_waon));  // ブランド名
            _params.put("title", convertSjisString(_printDataRes.getString(R.string.print_history_slip)));  // タイトル
            _params.put("user_mask_card_num", "XXXXXXXXXXXX0000");  // カード番号
            _params.put("term_ident_id_title", convertSjisString(_printDataRes.getString(R.string.print_term_ident_id)));  // 端末番号タイトル
            _params.put("mc_term_id_title", convertSjisString(_printDataRes.getString(R.string.print_term_id)));  // 機器番号タイトル
            _params.put("balance", "0");
            _params.put("sprw_id", "             ");

            // パラメータの格納
            _sendData.put("data", _params);

//            _ifBoxManager.setSendMeterDataStatus_General(IFBoxManager.SendMeterDataStatus_FutabaD.SENDING);
//            _ifBoxManager.setSendMeterDataPhase_General(IFBoxManager.SendMeterDataStatus_FutabaD.GENERICABORTCODE_NONACK);          //WAON歴出力本体 ACKなしで送出
//
//            _ifBoxManager.sendFutabaDExt(_sendData.toString(), PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              //フタバD専用送出を利用

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000); // 10秒(1万ミリ秒)間だけ処理を止める
                    } catch (InterruptedException e) {
                    }

                    PrinterManager printerManager = PrinterManager.getInstance();
                    printerManager.dismissPrintingDialogExt();
                }
            });

            thread.start();

            thread.join();

            Printing_end();
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：sendWsPrintHistryFutabD->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }



    }
    //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修

    // 履歴照会票（印刷データセット）
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTransHistory_WAON(DeviceClient.ResultWAON resultWAON){

        print_init();

        if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) ||
        //ADDCHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ||
            IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D))
        //ADDCHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        {
            isPT750_Print = false;
        }

        // ブランド名
        /* WAON */
        setAlign_Mid(_printDataRes.getString(R.string.print_brand_waon),PaintSize_Big);
        Log_BrandName = _printDataRes.getString(R.string.print_brand_waon);

        // デモモード確認
        PrintDemoCheck();
        
        // 票名
        /* 履歴照会票 */
        setAlign_Mid(_printDataRes.getString(R.string.print_history_slip),PaintSize_Medium);
        Log_SlipName = _printDataRes.getString(R.string.print_history_slip);

        // 照会日時
        if(resultWAON.time != null){
            setAlign_Left(history_time(resultWAON.time),PaintSize_Normal);
        }else{
            setAlign_Left(null,PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.time <%s>",_printDataRes.getString(R.string.printLog_printDataError), resultWAON.time);
        }

        // カード番号
        if(resultWAON.userMaskWaonNum() != null){
            setAlign_LR(_printDataRes.getString(R.string.print_card_id), history_card_id_separated(resultWAON.userMaskWaonNum()),PaintSize_Normal);
        }else{
            setAlign_LR(_printDataRes.getString(R.string.print_card_id), null,PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.userMaskWaonNum <%s>",_printDataRes.getString(R.string.printLog_printDataError), resultWAON.userMaskWaonNum());
        }

        // 端末番号
        if(resultWAON.termIdentId != null){
            setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), resultWAON.termIdentId, PaintSize_Normal);
        }else{
            setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.termIdentId <%s>",_printDataRes.getString(R.string.printLog_printDataError), resultWAON.termIdentId);
        }

        // 機器番号
        if(AppPreference.getMcTermId() != null){
            setAlign_LR(_printDataRes.getString(R.string.print_term_id), AppPreference.getMcTermId(),PaintSize_Normal);
        }else{
            setAlign_LR(_printDataRes.getString(R.string.print_term_id), null,PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_WAON->AppPreference.getMcTermId() <%s>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMcTermId());
        }

        if(resultWAON.addInfo != null) {

            // 改行
            setLF(1,PaintSize_Normal);

            // 過去決済履歴
            for(int i = 0; i < resultWAON.addInfo.historyData.length; i++){

                // ＜履歴Ｘ＞
                setAlign_Left(_printDataRes.getStringArray(R.array.print_history)[i],PaintSize_Normal);

                // 決済日時
                if(resultWAON.addInfo.historyData[i].historyDate != null && resultWAON.addInfo.historyData[i].historyTime != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_time), history_trans_time(resultWAON.addInfo.historyData[i].historyDate, resultWAON.addInfo.historyData[i].historyTime),PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_time), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].historyDate <%s> resultWAON.addInfo.historyData[%d].historyTime <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].historyDate, i, resultWAON.addInfo.historyData[i].historyTime);
                }

                // 決済通番
                if(resultWAON.addInfo.historyData[i].cardThroughNum != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_number), resultWAON.addInfo.historyData[i].cardThroughNum,PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_number), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].cardThroughNum <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].cardThroughNum);
                }

                // 決済種別番号
                if(resultWAON.addInfo.historyData[i].tradeTypeCode != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_code), resultWAON.addInfo.historyData[i].tradeTypeCode,PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_code), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].tradeTypeCode <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].tradeTypeCode);
                }

                // 決済種別名
                if(resultWAON.addInfo.historyData[i].tradeTypeCode != null && resultWAON.addInfo.historyData[i].chargeType != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_name), history_trade_type_name(resultWAON.addInfo.historyData[i].tradeTypeCode, resultWAON.addInfo.historyData[i].chargeType),PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_type_name), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].tradeTypeCode <%s> resultWAON.addInfo.historyData[%d].chargeType <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].tradeTypeCode, i, resultWAON.addInfo.historyData[i].chargeType);
                }

                // チャージ金額
                if(resultWAON.addInfo.historyData[i].chargeValue != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_cash_charge_amount), trans_amount(Integer.parseInt(resultWAON.addInfo.historyData[i].chargeValue)),PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_cash_charge_amount), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].chargeValue <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].chargeValue);
                }

                // 決済金額
                if(resultWAON.addInfo.historyData[i].value != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_amount), trans_amount(Integer.parseInt(resultWAON.addInfo.historyData[i].value)),PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_amount), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].value <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].value);
                }

                // 決済後残高
                if(resultWAON.addInfo.historyData[i].balance != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_after_amount), trans_amount(Integer.parseInt(resultWAON.addInfo.historyData[i].balance)),PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_trans_after_amount), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].balance <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].balance);
                }

                // 端末番号
                if(resultWAON.addInfo.historyData[i].terminalId != null){
                    setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), resultWAON.addInfo.historyData[i].terminalId,PaintSize_Normal);
                }else{
                    setAlign_LR(_printDataRes.getString(R.string.print_term_ident_id), null,PaintSize_Normal);
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printTransHistory_WAON->resultWAON.addInfo.historyData[%d].terminalId <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, resultWAON.addInfo.historyData[i].terminalId);
                }

                // 改行
                setLF(1,PaintSize_Normal);
            }
        }

        // 改行
        setLF(2,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s%s",_printDataRes.getString(R.string.printLog_printDataSet),Log_BrandName,Log_SlipName);
        // 印刷
        if (isPT750_Print == true) {
            Printing(_printCanvas);
        } else {
            // 双方向用にデータをWS送信
            //ADDCHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {              // フタバ双方向向け
                sendWsPrintHistryFutabaDCore(resultWAON);
            }
            else {
                sendWsPrintHistryWaon(resultWAON);
            }
            //ADDCHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        }
    }

    // 残高履歴票（印刷データセット）
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTransHistory_OKICA(BaseEMoneyOkicaViewModel.HistoryData historyData, String HistoryPrintDateTime){

        print_init();

        // ブランド名
        /* OKICA */
        setAlign_Mid(_printDataRes.getString(R.string.print_brand_okica),PaintSize_Big);
        Log_BrandName = _printDataRes.getString(R.string.print_brand_okica);

        // デモモード確認
        PrintDemoCheck();

        // 票名
        /* 残高履歴票 */
        setAlign_Mid(_printDataRes.getString(R.string.print_okica_history_slip),PaintSize_Medium);
        Log_SlipName = _printDataRes.getString(R.string.print_okica_history_slip);

        // 印刷日時
        String PrintDateTime = HistoryPrintDateTime;
        if(PrintDateTime != null && PrintDateTime.length() == 32){
            setAlign_Left(PrintDateTime, PaintSize_Normal);
        }else{
            setAlign_Left(null,PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_OKICA->PrintDateTime <%s>",_printDataRes.getString(R.string.printLog_printDataError), PrintDateTime);
        }

        // 機器番号
        if(AppPreference.getMcTermId() != null){
            setAlign_LR(_printDataRes.getString(R.string.print_term_id), AppPreference.getMcTermId(),PaintSize_Normal);
        }else{
            setAlign_LR(_printDataRes.getString(R.string.print_term_id), null,PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_OKICA->AppPreference.getMcTermId() <%s>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMcTermId());
        }

        // OKICA端末番号
        String okicaTermId = null;
        if (isDemoMode()) {
            // デモモードの場合、固定値を使用
            okicaTermId = "12345678901234567";
        }else{
            okicaTermId = AppPreference.getOkicaTerminalInfo() != null
                    ? AppPreference.getOkicaTerminalInfo().terminalId
                    : null;
        }

        if(okicaTermId != null && okicaTermId.length() == 17){
            setAlign_LR(_printDataRes.getString(R.string.print_okica_term_ident_id), okicaTermId, PaintSize_Normal);
        }else{
            setAlign_LR(_printDataRes.getString(R.string.print_okica_term_ident_id), null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_OKICA->okicaTermId <%s>",_printDataRes.getString(R.string.printLog_printDataError), okicaTermId);
        }

        // カード番号
        String CardNo = historyData.IDi.getCardNo();
        if(CardNo != null && CardNo.length() == 17){
            setAlign_LR(_printDataRes.getString(R.string.print_card_id), okica_history_card_id_separated(CardNo),PaintSize_Normal);
        }else{
            setAlign_LR(_printDataRes.getString(R.string.print_card_id), null,PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_OKICA->historyData.IDi.getCardNo() <%s>",_printDataRes.getString(R.string.printLog_printDataError), CardNo);
        }

        // 残高
        int Balance = historyData.sfBalanceInfo.getBalance();
        if(Balance >= 0 && Balance <=  16711425){
            setAlign_LR(_printDataRes.getString(R.string.print_history_balance), trans_amount(Balance),PaintSize_Normal);
        }else{
            setAlign_LR(_printDataRes.getString(R.string.print_history_balance), null,PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printTransHistory_OKICA->historyData.sfBalanceInfo.getBalance() <%d>",_printDataRes.getString(R.string.printLog_printDataError), Balance);
        }

        // 改行
        setLF(1,PaintSize_Normal);

        // SFログ情報（直近３件分）
        for(int i = 0; i < historyData.logs.length; i++){

            int Year = historyData.logs[i].getYear();
            int Month = historyData.logs[i].getMonth();
            int Date = historyData.logs[i].getDate();

            // 日付判定（履歴なし）
            if(Year == 0 && Month == 0 && Date == 0){
                break;
            }

            // ＜履歴Ｘ＞
            setAlign_Left(_printDataRes.getStringArray(R.array.print_history)[i],PaintSize_Normal);

            // 日付（年：0~99 月：1~12 日：1~31）
            if(Year >= 0 && Year <= 99 && Month >= 1 && Month <= 12 && Date >= 1 && Date <= 31){
                setAlign_LR(_printDataRes.getString(R.string.print_history_date), history_date(Year, Month, Date),PaintSize_Normal);
            }else{
                setAlign_LR(_printDataRes.getString(R.string.print_history_date), null,PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTransHistory_OKICA->historyData.logs[%d].Year <%d> historyData.logs[%d].Month <%d> historyData.logs[%d].Day <%s>",_printDataRes.getString(R.string.printLog_printDataError), i, Year, i, Month, i, Date);
            }

            // 種別（範囲：0~127）
            int ProcessingType = historyData.logs[i].getProcessingType();
            if(ProcessingType >= 0 && ProcessingType <= 127){
                setAlign_LR(_printDataRes.getString(R.string.print_history_process_type), okica_history_process_type_name(ProcessingType),PaintSize_Normal);
            }else{
                setAlign_LR(_printDataRes.getString(R.string.print_history_process_type), null,PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTransHistory_OKICA->historyData.logs[%d].getProcessingType() <%d>",_printDataRes.getString(R.string.printLog_printDataError), i, ProcessingType);
            }

            // 残高（範囲：0~16711425）
            int logsBalance = historyData.logs[i].getBalance();
            if(logsBalance >= 0 && logsBalance <=  16711425){
                setAlign_LR(_printDataRes.getString(R.string.print_history_balance), trans_amount(logsBalance),PaintSize_Normal);
            }else{
                setAlign_LR(_printDataRes.getString(R.string.print_history_balance), null,PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTransHistory_OKICA->historyData.logs[%d].getBalance() <%d>",_printDataRes.getString(R.string.printLog_printDataError), i, logsBalance);
            }

            // 改行
            setLF(1,PaintSize_Normal);
        }

        // 改行
        setLF(2,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s%s",_printDataRes.getString(R.string.printLog_printDataSet),Log_BrandName,Log_SlipName);
        // 印刷
        Printing(_printCanvas);
    }

    // 集計印刷命令
    // Order：未印刷(0), 過去の最新(1), ・・・ , 過去の最古(5)
    // AggregateType：集計・処理未了(0), 集計・処理未了・明細(1)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printAggregate(int Order, int AggregateType){
        String _meterStatus = "";
        isSlipDataList_Size = 0;
        isAggregateOrder = Order;
        isAggregateType = AggregateType;
        print_init();

        // 双方向用にデータをWS送信
        if(IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D)) {
            /* 集計票 */
            Log_SlipName = _printDataRes.getString(R.string.print_aggregate_slip);
            Timber.tag("Printer").i("%s：%s", _printDataRes.getString(R.string.printLog_printDataSet), Log_SlipName);
            // 双方向用にデータをWS送信
//            _meterStatus = _ifBoxManager.getMeterStatus();
//            if (_meterStatus.equals("KUUSYA")) {
//                sendWsPrintAggregate();
//            } else {
//                // 集計印字の際にメーター状態が不正
//                Timber.tag("Printer").e("printer status err <%s>", _meterStatus);
//                Printing_Duplex("NG", 0, PrinterConst.DuplexPrintStatus_METERSTSERROR);
//            }
        } else {
            // 別スレッド：伝票印刷関連データ取得
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    /* 全件取得 */
                    if(isAggregateOrder >= 0 && isAggregateOrder <= 5) {
                        _slipDataList = DBManager.getSlipDao().getAggregate(isAggregateOrder);
                        _aggregateData = DBManager.getAggregateDao().getAggregateHistoryByOrder(isAggregateOrder);
                    }
                }
            });
            thread.start();

            try {
                thread.join();
                isSlipDataList_Size = _slipDataList.size();
                if(isSlipDataList_Size > 0 && _aggregateData != null) {
                    // 印刷データあり
                    setPrintData_aggregate();
                }else if(isSlipDataList_Size == 0){
                    // 印刷データなし
                    NoPrintData();
                }else{
                    // 印刷データ異常（想定外）
                    Timber.tag("Printer").e("%s：printAggregate->isSlipDataList_Size <%d> _aggregateData <%s>",_printDataRes.getString(R.string.printLog_printDataError), isSlipDataList_Size, _aggregateData);
                    PrintDataError();
                }
            } catch (Exception e) {
                Timber.tag("Printer").e("%s：printAggregate->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
                PrintDataError();
                e.printStackTrace();
            }
        }
    }

    // 集計伝票（印刷データセット）
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPrintData_aggregate(){
        isTransFinished_TransCnt = 0;
        isTransFinished_TotalAmount = 0;
        isTransUnfinished_TransCnt = 0;
        isTransUnfinished_TotalAmount = 0;

        // 票名
        if(isAggregateOrder > 0)
        {
            /* 集計票 [再] */
            setAlign_Mid(_printDataRes.getString(R.string.print_aggregate_slip) + _printDataRes.getString(R.string.print_slip_again),PaintSize_Big);
            Log_SlipName = _printDataRes.getString(R.string.print_aggregate_slip) + _printDataRes.getString(R.string.print_slip_again);
        }else{
            /* 集計票  */
            setAlign_Mid(_printDataRes.getString(R.string.print_aggregate_slip),PaintSize_Big);
            Log_SlipName = _printDataRes.getString(R.string.print_aggregate_slip);
        }

        // デモモード確認
        PrintDemoCheck();

        // 営業所名・号機（車番）
        if(AppPreference.isServicePos()) {
            if(AppPreference.getPosMerchantOffice() != null && AppPreference.getMcCarId() >= 0){
                setAlign_LR(AppPreference.getPosMerchantOffice(), String.valueOf(AppPreference.getMcCarId()) + _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
            }else if(AppPreference.getPosMerchantName() != null && AppPreference.getMcCarId() >= 0) {
                // 営業所名が無い場合は会社名を印字
                setAlign_LR(AppPreference.getPosMerchantName(), String.valueOf(AppPreference.getMcCarId()) + _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
            }else if(AppPreference.getPosMerchantOffice() != null){
                setAlign_LR(AppPreference.getPosMerchantOffice(), _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_aggregate->AppPreference.getMcCarId() <%d>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMcCarId());
            }else if(AppPreference.getPosMerchantName() != null){
                setAlign_LR(AppPreference.getPosMerchantName(), _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_aggregate->AppPreference.getMcCarId() <%d>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMcCarId());
            }else if(AppPreference.getMcCarId() >= 0){
                setAlign_LR(null, String.valueOf(AppPreference.getMcCarId()) + _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_aggregate->AppPreference.getPosMerchantOffice() <%s> AppPreference.getPosMerchantName() <%s>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getPosMerchantOffice(), AppPreference.getPosMerchantName());
            }else{
                setAlign_LR(null,  _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_aggregate->AppPreference.getPosMerchantOffice() <%s> AppPreference.getPosMerchantName() <%s> AppPreference.getMcCarId() <%d>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMerchantOffice(), AppPreference.getPosMerchantName(), AppPreference.getMcCarId());
            }
        }else{
            if(AppPreference.getMerchantOffice() != null && AppPreference.getMcCarId() >= 0){
                setAlign_LR(AppPreference.getMerchantOffice(), String.valueOf(AppPreference.getMcCarId()) + _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
            }else if(AppPreference.getMerchantOffice() != null){
                setAlign_LR(AppPreference.getMerchantOffice(), _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_aggregate->AppPreference.getMcCarId() <%d>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMcCarId());
            }else if(AppPreference.getMcCarId() >= 0){
                setAlign_LR(null, String.valueOf(AppPreference.getMcCarId()) + _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_aggregate->AppPreference.getMerchantOffice() <%s>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMerchantOffice());
            }else{
                setAlign_LR(null,  _printDataRes.getString(R.string.print_car_id),PaintSize_Normal);
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_aggregate->AppPreference.getMerchantOffice() <%s> AppPreference.getMcCarId() <%d>",_printDataRes.getString(R.string.printLog_printDataError), AppPreference.getMerchantOffice(), AppPreference.getMcCarId());
            }
        }

        // 日時（最古～最新）
        setPrintData_aggregate_date();

        // 取引
        if(SlipDataList_TransResult_Existence(PrinterConst.TransResult_OK) == true){
            setPrintData_aggregate_trans_ok();
        }

        // 処理未了
        if(SlipDataList_TransResult_Existence(PrinterConst.TransResult_UnFinished) == true)
        {
            setPrintData_aggregate_trans_unfinished();
        }

        // 明細
        if(isAggregateType == PrinterConst.AggregateType_Detail){
            setPrintData_aggregate_trans_detail();
        }

        // ブランド名確認
        CheckBrandName();

        // 改行
        setLF(1,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s",_printDataRes.getString(R.string.printLog_printDataSet),Log_SlipName);
        // 印刷
        Printing(_printCanvas);
    }

    // 日時（最古～最新）
    private void setPrintData_aggregate_date(){
        String OldestDate;
        String LatestDate;

        if(_aggregateData.aggregateStartDatetime != null && _aggregateData.aggregateStartDatetime.length() == 19){
            OldestDate = _aggregateData.aggregateStartDatetime.substring(2,16);
        }else{
            OldestDate = null;
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_aggregate_date->_aggregateData.aggregateStartDatetime <%s>",_printDataRes.getString(R.string.printLog_printDataError), _aggregateData.aggregateStartDatetime);
        }

        if(_aggregateData.aggregateEndDatetime != null && _aggregateData.aggregateEndDatetime.length() == 19){
            LatestDate = _aggregateData.aggregateEndDatetime.substring(2,16);
        }else{
            LatestDate = null;
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_aggregate_date->_aggregateData.aggregateEndDatetime <%s>",_printDataRes.getString(R.string.printLog_printDataError), _aggregateData.aggregateEndDatetime);
        }

        if(OldestDate != null && LatestDate != null){
            setAlign_LR(OldestDate + " ～ ", LatestDate, PaintSize_Normal);
        }else if(OldestDate != null){
            setAlign_LR(OldestDate + " ～ ", null, PaintSize_Normal);
        }else if(LatestDate != null){
            setAlign_LR(null," ～ " + LatestDate, PaintSize_Normal);
        }else{
            setAlign_LR(null, null, PaintSize_Normal);
        }
    }

    // 取引結果の有無確認
    private boolean SlipDataList_TransResult_Existence(int TransResult_Type){
        boolean Result = false;

        for(int i = 0; i < isSlipDataList_Size; i++){
            if(_slipDataList.get(i).transResult == TransResult_Type){
                Result = true;
                break;
            }
        }
        return Result;
    }

    // 取引
    private void setPrintData_aggregate_trans_ok(){

        // 現金併用分の金額リセット
        togetherCashSales = 0;
        togetherCashCancel = 0;

        // 実線ライン
        setLine();

        // 取引
        setAlign_Mid(_printDataRes.getString(R.string.print_aggregate_trans_ok),PaintSize_Medium);

        // 実線ライン
        setLine();

        // クレジット（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_credit), PrinterConst.TransResult_OK);

        // 交通系電子マネー（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_suica), PrinterConst.TransResult_OK);

        // WAON（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_waon), PrinterConst.TransResult_OK);

        // 楽天Edy（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_edy_rakute), PrinterConst.TransResult_OK);

        // nanaco（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_nanaco), PrinterConst.TransResult_OK);

        // iD（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_id), PrinterConst.TransResult_OK);

        // QUICPay（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_quicpay), PrinterConst.TransResult_OK);

        // コード決済（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_codetrans), PrinterConst.TransResult_OK);

        // OKICA（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_okica), PrinterConst.TransResult_OK);

        // 現金併用分の加減算があるため一番最後に集計すること
        if(AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
            // 現金（集計データセット）
            setAggregate_data(_printDataRes.getString(R.string.print_brand_cash), PrinterConst.TransResult_OK);
        }

        // 為替類対応
        if(AppPreference.isServicePos() || AppPreference.isFixedAmountPostalOrder()) {
            // 為替類（集計データセット）
            setAggregate_data(_printDataRes.getString(R.string.print_brand_postal_order), PrinterConst.TransResult_OK);
        }

        // プリペイド（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.prepaid_brand), PrinterConst.TransResult_OK);

        // 改行
        setLF(1,PaintSize_Normal);

        // 合計（X件）　売上総額
        setTransFinished_TransCnt_TotalAmount();

        // 改行
        setLF(1,PaintSize_Normal);
    }

    // 処理未了
    private void setPrintData_aggregate_trans_unfinished(){

        // 実線ライン
        setLine();

        // 処理未了
        setAlign_Mid(_printDataRes.getString(R.string.print_aggregate_trans_unfinished),PaintSize_Medium);

        // 実線ライン
        setLine();

        // 交通系電子マネー（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_suica), PrinterConst.TransResult_UnFinished);

        // WAON（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_waon), PrinterConst.TransResult_UnFinished);

        // 楽天Edy（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_edy_rakute), PrinterConst.TransResult_UnFinished);

        // nanaco（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_nanaco), PrinterConst.TransResult_UnFinished);

        // iD（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_id), PrinterConst.TransResult_UnFinished);

        // QUICPay（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_quicpay), PrinterConst.TransResult_UnFinished);

        // コード決済（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_codetrans), PrinterConst.TransResult_UnFinished);

        // OKICA（集計データセット）
        setAggregate_data(_printDataRes.getString(R.string.print_brand_okica), PrinterConst.TransResult_UnFinished);

        // 改行
        setLF(1,PaintSize_Normal);

        // 計（X件）　未了総額
        setTransUnfinished_TransCnt_TotalAmount();

        // 改行
        setLF(1,PaintSize_Normal);
    }

    // 集計データセット
    private void setAggregate_data(String BrandName, Integer TransResult_Type){
        int TransCnt = 0;
        long TotalAmount = 0;
        _slipDataList_Filter = null;

        _slipDataList_Filter = Observable.fromIterable(_slipDataList)
                .filter(sdl -> sdl.transBrand.equals(BrandName) && sdl.transResult == TransResult_Type)
                .toList()
                .blockingGet();

        for(int i = 0; i < _slipDataList_Filter.size(); i++){
            if(_slipDataList_Filter.get(i).transDate == null || _slipDataList_Filter.get(i).transDate.length() != 19 ||
                    _slipDataList_Filter.get(i).transType == null || (_slipDataList_Filter.get(i).transType != 0 && _slipDataList_Filter.get(i).transType != 1 && _slipDataList_Filter.get(i).transType != 4) ||
                    _slipDataList_Filter.get(i).transAmount == null){
                /* 取引日時、取引種別、取引金額の内、どれか一つでも問題があった場合、印刷対象外 */
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setAggregate_data->_slipDataList.transBrand <%s> _slipDataList.transDate <%s> _slipDataList.transType <%d> _slipDataList.transAmount <%d> _slipDataList.transResult <%d>"
                        ,_printDataRes.getString(R.string.printLog_printDataError), BrandName, _slipDataList_Filter.get(i).transDate, _slipDataList_Filter.get(i).transType, _slipDataList_Filter.get(i).transAmount, _slipDataList_Filter.get(i).transResult);
            }else{
                if(_slipDataList_Filter.get(i).transType == 0){
                    // 取引種別：売上
                    TransCnt += 1;
                    TotalAmount += _slipDataList_Filter.get(i).transAmount;
                    if(!BrandName.equals(_printDataRes.getString(R.string.money_brand_cash)) && (TransResult_Type == PrinterConst.TransResult_OK)) {
                        // 現金決済以外の現金併用分は現金扱い（売上）として集計
                        togetherCashSales += _slipDataList_Filter.get(i).transCashTogetherAmount;
                    }
                }else if(_slipDataList_Filter.get(i).transType == 1){
                    // 取引種別：取消
                    TransCnt += 1;
                    TotalAmount -= _slipDataList_Filter.get(i).transAmount;
                    if(!BrandName.equals(_printDataRes.getString(R.string.money_brand_cash)) && (TransResult_Type == PrinterConst.TransResult_OK)) {
                        // 現金決済以外の現金併用分は現金扱い（取消）として集計
                        togetherCashCancel += _slipDataList_Filter.get(i).transCashTogetherAmount;
                    }
                }else{
                    // 必要な場合、追加
                }
            }
        }

        // 現金決済分に現金併用分の加減算
        if(BrandName.equals(_printDataRes.getString(R.string.money_brand_cash))) {
            TotalAmount += (togetherCashSales - togetherCashCancel);
        }

        if(TransCnt != 0){
            if(TransResult_Type == PrinterConst.TransResult_OK){
                // グランド名　件数
                setAlign_Left(BrandName + _printDataRes.getStringArray(R.array.print_aggregate_count)[0] + String.valueOf(TransCnt) + _printDataRes.getStringArray(R.array.print_aggregate_count)[1],PaintSize_Normal);
                isTransFinished_TransCnt += TransCnt;
                // 総額
                setAlign_Right(aggregate_brand_total_amount(TotalAmount), PaintSize_Normal);
                isTransFinished_TotalAmount += TotalAmount;
            }else if(TransResult_Type == PrinterConst.TransResult_UnFinished){
                // グランド名　件数
                setAlign_Left(BrandName + _printDataRes.getStringArray(R.array.print_aggregate_count)[0] + String.valueOf(TransCnt) + _printDataRes.getStringArray(R.array.print_aggregate_count)[1],PaintSize_Normal);
                isTransUnfinished_TransCnt += TransCnt;
                // 総額
                setAlign_Right(aggregate_brand_total_amount(TotalAmount), PaintSize_Normal);
                isTransUnfinished_TotalAmount += TotalAmount;
            }else{
                // 必要な場合、追加
            }
        } else {
            // 現金決済が0件だが、現金併用分がある場合の印字対応
            if (AppPreference.isServicePos()) {
                if (BrandName.equals(_printDataRes.getString(R.string.money_brand_cash)) &&
                        TransResult_Type == PrinterConst.TransResult_OK &&
                        TotalAmount != 0) {
                    // ブランド名　件数
                    setAlign_Left(BrandName + _printDataRes.getStringArray(R.array.print_aggregate_count)[0] + String.valueOf(TransCnt) + _printDataRes.getStringArray(R.array.print_aggregate_count)[1],PaintSize_Normal);
                    isTransFinished_TransCnt += TransCnt;
                    // 総額
                    setAlign_Right(aggregate_brand_total_amount(TotalAmount), PaintSize_Normal);
                    isTransFinished_TotalAmount += TotalAmount;
                }
            }
        }
    }

    // 合計（X件）　売上総額
    private void setTransFinished_TransCnt_TotalAmount(){
        // 合計（X件）
        setAlign_LR(_printDataRes.getStringArray(R.array.print_aggregate_TransOK_TransCnt_TotalAmount)[0] + String.valueOf(isTransFinished_TransCnt) + _printDataRes.getStringArray(R.array.print_aggregate_TransOK_TransCnt_TotalAmount)[1], _printDataRes.getStringArray(R.array.print_aggregate_TransOK_TransCnt_TotalAmount)[2],PaintSize_Medium);
        // 売上総額
        setAlign_Right(aggregate_total_amount(isTransFinished_TotalAmount),PaintSize_Medium);
    }

    // 計（X件）　未了総額
    private void setTransUnfinished_TransCnt_TotalAmount(){
        // 計（X件）
        setAlign_LR(_printDataRes.getStringArray(R.array.print_aggregate_TransUnfinished_TransCnt_TotalAmount)[0] + String.valueOf(isTransUnfinished_TransCnt) + _printDataRes.getStringArray(R.array.print_aggregate_TransUnfinished_TransCnt_TotalAmount)[1], _printDataRes.getStringArray(R.array.print_aggregate_TransUnfinished_TransCnt_TotalAmount)[2],PaintSize_Medium);
        // 未了総額
        setAlign_Right(aggregate_total_amount(isTransUnfinished_TotalAmount),PaintSize_Medium);
    }

    // ブランド名確認
    private void CheckBrandName(){

        _slipDataList_Filter = null;
        _slipDataList_Filter = Observable.fromIterable(_slipDataList)
                .filter(sdl -> sdl.transBrand == null ||
                        (sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_credit)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_suica)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_waon)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_edy_rakute)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_nanaco)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_id)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_quicpay)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_codetrans)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_watari)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.prepaid_brand)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_cash)) == false &&
                                sdl.transBrand.equals(_printDataRes.getString(R.string.print_brand_postal_order)) == false))
                .toList()
                .blockingGet();

        for(int i = 0; i < _slipDataList_Filter.size(); i++){
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setAggregate_data->_slipDataList.transBrand <%s> _slipDataList.transDate <%s> _slipDataList.transType <%d> _slipDataList.transAmount <%d> _slipDataList.transResult <%d>"
                    ,_printDataRes.getString(R.string.printLog_printDataError), _slipDataList_Filter.get(i).transBrand, _slipDataList_Filter.get(i).transDate, _slipDataList_Filter.get(i).transType, _slipDataList_Filter.get(i).transAmount, _slipDataList_Filter.get(i).transResult);
        }
    }

    // 明細
    private void setPrintData_aggregate_trans_detail(){
        isTransFinished_TotalAmount_NA_flg = false;

        // 実線ライン
        setLine();

        // 明細
        setAlign_Mid(_printDataRes.getString(R.string.print_aggregate_trans_detail),PaintSize_Medium);

        // 実線ライン
        setLine();

        // クレジット（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_credit));

        // 交通系電子マネー（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_suica));

        // WAON（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_waon));

        // 楽天Edy（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_edy_rakute));

        // nanaco（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_nanaco));

        // iD（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_id));

        // QUICPay（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_quicpay));

        // コード決済（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_codetrans));

        // OKICA（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_okica));

    	if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
            // 現金（集計データセット）
            setTransData_Details(_printDataRes.getString(R.string.print_brand_cash));
        }

        if (AppPreference.isServicePos() || AppPreference.isFixedAmountPostalOrder()) {
            // 為替類（集計データセット）
            setTransData_Details(_printDataRes.getString(R.string.print_brand_postal_order));
        }

    	// 和多利（明細データセット）
        setTransData_Details(_printDataRes.getString(R.string.print_brand_watari));

        // プリペイド
        setTransData_Details(_printDataRes.getString(R.string.prepaid_brand));

        // 売上総額対象外
        if(isTransFinished_TotalAmount_NA_flg == true){
            setAlign_Left(_printDataRes.getString(R.string.print_aggregate_TransFinished_TotalAmount_NA),PaintSize_Normal);
        }

        // 改行
        setLF(1,PaintSize_Normal);
    }

    // 明細データセット
    private void setTransData_Details(String BrandName){

        boolean isBrandTrans = false;
        String Date_YYMMDD = " ";
        _slipDataList_Filter = null;

        _slipDataList_Filter = Observable.fromIterable(_slipDataList)
                .filter(sdl -> sdl.transBrand.equals(BrandName))
                .toList()
                .blockingGet();

        for(int i = 0; i < _slipDataList_Filter.size(); i++){
            if(_slipDataList_Filter.get(i).transDate == null || _slipDataList_Filter.get(i).transDate.length() != 19 ||
                    _slipDataList_Filter.get(i).transType == null ||
                    (_slipDataList_Filter.get(i).transType != 0 && _slipDataList_Filter.get(i).transType != 1 && _slipDataList_Filter.get(i).transType != 4 &&
                            _slipDataList_Filter.get(i).transType != 2 && _slipDataList_Filter.get(i).transType != 3 && _slipDataList_Filter.get(i).transType != 5 && _slipDataList_Filter.get(i).transType != 6) ||
                    _slipDataList_Filter.get(i).transAmount == null || _slipDataList_Filter.get(i).transAmount == 0 ||
                    _slipDataList_Filter.get(i).transResult == null || (_slipDataList_Filter.get(i).transResult != PrinterConst.TransResult_OK &&
                    _slipDataList_Filter.get(i).transResult != PrinterConst.TransResult_NG && _slipDataList_Filter.get(i).transResult != PrinterConst.TransResult_UnFinished) ){
                /* 取引日時、取引種別、取引金額、取引結果の内、どれか一つでも問題があった場合、印刷対象外 */
                // 印刷データ異常（想定外）※取引と処理未了の方でログ残しているため、ログ不要
            }else{
                // 【ブランチ名】
                if(isBrandTrans == false){
                    setAlign_Left( "【" + BrandName + "】" ,PaintSize_Normal);
                    isBrandTrans = true;
                }

                // 年月日（・YY/MM/DD）
                if(Date_YYMMDD.equals(_slipDataList_Filter.get(i).transDate.substring(2,10)) == false){
                    Date_YYMMDD = _slipDataList_Filter.get(i).transDate.substring(2,10);
                    setAlign_Left("・" + Date_YYMMDD,PaintSize_Normal);
                }

                // 簡易詳細（時分、取引区別、金額orポイントチャージ数orカード番号、マーク）
                if(_slipDataList_Filter.get(i).transType == 0) {
                    // 取引種別：売上
                    if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_OK){
                        /* 成功 */
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11,16) ,aggregate_payment_amount(_slipDataList_Filter.get(i).transAmount) + "  ",PaintSize_Normal);

                        // 現金併用金額を印字
                        Integer togetherAmount = _slipDataList_Filter.get(i).transCashTogetherAmount;
                        if(AppPreference.isServicePos() && (togetherAmount != 0)) {
                            setAlign_Right("現金　" + aggregate_payment_amount(togetherAmount) + "  ",PaintSize_Normal);
                        }
                    }else if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_UnFinished){
                        /* 未了 */
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11,16) + " " + _printDataRes.getString(R.string.print_aggregate_trans_type_unfinished),aggregate_payment_amount(_slipDataList_Filter.get(i).transAmount) + " *",PaintSize_Normal);
                        isTransFinished_TotalAmount_NA_flg = true;
                    }else{
                        // 必要な場合、追加
                    }
                }else if(_slipDataList_Filter.get(i).transType == 1) {
                    // 取引種別：取消
                    if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_OK){
                        /* 成功 */
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11,16) ,aggregate_cancel_amount(_slipDataList_Filter.get(i).transAmount) + "  ",PaintSize_Normal);

                        // 現金併用金額を印字
                        Integer togetherAmount = _slipDataList_Filter.get(i).transCashTogetherAmount;
                        if(AppPreference.isServicePos() && (togetherAmount != 0)) {
                            setAlign_Right("現金　" + aggregate_cancel_amount(togetherAmount) + "  ",PaintSize_Normal);
                        }
                    }else if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_UnFinished){
                        /* 未了 */
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11,16) + " " + _printDataRes.getString(R.string.print_aggregate_trans_type_unfinished),aggregate_cancel_amount(_slipDataList_Filter.get(i).transAmount) + " *",PaintSize_Normal);
                        isTransFinished_TotalAmount_NA_flg = true;
                    }else{
                        // 必要な場合、追加
                    }
                }else if(_slipDataList_Filter.get(i).transType == 2) {
                    isTransFinished_TotalAmount_NA_flg = true;
                    // 取引種別：ポイント付与
                    if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_OK){
                        /* 成功 */
                        if (Objects.equals(BrandName, _printDataRes.getString(R.string.prepaid_brand))) {
                            setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11, 16), _slipDataList_Filter.get(i).prepaidAddPoint + "P *", PaintSize_Normal);
                        } else {
                            setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11, 16), _slipDataList_Filter.get(i).watariPoint + "P *", PaintSize_Normal);
                        }
                    }else{
                        // 必要な場合、追加
                    }
                }else if(_slipDataList_Filter.get(i).transType == 3) {
                    isTransFinished_TotalAmount_NA_flg = true;
                    // 取引種別：ポイント取消
                    if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_OK){
                        /* 成功 */
                        if (Objects.equals(BrandName, _printDataRes.getString(R.string.prepaid_brand))) {
                            setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11, 16), "△" + _slipDataList_Filter.get(i).prepaidAddPoint + "P *", PaintSize_Normal);
                        } else {
                            setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11, 16), "△" + _slipDataList_Filter.get(i).watariPoint + "P *", PaintSize_Normal);
                        }
                    }else{
                        // 必要な場合、追加
                    }
                }else if(_slipDataList_Filter.get(i).transType == 4) {
                    // 取引種別：現金チャージ
                    if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_OK){
                        /* 成功 */
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11,16) + " " + _printDataRes.getString(R.string.print_aggregate_trans_type_charge),aggregate_charge_amount(_slipDataList_Filter.get(i).transAmount) + " *",PaintSize_Normal);
                        isTransFinished_TotalAmount_NA_flg = true;
                    }else if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_UnFinished){
                        /* 未了 */
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11,16) + " " + _printDataRes.getString(R.string.print_aggregate_trans_type_unfinished_charge),aggregate_charge_amount(_slipDataList_Filter.get(i).transAmount) + " *",PaintSize_Normal);
                        isTransFinished_TotalAmount_NA_flg = true;
                    }else{
                        // 必要な場合、追加
                    }
                }else if(_slipDataList_Filter.get(i).transType == 5) {
                    // 取引種別：現金チャージ取消
                    if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_OK){
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11,16) + " " + _printDataRes.getString(R.string.print_aggregate_trans_type_charge),"△" + aggregate_charge_amount(_slipDataList_Filter.get(i).transAmount) + " *",PaintSize_Normal);
                        isTransFinished_TotalAmount_NA_flg = true;
                    }
                    // 必要な場合、追加
                }else if(_slipDataList_Filter.get(i).transType == 6) {
                    // 取引種別：ポイントチャージ
                    if(_slipDataList_Filter.get(i).transResult == PrinterConst.TransResult_OK){
                        setAlign_LR("　" + _slipDataList_Filter.get(i).transDate.substring(11, 16) + " " + _printDataRes.getString(R.string.print_aggregate_trans_type_point_charge), _slipDataList_Filter.get(i).prepaidAddPoint + "P *", PaintSize_Normal);
                    }
                    // 必要な場合、追加
                }else{
                    // 取引種別：その他
                }
            }

            if(i == (_slipDataList_Filter.size() - 1) && isBrandTrans == true){
                // 改行
                setLF(1,PaintSize_Normal);
            }
        }
    }

    // 取引明細書印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printDetailStatement(int id){
        print_init();

        // 別スレッド：POSサービスレシートデータ取得
        Thread thread = new Thread(() -> {
            _receiptData = DBManager.getReceiptDao().getReceiptsBySlipId(id);
            _slipData  = DBManager.getSlipDao().getOneById(id);

            if (_receiptData == null && _slipData != null) {
                // 取得できないときは通番と取引日時から元のやつを取る
                _receiptData = DBManager.getReceiptDao().getReceiptsByTermSequenceAndTransDate(_slipData.termSequence, _slipData.transDate);
            }
        });
        thread.start();

        try {
            thread.join();

            if (_slipData != null) {
                if (_slipData.printCnt > 0) isRePrinter = true;
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printDetailStatement->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
            }

            if(_receiptData != null) {
                // 取引明細書印刷データセット
                setPrintData_DetailStatement();

                // 印刷
                Printing(_printCanvas);
            }else{
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printDetailStatement->_receiptData <%s>",_printDataRes.getString(R.string.printLog_printDataError), _receiptData);
                PrintDataError();
            }
        } catch (InterruptedException e) {
            Timber.tag("Printer").e("%s：printDetailStatement->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    // 取引明細書印刷データセット
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPrintData_DetailStatement(){
        // 決済日時
        setAlign_Left(_receiptData.trans_date, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 票名
        setLF(1,PaintSize_Normal);
        if(_receiptData.money_type.equals(MoneyType.CASH.toString())) {
            setAlign_Mid("売上明細書", PaintSize_Medium);
        } else {
            setAlign_Mid("取引明細書", PaintSize_Medium);
        }

        // 宛名※インボイス番号設定時のみ
        if(!_receiptData.invoice_no.isEmpty()) {
            setLF(1,PaintSize_Normal);
            setAlign_Right(_printDataRes.getString(R.string.print_recipient),PaintSize_Normal);
            setLine();
        }

        // 加盟店名
        setAlign_Left(_receiptData.merchant_name, PaintSize_Normal);

        // 営業所名/号機番号
        setAlign_Left(_receiptData.organization_name + "/" + _receiptData.car_no + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);

        // 電話番号
        setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), _receiptData.phone_number, PaintSize_Normal);

        // 係員番号
        setAlign_LR(_printDataRes.getString(R.string.print_driver_id), _receiptData.staff_code, PaintSize_Normal);

        // 機器番号
        setAlign_LR(_printDataRes.getString(R.string.print_term_id), _receiptData.terminal_number, PaintSize_Normal);

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _receiptData.term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 商品情報
        try {
            // リスト形式のJSON文字列をリストに変換
            List<ReceiptProductDetail> productDetails = new Gson().fromJson(_receiptData.product_detail, new TypeToken<List<ReceiptProductDetail>>(){}.getType());
            for (int i = 0; i < productDetails.size(); i++) {
                setPrintData_ProductInfo(
                        productDetails.get(i).name,
                        productDetails.get(i).price,
                        productDetails.get(i).count,
                        productDetails.get(i).reducedTax,
                        productDetails.get(i).total,
                        productDetails.get(i).taxType);
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：_receiptData.product_detail e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
        setLine();
        setLF(1,PaintSize_Normal);

        ReceiptSubtotalDetail subtotalDetail = null;
        // 小計
        try {
            // JSON文字列をReceiptSubtotalDetailに変換
            subtotalDetail = new Gson().fromJson(_receiptData.subtotal_detail, ReceiptSubtotalDetail.class);
            setPrintData_SubtotalExcludingTax(false, subtotalDetail);
            setPrintData_SubtotalExcludingTax(true, subtotalDetail);
            setPrintData_SubtotalIncludingTax(false, subtotalDetail);
            setPrintData_SubtotalIncludingTax(true, subtotalDetail);
            setPrintData_SubtotalNonTax(subtotalDetail);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：_receiptData.subtotal_detail e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
        setLF(1,PaintSize_Normal);

        // 合計
        setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_receiptData.total_amount), PaintSize_Medium);
        if(subtotalDetail != null) {
            setPrintData_Total(false, subtotalDetail);
            setPrintData_Total(true, subtotalDetail);
            setPrintData_TotalTax(false, subtotalDetail);
            setPrintData_TotalTax(true, subtotalDetail);
        }
        setLF(1,PaintSize_Normal);

        if(_receiptData.money_type.equals(MoneyType.CASH.toString())) {
            // お釣り
            setPrintData_CashChange(_receiptData.trans_cash_amount, _receiptData.change_amount);
        } else {
            // 支払種別
            setPrintData_TransType(TransMap.TYPE_SALES, _receiptData.money_type, _receiptData.trans_type_amount, _receiptData.trans_cash_amount);
        }

        // メモ欄
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[0],PaintSize_Normal);
        setLF(2,PaintSize_Normal);
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[1],PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // インボイス番号
        if(!_receiptData.invoice_no.isEmpty()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), _receiptData.invoice_no, PaintSize_Normal);
        }
        setLF(1,PaintSize_Normal);
    }

    /**
     * 商品情報の印刷データセット
     * @param name 商品名
     * @param price 単価
     * @param count 数量
     * @param reducedTax 税率(* or 空)
     * @param total 小計
     * @param taxType 税種(込 or 空)
     */
    private void setPrintData_ProductInfo(String name, int price, int count, String reducedTax, int total, String taxType){
        if(0 != total) {
            setAlign_Left(name, PaintSize_Normal);
            setAlign_LR("　" + trans_amount(price) + "ｘ" + count + "点", reducedTax + trans_amount(total) + taxType, PaintSize_Normal);
        }
    }

    /**
     * 小計情報（税抜き)の印刷データセット
     * @param isStandardTaxRate 税率
     * @param subtotalDetail 小計情報詳細
     */
    private void setPrintData_SubtotalExcludingTax(boolean isStandardTaxRate, ReceiptSubtotalDetail subtotalDetail){
        int taxRate,  product,  tax;
        if(isStandardTaxRate) {
            // 標準税率
            taxRate = subtotalDetail.standard_tax_rate;
            product = subtotalDetail.amount_tax_exclusive_standard_without_tax;
            tax = subtotalDetail.amount_tax_exclusive_standard_only_tax;
        } else {
            // 軽減税率
            taxRate = subtotalDetail.reduced_tax_rate;
            product = subtotalDetail.amount_tax_exclusive_reduced_without_tax;
            tax = subtotalDetail.amount_tax_exclusive_reduced_only_tax;
        }

        if(0 != product) {
            setAlign_LR("小計（税抜" + taxRate + "%）", trans_amount(product), PaintSize_Normal);
            setAlign_LR("　　　　消費税等（" + taxRate + "%）", trans_amount(tax), PaintSize_Normal);
        }
    }

    /**
     * 小計情報（税込み)の印刷データセット
     * @param isStandardTaxRate 税率
     * @param subtotalDetail 小計情報詳細
     */
    private void setPrintData_SubtotalIncludingTax(boolean isStandardTaxRate, ReceiptSubtotalDetail subtotalDetail) {
        int taxRate, productAndTax;
        if(isStandardTaxRate) {
            // 標準税率
            taxRate = subtotalDetail.standard_tax_rate;
            productAndTax = subtotalDetail.amount_tax_inclusive_standard;
        } else {
            // 軽減税率
            taxRate = subtotalDetail.reduced_tax_rate;
            productAndTax = subtotalDetail.amount_tax_inclusive_reduced;
        }

        if(0 != productAndTax) {
            setAlign_LR("小計（税込" + taxRate + "%）", trans_amount(productAndTax), PaintSize_Normal);
        }
    }

    /**
     * 小計情報（非課税)の印刷データセット
     * @param subtotalDetail 小計情報詳細
     */
    private void setPrintData_SubtotalNonTax(ReceiptSubtotalDetail subtotalDetail){
        if(0 != subtotalDetail.amount_tax_free) {
            setAlign_LR("小計（非課税）", trans_amount(subtotalDetail.amount_tax_free), PaintSize_Normal);
        }
    }

    /**
     * 合計（商品代 + 税）の印刷データセット
     * @param isStandardTaxRate 税率
     * @param subtotalDetail 小計情報詳細
     */
    private void setPrintData_Total(boolean isStandardTaxRate, ReceiptSubtotalDetail subtotalDetail) {
        int taxRate, total;
        if(isStandardTaxRate) {
            // 標準税率
            taxRate = subtotalDetail.standard_tax_rate;
            total = subtotalDetail.amount_tax_standard;
        } else {
            // 軽減税率
            taxRate = subtotalDetail.reduced_tax_rate;
            total = subtotalDetail.amount_tax_reduced;
        }

        if(0 != total) {
            setAlign_LR("　　（税率" + taxRate + "%対象", trans_amount(total) + "）", PaintSize_Normal);
        }
    }

    /**
     * 合計（税）の印刷データセット
     * @param isStandardTaxRate 税率
     * @param subtotalDetail 小計情報詳細
     */
    private void setPrintData_TotalTax(boolean isStandardTaxRate, ReceiptSubtotalDetail subtotalDetail) {
        int taxRate, total, totalTax;
        if(isStandardTaxRate) {
            // 標準税率
            taxRate = subtotalDetail.standard_tax_rate;
            total = subtotalDetail.amount_tax_standard;
            totalTax = subtotalDetail.amount_tax_standard_only_tax;
        } else {
            // 軽減税率
            taxRate = subtotalDetail.reduced_tax_rate;
            total = subtotalDetail.amount_tax_reduced;
            totalTax = subtotalDetail.amount_tax_reduced_only_tax;
        }

        if(0 != total) {
            setAlign_LR("　　（内消費税等" + taxRate + "%対象", trans_amount(totalTax) + "）", PaintSize_Normal);
        }
    }

    /**
     * 支払種別の印刷データセット
     * @param transType 支払種別
     * @param moneyType マネー種別
     * @param transAmount 支払種別の金額
     * @param cashAmount 現金の金額
     */
    private void setPrintData_TransType(int transType, String moneyType, int transAmount, int cashAmount) {
        // 現金併用時の現金の金額
        if(0 != cashAmount) {
            if (moneyType.equals(MoneyType.POSTALORDER.toString())){
                setAlign_LR(_printDataRes.getString(R.string.print_trans_postal_order_amount), trans_amount(cashAmount), PaintSize_Normal);
            } else {
                setAlign_LR(_printDataRes.getString(R.string.print_trans_cash_together_amount), trans_amount(cashAmount), PaintSize_Normal);
            }
        }

        if(0 != transAmount) {
            String transTypeStr = "";
            if(moneyType.equals(MoneyType.CREDIT.toString()) || moneyType.equals(MoneyType.UNIONPAY.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_brand_credit);
            }else if(moneyType.equals(MoneyType.JR.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_suica_xxx);
            }else if(moneyType.equals(MoneyType.ID.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_iD_xxx);
            }else if(moneyType.equals(MoneyType.QUICKPAY.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_brand_quicpay);
            }else if(moneyType.equals(MoneyType.NANACO.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_nanaco_xxx);
            }else if(moneyType.equals(MoneyType.WAON.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_waon_xxx);
            }else if(moneyType.equals(MoneyType.EDY.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_edy_rakute_xxx);
            }else if(moneyType.equals(MoneyType.QR.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_brand_codetrans);
            }else if(moneyType.equals(MoneyType.OKICA.toString())){
                transTypeStr = _printDataRes.getString(R.string.print_brand_okica);
            }

            if(transType == TransMap.TYPE_SALES) {
                setAlign_LR(transTypeStr + " " + _printDataRes.getString(R.string.print_xxx_payment), trans_amount(transAmount), PaintSize_Normal);
            } else if(transType == TransMap.TYPE_CANCEL) {
                setAlign_LR(transTypeStr + " " + _printDataRes.getString(R.string.print_xxx_cancel), trans_amount(transAmount), PaintSize_Normal);
            }
        }
    }

    /**
     * お釣りの印刷データセット
     * @param cashAmount 現金の金額
     * @param changeAmount お釣りの金額
     */
    private void setPrintData_CashChange(int cashAmount, int changeAmount) {
        setAlign_LR("お預り", trans_amount(cashAmount + changeAmount), PaintSize_Normal);
        if(0 != changeAmount) {
            setAlign_LR("お釣", trans_amount(changeAmount), PaintSize_Normal);
        }
    }

    // 取消票印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printCancelTicket(int slipId){
        print_init();

        // 別スレッド：POSサービスレシートデータ取得
        Thread thread = new Thread(() -> {
            _receiptData  = DBManager.getReceiptDao().getReceiptsBySlipId(slipId);
            _slipData  = DBManager.getSlipDao().getOneById(slipId);

            if (_receiptData == null && _slipData != null) {
                // 取得できないときは通番と取引日時から元のやつを取る
                _receiptData = DBManager.getReceiptDao().getReceiptsByTermSequenceAndTransDate(_slipData.termSequence, _slipData.transDate);
            }
        });
        thread.start();

        try {
            thread.join();

            if (_slipData != null) {
                if (_slipData.printCnt > 0) isRePrinter = true;
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printCancelTicket->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
            }

            if(_receiptData != null) {
                // 取消票印刷データセット
                setPrintData_CancelTicket();

                // 印刷
                Printing(_printCanvas);
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printCancelTicket->_receiptData <%s>",_printDataRes.getString(R.string.printLog_printDataError), _receiptData);
                PrintDataError();
            }
        } catch (InterruptedException e) {
            Timber.tag("Printer").e("%s：printCancelTicket->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    // 取消票印刷データセット
    private void setPrintData_CancelTicket() {
        // 決済日時
        setAlign_Left(_receiptData.trans_date, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 票名
        setLF(1,PaintSize_Normal);
        setLine_emphasize();
        setAlign_Mid(_printDataRes.getString(R.string.print_cancel_slip), PaintSize_Medium);
        setLine_emphasize();

        // 加盟店名
        setAlign_Left(_receiptData.merchant_name, PaintSize_Normal);

        // 営業所名/号機番号
        setAlign_Left(_receiptData.organization_name + "/" + _receiptData.car_no + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);

        // 電話番号
        setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), _receiptData.phone_number, PaintSize_Normal);

        // 係員番号
        setAlign_LR(_printDataRes.getString(R.string.print_driver_id), _receiptData.staff_code, PaintSize_Normal);

        // 機器番号
        setAlign_LR(_printDataRes.getString(R.string.print_term_id), _receiptData.terminal_number, PaintSize_Normal);

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _receiptData.term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);
        setLine();
        setLF(1,PaintSize_Normal);

        // 取消対象
        setAlign_Left("取消対象", PaintSize_Normal);

        // 決済日時
        if(_receiptData.old_trans_date != null && _receiptData.old_trans_date.length() == 19){
            setAlign_LR(_receiptData.old_trans_date.substring(0,10), _receiptData.old_trans_date.substring(11,19),PaintSize_Normal);
        }else{
            setAlign_LR(null,null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_CancelTicket->_receiptData.old_trans_date <%s>",_printDataRes.getString(R.string.printLog_printDataError), _receiptData.old_trans_date);
        }

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _receiptData.old_term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 取消合計
        setAlign_LR(_printDataRes.getString((R.string.print_trans_cancel)) + _printDataRes.getString(R.string.print_total_amount), trans_amount(_receiptData.total_amount), PaintSize_Medium);
        setLF(1,PaintSize_Normal);

        // 支払種別
        setPrintData_TransType(TransMap.TYPE_CANCEL, _receiptData.money_type, _receiptData.trans_type_amount, _receiptData.trans_cash_amount);

        // メモ欄
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[2],PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // インボイス番号
        if(!_receiptData.invoice_no.isEmpty()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), _receiptData.invoice_no, PaintSize_Normal);
        }
        setLF(1,PaintSize_Normal);
    }

    // 領収書印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printReceipt(int id, boolean isDetail){
        print_init();

        // 別スレッド：POSサービスレシートデータ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _receiptData  = DBManager.getReceiptDao().getReceiptsBySlipId(id);

                if (_receiptData == null) {
                    // slipDataを取得して、取引日時と通番からreceiptDataを取得
                    SlipData slipData = DBManager.getSlipDao().getOneById(id);
                    if (slipData != null) {
                        _receiptData = DBManager.getReceiptDao().getReceiptsByTermSequenceAndTransDate(slipData.termSequence, slipData.transDate);
                    }
                }
            }
        });
        thread.start();

        try {
            thread.join();

            if(_receiptData != null) {
                boolean isReprinting;
                if(0 == _receiptData.print_cnt) {
                    isReprinting = false;
                } else {
                    isReprinting = true;
                }
                isRePrinter = isReprinting;

                // 領収書共通部印刷データセット
                setPrintData_Receipt_Common(id, isReprinting);

                if(isDetail){
                    // 領収書詳細部印刷データセット
                    setPrintData_Receipt_Detail(id);
                }

                // 担当者押印欄印刷データセット
                setPrintData_Receipt_Stamp();

                // 印刷
                Printing(_printCanvas);
            }else{
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTrans->_receiptData <%s>",_printDataRes.getString(R.string.printLog_printDataError), _receiptData);
                PrintDataError();
            }
        } catch (InterruptedException e) {
            Timber.tag("Printer").e("%s：printTrans->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    // 領収書（共通部印刷データセット）
    private void setPrintData_Receipt_Common(int id, boolean isReprint){
        // 取引日時
        setAlign_Left(_receiptData.trans_date, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 票名
        if(isReprint) {
            setAlign_Mid(_printDataRes.getString(R.string.print_receipt) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Big);
        } else {
            setAlign_Mid(_printDataRes.getString(R.string.print_receipt), PaintSize_Big);
        }
        setLF(1,PaintSize_Normal);

        // 宛名欄
        setAlign_Right(_printDataRes.getString(R.string.print_receipt_name_column),PaintSize_Medium);
        setLF(1,PaintSize_Normal);

        // 合計金額
        setAlign_LR(_printDataRes.getString(R.string.print_receipt_total_amount), trans_amount(_receiptData.total_amount) + _printDataRes.getString(R.string.print_receipt_total_amount_end),PaintSize_Medium);
        setLine();

        // 税率毎の小計、税額
        try {
            // JSON文字列をReceiptSubtotalDetailに変換
            ReceiptSubtotalDetail subtotalDetail = new Gson().fromJson(_receiptData.subtotal_detail, ReceiptSubtotalDetail.class);
            setPrintData_Total(false, subtotalDetail);
            setPrintData_Total(true, subtotalDetail);
            setPrintData_TotalTax(false, subtotalDetail);
            setPrintData_TotalTax(true, subtotalDetail);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：_receiptData.subtotal_detail e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
        setLF(1,PaintSize_Normal);

        // 但書き欄
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_proviso_column)[0], PaintSize_Medium);
        setLF(1,PaintSize_Normal);
        setAlign_LR(_printDataRes.getStringArray(R.array.print_receipt_proviso_column)[1],_printDataRes.getStringArray(R.array.print_receipt_proviso_column)[2],PaintSize_Normal);
        setLine();
        setLF(1,PaintSize_Normal);

        // 加盟店名
        setAlign_Left(_receiptData.merchant_name, PaintSize_Normal);

        // 営業所名/号機番号
        setAlign_Left(_receiptData.organization_name + "/" + _receiptData.car_no + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);

        // 住所
        setAlign_Left(_receiptData.address, PaintSize_Normal);

        // 電話番号
        setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), _receiptData.phone_number, PaintSize_Normal);

        // インボイス番号
        if(!_receiptData.invoice_no.isEmpty()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), _receiptData.invoice_no, PaintSize_Normal);
        }
        setLF(1,PaintSize_Normal);
    }

    // 領収書（明細部印刷データセット）
    private void setPrintData_Receipt_Detail(int id){
        // 票名
        setLine();
        setAlign_Mid(_printDataRes.getString(R.string.print_receipt_detail), PaintSize_Big);
        setLF(1,PaintSize_Normal);

        // 係員番号
        setAlign_LR(_printDataRes.getString(R.string.print_driver_id), _receiptData.staff_code, PaintSize_Normal);

        // 機器番号
        setAlign_LR(_printDataRes.getString(R.string.print_term_id), _receiptData.terminal_number, PaintSize_Normal);

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _receiptData.term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 商品情報
        try {
            // リスト形式のJSON文字列をリストに変換
            List<ReceiptProductDetail> productDetails = new Gson().fromJson(_receiptData.product_detail, new TypeToken<List<ReceiptProductDetail>>(){}.getType());
            for (int i = 0; i < productDetails.size(); i++) {
                setPrintData_ProductInfo(
                        productDetails.get(i).name,
                        productDetails.get(i).price,
                        productDetails.get(i).count,
                        productDetails.get(i).reducedTax,
                        productDetails.get(i).total,
                        productDetails.get(i).taxType);
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：_receiptData.product_detail e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
        setLine();

        // 合計
        setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_receiptData.total_amount), PaintSize_Medium);

        // 税率毎の小計、税額
        try {
            // JSON文字列をReceiptSubtotalDetailに変換
            ReceiptSubtotalDetail subtotalDetail = new Gson().fromJson(_receiptData.subtotal_detail, ReceiptSubtotalDetail.class);
            setPrintData_Total(false, subtotalDetail);
            setPrintData_Total(true, subtotalDetail);
            setPrintData_TotalTax(false, subtotalDetail);
            setPrintData_TotalTax(true, subtotalDetail);
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：_receiptData.subtotal_detail e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
        setLF(1,PaintSize_Normal);

        if(_receiptData.money_type.equals(MoneyType.CASH.toString())) {
            // お釣り
            setPrintData_CashChange(_receiptData.trans_cash_amount, _receiptData.change_amount);
        } else {
            // 支払種別
            setPrintData_TransType(TransMap.TYPE_SALES, _receiptData.money_type, _receiptData.trans_type_amount, _receiptData.trans_cash_amount);
        }

        // メモ
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[0],PaintSize_Normal);
        setLF(1,PaintSize_Normal);
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[1],PaintSize_Normal);
        setLF(1,PaintSize_Normal);
    }

    /**
     * 担当者押印欄印刷データセット
     */
    private void setPrintData_Receipt_Stamp() {
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[0],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[1],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[1],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[1],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[1],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[1],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[1],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[1],PaintSize_Normal);
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_stamp)[2],PaintSize_Normal);
        setLF(1,PaintSize_Normal);
    }

    /** ▼チケット販売時の伝票データ作成▼ **/
    // チケット販売時の取引明細書印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTicketDetailStatement(int id){
        print_init();

        // 別スレッド：チケットサービスレシートデータ取得
        Thread thread = new Thread(() -> {
            _ticketReceiptData = DBManager.getTicketReceiptDao().getReceiptsBySlipId(id);
            _slipData  = DBManager.getSlipDao().getOneById(id);

            if (_ticketReceiptData == null && _slipData != null) {
                // 取得できないときは通番と取引日時から元のやつをとる
                _ticketReceiptData = DBManager.getTicketReceiptDao().getReceiptsByTermSequenceAndTransDate(_slipData.termSequence, _slipData.transDate);
                isRecovery = true;
            }
        });
        thread.start();

        try {
            thread.join();

            if (_slipData != null) {
                if (_slipData.printCnt > 0) isRePrinter = true;
                // 障害復旧時は再印刷と見なす
                if (isRecovery) {
                    isRePrinter = true;
                    // もう不要なのでフラグは戻す
                    isRecovery = false;
                }
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printDetailStatement->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
            }

            if (_ticketReceiptData != null) {
                // チケット販売時の取引明細書印刷データセット
                setPrintData_TicketDetailStatement();
                // 印刷
                Printing(_printCanvas);
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTicketDetailStatement->_ticketReceiptData <%s>",_printDataRes.getString(R.string.printLog_printDataError), _ticketReceiptData);
                PrintDataError();
            }
        } catch (InterruptedException e) {
            Timber.tag("Printer").e("%s：printTicketDetailStatement->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    // チケット販売時の取引明細書印刷データセット
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setPrintData_TicketDetailStatement(){
        Log_BrandName = getBrandName(_ticketReceiptData.money_type);

        // 決済日時
        setAlign_Left(_ticketReceiptData.trans_date, PaintSize_Normal);
        setLF(1,PaintSize_Normal);
        Log_transDate = _ticketReceiptData.trans_date;

        // 票名
        setLF(1,PaintSize_Normal);
        if (_ticketReceiptData.money_type.equals(MoneyType.CASH.toString())) {
            setAlign_Mid("売上明細書", PaintSize_Medium);
            Log_SlipName = "売上明細書";
        } else {
            setAlign_Mid("取引明細書", PaintSize_Medium);
            Log_SlipName = "取引明細書";
        }

        // 加盟店名
        setAlign_Left(_ticketReceiptData.merchant_name, PaintSize_Normal);

        // 営業所名/号機番号
        setAlign_Left(_ticketReceiptData.organization_name + "/" + _ticketReceiptData.car_no + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);

        // 電話番号
        setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), _ticketReceiptData.phone_number, PaintSize_Normal);

        // 係員番号
        setAlign_LR(_printDataRes.getString(R.string.print_driver_id), _ticketReceiptData.staff_code, PaintSize_Normal);

        // 機器番号
        setAlign_LR(_printDataRes.getString(R.string.print_term_id), _ticketReceiptData.terminal_number, PaintSize_Normal);

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _ticketReceiptData.term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // チケット情報
        try {
            // リスト形式のJSON文字列をリストに変換
            List<TicketReceiptDetail> ticketReceiptDetail = new Gson().fromJson(_ticketReceiptData.ticket_detail, new TypeToken<List<TicketReceiptDetail>>(){}.getType());
            for (int i = 0; i < ticketReceiptDetail.size(); i++) {
                setPrintData_TicketInfo(
                        ticketReceiptDetail.get(i).categoryType,
                        ticketReceiptDetail.get(i).price,
                        ticketReceiptDetail.get(i).count,
                        ticketReceiptDetail.get(i).reducedTax,
                        ticketReceiptDetail.get(i).total,
                        ticketReceiptDetail.get(i).taxType);
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：setPrintData_TicketDetailStatement->_ticketReceiptData.ticket_detail e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
        setLine();
        setLF(1,PaintSize_Normal);

        // 小計
        String taxRate = String.format("小計（税込%s%%）", AppPreference.getreceiptTax());
        setAlign_LR(taxRate, trans_amount(_ticketReceiptData.total_amount), PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 合計
        setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_ticketReceiptData.total_amount), PaintSize_Medium);
        setLF(1,PaintSize_Normal);

        if (_ticketReceiptData.money_type.equals(MoneyType.CASH.toString())) {
            // お釣り
            setPrintData_CashChange(_ticketReceiptData.trans_cash_amount, _ticketReceiptData.change_amount);
        } else {
            // 支払種別
            setPrintData_TransType(TransMap.TYPE_SALES, _ticketReceiptData.money_type, _ticketReceiptData.trans_type_amount, _ticketReceiptData.trans_cash_amount);
        }

        // メモ欄
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[0],PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // インボイス番号
        if (!_ticketReceiptData.invoice_no.isEmpty()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), _ticketReceiptData.invoice_no, PaintSize_Normal);
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_TicketDetailStatement->_ticketReceiptData.invoice_no <%s>",_printDataRes.getString(R.string.printLog_printDataError), _ticketReceiptData.invoice_no);
        }
        setLF(1,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s %s 取引日時(%s)",_printDataRes.getString(R.string.printLog_printDataSet), Log_BrandName, Log_SlipName, Log_transDate);

        /* 合計 */
        Log_Amounts = String.format("合計:%s ", _ticketReceiptData.total_amount);
        /* 取引種別金額 */
        Log_Amounts += String.format("取引種別金額:%s ", _ticketReceiptData.trans_type_amount);
        /* 現金支払金額 */
        Log_Amounts += String.format("現金支払金額:%s ", _ticketReceiptData.trans_cash_amount);
        /* お釣り */
        Log_Amounts += String.format("お釣り:%s ", _ticketReceiptData.change_amount);
        Timber.tag("Printer").i("%s( %s)",_printDataRes.getString(R.string.printLog_printDataAmounts), Log_Amounts);
    }

    /**
     * チケット情報の印刷データセット
     * @param name カテゴリ名
     * @param price 単価
     * @param count 数量
     * @param reducedTax 税率(空)
     * @param total 小計
     * @param taxType 税種(込)
     */
    private void setPrintData_TicketInfo(String name, int price, int count, String reducedTax, int total, String taxType){
        String strReducedTax = "";
        if (reducedTax.equals("")) {
            strReducedTax = "空";
        } else {
            strReducedTax = reducedTax;
        }

        if (0 <= total) {
            setAlign_Left(name, PaintSize_Normal);
            setAlign_LR("　" + trans_amount(price) + "ｘ" + count, reducedTax + trans_amount(total) + taxType, PaintSize_Normal);
            Timber.tag("Printer").i("%s：チケット情報 (カテゴリ名:%s 単価:%s 数量:%s 税率:%s 小計:%s 税種:%s)",_printDataRes.getString(R.string.printLog_printDataSet),
                    name, price, count, strReducedTax, total, taxType);
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：チケット情報 (カテゴリ名:%s 単価:%s 数量:%s 税率:%s 小計:%s 税種:%s)",_printDataRes.getString(R.string.printLog_printDataError),
                    name, price, count, strReducedTax, total, taxType);
        }
    }

    // チケット販売時の取消票印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTicketCancel(int slipId){
        print_init();

        // 別スレッド：チケットサービスレシートデータ取得
        Thread thread = new Thread(() -> {
            _ticketReceiptData = DBManager.getTicketReceiptDao().getReceiptsBySlipId(slipId);
            _slipData  = DBManager.getSlipDao().getOneById(slipId);

            if (_ticketReceiptData == null && _slipData != null) {
                // 取得できないときは通番と取引日時から元のやつをとる
                _ticketReceiptData = DBManager.getTicketReceiptDao().getReceiptsByTermSequenceAndTransDate(_slipData.termSequence, _slipData.transDate);
            }
        });
        thread.start();

        try {
            thread.join();

            if (_slipData != null) {
                if (_slipData.printCnt > 0) isRePrinter = true;
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTicketCancel->_slipData <%s>", _printDataRes.getString(R.string.printLog_printDataError), _slipData);
            }

            if(_ticketReceiptData != null) {
                // チケット販売時の取消票印刷データセット
                setPrintData_TicketCancel();
                // 印刷
                Printing(_printCanvas);
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTicketCancel->_ticketReceiptData <%s>",_printDataRes.getString(R.string.printLog_printDataError), _ticketReceiptData);
                PrintDataError();
            }
        } catch (InterruptedException e) {
            Timber.tag("Printer").e("%s：printTicketCancel->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    // チケット販売時の取消票印刷データセット
    private void setPrintData_TicketCancel() {
        Log_BrandName = getBrandName(_ticketReceiptData.money_type);

        // 決済日時
        setAlign_Left(_ticketReceiptData.trans_date, PaintSize_Normal);
        setLF(1,PaintSize_Normal);
        Log_transDate = _ticketReceiptData.trans_date;

        // 票名
        setLF(1,PaintSize_Normal);
        setLine_emphasize();
        setAlign_Mid(_printDataRes.getString(R.string.print_cancel_slip), PaintSize_Medium);
        setLine_emphasize();
        Log_SlipName = _printDataRes.getString(R.string.print_cancel_slip);

        // 加盟店名
        setAlign_Left(_ticketReceiptData.merchant_name, PaintSize_Normal);

        // 営業所名/号機番号
        setAlign_Left(_ticketReceiptData.organization_name + "/" + _ticketReceiptData.car_no + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);

        // 電話番号
        setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), _ticketReceiptData.phone_number, PaintSize_Normal);

        // 係員番号
        setAlign_LR(_printDataRes.getString(R.string.print_driver_id), _ticketReceiptData.staff_code, PaintSize_Normal);

        // 機器番号
        setAlign_LR(_printDataRes.getString(R.string.print_term_id), _ticketReceiptData.terminal_number, PaintSize_Normal);

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _ticketReceiptData.term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);
        setLine();
        setLF(1,PaintSize_Normal);

        // 取消対象
        setAlign_Left("取消対象", PaintSize_Normal);

        // 決済日時
        if (_ticketReceiptData.old_trans_date != null && _ticketReceiptData.old_trans_date.length() == 19) {
            setAlign_LR(_ticketReceiptData.old_trans_date.substring(0,10), _ticketReceiptData.old_trans_date.substring(11,19),PaintSize_Normal);
        } else {
            setAlign_LR(null,null, PaintSize_Normal);
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_TicketCancel->_ticketReceiptData.old_trans_date <%s>",_printDataRes.getString(R.string.printLog_printDataError), _ticketReceiptData.old_trans_date);
        }

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _ticketReceiptData.old_term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 取消合計
        setAlign_LR(_printDataRes.getString((R.string.print_trans_cancel)) + _printDataRes.getString(R.string.print_total_amount), trans_amount(_ticketReceiptData.total_amount), PaintSize_Medium);
        setLF(1,PaintSize_Normal);

        // 支払種別
        setPrintData_TransType(TransMap.TYPE_CANCEL, _ticketReceiptData.money_type, _ticketReceiptData.trans_type_amount, _ticketReceiptData.trans_cash_amount);

        // メモ欄
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[2],PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // インボイス番号
        if (!_ticketReceiptData.invoice_no.isEmpty()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), _ticketReceiptData.invoice_no, PaintSize_Normal);
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_TicketCancel->_ticketReceiptData.invoice_no <%s>",_printDataRes.getString(R.string.printLog_printDataError), _ticketReceiptData.invoice_no);
        }
        setLF(1,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s %s 取引日時(%s)",_printDataRes.getString(R.string.printLog_printDataSet), Log_BrandName, Log_SlipName, Log_transDate);

        /* 合計 */
        Log_Amounts = String.format("合計:%s ", _ticketReceiptData.total_amount);
        /* 取引種別金額 */
        Log_Amounts += String.format("取引種別金額:%s ", _ticketReceiptData.trans_type_amount);
        /* 現金支払金額 */
        Log_Amounts += String.format("現金支払金額:%s ", _ticketReceiptData.trans_cash_amount);
        /* お釣り */
        Log_Amounts += String.format("お釣り:%s ", _ticketReceiptData.change_amount);
        Timber.tag("Printer").i("%s( %s)",_printDataRes.getString(R.string.printLog_printDataAmounts), Log_Amounts);
    }

    private String getBrandName(String moneyType) {
        String name = "";
        if (moneyType.equals(MoneyType.CREDIT.toString()) || moneyType.equals(MoneyType.UNIONPAY.toString())) {
            name = _printDataRes.getString(R.string.print_brand_credit);
        } else if (moneyType.equals(MoneyType.JR.toString())) {
            name = _printDataRes.getString(R.string.print_suica_xxx);
        } else if (moneyType.equals(MoneyType.ID.toString())) {
            name = _printDataRes.getString(R.string.print_iD_xxx);
        } else if (moneyType.equals(MoneyType.QUICKPAY.toString())) {
            name = _printDataRes.getString(R.string.print_brand_quicpay);
        } else if (moneyType.equals(MoneyType.NANACO.toString())) {
            name = _printDataRes.getString(R.string.print_nanaco_xxx);
        } else if (moneyType.equals(MoneyType.WAON.toString())) {
            name = _printDataRes.getString(R.string.print_waon_xxx);
        } else if (moneyType.equals(MoneyType.EDY.toString())) {
            name = _printDataRes.getString(R.string.print_edy_rakute_xxx);
        } else if (moneyType.equals(MoneyType.QR.toString())) {
            name = _printDataRes.getString(R.string.print_brand_codetrans);
        } else if (moneyType.equals(MoneyType.OKICA.toString())) {
            name = _printDataRes.getString(R.string.print_brand_okica);
        } else if (moneyType.equals(MoneyType.CASH.toString())) {
            name = _printDataRes.getString(R.string.print_brand_cash);
        } else {
            name = moneyType;
        }
        return name;
    }

    // チケット販売時の領収書印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printTicketReceipt(int id, boolean isDetail){
        print_init();

        // 別スレッド：チケットサービスレシートデータ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _ticketReceiptData = DBManager.getTicketReceiptDao().getReceiptsBySlipId(id);

                if (_ticketReceiptData == null) {
                    // slipDataを取得して、取引日時と通番からtickerReceiptDataを取得
                    SlipData slipData = DBManager.getSlipDao().getOneById(id);
                    if (slipData != null) {
                        _ticketReceiptData = DBManager.getTicketReceiptDao().getReceiptsByTermSequenceAndTransDate(slipData.termSequence, slipData.transDate);
                    }
                }
            }
        });
        thread.start();

        try {
            thread.join();

            if(_ticketReceiptData != null) {
                if (_ticketReceiptData.print_cnt > 0) isRePrinter = true;

                // 領収書
                setPrintData_TicketReceipt(id, isDetail);

                if (isDetail) {
                    // 領収書明細
                    setPrintData_TicketReceipt_Detail(id);
                }

                // 担当者押印欄
                setPrintData_Receipt_Stamp();

                // 印刷
                Printing(_printCanvas);
            }else{
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printTicketReceipt->_ticketReceiptData <%s>",_printDataRes.getString(R.string.printLog_printDataError), _receiptData);
                PrintDataError();
            }
        } catch (InterruptedException e) {
            Timber.tag("Printer").e("%s：printTicketReceipt->Exception e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            PrintDataError();
            e.printStackTrace();
        }
    }

    // チケット販売時の領収書印刷データセット
    private void setPrintData_TicketReceipt(int id, boolean isDetail){
        Log_BrandName = getBrandName(_ticketReceiptData.money_type);

        // 取引日時
        setAlign_Left(_ticketReceiptData.trans_date, PaintSize_Normal);
        setLF(1,PaintSize_Normal);
        Log_transDate = _ticketReceiptData.trans_date;

        // 票名
        if (0 == _ticketReceiptData.print_cnt) {
            setAlign_Mid(_printDataRes.getString(R.string.print_receipt), PaintSize_Big);
            Log_SlipName = _printDataRes.getString(R.string.print_receipt);
        } else {
            setAlign_Mid(_printDataRes.getString(R.string.print_receipt) + _printDataRes.getString(R.string.print_slip_again), PaintSize_Big);
            Log_SlipName = _printDataRes.getString(R.string.print_receipt) + _printDataRes.getString(R.string.print_slip_again);
        }
        setLF(1,PaintSize_Normal);

        // 宛名欄
        setAlign_Right(_printDataRes.getString(R.string.print_receipt_name_column),PaintSize_Medium);
        setLF(1,PaintSize_Normal);

        // 領収金額
        setAlign_LR(_printDataRes.getString(R.string.print_receipt_total_amount), trans_amount(_ticketReceiptData.total_amount) + _printDataRes.getString(R.string.print_receipt_total_amount_end),PaintSize_Medium);
        setLine();
        setLF(1,PaintSize_Normal);

        // 但書き欄
        setAlign_Mid(_printDataRes.getStringArray(R.array.print_receipt_proviso_column)[0], PaintSize_Medium);
        setLF(1,PaintSize_Normal);
        setAlign_LR(_printDataRes.getStringArray(R.array.print_receipt_proviso_column)[1],_printDataRes.getStringArray(R.array.print_receipt_proviso_column)[2],PaintSize_Normal);
        setLine();
        setLF(1,PaintSize_Normal);

        // 加盟店名
        setAlign_Left(_ticketReceiptData.merchant_name, PaintSize_Normal);

        // 営業所名/号機番号
        setAlign_Left(_ticketReceiptData.organization_name + "/" + _ticketReceiptData.car_no + _printDataRes.getString(R.string.print_car_id), PaintSize_Normal);

        // 住所
        if (_ticketReceiptData.address != null && !_ticketReceiptData.address.equals("")) {
            setAlign_Left(_ticketReceiptData.address, PaintSize_Normal);
        }

        // 電話番号
        setAlign_LR(_printDataRes.getString(R.string.print_merchant_tel_number), _ticketReceiptData.phone_number, PaintSize_Normal);

        // インボイス番号
        if (!_ticketReceiptData.invoice_no.isEmpty()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), _ticketReceiptData.invoice_no, PaintSize_Normal);
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_TicketReceipt->_ticketReceiptData.invoice_no <%s>",_printDataRes.getString(R.string.printLog_printDataError), _ticketReceiptData.invoice_no);
        }
        setLF(1,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s %s 取引日時(%s)",_printDataRes.getString(R.string.printLog_printDataSet), Log_BrandName, Log_SlipName, Log_transDate);

        if (!isDetail) {
            /* 合計 */
            Log_Amounts = String.format("合計:%s ", _ticketReceiptData.total_amount);
            /* 取引種別金額 */
            Log_Amounts += String.format("取引種別金額:%s ", _ticketReceiptData.trans_type_amount);
            /* 現金支払金額 */
            Log_Amounts += String.format("現金支払金額:%s ", _ticketReceiptData.trans_cash_amount);
            /* お釣り */
            Log_Amounts += String.format("お釣り:%s ", _ticketReceiptData.change_amount);
            Timber.tag("Printer").i("%s( %s)",_printDataRes.getString(R.string.printLog_printDataAmounts), Log_Amounts);
        }
    }

    // チケット販売時の領収書明細印刷データセット
    private void setPrintData_TicketReceipt_Detail(int id){
        Log_BrandName = getBrandName(_ticketReceiptData.money_type);
        Log_transDate = _ticketReceiptData.trans_date;

        // 票名
        setLine();
        setAlign_Mid(_printDataRes.getString(R.string.print_receipt_detail), PaintSize_Big);
        setLF(1,PaintSize_Normal);
        Log_SlipName = _printDataRes.getString(R.string.print_receipt_detail);

        // 係員番号
        setAlign_LR(_printDataRes.getString(R.string.print_driver_id), _ticketReceiptData.staff_code, PaintSize_Normal);

        // 機器番号
        setAlign_LR(_printDataRes.getString(R.string.print_term_id), _ticketReceiptData.terminal_number, PaintSize_Normal);

        // 機器通番
        setAlign_LR(_printDataRes.getString(R.string.print_term_sequence), _ticketReceiptData.term_sequence, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // チケット情報
        try {
            // リスト形式のJSON文字列をリストに変換
            List<TicketReceiptDetail> ticketReceiptDetail = new Gson().fromJson(_ticketReceiptData.ticket_detail, new TypeToken<List<TicketReceiptDetail>>(){}.getType());
            for (int i = 0; i < ticketReceiptDetail.size(); i++) {
                setPrintData_TicketInfo(
                        ticketReceiptDetail.get(i).categoryType,
                        ticketReceiptDetail.get(i).price,
                        ticketReceiptDetail.get(i).count,
                        ticketReceiptDetail.get(i).reducedTax,
                        ticketReceiptDetail.get(i).total,
                        ticketReceiptDetail.get(i).taxType);
            }
        } catch (Exception e) {
            Timber.tag("Printer").e("%s：setPrintData_TicketReceipt_Detail->_ticketReceiptData.ticket_detail e <%s>",_printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }
        setLine();
        setLF(1,PaintSize_Normal);

        // 小計
        String taxRate = String.format("小計（税込%s%%）", AppPreference.getreceiptTax());
        setAlign_LR(taxRate, trans_amount(_ticketReceiptData.total_amount), PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 合計
        setAlign_LR(_printDataRes.getString(R.string.print_total_amount), trans_amount(_ticketReceiptData.total_amount), PaintSize_Medium);
        setLF(1,PaintSize_Normal);

        if (_ticketReceiptData.money_type.equals(MoneyType.CASH.toString())) {
            // お釣り
            setPrintData_CashChange(_ticketReceiptData.trans_cash_amount, _ticketReceiptData.change_amount);
        } else {
            // 支払種別
            setPrintData_TransType(TransMap.TYPE_SALES, _ticketReceiptData.money_type, _ticketReceiptData.trans_type_amount, _ticketReceiptData.trans_cash_amount);
        }

        // メモ欄
        setAlign_Left(_printDataRes.getStringArray(R.array.print_memo)[0],PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // インボイス番号
        if (!_ticketReceiptData.invoice_no.isEmpty()) {
            setAlign_LR(_printDataRes.getString(R.string.print_invoice_number), _ticketReceiptData.invoice_no, PaintSize_Normal);
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：setPrintData_TicketDetailStatement->_ticketReceiptData.invoice_no <%s>",_printDataRes.getString(R.string.printLog_printDataError), _ticketReceiptData.invoice_no);
        }
        setLF(1,PaintSize_Normal);
        Timber.tag("Printer").i("%s：%s %s 取引日時(%s)",_printDataRes.getString(R.string.printLog_printDataSet), Log_BrandName, Log_SlipName, Log_transDate);

        /* 合計 */
        Log_Amounts = String.format("合計:%s ", _ticketReceiptData.total_amount);
        /* 取引種別金額 */
        Log_Amounts += String.format("取引種別金額:%s ", _ticketReceiptData.trans_type_amount);
        /* 現金支払金額 */
        Log_Amounts += String.format("現金支払金額:%s ", _ticketReceiptData.trans_cash_amount);
        /* お釣り */
        Log_Amounts += String.format("お釣り:%s ", _ticketReceiptData.change_amount);
        Timber.tag("Printer").i("%s( %s)",_printDataRes.getString(R.string.printLog_printDataAmounts), Log_Amounts);
    }

    // チケット販売時のQR券印刷命令
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printQRTicketReceipt() {
        print_init();
        isQRTicketPrintSts = QRTicketPrintSts.INFO.ordinal();
        _dynamicTicketItem = AppPreference.getDynamicTicketItem();

        if (_dynamicTicketItem != null) {
            setPrintData_QRTicketReceipt();
            // 印刷
            Printing(_printCanvas);
        } else {
            // 印刷データ異常（想定外）
            Timber.tag("Printer").e("%s：printQRTicketReceipt->dynamicTicket = null",_printDataRes.getString(R.string.printLog_printDataError));
            PrintDataError();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void printQRTicketReceipt(int Sts){
        print_init();
        isQRTicketPrintSts = Sts;
        setPrintData_QRTicketReceipt();
        // 印刷
        if (isQRTicketPrintSts == QRTicketPrintSts.QRCODE.ordinal()) {
            if (_printBitmap != null) {
                // QRコード
                _printTask.setGray(200);
                Printing(_printBitmap);
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：printQRTicketReceipt->_printBitmap = null",_printDataRes.getString(R.string.printLog_printDataError));
                printQRTicketReceipt(QRTicketPrintSts.CATEGORY.ordinal());
            }
        } else {
            Printing(_printCanvas);
        }
    }

    // QR券印刷データセット
    private void setPrintData_QRTicketReceipt() {

        if (isQRTicketPrintSts == QRTicketPrintSts.INFO.ordinal()) {

            String merchantName = AppPreference.getMerchantName();
            String ticketName = _dynamicTicketItem.ticket_name;
            String embarkName = _dynamicTicketItem.stop_name;
            String date = _dynamicTicketItem.reservation_date;
            String time = _dynamicTicketItem.departure_time;

            // 加盟店名
            setLF(1,PaintSize_Normal);
            setAlign_Mid(merchantName, PaintSize_Normal);
            setLF(1,PaintSize_Normal);
            if (merchantName == null || merchantName.equals("")) {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->merchantName = %s",_printDataRes.getString(R.string.printLog_printDataError), merchantName);
            }
            Log_QRTicket = String.format("加盟店名=[%s] ", merchantName);

            // チケット名
            setAlign_Mid(ticketName, PaintSize_Normal);
            if (ticketName == null || ticketName.equals("")) {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->ticketName = %s",_printDataRes.getString(R.string.printLog_printDataError), ticketName);
            }
            Log_QRTicket += String.format("チケット名=[%s] ", ticketName);

            // のりば名
            setAlign_Mid(embarkName, PaintSize_Normal);
            if (embarkName == null || embarkName.equals("")) {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->embarkName = %s",_printDataRes.getString(R.string.printLog_printDataError), embarkName);
            }
            Log_QRTicket += String.format("のりば名=[%s] ", embarkName);

            // 便情報（出発時刻）
            if (date != null && date.length() == 8 && time != null && time.length() >= 5) {
                String dateTime = String.format("%s/%s/%s %s:%s発", date.substring(2,4), date.substring(4,6), date.substring(6,8), time.substring(0,2), time.substring(3,5));
                setAlign_Mid(dateTime, PaintSize_Normal);
                Log_QRTicket += String.format("便情報（出発時刻）=[%s]", dateTime);
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->date = %s time = %s",_printDataRes.getString(R.string.printLog_printDataError), date, time);
            }

        } else if (isQRTicketPrintSts == QRTicketPrintSts.QRCODE.ordinal()) {

            if (_dynamicTicketItem.qr_code_item != null) {
                // QRコード
                setBitmap_QRCode(_dynamicTicketItem.qr_code_item);
                Log_QRTicket = "QRコード=[" + _dynamicTicketItem.qr_code_item + "]";
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->_dynamicTicket.qr_code_item = null",_printDataRes.getString(R.string.printLog_printDataError));
            }

        } else if (isQRTicketPrintSts == QRTicketPrintSts.CATEGORY.ordinal()) {

            // 予約番号
            String reservationNumber = _printDataRes.getString(R.string.print_reservation_number) + "：";
            if (_dynamicTicketItem.reservation_no != null) {
                reservationNumber += _dynamicTicketItem.reservation_no;
            } else {
                // 印刷データ異常（想定外）
                Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->_dynamicTicket.reservation_no = null",_printDataRes.getString(R.string.printLog_printDataError));
            }
            setAlign_Mid(reservationNumber, PaintSize_Normal);
            setLF(1,PaintSize_Normal);
            Log_QRTicket = "[" + reservationNumber + "]";

            // 乗客カテゴリ別の人数
            if (_dynamicTicketItem.peoples != null) {
                int peoples = _dynamicTicketItem.peoples.length;

                for (int i = 0; i < peoples; i++) {
                    String categoryType = _dynamicTicketItem.peoples[i].category_type;
                    Integer number = _dynamicTicketItem.peoples[i].num;
                    String categoryInfo = null;

                    if (categoryType.equals("unknown")) {
                        /* 大人 */
                        if (number != null) {
                            categoryInfo = String.format("%sｘ%s", _printDataRes.getString(R.string.print_adult), number);
                        } else {
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->peoples[%s] %sｘ%s",_printDataRes.getString(R.string.printLog_printDataError), i, _printDataRes.getString(R.string.print_adult), number);
                        }
                    } else if (categoryType.equals("child")) {
                        /* 小人 */
                        if (number != null) {
                            categoryInfo = String.format("%sｘ%s", _printDataRes.getString(R.string.print_child), number);
                        } else {
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->peoples[%s] %sｘ%s",_printDataRes.getString(R.string.printLog_printDataError), i, _printDataRes.getString(R.string.print_child), number);
                        }
                    } else if (categoryType.equals("disabled")) {
                        /* 障がい者 大人 */
                        if (number != null) {
                            categoryInfo = String.format("%sｘ%s", _printDataRes.getString(R.string.print_adult_disability), number);
                        } else {
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->peoples[%s] %sｘ%s",_printDataRes.getString(R.string.printLog_printDataError), i, _printDataRes.getString(R.string.print_adult_disability), number);
                        }
                    } else if (categoryType.equals("child_disabled")) {
                        /* 障がい者 小人 */
                        if (number != null) {
                            categoryInfo = String.format("%sｘ%s", _printDataRes.getString(R.string.print_child_disability), number);
                        } else {
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->peoples[%s] %sｘ%s",_printDataRes.getString(R.string.printLog_printDataError), i, _printDataRes.getString(R.string.print_child_disability), number);
                        }
                    } else if (categoryType.equals("carer")) {
                        /* 介助者 */
                        if (number != null) {
                            categoryInfo = String.format("%sｘ%s", _printDataRes.getString(R.string.print_caregiver), number);
                        } else {
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->peoples[%s] %sｘ%s", _printDataRes.getString(R.string.printLog_printDataError), i, _printDataRes.getString(R.string.print_caregiver), number);
                        }
                    } else if (categoryType.equals("baby")) {
                        /* 乳幼児 */
                        if (number != null) {
                            categoryInfo = String.format("%sｘ%s", _printDataRes.getString(R.string.print_baby), number);
                        } else {
                            // 印刷データ異常（想定外）
                            Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->peoples[%s] %sｘ%s", _printDataRes.getString(R.string.printLog_printDataError), i, _printDataRes.getString(R.string.print_baby), number);
                        }
                    } else {
                        /* その他 */
                        // 印刷データ異常（想定外）
                        Timber.tag("Printer").e("%s：setPrintData_QRTicketReceipt->peoples[%s] categoryType=%s number=%s",_printDataRes.getString(R.string.printLog_printDataError), i, categoryType, number);
                    }

                    if (categoryInfo != null) {
                        setAlign_Left(categoryInfo, PaintSize_Normal);
                        Log_QRTicket += "\n";
                        Log_QRTicket += "[" + categoryInfo + "]";
                    }
                }
            }

            setLine();
            // 注意文
            setAlign_Left(_printDataRes.getStringArray(R.array.print_caution)[0],PaintSize_Normal);
            setAlign_Left(_printDataRes.getStringArray(R.array.print_caution)[1],PaintSize_Normal);
            setLF(1,PaintSize_Normal);

        } else {
            // 想定外（改行のみ）
            setLF(1,PaintSize_Normal);
        }

        Timber.tag("Printer").i("%s：%s",_printDataRes.getString(R.string.printLog_printQRTicketDataSet), Log_QRTicket);
    }

    private void setBitmap_QRCode(String code) {

        try {
            //生成処理
            ConcurrentHashMap hints = new ConcurrentHashMap();
            //エラー訂正レベル指定
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            //エンコーディング指定
            //hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            //マージン指定
            hints.put(EncodeHintType.MARGIN, 10);
            QRCodeWriter writer = new QRCodeWriter();

            BitMatrix bitMatrix = writer.encode(code, BarcodeFormat.QR_CODE, 200, 200, hints);

            // BitMatrix -> Bitmap変換
            final int WHITE = 0xFFFFFFFF;
            final int BLACK = 0xFF000000;

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            int yStart = 0;
            int yEnd = 0;
            boolean yCheck;

            for (int y = 0; y < height; y++) {
                yCheck = false;
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? BLACK : WHITE;
                    if (pixels[offset + x] == BLACK) {
                        //Timber.d("y=%d x=%d",y ,x);
                        yCheck = true;
                        if (0 == yStart) yStart = y;
                    }
                }

                if (yStart != 0 && yCheck == false) {
                    yEnd = y;
                    break;
                }
            }

            int bitmapWidth = width;
            int bitmapHeight = yEnd-yStart;
            int count = bitmapWidth * bitmapHeight;
            int[] bitmapPixels = new int[count];
            //Timber.d("bitmapWidth=%d bitmapHeight=%d",bitmapWidth , bitmapHeight);
            for (int i = 0; i < count; i++) {
                // QRコードの上下空白部分を除く
                bitmapPixels[i] = pixels[yStart*bitmapWidth + i];
            }

            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(bitmapPixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
            _printBitmap = bitmap;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /** ▲チケット販売時の伝票データ作成▲ **/

    // デバイスチェック結果
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void PrintDeviceCheckResult(String CheckResult){

        print_init();

        _printCanvas.drawText(CheckResult, _paint);

        Timber.tag("Printer").i("%s：%s",_printDataRes.getString(R.string.printLog_printDataSet),_printDataRes.getString(R.string.printLog_deviceCheckResult));
        Printing(_printCanvas);
    }

    // 自動釣銭機機内残高印刷
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void CashHistory(MenuCashChangerViewModel.AmountValue AmountValue){

        print_init();

        String value10000 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value10000);
        String value5000 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value5000);
        String value2000 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value2000);
        String value1000 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value1000);
        String value500 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value500);
        String value100 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value100);
        String value50 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value50);
        String value10 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value10);
        String value5 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value5);
        String value1 = String.format(Locale.JAPANESE, "%,3d", AmountValue.value1);

        String value10000Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value10000Total);
        String value5000Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value5000Total);
        String value2000Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value2000Total);
        String value1000Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value1000Total);
        String value500Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value500Total);
        String value100Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value100Total);
        String value50Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value50Total);
        String value10Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value10Total);
        String value5Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value5Total);
        String value1Total = String.format(Locale.JAPANESE, "%,6d", AmountValue.value1Total);

        String valueTotal = String.format(Locale.JAPANESE, "%,7d", AmountValue.valueTotal);

        // 残高取得日時
        setAlign_Left(AmountValue.cashDate, PaintSize_Normal);
        setLF(1,PaintSize_Normal);

        // 票名
        setLF(1,PaintSize_Normal);
        setAlign_Mid("つり銭機残高", PaintSize_Medium);

        // 加盟店名
        String merchantName = AppPreference.getMerchantName();
        setAlign_Left(merchantName, PaintSize_Normal);

        // 営業所名/号機番号
        String merchantOffice = AppPreference.getMerchantOffice();  //加盟店営業所名
        int carId = AppPreference.getMcCarId(); //号機番号（車番）
        setAlign_Left(merchantOffice + "/" + carId + MainApplication.getInstance().getResources().getString(R.string.print_car_id), PaintSize_Normal);

        // 係員番号
        if (AppPreference.isDriverCodeInput()) {
            setAlign_LR(MainApplication.getInstance().getResources().getString(R.string.print_driver_id), AppPreference.getMcDriverId(), PaintSize_Normal);
        }

        // 機器番号
        String termId = AppPreference.getMcTermId();   //機器番号
        setAlign_LR(MainApplication.getInstance().getResources().getString(R.string.print_term_id), termId, PaintSize_Normal);
        setLine();
        setLF(1,PaintSize_Normal);

        // 合計金額
        //setAlign_Right("合計金額" + valueTotal + "円", PaintSize_Medium);
        setAlign_LR("合計金額", valueTotal + "円", PaintSize_Medium);
        setLF(1,PaintSize_Normal);

        setAlign_Mid("　　万　" + value10000 + "枚　" + value10000Total + "円", PaintSize_Medium);
        setAlign_Mid("　５千　" + value5000 + "枚　" + value5000Total + "円", PaintSize_Medium);
        setAlign_Mid("　２千　" + value2000 + "枚　" + value2000Total + "円", PaintSize_Medium);
        setAlign_Mid("　　千　" + value1000 + "枚　" + value1000Total + "円", PaintSize_Medium);
        setAlign_Mid("５００　" + value500 + "枚　" + value500Total + "円", PaintSize_Medium);
        setAlign_Mid("１００　" + value100 + "枚　" + value100Total + "円", PaintSize_Medium);
        setAlign_Mid("　５０　" + value50 + "枚　" + value50Total + "円", PaintSize_Medium);
        setAlign_Mid("　１０　" + value10 + "枚　" + value10Total + "円", PaintSize_Medium);
        setAlign_Mid("　　５　" + value5 + "枚　" + value5Total + "円", PaintSize_Medium);
        setAlign_Mid("　　１　" + value1 + "枚　" + value1Total + "円", PaintSize_Medium);

        Timber.tag("Printer").i("%s：%s",_printDataRes.getString(R.string.printLog_printDataSet),"自動釣銭機機内残高印刷");
        Printing(_printCanvas);
    }

    // デモモード確認
    private void PrintDemoCheck(){
        if(isDemoMode()) {
            /* デモモードの場合 */
            setAlign_Mid(_printDataRes.getStringArray(R.array.print_slip_demo)[0],PaintSize_Big);
            setAlign_Mid(_printDataRes.getStringArray(R.array.print_slip_demo)[1],PaintSize_Big);
            setAlign_Mid(_printDataRes.getStringArray(R.array.print_slip_demo)[2],PaintSize_Big);
        }

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
            /* フタバ双方向の場合 */
            setAlign_Mid(_printDataRes.getStringArray(R.array.print_slip_manual)[0],PaintSize_Big);
            setAlign_Mid(_printDataRes.getStringArray(R.array.print_slip_manual)[1],PaintSize_Big);
            setAlign_Mid(_printDataRes.getStringArray(R.array.print_slip_manual)[2],PaintSize_Big);
        }
    }

    // 印刷データなし
    private void NoPrintData(){
        Handler handler= new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                PrinterManager.getInstance().NoPrintData();
                Printing_end();
            }
        });
    }

    // 印刷データ異常（即時終了）
    private void PrintDataError(){
        Handler handler= new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                PrinterManager.getInstance().PrintDataError();
                Printing_end();
            }
        });
    }

    // 印刷
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void Printing(PrintCanvas printCanvas){
        if (AppPreference.getIsExternalPrinter()) { //cashDrawer対応
            Printing_CashChanger(printCanvas);
        } else {
            isPrinterSts = _printer.getStatus();

            if (isPrinterSts == Printer.PRINTER_OK) {
                Timber.tag("Printer").i("%s", _printDataRes.getString(R.string.printLog_printStart));
                _printTask.setPrintCanvas(printCanvas);
                BitmapSaver.saveReceipt(printCanvas.getBitmap());
                _printer.startPrint(_printTask, new PrinterCallback() {
                    @Override
                    public void onResult(int result, PrintTask printTask) {
                        Timber.tag("Printer").i("%s：%d", _printDataRes.getString(R.string.printLog_printEnd), result);

                        if (result == 0 && isQRTicketPrintSts == QRTicketPrintSts.INFO.ordinal()) {
                            // 継続印刷
                            printQRTicketReceipt(QRTicketPrintSts.QRCODE.ordinal());
                            return;
                        }

                        // 印刷終了
                        if (result == 0 && isCreditAnnounceSignature == true) {
                            // 加盟店控えが正常印刷完了かつサイン音声案内が必要な場合のみ
                            CreditSettlement.getInstance().startAnnounceSignature();
                        }
                        PrinterManager.getInstance().PrintEnd(isMaskCardId, isTransResult, result, isTransType, isRePrinter);
                        Printing_end();
                    }
                });
            } else {
                Timber.tag("Printer").e("%s：%d", _printDataRes.getString(R.string.printLog_printStart_NG), isPrinterSts);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 印刷終了
                        PrinterManager.getInstance().PrintEnd(isMaskCardId, isTransResult, isPrinterSts, isTransType, isRePrinter);
                        Printing_end();
                    }
                });
            }
        }
    }

    // 印刷（ビットマップ）
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void Printing(Bitmap bitmap){
        isPrinterSts = _printer.getStatus();

        if (isPrinterSts == Printer.PRINTER_OK) {
            Timber.tag("Printer").i("%s", _printDataRes.getString(R.string.printLog_printStart));
            _printTask.setPrintBitmap(bitmap);
            _printer.startPrint(_printTask, new PrinterCallback() {
                @Override
                public void onResult(int result, PrintTask printTask) {
                    Timber.tag("Printer").i("%s：%d", _printDataRes.getString(R.string.printLog_printEnd), result);

                    if (result == 0 && isQRTicketPrintSts == QRTicketPrintSts.QRCODE.ordinal()) {
                        // 継続印刷
                        printQRTicketReceipt(QRTicketPrintSts.CATEGORY.ordinal());
                    } else {
                        // 印刷終了
                        PrinterManager.getInstance().PrintEnd(isMaskCardId, isTransResult, result, isTransType, isRePrinter);
                        Printing_end();
                    }
                }
            });
        } else {
            Timber.tag("Printer").e("%s：%d", _printDataRes.getString(R.string.printLog_printStart_NG), isPrinterSts);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 印刷終了
                    PrinterManager.getInstance().PrintEnd(isMaskCardId, isTransResult, isPrinterSts, isTransType, isRePrinter);
                    Printing_end();
                }
            });
        }
    }

    // LT27ヤザキ双方向印刷応答結果
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void Printing_Duplex(String result,int contflg,int errcd){
        if (result.equals("OK")) {
            Timber.tag("IM-A820").i("%s：%s 継続フラグ：%d", _printDataRes.getString(R.string.printLog_printEnd), result, contflg);
        } else {
            //ADD-S BMT S.Oyama 2024/10/22 フタバ双方向向け改修
            String tmpErrorMesHeader = "印刷結果";
            if (_printDataRes != null) {
                tmpErrorMesHeader = _printDataRes.getString(R.string.printLog_printEnd);
            }
            //ADD-E BMT S.Oyama 2024/10/22 フタバ双方向向け改修

            Timber.tag("IM-A820").e("%s：%s エラーコード：%d", tmpErrorMesHeader, result, errcd);
        }

        if(isTransResult == PrinterConst.TransResult_UnFinished) {
            if (null != _slipData && null != _slipData.cardIdMerchant) {
                card_id_masked(_slipData.cardIdMerchant);
            }
        }
        if(result.equals("OK") ){
            if (isCreditAnnounceSignature) {
                // サイン音声案内が必要な場合のみ
                CreditSettlement.getInstance().startAnnounceSignature();
            }
            Handler handler= new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 印刷終了
                    PrinterManager.getInstance().PrintEndDuplex(isMaskCardId,isTransResult,Printer.PRINTER_OK,contflg);
                    Printing_end();
                }
            });
        }
        else
        {
            Handler handler= new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 印刷終了
                    int argErrcd = errcd;
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
                        // メーターから受信したエラーコードを、PT-750のエラーコードへ変換
                        argErrcd = convertErrorCode_OKABE(errcd);
                    }
                    //PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult,errcd,0);
                    PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult,argErrcd,0);
                    Printing_end();
                    //PrinterManager.getInstance().PrinterDuplexError(errcd) ;
                }
            });
        }
    }

    //ADD-S BMT S.Oyama 2024/10/25 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  印刷応答結果（フタバ双方向用）
     * @note   印刷応答結果 /printdata/v1::print_end
     * @param [in] String result   ステータス　文字列化数値(0,1,2,8,9)
     *             int contflg 継続フラグ(0,1,2,4,5,6,7  0,1,2,4,5以外は0とみなす)
     *             int errcd 未使用
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void Printing_DuplexFutabaD(String result, int contflg, int errcd){
        String tmpStr = "印刷結果";
        if (_printDataRes != null) {
            tmpStr = _printDataRes.getString(R.string.printLog_printEnd);
        }
        if (result.equals("OK")) {       // 正常終了
            Timber.i("[FUTABA-D]%s：result %s 継続フラグ：%d", tmpStr, result, contflg);
        } else {                        // 異常終了
            Timber.e("[FUTABA-D]%s：result %s 継続フラグ：%d エラーコード：%d", tmpStr, result, contflg, errcd);
        }

        if(isTransResult == PrinterConst.TransResult_UnFinished) {
            if (null != _slipData && null != _slipData.cardIdMerchant) {
                card_id_masked(_slipData.cardIdMerchant);
            }
        }
        if(result.equals("OK") ){        // 正常終了
            if (isCreditAnnounceSignature) {
                // サイン音声案内が必要な場合のみ
                CreditSettlement.getInstance().startAnnounceSignature();
            }
            Handler handler= new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 印刷終了
                    int tmpContflg = contflg;
                    tmpContflg = convertContinueCode_FutabaD(tmpContflg);
                    PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult, Printer.PRINTER_OK, tmpContflg);
                    Printing_end();
                    //ADD-S BMT S.Oyama 2025/04/24 フタバ双方向向け改修
                    if (tmpContflg == 0) {         //継続印字が無くなった＝印字完全終了
                        _DuplexPrint_BlandName = "";
                    }
                    //ADD-E BMT S.Oyama 2025/04/24 フタバ双方向向け改修
                }
            });
        }
        else                        // 異常終了
        {
            Handler handler= new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // 印刷終了
                    int argErrcd ;
                    int tmp750ErrCD = errcd;

//                    try {
//                        argErrcd = Converters.stringToInteger(result);
//                    } catch (NumberFormatException e) {
//                        Timber.e(e, "Printing_DuplexFutabaD() NumberFormatException status:%s", result);
//                        tmp750ErrCD = PrinterConst.DuplexPrintStatus_DATAERROR;
//                        argErrcd = 10;
//                    }

                    argErrcd = convertErrorCode_FutabaD(errcd);
                    int tmpContflg = contflg;

                    //PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult,errcd,0);
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true ) {
                        tmpContflg = convertContinueCode_FutabaD(tmpContflg);
                        PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult, argErrcd, tmpContflg);
                    }
                    else {
                        PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult, argErrcd, 0);
                    }
                    Printing_end();
                    //PrinterManager.getInstance().PrinterDuplexError(errcd) ;
                }
            });
        }
    }
    /******************************************************************************/
    /*!
     * @brief  印刷応答結果（フタバ双方向用）
     * @note   印刷応答結果 /printdata/v1::print_end
     * @param [in] String result   ステータス　文字列化数値"OK/NG"
     *             int contflg 継続フラグ(0,1,2,4,5,6,7  0,1,2,4,5以外は0とみなす)
     *             int errcd エラーコード（フタバD）
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
//    public void Printing_DuplexFutabaD(String result, int contflg, String errcd){
//        String tmpStr = "印刷結果";
//        if (_printDataRes != null) {
//            tmpStr = _printDataRes.getString(R.string.printLog_printEnd);
//        }
//        if (result.equals("OK")) {       // 正常終了
//            Timber.i("[FUTABA-D]%s：result %s 継続フラグ：%d", tmpStr, result, contflg);
//        } else {                        // 異常終了
//            Timber.e("[FUTABA-D]%s：result %s 継続フラグ：%d エラーコード：%s", tmpStr, result, contflg, errcd);
//        }
//
//        if(isTransResult == PrinterConst.TransResult_UnFinished) {
//            if (null != _slipData && null != _slipData.cardIdMerchant) {
//                card_id_masked(_slipData.cardIdMerchant);
//            }
//        }
//        if(result.equals("OK") ){        // 正常終了
//            if (isCreditAnnounceSignature) {
//                // サイン音声案内が必要な場合のみ
//                CreditSettlement.getInstance().startAnnounceSignature();
//            }
//            Handler handler= new Handler(Looper.getMainLooper());
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    // 印刷終了
//                    int tmpContflg = contflg;
//                    tmpContflg = convertContinueCode_FutabaD(tmpContflg);
//                    PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult, Printer.PRINTER_OK, tmpContflg);
//                    Printing_end();
//                }
//            });
//        }
//        else                        // 異常終了
//        {
//            Handler handler= new Handler(Looper.getMainLooper());
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    // 印刷終了
////                    int argErrcd ;
////                    String tmp750ErrCD = errcd;
////
////                    try {
////                        argErrcd = Converters.stringToInteger(result);
////                    } catch (NumberFormatException e) {
////                        Timber.e(e, "Printing_DuplexFutabaD() NumberFormatException status:%s", result);
////                        tmp750ErrCD = PrinterConst.DuplexPrintStatus_DATAERROR;
////                        argErrcd = 10;
////                    }
////
////                    argErrcd = convertErrorCode_FutabaD(argErrcd, tmp750ErrCD);
////
////                    //PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult,errcd,0);
////                    PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult, argErrcd,0);
////                    Printing_end();
////                    //PrinterManager.getInstance().PrinterDuplexError(errcd) ;
//                }
//            });
//        }
//    }
    //ADD-E BMT S.Oyama 2024/10/25 フタバ双方向向け改修

    private int convertErrorCode_OKABE(int errcd) {
        // メーターから受信したエラーコードを、PT-750のエラーコードへ変換
        int rtnErrcd = errcd;

        switch (errcd) {
            case PrinterConst.DuplexOkabePrintStatus_PAPERLACKING:  // 紙切れ
                rtnErrcd = PrinterConst.DuplexPrintStatus_PAPERLACKING;
                break;
            case PrinterConst.DuplexOkabePrintStatus_PRINTERROR:  // プリンター異常
                rtnErrcd = PrinterConst.DuplexPrintStatus_PRINTCHECK;
                break;
            default:
                // メーターから受信したエラーコード以外は変換しない
                rtnErrcd = errcd;
                break;
        }

        return rtnErrcd;
    }

    //ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  メーターから受信したエラーコードを、PT-750のエラーコードへ変換(フタバ双方向)
     * @note   メーターから受信したエラーコードを、PT-750のエラーコードへ変換
     * @param [in] int errcd メーターから受信したエラーコード
     *             int tmpInnerErrCD PT-750内でエラー発生時のエラーコード
     * @retval なし
     * @return int PT-750のエラーコード
     * @private
     */
    /******************************************************************************/
    private int convertErrorCode_FutabaD(int errcd) {
        // メーターから受信したエラーコードを、PT-750のエラーコードへ変換
        int rtnErrcd = errcd;

        switch (errcd) {
            case 0:     //正常印字
                break;
            case 1:     //紙切れ
                rtnErrcd = PrinterConst.DuplexPrintStatus_PAPERLACKING;
                break;
            case 2:     //プリンター確認
                rtnErrcd = PrinterConst.DuplexPrintStatus_PRINTCHECK;
                break;
            case 8:     //印字拒否
                rtnErrcd = PrinterConst.DuplexPrintStatus_DENY;
                break;
            case 9:     //印字不能
                rtnErrcd = PrinterConst.DuplexPrintStatus_ERROR;
                break;
//            case 10:    //PT750内部で発生したエラー
//                rtnErrcd = tmpInnerErrCD;
//                break;
            default:
                rtnErrcd = -10000 - errcd;
                break;
        }

        return rtnErrcd;
    }

//    private int convertErrorCode_FutabaDStr(String errcd, int tmpInnerErrCD) {
//        // メーターから受信したエラーコードを、PT-750のエラーコードへ変換
//        //PDFのP21
//
//        int rtnErrcd = 0;
//
//        if (errcd == null) {
//            rtnErrcd = 0;
//        }
//        else if (errcd.equals("") == true )     //正常印字
//        {
//            rtnErrcd = 0;
//        } if (errcd.equals("1") == true )       //紙切れ
//        {
//            rtnErrcd = PrinterConst.DuplexPrintStatus_PAPERLACKING;
//        }
//        if (errcd.equals("2") == true )       //プリンター確認
//        {
//            rtnErrcd = PrinterConst.DuplexPrintStatus_PRINTCHECK;
//        }
//        if (errcd.equals("8") == true )       //印字拒否
//        {
//            rtnErrcd = PrinterConst.DuplexPrintStatus_PRINTCHECK;
//        }
//        if (errcd.equals("9") == true )       //印字不能
//        {
//            rtnErrcd = PrinterConst.DuplexPrintStatus_PRINTCHECK;
//        }
//        if (errcd.equals("10") == true )      //PT750内部で発生したエラー
//        {
//            rtnErrcd = tmpInnerErrCD;
//        }
//        else
//        {
//            rtnErrcd = PrinterConst.DuplexPrintStatus_PRINTCHECK;
//        }
//        return rtnErrcd;
//    }


    //ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/10/25 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  メーターから受信した継続印字情報を、PT-750のエラーコードへ変換(フタバ双方向)
     * @note   メーターから受信した継続印字情報を、PT-750のエラーコードへ変換
     * @param [in] int tmpContCD : 継続印字情報(フタバD用)
     * @retval なし
     * @return　継続印字情報(PT-750用)
     * @private
     */
    /******************************************************************************/
    private int convertContinueCode_FutabaD(int tmpContCD) {
        // メーターから受信した継続印字情報を、PT-750のコードへ変換
        int rtnContCD = tmpContCD;

        switch (tmpContCD) {
            case 0:  // 無し
                rtnContCD = 0;
                break;
            case 1:  // 有り
                rtnContCD = 1;
                break;
            case 2:  // 有り(残金あり表示)
                rtnContCD = 1;
                break;
            case 4:  // 有り(残金あり表示4)
                rtnContCD = 1;
                break;
            case 5:  // 無し(残金あり表示)
                rtnContCD = 0;
                break;
            case 6:  // 無し(残金あり表示2)
                rtnContCD = 0;
                break;
            case 7:  // 無し(残金あり表示3)
                rtnContCD = 0;
                break;
            default:
                // メーターから受信した継続印字情報以外は変換しない
                rtnContCD = tmpContCD;
                break;
        }

        return rtnContCD;
    }
    //ADD-E BMT S.Oyama 2024/10/25 フタバ双方向向け改修


    // LT27ヤザキ双方向印刷応答結果※例外発生時のみ
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void Printing_Duplex_Exception(int errcd){

        Timber.tag("IM-A820").e("%s： エラーコード：%d", _printDataRes.getString(R.string.printLog_printEnd), errcd);
        Handler handler= new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // 印刷終了
                PrinterManager.getInstance().PrintEndDuplex(isMaskCardId, isTransResult, errcd,0);
                Printing_end();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void Printing_CashChanger(PrintCanvas printCanvas) {
        EpsonPrinterProc epsonPrinterProc = EpsonPrinterProc.getInstance();
        Timber.tag("Printer").i("Epson印刷開始");

        int result = epsonPrinterProc.print(printCanvas);
        if (result == Epos2CallbackCode.CODE_SUCCESS) {
            Timber.tag("Printer").i("Epson印刷正常終了");
            isPrinterSts = Printer.PRINTER_OK;
        } else {
            // エラー
            Timber.tag("Printer").e("Epson印刷エラー result:%d", result);
            isPrinterSts = result;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // 印刷終了
                PrinterManager.getInstance().PrintEnd(isMaskCardId, isTransResult, isPrinterSts, isTransType, false);
                Printing_end();
            }
        });
    }

    // 印刷終了
    private void Printing_end()
    {
        _printer = null;
        _printTask = null;
        _printCanvas = null;
        _paint = null;
        _slipData = null;
        _slipDataList = null;

        //ADD-S BMT S.Oyama 2024/10/15 フタバ双方向向け改修
        _DuplexComm_BlandName = "";
        _DuplexComm_SlipIDBackup = 0;
        //ADD-S BMT S.Oyama 2024/10/15 フタバ双方向向け改修
    }

    private void LogAmounts() {
        /* 取引金額 */
        if (_slipData.transAmount == null) {
            Log_Amounts = "取引金額:null ";
        } else {
            Log_Amounts = String.format("取引金額:%d ", _slipData.transAmount);
        }
        /* 定額金額 */
        if (_slipData.transSpecifiedAmount == null) {
            Log_Amounts += "定額:null ";
        } else {
            Log_Amounts += String.format("定額:%d ", _slipData.transSpecifiedAmount);
        }
        /* メーター金額 */
        if (_slipData.transMeterAmount == null) {
            Log_Amounts += "メーター金額:null ";
        } else {
            Log_Amounts += String.format("メーター金額:%d ", _slipData.transMeterAmount);
        }
        /* 増減額 */
        if (_slipData.transAdjAmount == null) {
            Log_Amounts += "増減額:null ";
        } else {
            Log_Amounts += String.format("増減額:%d ", _slipData.transAdjAmount);
        }
        /* 現金併用金額 */
        if (_slipData.transCashTogetherAmount == null) {
            Log_Amounts += "現金併用金額:null ";
        } else {
            Log_Amounts += String.format("現金併用金額:%d ", _slipData.transCashTogetherAmount);
        }
        /* 取引前残高*/
        if (_slipData.transBeforeBalance == null) {
            Log_Amounts += "取引前残高:null ";
        } else {
            Log_Amounts += String.format("取引前残高:%d ", _slipData.transBeforeBalance);
        }
        /* 取引後残高 */
        if (_slipData.transAfterBalance == null) {
            Log_Amounts += "取引後残高:null ";
        } else {
            Log_Amounts += String.format("取引後残高:%d ", _slipData.transAfterBalance);
        }
        /* その他金額①種別 */
        if (_slipData.transOtherAmountOneType == null) {
            Log_Amounts += "その他金額①[種別:null ";
        } else {
            Log_Amounts += String.format("その他金額①[種別:%d ", _slipData.transOtherAmountOneType);
        }
        /* その他金額① */
        if (_slipData.transOtherAmountOne == null) {
            Log_Amounts += "金額:null] ";
        } else {
            Log_Amounts += String.format("金額:%d] ", _slipData.transOtherAmountOne);
        }
        /* その他金額②種別（予備） */
        if (_slipData.transOtherAmountTwoType == null) {
            Log_Amounts += "その他金額②[種別:null ";
        } else {
            Log_Amounts += String.format("その他金額②[種別:%d ", _slipData.transOtherAmountTwoType);
        }
        /* その他金額②（予備） */
        if (_slipData.transOtherAmountTwo == null) {
            Log_Amounts += "金額:null] ";
        } else {
            Log_Amounts += String.format("金額:%d] ", _slipData.transOtherAmountTwo);
        }

        Timber.tag("Printer").i("%s( %s)",_printDataRes.getString(R.string.printLog_printDataAmounts), Log_Amounts);
    }
    /*インボイス対応*/
    private void LogInvoice() {
        if (Log_InvoiceNo != null && Log_InvoiceNo.equals("")){
            Timber.tag("Printer").i("%s：%s %s：(No.%s, %s%%)",_printDataRes.getString(R.string.printLog_ServicePos),Log_ServicePos,_printDataRes.getString(R.string.printLog_printInvoiceDataSet)," ",Log_InvoiceTax);
        } else {
            Timber.tag("Printer").i("%s：%s %s：(No.%s, %s%%)",_printDataRes.getString(R.string.printLog_ServicePos),Log_ServicePos,_printDataRes.getString(R.string.printLog_printInvoiceDataSet),Log_InvoiceNo,Log_InvoiceTax);
        }
    }
    //インボイス番号
    private boolean invoicePaymentType() {
        if ((_slipData.transType == 0 || _slipData.transType == 1) && !_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))) {
            return true;
        } else {
            //何もしない
        }
        return false;
    }
    /*インボイス対応*/
    //消費税率、運賃
    private boolean Tax_FareOutputConfirm(){
        //売上の場合
        if(_slipData.transType == 0 && !_slipData.transBrand.equals(_printDataRes.getString(R.string.print_brand_okica))){
            return true;

        
        } else {
            //取消の場合、レシート出力しない
            return false;
        }
    }


    public String convertDate(String str) {
        String date = String.valueOf(str.toCharArray(), 0, 4);
        date += '/';
        date += String.valueOf(str.toCharArray(), 4, 2);
        date += '/';
        date += String.valueOf(str.toCharArray(), 6, 2);
        return date;
    }

    public String convertTime(String str) {
        String time = String.valueOf(str.toCharArray(), 0, 2);
        time += ':';
        time += String.valueOf(str.toCharArray(), 2, 2);
        time += ':';
        if(str.length() >= 5) {
            // 秒もある場合
            time += String.valueOf(str.toCharArray(), 4, 2);
        } else {
            time += "00";
        }
        return time;
    }

    public String convertDatetime(String str) {
        String datetime = convertDate(str);
        datetime += ' ';
        datetime += convertTime(str.substring(8));
        return datetime;
    }

    //ADD-S BMT S.Oyama 2024/10/01 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  時間文字列の書式変更(フタバ双方向)
     * @note   時間文字列の書式変更　hhmmss or hhmm -> hhmm
     * @param [in] String str 時間文字列　4文字以上，
     * @retval なし
     * @return　変換後の時間文字列　4文字以上hhmm 4文字未満はそのまま返す　null時は空文字
     * @private
     */
    /******************************************************************************/
    public String convertTimeNotSecAndColon(String str) {
        String result = "";
        if (str == null || (str.equals("") == true)) {
            return result;
        }

        if (str.length() >= 4)
        {
            result = String.valueOf(str.toCharArray(), 0, 2);       //hh
            result += String.valueOf(str.toCharArray(), 2, 2);      //mm
        }
        else
        {
            result = str;
        }

        return result;
    }
    //ADD-E BMT S.Oyama 2024/10/01 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/12/02 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  プリペイド：決済選択処理を実施(フタバ双方向)
     * @note   プリペイド：決済選択処理を実施
     * @param [in] int tmpSettlementSelectMode 決済選択モード
     * @retval なし
     * @return　送信成功時true 失敗時false
     * @private
     */
    /******************************************************************************/
    private boolean send820PrepaidSettlementSelectMode(int tmpSettlementSelectMode)
    {

        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)) {              //フタバD以外は処理しない
            return false;
        }
//
//        if (_ifBoxManager.getIsConnected820() == false)             //820未接続の場合
//        {
//            PrinterManager.getInstance().PrinterDuplexError(PrinterConst.DuplexPrintStatus_DISCON);
//            Printing_end();
//            return false;
//        }
//
//        IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
//        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.IsLoopBreakOut = false;
//        tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
//        tmpSend820Info.ErrorCodeExt1 = 0;
//
//        meterDataV4InfoDisposable = _ifBoxManager.getMeterDataV4().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
//            Timber.i("[FUTABA-D]send820PrepaidSettlementSelectMode:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
//            if (meter.meter_sub_cmd == 9) {              //ファンクション実行要求：決済選択イベント
//                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
//            }
//        });
//
//        meterDataV4ErrorDisposable = _ifBoxManager.getMeterDataV4Error().subscribeOn(
//                Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
//            Timber.e("[FUTABA-D]send820PrepaidSettlementSelectMode:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
//            tmpSend820Info.StatusCode = error.ErrorCode;
//            tmpSend820Info.ErrorCode820 = error.ErrorCode820;
//
//        });
//
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                _ifBoxManager.send820_SettlementSelectMode(tmpSettlementSelectMode, false);    //820へプリペイド系決済選択を送信
//                for(int i = 0; i < (DuplexPrintResponseTimerSec + 1) * 10; i++)        //最大26秒ほど待ってみる
//                {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                    }
//
//                    if (tmpSend820Info.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
//                    {
//                        tmpSend820Info.IsLoopBreakOut = true;
//                        break;
//                    }
//                }
//            }
//        });
//        thread.start();
//
//        tmpSend820Info.ErrorCodeExt1 = 999999;               //通信等でエラー発生時は999999以外のエラーコードをセットする
//        try {
//            thread.join();
//
//            if (meterDataV4InfoDisposable != null) {       //コールバック系を後始末
//                meterDataV4InfoDisposable.dispose();
//                meterDataV4InfoDisposable = null;
//            }
//
//            if (meterDataV4ErrorDisposable != null) {      //コールバック系を後始末
//                meterDataV4ErrorDisposable.dispose();
//                meterDataV4ErrorDisposable = null;
//            }
//
//            if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
//                tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//            }
//            else
//            {
//                switch(tmpSend820Info.StatusCode)                       //ステータスコードのチェック
//                {
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
//                        tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        break;
//                    case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
//                        Timber.e("[FUTABA-D](demo)820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
//                        //ADD-S BMT S.Oyama 2025/01/29 フタバ双方向向け改修
//                        if (tmpSend820Info.ErrorCode820 == IFBoxManager.Send820Status_Error_FutabaD.ERROR_STATUS820_PAPERLACKING)       //用紙無しエラー
//                        {
//                            tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART;                     //用紙なしの場合はエラーコードを入れる
//                        } else {
//                            tmpSend820Info.ErrorCodeExt1 = PrinterConst.DuplexPrintStatus_IFBOXERROR;                       //IFBOX接続エラー
//                        }
//                        break;
//                        //ADD-E BMT S.Oyama 2025/01/29 フタバ双方向向け改修
//                    default:
//                        //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
//                        break;
//                }
//            }
//
//
//        } catch (Exception e) {
//            Timber.e(e);
//            tmpSend820Info.ErrorCodeExt1 =  PrinterConst.DuplexPrintStatus_DATAERROR;
//        }
//
//        if (tmpSend820Info.ErrorCodeExt1 != 999999){           //エラーコードが設定されている場合
//            Handler handler= new Handler(Looper.getMainLooper());
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    if (tmpSend820Info.ErrorCodeExt1 == PrinterConst.DuplexPrintStatus_OUTOFPAPER_NORESTART) {           //用紙なしエラーの場合
//                        PrinterManager.getInstance().PrinterDuplexError(tmpSend820Info.ErrorCodeExt1);
//                    } else {                                                                              //その他のエラーの場合
//                        PrinterManager.getInstance().PrinterDuplexError(PrinterConst.DuplexPrintStatus_DISCON);
//                    }
//
//                    PrinterManager.getInstance().dismissPrintingDialogExt();
//                    PrintDataError();
//                    //Printing_end();
//                }
//            });
//
//            return false;
//        }

        return true;
    }
    //ADD-E BMT S.Oyama 2024/12/02 フタバ双方向向け改修

    // テスト用の伝票印刷データ
    private void TestSlipData(){
        //_slipData.transBrand = null;                    // ブランド名(1)
        //_slipData.transType = null;                     // 取引種別(2)
        //_slipData.transTypeCode = null;                 // 取引種別コード(3)
        //_slipData.transResult = null;                   // 取引結果(4)
        //_slipData.transResultDetail = null;             // 取引結果詳細(5)
        //_slipData.printCnt = null;                      // 印刷回数(6)
        //_slipData.oldAggregateOrder = null;             // 過去の集計印刷順(7)
        //_slipData.encryptType = null;                   // 暗号化パターン(8)
        //_slipData.cancelFlg = null;                     // 取消可否(9)
        //_slipData.transId = null;                       // 決済ID(10)
        //_slipData.merchantName = null;                  // 加盟店名(11)
        //_slipData.merchantOffice = null;                // 加盟店営業所名(12)
        //_slipData.merchantTelnumber = null;             // 加盟店電話番号(13)
        //_slipData.carId = null;                         // 車番(14)
        //_slipData.driverId = null;                      // 乗務員コード(15)
        //_slipData.termId = null;                        // 機器番号(16)
        //_slipData.termSequence = null;                  // 機器通番(17)
        //_slipData.transDate = null;                     // 取引日時(18)
        //_slipData.cardCompany = null;                   // カード発行会社(19)
        //_slipData.cardIdMerchant = null;                // カード番号（加盟店控え印刷用）(20)
        //_slipData.cardIdCustomer = null;                // カード番号（お客様控え印刷用）(21)
        //_slipData.cardExpDate = null;                   // カード有効期限(22)
        //_slipData.cardTransNumber = null;               // 取引通番(23)
        //_slipData.nanacoSlipNumber = null;              // 取引通番※nanaco専用(24)
        //_slipData.edyTransNumber = null;                // Edy取引通番(25)
        //_slipData.slipNumber = null;                    // 伝票番号(26)
        //_slipData.oldSlipNumber = null;                 // 元伝票番号※取消対象(27)
        //_slipData.authId = null;                        // 承認番号(28)
        //_slipData.authSequenceNumber = null;            // 処理通番(29)
        //_slipData.commodityCode = null;                 // 商品コード(30)
        //_slipData.installment = null;                   // 分割回数(31)
        //_slipData.point = null;                         // 今回ポイント(32)
        //_slipData.pointGrantType = null;                // ポイント付与区分(33)
        //_slipData.pointGrantMsgOne = null;              // ポイント付与メッセージ１(34)
        //_slipData.pointGrantMsgTwo = null;              // ポイント付与メッセージ２(35)
        //_slipData.termIdentId = null;                   // 端末番号(36)
        //_slipData.transAmount = null;                   // 取引金額(37)
        //_slipData.transSpecifiedAmount = null;          // 定額(38)
        //_slipData.transMeterAmount = null;              // メーター金額(39)
        //_slipData.transAdjAmount = null;                // 増減額(40)
        //_slipData.transCashTogetherAmount = null;       // 現金併用金額(41)
        //_slipData.transOtherAmountOneType = null;       // その他金額①種別(42)
        //_slipData.transOtherAmountOne = null;           // その他金額①(43)
        //_slipData.transOtherAmountTwoType = null;       // その他金額②種別（予備）(44)
        //_slipData.transOtherAmountTwo = null;           // その他金額②（予備）(45)
        //_slipData.freeCountOne = null;                  // フリーカウント①(46)
        //_slipData.freeCountTwo = null;                  // フリーカウント②(47)
        //_slipData.transBeforeBalance = null;            // 取引前残高(48)
        //_slipData.transAfterBalance = null;             // 取引後残高(49)
        //_slipData.commonName = null;                    // ユニークID(50)
        //_slipData.creditType = null;                    // IC/MS(51)
        //_slipData.creditArc = null;                     // ARC(52)
        //_slipData.creditAid = null;                     // AID(53)
        //_slipData.creditApl = null;                     // APL(54)
        //_slipData.creditSignatureFlg = null;            // サイン(55)
        //_slipData.codetransOrderId = null;              // 伝票番号※コード決済専用(56)
        //_slipData.codetransPayTypeName = null;          // 決済種別名称(57)
    }

    //ADD-S BMT S.Oyama 2025/03/17 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  現処理slipidのブランド名を返す（プリペイド処理時の詳細情報が欲しいため）
     * @note   現処理slipidのブランド名を返す（プリペイド処理時の詳細情報が欲しいため）
     * @param なし
     * @retval なし
     * @return      第0要素：ブランド名　第１要素：transType
     * @private
     */
    /******************************************************************************/
    public String[] getBrandNameFromSlipID(int tmpSlipID){
        String tmpResult[] = {"", ""};

        // 別スレッド：伝票印刷関連データ取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _slipData = DBManager.getSlipDao().getOneById(tmpSlipID);
            }
        });
        thread.start();

        try {
            thread.join();

            tmpResult[0] = _slipData.transBrand;
            tmpResult[1] = _slipData.transType.toString();

        } catch (Exception e) {
            Timber.tag("Printer").e("%s：getBrandNameFromSlipID->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
            e.printStackTrace();
        }

        return tmpResult;
    }
    //ADD-E BMT S.Oyama 2025/03/17 フタバ双方向向け改修


}



//    /******************************************************************************/
//    /*!
//     * @brief  メータデータ通知（フタバ双方向用）
//     * @note   PT750->IM820へのメータデータ通知 /printdata/v3:meter_data
//     * @param [in] なし
//     * @retval なし
//     * @return
//     * @private
//     */
//    /******************************************************************************/
//下記実装は，IFBoxManagerへ集約　当面廃止
//    private void sendWsMeterdata_FutabaD() {
//
//        if(_sendMeterData_FutabaD == null)      //メータデータ通知データがNULLの場合
//        {
//            Timber.tag("Printer").e("%s：sendWsMeterdata_FutabaD::_sendMeterData_FutabaD is NULL", _printDataRes.getString(R.string.printLog_printDataError));
//            PrintDataError();
//
//            return;
//        }
//
//        String tmpDateTimeStrConvert;
//
//        JSONObject _params = new JSONObject();
//        JSONObject _sendData = new JSONObject();
//        try {
//            _sendData.put("type", "/printdata/v3");
//            _sendData.put("cmd","meter_data");
//            _sendData.put("timer",PrinterConst.DuplexMeterSendWaitTimerFutabaD);       //10秒
//
//            _params.put("meter_sub_cmd",    _sendMeterData_FutabaD.meter_sub_cmd);
//            _params.put("status",           _sendMeterData_FutabaD.status);
//            _params.put("term_ver",         _sendMeterData_FutabaD.term_ver);
//            _params.put("car_id",           _sendMeterData_FutabaD.car_id);
//            _params.put("input_kingaku",    _sendMeterData_FutabaD.input_kingaku);
//            _params.put("key_code",         _sendMeterData_FutabaD.key_code);
//            tmpDateTimeStrConvert = convertDatetime(_sendMeterData_FutabaD.trans_date);                 //フタバ日付フォーマット YYYY/MM/DD HH:MM:SS
//            _params.put("trans_date",       tmpDateTimeStrConvert);
//            _params.put("discount_way",     _sendMeterData_FutabaD.discount_way);
//            _params.put("discount_type",    _sendMeterData_FutabaD.discount_type);
//            tmpDateTimeStrConvert = convertDate(_sendMeterData_FutabaD.exp_date);                       //フタバ日付フォーマット YYYY/MM/DD
//            _params.put("exp_date",         tmpDateTimeStrConvert);
//            _params.put("adr_l2",           _sendMeterData_FutabaD.adr_l2);
//            _params.put("adr_l3",           _sendMeterData_FutabaD.adr_l3);
//            _params.put("adr_input1",       _sendMeterData_FutabaD.adr_input1);
//            _params.put("adr_input2",       _sendMeterData_FutabaD.adr_input2);
//            _params.put("if_ver",           _sendMeterData_FutabaD.if_ver);
//
//            // パラメータの格納
//            _sendData.put("data", _params);
//            _ifBoxManager.send(_sendData.toString(), PrinterConst.DuplexPrintResponseTimer);
//        } catch (Exception e) {
//            Timber.tag("Printer").e("%s：printTrans->Exception e <%s>", _printDataRes.getString(R.string.printLog_printDataError), e);
//            PrintDataError();
//            e.printStackTrace();
//        }
//    }

