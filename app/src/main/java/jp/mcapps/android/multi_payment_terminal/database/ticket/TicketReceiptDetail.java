package jp.mcapps.android.multi_payment_terminal.database.ticket;

public class TicketReceiptDetail {
    public String categoryType; // カテゴリ名
    public int price; // 単価
    public int count; // 数量
    public String reducedTax; // 税率
    public int total; // 小計
    public String taxType; // 税種

    // データ作成コンストラクタ
    public TicketReceiptDetail() {
        this.categoryType = "";
        this.price = 0;
        this.count = 0;
        this.reducedTax = "";
        this.total = 0;
        this.taxType = "　"; // レイアウトを揃えるための全角スペース
    }
}
