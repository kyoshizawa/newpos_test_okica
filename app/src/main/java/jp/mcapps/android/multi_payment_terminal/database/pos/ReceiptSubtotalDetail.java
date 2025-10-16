package jp.mcapps.android.multi_payment_terminal.database.pos;

public class ReceiptSubtotalDetail {
    public int reduced_tax_rate;     // 軽減税率
    public int standard_tax_rate;    // 標準税率
    public int amount_tax_exclusive_reduced_without_tax;   // 税抜き・軽減税率の商品代
    public int amount_tax_exclusive_reduced_only_tax;       // 税抜き・軽減税率の税
    public int amount_tax_exclusive_standard_without_tax;  // 税抜き・標準税率の商品代
    public int amount_tax_exclusive_standard_only_tax;      // 税抜き・標準税率の税
    public int amount_tax_inclusive_reduced;    // 税込み・軽減税率
    public int amount_tax_inclusive_standard;   // 税込み・標準税率
    public int amount_tax_free; // 非課税の商品代
    public int amount_tax_reduced;     // 軽減税率の商品代＋税（合計）
    public int amount_tax_standard;    // 標準税率の商品代＋税（合計）,
    public int amount_tax_reduced_only_tax;  // 軽減税率の税（合計）
    public int amount_tax_standard_only_tax; // 標準税率の税 （合計）

    // データ作成コンストラクタ
    public ReceiptSubtotalDetail() {
        this.reduced_tax_rate = 0;
        this.standard_tax_rate = 0;
        this.amount_tax_exclusive_reduced_without_tax = 0;
        this.amount_tax_exclusive_reduced_only_tax = 0;
        this.amount_tax_exclusive_standard_without_tax = 0;
        this.amount_tax_exclusive_standard_only_tax = 0;
        this.amount_tax_inclusive_reduced = 0;
        this.amount_tax_inclusive_standard = 0;
        this.amount_tax_free = 0;
        this.amount_tax_reduced = 0;
        this.amount_tax_standard = 0;
        this.amount_tax_reduced_only_tax = 0;
        this.amount_tax_standard_only_tax = 0;
    }
}
