package jp.mcapps.android.multi_payment_terminal.database.pos;

public class ReceiptProductDetail {
    public String name; // 商品名

    public int price; // 単価

    public int count; // 数量

    public String reducedTax; // 税率

    public int total; // 小計

    public String taxType; // 税種

    // データ作成コンストラクタ
    public ReceiptProductDetail() {
        this.name = "";
        this.price = 0;
        this.count = 0;
        this.reducedTax = "";
        this.total = 0;
        this.taxType = "　"; // レイアウトを揃えるための全角スペース
    }
}
