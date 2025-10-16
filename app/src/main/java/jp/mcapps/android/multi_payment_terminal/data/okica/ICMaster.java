package jp.mcapps.android.multi_payment_terminal.data.okica;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import timber.log.Timber;

public class ICMaster {
    private MasterCommonHeader header;
    public MasterCommonHeader getHeader() {
        return header;
    }

    private Data newData;
    private Data oldData;

    // 新旧有効な方のデータを返す
    public Data getData() {
        final Calendar calLimit1 = Calendar.getInstance();
        final Calendar calLimit2 = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        final long SystemTimeInMillis = System.currentTimeMillis();
        calLimit1.setTimeZone(tz);
        calLimit2.setTimeZone(tz);

        // 旧データの使用開始日付が新データより未来の場合データを返さない
        if (newData.startDatetime < oldData.startDatetime) {
            calLimit1.setTimeInMillis(newData.startDatetime);
            calLimit2.setTimeInMillis(oldData.startDatetime);
            Timber.e("IC運用マスタデータ異常:(新)%s < (旧)%s", calLimit1.getTime().toString(), calLimit2.getTime().toString());
            return null;
        }

        // 現在時刻より旧データの開始日が先の場合はデータを返さない
        if (SystemTimeInMillis < oldData.startDatetime) {
            calLimit1.setTimeInMillis(SystemTimeInMillis);
            calLimit2.setTimeInMillis(oldData.startDatetime);
            Timber.e("IC運用マスタデータ異常:(現)%s < (旧)%s", calLimit1.getTime().toString(), calLimit2.getTime().toString());
            return null;
        }

        return SystemTimeInMillis >= newData.startDatetime
            ? newData
            : oldData;
    }

    public ICMaster(byte[] master) {
        this.header = new MasterCommonHeader(master);
        this.newData = new Data(Arrays.copyOfRange(master, MasterCommonHeader.SIZE, MasterCommonHeader.SIZE + Data.SIZE));
        this.oldData = new Data(Arrays.copyOfRange(master, MasterCommonHeader.SIZE + Data.SIZE, MasterCommonHeader.SIZE + Data.SIZE*2));
    }

    public static class Data {
        private static int SIZE = 896;

        private final int version;
        public int getVersion() {
            return this.version;
        }

        private final int dataFormatType;
        public int getDataFormatType() {
            return this.dataFormatType;
        }

        private final long startDatetime;
        public long getStartDatetime() {
            return this.startDatetime;
        }

        private final int checkSum;
        public int getCheckSum() {
            return this.checkSum;
        }

        private final FirstIssuer[] firstIssuers;
        public FirstIssuer[] getFirstIssuers() {
            return this.firstIssuers;
        }

        private final Activator[] activators;
        public Activator[] getActivators() {
            return this.activators;
        }
        public Activator getActivator(int companyCode) {
            for (Activator activator : activators) {
                if (activator.companyCode == companyCode) {
                    return activator;
                }
            }
            return null;
        }

        public Data(byte[] data) {
            int offset = MasterCommonHeader.SIZE;
            this.version = ( 0xF0 & data[0] >> 4 ) * 10 + ( 0x0F & data[0] );
            this.dataFormatType = 0b0000_0001 & data[1];

            final Calendar c = Calendar.getInstance();
            final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
            c.setTimeZone(tz);
            c.set(Calendar.MILLISECOND, 0);

            int year = ( ( 0xF0 & data[2] ) >> 4 ) * 1000 + ( 0x0F & data[2] ) * 100 + ( ( 0xF0 & data[3] ) >> 4 ) * 10 + ( 0x0F & data[3] );
            int month = ( ( 0xF0 & data[4] ) >> 4 ) * 10 + ( 0x0F & data[4] );
            int date = ( ( 0xF0 & data[5] ) >> 4 ) * 10 + ( 0x0F & data[5] );
            int hour = ( ( 0xF0 & data[6] ) >> 4 ) * 10 + ( 0x0F & data[6] );
            int minute = ( ( 0xF0 & data[7] ) >> 4 ) * 10 + ( 0x0F & data[7] );

            c.set(year, month-1, date, hour, minute, 0);
            this.startDatetime = c.getTimeInMillis();

            this.checkSum = ( 0xFF & data[14] ) + ( ( 0xFF & data[15] ) << 8 );

            final List<FirstIssuer> issuers = new ArrayList<FirstIssuer>();

            // 一次発行事業者の最大数は10 有効なものだけ保持する
            for (int i = 0; i < 10; i++) {
                int v = 0;
                for (int j = 16 + i*8; j < 16 + i*8 + 8; j++) {
                    v += (byte) data[j];
                }
                if (v > 0) {
                    issuers.add(new FirstIssuer(Arrays.copyOfRange(data, (16) + i*8, (16) + (i*8 + 8))));
                }
            }

            this.firstIssuers = new FirstIssuer[issuers.size()];
            for (int i = 0; i < issuers.size(); i++) {
                this.firstIssuers[i] = issuers.get(i);
            }

            final List<Activator> activators = new ArrayList<Activator>();

            // 活性事業者の最大数は50 有効なものだけ保持する
            for (int i = 0; i < 50; i++) {
                int v = 0;
                for (int j = 96 + i*16; j < 96 + i*16 + 16; j++) {
                    v += (byte) data[j];
                }
                if (v > 0) {
                    activators.add(new Activator(Arrays.copyOfRange(data, (96) + i*16, (96) + (i*16 + 16))));
                }
            }

            this.activators = new Activator[activators.size()];
            for (int i = 0; i < activators.size(); i++) {
                this.activators[i] = activators.get(i);
            }
        }
    }

