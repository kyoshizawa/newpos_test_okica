package jp.mcapps.android.multi_payment_terminal.data.okica;

import android.annotation.SuppressLint;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * IDiをオブジェクトのように扱うためのクラスです
 */
public class IDi {
    private final byte[] rawData;

    /**
     * コンストラクタ
     *
     * @param IDi 生データ
     */
    public IDi(byte[] IDi) {
        this.rawData = IDi;
    }

    /**
     * 生データを取得します
     *
     * @return 生データ
     */
    public byte[] getRawData() {
        return rawData;
    }

    /**
     * 事業者コードを取得します
     *
     * @return 事業者コード
     */
    public int getCompanyCode() {
        return ( ( 0xFF & rawData[0]) << 8) + ( 0xFF & rawData[1] );
    }

    /**
     * カードバージョン
     *
     * @return カードバージョン(1 ~ F)
     */
    public int getVersion() {
        return ( 0b1111_0000 & rawData[2] ) >> 4;
    }

    /**
     * ICカード種別を取得します
     *
     * @return ICカード種別(0 ~ F)
     */
    public int getType() {
        return 0b0000_1111 & rawData[2];
    }

    /**
     * 発券機No.を取得します
     *
     * @return 発券機No.(0 ~ F)
     */
    public int getHakkenNo() {
        return ( 0b1111_0000 & rawData[3] ) >> 4;
    }

    /**
     * チェックデジット値を取得します
     *
     * @return チェックデジット値(0 ~ F)
     */
    public int getCheckDigit() {
        return 0b0000_1111 & rawData[3];
    }

    /**
     * 発行年を取得します
     *
     * @return 発行年(西暦下位2桁)
     */
    public int getYear() {
        return ( 0b1111_1110 & rawData[4] ) >> 1;
    }

    /**
     * 発行月を取得します
     *
     * @return 発行月
     */
    public int getMonth() {
        return ( ( 0b0000_0001 & rawData[4] ) <<3 ) + ( ( 0b1110_0000 & rawData[5] ) >> 5 );
    }

    /**
     * 発行日を取得します
     *
     * @return 発行日
     */
    public int getDate() {
        return 0b0001_1111 & rawData[5];
    }

    /**
     * 発行シリアルNo.を取得します
     *
     * @return 発見シリアルNo.(0 ~ 65535)
     */
    public int getSerialNo() {
        return ( ( 0xFF & rawData[6] ) << 8 ) + ( 0xFF & rawData[7] );
    }

    /**
     * IDiの値の一致をチェックします
     *
     * @param IDi IDi
     * @return true: 一致 false: 不一致
     */
    public boolean equals(byte[] IDi) {
        return Arrays.equals(IDi, rawData);
    }

    /**
     * カードに刻印されているIDiの番号を返却します
     *
     * @return IDi刻印
     */
    @SuppressLint("DefaultLocale")
    @NotNull
    public String getCardNo() {
        return String.format("%c%c%01X%01X%01X%01X%02d%02d%02d%05d",
                (char) rawData[0],
                (char) rawData[1],
                getVersion(),
                getType(),
                getHakkenNo(),
                getCheckDigit(),
                getYear(),
                getMonth(),
                getDate(),
                getSerialNo()
        );
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("%02X%02X%02X%02X%02X%02X%02X%02X",
                rawData[0], rawData[1], rawData[2], rawData[3],
                rawData[4], rawData[5], rawData[6], rawData[7]);
    }
}
