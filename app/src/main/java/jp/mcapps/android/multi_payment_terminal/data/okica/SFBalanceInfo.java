package jp.mcapps.android.multi_payment_terminal.data.okica;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * SF残額情報のブロックデータをオブジェクトのように扱うためのクラスです
 */
public class SFBalanceInfo {
    public static final int SERVICE_CODE = 0x82;
    private final byte[] bd;

    /**
     * コンストラクタ
     *
     * @param blockData ブロックデータ
     */
    public SFBalanceInfo(byte[] blockData) {
        this.bd = blockData;
    }

    /**
     * ブロックデータを取得します
     *
     * @return ブロックデータ
     */
    public byte[] getBlockData() {
        return bd;
    }

    /**
     * 残額を取得します
     *
     * @return 残額
     */
    public int getBalance() {
        // 他のブロックでは金額が3バイト扱いなので4バイト目は無視する
        return ( 0xFF & bd[0] ) + ( ( 0xFF & bd[1] ) << 8 ) + ( ( 0xFF & bd[2] ) << 16 );
    }

    /**
     * 残額を設定します
     *
     * @param amount 残額
     * @return this
     */
    public SFBalanceInfo setBalance(int amount) {
        bd[0] = (byte) ( ( amount & 0x00_00_00_FF )       );
        bd[1] = (byte) ( ( amount & 0x00_00_FF_00 ) >> 8  );
        bd[2] = (byte) ( ( amount & 0x00_FF_00_00 ) >> 16 );

        return this;
    }

    /**
     * 積増処理を行なった最新の年を取得します
     *
     * @return 積増処理を行なった最新の年(西暦下位2桁)
     */
    public int getYear() {
        return ( 0b1111_1110 & bd[8] ) >> 1;
    }

    /**
     * 積増処理を行う年を設定します
     *
     * @param year 年(西暦下位2桁)
     * @return this
     */
    public SFBalanceInfo setYear(int year) {
        bd[8] = (byte) ( ( ( 0b0111_1111 & year ) << 1 ) + ( 0b0000_0001 & bd[8] ) );

        return this;
    }

    /**
     * 積増処理を行なった最新の月を取得します
     *
     * @return 積増処理を行なった最新の月
     */
    public int getMonth() {
        return ( ( 0b0000_0001 & bd[8] ) << 3 ) + ( ( 0b1110_0000 & bd[9] ) >> 5 );
    }

    /**
     * 積増処理を行う月を設定します
     *
     * @param month 月
     * @return this
     */
    public SFBalanceInfo setMonth(int month) {
        bd[8] = (byte) ( ( ( 0b0000_1000 & month ) >> 3 ) + ( 0b1111_1110 & bd[8] ) );
        bd[9] = (byte) ( ( ( 0b0000_0111 & month ) << 5 ) + ( 0b0001_1111 & bd[9] ) );

        return this;
    }

    /**
     * 積増処理を行なった最新の日を取得します
     *
     * @return 積増処理を行なった最新の日
     */
    public int getDate() {
        return 0b0001_1111 & bd[9];
    }

    /**
     * 積増処理を行う日を設定します
     *
     * @param date 日
     * @return this
     */
    public SFBalanceInfo setDate(int date) {
        bd[9] = (byte) ( (0b0001_1111 & date) + (0b1110_0000 & bd[9]) );

        return this;
    }

    /**
     * 積増処理を行なった最新の時を取得します
     *
     * @return 積増処理を行なった最新の時
     */
    public int getHour() {
        return ( 0b1111_1000 & bd[10] ) >> 3;
    }

    /**
     * 積増処理を行う時を設定します
     *
     * @param hour 時
     * @return this
     */
    public SFBalanceInfo setHour(int hour) {
        bd[10] = (byte) ( ( ( 0b0001_1111 & hour ) << 3 ) + ( 0b0000_0111 & bd[10] ) );

        return this;
    }

