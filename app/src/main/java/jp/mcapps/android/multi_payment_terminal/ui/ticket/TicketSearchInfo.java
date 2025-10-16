package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTripPageInfo;
import timber.log.Timber;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;

public class TicketSearchInfo {
    String serviceInstanceID;               // サービスインスタンスID
    String ticketClassID;                   // チケット分類ID
    String ticketID;                        // チケットID
    String date;                            // 対象日(YYYYMMDD)　※基本的には当日を指定
    String departureTime;                   // 出発時刻(hhmmss)　※基本的には現在時刻を指定。 ただし、前便・次便検索を検索する場合は起点となる時刻（検索結果の出発時刻）を設定
    String embarkStopID;                    // のりばID
    String disEmbarkStopID;                 // おりばID
    String prevNextTripType;                // 前便 or 次便 or 指定なし（null）　※前便を検索する場合はprevを、次便を検索する場合はnextを指定
    String tripID;                          // 便ID
    TicketTripPageInfo page_info;           // ページ情報

    Integer adultNumber;                    // 大人(人数)
    Integer childNumber;                    // 小人(人数)
    Integer babyNumber;                     // 乳幼児(人数)
    Integer adultDisabilityNumber;          // 障がい者 大人(人数)
    Integer childDisabilityNumber;          // 障がい者 小人(人数)
    Integer caregiverNumber;                // 介助者(人数)
    Integer totalPeoples;                   // 総人数

    private TerminalData _terminalData;

    private Boolean setServiceInstanceID() {
        boolean result = false;
        serviceInstanceID = "";
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _terminalData  = LocalDatabase.getInstance().terminalDao().getTerminal();
            }
        });
        thread.start();

        try {
            thread.join();
            if (_terminalData != null && _terminalData.service_instance_abt != null)
            {
                serviceInstanceID = _terminalData.service_instance_abt;
                result = true;
            } else {
                if (_terminalData == null) {
                    Timber.e("setServiceInstanceID>_terminalData = null");
                } else {
                    Timber.e("setServiceInstanceID>_terminalData.service_instance_abt = null");
                }
            }
        } catch (Exception e) {
            Timber.e(e);
            e.printStackTrace();
        }
        return result;
    }

    private Boolean setTicketClassID() {
        boolean result = false;
        if (AppPreference.getSelectedTicketClassData() != null) {
            ticketClassID = String.valueOf(AppPreference.getSelectedTicketClassData().ticket_class_id);
            result = true;
        } else {
            Timber.e("setTicketClassID>AppPreference.getSelectedTicketClassData() = null");
        }
        return result;
    }

    private Boolean setDate() {
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd", Locale.JAPANESE);
            date = dateFmt.format(new Date());
        return true;
    }

    private Boolean setDepartureTime(String tripType) {
        boolean result = false;
        if (tripType == null) {
            SimpleDateFormat dateFmt = new SimpleDateFormat("HHmmss", Locale.JAPANESE);
            departureTime = dateFmt.format(new Date());
            result = true;
        } else {
            if (AppPreference.getTicketSearchResults() != null) {

                departureTime = AppPreference.getTicketSearchResults().departureTime.replace(":","");
                result = true;
            } else {
                Timber.e("setDepartureTime>AppPreference.getTicketSearchResults() = null");
            }
        }
        return result;
    }

    private Boolean setEmbarkStopID() {
        boolean result = false;
        if (AppPreference.getSelectedTicketEmbarkData() != null) {
            embarkStopID = AppPreference.getSelectedTicketEmbarkData().stop_id;
            result = true;
        } else {
            Timber.e("setEmbarkStopID>AppPreference.getSelectedTicketEmbarkData() = null");
        }
        return result;
    }

    private Boolean setDisEmbarkStopID() {
        boolean result = false;
        if (AppPreference.getSelectedTicketDisembarkData() != null) {
            disEmbarkStopID = AppPreference.getSelectedTicketDisembarkData().stop_id;
            result = true;
        } else {
            Timber.e("setDisEmbarkStopID>AppPreference.getSelectedTicketDisembarkData() = null");
        }
        return result;
    }

    private Boolean setPageInfo(TicketTripPageInfo ticketTripPageInfo) {
        boolean result = false;

        if (null == ticketTripPageInfo) {
            result = true;
        } else {
            if (null != ticketTripPageInfo.trip_info) {
                page_info = ticketTripPageInfo;
                result = true;
            } else {
                Timber.e("setPageInfo>ticketTripPageInfo.trip_info = null");
            }
        }

        return result;
    }

    private Boolean setPrevNextTripType(String type) {
        prevNextTripType = type;
        return true;
    }

    public Boolean setInfo(Integer adultNum, Integer childNum, Integer babyNum, Integer adultDisabilityNum, Integer childDisabilityNum, Integer caregiverNum, TicketTripPageInfo ticketTripPageInfo, String tripType) {

        adultNumber = adultNum;
        childNumber = childNum;
        babyNumber = babyNum;
        adultDisabilityNumber = adultDisabilityNum;
        childDisabilityNumber = childDisabilityNum;
        caregiverNumber = caregiverNum;
        totalPeoples = adultNum + childNum + babyNum + adultDisabilityNum + childDisabilityNum + caregiverNum;

        if (!setServiceInstanceID()) return false;
        if (!setTicketClassID()) return false;
        if (!setDate()) return false;
        if (!setDepartureTime(tripType)) return false;
        if (!setEmbarkStopID()) return false;
        if (!setDisEmbarkStopID()) return false;
        if (!setPageInfo(ticketTripPageInfo)) return false;
        return setPrevNextTripType(tripType);
    }
}
