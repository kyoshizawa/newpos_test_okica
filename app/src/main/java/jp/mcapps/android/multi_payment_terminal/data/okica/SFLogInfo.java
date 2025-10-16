package jp.mcapps.android.multi_payment_terminal.data.okica;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * SFログ情報のブロックデータをオブジェクトのように扱うためのクラスです
 */
public class SFLogInfo {
    public static final int SERVICE_CODE = 0x84;

    private final byte[] bd;

    /**
     * コンストラクタ(Write用)
     */
    public SFLogInfo() {
        this.bd = new byte[16];
    }

    /**
     * コンストラクタ(Read用)
     *
     * @param blockData ブロックデータ
     */
    public SFLogInfo(byte[] blockData) {
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
     * 種別コードを取得します
     *
     * @return 種別コード 0: 鉄道・バス 1: 関連事業分野
     */
    public int getTypeCode() {
        return ( 0b1000_0000 & bd[0] ) >> 7;
    }

    /**
     * 種別コードを設定します
     *
     * @param code 0: 鉄道・バス 1: 関連事業分野
     * @return this
     */
    public SFLogInfo setTypeCode(int code) {
        bd[0] = (byte) ( ( ( 0b0000_0001 & code ) << 7 ) + ( 0b0111_1111 & bd[0] ) );

        return this;
    }

    /**
     * 機種コードを取得します
     *
     * @return 機種コード
     */
    public int getModelCode() {
        return 0b0111_1111 & bd[0];
    }

    /**
     * 機種コードを設定します
     *
     * @param code 機種コード
     * @return this
     */
    public SFLogInfo setModelCode(int code) {
        bd[0] = (byte) ( (0b0111_1111 & code) + (0b1000_0000 & bd[0]) );

        return this;
    }

    /**
     * SF内訳を取得します
     *
     * @return SF内訳
     */
    public int getSFUchiwake() {
        return (0b1000_0000 & bd[1]) >> 7;
    }

    /**
     * SF内訳を設定します
     *
     * @param val SF内訳
     * @return this
     */
    public SFLogInfo setSFUchiwake(int val) {
        bd[1] = (byte) ( ( ( 0b0000_0001 & val ) << 7 ) + ( 0b0111_1111 & bd[1] ) );

        return this;
    }

    /**
     * 処理種別を取得します
     *
     * @return 処理種別
     */
    public int getProcessingType() {
        return 0b0111_1111 & bd[1];
    }

    /**
     * 処理種別を設定します
     *
     * @param type 処理種別
     * @return this
     */
    public SFLogInfo setProcessingType(int type) {
        bd[1] = (byte) ( ( 0b0111_1111 & type ) + ( 0b1000_0000 & bd[1] ) );

        return this;
    }

    /**
     * 入金区分を取得します
     *
     * @return 入金区分
     */
    public int getDepositClass() {
        return 0b0111_1111 & bd[2];
    }

    /**
     * 入金区分を設定します
     *
     * @param val 入金区分
     * @return this
     */
    public SFLogInfo setDepositClass(int val) {
        bd[2] = (byte) ( ( 0b0111_1111 & val ) + ( 0b1000_0000 & bd[2] ) );

        return this;
    }

    /**
     * 利用駅種別を取得します
     *
     * @return 利用駅種別
     */
    public int getStationType() {
        return 0b1111_1111 & bd[3];
    }

    /**
     * 利用駅種別を設定します
     *
     * @param type 利用駅種別
     * @return this
     */
    public SFLogInfo setStationType(int type) {
        bd[3] = (byte) type;

        return this;
    }

    /**
     * 現在の日付を設定します
     * タイムゾーンはAsia/Tokyoに固定されます
     *
     * @return this
     */
    public SFLogInfo setCurrentDate() {
        final Calendar c = Calendar.getInstance();

        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        c.setTimeZone(tz);

        setYear(c.get(Calendar.YEAR) % 100);  // 下2桁だけ
        setMonth(c.get(Calendar.MONTH) + 1);  // 0 ~ 11なので+1
        setDate(c.get(Calendar.DATE));

        return this;
    }

    /**
     * ログ書き込みを行った年を取得します
     *
     * @return ログ書き込みを行った年(西暦下位2桁)
     */
    public int getYear() {
        return ( 0b1111_1110 & bd[4] ) >> 1;
    }

    /**
     * ログ書き込みを行う年を設定します
     *
     * @param year 年(西暦下位2桁)
     * @return this
     */
    public SFLogInfo setYear(int year) {
        bd[4] = (byte) ( ( ( 0b0111_1111 & year ) << 1 ) + ( 0b0000_0001 & bd[4] ) );

        return this;
    }

    /**
     * ログ書き込みを行った月を取得します
     *
     * @return ログ書き込みを行った月
     */
    public int getMonth() {
        return ( ( 0b0000_0001 & bd[4] ) << 3 ) + ( ( 0b1110_0000 & bd[5] ) >> 5 );
    }

    /**
     * ログ書き込みを行う月を設定します
     *
     * @param month 月
     * @return this
     */
    public SFLogInfo setMonth(int month) {
        bd[4] = (byte) ( ( ( 0b0000_1000 & month ) >> 3 ) + ( 0b1111_1110 & bd[4] ) );
        bd[5] = (byte) ( ( ( 0b0000_0111 & month ) << 5 ) + ( 0b0001_1111 & bd[5] ) );

        return this;
    }

    /**
     * ログ書き込みを行った日を取得します
     *
     * @return ログ書き込みを行った日
     */
    public int getDate() {
        return 0b0001_1111 & bd[5];
    }

    /**
     * ログ書き込みを行う日を設定します
     * @param date ログ書き込みを行う日
     * @return this
     */
    public SFLogInfo setDate(int date) {
        bd[5] = (byte) ( (0b0001_1111 & date) + (0b1110_0000 & bd[5]) );

        return this;
    }

    /**
     * 利用駅1を取得します
     *
     * @return 利用駅1
     */
    public int getStation1() {
        return ( ( 0xFF & bd[6] ) << 8 ) + ( 0xFF & bd[7] );
    }

    /**
     * 利用駅1を設定します
     *
     * @param code 利用駅
     * @return this
     */
    public SFLogInfo setStation1(int code) {
        bd[6] = (byte) ( ( 0xFF_00 & code ) >> 8 );
        bd[7] = (byte) ( ( 0x00_FF & code )      );

        return this;
    }

    /**
     * 利用駅2を取得します
     *
     * @return 利用駅2
     */
    public int getStation2() {
        return ( ( 0xFF & bd[8] ) << 8 ) + ( 0xFF & bd[9] );
    }

    /**
     * 利用駅2を設定します
     *
     * @param code 利用駅
     * @return this
     */
    public SFLogInfo setStation2(int code) {
        bd[8] = (byte) ( ( 0xFF_00 & code ) >> 8 );
        bd[9] = (byte) ( ( 0x00_FF & code )      );

        return this;
    }

    /**
     * 残額を取得します
     *
     * @return 残額
     */
    public int getBalance() {
        return ( 0xFF & bd[10] ) + ( ( 0xFF & bd[11] ) << 8 ) + ( ( 0xFF & bd[12] ) << 16 );
    }

    /**
     * 残額を設定します
     *
     * @param amount 残額
     * @return this
     */
    public SFLogInfo setBalance(int amount) {
        bd[10] = (byte) ( ( amount & 0x00_00_00_FF )       );
        bd[11] = (byte) ( ( amount & 0x00_00_FF_00 ) >> 8  );
        bd[12] = (byte) ( ( amount & 0x00_FF_00_00 ) >> 16 );

        return this;
    }

    /**
     * SFログIDを取得します
     *
     * @return SFログID
     */
    public int getSFLogId() {
        return ( ( 0xFF & bd[13] ) << 8 ) + ( 0xFF & bd[14] );
    }

    /**
     * SFログIDを設定します
     *
     * @param id SFログID
     * @return this
     */
    public SFLogInfo setSFLogId(int id) {
        bd[13] = (byte) ( ( 0xFF_00 & id ) >> 8 );
        bd[14] = (byte) ( ( 0x00_FF & id )      );

        return this;
    }

    // 地域識別コード 利用駅1

    /**
     * 地域識別コード-利用駅1を取得します
     * @return 地域識別コード-利用駅1
     */
    public int getRegionCodeStation1() {
        return ( 0b1100_0000 & bd[15] ) >> 6;
    }

    /**
     * 地域識別コード-利用駅1を設定します
     *
     * @param code 利用駅
     * @return this
     */
    public SFLogInfo setRegionCodeStation1(int code) {
        bd[15] = (byte) ( ( ( 0b0000_0011 & code ) << 6 ) + (0b0011_1111 & bd[15]) );

        return this;
    }

    /**
     * 地域識別コード-利用駅2を取得します
     * @return 地域識別コード-利用駅2
     */
    public int getRegionCodeStation2() {
        return ( 0b0011_0000 & bd[15] ) >> 4;
    }

    /**
     * 地域識別コード-利用駅2を設定します
     *
     * @param code 利用駅
     * @return this
     */
    public SFLogInfo setRegionCodeStation2(int code) {
        bd[15] = (byte) ( ( ( 0b0000_0011 & code ) << 4 ) + ( 0b1100_1111 & bd[15] ) );

        return this;
    }

    /**
     * コピーオブジェクトを生成します
     *
     * @return コピーオブジェクト
     */
    public SFLogInfo copy() {
        return new SFLogInfo(Arrays.copyOf(bd, bd.length));
    }

    @Override
    @NotNull
    public String toString() {
        return "----- SFログ情報 -----\n" +
                "種別コード: " + getTypeCode() + "\n" +
                "機種コード: " + getModelCode() + "\n" +
                "SF内訳: " + getSFUchiwake() + "\n" +
                "処理種別詳細: " + getProcessingType() + "\n" +
                "入金区分: " + getDepositClass() + "\n" +
                "利用駅種別: " + getStationType() + "\n" +
                "年: " + getYear() + "\n" +
                "月: " + getMonth() + "\n" +
                "日: " + getDate() + "\n" +
                "利用駅1: " + getStation1() + "\n" +
                "利用駅2: " + getStation2() + "\n" +
                "残額: " + getBalance() + "\n" +
                "SFログID: " + getSFLogId() + "\n" +
                "地域識別コード 利用駅1: " + getRegionCodeStation1() + "\n" +
                "地域識別コード 利用駅2: " + getRegionCodeStation2();
    }
}
