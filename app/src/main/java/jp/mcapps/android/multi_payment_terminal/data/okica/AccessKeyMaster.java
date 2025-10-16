package jp.mcapps.android.multi_payment_terminal.data.okica;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.OkicaDateUtils;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import timber.log.Timber;

public class AccessKeyMaster {
    private final MasterCommonHeader header;
    public MasterCommonHeader getHeader() {
        return header;
    }

    private final Data data;
    public Data getData() {
        return this.data;
    }

    public AccessKeyMaster(byte[] master) {
        this.header = new MasterCommonHeader(master);
        this.data = new Data(Arrays.copyOfRange(master, MasterCommonHeader.SIZE, master.length - 4));
    }

    public static class Data {
        private final int companyCode1;
        public int getCompanyCode1() {
            return this.companyCode1;
        }

        private final int companyCode2;
        public int getCompanyCode2() {
            return this.companyCode2;
        }

        private final int accessKeyDataLength;
        public int getAccessKeyDataLength() {
            return accessKeyDataLength;
        }

        private final long[] offsetTable;
        public long[] getOffsetTable() {
            return offsetTable;
        }

        private final AccessKeyData[] accessKeyDataTable;
        public AccessKeyData[] getAccessKeyDataTable() {
            return this.accessKeyDataTable;
        }

        public Data(byte[] data) {
            this.companyCode1 = ( ( 0xFF & data[0] ) << 8 ) + ( 0xFF & data[1] );
            this.companyCode2 = ( ( 0xFF & data[2] ) << 8 ) + ( 0xFF & data[3] );
            // 4~13は予備領域
            this.accessKeyDataLength = 0xFF & data[14];

            this.offsetTable = new long[this.accessKeyDataLength];

            for (int i = 0; i < this.accessKeyDataLength; i++) {
                this.offsetTable[i] = ( (long) ( 0xFF & data[15 + i*4] ) << 24 ) + ( ( 0xFF & data[16 + i*4] ) << 16 ) + ( ( 0xFF & data[17 + i*4] ) << 8 ) + ( 0xFF & data[18 + i+4] );
            }

            this.accessKeyDataTable = new AccessKeyData[this.accessKeyDataLength];
            int offset = 15 + this.accessKeyDataLength*4;

            for (int i = 0; i < this.accessKeyDataLength; i++) {
                this.accessKeyDataTable[i] = new AccessKeyData(Arrays.copyOfRange(data, offset + i*324, offset + i*324 + 324));
            }
        }
    }

    public static class AccessKeyData {
        private final int length;
        public int getLength() {
            return this.length;
        }

        private final int lengthCheckSum;
        public int getLengthCheckSum() {
            return this.lengthCheckSum;
        }

        private final int systemCode;
        public int getSystemCode() {
            return this.systemCode;
        }

        private final int systemVersion;
        public int getSystemVersion() {
            return this.systemVersion;
        }

        private final AccessKeyDetail detail;
        public final AccessKeyDetail getDetail() {
            return this.detail;
        }

        private final byte[] parity;
        public byte[] getParity() {
            return this.parity;
        }

        private final int dataCheckSum;
        public final int getDataCheckSum() {
            return this.dataCheckSum;
        }

        public AccessKeyData(byte[] data) {
            this.length = ( ( 0xFF & data[0] ) << 8 ) + ( 0xFF & data[1] );
            this.lengthCheckSum = 0xFF & data[2];

            final byte[] encData = Arrays.copyOfRange(data, 3, 320+3);
            final byte[] decData = Crypto.TripleDES.decrypt(encData, Constants.ACCESS_KEY_3DES_KEY, Constants.ACCESS_KEY_3DES_IV);

            this.systemCode = ( ( 0xFF & decData[0] ) << 8 ) + ( 0xFF & decData[1] ) ;
            this.systemVersion = ( ( 0xFF & decData[2] ) << 8 ) + ( 0xFF & decData[3] );

            final int okicaSystemCode = ( ( 0xFF & jp.mcapps.android.multi_payment_terminal.data.sam.Constants.SYSTEM_CODE[0] ) << 8 ) + ( 0xFF & jp.mcapps.android.multi_payment_terminal.data.sam.Constants.SYSTEM_CODE[1] );

            final AccessKeyDetail gen0 = new AccessKeyDetail(
                    Arrays.copyOfRange(decData, 4, 6),
                    Arrays.copyOfRange(decData, 8, 152),
                    Arrays.copyOfRange(decData, 298, 304),
                    0);

            if (isValidDate(gen0)) {
                this.detail = gen0;
            } else {
                AccessKeyDetail gen1 = new AccessKeyDetail(
                        Arrays.copyOfRange(decData, 6, 8),
                        Arrays.copyOfRange(decData, 152, 296),
                        Arrays.copyOfRange(decData, 306, 312),
                        1);
                if (isValidDate(gen1)) {
                    this.detail = gen1;
                } else {
                    this.detail = null;
                }
            }

            this.parity = Arrays.copyOfRange(decData, 312, 320);

            this.dataCheckSum = 0xFF & data[323];
        }
    }

