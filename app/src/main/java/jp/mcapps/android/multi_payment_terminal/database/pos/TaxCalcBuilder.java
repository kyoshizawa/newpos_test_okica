package jp.mcapps.android.multi_payment_terminal.database.pos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import timber.log.Timber;

/**
 * 税金計算の実装（仕様については、以下のWordドキュメントを参照）
 * https://mobilecreatecorpjp.sharepoint.com/:w:/s/PF/EYlBEH2Z_a9PgzsQU9B7kOMBjUvqIyQ5Y8-LBWEK2u9P-g?e=VFuJAG
 */
public class TaxCalcBuilder {

    private final BigDecimal TAX_RATE_STANDARD; // 一般課税率
    private final BigDecimal TAX_RATE_REDUCED; // 軽減課税率
    private final TaxRoundings TAX_ROUNDINGS; // 端数処理

    public TaxCalcBuilder(@Nullable ServiceFunctionData serviceFunctionData) {
        if (serviceFunctionData != null) {
            // 端数処理が店舗ごとに違うらしい
            TAX_RATE_STANDARD = new BigDecimal(serviceFunctionData.standard_tax_rate);
            TAX_RATE_REDUCED = new BigDecimal(serviceFunctionData.reduced_tax_rate);
            TAX_ROUNDINGS = TaxRoundings.valueOf(serviceFunctionData.tax_rounding);
        } else {
            // デフォルト
            TAX_RATE_STANDARD = new BigDecimal("0.1");
            TAX_RATE_REDUCED = new BigDecimal("0.08");
            TAX_ROUNDINGS = TaxRoundings.FLOOR;
        }
    }

    private int totalItemCount; // 個数合計
    private int totalAmountTaxFree; // 非課税合計
    private int totalAmountTaxExclusiveStandardWithoutTax; // 外税合計（一般課税）
    private int totalAmountTaxExclusiveReducedWithoutTax; // 外税合計（軽減課税）
    private int totalAmountTaxInclusiveStandard; // 内税合計（一般課税）
    private int totalAmountTaxInclusiveReduced; // 内税合計（軽減課税）

    public TaxCalcBuilder add(@NotNull CartData item) {

        if (item.count == 0) {
            Timber.d("%s: 個数0個なのでスキップ", item.product_name);
            return this;
        }

        totalItemCount += item.count; // 個数合計

        if (item.tax_type == ProductTaxTypes.EXEMPTION.value) {
            // 非課税
            Timber.d("%s: 非課税", item.product_name);
            totalAmountTaxFree += unitPrice(item) * item.count; // 非課税の単価の加算
            return this;
        }

        if (item.included_tax_type == IncludedTaxTypes.INCLUDED.value) {
            // 内税
            Timber.d("%s: 内税", item.product_name);
            switch (ReducedTaxTypes.valueOf(item.reduce_tax_type)) {
                case REDUCED:
                    // 軽減税率(内税)
                    totalAmountTaxInclusiveReduced += unitPrice(item) * item.count;
                    break;

                case GENERAL:
                default:
                    // 一般課税(内税)
                    totalAmountTaxInclusiveStandard += unitPrice(item) * item.count;
                    break;
            }
        } else {
            // 外税
            Timber.d("%s: 外税", item.product_name);
            switch (ReducedTaxTypes.valueOf(item.reduce_tax_type)) {
                case REDUCED:
                    // 軽減税率(外税)
                    totalAmountTaxExclusiveReducedWithoutTax += unitPrice(item) * item.count;
                    break;

                case GENERAL:
                default:
                    // 一般課税(外税)
                    totalAmountTaxExclusiveStandardWithoutTax += unitPrice(item) * item.count;
                    break;
            }
        }
        return this;
    }

