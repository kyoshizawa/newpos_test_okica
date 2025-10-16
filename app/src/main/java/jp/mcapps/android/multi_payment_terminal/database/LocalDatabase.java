package jp.mcapps.android.multi_payment_terminal.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Calendar;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateDao;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateData;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverDao;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverData;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsData;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationDao;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationData;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioDao;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDetailDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDetailData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassNameLangDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassNameLangData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkNameLangDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkNameLangData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateEmbarkData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGtfsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGtfsData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptData;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketGateEmbark;
import timber.log.Timber;

@Database(entities = {
        RadioData.class,
        GpsData.class,
        OperationData.class,
        DriverData.class,
        ErrorData.class,
        ErrorStackingData.class,
        SlipData.class,
        UriData.class,
        AggregateData.class,
        UriOkicaData.class,
        ProductData.class,
        CategoryData.class,
        CartData.class,
        TenantData.class,
        ServiceFunctionData.class,
        TaxCalcData.class,
        TransactionData.class,
        TransactionDetailData.class,
        TerminalData.class,
        ReceiptData.class,
        TicketClassData.class,
        TicketClassNameLangData.class,
        TicketEmbarkData.class,
        TicketEmbarkNameLangData.class,
        TicketGateEmbarkData.class,
        TicketGateSettingsData.class,
        TicketReceiptData.class,
        TicketGtfsData.class,
}, version = 11, exportSchema = true)

@TypeConverters({Converters.class})
public abstract class LocalDatabase extends RoomDatabase {
    public abstract RadioDao radioDao();

    public abstract GpsDao gpsDao();

    public abstract OperationDao operationDao();

    public abstract DriverDao driverDao();

    public abstract ErrorDao errorDao();

    public abstract ErrorStackingDao errorStackingDao();

    public abstract SlipDao slipDao();

    public abstract UriDao uriDao();

    public abstract AggregateDao aggregateDao();

    public abstract UriOkicaDao uriOkicaDao();

    public abstract ProductDao productDao();

    public abstract CartDao cartDao();

    public abstract CategoryDao categoryDao();

    public abstract TenantDao tenantDao();

    public abstract TerminalDao terminalDao();

    public abstract ServiceFunctionDao serviceFunctionDao();

    public abstract TaxCalcDao taxCalcDao();

    public abstract TransactionDao transactionDao();

    public abstract TransactionDetailDao transactionDetailDao();

    public abstract ReceiptDao receiptDao();

    public abstract TicketClassDao ticketClassDao();

    public abstract TicketClassNameLangDao ticketClassNameLangDao();

    public abstract TicketEmbarkDao ticketEmbarkDao();

    public abstract TicketEmbarkNameLangDao ticketEmbarkNameLangDao();

    public abstract TicketGateEmbarkDao ticketGateEmbarkDao();

    public abstract TicketGateSettingsDao ticketGateSettingsDao();

    public abstract TicketReceiptDao ticketReceiptDao();

    public abstract TicketGtfsDao ticketGtfsDao();

