package jp.mcapps.android.multi_payment_terminal.database;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateDao;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationDao;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDaoSecure;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDaoSecure;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaDaoSecure;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckDao;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckDaoSecure;

public class DBManager {
    public static DriverDao getDriverDao() {
        return LocalDatabase.getInstance().driverDao();
    }

    public static ErrorDao getErrorDao() {
        return LocalDatabase.getInstance().errorDao();
    }

    public static ErrorStackingDao getErrorStackingDao() {
        return LocalDatabase.getInstance().errorStackingDao();
    }

    public static GpsDao getGpsDao() {
        return LocalDatabase.getInstance().gpsDao();
    }

    public static OperationDao getOperationDao() {
        return LocalDatabase.getInstance().operationDao();
    }

    public static RadioDao getRadioDao() {
        return LocalDatabase.getInstance().radioDao();
    }

    public static SlipDaoSecure getSlipDao() {
        return new SlipDaoSecure(AppPreference.isDemoMode());
    }

    public static UriDaoSecure getUriDao() {
        return new UriDaoSecure(AppPreference.isDemoMode());
    }

    public static AggregateDao getAggregateDao() {
        return AppPreference.isDemoMode()
                ? DemoDatabase.getInstance().aggregateDao()
                : LocalDatabase.getInstance().aggregateDao();
    }

    public static ValidationCheckDao getValidationCheckDao() {
        return new ValidationCheckDaoSecure(AppPreference.isDemoMode());
    }

    public static UriOkicaDaoSecure getUriOkicaDao() {
        return new UriOkicaDaoSecure(AppPreference.isDemoMode());
    }

    public static CartDao getCartDao() {
        return LocalDatabase.getInstance().cartDao();
    }

    public static CategoryDao getCategoryDao() {
        return LocalDatabase.getInstance().categoryDao();
    }

    public static ProductDao getProductDao() {
        return LocalDatabase.getInstance().productDao();
    }

    public static TenantDao getTenantDao() {
        return LocalDatabase.getInstance().tenantDao();
    }

    public static TaxCalcDao getTaxCalcDao(){return LocalDatabase.getInstance().taxCalcDao();}
    public static TransactionDao getTransactionDao(){return LocalDatabase.getInstance().transactionDao();}

    public static ServiceFunctionDao getServiceFunctionDao() {
        return LocalDatabase.getInstance().serviceFunctionDao();
    }

    public static ReceiptDao getReceiptDao() {
        return LocalDatabase.getInstance().receiptDao();
    }

    public static TicketClassDao getTicketClassDao() {
        return LocalDatabase.getInstance().ticketClassDao();
    }

    public static TicketEmbarkDao getTicketEmbarkDao() {
        return LocalDatabase.getInstance().ticketEmbarkDao();
    }

    public static TicketGateEmbarkDao getTicketGateEmbarkDao() {
        return LocalDatabase.getInstance().ticketGateEmbarkDao();
    }

    public static TicketGateSettingsDao getTicketGateSettingsDao() {
        return LocalDatabase.getInstance().ticketGateSettingsDao();
    }

    public static TicketReceiptDao getTicketReceiptDao() {
        return LocalDatabase.getInstance().ticketReceiptDao();
    }
}