    public static class FirstIssuer {
        private final int companyCode;
        public int getCompanyCode() {
            return companyCode;
        }

        public FirstIssuer(byte[] data) {
            int companyCode = ( ( ( 0xF0 & data[0] ) >> 4 ) * 10  + ( 0x0F & data[0] ) ) << 8;
            companyCode += ( ( 0xF0 & data[1] ) >> 4 ) * 1000 + ( 0x0F & data[1] ) * 100;
            companyCode += ( ( 0xF0 & data[2] ) >> 4 ) * 10 + ( 0x0F & data[2] );

            this.companyCode = companyCode;
        }
    }

    public static class Activator {
        private final int companyCode;
        public int getCompanyCode() {
            return companyCode;
        }

        private final int aggregateGroup;
        public int getAggregateGroup() {
            return this.aggregateGroup;
        }

        private final boolean hasAutoCharge;
        public boolean hasAutoCharge() {
            return this.hasAutoCharge;
        }

        private final boolean allowTestCard;
        public boolean allowTestCard() {
            return this.allowTestCard;
        }

        private final boolean allowChildTicket;
        public boolean allowChildTicket() {
            return this.allowChildTicket;
        }

        private final boolean check10YearsExpired;
        public boolean check10YearsExpired() {
            return this.check10YearsExpired;
        }

        private final int purseLimitAmount;
        public int getPurseLimitAmount() {
            return purseLimitAmount;
        }

        private final int maxCardAmount;
        public int getMaxCardAmount() {
            return maxCardAmount;
        }

        public Activator(byte[] data) {
            int companyCode = ( ( ( 0xF0 & data[0] ) >> 4 ) * 10  + ( 0x0F & data[0] ) ) << 8;
            companyCode += ( ( 0xF0 & data[1] ) >> 4 ) * 1000 + ( 0x0F & data[1] ) * 100;
            companyCode += ( ( 0xF0 & data[2] ) >> 4 ) * 10 + ( 0x0F & data[2] );

            this.companyCode = companyCode;

            this.aggregateGroup = ( ( 0xF0 & data[3] ) >> 4 ) * 10 + ( 0x0F & data [3] );
            this.hasAutoCharge = ( 0b0000_0001 & data[4] ) != 0;
            this.allowTestCard = ( 0b0000_0010 & data[4] ) != 0;
            this.allowChildTicket = ( 0b0000_0100 & data[4] ) == 0;
            this.check10YearsExpired = ( 0b0000_1000 & data[4] ) != 0;

            this.purseLimitAmount = ( ( 0xF0 & data[5] ) >> 4 ) * 100_000 + ( 0x0F & data[5] ) * 10_000
                    + ( ( 0xF0 & data[6] ) >> 4 ) * 1_000 + ( 0x0F & data[6] ) * 100
                    + ( ( 0xF0 & data[7] ) >> 4 ) * 10 + ( 0x0F & data[7] );

            this.maxCardAmount = ( ( 0xF0 & data[8] ) >> 4 ) * 100_000 + ( 0x0F & data[8] ) * 10_000
                    + ( ( 0xF0 & data[9] ) >> 4 ) * 1_000 + ( 0x0F & data[9] ) * 100
                    + ( ( 0xF0 & data[10] ) >> 4 ) * 10 + ( 0x0F & data[10] );
        }
    }