    public static class AccessKeyDetail {
        private final int generation;
        public final int getGeneration() {
            return this.generation;
        }

        private final int areaNum;
        public final int getAreaNum() {
            return this.areaNum;
        }

        private final int serviceNum;
        public final int getServiceNum() {
            return this.serviceNum;
        }

        private final byte[] areaKeyVersions;
        public final byte[] getAreaKeyVersions() {
            return this.areaKeyVersions;
        }

        private final byte[] serviceKeyVersions;
        public final byte[] getServiceKeyVersions() {
            return this.serviceKeyVersions;
        }

        private final byte[] areaCodeList;
        public byte[] getAreaCodeList() {
            return this.areaCodeList;
        }

        private final byte[] serviceCodeList;
        public byte[] getServiceCodeList() {
            return this.serviceCodeList;
        }

        private final byte[] groupAccessKey;
        public byte[] getGroupAccessKey() {
            return this.groupAccessKey;
        }

        private final byte[] serviceAccessKey;
        public byte[] getServiceAccessKey() {
            return this.serviceAccessKey;
        }

        private final int startDate;
        public int getStartDate() {
            return this.startDate;
        }

        private final int changeDate;
        public int getChangeDate() {
            return this.changeDate;
        }

        private final int endDate;
        public int getEndDate() {
            return this.endDate;
        }

        public AccessKeyDetail(byte[] data1, byte[] data2, byte[] data3, int gen) {
            this.generation = gen;

            this.areaNum = 0xFF & data1[0];
            this.serviceNum = 0xFF & data1[1];

            this.areaKeyVersions = Arrays.copyOfRange(data2, 0, 32);
            this.serviceKeyVersions = Arrays.copyOfRange(data2, 32, 64);

            this.areaCodeList = Arrays.copyOfRange(data2, 64, 96);
            this.serviceCodeList = Arrays.copyOfRange(data2, 96, 128);

            this.groupAccessKey = Arrays.copyOfRange(data2, 128, 136);

            this.serviceAccessKey = Arrays.copyOfRange(data2, 136, 144);

            this.startDate = ( ( 0xFF & data3[1] ) << 8 ) + ( 0xFF & data3[0] );
            this.changeDate = ( ( 0xFF & data3[3] ) << 8 ) + ( 0xFF & data3[2] );
            this.endDate = ( ( 0xFF & data3[5] ) << 8 ) + ( 0xFF & data3[4] );
        }

        public String debugInfo() {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("エリア数: %s\n", areaNum));
            sb.append(String.format("サービス数: %s\n", serviceNum));

            for (int i = 0; i < this.areaKeyVersions.length; i++) {
                sb.append(String.format("エリア・キーバージョン%s: %04X\n", i+1, this.areaKeyVersions[i]));
            }

            for (int i = 0; i < this.serviceKeyVersions.length; i++) {
                sb.append(String.format("サービス・キーバージョン%s: %04X\n", i+1, this.serviceKeyVersions[i]));
            }

            for (int i = 0; i < this.areaNum; i++) {
                sb.append(String.format("エリアコード%s: %02X%02X\n", i+1, this.areaCodeList[i*2], this.areaCodeList[1 + i*2]));
            }

            for (int i = 0; i < this.serviceNum; i++) {
                sb.append(String.format("サービスコード%s: %02X%02X\n", i+1, this.serviceCodeList[i*2], this.serviceCodeList[1 + i*2]));
            }

            sb.append("グループアクセスキー:");
            for (byte b : this.groupAccessKey) {
                sb.append(String.format(" %02X", b));
            }
            sb.append("\n");

