package jp.mcapps.android.multi_payment_terminal.data;

import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoEdy;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoId;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoNanaco;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoQuicpay;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoSuica;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoWaon;

public class EmoneyOpeningInfo {
    private static OpeningInfoSuica _suica;
    public static OpeningInfoSuica getSuica() { return _suica; }
    public static void setSuica(OpeningInfoSuica suica) { _suica = suica; }

    private static OpeningInfoWaon _waon;
    public static OpeningInfoWaon getWaon() { return _waon; }
    public static void setWaon(OpeningInfoWaon waon) { _waon = waon; }

    private static OpeningInfoNanaco _nanaco;
    public static OpeningInfoNanaco getNanaco() { return _nanaco; }
    public static void setNanaco(OpeningInfoNanaco nanaco) { _nanaco = nanaco; }

    private static OpeningInfoEdy _edy;
    public static OpeningInfoEdy getEdy() { return _edy; }
    public static void setEdy(OpeningInfoEdy edy) { _edy = edy; }

    private static OpeningInfoId _id;
    public static OpeningInfoId getId() { return _id; }
    public static void setId(OpeningInfoId id) { _id = id; }

    private static OpeningInfoQuicpay _quicpay;
    public static OpeningInfoQuicpay getQuicpay() { return _quicpay; }
    public static void setQuicpay(OpeningInfoQuicpay quicpay) { _quicpay = quicpay; }
}