    public TaxCalcData build() {
        BigDecimal taxCoefReduced = BigDecimal.ONE.add(TAX_RATE_REDUCED); // 軽減課税率係数 (1 + 0.08)
        BigDecimal taxCoefStandard = BigDecimal.ONE.add(TAX_RATE_STANDARD); // 一般課税率係数 (1 + 0.1)

        // 税率8%対象（外税）の税込金額を求める: 税抜金額 * (1 + 0.08)
        BigDecimal totalAmountTaxExclusiveReducedReal = new BigDecimal(totalAmountTaxExclusiveReducedWithoutTax)
                .multiply(taxCoefReduced);
        BigDecimal totalAmountTaxExclusiveReduced = roundValue(totalAmountTaxExclusiveReducedReal, TAX_ROUNDINGS);
        BigDecimal totalAmountTaxExclusiveReducedOnlyTax = totalAmountTaxExclusiveReduced
                .subtract(new BigDecimal(totalAmountTaxExclusiveReducedWithoutTax));

        // 税率10%対象（外税）の税込金額を求める (税抜金額 * (1 + 0.1)
        BigDecimal totalAmountTaxExclusiveStandardReal = new BigDecimal(totalAmountTaxExclusiveStandardWithoutTax)
                .multiply(taxCoefStandard);
        BigDecimal totalAmountTaxExclusiveStandard = roundValue(totalAmountTaxExclusiveStandardReal, TAX_ROUNDINGS);
        BigDecimal totalAmountTaxExclusiveStandardOnlyTax = totalAmountTaxExclusiveStandard
                .subtract(new BigDecimal(totalAmountTaxExclusiveStandardWithoutTax));

        // 税率8％対象の税込合計を求める（サ）: 税抜金額 * (1 + 0.08) + 税込金額 ※端数処理あり
        BigDecimal totalAmountReducedTax = roundValue(
                totalAmountTaxExclusiveReducedReal.add(new BigDecimal(totalAmountTaxInclusiveReduced)),
                TAX_ROUNDINGS);

        // 税率10％対象の税込合計を求める（シ）: 税抜金額 * (1 + 0.1) + 税込金額 ※端数処理あり
        BigDecimal totalAmountStandardTax = roundValue(
                totalAmountTaxExclusiveStandardReal.add(new BigDecimal(totalAmountTaxInclusiveStandard)),
                TAX_ROUNDINGS);

        // 税率8％対象の内消費税額を求める（ス）: (税込金額 - 税抜金額) / (1 + 0.08) * 0.08 ※端数処理あり
        BigDecimal totalAmountReducedTaxOnlyTax = roundValue(
                totalAmountReducedTax.divide(taxCoefReduced, MathContext.DECIMAL128).multiply(TAX_RATE_REDUCED),
                TAX_ROUNDINGS);

        // 税率10％対象の内消費税額を求める（セ）: (税込金額 - 税抜金額) / (1 + 0.1) * 0.1 ※端数処理あり
        BigDecimal totalAmountStandardTaxOnlyTax = roundValue(
                totalAmountStandardTax.divide(taxCoefStandard, MathContext.DECIMAL128).multiply(TAX_RATE_STANDARD),
                TAX_ROUNDINGS);

        // 合計金額: 非課税合計 + 税率8％対象の税込合計 + 税率10％対象の税込合計
        BigDecimal totalAmount = new BigDecimal(totalAmountTaxFree).add(totalAmountReducedTax).add(totalAmountStandardTax);

        // TaxCalcDataに詰める
        TaxCalcData data = new TaxCalcData();
        data.standard_tax_rate = TAX_RATE_STANDARD.multiply(new BigDecimal(100)).intValue(); // 税率10%
        data.reduced_tax_rate = TAX_RATE_REDUCED.multiply(new BigDecimal(100)).intValue(); // 税率8%
        data.total_amount = totalAmount.intValue(); // 合計金額
        data.total_count = totalItemCount; // 合計個数
        data.amount_tax_free = totalAmountTaxFree; // 非課税合計
        data.amount_tax_reduced = totalAmountReducedTax.intValue(); // 税率8％対象の税込合計
        data.amount_tax_standard = totalAmountStandardTax.intValue(); // 税率10％対象の税込合計
        data.amount_tax_reduced_only_tax = totalAmountReducedTaxOnlyTax.intValue(); // 税率8％対象の内消費税額
        data.amount_tax_standard_only_tax = totalAmountStandardTaxOnlyTax.intValue(); // 税率10％対象の内消費税額
        data.amount_tax_exclusive_reduced = totalAmountTaxExclusiveReduced.intValue(); // 税率8%対象（外税）の税込金額
        data.amount_tax_exclusive_standard =totalAmountTaxExclusiveStandard.intValue(); // 税率10%対象（外税）の税込金額
        data.amount_tax_exclusive_reduced_without_tax = totalAmountTaxExclusiveReducedWithoutTax; // 税率8%対象（外税）の税抜金額
        data.amount_tax_exclusive_standard_without_tax = totalAmountTaxExclusiveStandardWithoutTax; // 税率10%対象（外税）の税抜金額
        data.amount_tax_exclusive_reduced_only_tax = totalAmountTaxExclusiveReducedOnlyTax.intValue(); // 税率8%対象（外税）の税額
        data.amount_tax_exclusive_standard_only_tax = totalAmountTaxExclusiveStandardOnlyTax.intValue(); // 税率10%対象（外税）の税額
        data.amount_tax_inclusive_reduced = totalAmountTaxInclusiveReduced; // 税率8%対象（内税）の税込金額
        data.amount_tax_inclusive_standard = totalAmountTaxInclusiveStandard; // 税率10%対象（内税）の税込金額
        return data;
    }

    static BigDecimal roundValue(BigDecimal value, TaxRoundings roundings) {
        switch (roundings) {
            case ROUND:
                // 四捨五入
                return value.setScale(0, RoundingMode.HALF_UP);

            case CEILING:
                // 切り上げ
                return value.setScale(0, RoundingMode.UP);

            case FLOOR:
            default:
                // 切り捨て
                return value.setScale(0, RoundingMode.DOWN);
        }
    }

    // 単価を取得する
    private static int unitPrice(CartData item) {
        if(item.is_custom_price) {
            return item.custom_unit_price;
        } else {
            return item.standard_unit_price;
        }
    }
}
