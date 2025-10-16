package jp.mcapps.android.multi_payment_terminal.ui.pos;

public class CartCalculateResultModel {
    public Integer noTaxAmount; // 非課税合計
    public Integer excludeTaxCommonAmount; // 一般課税合計(外税)
    public Integer excludeTaxReduceAmount; // 軽減課税合計(外税)
    public Integer includeTaxCommonAmount; // 一般課税合計(内税)
    public Integer includeTaxReduceAmount; // 軽減課税合計(内税)
    public Integer countAmount; // 個数合計
    public Integer unitPriceAmount; // 単価合計
    public Integer priceAmount; // 金額合計
}
