package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import java.util.Calendar;
import java.util.TimeZone;

public class OkicaDateUtils {

    /**
     * 日付の検証を行います
     *
     * @param year  年(西暦下位2桁)
     * @param month 月
     * @param date  日
     */
    public static boolean validateDate(int year, int month, int date, boolean allowEmpty) {
        if (allowEmpty && (year == 0 && month == 0 && date == 0)) {
            return true;
        }

        return  ( 0 <= year && year <= 99 ) &&  // 0年 ~ 99年
                ( 1 <= month && month <= 12 ) &&  // 1月 ~ 12月
                ( 1 <= date && date <= getLastDay(year, month) );  // 1日 ~ その月の最終日
    }

    /**
     * 閏年かどうかを判定します
     *
     * @param year 年(西暦下位2桁)
     * @return 閏年の場合はtrue
     */
    public static boolean isLeapYear(int year) {
        // 下位2桁しかないので100の倍数400の倍数判定はできない2400年まで発生しないので0の時は閏年にしない
        return year % 4 == 0 && year != 0;
    }

    /**
     * 現在日付を返却します
     *
     * @return 現在日付 1バイト目: 日 2バイト目: 月 3バイト目: 年
     */
    public static int nowDate() {
        final Calendar c = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        c.setTimeZone(tz);

        return ( ( c.get(Calendar.YEAR) % 100) << 16 ) +  ( ( c.get(Calendar.MONTH) + 1 ) << 8 ) + c.get(Calendar.DATE);
    }

    /**
     * 運用日付を日付を返却します
     *
     * @return 運用日付 1バイト目: 日 2バイト目: 月 3バイト目: 年
     */
    public static int getOperationDate() {
        final Calendar c = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        c.setTimeZone(tz);
        c.add(Calendar.HOUR_OF_DAY, -4);

        return ( ( c.get(Calendar.YEAR) % 100) << 16 ) +  ( ( c.get(Calendar.MONTH) + 1 ) << 8 ) + c.get(Calendar.DATE);
    }

    /**
     * 起算日を返却します (実日付+1日)
     *
     * @param year 年(実日付)
     * @param month 月(実日付)
     * @param date 日(実日付)
     *
     * @return 起算日 1バイト目: 日 2バイト目: 月 3バイト目: 年
     */
    public static int getKisanbi(int year, int month, int date) {
        // ほんとはオブジェクトで年月日を返したいけど面倒なのでintに詰め込む

        // 12月31日の場合は翌年の1月1日にする
        if (month == 12 && date == 31) {
            year += 1;
            month = 1;
            date  = 1;
        }
        // 月の最終日だった場合翌月の1日にする
        else if (date+1 > getLastDay(year, month)) {
            month += 1;
            date = 1;
        }
        // それ以外は日付をインクリメントする
        else {
            date += 1;
        }

        return ( year << 16 ) + ( month << 8 ) + date;
    }

    /**
     * 10年失効判定の判定日付を返却します
     *
     * @param year 年(実日付)
     * @param month 月(実日付)
     * @param date 日(実日付)
     *
     * @return 10年失効判定の日付 1バイト目: 日 2バイト目: 月 3バイト目: 年
     */
    public static int get10yearsJudgeDate(int year, int month, int date) {
        int kisanbi = getKisanbi(year, month, date);
        int y = ( ( 0xFF_00_00 & kisanbi ) >> 16 ) + 10;
        int m = ( ( 0x00_FF_00 & kisanbi ) >> 8 );
        int d = 0x00_00_FF & kisanbi;

        // 1月1日の場合は前年の12月31日にする
        if (d == 1 && m == 1) {
            y -= 1;
            m = 12;
            d = 31;
        }
        // 1月以外で1日の場合は前月の最終日にする
        else if (d == 1) {
            m = m -1;
            d = getLastDay(y, m);
        }
        // それ以外は日付をデクリメントする
        else {
            d -= 1;
        }

        return ( y << 16 ) + ( m << 8 ) + d;
    }

    /**
     * その月の最終日を返却します
     *
     * @param year 年
     * @param month 月
     *
     * @return 最終日
     */
    public static int getLastDay(int year, int month) {
        switch (month) {
            // 31日まである月
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                return 31;
            // 30日までの月
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            case 2:
                return isLeapYear(year) ? 29 : 28;
        }

        return -1;
    }
}
