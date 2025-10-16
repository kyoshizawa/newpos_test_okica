package jp.mcapps.android.multi_payment_terminal.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDaoSecure;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;

public class HistorySlipProvider extends ContentProvider {

    private static final String AUTHORITY = BuildConfig.PREPAID_PROVIDER_NAME;
    private static final String BASE_PATH = "historySlip";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final int CONTACTS = 3;
    private static final int CONTACT_ID = 4;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, CONTACTS);
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CONTACT_ID);
    }

    private SlipDaoSecure db;

    @Override
    public boolean onCreate() {
//        db = new
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                return "vnd.android.cursor.dir/vnd.example.contacts";
            case CONTACT_ID:
                return "vnd.android.cursor.item/vnd.example.contacts";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        long id;
        SlipData data = new SlipData();

        db = new SlipDaoSecure(AppPreference.isDemoMode());

        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                data.transBrand = (String) values.get("transBrand");
                data.transType = (Integer) values.get("transType");
                data.transTypeCode = "0";
                data.transResult = (Integer) values.get("transResult");
                data.transResultDetail = (Integer) values.get("transResultDetail");
                data.printCnt = (Integer) values.get("printCnt");
                data.oldAggregateOrder = (Integer) values.get("oldAggregateOrder");
                data.encryptType = (Integer) values.get("encryptType");
                if (values.get("cancelFlg") != null) {
                    data.cancelFlg = (Integer) values.get("cancelFlg");
                }
                data.transId = (String) values.get("transId");
                data.merchantName = AppPreference.getMerchantName();
                data.merchantOffice = AppPreference.getMerchantOffice();
                data.merchantTelnumber = AppPreference.getMerchantTelnumber();
                data.carId = AppPreference.getMcCarId();
                data.driverId = AppPreference.getMcDriverId();
                data.termId = AppPreference.getMcTermId();
                data.termSequence = (Integer) values.get("termSequence");
                data.transDate = (String) values.get("transDate");
                data.cardCompany = (String) values.get("cardCompany");
                data.cardIdMerchant = (String) values.get("cardIdMerchant");
                data.cardIdCustomer = (String) values.get("cardIdCustomer");
                data.cardExpDate = (String) values.get("cardExpDate");
                data.cardTransNumber = (Integer) values.get("cardTransNumber");

                data.slipNumber = (Integer) values.get("slipNumber");

                data.commodityCode = (String) values.get("commodityCode");
                data.installment = (String) values.get("installment");

                data.transAmount = (Integer) values.get("transAmount");
                data.transSpecifiedAmount = (Integer) values.get("transSpecifiedAmount");
                data.transMeterAmount = (Integer) values.get("transMeterAmount");
                data.transAdjAmount = (Integer) values.get("transAdjAmount");
                data.transCashTogetherAmount = (Integer) values.get("transCashTogetherAmount");
                data.transOtherAmountOneType = (Integer) values.get("transOtherAmountOneType");
                data.transOtherAmountOne = (Integer) values.get("transOtherAmountOne");
                data.transOtherAmountTwoType = (Integer) values.get("transOtherAmountTwoType");
                data.transOtherAmountTwo = (Integer) values.get("transOtherAmountTwo");
                data.transAfterBalance = (Long) values.get("transAfterBalance");
                data.freeCountOne = (Integer) values.get("freeCountOne");
                data.freeCountTwo = (Integer) values.get("freeCountTwo");
                data.transCompleteAmount = (Integer) values.get("transCompleteAmount");
                data.transactionTerminalType = (Integer) values.get("transactionTerminalType");
                data.sendCancelPurchasedTicket = (Integer) values.get("sendCancelPurchasedTicket");

                data.card_id_card_company = (String) values.get("cardIdCardCompany");

                data.prepaidAddPoint = (Integer) values.get("prepaidAddPoint");
                data.prepaidTotalPoint = (Integer) values.get("prepaidTotalPoint");
                data.prepaidNextExpired = (String) values.get("prepaidNextExpired");
                data.prepaidNextExpiredPoint = (Integer) values.get("prepaidNextExpiredPoint");
                data.prepaidServiceName = (String) values.get("prepaidServiceName");

                data.cardIdCustomer = data.card_id_card_company;
                data.cardIdMerchant = data.card_id_card_company;

                db.insertSlipData(data);
                data = db.getLatestOne();
                id = data.id;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI yaaa: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(CONTENT_URI, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
