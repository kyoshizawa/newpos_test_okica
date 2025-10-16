package jp.mcapps.android.multi_payment_terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcBuilder;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxRoundings;

public class TaxCalcBuilderUnitTest {
    @Test
    public void calculate_tax_amount() {

        // 計算パラメータ
        ServiceFunctionData serviceFunctionData = new ServiceFunctionData();
        serviceFunctionData.reduced_tax_rate = "0.08";
        serviceFunctionData.standard_tax_rate = "0.1";
        serviceFunctionData.tax_rounding = TaxRoundings.FLOOR.value;

        // 計算対象
        List<CartData> cartDataList = new ArrayList<>();

        // 非課税: 220
        cartDataList.add(new CartData(110,
                ProductTaxTypes.EXEMPTION.value,
                ReducedTaxTypes.UNKNOWN.value,
                IncludedTaxTypes.UNKNOWN.value));
        cartDataList.add(new CartData(110,
                ProductTaxTypes.EXEMPTION.value,
                ReducedTaxTypes.UNKNOWN.value,
                IncludedTaxTypes.UNKNOWN.value));

        // 軽減税率対象、内税: 210
        cartDataList.add(new CartData(110,
                ProductTaxTypes.TAX.value,
                ReducedTaxTypes.REDUCED.value,
                IncludedTaxTypes.INCLUDED.value));
        cartDataList.add(new CartData(100,
                ProductTaxTypes.TAX.value,
                ReducedTaxTypes.REDUCED.value,
                IncludedTaxTypes.INCLUDED.value));

        // 標準税率対象、内税: 130
        cartDataList.add(new CartData(130,
                ProductTaxTypes.TAX.value,
                ReducedTaxTypes.GENERAL.value,
                IncludedTaxTypes.INCLUDED.value));

        // 軽減税率対象、外税: 90
        cartDataList.add(new CartData(90,
                ProductTaxTypes.TAX.value,
                ReducedTaxTypes.REDUCED.value,
                IncludedTaxTypes.EXCLUDED.value));

        // 標準税率対象、外税: 270
        cartDataList.add(new CartData(170,
                ProductTaxTypes.TAX.value,
                ReducedTaxTypes.GENERAL.value,
                IncludedTaxTypes.EXCLUDED.value));
        cartDataList.add(new CartData(100,
                ProductTaxTypes.TAX.value,
                ReducedTaxTypes.GENERAL.value,
                IncludedTaxTypes.EXCLUDED.value));

        // テスト対象のロジック
        TaxCalcBuilder builder = new TaxCalcBuilder(serviceFunctionData);
        for (CartData cartData : cartDataList) {
            builder.add(cartData);
        }
        TaxCalcData data = builder.build();

        // 検証
        assertEquals(8, data.total_count.intValue());
        assertEquals(220 + 307 + 427, data.total_amount.intValue());
        assertEquals(220, data.amount_tax_free.intValue());
        assertEquals(307, data.amount_tax_reduced.intValue());
        assertEquals(427, data.amount_tax_standard.intValue());
        assertEquals(22, data.amount_tax_reduced_only_tax.intValue());
        assertEquals(38, data.amount_tax_standard_only_tax.intValue());
    }
}