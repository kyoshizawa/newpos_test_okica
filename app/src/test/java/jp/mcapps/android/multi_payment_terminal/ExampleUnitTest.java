package jp.mcapps.android.multi_payment_terminal;

import org.junit.Test;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void calendar_getTime() throws ParseException {
        Date date;

        // 2024-01-02 00:00:00 JST (SimpleDateFormat)
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        date = format.parse("2024-01-02 00:00:00");
        assertEquals("2024-01-02 00:00:00", format.format(date));
        assertEquals(-9*60, date.getTimezoneOffset());

        // 2024-01-02 00:00:00 JST (Calendar)
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2024, 0, 2);
        date = cal.getTime();
        assertEquals("2024-01-02 00:00:00", format.format(date));
        assertEquals(-9*60, date.getTimezoneOffset());
    }
}