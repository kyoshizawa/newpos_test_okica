package jp.mcapps.android.multi_payment_terminal.data.okica;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * SF残額情報のブロックデータをオブジェクトのように扱うためのクラスです
 */
public class SFChargeInfo {
    public static final int SERVICE_CODE = 0x82;
    private final byte[] bd;

    /**
     * コンストラクタ(Write用)
     */
    public SFChargeInfo() {
        this.bd = new byte[16];
    }

    /**
     * コンストラクタ(Read用)
     *
     * @param blockData ブロックデータ
     */
    public SFChargeInfo(byte[] blockData) {
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
    public SFChargeInfo setTypeCode(int code) {
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
    public SFChargeInfo setModelCode(int code) {
        bd[0] = (byte) ( (0b0111_1111 & code) + (0b1000_0000 & bd[0]) );

        return this;
    }

    /**
     * チャージ箇所コードを取得します
     *
     * @return チャージ箇所コード
     */
    public long getChargePointCode() {
        return ( bd[1] << 24 ) + ( bd[2] << 16 ) + ( bd[3] << 8 ) + bd[4];
    }

    /**
     * チャージ箇所コードを設定します
     *
     * @param code チャージ箇所コード
     * @return this
     */
    public SFChargeInfo setChargePointCode(long code) {
        bd[1] = (byte) ( ( 0xFF000000 & code ) >> 24 );
        bd[2] = (byte) ( ( 0x00FF0000 & code ) >> 16 );
        bd[3] = (byte) ( ( 0x0000FF00 & code ) >> 8  );
        bd[4] = (byte) ( ( 0x000000FF & code )       );

        return this;
    }

    /**
     * チャージ金額を取得します
     *
     * @return チャージ金額
     */
    public int getChargeAmount() {
        return ( 0xFF & bd[5] ) + ( ( 0xFF & bd[6] ) << 8 ) + ( ( 0xFF & bd[7] ) << 16);
    }

    /**
     * チャージ金額を設定します
     *
     * @param amount
     * @return this
     */
    public SFChargeInfo setChargeAmount(int amount) {
        bd[5] = (byte) ( ( amount & 0x00_00_00_FF )       );
        bd[6] = (byte) ( ( amount & 0x00_00_FF_00 ) >> 8  );
        bd[7] = (byte) ( ( amount & 0x00_FF_00_00 ) >> 16 );

        return this;
    }

    @Override
    public String toString() {
        return "----- SF積増情報 -----\n" +
                "種別コード: " + getTypeCode() + "\n" +
                "機種子オード: " + getModelCode() + "\n" +
                "チャージ箇所コード: " + String.format("%08X", 0x00000000FFFFFFFF & getChargePointCode()) + "\n" +
                "積増額: " + getChargeAmount();
    }
}
