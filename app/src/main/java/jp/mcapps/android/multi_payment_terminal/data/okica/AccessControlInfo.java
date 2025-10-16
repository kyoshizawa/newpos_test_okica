package jp.mcapps.android.multi_payment_terminal.data.okica;

import java.util.Arrays;

/**
 * アクセス制御情報のブロックデータをオブジェクトのように扱うためのクラスです
 */
public class AccessControlInfo {
    public static int SERVICE_CODE = 0x81;

    private final byte[] bd;

    /**
     * コンストラクタ
     *
     * @param blockData ブロックデータ
     */
    public AccessControlInfo(byte[] blockData) {
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
     * カード制御情報を取得します
     * @return カード制御情報
     */
    public int getCardControlInfo() { return (0xFF & bd[8]); }

    /**
     * カードの使用可否取得します
     * @return true: 使用可能 false: 使用不可
     */
    public boolean cardAvailable() {
        return ( ( ( 0b1000_0000 & bd[8] ) >> 7 ) == 0 );
    }

    /**
     * カードのネガビットをセットします
     *
     * @return this
     */
    public AccessControlInfo setNegaBit() {
        bd[8] |= 0b1000_0000;

        return this;
    }

    /**
     * オートNdayが利用可否を取得します
     *
     * @return true: 利用可能 false: 利用不可
     */
    public boolean autoNdayAvailable() {
        return ( ( ( 0b0100_0000 & bd[8] ) >> 6 ) == 0 );
    }

    /**
     * SF利用可否を取得します
     *
     * @return true: 利用可能 false: 利用不可
     */
    public boolean SFAvailable() {
        return ( ( (0b0010_0000 & bd[8]) >> 5 ) == 1 );
    }

    /**
     * 音声案内利用有無を取得します
     *
     * @return true: 利用する false: 利用しない
     */
    public boolean useVoiceGuide() {
        return ( ( ( 0b0001_0000 & bd[8] ) >> 4 ) == 1 );
    }

    /**
     * 地域識別を取得します
     *
     * @return 1: モノレール 2: バス 3: その他
     */
    public int getRegionCode() {
        return 0b0000_0011 & bd[8];
    }

    /**
     * サイクル異常を取得します
     *
     * @return 0 ~ 15
     */
    public int getCycleError() {
        return ( 0b1111_0000 & bd[9] ) >> 4;
    }

    /**
     * 時刻回数異常を取得します
     *
     * @return 0 ~ 15
     */
    public int getTimeCountError() {
        return 0b0000_1111 & bd[9];
    }

    /**
     * 同一駅異常を取得します
     *
     * @return 0 ~ 15
     */
    public int getSameStationError() {
        return ( 0b1111_0000 & bd[10] ) >> 4;
    }

    /**
     * パース金額を取得します
     *
     * @return パース金額
     */
    public int getPurseAmount() {
        return ( 0xFF & bd[11] ) + ( ( 0xFF & bd[12] ) << 8 ) + ( ( 0xFF & bd[13] ) << 16 );
    }

    /**
     * パース金額を設定します
     *
     * @param amount パース金額
     * @return this
     */
    public AccessControlInfo setPurseAmount(int amount) {
        bd[11] = (byte) ( ( amount & 0x00_00_00_FF )       );
        bd[12] = (byte) ( ( amount & 0x00_00_FF_00 ) >> 8  );
        bd[13] = (byte) ( ( amount & 0x00_FF_00_00 ) >> 16 );

        return this;
    }

    /**
     * 一件明細IDを取得します
     *
     * @return 一件明細ID
     */
    public int getIkkenMeisaiId() {
        return ( ( 0xFF & bd[14] ) << 8 ) + ( 0xFF & (bd[15] ) );
    }

    /**
     * 一件明細IDをインクリメントします
     *
     * @return this
     */
    public AccessControlInfo incrementIkkenMeisaiId() {
        int nextId = getIkkenMeisaiId() + 1;

        bd[14] = (byte) ( ( 0xFF_00 & nextId ) >> 8 );
        bd[15] = (byte) ( ( 0x00_FF & nextId )      );

        return this;
    }

    /**
     * コピーオブジェクトを生成します
     *
     * @return コピーオブジェクト
     */
    public AccessControlInfo copy() {
        return new AccessControlInfo(Arrays.copyOf(bd, bd.length));
    }

    @Override
    public String toString() {
        return "----- アクセス制御情報 -----\n" +
                "カード使用可能: " + cardAvailable() + "\n" +
                "オートNday利用可能: " + autoNdayAvailable() + "\n" +
                "SF利用可能: " + SFAvailable() + "\n" +
                "音声案内利用: " + useVoiceGuide() + "\n" +
                "地域識別: " + getRegionCode() + "\n" +
                "サイクル異常: " + getCycleError() + "\n" +
                "時刻回数異常: " + getTimeCountError() + "\n" +
                "同一駅異常: " + getSameStationError() + "\n" +
                "パース金額: " + getPurseAmount() + "\n" +
                "一件明細ID: "+ getIkkenMeisaiId();
    }
}
