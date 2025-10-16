package jp.mcapps.android.multi_payment_terminal.model.pos;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.Result;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.encoding.ErrorDetail;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr.QRCodeData;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr.QRCodeParser;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128.BarcodeData;
import jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128.BarcodeParser;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import timber.log.Timber;

public class CartRepository {

    final CartDao _cardDao = LocalDatabase.getInstance().cartDao();
    final ProductDao _productDao = LocalDatabase.getInstance().productDao();

    /**
     * 商品をカートに追加する
     * @param code 商品コード
     */
    public Result<CartData, DomainErrors> insertProduct(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code is null");
        }
        CartData cart;

        List<ProductData> products = _productDao.getProductsByCode(code);
        if (!products.isEmpty()) {
            // 商品が見つかった場合は、カートに追加
            ProductData product = products.get(0);
            try {
                cart = insertProduct(product);
                return Result.ok(cart);
            } catch (DomainErrors.Exception e) {
                return Result.err(e.getError());
            }
        }

        // GS1-128
        cart = maybeGS1_128Code(code);
        if (cart != null) {
            return Result.ok(cart);
        }

        // eL-QR
        cart = maybeELQRCode(code);
        if (cart != null) {
            return Result.ok(cart);
        }

        // 商品が見つからない
        return Result.err(DomainErrors.NOT_FOUND);
    }

    /**
     * 商品をカートに追加する
     * @param product 商品情報
     */
    public CartData insertProduct(ProductData product) throws DomainErrors.Exception {
        if (product == null) {
            throw new IllegalArgumentException("product is null");
        }

        // 商品コードをカートに突っ込む
        List<CartData> carts = _cardDao.getProductByProductCode(product.product_code);
        if (!carts.isEmpty()) {
            // すでに同じ商品がカートに入っている場合は、数量を増やす
            CartData cart = carts.get(0);
            cart.Increment(); // Domain Error: OUT_OF_RANGE
            _cardDao.updateCountById(cart.id, cart.count);
            return cart;
        } else {
            // 新規商品の場合は、カートに追加
            CartData cart = new CartData(product);
            _cardDao.insertCartData(cart);
            return cart;
        }
    }

    /**
     * コンビニ収納バーコード（GS1-128コード）かどうかを判定して、カートに追加する
     * @param code バーコード文字列
     */
    private CartData maybeGS1_128Code(String code) {

        // GS1-128コードかどうかを判定する
        BarcodeParser parser = new BarcodeParser();
        Result<BarcodeData, ErrorDetail> result = parser.parseString(code);
        if (!result.isOk()) {
            // GS1-128コードではない
            return null;
        }
        BarcodeData barcode = result.ok;
        Timber.i("GS1-128 code detected: %s (JPY %d)", code, barcode.getPaymentAmount());

        // すでに同じバーコードがカートに入っている場合は、追加しない
        List<CartData> carts = _cardDao.getProductByBarcode(code);
        if (!carts.isEmpty()) {
            return carts.get(0);
        }

        // カートデータを作成する (非課税)
        CartData cart = new CartData(
                barcode.getPaymentAmount(),
                ProductTaxTypes.EXEMPTION.value,
                ReducedTaxTypes.EXEMPTION.value,
                IncludedTaxTypes.EMPTY.value);
        cart.product_name = "コンビニ収納用バーコード";
        cart.barcode_type = CartData.BARCODE_TYPE_GS1_128;
        cart.barcode_text = code;
        cart.payment_due_date = barcode.getDueDate();

        // 支払期限が過ぎているかどうかを判定する
        Date today = today();
        if (cart.payment_due_date != null && cart.payment_due_date.before(today)) {
            // 期限切れでもカート追加は可能、ただし警告を表示する必要がある
            cart.is_payment_overdue = true;
        }

        // カートに追加
        _cardDao.insertCartData(cart);

        return cart;
    }


    /**
     * 地方統一税QRコード（eL-QR）かどうかを判定して、カートに追加する
     * @param code バーコード文字列
     */
    private CartData maybeELQRCode(String code) {

        // eL-QRコードかどうかを判定する
        QRCodeParser parser = new QRCodeParser();
        Result<QRCodeData, ErrorDetail> result = parser.parseString(code);
        if (!result.isOk()) {
            // eL-QRコードではない
            return null;
        }
        QRCodeData qrCode = result.ok;
        Timber.i("eL-QR code detected: %s (JPY %d)", code, qrCode.getPaymentAmount());

        // すでに同じバーコードがカートに入っている場合は、追加しない
        List<CartData> carts = _cardDao.getProductByBarcode(code);
        if (!carts.isEmpty()) {
            return carts.get(0);
        }

        // カートデータを作成する (非課税)
        CartData cart;
        List<ProductData> products = _productDao.getProductsByCode(qrCode.getTaxItemNumber());
        if (!products.isEmpty()) {
            // 「税目・料金番号」が商品コードとして登録されている場合は、その商品マスタを使用する
            cart = new CartData(products.get(0));
            // 金額、税区分は、QRコードの内容を使用する
            cart.standard_unit_price = qrCode.getPaymentAmount();
            cart.tax_type = ProductTaxTypes.EXEMPTION.value;
            cart.reduce_tax_type = ReducedTaxTypes.EXEMPTION.value;
            cart.included_tax_type = IncludedTaxTypes.EMPTY.value;
        } else {
            cart = new CartData(
                    qrCode.getPaymentAmount(),
                    ProductTaxTypes.EXEMPTION.value,
                    ReducedTaxTypes.EXEMPTION.value,
                    IncludedTaxTypes.EMPTY.value);
            cart.product_name = "地方統一税QR";
        }
        cart.barcode_type = CartData.BARCODE_TYPE_EL_QR;
        cart.barcode_text = code;
        cart.filing_due_date = qrCode.getFilingDueDate();
        cart.payment_due_date = qrCode.getPaymentDueDate();

        Date today = today();
        // 納期限が過ぎているかどうかを判定する
        if (cart.filing_due_date != null && cart.filing_due_date.before(today)) {
            // 期限切れでもカート追加は可能、ただし警告を表示する必要がある
            cart.is_filing_overdue = true;
        }
        // 支払期限が過ぎているかどうかを判定する
        if (cart.payment_due_date != null && cart.payment_due_date.before(today)) {
            // 期限切れでもカート追加は可能、ただし警告を表示する必要がある
            cart.is_payment_overdue = true;
        }

        // カートに追加
        _cardDao.insertCartData(cart);

        return cart;
    }

    private Date today() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }
}
