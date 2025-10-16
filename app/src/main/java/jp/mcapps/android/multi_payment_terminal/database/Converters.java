package jp.mcapps.android.multi_payment_terminal.database;

import androidx.room.TypeConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    public static Integer stringToInteger(String value) {
        return value == null ? null : Integer.parseInt(value);
    }

    public static Long stringToLong(String value) {
        return value == null ? null : Long.parseLong(value);
    }

    public static String dateFormat(String value) {
        Date date = new Date();
        if (value != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
            try {
                //取引日時のフォーマット変換
                date = fmt.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
                return "";
            }
        }

        if(date == null) {
            return "";
        }

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        return sdf.format(date);
    }

    public static String tenantMaskSuica(String value) {
        if (value == null) {
            return null;
        }
        return value.substring(0,2) + "***********" + value.substring(value.length() - 4);
    }

    public static String tenantMaskOkica(String value) {
        if (value == null) {
            return null;
        }
        return "*************" + value.substring(value.length() - 4);
    }

    //ADD-S BMT S.Oyama 2024/10/01 フタバ双方向向け改修
    public static String integerToString(Integer value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    public static String longToString(Long value) {
        if (value == null) {
            return "";
        }
        return value.toString();
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

    public static int intToIntBCD(int num, int tmpLen) {
        int bcd = 0;
        int shift = 0;

        if (tmpLen == 0) {
            return 0;
        }

        while ((num > 0) && (tmpLen > 0)) {
            int digit = num % 10;
            bcd |= (digit << (shift * 4));
            num /= 10;
            shift++;
            tmpLen--;
        }
        return bcd;
    }

    public static long longToIntBCD(long num, int tmpLen) {
        long bcd = 0;
        long shift = 0;

        if (tmpLen == 0) {
            return 0;
        }

        while ((num > 0) && (tmpLen > 0)) {
            long digit = num % 10;
            bcd |= (digit << (shift * 4));
            num /= 10;
            shift++;
            tmpLen--;
        }
        return bcd;
    }

    public static String repeatSpace(int n) {
        StringBuilder sb = new StringBuilder();
        while(n-- > 0) sb.append(" ");
        return sb.toString();
    }
    //ADD-E BMT S.Oyama 2024/10/01 フタバ双方向向け改修

}