            sb.append("サービスアクセスキー:");
            for (byte b : this.serviceAccessKey) {
                sb.append(String.format(" %02X", b));
            }
            sb.append("\n");

            sb.append(String.format("使用開始日: %04X\n", this.startDate));
            sb.append(String.format("交換開始日: %04X\n", this.changeDate));
            sb.append(String.format("使用期限: %04X", this.endDate));

            return sb.toString();
        }
    }
    /**
     * データのチェックを行いファイルを保存を行います
     *
     * @param rawData 生データ
     * @return 成否
     */

    public static boolean validate(byte[] rawData) {
        AccessKeyMaster a = new AccessKeyMaster(rawData);
        final int REGISTER_NUM_IDX = 36;
        final int OFFSET_TABLE_IDX = 37;

        // フッターサム値チェック
        long footerSum = ( (long) (0xFF & rawData[rawData.length - 4]) << 24) +
                ((0xFF & rawData[rawData.length - 3]) << 16) +
                ((0xFF & rawData[rawData.length - 2]) << 8) +
                ((0xFF & rawData[rawData.length - 1]));

        long calcSum = 0;
        // データの最初からフッターのサム値の手前までのサム値を計算
        for (int i = 0; i < rawData.length - 4; i++) {
            calcSum += 0xFF & rawData[i];
        }

        if (calcSum != footerSum) {
            System.out.println(footerSum);
            System.out.println(calcSum);
            return false;
        }

        int num = rawData[REGISTER_NUM_IDX];
        int[] offsets = new int[num];
        for (int i = 0; i < num; i++) {
            offsets[i] = ( ( 0xFF & rawData[OFFSET_TABLE_IDX + 4*i] ) << 24 ) +
                    ( ( 0xFF & rawData[OFFSET_TABLE_IDX + 1 + 4*i] ) << 16 ) +
                    ( ( 0xFF & rawData[OFFSET_TABLE_IDX + 2 + 4*i] ) << 8  ) +
                    ( ( 0xFF & rawData[OFFSET_TABLE_IDX + 3 + 4*i] )       );
        }

        for (int offset : offsets) {
            final int length = ((0xFF & rawData[offset]) << 8) + (0xFF & rawData[offset + 1]);
            int sum = 0;

            for (int i = offset; i < offset + 3; i++) {
                sum = 0xFF & (sum + (0xFF & rawData[i]));
            }

            if (sum != 0) {
                return false;
            }

            byte[] enc = Arrays.copyOfRange(rawData, offset + 3, offset + 3 + length-1);
            final byte[] dec = Crypto.TripleDES.decrypt(enc, Constants.ACCESS_KEY_3DES_KEY, Constants.ACCESS_KEY_3DES_IV);

            sum = 0;
            for (byte b : dec) {
                sum += b;
            }

            sum = 0xFF & ( sum + rawData[offset+3+length-1] );

            if (sum != 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidDate(AccessKeyMaster.AccessKeyDetail detail)  {
        final int opDate = OkicaDateUtils.getOperationDate();

        if (detail.getStartDate() == 0xFFFF) {
            return true;
        } else {
            int s = detail.getStartDate();

            int sy = ( 0b1111_1110_0000_0000 & s );
            int sm = ( 0b0000_0001_1110_0000 & s );
            int sd = ( 0b0000_0000_0001_1111 & s );
            sy = (sy >> 9);
            sm = (sm >> 5);
            int startDate = (sy << 16) + (sm << 8) + sd;

            if (opDate >= startDate) {
                int e = detail.getEndDate();
                if (e == 0xFFFF) {
                    return true;
                } else {
                    int ey = (0b1111_1110_0000_0000 & e);
                    int em = (0b0000_0001_1110_0000 & e);
                    int ed = (0b0000_0000_0001_1111 & e);
                    ey = (ey >> 9);
                    em = (em >> 5);
                    int endDate = (ey << 16) + (em << 8) + ed;

                    return opDate <= endDate;
                }
            }
        }

        return false;
    }

    /**
     * ファイルの更新確認の日付を更新します
     */
    public static void updateCheckDate() {
        AccessKeyInfo accessKeyInfo = AppPreference.getOkicaAccessKeyInfo();
        accessKeyInfo.checkDate = System.currentTimeMillis();
        AppPreference.setOkicaAccessKeyInfo(accessKeyInfo);
    }
}
