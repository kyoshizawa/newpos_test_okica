package jp.mcapps.android.multi_payment_terminal.data.okica;

/**
 * カード基本情報のブロックデータをオブジェクトのように扱うためのクラスです
 */
public class CardBasicInfo {
    public static int SERVICE_CODE = 0x80;

    public static class Block3 {
        private final byte[] bd;

        /**
         * コンストラクタ
         *
         * @param blockData ブロックデータ
         */
        public Block3(byte[] blockData) {
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
         * カード(非)活性事業者コードを取得します
         *
         * @return 事業者コード
         */
        public int getCompanyCode() {
            return ( ( 0xFF & bd[0] ) << 8 ) + ( 0xFF & bd[1] );
        }

        /**
         * 種別コードを取得します
         *
         * @return 種別コード
         */
        public int getTypeCode() {
            return ( 0b1000_0000 & bd[2] ) >> 7;
        }

        /**
         * 機種コードを取得します
         *
         * @return 機種コード
         */
        public int getModelCode() {
            return 0b0111_1111 & bd[2];
        }

        /**
         * カード(非)活性所コードを取得します
         *
         * @return カード(非)活性所コード
         */
        public long getActivePlaceCode() {
            return  ( ( 0xFF & bd[3] ) << 24 ) +
                    ( ( 0xFF & bd[4] ) << 16 ) +
                    ( ( 0xFF & bd[5] ) << 8  ) +
                    ( ( 0xFF & bd[6] )       ) ;
        }

        /**
         * カード(非)活性化年を取得します
         *
         * @return カード(非)活性化年
         */
        public int getActivateYear() {
            return ( 0b1111_1110 & bd[7] ) >> 1;
        }

        /**
         * カード(非)活性化月を取得します
         *
         * @return カード(非)活性化月
         */
        public int getActivateMonth() {
            return ( ( 0b0000_0001 & bd[7] ) << 3 ) + ( ( 0b1110_0000 & bd[8] ) >> 5 );
        }

        /**
         * カード(非)活性化日を取得します
         *
         * @return カード(非)活性化日
         */
        public int getActivateDate() {
            return 0b0001_1111 & bd[8];
        }

        /**
         * 活性化フラグを取得します
         *
         * @return 活性化フラグ
         */
        public boolean isActive() {
            return ( 0b1000_0000 & bd[9] ) != 0;
        }

        /**
         * 非活性化フラグを取得します
         *
         * @return 非活性化フラグ
         */
        public boolean isNonActive() {
            return ( 0b0100_0000 & bd[9] ) != 0;
        }

        /**
         * 機能種別を取得します
         *
         * @return 機能種別
         */
        public int getFuncType() { return ( ( 0xFF & bd[10] ) << 8 ) + ( 0xFF & bd[11] ); }

        /**
         * SF機能の有無を取得します
         *
         * @return SF機能の有無
         */
        public boolean hasSF() {
            return ( 0b1000_0000 & bd[10] ) != 0;
        }

        /**
         * 鉄道・バスでのSF利用可能フラグを取得します
         *
         * @return 鉄道・バスでのSF利用可能フラグ
         */
        public boolean useSFTransport() {
            return ( 0b0100_0000 & bd[10] ) != 0;
        }

        /**
         * 関連事業でのSF利用可能フラグを取得します
         *
         * @return 関連事業でのSF利用可能フラグ
         */
        public boolean useSFKanrenJigyo() {
            return ( 0b0010_0000 & bd[10] ) != 0;
        }

        /**
         * クレジットIC機能の有無を取得します
         *
         * @return クレジットIC機能の有無
         */
        public boolean hasCreditIC() {
            return ( 0b0001_0000 & bd[10] ) != 0;
        }

        /**
         * 定期券情報の有無を取得します
         *
         * @return 定期券情報の有無
         */
        public boolean isTeiki() {
            return ( 0b0000_0100 & bd[10] ) == 0;
        }

        /**
         * バス定期券情報の有無を取得します
         *
         * @return バス定期券情報の有無
         */
        public boolean isBusTeiki1() {
            return ( 0b0000_0100 & bd[11] ) == 0;
        }

        /**
         * バス定期券情報の有無を取得します
         *
         * @return バス定期券情報の有無
         */
        public boolean isBusTeiki2() {
            return ( 0b0000_0010 & bd[11] ) == 0;
        }

        /**
         * バス定期券情報の有無を取得します
         *
         * @return バス定期券情報の有無
         */
        public boolean isBusTeiki3() {
            return ( 0b0000_0001 & bd[11] ) == 0;
        }

        /**
         * マルチアプリ機能の有無を取得します
         *
         * @return マルチアプリ機能の有無を取得します
         */
        public boolean hasMultiApp() {
            return ( 0b0001_0000 & bd[13] ) != 0;
        }

        /**
         * 券種コードを取得します
         *
         * @return 券種コード
         */
        public int getSFTicketTypeCode() {
            return 0b0000_1111 & bd[13];
        }

        /**
         * カード有効終了年を取得します
         *
         * @return カード有効終了年
         */
        public int getExpireYear() {
            return ( 0b1111_1110 & bd[14] ) >> 1;
        }

        /**
         * カード有効終了月を取得します
         *
         * @return カード有効終了月
         */
        public int getExpireMonth() {
            return ( ( 0b0000_0001 & bd[14] ) << 3 ) + ( ( 0b1110_0000 & bd[15] ) >> 5 );
        }

        /**
         * カード有効終了日を取得します
         *
         * @return カード有効終了
         */
        public int getExpireDate() {
            return 0b0001_1111 & bd[15];
        }

        /**
         * 有効期限が有無を取得します
         *
         * @return 年月日がオール0ではない場合はtrue
         */
        public boolean hasExpiration() {
            return ( getExpireYear() + getExpireMonth() + getExpireDate() ) > 0;
        }

        @Override
        public String toString() {
            return "----- カード基本情報ブロック3 -----\n" +
                    "事業者コード: " + String.format("%04X", getCompanyCode()) + "\n" +
                    "種別コード: " + getTypeCode() + "\n" +
                    "機種コード: " + getModelCode() + "\n" +
                    "活性所コード: " + getActivePlaceCode() + "\n" +
                    "活性年: " + getActivateYear() + "\n" +
                    "活性月: " + getActivateMonth() + "\n" +
                    "活性日: " + getActivateDate() + "\n" +
                    "活性化: " + isActive() + "\n" +
                    "非活性化: " + isNonActive() + "\n" +
                    "SF機能: " + hasSF() + "\n" +
                    "SF用途(鉄道・バス): " + useSFTransport() + "\n" +
                    "SF用途(関連事業): " + useSFKanrenJigyo() + "\n" +
                    "クレジットIC機能: " + hasCreditIC() + "\n" +
                    "定期券機能: " + isTeiki() + "\n" +
                    "バス定期券機能1: " + isBusTeiki1() + "\n" +
                    "バス定期券機能2: " + isBusTeiki2() + "\n" +
                    "バス定期券機能3: " + isBusTeiki3() + "\n" +
                    "マルチアプリ機能: " + hasMultiApp() + "\n" +
                    "SF券種コード: " + getSFTicketTypeCode() + "\n" +
                    "カード有効終了年: " + getExpireYear() + "\n" +
                    "カード有効終了月: " + getExpireMonth() + "\n" +
                    "カード有効終了日: " + getExpireDate();
        }
    }
}
