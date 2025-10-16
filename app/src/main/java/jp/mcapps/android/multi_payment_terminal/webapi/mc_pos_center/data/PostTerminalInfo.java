package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import android.annotation.SuppressLint;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoEdy;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoId;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoNanaco;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoQuicpay;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoSuica;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningInfoWaon;
import timber.log.Timber;

import jp.mcapps.android.multi_payment_terminal.util.SimUtils;

public class PostTerminalInfo {
    public static class Request {
        @Expose
        public String terminalNo;       //端末番号

        @Expose
        public String runStart;         //端末が記録した最終業務開始の時刻。存在しない場合はnull

        @Expose
        public String runEnd;           //端末が記録した最終業務終了の時刻。存在しない場合はnull

        @Expose
        @SerializedName("JR_Sprwid")
        public String jrSprwid;         //交通系の物販端末ID。未取得ならnull

        @Expose
        @SerializedName("JR_OpenResult")
        public Boolean jrOpenResult;    //交通系の開局結果

        @Expose
        @SerializedName("JR_OpenTime")
        public String jrOpenTime;       //交通系の開局時間。(YYYYMMDDHHMMSS)。未開局ならnull

        @Expose
        @SerializedName("QP_TID")
        public String qpTid;           //QPの物販端末ID。未取得ならnull

        @Expose
        @SerializedName("QP_OpenResult")
        public Boolean qpOpenResult;    //QPの開局結果

        @Expose
        @SerializedName("QP_OpenTime")
        public String qpOpenTime;       //QPの開局時間。(YYYYMMDDHHMMSS)。未開局ならnull

        @Expose
        @SerializedName("iD_TID")
        public String idTid;            //iDの物販端末ID。未取得ならnull

        @Expose
        @SerializedName("iD_OpenResult")
        public Boolean idOpenResult;    //iDの開局結果

        @Expose
        @SerializedName("iD_OpenTime")
        public String idOpenTime;       //iDの開局時間。(YYYYMMDDHHMMSS)。未開局ならnull

        @Expose
        @SerializedName("ED_Htid")
        public String edHtid;           //EDyの上位端末ID。未取得ならnull

        @Expose
        @SerializedName("ED_OpenResult")
        public Boolean edOpenResult;    //Edyの開局結果

        @Expose
        @SerializedName("ED_OpenTime")
        public String edOpenTime;       //Edyの開局時間。(YYYYMMDDHHMMSS)。未開局ならnull

        @Expose
        @SerializedName("WN_Sprwid")
        public String wnSprwid;         //WAONの物販端末ID。未取得ならnull

        @Expose
        @SerializedName("WN_OpenResult")
        public Boolean wnOpenResult;    //WAONの開局結果

        @Expose
        @SerializedName("WN_OpenTime")
        public String wnOpenTime;       //WAONの開局時間。(YYYYMMDDHHMMSS)。未開局ならnull

        @Expose
        @SerializedName("NN_Htid")
        public String nnHtid;           //nanacoの上位端末ID。未取得ならnull

        @Expose
        @SerializedName("NN_OpenResult")
        public Boolean nnOpenResult;    //nanacoの開局結果

        @Expose
        @SerializedName("NN_OpenTime")
        public String nnOpenTime;       //nanacoの開局時間。(YYYYMMDDHHMMSS)。未開局ならnull

        @Expose
        @SerializedName("ICCID")
        public String iccId;            //端末にセットされているSIMを特定する情報。

        @Expose
        @SerializedName("Ifbox_Serial")
        public String ifboxSerial;      //IM-820のシリアル番号、未連携の場合は項目なし

        @Expose
        @SerializedName("Ifbox_Firmware")
        public String ifboxFirmware;    //IM-A820のファームウェア（メーター）種別、未連携の場合は項目なし

        @Expose
        @SerializedName("Ifbox_Version")
        public String ifboxVersion;     //IM-A820のファームウェアバージョン、未連携の場合は項目なし



