package jp.mcapps.android.multi_payment_terminal.ui;

import static java.lang.Integer.parseInt;
import static jp.mcapps.android.multi_payment_terminal.data.okica.Constants.COMPANY_CODE_BUPPAN;

import android.content.Context;

import androidx.databinding.InverseMethod;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;

public class Converters {
    @InverseMethod("stringToDate")
    public static String dateToString(Date value) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);

        if (value == null) return "";
        return sdf.format(value);
    }

    public static Date stringToDate(String value) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);

        try {
            final Date date = sdf.parse(value);
            return date;
        } catch (Exception ex) {
            return new Date();
        }
    }

    @InverseMethod("stringToInteger")
    public static String integerToString(Integer value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static Integer stringToInteger(String value) {
        return Integer.parseInt(value);
    }

    @InverseMethod("numberFormatToInteger")
    public static String integerToNumberFormat(Integer value) {
        if (value == null) {
            return null;
        }
        return NumberFormat.getNumberInstance().format(value);
    }

    public static Integer numberFormatToInteger(String value) {
        return parseInt(Objects.requireNonNull(
                value.replace(",", "")));
    }

    @InverseMethod("numberFormatToString")
    public static String stringToNumberFormat(String value) {
        if (value == null) {
            return null;
        }
        return NumberFormat.getNumberInstance().format(Integer.valueOf(value));
    }

    public static String numberFormatToString(String value) {
        if (value == null) {
            return null;
        }
        return value.replace(",", "");
    }

    private final static String RADIO_UNAVAILABLE_TEXT = "取得失敗";

    public static String integerToRadioParam(Integer value) {
        if (value == null) return RADIO_UNAVAILABLE_TEXT;
        return value.toString();
    }

    public static String integerToDbmFormat(Integer value) {
        if (value == null) return RADIO_UNAVAILABLE_TEXT;
        return String.valueOf((double)value / 10);
    }

    @InverseMethod("stringToFloat")
    public static String floatToString(Float value) {
        return String.valueOf(value);
    }

    //小数点以下の桁数を指定するメソッド
    public static String floatToString(Float value, int digit) {
        return String.format("%." + digit + "f", value);
    }

    //速度の単位変換 m/s -> km/h
    public static String speedToString(Float value) {
        if (value == null) return "";
        return String.format(Locale.JAPANESE, "%.1f", value * 60 * 60 / 1000);
    }

    public static Float stringToFloat(String value) {
        return Float.parseFloat(value);
    }

    @InverseMethod("stringToDouble")
    public static String doubleToString(Double value) {
        return String.valueOf(value);
    }

    //小数点以下の桁数を指定するメソッド
    public static String doubleToString(Double value, int digit) {
        return String.format("%." + digit + "f", value);
    }

    public static Double stringToDouble(String value) {
        return Double.parseDouble(value);
    }

    public static String booleanToEnabledText (boolean b) {
        return MainApplication.getInstance().getString(b ? R.string.enable : R.string.disable);
    }

    public static int booleanToEnabledColor (boolean b) {
        Context appContext = MainApplication.getInstance();

        return appContext.getResources().getColor(
                b ? R.color.black : R.color.gray, appContext.getTheme());

    }

    //取引種別の変換
    public static String transTypeToString (int key) {
        return TransMap.getType(key);
    }

    //取引結果の変換
    public static String transResultToString (int key) {
        return TransMap.getResult(key);
    }

    public static Boolean is0(Integer n) {
        return n == 0;
    }

    //「0」~「9」ボタン有効判定
    public static Boolean isButtonEnabled(Integer ChargeAmount,Integer InputAmount) {
        boolean Button = true;

        /* チャージ限度額 */
        int chargeMaxAmount;
        if (!AppPreference.isDemoMode()) {
            final ICMaster.Activator activator = MainApplication.getInstance().getOkicaICMaster().getData().getActivator(COMPANY_CODE_BUPPAN);
            chargeMaxAmount = activator.getPurseLimitAmount();
        } else {
            chargeMaxAmount = 30_000;
        }

        /* チャージ限度額の桁数 */
        int chargeMaxAmountLength = String.valueOf(chargeMaxAmount).length();
        /* チャージ金額の桁数 */
        int chargeAmountLength = ChargeAmount.toString().length();

        if(chargeAmountLength == chargeMaxAmountLength) {
            /* チャージ金額の桁数がチャージ限度額のMAX桁数と一致した場合、「0」~「9」を無効 */
            Button = false;
        }else if(chargeAmountLength == (chargeMaxAmountLength-1)){
            /* チャージ金額の桁数がチャージ限度額のMAX桁数より1桁少ない場合 */
            if(ChargeAmount == (chargeMaxAmount/10) && InputAmount != 0){
                /* 条件を満たす場合、「1」~「9」を無効 */
                Button = false;
            }else if(ChargeAmount == (chargeMaxAmount/10000*1000) && ChargeAmount < (chargeMaxAmount/10) && (chargeMaxAmount%10000) < InputAmount){
                /* 条件を満たす場合、そのボタンを無効 */
                Button = false;
            }else if(ChargeAmount > (chargeMaxAmount/10000*1000)){
                /* 条件を満たす場合、「0」~「9」を無効 */
                Button = false;
            }
        }else if(chargeAmountLength == 1){
            /* チャージ金額の桁数が1桁の場合(未入力状態) */
            if(InputAmount == 0){
                /* 「0」を無効 */
                Button = false;
            }else if(InputAmount > chargeMaxAmount){
                /* 条件を満たす場合、そのボタンを無効 */
                Button = false;
            }
        }

        return Button;
    }

    public static String chargeMessage(){
        String strChargeMessage = "";
        /* チャージ限度額 */
        int chargeMaxAmount;
        if (!AppPreference.isDemoMode()) {
            final ICMaster.Activator activator = MainApplication.getInstance().getOkicaICMaster().getData().getActivator(COMPANY_CODE_BUPPAN);
            chargeMaxAmount = activator.getPurseLimitAmount();
        } else {
            chargeMaxAmount = 30_000;
        }
        strChargeMessage = "(※チャージ限度額："+ Converters.integerToNumberFormat(chargeMaxAmount) + "円)";

        return strChargeMessage;
    }

    public static String longToNumberFormat(Long value) {
        if (value == null) {
            return null;
        }
        return NumberFormat.getNumberInstance().format(value);
    }

    public static Boolean stringToBoolean(String string) {
        return !(string == null || string == "");
    }

    //有効性確認結果の変換 仮置き
    public static String validationCheckResultToString (int key) {
        switch (key) {
            case 0:
                return "OK";
            case 1:
                return "NG";
            default:
                return "未確認";
        }
    }

    public static Boolean isMoneyOkicaEnabled() {
        return AppPreference.isMoneyOkica();
    }

    public static Boolean isOkicaTerminalInfoNull() {
        if (AppPreference.getOkicaTerminalInfo() != null) {
            return false;
        } else {
            return true;
        }
    }

    public static Boolean isIFBoxSetupFinished() {
        return AppPreference.isIFBoxSetupFinished() || isTabletSetupFinished();
    }

    public static Boolean isTabletSetupFinished() {
        if (AppPreference.getTabletLinkInfo() != null) {
            return true;
        } else {
            return false;
        }
    }

    public static String getIfboxInfo() {
        String ret = "";
        String model, ver;

        if (AppPreference.getIFBoxVersionInfo() != null) {
            model = AppPreference.getIFBoxVersionInfo().appModel;
            if (model != null) {
                model = model.replace("YAZAKI", "矢崎");
                model = model.replace("FUTABA", "二葉");
                model = model.replace("NISHIBE", "ニシベ");
                model = model.replace("OKABE", "岡部");
                model = model.replace("/", "");
                model = model.replace("-D", "双方向");
            } else {
                model = "";
            }
            ver = AppPreference.getIFBoxVersionInfo().appVersion;
        } else {
            model = "";
            ver = "";
        }
        ret = model + ":" + ver;

        return ret;
    }

    public static String convertDate(String str) {
        String date = String.valueOf(str.toCharArray(), 0, 4);
        date += '/';
        date += String.valueOf(str.toCharArray(), 4, 2);
        date += '/';
        date += String.valueOf(str.toCharArray(), 6, 2);
        return date;
    }

    public static String convertTime(String str) {
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

    public static String convertDatetime(String str) {
        String datetime = convertDate(str);
        datetime += ' ';
        datetime += convertTime(str.substring(8));
        return datetime;
    }

}
