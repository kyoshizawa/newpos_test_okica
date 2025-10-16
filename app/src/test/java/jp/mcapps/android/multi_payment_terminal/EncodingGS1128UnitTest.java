package jp.mcapps.android.multi_payment_terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.text.SimpleDateFormat;

import jp.mcapps.android.multi_payment_terminal.encoding.ErrorDetail;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128.BarcodeData;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128.CheckDigitGenerator;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128.BarcodeParser;

public class EncodingGS1128UnitTest {

    @Test
    public void calculate_check_digit() {

        // 検証
        {
            String exampleCode = "91959919833262024010101043480024071000395003";
            int checkDigit = new CheckDigitGenerator(exampleCode).generate();
            assertEquals(3, checkDigit);
        }

        // 検証
        {
            String exampleCode = "91959919833262024010101043470024071000435007";
            int checkDigit = new CheckDigitGenerator(exampleCode).generate();
            assertEquals(7, checkDigit);
        }
    }

    @Test
    public void parse_barcode() {

        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 検証
        {
            String exampleCode = "91959919833262024010101043480024071000395003";
            Result<BarcodeData, ErrorDetail> parseResult = new BarcodeParser().parseString(exampleCode);
            assertTrue(parseResult.isOk());
            assertNotNull(parseResult.ok);
            assertEquals("959919", parseResult.ok.getInvoiceCompanyCode());
            assertEquals(0, parseResult.ok.getReissueCount());
            assertEquals("2024-07-10 00:00:00", format.format(parseResult.ok.getDueDate()));
            assertEquals(0, parseResult.ok.getStampFlag());
            assertEquals(39500, parseResult.ok.getPaymentAmount());
            assertEquals("833262024010101043480", parseResult.ok.getFreeField());
        }

        // 検証
        {
            String exampleCode = "91959919833262024010101043470024071000435007";
            Result<BarcodeData, ErrorDetail> parseResult = new BarcodeParser().parseString(exampleCode);
            assertTrue(parseResult.isOk());
            assertNotNull(parseResult.ok);
            assertEquals("959919", parseResult.ok.getInvoiceCompanyCode());
            assertEquals(0, parseResult.ok.getReissueCount());
            assertEquals("2024-07-10 00:00:00", format.format(parseResult.ok.getDueDate()));
            assertEquals(0, parseResult.ok.getStampFlag());
            assertEquals(43500, parseResult.ok.getPaymentAmount());
            assertEquals("833262024010101043470", parseResult.ok.getFreeField());
        }
    }
}