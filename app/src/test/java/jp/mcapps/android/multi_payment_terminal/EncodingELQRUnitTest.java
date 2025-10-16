package jp.mcapps.android.multi_payment_terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.text.SimpleDateFormat;

import jp.mcapps.android.multi_payment_terminal.encoding.ErrorDetail;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr.CRCGenerator;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr.QRCodeData;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr.QRCodeParser;

public class EncodingELQRUnitTest {

    @Test
    public void compute_crc() {

        // 検証
        {
            String exampleCode = "123456789";
            int crc = new CRCGenerator(exampleCode).generate();
            assertEquals(10673, crc % 100000);
        }

        // 実際のeL-QRコードでの検証
        {
            String exampleCode = "000201" +
                    "010212" +
                    "2796000513800018338000000000000000003950021380001270000082000002024010101043487782840440000400000000"+
                    "541100000039500"+
                    "622620242024002024053120240710"+
                    "80850000000000000000000000000000000000000000000000000000000000000000000000000000000000000"+
                    "630520201";
            int computedCrc = new CRCGenerator(exampleCode.substring(0, exampleCode.length()-5)).generate();
            assertEquals(20201, computedCrc % 100000);
        }
    }

    @Test
    public void parse_barcode() {

        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 検証
        {
            String exampleCode = "000201010212279600051380001833800000000000000000395002138000127000008200000202401010104348778284044000040000000054110000003950062262024202400202405312024071080850000000000000000000000000000000000000000000000000000000000000000000000000000000000000630520201";
            Result<QRCodeData, ErrorDetail> parseResult = new QRCodeParser().parseString(exampleCode);
            assertTrue(parseResult.isOk());
            assertNotNull(parseResult.ok);
            QRCodeData data = parseResult.ok;
            assertEquals("127", data.getTaxItemNumber());
            assertEquals("2024-05-31 00:00:00", format.format(data.getFilingDueDate()));
            assertEquals("2024-07-10 00:00:00", format.format(data.getPaymentDueDate()));
            assertEquals(39500, data.getPaymentAmount());
        }

        // 検証
        {
            String exampleCode = "000201010212279600051380001838100000000000000000435002138000127000002900000202401010104347605497044000040000000054110000004350062262024202400202405312024071080850000000000000000000000000000000000000000000000000000000000000000000000000000000000000630534977";
            Result<QRCodeData, ErrorDetail> parseResult = new QRCodeParser().parseString(exampleCode);
            assertTrue(parseResult.isOk());
            assertNotNull(parseResult.ok);
            QRCodeData data = parseResult.ok;
            assertEquals("127", data.getTaxItemNumber());
            assertEquals("2024-05-31 00:00:00", format.format(data.getFilingDueDate()));
            assertEquals("2024-07-10 00:00:00", format.format(data.getPaymentDueDate()));
            assertEquals(43500, data.getPaymentAmount());
        }
    }
}