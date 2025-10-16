package jp.mcapps.android.multi_payment_terminal.data.okica;

import java.util.Calendar;
import java.util.TimeZone;

public class MasterCommonHeader {
    public static int SIZE = 22;

    /**
     * 登録数 4byte
     */
    private final long registerNum;
    public long getRegisterNum() {
        return registerNum;
    }

    private final int version;
    public int getVersion() {
        return version;
    }

    private final int typeCode;
    public int getTypeCode() {
        return typeCode;
    }

    private final long createdDatetime;
    public long getCreatedDatetime() {
        return createdDatetime;
    }

    private final long newDataStartDate;
    public long getNewDataStartDate() {
        return newDataStartDate;
    }

    public MasterCommonHeader(byte[] data) {
        this.registerNum = ( (long) (0xFF & data[0] ) << 24 ) + ( ( 0xFF & data[1] ) << 16 ) + ( (0xFF & data[2] ) << 8 ) + ( 0xFF & data[3] );
        this.version = 0xFF & data[4];
        this.typeCode = 0xFF & data[5];

        final Calendar c = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        c.setTimeZone(tz);
        c.set(Calendar.MILLISECOND, 0);

        int year = ( ( 0xF0 & data[6] ) >> 4 ) * 1000 + ( 0x0F & data[6] ) * 100 + ( ( 0xF0 & data[7] ) >> 4 ) * 10 + ( 0x0F & data[7] );
        int month = ( ( 0xF0 & data[8] ) >> 4 ) * 10 + ( 0x0F & data[8] );
        int date = ( ( 0xF0 & data[9] ) >> 4 ) * 10 + ( 0x0F & data[9] );
        int hour = ( ( 0xF0 & data[10] ) >> 4 ) * 10 + ( 0x0F & data[10] );
        int minute = ( ( 0xF0 & data[11] ) >> 4 ) * 10 + ( 0x0F & data[11] );

        c.set(year, month-1, date, hour, minute, 0);
        this.createdDatetime = c.getTimeInMillis();

        year = ( ( 0xF0 & data[18] ) >> 4 ) * 1000 + ( 0x0F & data[18] ) * 100 + ( ( 0xF0 & data[19] ) >> 4 ) * 10 + ( 0x0F & data[19] );
        month = ( ( 0xF0 & data[20] ) >> 4 ) * 10 + ( 0x0F & data[20] );
        date = ( ( 0xF0 & data[21] ) >> 4 ) * 10 + ( 0x0F & data[21] );

        c.set(year, month-1, date, 0, 0, 0);
        this.newDataStartDate = c.getTimeInMillis();
        }
        }