    private static LocalDatabase _localDatabase;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration ver1->2");
            database.execSQL("CREATE TABLE history_uri_okica(" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    " okica_send INTEGER, " +
                    " okica_card_idi BLOB, " +
                    " okica_detail_id INTEGER, " +
                    " okica_process_code INTEGER, " +
                    " okica_trans_date TEXT, " +
                    " okica_functiion_type INTEGER, " +
                    " okica_control_code INTEGER, " +
                    " okica_unfinished_flg INTEGER, " +
                    " okica_business_code INTEGER, " +
                    " okica_model_code TEXT, " +
                    " okica_machine_id TEXT, " +
                    " okica_sequence INTEGER, " +
                    " okica_process_type INTEGER, " +
                    " okica_sf_log_id INTEGER, " +
                    " okica_sf1_business_id INTEGER, " +
                    " okica_sf1_amount INTEGER, " +
                    " okica_sf1_balance INTEGER, " +
                    " okica_sf1_category INTEGER, " +
                    " trans_amount INTEGER, " +
                    " trans_specified_amount INTEGER, " +
                    " trans_meter_amount INTEGER, " +
                    " trans_adj_amount INTEGER, " +
                    " trans_cash_together_amount INTEGER, " +
                    " trans_other_amount_one_type INTEGER, " +
                    " trans_other_amount_one INTEGER, " +
                    " trans_other_amount_two_type INTEGER, " +
                    " trans_other_amount_two INTEGER, " +
                    " trans_time INTEGER, " +
                    " term_latitude TEXT, " +
                    " term_longitude TEXT, " +
                    " term_network_type TEXT, " +
                    " term_radio_level INTEGER, " +
                    " encrypt_type INTEGER)"
            );
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration ver2->3");
            database.execSQL("ALTER TABLE history_uri ADD COLUMN wallet TEXT");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase _db) {
            Timber.i("DB migration ver3->4");

            // 以下の（generated）ソースファイルからSQLをコピー
            // jp.mcapps.android.multi_payment_terminal.database/LocalDatabase_Impl
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `product_id` INTEGER NOT NULL, `service_instance_id` TEXT NOT NULL, `product_code` TEXT NOT NULL, `name` TEXT, `name_kana` TEXT, `name_short` TEXT, `standard_unit_price` INTEGER, `tax_type` INTEGER, `reduce_tax_type` INTEGER, `included_tax_type` INTEGER, `sale_start_at` INTEGER, `sale_end_at` INTEGER, `status` INTEGER, `remarks` TEXT, `product_category_id` INTEGER, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER)");
            _db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pos_products_product_id_generation_id` ON `pos_products` (`product_id`, `generation_id`)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `service_instance_id` TEXT NOT NULL, `name` TEXT NOT NULL, `name_kana` TEXT, `name_short` TEXT, `status` INTEGER, `parent_id` INTEGER, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER)");
            _db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pos_categories_category_id_generation_id` ON `pos_categories` (`category_id`, `generation_id`)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_carts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `product_id` INTEGER, `product_category_id` INTEGER, `count` INTEGER, `product_name` TEXT, `product_name_short` TEXT, `product_code` TEXT, `standard_unit_price` INTEGER, `custom_unit_price` INTEGER, `tax_type` INTEGER, `reduce_tax_type` INTEGER, `included_tax_type` INTEGER, `is_manual` INTEGER, `is_custom_price` INTEGER, `created_at` INTEGER, `update_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_tenants` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tenant_id` INTEGER NOT NULL, `service_instance_id` TEXT NOT NULL, `tenant_code` TEXT NOT NULL, `merchant_id` INTEGER NOT NULL, `customer_code` TEXT NOT NULL, `name` TEXT, `name_kana` TEXT, `zipcode` TEXT, `pref_cd` INTEGER, `city` TEXT, `address_line1` TEXT, `address_line2` TEXT, `address_line3` TEXT, `kana_city` TEXT, `address_kana_line1` TEXT, `address_kana_line2` TEXT, `address_kana_line3` TEXT, `phone_number` TEXT, `fax` TEXT, `houjin_bangou` TEXT, `alphabet_name` TEXT, `parent_name` TEXT, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER)");
            _db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pos_tenants_tenant_id_generation_id` ON `pos_tenants` (`tenant_id`, `generation_id`)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_service_functions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customer_code` TEXT NOT NULL, `is_product_category` INTEGER NOT NULL, `is_pos_receipt` INTEGER NOT NULL, `is_manual_amount` INTEGER NOT NULL, `slip_title` TEXT NOT NULL DEFAULT '', `tax_rounding` INTEGER NOT NULL DEFAULT 0, `standard_tax_rate` TEXT, `reduced_tax_rate` TEXT, `receipt_count` INTEGER NOT NULL, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER)");
            _db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pos_service_functions_generation_id` ON `pos_service_functions` (`generation_id`)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_tax_calc` (`id` INTEGER NOT NULL, `standard_tax_rate` INTEGER, `reduced_tax_rate` INTEGER, `total_amount` INTEGER, `total_count` INTEGER, `amount_tax_free` INTEGER, `amount_tax_reduced` INTEGER, `amount_tax_standard` INTEGER, `amount_tax_reduced_only_tax` INTEGER, `amount_tax_standard_only_tax` INTEGER, `amount_tax_exclusive_reduced` INTEGER, `amount_tax_exclusive_standard` INTEGER, `amount_tax_exclusive_reduced_without_tax` INTEGER, `amount_tax_exclusive_standard_without_tax` INTEGER, `amount_tax_exclusive_reduced_only_tax` INTEGER, `amount_tax_exclusive_standard_only_tax` INTEGER, `amount_tax_inclusive_reduced` INTEGER, `amount_tax_inclusive_standard` INTEGER, PRIMARY KEY(`id`))");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `service_instance_id` TEXT, `transaction_type` INTEGER, `transaction_at` INTEGER, `tid` TEXT, `terminal_id` INTEGER, `terminal_name` TEXT, `merchant_id` INTEGER, `tenant_id` INTEGER, `tenant_code` TEXT, `tenant_name` TEXT, `transaction_no` TEXT, `card_category` TEXT, `card_brand` TEXT, `amount` INTEGER, `cash_amount` INTEGER, `staff_code` TEXT, `staff_name` TEXT, `org_transaction_at` INTEGER, `org_transaction_id` TEXT, `is_unexecuted` INTEGER NOT NULL, `uploaded` INTEGER NOT NULL)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_transactions_detail` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `payment_transaction_id` INTEGER NOT NULL, `transaction_at` INTEGER, `product_id` INTEGER NOT NULL, `product_code` TEXT, `product_name` TEXT, `unit_price` INTEGER, `count` INTEGER, `product_tax_type` INTEGER, `reduced_tax_type` INTEGER, `included_tax_type` INTEGER, `is_manual` INTEGER, `org_unit_price` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_terminals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `terminal_id` TEXT NOT NULL, `customer_id` TEXT NOT NULL, `terminal_no` TEXT NOT NULL, `service_instance_abt` TEXT NOT NULL, `service_instance_pos` TEXT NOT NULL, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER)");
            _db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pos_terminals_generation_id` ON `pos_terminals` (`generation_id`)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `pos_receipts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `slip_id` INTEGER NOT NULL, `merchant_name` TEXT NOT NULL, `organization_name` TEXT NOT NULL, `car_no` TEXT NOT NULL, `phone_number` TEXT NOT NULL, `address` TEXT NOT NULL, `staff_code` TEXT NOT NULL, `term_sequence` TEXT NOT NULL, `trans_date` TEXT NOT NULL, `old_term_sequence` TEXT NOT NULL, `old_trans_date` TEXT NOT NULL, `terminal_number` TEXT NOT NULL, `total_amount` INTEGER NOT NULL, `money_type` TEXT NOT NULL, `trans_type_amount` INTEGER NOT NULL, `trans_cash_amount` INTEGER NOT NULL, `change_amount` INTEGER NOT NULL, `invoice_no` TEXT NOT NULL, `print_cnt` INTEGER NOT NULL, `canceled_trans` INTEGER, `product_detail` TEXT NOT NULL, `subtotal_detail` TEXT NOT NULL)");
            _db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pos_receipts_slip_id` ON `pos_receipts` (`slip_id`)");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration ver4->5");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN credit_kid TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN trans_complete_amount INTEGER");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration ver5->6");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN printing_authid TEXT");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase _db) {
            Timber.i("DB migration ver6->7");

            // 以下の（generated）ソースファイルからSQLをコピー
            // jp.mcapps.android.multi_payment_terminal.database/LocalDatabase_Impl
            if (AppPreference.isServicePos()) {
                _db.execSQL("ALTER TABLE history_slip ADD COLUMN transaction_terminal_type INTEGER DEFAULT 1");
            } else {
                _db.execSQL("ALTER TABLE history_slip ADD COLUMN transaction_terminal_type INTEGER DEFAULT 0");
            }
            _db.execSQL("ALTER TABLE history_slip ADD COLUMN card_category TEXT");
            _db.execSQL("ALTER TABLE history_slip ADD COLUMN card_brand_code TEXT");
            _db.execSQL("ALTER TABLE history_slip ADD COLUMN card_brand_name TEXT");
            _db.execSQL("ALTER TABLE history_slip ADD COLUMN purchased_ticket_deal_id TEXT");
            _db.execSQL("ALTER TABLE history_slip ADD COLUMN trip_reservation_id TEXT");
            _db.execSQL("ALTER TABLE history_slip ADD COLUMN send_cancel_purchased_ticket INTEGER");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_gtfs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gtfs_version` TEXT, `created_at` INTEGER, `updated_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_classes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ticket_class_id` INTEGER, `ticket_class_name` TEXT, `service_instance_id` TEXT, `reserve_type` TEXT, `enable_route_judge` INTEGER, `enable_trip_judge` INTEGER, `enable_stop_judge` INTEGER, `ticket_class_created_at` TEXT, `ticket_class_created_auth_server_id` INTEGER, `ticket_class_created_user_id` TEXT, `ticket_class_created_user_name` TEXT, `ticket_class_updated_at` TEXT, `ticket_class_updated_auth_server_id` INTEGER, `ticket_class_updated_user_id` TEXT, `ticket_class_updated_user_name` TEXT, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER, `updated_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_class_name_lang` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ticket_class_id` INTEGER, `lang` TEXT, `name` TEXT, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER, `updated_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_embark` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ticket_class_id` INTEGER NOT NULL, `route_id` TEXT, `route_name` TEXT, `stop_id` TEXT, `stop_name` TEXT, `stop_type` TEXT, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER, `updated_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_embark_name_lang` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stop_id` TEXT, `lang` TEXT, `name` TEXT, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER, `updated_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_gate_embark` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stop_id` TEXT, `stop_name` TEXT, `stop_type` TEXT, `generation_id` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER, `updated_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_gate_settings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stop_id` TEXT, `stop_name` TEXT, `stop_type` TEXT, `gate_check_start_time` INTEGER, `auto_gate_check` INTEGER, `auto_gate_check_interval` INTEGER, `route_id` TEXT, `route_name` TEXT, `trip_id` TEXT, `start_stop_name` TEXT, `start_departure_time` TEXT, `end_stop_name` TEXT, `end_arrival_time` TEXT, `created_at` INTEGER, `updated_at` INTEGER)");
            _db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_receipts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `slip_id` INTEGER NOT NULL, `merchant_name` TEXT NOT NULL, `organization_name` TEXT NOT NULL, `car_no` TEXT NOT NULL, `phone_number` TEXT NOT NULL, `address` TEXT NOT NULL, `staff_code` TEXT NOT NULL, `term_sequence` TEXT NOT NULL, `trans_date` TEXT NOT NULL, `old_term_sequence` TEXT NOT NULL, `old_trans_date` TEXT NOT NULL, `terminal_number` TEXT NOT NULL, `total_amount` INTEGER NOT NULL, `money_type` TEXT NOT NULL, `trans_type_amount` INTEGER NOT NULL, `trans_cash_amount` INTEGER NOT NULL, `change_amount` INTEGER NOT NULL, `invoice_no` TEXT NOT NULL, `print_cnt` INTEGER NOT NULL, `canceled_trans` INTEGER, `ticket_detail` TEXT NOT NULL, `subtotal_detail` TEXT NOT NULL)");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration var7->8");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN watari_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN watari_sum_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN watari_calidity_period TEXT");
        }
    };

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration var8->9");
            database.execSQL("ALTER TABLE pos_carts ADD COLUMN barcode_type TEXT");
            database.execSQL("ALTER TABLE pos_carts ADD COLUMN barcode_text TEXT");
            database.execSQL("ALTER TABLE pos_carts ADD COLUMN filing_due_date INTEGER");
            database.execSQL("ALTER TABLE pos_carts ADD COLUMN is_filing_overdue INTEGER");
            database.execSQL("ALTER TABLE pos_carts ADD COLUMN payment_due_date INTEGER");
            database.execSQL("ALTER TABLE pos_carts ADD COLUMN is_payment_overdue INTEGER");
            database.execSQL("ALTER TABLE pos_transactions_detail ADD COLUMN cart_data_json TEXT");
        }
    };

    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration var9->10");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_add_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_total_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_next_expired TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_next_expired_point INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN prepaid_service_name TEXT");
        }
    };

    //ADD-S BMT E.Oyama 2024/09/18 フタバ双方向向け改修
    private static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("DB migration var10->11");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  trans_type_name TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  status TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  slip_kind INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  pay_separation INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_id_card_company TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_exp_date_merchant TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_exp_date_card_company TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  auth_id_str TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  cat_dual_type INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_seq_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  atc TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  rw_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  sprw_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  off_on_type TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_type TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_yuko_msg TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_marchant INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_total TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_exp_date TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  point_exp INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  waon_trans_type_code INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_slip_no INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  lid INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  service_name TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  card_trans_number_str TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  pay_id INTEGER");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  ic_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  old_ic_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  terminal_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  terminal_seq_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  terminal_id TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  edy_seq_no TEXT");
            database.execSQL("ALTER TABLE history_slip ADD COLUMN  input_kingaku INTEGER");
        }
    };
    //ADD-E BMT E.Oyama 2024/09/18 フタバ双方向向け改修

    public static LocalDatabase getInstance() {
        if (_localDatabase == null) {
            synchronized (LocalDatabase.class) {
                if (_localDatabase == null) {
                    Builder<LocalDatabase> builder = Room.databaseBuilder(
                                    MainApplication.getInstance().getApplicationContext(),
                                    LocalDatabase.class,
                                    "multi_payment_terminal")
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .addMigrations(MIGRATION_3_4)
                            .addMigrations(MIGRATION_4_5)
                            .addMigrations(MIGRATION_5_6)
                            .addMigrations(MIGRATION_6_7)
                            .addMigrations(MIGRATION_7_8)
                            .addMigrations(MIGRATION_8_9)
                            .addMigrations(MIGRATION_9_10)
                            //ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
                            .addMigrations(MIGRATION_10_11);
                            //ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修

                            // ！！ 注意 ！！
                            // マイグレーション対象のデータベースクラスをDemoDatabaseクラスでも定義している場合、
                            // DemoDatabaseクラスのgetInstance()にもマイグレーションを追加すること
                    if (BuildConfig.DEBUG) {
                        // マイグレーション失敗時、データベース再作成 ※DEBUGビルド時のみ
                        builder.fallbackToDestructiveMigration();
                    }
                    _localDatabase = builder.build();
                }
            }
        }
        return _localDatabase;
    }

    public static void closeDatabase() {
        if (_localDatabase != null) {
            if (_localDatabase.isOpen()) {
                deleteOldRecords();
                _localDatabase.close();
                //インスタンス初期化
                _localDatabase = null;
                Timber.d("Room Database closed");
            }
        }
    }

    private static void deleteOldRecords() {
        //トランザクション
        _localDatabase.runInTransaction(() -> {
            _localDatabase.uriDao().deletePosSentData(); //POS送信済の売上データが残っていれば削除 ダミーデータ用

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1); //アンテナ、GPSログは1ヶ月分まで保持
            _localDatabase.radioDao().deleteOldRecords(calendar.getTime());
            _localDatabase.gpsDao().deleteOldRecords(calendar.getTime());
            calendar.add(Calendar.MONTH, -1); //エラー、操作ログは2ヶ月分まで保持
            _localDatabase.errorDao().deleteOldRecords(calendar.getTime());
            _localDatabase.operationDao().deleteOldRecords(calendar.getTime());
        });
    }
}