    /**
     * 積増処理を行なった最新の分を取得します
     *
     * @return 積増処理を行なった最新の分
     */
    public int getMinute() {
        return ( ( 0b0000_0111 & bd[10] ) << 3) + ( ( 0b1110_0000 & bd[11]) >> 5 );
    }

    /**
     * 積増処理を行う時を設定します
     *
     * @param minute 分
     * @return this
     */
    public SFBalanceInfo setMinute(int minute) {
        bd[10] = (byte) ( ( ( 0b0011_1000 & minute ) >> 3 ) + ( 0b1111_1000 & bd[10] ) );
        bd[11] = (byte) ( ( ( 0b0000_0111 & minute ) << 5 ) + ( 0b0001_1111 & bd[11] ) );

        return this;
    }

    /**
     * 現在の日付を設定します
     * タイムゾーンはAsia/Tokyoに固定されます
     *
     * @return this
     */
    public SFBalanceInfo setCurrentDate() {
        final Calendar c = Calendar.getInstance();

        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        c.setTimeZone(tz);

        setYear(c.get(Calendar.YEAR) % 100);  // 下2桁だけ
        setMonth(c.get(Calendar.MONTH) + 1);  // 0 ~ 11なので+1
        setDate(c.get(Calendar.DAY_OF_MONTH));
        setHour(c.get(Calendar.HOUR_OF_DAY));
        setMinute(c.get(Calendar.MINUTE));

        return this;
    }

    /**
     * 地域番号を取得します
     *
     * @return 地域番号
     */
    public int getRegionNo() {
        return 0b0001_1111 & bd[11];
    }

    /**
     * 地域番号を設定します
     *
     * @param no 地域番号
     * @return this
     */
    public SFBalanceInfo setRegionNo(int no) {
        bd[11] = (byte) ( ( 0b0001_1111 & no ) + ( 0b1110_0000 & bd[11] ) );

        return this;
    }

    /**
     * ユーザコードを取得します
     *
     * @return ユーザコード
     */
    public int getUserCode() {
        return 0xFF & bd[12];
    }

    /**
     * ユーザコードを設定します
     *
     * @param code ユーザーコード
     * @return this
     */
    public SFBalanceInfo setUserCode(int code) {
        bd[12] = (byte) ( 0xFF & code );

        return this;
    }

    /**
     * 入金区分を取得します
     *
     * @return 入金区分
     */
    public int getDepositClass() {
        return 0b0111_1111 & bd[13];
    }

    /**
     * 入金区分を設定します
     *
     * @return this
     */
    public SFBalanceInfo setDepositClass(int cls) {
        bd[13] = (byte) ( 0xFF & cls );

        return this;
    }

    /**
     * 実行IDを取得します
     *
     * @return 実行ID
     */
    public int getExecId() {
        return ( ( 0xFF & bd[14] ) << 8 ) + ( 0xFF & bd[15] );
    }

    /**
     * 実行IDをインクリメントします
     *
     * @return this
     */
    public SFBalanceInfo incrementExecId() {
        int nextId = getExecId() + 1;

        bd[14] = (byte) ((0xFF_00 & nextId)>>8);
        bd[15] = (byte) ((0x00_FF & nextId)>>0);

        return this;
    }

    /**
     * コピーオブジェクトを生成します
     *
     * @return コピーオブジェクト
     */
    public SFBalanceInfo copy() {
        return new SFBalanceInfo(Arrays.copyOf(bd, bd.length));
    }

    @Override
    public String toString() {
        return "----- SF残額情報 -----\n" +
                "残額: " + getBalance() + "\n" +
                "年: " + getYear() + "\n" +
                "月: " + getMonth() + "\n" +
                "日: " + getDate() + "\n" +
                "時: " + getHour() + "\n" +
                "分: " + getMinute() + "\n" +
                "地域番号: " + getRegionNo() + "\n" +
                "ユーザコード: " + getUserCode() + "\n" +
                "入金区分: " + getDepositClass() + "\n" +
                "実行ID: "+ getExecId();
    }
}