    /**
     * データのチェックを行いファイルを保存を行います
     *
     * @param rawData 生データ
     * @param fileName ファイル名
     * @return 成否
     */
    public static boolean save(byte[] rawData, String fileName) {
        final int HEADER_SIZE = 22;
        final int DATA_SIZE = 896;
        final int OFFSET_SUM = 14;
        final int CALC_DATA_SIZE = DATA_SIZE - (OFFSET_SUM + 2);

        try {
            // フッターサム値チェック
            long data_sum = ( (long) ( 0xFF & rawData[rawData.length-4] ) << 24 ) +
                ( ( 0xFF & rawData[rawData.length-3] ) << 16 ) +
                ( ( 0xFF & rawData[rawData.length-2] ) << 8  ) +
                ( ( 0xFF & rawData[rawData.length-1] )       );

            long calc_sum = 0;
            // データの最初からフッターのサム値の手前までのサム値を計算
            for (int i = 0; i < rawData.length-4; i++) {
                calc_sum += 0xFF & rawData[i];
            }

            if (calc_sum != data_sum) {
                return false;
            }

            // 新データサム値チェック
            data_sum = ( 0xFF & rawData[HEADER_SIZE + OFFSET_SUM] ) + ( ( 0xFF & rawData[HEADER_SIZE + OFFSET_SUM + 1]) << 8);

            int start_idx = HEADER_SIZE + OFFSET_SUM + 2;
            calc_sum = 0;
            System.out.println("idx: " + start_idx);
            for (int i = start_idx; i < start_idx + CALC_DATA_SIZE; i++) {
                calc_sum += rawData[i];
            }

            if (calc_sum != data_sum) {
                return false;
            }

            // 旧データサム値チェック
            data_sum = ( 0xFF & rawData[HEADER_SIZE + DATA_SIZE + OFFSET_SUM] ) + ( ( 0xFF & rawData[HEADER_SIZE + DATA_SIZE + OFFSET_SUM + 1]) << 8);

            start_idx = HEADER_SIZE + DATA_SIZE + OFFSET_SUM + 2;
            calc_sum = 0;
            for (int i = start_idx; i < start_idx + CALC_DATA_SIZE; i++) {
                calc_sum += rawData[i];
            }

            if (calc_sum != data_sum) {
                return false;
            }
        } catch(IndexOutOfBoundsException e) {
            // データサイズが異常の場合
            return false;
        }

        final MainApplication app = MainApplication.getInstance();

        try (final FileOutputStream fos = app.openFileOutput(fileName, Context.MODE_PRIVATE);
             final BufferedOutputStream buf = new BufferedOutputStream(fos);) {
            buf.write(rawData);
            buf.flush();
            ICMasterInfo masterInfo = new ICMasterInfo();
            masterInfo.fileName = fileName;
            masterInfo.version = rawData[4];
            masterInfo.checkDate = System.currentTimeMillis();

            final Calendar cl = Calendar.getInstance();
            final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

            cl.setTimeZone(tz);

            ICMasterInfo icMasterInfo = AppPreference.getOkicaICMasterInfo();
            if (icMasterInfo == null || icMasterInfo.version != masterInfo.version) {
                if (icMasterInfo == null) {
                    Timber.i("IC運用マスタ取得情報 (新)Ver:%d (現)Ver:Null", masterInfo.version);
                } else {
                    Timber.i("IC運用マスタ取得情報 (新)Ver:%d (現)Ver:%d", masterInfo.version, icMasterInfo.version);
                }
            }
            AppPreference.setOkicaICMasterInfo(masterInfo);
            icMasterInfo = AppPreference.getOkicaICMasterInfo();
            if (icMasterInfo != null) {
                cl.setTimeInMillis(icMasterInfo.checkDate);
                Timber.i("IC運用マスタ使用情報 Ver:%d Date:%04d/%02d/%02d %02d:%02d:%02d", icMasterInfo.version,
                        cl.get(Calendar.YEAR), cl.get(Calendar.MONTH)+1, cl.get(Calendar.DATE),
                        cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
            }

            return true;
        } catch (IOException e) {
            Timber.e("IC運用マスタファイル書き込みエラー");

            return false;
        }
    }

    /**
     * ファイルからデータを取得します
     *
     * @return IC運用マスタ
     */
    public static ICMaster load() {
        final MainApplication app = MainApplication.getInstance();

        final String fileName = AppPreference.getOkicaICMasterInfo().fileName;

        try (final FileInputStream fis = app.openFileInput(fileName);
             final BufferedInputStream in = new BufferedInputStream(fis);
             final ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            byte[] data = new byte[1024];

            int n;
            while ((n = in.read(data)) != -1) {
                out.write(data, 0, n);
            }

            return new ICMaster(out.toByteArray());
        } catch (
                IOException e) {
            Timber.e(e);
        }

        return null;
    }

    /**
     * ファイルの削除をおこないます
     */
    public static boolean delete() {
        try {
            final MainApplication app = MainApplication.getInstance();
            final String fileName = AppPreference.getOkicaICMasterInfo().fileName;
            app.deleteFile(fileName);
            AppPreference.setOkicaICMasterInfo(null);
            Timber.i("OKICA IC運用マスタファイル削除");
            return true;
        } catch (Exception e) {
            Timber.e(e);
        }
        return false;
    }

    /**
     * ファイルの更新確認の日付を更新します
     */
    public static void updateCheckDate() {
        ICMasterInfo masterInfo = AppPreference.getOkicaICMasterInfo();
        masterInfo.checkDate = System.currentTimeMillis();
        AppPreference.setOkicaICMasterInfo(masterInfo);
    }
}
