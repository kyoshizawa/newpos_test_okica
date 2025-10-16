package jp.mcapps.android.multi_payment_terminal.ui.pos;

public class CartModel {
    public int id;
    public String name;
    public int unitPrice;
    public String  displayUnitPrice;
    public int count;
    public boolean isCountEditable;
    public boolean isPriceEditable;
    public boolean isCustomPrice;
    public String code;
    public String notice;
    public String code2;
    public String notice2;

    {
        isCountEditable = true;
        isPriceEditable = true;
        isCustomPrice = false;
    }
}
