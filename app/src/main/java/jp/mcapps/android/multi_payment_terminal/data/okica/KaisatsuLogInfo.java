package jp.mcapps.android.multi_payment_terminal.data.okica;

import org.jetbrains.annotations.NotNull;

/**
 * 改札ログ情報のブロックデータをオブジェクトのように扱うためのクラスです
 */
public class KaisatsuLogInfo {
    public static final int SERVICE_CODE = 0x86;

    private final byte[] bd;

    /**
     * コンストラクタ
     *
     * @param blockData ブロックデータ
     */
    public KaisatsuLogInfo(byte[] blockData) {
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
     * 入場か出場かを取得します
     *
     * @return 0: 出場 1: 入場
     */
    public int getEnterOrExit() {
        return (0b1000_0000 & bd[0]) >> 7;
    }

    /**
     * 定期利用かどうかを取得します
     *
     * @return 定期利用の場合trueを返します
     */
    public boolean usedTeiki() {
        return (0b0100_0000 & bd[0]) != 0;
    }

    /**
     * SF利用かどうかを取得します
     *
     * @return SF利用の場合はtrueを返します
     */
    public boolean usedSF() {
        return (0b0010_0000 & bd[0]) != 0;
    }

    /**
     * 駅コードを取得します
     *
     * @return 駅コード
     */
    public int getStationCode() {
        return ( ( 0xFF & bd[2] ) << 8 ) + ( 0xFF & bd[3] );
    }

    /**
     * コーナコードを取得します
     *
     * @return コーナコード
     */
    public int getCornerCode() {
        return ( 0b1111_0000 & bd[4] ) >> 4;
    }

    /**
     * 号機番号を取得します
     *
     * @return 号機番号
     */
    public int getUnitNo() {
        return ( ( 0b0000_1111 & bd[4] ) << 8 ) + ( 0xFF & bd[5] );
    }

    /**
     * 利用年を取得します
     *
     * @return 利用年
     */
    public int getYear() {
        return ( 0b1111_1110 & bd[6] ) >> 1;
    }

    /**
     * 利用月を取得します
     *
     * @return 利用月
     */
    public int getMonth() {
        return ( ( 0b0000_0001 & bd[6] ) << 3 ) + ( ( 0b1110_0000 & bd[7] ) >> 5 );
    }

    /**
     * 利用日を取得します
     *
     * @return 利用日
     */
    public int getDate() {
        return 0b0001_1111 & bd[7];
    }

    /**
     * 時刻を取得します
     *
     * @return 時刻(BCD 2バイト)
     */
    public int getTime() {
        return  ( ( 0xFF & bd[8] )  << 8 ) + ( 0xFF & bd[9] );
    }

    /**
     * ピーク運賃を取得します
     *
     * @return ピーク運賃(0 ~ 65535円)
     */
    public int getPeakFare() {
        return ( 0xFF & bd[10] ) + ( ( 0xFF & bd[11] ) << 8 );
    }

    /**
     * 仮清算金を取得します
     *
     * @return 仮清算金(0 ~ 65535円)
     */
    public int getKariSeisanKin() {
        return ( 0xFF & bd[12] ) + ( ( 0xFF & bd[13] ) << 8 );
    }

    /**
     * 仮清算駅を取得します
     *
     * @return 線区 + 駅順
     */
    public int getKariSeisanEki() {
        return ( ( 0xFF & bd[12] ) << 8 ) + ( 0xFF & bd[13] );
    }

    @Override
    @NotNull
    public String toString() {
        return "----- 改札ログ情報 -----\n" +
                "入出場: " + (getEnterOrExit() == 0 ? "出場" : "入場") + "\n" +
                "定期利用: " + usedTeiki() + "\n" +
                "SF利用: " + usedSF() + "\n" +
                "駅コード: " + getStationCode() + "\n" +
                "コーナコード" + getCornerCode() + "\n" +
                "号機番号" + getUnitNo() + "\n" +
                "年" + getYear() + "\n" +
                "月" + getMonth() + "\n" +
                "日" + getDate() + "\n" +
                "ピーク運賃" + getPeakFare() + "\n" +
                "仮清算金" + getKariSeisanKin() + "\n" +
                "仮清算駅" + getKariSeisanEki();
    }
}