        //type：送信種別
        //      0：電マネ開局(自動/手動)
        //      1：業務終了
        @SuppressLint({"HardwareIds", "MissingPermission"})
        public Request(int type) {
            //日時のフォーマット変換用 YYYY/MM/DD HH:MM:SS -> YYYYMMDDHHMMSS
            final String regex = "[/: ]";
            final String replacement = "";

            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
            String runDate = dateFmt.format(new Date());

            terminalNo = AppPreference.getMcTermId();
            //業務開始、再開局時はrunStart、業務終了時はrunEndのみ設定
            if (type == 0) {
                runStart = runDate;
            } else {
                runEnd = runDate;
            }

            OpeningInfoSuica infoSuica = EmoneyOpeningInfo.getSuica();
            if (infoSuica != null) {
                jrSprwid = infoSuica.sprwid;
                jrOpenResult = infoSuica.result;
                jrOpenTime = AppPreference.getDatetimeOpeningSuica().replaceAll(regex, replacement);
            } else {
                jrOpenResult = false;
            }

            OpeningInfoQuicpay infoQuicpay = EmoneyOpeningInfo.getQuicpay();
            if (infoQuicpay != null) {
                qpTid = infoQuicpay.termIdentId;
                qpOpenResult = infoQuicpay.mresult;
                qpOpenTime = AppPreference.getDatetimeOpeningQuicpay().replaceAll(regex, replacement);
            } else {
                qpOpenResult = false;
            }

            OpeningInfoId infoId = EmoneyOpeningInfo.getId();
            if (infoId != null) {
                idTid = infoId.termIdentId;
                idOpenResult = infoId.mresult;
                idOpenTime = AppPreference.getDatetimeOpeningId().replaceAll(regex, replacement);
            } else {
                idOpenResult = false;
            }

            OpeningInfoEdy infoEdy = EmoneyOpeningInfo.getEdy();
            if (infoEdy != null) {
                edHtid = infoEdy.termIdentId;
                edOpenResult = infoEdy.mresult;
                edOpenTime = AppPreference.getDatetimeOpeningEdy().replaceAll(regex, replacement);
            } else {
                edOpenResult = false;
            }

            OpeningInfoWaon infoWaon = EmoneyOpeningInfo.getWaon();
            if (infoWaon != null) {
                wnSprwid = infoWaon.termIdentId;
                wnOpenResult = infoWaon.mresult;
                wnOpenTime = AppPreference.getDatetimeOpeningWaon().replaceAll(regex, replacement);
            } else {
                wnOpenResult = false;
            }

            OpeningInfoNanaco infoNanaco = EmoneyOpeningInfo.getNanaco();
            if (infoNanaco != null) {
                nnHtid = infoNanaco.termIdentId;
                nnOpenResult = infoNanaco.mresult;
                nnOpenTime = AppPreference.getDatetimeOpeningNanaco().replaceAll(regex, replacement);
            } else {
                nnOpenResult = false;
            }

            try {
//                iccId = null;
//                TelephonyManager tm = (TelephonyManager)MainApplication.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
//                iccId = tm.getSimSerialNumber();
                iccId = SimUtils.getIccId(MainApplication.getInstance());
            } catch (Exception e) {
                Timber.e(e);
            }

            ifboxSerial = null;
            ifboxFirmware = null;
            ifboxVersion = null;
            if (AppPreference.getIFBoxVersionInfo() != null) {
                ifboxSerial = AppPreference.getIFBoxVersionInfo().mcSerial;
                ifboxFirmware = AppPreference.getIFBoxVersionInfo().appModel;
                ifboxVersion = AppPreference.getIFBoxVersionInfo().appVersion;
            }
        }
    }

    public static class Response {
        @Expose
        public Boolean result; //処理の成否。正常：true 、異常：false

        @Expose
        public String errorCode; //処理が異常な場合のコード。Result：trueの場合、本項目は""となる。
    }
}
