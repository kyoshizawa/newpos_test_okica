package jp.mcapps.android.multi_payment_terminal;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiConsumer;

import jp.mcapps.android.multi_payment_terminal.data.AdditionalSettingKeys;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.FirmWareInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.TabletLinkInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessKeyInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMasterInfo;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxRoundings;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
// import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketSearchResults;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.DynamicTicketItem;
import kotlin.text.UStringsKt;
import timber.log.Timber;

public class AppPreference {
    private static SharedPreferences p;
    private static Gson _gson = new Gson();

    public static void initialize(Context context) {

        p = PreferenceManager.getDefaultSharedPreferences(context);
        load();
    }

    public static void load() {
        /* setting_demo_mode */
        _isDemoMode = p.getBoolean(getKey(R.string.setting_key_demo_mode), getDefaultBoolean(R.bool.setting_default_demo_mode));
        /* setting_driverid_input */
        _isDriverCodeInput = p.getBoolean(getKey(R.string.setting_key_driverid_input), getDefaultBoolean(R.bool.setting_default_driverid_input));
        /* setting_driverid_history */
        _isDriverIdHistory = p.getBoolean(getKey(R.string.setting_key_driverid_history), getDefaultBoolean(R.bool.setting_default_driverid_history));
        /* confirm_sound_payment */
        _soundPaymentVolume = p.getInt(getKey(R.string.confirm_key_sound_payment), getDefaultInt(R.integer.confirm_default_sound_payment));
        /* confirm_sound_guidance */
        _soundGuidanceVolume = p.getInt(getKey(R.string.confirm_key_sound_guidance), getDefaultInt(R.integer.confirm_default_sound_guidance));
        /* confirm_brightness */
        _brightnessValue = p.getInt(getKey(R.string.confirm_key_brightness), getDefaultInt(R.integer.confirm_default_brightness));
        /* setting_antlog_enabled */
        _isAntlogEnabled = p.getBoolean(getKey(R.string.setting_key_antlog_enabled), getDefaultBoolean(R.bool.setting_default_antlog_enabled));
        /* setting_antlog_time */
        _antlogTime = p.getInt(getKey(R.string.setting_key_antlog_time), getDefaultInt(R.integer.setting_default_antlog_time));
        /* setting_gpslog_enabled */
        _isGpslogEnabled = p.getBoolean(getKey(R.string.setting_key_gpslog_enabled), getDefaultBoolean(R.bool.setting_default_gpslog_enabled));
        /* setting_gpslog_time */
        _gpslogTime = p.getInt(getKey(R.string.setting_key_gpslog_time), getDefaultInt(R.integer.setting_default_gpslog_time));
        /* setting_gpslog_distance */
        _gpslogDistance = p.getInt(getKey(R.string.setting_key_gpslog_distance), getDefaultInt(R.integer.setting_default_gpslog_distance));
        /* setting_input_1yen */
        _isInput1yen = p.getBoolean(getKey(R.string.setting_key_input_1yen), getDefaultBoolean(R.bool.setting_default_input_1yen));
        /* setting_withcash_1yen */
        _isWithcash1yen = p.getBoolean(getKey(R.string.setting_key_withcash_1yen), getDefaultBoolean(R.bool.setting_default_withcash_1yen));
        /* setting_jrem_activateid */
        _jremActivateId = p.getString(getKey(R.string.setting_key_jrem_activateid), "");
        /* setting_jrem_password */
        _jremPassword = p.getString(getKey(R.string.setting_key_jrem_password), "");
        /* setting_jrem_unique_id */
        _jremUniqueId = p.getString(getKey(R.string.setting_key_jrem_unique_id), "");
        /* setting_money_credit */
        _isMoneyCredit = p.getBoolean(getKey(R.string.setting_key_money_credit), getDefaultBoolean(R.bool.setting_default_money_credit));
        /* setting_pinless_enabled */
        _isPinLessEnabled = p.getBoolean(getKey(R.string.setting_key_pinless_enabled), getDefaultBoolean(R.bool.setting_default_pinless_enabled));
        /* setting_pinless_limit_fare */
        _PinLessLimitFare = p.getInt(getKey(R.string.setting_key_pinless_limit_fare), getDefaultInt(R.integer.setting_default_pinless_limit_fare));
        /* setting_money_contactless */
        _isMoneyContactless = p.getBoolean(getKey(R.string.setting_key_money_contactless), getDefaultBoolean(R.bool.setting_default_money_contactless));
        /* setting_money_unionpay */
        _isMoneyUnionpay = p.getBoolean(getKey(R.string.setting_key_money_unionpay), getDefaultBoolean(R.bool.setting_default_money_unionpay));
        /* setting_money_suica */
        _isMoneySuica = p.getBoolean(getKey(R.string.setting_key_money_suica), getDefaultBoolean(R.bool.setting_default_money_suica));
        /* setting_money_id */
        _isMoneyId = p.getBoolean(getKey(R.string.setting_key_money_id), getDefaultBoolean(R.bool.setting_default_money_id));
        /* setting_money_waon */
        _isMoneyWaon = p.getBoolean(getKey(R.string.setting_key_money_waon), getDefaultBoolean(R.bool.setting_default_money_waon));
        /* setting_money_nanaco */
        _isMoneyNanaco = p.getBoolean(getKey(R.string.setting_key_money_nanaco), getDefaultBoolean(R.bool.setting_default_money_nanaco));
        /* setting_money_edy */
        _isMoneyEdy = p.getBoolean(getKey(R.string.setting_key_money_edy), getDefaultBoolean(R.bool.setting_default_money_edy));
        /* setting_money_quiqpay */
        _isMoneyQuicpay = p.getBoolean(getKey(R.string.setting_key_money_quicpay), getDefaultBoolean(R.bool.setting_default_money_quicpay));
        /* setting_money_okica */
        _isMoneyOkica = p.getBoolean(getKey(R.string.setting_key_money_okica), getDefaultBoolean(R.bool.setting_default_money_okica));
        /* setting_money_qr */
        _isMoneyQr = p.getBoolean(getKey(R.string.setting_key_money_qr), getDefaultBoolean(R.bool.setting_default_money_qr));
        /* setting_prepaid */
        _isPrepaid = p.getBoolean(getKey(R.string.setting_key_prepaid), getDefaultBoolean(R.bool.setting_default_prepaid));
        /* setting_prepaid_domain */
        _prepaidServiceDomain = p.getString(getKey(R.string.setting_key_prepaid_domain), getKey(R.string.setting_default_prepaid_domain));
        /* setting_prepaid_Service_Key */
        _prepaidServiceKey = p.getString(getKey(R.string.setting_key_prepaid_key), getKey(R.string.setting_default_prepaid_key));
        /* setting_watari_point */
        _isWatariPoint = p.getBoolean(getKey(R.string.setting_key_watari_point), getDefaultBoolean(R.bool.setting_default_watari_point));
        /* setting_product_code */
        _productCode = p.getString(getKey(R.string.setting_key_product_code), getKey(R.string.setting_default_product_code));
        /* setting_merchant_name */
        _merchantName = p.getString(getKey(R.string.setting_key_merchant_name), getKey(R.string.setting_default_merchant_name));
        /* setting_merchant_office */
        _merchantOffice = p.getString(getKey(R.string.setting_key_merchant_office), getKey(R.string.setting_default_merchant_office));
        /* setting_merchant_telnumber */
        _merchantTelnumber = p.getString(getKey(R.string.setting_key_merchant_telnumber), getKey(R.string.setting_default_merchant_telnumber));
        /* setting_carid_input */
        _isCarIdInput = p.getBoolean(getKey(R.string.setting_key_carid_input), getDefaultBoolean(R.bool.setting_default_carid_input));
        /* setting_version */
        _settingVersion = p.getInt(getKey(R.string.setting_key_version), getDefaultInt(R.integer.setting_default_version));
        /* setting_screenlock_enabled */
        _isScreenlockEnabled = p.getBoolean(getKey(R.string.setting_key_screenlock_enabled), getDefaultBoolean(R.bool.setting_default_screenlock_enabled));
        /* setting_screenlock_password */
        _screenlockPassword = p.getString(getKey(R.string.setting_key_screenlock_password), getKey(R.string.setting_default_screenlock_password));
        /* setting_timeout_screen */
        _TimeoutScreen = p.getInt(getKey(R.string.setting_key_timeout_screen), getDefaultInt(R.integer.setting_default_timeout_screen));
        /* confirm_normal_shutdown */
        _confirmNormalShutdown = p.getBoolean("confirm_normal_shutdown", true);
        /* datetime_opening_suica */
        _datetimeOpeningSuica = p.getString("datetime_opening_suica", getKey(R.string.setting_default_opening_datetime));
        /* dadetime_opening_id */
        _datetimeOpeningId = p.getString("datetime_opening_id", getKey(R.string.setting_default_opening_datetime));
        /* dadetime_opening_waon */
        _datetimeOpeningWaon = p.getString("datetime_opening_waon", getKey(R.string.setting_default_opening_datetime));
        /* dadetime_opening_nanaco */
        _datetimeOpeningNanaco = p.getString("datetime_opening_nanaco", getKey(R.string.setting_default_opening_datetime));
        /* dadetime_opening_quicpay */
        _datetimeOpeningQuicpay = p.getString("datetime_opening_quicpay", getKey(R.string.setting_default_opening_datetime));
        /* dadetime_opening_edy */
        _datetimeOpeningEdy = p.getString("datetime_opening_edy", getKey(R.string.setting_default_opening_datetime));
        /* datetime_authentication_mc */
        _datetimeAuthenticationMc = p.getString("datetime_authentication_mc", getKey(R.string.setting_default_opening_datetime));
        /* datetime_authentication_qr */
        _datetimeAuthenticationQr = p.getString("datetime_authentication_qr", getKey(R.string.setting_default_opening_datetime));
        /* setting_aggregate_detail */
        _isAggregateDetail = p.getBoolean(getKey(R.string.setting_aggregate_detail), getDefaultBoolean(R.bool.setting_default_aggregate_detail));
        /* setting_organization_id */
        _organizationId = p.getString(getKey(R.string.setting_key_organization_id), null);
        /* setting_mc_driverid */
        _mcDriverId = p.getInt(getKey(R.string.setting_key_mc_driverid), getDefaultInt(R.integer.setting_default_mc_driverid));
        /* setting_mc_carid */
        _mcCarId = p.getInt(getKey(R.string.setting_key_mc_carid), getDefaultInt(R.integer.setting_default_mc_carid));
        /* setting_mc_termid */
        _mcTermId = p.getString(getKey(R.string.setting_key_mc_termid), getKey(R.string.setting_default_mc_termid));
        /* setting_qr_userid */
        _qrUserId = p.getString(getKey(R.string.setting_key_qr_userid), getKey(R.string.setting_default_qr_userid));
        /* setting_qr_password */
        _qrPassword = p.getString(getKey(R.string.setting_key_qr_password), getKey(R.string.setting_default_qr_password));
        /* term_sequence */
        _termSequence = p.getInt("term_sequence", 0);
        /* slip_no_watari */
        _slipNoWatari = p.getInt("slip_no_watari", 0);
        /* setting_developer_mode */
        _isDeveloperMode = p.getBoolean(getKey(R.string.setting_key_developer_mode), getDefaultBoolean(R.bool.setting_default_developer_mode));
        /* driver_code */
        _driverCode = p.getString("driver_code", String.valueOf(getDefaultInt(R.integer.setting_default_mc_driverid)));
        /* driver_name */
        _driverName = p.getString("driver_name", "");
        /* gmo_header_time_key */
        _gmoHeaderTime = p.getString("gmo_header_time_key", null);
        /* gmo_header_nonce_str_key */
        _gmoHeaderNonceStr = p.getString("gmo_header_nonce_str_key", null);
        /* gmo_header_sign_key */
        _gmoHeaderSign = p.getString("gmo_header_sign_key", null);
        /* ifbox_firmware_info */
        _ifBoxOTAInfo = parseJson("ifbox_firmware_info", FirmWareInfo.class);
        /* ifbox_version_info */
        _ifBoxVersionInfo = parseJson("ifbox_version_info", Version.Response.class);
        /* setting_screen_lock_sec */
        _screenLockSec = p.getInt(getKey(R.string.setting_key_screen_lock_sec), getDefaultInt(R.integer.setting_default_screen_lock_sec));
        /* term_sequence */
        _validationCheckTermSequence = p.getInt("validation_check_term_sequence", 0);
        _tabletLinkInfo = parseJson("tablet_link_info", TabletLinkInfo.class);
        _tabletVersionInfo = parseJson("tablet_version_info", jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version.Response.class);
        /* datetime_lt27_printable */
        _datetimeLt27Printable = p.getString("datetime_lt27_printable", getKey(R.string.setting_default_opening_datetime));
        _ifBoxServiceInfo = p.getString("ifbox_service_info", null);
        _okicaAccessToken = p.getString(getKey(R.string.setting_key_okica_access_token), null);
        _okicaAuthCode = p.getString(getKey(R.string.setting_key_okica_auth_code), null);
        _okicaTerminalInfo = parseJson(getKey(R.string.setting_key_okica_terminal_info), jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo.Response.class);
        _okicaICMasterInfo = parseJson(getKey(R.string.setting_key_okica_ic_master_info), ICMasterInfo.class);
        _okicaAccessKeyInfo = parseJson(getKey(R.string.setting_key_okica_access_key_info), AccessKeyInfo.class);
        /* okica_Nega_Datetime */
        _okicaNegaDatetime = p.getString("setting_okica_nega_dadetime", null);
        /* setting_service_pos */
        _isServicePos = p.getBoolean(getKey(R.string.setting_key_service_pos), getDefaultBoolean(R.bool.setting_default_service_pos));
        _servicePosAccessToken = p.getString(getKey(R.string.setting_key_service_pos_access_token), null);
        _servicePosRefreshToken = p.getString(getKey(R.string.setting_key_service_pos_refresh_token), null);
        /* invoice_no */
        _invoiceNo = p.getString(getKey(R.string.setting_key_invoice_no), "");
        _receiptTax = p.getString(getKey(R.string.setting_key_receiptTax), "0");
        /* 取引先コード */
        _supplierCd = p.getString((getKey(R.string.setting_key_supplier_cd)), null);
        /* 利用可能な上限金額タイプ */
        _maxAmountType = p.getInt((getKey(R.string.setting_key_max_amount_type)), 0);
        /* 標準税率 */
        _standard_tax_rate = p.getInt((getKey(R.string.setting_key_standard_tax_rate)), 0);
        /* 軽減税率 */
        _reduced_tax_rate = p.getInt((getKey(R.string.setting_key_reduced_tax_rate)), 0);
        /* 消費税の端数処理 */
        _taxRounding = p.getInt((getKey(R.string.setting_key_tax_rounding)), 0);
        /* 住所 */
        _address = p.getString((getKey(R.string.setting_key_address)), "");
        /* 加盟店控えの伝票タイトル */
        _slipTitle = p.getString((getKey(R.string.setting_key_slip_title)), "");
        /* レシート控え印字枚数 */
        _receiptCount = p.getInt((getKey(R.string.setting_key_receipt_count)), 0);
        /* 商品カテゴリ表示 */
        _isProductCategory = p.getBoolean((getKey(R.string.setting_key_is_product_category)), getDefaultBoolean(R.bool.setting_default_is_product_category));
        /* 領収書発行 */
        _isPosReceipt = p.getBoolean((getKey(R.string.setting_key_is_pos_receipt)), getDefaultBoolean(R.bool.setting_default_is_pos_receipt));
        /* 金額手入力 */
        _isManualAmount = p.getBoolean((getKey(R.string.setting_key_is_manual_amount)), getDefaultBoolean(R.bool.setting_default_is_manual_amount));
        /* setting_service_ticket */
        _isServiceTicket = p.getBoolean(getKey(R.string.setting_key_service_ticket), getDefaultBoolean(R.bool.setting_default_service_ticket));
        _serviceTicketAccessToken = p.getString(getKey(R.string.setting_key_service_ticket_access_token), null);
        _serviceTicketRefreshToken = p.getString(getKey(R.string.setting_key_service_ticket_refresh_token), null);

        // POS機能で利用する店舗情報（店舗情報を取得した際、posTenantSaveで保存するデータ）
        _posMerchantName = p.getString(getKey(R.string.setting_key_pos_merchant_name), null);
        _posMerchantOffice = p.getString(getKey(R.string.setting_key_pos_merchant_office), null);
        _posMerchantTelnumber = p.getString(getKey(R.string.setting_key_pos_merchant_telnumber), null);
        _posAddress = p.getString(getKey(R.string.setting_key_pos_address), null);
        _isFixedAmountPostalOrder = p.getBoolean(getKey(R.string.setting_key_is_fixed_amount_postal_order), getDefaultBoolean(R.bool.setting_default_is_fixed_amount_postal_order)); // 郵便小為替対応

        /* 自動つり銭機連動 */
        _isCashChanger = p.getBoolean(getKey(R.string.setting_key_cashchanger), getDefaultBoolean(R.bool.setting_default_cashchanger));

        /* キャッシュドロア連動 */
        _cashDrawerType = p.getInt(getKey(R.string.setting_key_cashdrawer_type), getDefaultInt(R.integer.setting_default_cashdrawer_type));

        //※内部保持データ一覧更新してから、追加してください
        /*  */
    }

    public static void save(TerminalInfo.Response info) {
        Timber.d("save shared preferences");

        final SharedPreferences.Editor e = p.edit();
        // 端末番号
        if (info.terminalNo != null) {
            e.putString(getKey(R.string.setting_key_mc_termid), info.terminalNo);
            _mcTermId = info.terminalNo;
        }

        // 乗務員コード入力可能か
        if (info.isInputDriverCd != null) {
            e.putBoolean(getKey(R.string.setting_key_driverid_input), info.isInputDriverCd);
            _isDriverCodeInput = info.isInputDriverCd;
        }

        // 固定乗務員コード
        if (info.fixedDriverCd != null) {
            e.putInt(getKey(R.string.setting_key_mc_driverid), info.fixedDriverCd);
            _mcDriverId = info.fixedDriverCd;
        }

        // 号機番号（車番）
        if (info.carNo != null) {
            e.putInt(getKey(R.string.setting_key_mc_carid), info.carNo);
            _mcCarId = info.carNo;
        }

        // 乗務員コード履歴閲覧フラグ
        if (info.isHistoryDriverCd != null) {
            e.putBoolean(getKey(R.string.setting_key_driverid_history), info.isHistoryDriverCd);
            _isDriverIdHistory = info.isHistoryDriverCd;
        }

        // アンテナログ利用フラグ
        if (info.isAntennaLog != null) {
            e.putBoolean(getKey(R.string.setting_key_antlog_enabled), info.isAntennaLog);
            _isAntlogEnabled = info.isAntennaLog;
        }

        // アンテナログ間隔（秒）
        if (info.antennaLogIntervalSeconds != null) {
            e.putInt(getKey(R.string.setting_key_antlog_time), info.antennaLogIntervalSeconds);
            _antlogTime = info.antennaLogIntervalSeconds;
        }

        // GPSログ利用フラグ
        if (info.isGpsLog != null) {
            e.putBoolean(getKey(R.string.setting_key_gpslog_enabled), info.isGpsLog);
            _isGpslogEnabled = info.isGpsLog;
        }

        // GPSログ間隔（秒）
        if (info.gpsLogIntervalSeconds != null) {
            e.putInt(getKey(R.string.setting_key_gpslog_time), info.gpsLogIntervalSeconds);
            _gpslogTime = info.gpsLogIntervalSeconds;
        }

        // GPSログ間隔（ｍ）
        if (info.gpsLogIntervalMeter != null) {
            e.putInt(getKey(R.string.setting_key_gpslog_distance), info.gpsLogIntervalMeter);
            _gpslogDistance = info.gpsLogIntervalMeter;
        }

        // 1円入力可能フラグ
        if (info.isFareUnit1Yen != null) {
            e.putBoolean(getKey(R.string.setting_key_input_1yen), info.isFareUnit1Yen);
            _isInput1yen = info.isFareUnit1Yen;
        }

        // 現金併用1円設定フラグ
        if (info.isCashTogetherFareUnit1Yen != null) {
            e.putBoolean(getKey(R.string.setting_key_withcash_1yen), info.isCashTogetherFareUnit1Yen);
            _isWithcash1yen = info.isCashTogetherFareUnit1Yen;
        }

        // アクティベートID
        if (info.activateId != null) {
            e.putString(getKey(R.string.setting_key_jrem_activateid), info.activateId);
            _jremActivateId = info.activateId;
        }
        jremActivateIdcheck();

        // パスワード
        if (info.password != null) {
            e.putString(getKey(R.string.setting_key_jrem_password), info.password);
            _jremPassword = info.password;
        }

        // クレジット利用フラグ
        if (info.isCredit != null) {
            e.putBoolean(getKey(R.string.setting_key_money_credit), info.isCredit);
            _isMoneyCredit = info.isCredit;
        }

        // 非接触IC利用フラグ
        if (info.isContactless != null) {
            e.putBoolean(getKey(R.string.setting_key_money_contactless), info.isContactless);
            _isMoneyContactless = info.isContactless;
        }

        // 銀聯利用フラグ
        if (info.isUnionPay != null) {
            //e.putBoolean(getKey(R.string.setting_key_money_unionpay), info.isUnionPay);
            //_isMoneyUnionpay = info.isUnionPay;
            //TODO:銀聯対応まではセンター設定に関係なくfalseとする
            e.putBoolean(getKey(R.string.setting_key_money_unionpay), false);
            _isMoneyUnionpay = false;
        }

        // 交通系利用フラグ
        if (info.isJr != null) {
            e.putBoolean(getKey(R.string.setting_key_money_suica), info.isJr);
            _isMoneySuica = info.isJr;
        }

        // iD利用フラグ
        if (info.isId != null) {
            e.putBoolean(getKey(R.string.setting_key_money_id), info.isId);
            _isMoneyId = info.isId;
        }

        // WAON利用フラグ
        if (info.isWaon != null) {
            e.putBoolean(getKey(R.string.setting_key_money_waon), info.isWaon);
            _isMoneyWaon = info.isWaon;
        }

        // nanaco利用フラグ
        if (info.isNanaco != null) {
            e.putBoolean(getKey(R.string.setting_key_money_nanaco), info.isNanaco);
            _isMoneyNanaco = info.isNanaco;
        }

        // Edy利用フラグ
        if (info.isEdy != null) {
            e.putBoolean(getKey(R.string.setting_key_money_edy), info.isEdy);
            _isMoneyEdy = info.isEdy;
        }

        // QuicPay利用フラグ
        if (info.isQuicPay != null) {
            e.putBoolean(getKey(R.string.setting_key_money_quicpay), info.isQuicPay);
            _isMoneyQuicpay = info.isQuicPay;
        }

        // OKICA利用フラグ
        // 実験用に常に True
        _isMoneyOkica = true;
//        if (info.isOkica != null) {
//
//            if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D)) {
//                /* ヤザキLT27双方向連動ではOKICA未対応のため、OKICA利用フラグを無効にする */
//                e.putBoolean(getKey(R.string.setting_key_money_okica), false);
//                _isMoneyOkica = false;
//            } else {
//                e.putBoolean(getKey(R.string.setting_key_money_okica), info.isOkica);
//                if (_isMoneyOkica == true && info.isOkica == false) {
//                    // OKICA利用フラグON時にOFFを受信した場合
//                    Timber.i("OKICA利用フラグON時にOFFを受信");
//                    OkicaMasterControl.force_okica_off = true;
//                } else {
//                    OkicaMasterControl.force_okica_off = false;
//                    _isMoneyOkica = info.isOkica;
//                }
//            }
//        }

        // QR利用フラグ
        if (info.isQr != null) {
            e.putBoolean(getKey(R.string.setting_key_money_qr), info.isQr);
            _isMoneyQr = info.isQr;
        }

        // Watari利用フラグ
        if (info.isWatariPoint != null) {
            e.putBoolean(getKey(R.string.setting_key_watari_point), info.isWatariPoint);
            _isWatariPoint = info.isWatariPoint;
        }

        // 郵便小為替利用フラグ
        if (info.posServiceFunc.isFixedAmountPostalOrder != null) {
            e.putBoolean(getKey(R.string.setting_key_is_fixed_amount_postal_order), info.posServiceFunc.isFixedAmountPostalOrder);
            _isFixedAmountPostalOrder = info.posServiceFunc.isFixedAmountPostalOrder;
        }

        // 商品区分コード
        if (info.productCd != null) {
            e.putString(getKey(R.string.setting_key_product_code), info.productCd);
            _productCode = info.productCd;
        }

        // 会社名
        if (info.branchOfficeName != null) {
            e.putString(getKey(R.string.setting_key_merchant_name), info.branchOfficeName);
            _merchantName = info.branchOfficeName;
        }

        // 営業所名
        if (info.salesOfficeName != null) {
            e.putString(getKey(R.string.setting_key_merchant_office), info.salesOfficeName);
            _merchantOffice = info.salesOfficeName;
        }

        // 電話番号
        if (info.telNumber != null) {
            e.putString(getKey(R.string.setting_key_merchant_telnumber), info.telNumber);
            _merchantTelnumber = info.telNumber;
        }

        // 号機番号（車番）入力可能フラグ
        if (info.isInputCarNo != null) {
            e.putBoolean(getKey(R.string.setting_key_carid_input), info.isInputCarNo).apply();
            _isCarIdInput = info.isInputCarNo;
        }

        // 画面ロック利用フラグ
        if (info.isScreenLock != null) {
            e.putBoolean(getKey(R.string.setting_key_screenlock_enabled), info.isScreenLock);
            _isScreenlockEnabled = info.isScreenLock;
        }

        // 画面ロックパスワード
        if (info.screenPass != null) {
            e.putString(getKey(R.string.setting_key_screenlock_password), info.screenPass);
            _screenlockPassword = info.screenPass;
        }

        // 画面オフ時間
        if (info.screenTimeUpSec != null) {
            e.putInt(getKey(R.string.setting_key_timeout_screen), info.screenTimeUpSec);
            _TimeoutScreen = info.screenTimeUpSec;
        }

        // 画面ロック時間
        if (info.screenLockSec != null) {
            e.putInt(getKey(R.string.setting_key_screen_lock_sec), info.screenLockSec);
            _screenLockSec = info.screenLockSec;
        }

        // 集計明細印刷フラグ
        if (info.isDayTotalDetail != null) {
            e.putBoolean(getKey(R.string.setting_aggregate_detail), info.isDayTotalDetail);
            _isAggregateDetail = info.isDayTotalDetail;
        }

        // 顧客コード
        if (info.customerCd != null) {
            e.putString(getKey(R.string.setting_key_organization_id), info.customerCd);
            _organizationId = info.customerCd;
        }

        // QR決済認証用の端末ID
        if (info.qrUserId != null) {
            e.putString(getKey(R.string.setting_key_qr_userid), info.qrUserId);
            _qrUserId = info.qrUserId;
        }

        // QR決済認証用のパスワード
        if (info.qrPassword != null) {
            e.putString(getKey(R.string.setting_key_qr_password), info.qrPassword);
            _qrPassword = info.qrPassword;
        }

        // 開発者モード
        if (info.isDeveloperMode != null) {
            e.putBoolean(getKey(R.string.setting_key_developer_mode), info.isDeveloperMode);
            _isDeveloperMode = info.isDeveloperMode;
        }
        /*インボイス対応*/
        // インボイス番号
        if (info.invoiceNo != null) {
            e.putString(getKey(R.string.setting_key_invoice_no), info.invoiceNo);
            _invoiceNo = info.invoiceNo;
        }

        /*インボイス対応*/
         // レシート税率
        if (info.receiptTax != null) {
            Float tmpReceiptTax = info.receiptTax*100;
            boolean decimalCheck = tmpReceiptTax % 1 != 0;  //true:小数点以下の値有り  false:小数点以下の値無し
            String decimalPoint;
            //小数点以下の値あり
            if (decimalCheck == true){
                decimalPoint = String.valueOf(Math.floor(tmpReceiptTax * 1000) / 1000);
                e.putString(getKey(R.string.setting_key_receiptTax), decimalPoint);
                _receiptTax = decimalPoint;

            //小数点以下の値なし(切り捨て）
            } else {
                decimalPoint = String.valueOf(tmpReceiptTax);
                decimalPoint = decimalPoint.substring(0, decimalPoint.length()-2);
                e.putString(getKey(R.string.setting_key_receiptTax), decimalPoint);
                _receiptTax = decimalPoint;

            }
        }

        // 取引先コード
        if (info.supplierCd != null) {
            e.putString(getKey(R.string.setting_key_supplier_cd), info.supplierCd);
            _supplierCd = info.supplierCd;
        }

        // 利用可能な取引金額上限タイプ
        if (info.maxAmountType != null) {
            e.putInt(getKey(R.string.setting_key_max_amount_type), info.maxAmountType);
            _maxAmountType = info.maxAmountType;
        }

        if(info.posServiceFunc != null) {
            // 消費税の端数処理
            if (info.posServiceFunc.taxRounding != null) {
                e.putInt(getKey(R.string.setting_key_tax_rounding), info.posServiceFunc.taxRounding);
                _taxRounding = info.posServiceFunc.taxRounding;
            }

            // 税率
            if (info.posServiceFunc.taxList != null) {
                for(int i=0; i< info.posServiceFunc.taxList.length; i++){
                    int tax = (int)(info.posServiceFunc.taxList[i].tax * 100);
                    if(info.posServiceFunc.taxList[i].taxClass == 0){
                        // 0:標準税率
                        e.putInt(getKey(R.string.setting_key_standard_tax_rate), tax);
                        _standard_tax_rate = tax;
                    }
                    else if (info.posServiceFunc.taxList[i].taxClass == 1){
                        // 1:軽減税率
                        e.putInt(getKey(R.string.setting_key_reduced_tax_rate), tax);
                        _reduced_tax_rate = tax;
                    }
                }
            }

            // 加盟店控えの伝票タイトル
            if (info.posServiceFunc.slipTitle != null) {
                e.putString(getKey(R.string.setting_key_slip_title), info.posServiceFunc.slipTitle);
                _slipTitle = info.posServiceFunc.slipTitle;
            }

            // レシート控え印字枚数
            if (info.posServiceFunc.receiptCounts != null) {
                e.putInt(getKey(R.string.setting_key_receipt_count), info.posServiceFunc.receiptCounts);
                _receiptCount = info.posServiceFunc.receiptCounts;
            }

            // 商品カテゴリ表示
            if (info.posServiceFunc.isProductCategory != null) {
                e.putBoolean(getKey(R.string.setting_key_is_product_category), info.posServiceFunc.isProductCategory);
                _isProductCategory = info.posServiceFunc.isProductCategory;
            }

            // 領収書発行
            if (info.posServiceFunc.isPosReceipt != null) {
                e.putBoolean(getKey(R.string.setting_key_is_pos_receipt), info.posServiceFunc.isPosReceipt);
                _isPosReceipt = info.posServiceFunc.isPosReceipt;
            }

            // 金額手入力
            if (info.posServiceFunc.isManualAmount != null) {
                e.putBoolean(getKey(R.string.setting_key_is_manual_amount), info.posServiceFunc.isManualAmount);
                _isManualAmount = info.posServiceFunc.isManualAmount;
            }
        }

        // プリペイドフラグ
        if (info.isPrepaid != null) {
            e.putBoolean(getKey(R.string.setting_key_prepaid), info.isPrepaid);
            _isPrepaid = info.isPrepaid;

            // プリペイドサービスドメイン
            if (info.prepaidServiceDomain != null) {
                e.putString(getKey(R.string.setting_key_prepaid_domain), info.prepaidServiceDomain);
                _prepaidServiceDomain = info.prepaidServiceDomain;
            }

            // プリペイドサービス認証キー
            if (info.prepaidServiceKey != null) {
                e.putString(getKey(R.string.setting_key_prepaid_key), info.prepaidServiceKey);
                _prepaidServiceKey = info.prepaidServiceKey;
            }
        }

        // 端末拡張設定
        if (info.additionalSettings != null) {
            for (int i = 0; i < info.additionalSettings.length; i++) {
                String paramName = info.additionalSettings[i].getParamName();
                String value = info.additionalSettings[i].getValue();

                if (paramName == null || value == null) continue;

                BiConsumer<SharedPreferences.Editor, String> handler = settingHandlers.get(paramName);
                if (handler != null) {
                    handler.accept(e, value);
                } else {
                    Timber.w("未定義の端末拡張設定です: %s = %s", paramName, value);
                }
            }
        }

        e.apply();
    }

    // 端末拡張設定の保存処理
    private static final Map<String, BiConsumer<SharedPreferences.Editor, String>> settingHandlers = new HashMap<>();
    static {
        settingHandlers.put(AdditionalSettingKeys.EMV_PINLESS_ENABLE, (e, value) -> {
            if (Boolean.parseBoolean(value)) {
                e.putBoolean(getKey(R.string.setting_key_pinless_enabled), true);
                _isPinLessEnabled = true;
            } else {
                e.putBoolean(getKey(R.string.setting_key_pinless_enabled), false);
                _isPinLessEnabled = false;
            }
        });

        settingHandlers.put(AdditionalSettingKeys.EMV_PINLESS_LIMIT_FARE, (e, value) -> {
            e.putInt(getKey(R.string.setting_key_pinless_limit_fare), Integer.parseInt(value));
            _PinLessLimitFare = Integer.parseInt(value);
        });
    }
    public static void posTenantSave(TenantData tenantData) {
        Timber.d("posTenantSave shared preferences");

        final SharedPreferences.Editor e = p.edit();

        if (tenantData.parent_name != null) {
            // 会社名（加盟店名/店舗・営業所）
            if (tenantData.parent_name != null) {
                e.putString(getKey(R.string.setting_key_pos_merchant_name), tenantData.parent_name);
                _posMerchantName = tenantData.parent_name;
            }

            // 営業所名（加盟店名/店舗・営業所）
            if (tenantData.name != null) {
                e.putString(getKey(R.string.setting_key_pos_merchant_office), tenantData.name);
                _posMerchantOffice = tenantData.name;
            }
        } else {
            // 会社名（加盟店名/店舗・営業所）
            if (tenantData.name != null) {
                e.putString(getKey(R.string.setting_key_pos_merchant_name), tenantData.name);
                _posMerchantName = tenantData.name;
            }

            // 営業所名（加盟店名/店舗・営業所）
            // parent_nameがnullの場合、nameを_posMerchantNameに設定して、_posMerchantOfficeは未設定
        }


        // 電話番号
        if (tenantData.phone_number != null) {
            e.putString(getKey(R.string.setting_key_pos_merchant_telnumber), tenantData.phone_number);
            _posMerchantTelnumber = tenantData.phone_number;
        }

        // 住所
        if (tenantData.address_line1 != null) {
            String addressFull = tenantData.address_line1 + tenantData.address_line2 + tenantData.address_line3;
            e.putString(getKey(R.string.setting_key_pos_address), addressFull);
            _posAddress = addressFull;
        }

        e.apply();
    }

    public static List<String[]> getTerminalInfo() {

        List<String[]> list = new ArrayList<>();

        list.add(new String[]{"端末番号", String.valueOf(_mcTermId)});
        list.add(new String[]{"係員番号入力可能フラグ", String.valueOf(_isDriverCodeInput)});
        list.add(new String[]{"固定係員番号", String.valueOf(_mcDriverId)});
        list.add(new String[]{"号機番号", String.valueOf(_mcCarId)});
        list.add(new String[]{"係員番号履歴閲覧フラグ", String.valueOf(_isDriverIdHistory)});
        list.add(new String[]{"アンテナログ利用フラグ", String.valueOf(_isAntlogEnabled)});
        list.add(new String[]{"アンテナログ間隔（秒）", String.valueOf(_antlogTime)});
        list.add(new String[]{"GPSログ利用フラグ", String.valueOf(_isGpslogEnabled)});
        list.add(new String[]{"GPSログ間隔（秒）", String.valueOf(_gpslogTime)});
        list.add(new String[]{"GPSログ間隔（m）", String.valueOf(_gpslogDistance)});
        list.add(new String[]{"1円入力可能フラグ", String.valueOf(_isInput1yen)});
        list.add(new String[]{"現金併用1円設定フラグ", String.valueOf(_isWithcash1yen)});
        list.add(new String[]{"電マネアクティベートID", String.valueOf(_jremActivateId)});
        list.add(new String[]{"電マネパスワード", String.valueOf(_jremPassword)});
        list.add(new String[]{"クレジット利用フラグ", String.valueOf(_isMoneyCredit)});
        list.add(new String[]{"PINレス機能", String.valueOf(_isPinLessEnabled)});
        list.add(new String[]{"PINレス金額", String.valueOf(_PinLessLimitFare)});
        list.add(new String[]{"非接触IC利用フラグ", String.valueOf(_isMoneyContactless)});
        list.add(new String[]{"銀聯利用フラグ", String.valueOf(_isMoneyUnionpay)});
        list.add(new String[]{"交通系利用フラグ", String.valueOf(_isMoneySuica)});
        list.add(new String[]{"iD利用フラグ", String.valueOf(_isMoneyId)});
        list.add(new String[]{"WAON利用フラグ", String.valueOf(_isMoneyWaon)});
        list.add(new String[]{"nanaco利用フラグ", String.valueOf(_isMoneyNanaco)});
        list.add(new String[]{"Edy利用フラグ", String.valueOf(_isMoneyEdy)});
        list.add(new String[]{"QUICPay利用フラグ", String.valueOf(_isMoneyQuicpay)});
        list.add(new String[]{"OKICA利用フラグ", String.valueOf(_isMoneyOkica)});
        list.add(new String[]{"QR利用フラグ", String.valueOf(_isMoneyQr)});
        list.add(new String[]{"和多利利用フラグ", String.valueOf(_isWatariPoint)});
        if(_isServicePos){
            list.add(new String[]{"為替類利用フラグ", String.valueOf(_isFixedAmountPostalOrder)});
        }
        list.add(new String[]{"商品区分コード", String.valueOf(_productCode)});
        if(_isServicePos){
            // POSアクティベート状態の時、決済システム側のデータではなくPOSのデータを表示する
            list.add(new String[]{"会社名", _posMerchantName != null ? String.valueOf(_posMerchantName) : "―" });
            list.add(new String[]{"営業所名", _posMerchantOffice != null ? String.valueOf(_posMerchantOffice) : "―" });
            list.add(new String[]{"住所", _posAddress != null ? String.valueOf(_posAddress) : "―" });
            list.add(new String[]{"電話番号", _posMerchantTelnumber != null ? String.valueOf(_posMerchantTelnumber) : "―" });
        }
        else {
            list.add(new String[]{"会社名", String.valueOf(_merchantName)});
            list.add(new String[]{"営業所名", String.valueOf(_merchantOffice)});
            list.add(new String[]{"電話番号", String.valueOf(_merchantTelnumber)});
        }
        list.add(new String[]{"号機番号入力可能フラグ", String.valueOf(_isCarIdInput)});
        list.add(new String[]{"画面ロック利用フラグ", String.valueOf(_isScreenlockEnabled)});
        list.add(new String[]{"画面ロックパスワード", String.valueOf(_screenlockPassword)});
        list.add(new String[]{"画面オフ時間（秒）", String.valueOf(_TimeoutScreen)});
        list.add(new String[]{"画面ロック時間（秒）", String.valueOf(_screenLockSec)});
        list.add(new String[]{"集計明細印刷フラグ", String.valueOf(_isAggregateDetail)});
        list.add(new String[]{"顧客コード", String.valueOf(_organizationId)});
        list.add(new String[]{"QRユーザID", String.valueOf(_qrUserId)});
        list.add(new String[]{"QRユーザパスワード", String.valueOf(_qrPassword)});
        if (_isMoneyOkica) {
            list.add(new String[]{"OKICAアクセスキー取得日時", _okicaAccessKeyInfo != null ? getCheckData(_okicaAccessKeyInfo.checkDate) : "―"});
            list.add(new String[]{"OKICAアクセスキーVer.", _okicaAccessKeyInfo != null ? String.valueOf(_okicaAccessKeyInfo.version) : "―"});
            list.add(new String[]{"OKICAマスタデータ取得日時", _okicaICMasterInfo != null ? getCheckData(_okicaICMasterInfo.checkDate) : "―"});
            list.add(new String[]{"OKICAマスタデータVer.", _okicaICMasterInfo != null ? String.valueOf(_okicaICMasterInfo.version) : "―"});
            list.add(new String[]{"OKICAネガ取得日時", _okicaNegaDatetime != null ? getOkicaNegaDatetime() : "―"});
        }
        String standardTax = _standard_tax_rate != null ? String.format("%3d", _standard_tax_rate) + "％" : "―";
        String reducedTax = _reduced_tax_rate != null ? String.format("%3d", _reduced_tax_rate) + "％" : "―";
        list.add(new String[]{"税率", "標準税率：" + standardTax + "\n軽減税率：" + reducedTax });
        list.add(new String[]{"消費税の端数処理", _taxRounding != null ? TaxRoundingToString(_taxRounding) : "―" });
        list.add(new String[]{"インボイス番号", _invoiceNo != null && !_invoiceNo.equals("") ? String.valueOf(_invoiceNo) : "―" });      /*インボイス対応*/
        list.add(new String[] {"消費税率(%)", _receiptTax != null && !_receiptTax.equals("0") ? String.valueOf(_receiptTax): "―" });    /*インボイス対応*/
        list.add(new String[]{"店舗の取引先コード", _supplierCd != null ? String.valueOf(_supplierCd) : "―" });
        String maxAmountType = _maxAmountType != null ? MaxAmountTypeToString(_maxAmountType) : "―";
        list.add(new String[]{"利用可能な上限金額タイプ", maxAmountType});
        if (_isServicePos) {
            list.add(new String[]{"加盟店控えの伝票タイトル", _slipTitle != null ? String.valueOf(_slipTitle): "―" });
            list.add(new String[]{"レシート控え印字枚数", _receiptCount != null ? String.valueOf(_receiptCount): "―" });
            list.add(new String[]{"商品カテゴリ表示", BooleanToStringOnOff(_isProductCategory)});
            list.add(new String[]{"領収書発行", BooleanToStringOnOff(_isPosReceipt)});
//            list.add(new String[]{"金額手入力", BooleanToStringOnOff(_isManualAmount)});
        }
        list.add(new String[]{"開発者モードフラグ", String.valueOf(_isDeveloperMode)});

        return list;
    }

    private static String getCheckData(long checkDate) {
        final Calendar calLimit = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        calLimit.setTimeZone(tz);
        calLimit.setTimeInMillis(checkDate);

        int year = calLimit.get(Calendar.YEAR);
        int month = calLimit.get(Calendar.MONTH) + 1;
        int date = calLimit.get(Calendar.DATE);
        int hour = calLimit.get(Calendar.HOUR_OF_DAY);
        int minute = calLimit.get(Calendar.MINUTE);
        int second = calLimit.get(Calendar.SECOND);

        return String.format("%04d/%02d/%02d　%02d:%02d:%02d", year, month, date, hour, minute, second);
    }

    private static String getKey(int id) {
        return MainApplication.getInstance().getString(id);
    }

    private static boolean getDefaultBoolean(int id) {
        return MainApplication.getInstance().getResources().getBoolean(id);
    }

    private static int getDefaultInt(int id) {
        return MainApplication.getInstance().getResources().getInteger(id);
    }

    private static <T> T parseJson(String key, Class<T> clsT) {
        return parseJson(key, clsT, null);
    }

    private static <T> T parseJson(String key, Class<T> clsT, Object defaultValue) {
        try {
            return _gson.fromJson(p.getString(key, null), clsT);
        } catch (Exception e) {
            try {
                return clsT.cast(defaultValue);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static ArrayList<Long> _prepaidSlipIds = new ArrayList<>();
    public static ArrayList<Long> getPrepaidSlipId() {
        return _prepaidSlipIds;
    }

    public static void setPrepaidSlipId(ArrayList<Long> slipIds) {
        _prepaidSlipIds = slipIds;
    }

    private static boolean _isDemoMode;

    public static boolean isDemoMode() {
        return _isDemoMode;
    }

    private static boolean _isDriverCodeInput;

    public static boolean isDriverCodeInput() {
        return _isDriverCodeInput;
    }

    private static String _driverCode;

    public static String getDriverCode() {
        if (AppPreference.isDemoMode()) {
            return String.valueOf(_mcDriverId);
        } else {
            return _driverCode;
        }
    }

    public static void setDriverCode(String driverCode) {
        _driverCode = driverCode;
        p.edit().putString("driver_code", driverCode).apply();
    }

    private static String _driverName;

    public static String getDriverName() {
        if (AppPreference.isDemoMode()) {
            return getKey(R.string.demo_driver_name);
        } else {
            return _driverName;
        }
    }

    public static void setDriverName(String driverName) {
        _driverName = driverName;
        p.edit().putString("driver_name", driverName).apply();
    }

    private static boolean _isCarIdInput;

    public static boolean isCarIdInput() {
        return _isCarIdInput;
    }

    private static boolean _isDriverIdHistory;

    public static boolean isDriverIdHistory() {
        return _isDriverIdHistory;
    }

    private static boolean _isMoneyContactless;

    public static boolean isMoneyContactless() {
        if (AppPreference.isDemoMode()) {
            return true;
        }
        return _isMoneyContactless;
    }

    private static boolean _isMoneyCredit;

    public static boolean isMoneyCredit() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyCredit;
        }
    }

    private static boolean _isPinLessEnabled;
    public static boolean isPinLessEnabled() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isPinLessEnabled;
        }
    }

    private static int _PinLessLimitFare;
    public static int getPinLessLimitFare() {
        if (isDemoMode()) {
            return 15000; // デモモードでは15,000円を返す
        } else {
            return _PinLessLimitFare;
        }
    }

    private static boolean _isMoneyEdy;

    public static boolean isMoneyEdy() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyEdy;
        }
    }

    private static boolean _isMoneyId;

    public static boolean isMoneyId() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyId;
        }
    }

    private static boolean _isMoneyNanaco;

    public static boolean isMoneyNanaco() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyNanaco;
        }
    }

    private static boolean _isMoneyQr;

    public static boolean isMoneyQr() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyQr;
        }
    }

    private static boolean _isMoneyQuicpay;

    public static boolean isMoneyQuicpay() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyQuicpay;
        }
    }

    private static boolean _isMoneySuica;

    public static boolean isMoneySuica() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneySuica;
        }
    }

    private static boolean _isMoneyUnionpay;

    public static boolean isMoneyUnionpay() {
        return _isMoneyUnionpay;
    }

    private static boolean _isMoneyWaon;

    public static boolean isMoneyWaon() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyWaon;
        }
    }

    private static boolean _isMoneyOkica;

    public static boolean isMoneyOkica() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isMoneyOkica;
        }
    }

    private static boolean _isWatariPoint;

    public static boolean isWatariPoint() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isWatariPoint;
        }
    }

    private static boolean _isFixedAmountPostalOrder;

    public static boolean isFixedAmountPostalOrder() {
        if (isDemoMode()) {
            return true;
        } else {
            return _isFixedAmountPostalOrder;
        }
    }

    private static boolean _isInput1yen;

    public static boolean isInput1yenEnabled() {
        return _isInput1yen;
    }

    private static boolean _isWithcash1yen;

    public static boolean isWishcash1yenEnabled() {
        return _isWithcash1yen;
    }

    private static int _settingVersion;

    public static int getSettingVersion() {
        return _settingVersion;
    }

    private static boolean _confirmNormalShutdown;

    public static boolean getConfirmNormalShutdown() {
        return _confirmNormalShutdown;
    }

    public static void setConfirmNormalShutdown(boolean value) {
        _confirmNormalShutdown = value;
        p.edit().putBoolean("confirm_normal_shutdown", value).apply();
    }

    public static boolean getConfirmNormalEmoney() {
        return p.getBoolean("confirm_normal_emoney", true);
    }

    public static void setConfirmNormalEmoney(boolean value) {
        p.edit().putBoolean("confirm_normal_emoney", value).apply();
    }

    public static int getConfirmObstacleBusinessid() {
        return p.getInt("confirm_obstacle_businessid", -1);
    }

    public static void setConfirmObstacleBusinessid(int businessid) {
        p.edit().putInt("confirm_obstacle_businessid", businessid).apply();
    }

    public static String getConfirmObstacleSid() {
        return p.getString("confirm_obstacle_sid", null);
    }

    public static void setConfirmObstacleSid(String sid) {
        p.edit().putString("confirm_obstacle_sid", sid).apply();
    }

    private static Boolean _poweroffTrans;

    public static Boolean getPoweroffTrans() {
        return _poweroffTrans;
    }

    public static void setPoweroffTrans(Boolean poweroffTrans) {
        _poweroffTrans = poweroffTrans;
    }

    private static String _datetimeOpeningSuica;

    public static String getDatetimeOpeningSuica() {
        return _datetimeOpeningSuica;
    }

    public static void setDatetimeOpeningSuica(String datetime) {
        _datetimeOpeningSuica = datetime;
        p.edit().putString("datetime_opening_suica", datetime).apply();
    }

    private static String _datetimeOpeningId;

    public static String getDatetimeOpeningId() {
        return _datetimeOpeningId;
    }

    public static void setDatetimeOpeningId(String datetime) {
        _datetimeOpeningId = datetime;
        p.edit().putString("datetime_opening_id", datetime).apply();
    }

    private static String _datetimeOpeningWaon;

    public static String getDatetimeOpeningWaon() {
        return _datetimeOpeningWaon;
    }

    public static void setDatetimeOpeningWaon(String datetime) {
        _datetimeOpeningWaon = datetime;
        p.edit().putString("datetime_opening_waon", datetime).apply();
    }

    private static String _datetimeOpeningNanaco;

    public static String getDatetimeOpeningNanaco() {
        return _datetimeOpeningNanaco;
    }

    public static void setDatetimeOpeningNanaco(String datetime) {
        _datetimeOpeningNanaco = datetime;
        p.edit().putString("datetime_opening_nanaco", datetime).apply();
    }

    private static String _datetimeOpeningQuicpay;

    public static String getDatetimeOpeningQuicpay() {
        return _datetimeOpeningQuicpay;
    }

    public static void setDatetimeOpeningQuicpay(String datetime) {
        _datetimeOpeningQuicpay = datetime;
        p.edit().putString("datetime_opening_quicpay", datetime).apply();
    }

    private static String _datetimeOpeningEdy;

    public static String getDatetimeOpeningEdy() {
        return _datetimeOpeningEdy;
    }

    public static void setDatetimeOpeningEdy(String datetime) {
        _datetimeOpeningEdy = datetime;
        p.edit().putString("datetime_opening_edy", datetime).apply();
    }

    private static String _datetimeAuthenticationMc;

    public static String getDatetimeAuthenticationMc() {
        return _datetimeAuthenticationMc;
    }

    public static void setDatetimeAuthenticationMc(String datetime) {
        _datetimeAuthenticationMc = datetime;
        p.edit().putString("datetime_authentication_mc", datetime).apply();
    }

    private static String _datetimeAuthenticationQr;

    public static String getDatetimeAuthenticationQr() {
        return _datetimeAuthenticationQr;
    }

    public static void setDatetimeAuthenticationQr(String datetime) {
        _datetimeAuthenticationQr = datetime;
        p.edit().putString("datetime_authentication_qr", datetime).apply();
    }

    private static int _termSequence;

    public static int getTermSequence() {
        return _termSequence;
    }

    public static void setTermSequence(int value) {
        _termSequence = value;
        p.edit().putInt("term_sequence", value).apply();
    }

    // 和多利用伝票番号
    private static int _slipNoWatari;

    public static int getSlipNoWatari() {
        return _slipNoWatari;
    }

    public static void setSlipNoWatari(int value) {
        _slipNoWatari = value;
        p.edit().putInt("slip_no_watari", value).apply();
    }

    public static void termSequenceIncrement() {
        _termSequence = _termSequence < 999 ? _termSequence + 1 : 1;
        p.edit().putInt("term_sequence", _termSequence).apply();
    }

    private static String _jremActivateId;

    public static String getJremActivateId() {
        return _jremActivateId;
    }

    public static void clearJremActivateId() {
        _jremActivateId = "";
        p.edit().putString(getKey(R.string.setting_key_jrem_activateid), "").apply();
    }

    private static String _jremPassword;

    public static String getJremPassword() {
        return _jremPassword;
    }

    public static void clearJremPassword() {
        _jremPassword = "";
        p.edit().putString(getKey(R.string.setting_key_jrem_password), "").apply();
    }

    // JREM認証時に保存
    private static String _jremUniqueId;

    public static String getJremUniqueId() {
        return _jremUniqueId;
    }

    public static void setJremUniqueId(String id) {
        _jremUniqueId = id;
        p.edit().putString(getKey(R.string.setting_key_jrem_unique_id), id).apply();
    }

    private static boolean _isScreenlockEnabled;

    public static boolean isScreenlockEnabled() {
        return _isScreenlockEnabled;
    }

    private static String _screenlockPassword;

    public static String getScreenlockPassword() {
        return _screenlockPassword;
    }

    private static int _TimeoutScreen;

    public static int getTimeoutScreen() {
        return _TimeoutScreen;
    }

    private static int _screenLockSec;

    public static int getScreenLockSec() {
        return _screenLockSec;
    }

    private static String _merchantName;

    public static String getMerchantName() {
        return _merchantName;
    }

    private static String _merchantOffice;

    public static String getMerchantOffice() {
        return _merchantOffice;
    }

    private static String _merchantTelnumber;

    public static String getMerchantTelnumber() {
        return _merchantTelnumber;
    }

    private static boolean _isAntlogEnabled;

    public static boolean isAntlogEnabled() {
        return _isAntlogEnabled;
    }

    private static int _antlogTime;

    public static int getAntlogTime() {
        return _antlogTime;
    }

    private static boolean _isGpslogEnabled;

    public static boolean isGpslogEnabled() {
        return _isGpslogEnabled;
    }

    private static int _gpslogTime;

    public static int getGpslogTime() {
        return _gpslogTime;
    }

    private static int _gpslogDistance;

    public static int getGpslogDistance() {
        return _gpslogDistance;
    }

    private static int _soundPaymentVolume;

    public static int getSoundPaymentVolume() {
        return _soundPaymentVolume;
    }

    private static int _soundOperationVolume;

    public static int getSoundOperationVolume() {
        return _soundOperationVolume;
    }

    private static int _soundGuidanceVolume;

    public static int getSoundGuidanceVolume() {
        return _soundGuidanceVolume;
    }

    private static int _brightnessValue;

    public static int getBrightnessValue() {
        return _brightnessValue;
    }

    private static int _mcCarId;

    public static int getMcCarId() {
        return _mcCarId;
    }

    public static void setMcCarId(int carId) {
        _mcCarId = carId;
        p.edit().putInt(getKey(R.string.setting_key_mc_carid), carId).apply();
    }

    private static String _invoiceNo;
    public static String getInvoiceNo() {
        if (isDemoMode()){
            return "T1234567891011";
        }
        return _invoiceNo;
    }

    /*インボイス対応*/
    private static String _receiptTax;
    public static String getreceiptTax() {
        if (isDemoMode()){
            return "10";
        }
        return _receiptTax;
    }

    /*インボイス対応*/
    /*-----------------------------------------------
    * 値が範囲外の場合、レシートに出力させない
    * -----------------------------------------------*/
    public static boolean judgeInvoice(boolean input_ErrLog)
    {
        if (isDemoMode()){
            return true;
        }
        //番号、税率共にnullでない場合
        
        if (_invoiceNo != null && _receiptTax != null){
            //番号：空白、税率：0ではない&&インボイス番号先頭文字&&文字列の長さ14の場合
            if ((_invoiceNo.startsWith("T") && _invoiceNo.length() == 14) && judgeTaxValue()){
                return true;//レシート出力
            } else if (input_ErrLog){
                SettlementErrorLog();   //インボイス情報エラー
            }
        } else if (input_ErrLog){
            SettlementErrorLog();   //インボイス情報エラー
        }
        return false;//レシート出力しない
    }

    /*インボイス対応*/
    //取得消費税が範囲内ならtrueを返す（1 < 取得消費税 < 100）
    public static boolean judgeTaxValue(){
        Float fi;

        if (_receiptTax ==null){
            return false;
        } else {
            fi = Float.parseFloat(_receiptTax);
        }

        if (fi >= 1 && fi <= 100){
            return true;
        } else {
            return false;
        }
    }

    /*インボイス対応*/
    //起動時または再開局時にインボイス情報のどちらかでもnullだった場合スタックエラーを表示させる
    public static boolean judgeInvoiceStack()
    {
        if (_invoiceNo == null || _receiptTax == null){
            return true;
        }else{
            return false;
        }
    }
	/*インボイス対応*/
	//POSモードの判定
    public static boolean settlementPosChk() {
        return _isServicePos;
    }

    /*インボイス対応*/
    //レシート出力時のインボイス情報エラー
    public static void SettlementErrorLog(){
        if (_invoiceNo!= null && _invoiceNo.equals("")){
            Timber.tag("Printer").e("インボイス情報エラー(番号:%s, 消費税:%s%%)"," ",_receiptTax);
        } else {
            Timber.tag("Printer").e("インボイス情報エラー(番号:%s, 消費税:%s%%)",_invoiceNo,_receiptTax);
        }

    }

    private static String _supplierCd;
    public static  String getSupplierCd() {
        return _supplierCd;
    }

    private static Integer _maxAmountType;
    public static MaxAmountType getMaxAmountType() {
        if(_maxAmountType == null || _maxAmountType == 0){
            return MaxAmountType.DEFAULT;
        }
        else {
            return MaxAmountType.LARGE;
        }
    }
    private static String MaxAmountTypeToString(int value) {
        if (value == 0) {
            return getKey(R.string.setting_max_amount_type_default);
        } else {
            return getKey(R.string.setting_max_amount_type_large);
        }
    }
    public enum MaxAmountType {
        DEFAULT,
        LARGE;
    }

    private static int _mcDriverId;

    public static int getMcDriverId() {
        if (AppPreference.isDriverCodeInput()) {
            // 入力有効設定
            return Integer.parseInt(AppPreference.getDriverCode());
        } else {
            // 入力無効設定
            return _mcDriverId;
        }
    }

    public static String getPosStaffCode() {
        // 固定乗務員の場合、POSには空文字で送るためgetMcDriverIdの代わりに使用
        if (AppPreference.isDriverCodeInput()) {
            // 入力有効設定
            return String.valueOf(AppPreference.getDriverCode());
        } else {
            // 入力無効設定
            return "";
        }
    }

    private static String _mcTermId;

    public static String getMcTermId() {
        return _mcTermId;
    }

    private static String _qrUserId;

    public static String getQrUserId() {
        return _qrUserId;
    }

    public static void clearQrUserId() {
        _qrUserId = "";
        p.edit().putString(getKey(R.string.setting_key_qr_userid), _qrUserId).apply();
    }

    private static String _qrPassword;

    public static String getQrPassword() {
        return _qrPassword;
    }

    public static void clearQrPassword() {
        _qrPassword = "";
        p.edit().putString(getKey(R.string.setting_key_qr_password), _qrPassword).apply();
    }

    // ミリ秒単位のUNIX時間(ログイン時の時刻)
    private static String _gmoHeaderTime;

    public static String getGmoHeaderTime() {
        return _gmoHeaderTime;
    }

    public static void setGmoHeaderTime(String time) {
        _gmoHeaderTime = time;
        p.edit().putString("gmo_header_time_key", time).apply();
    }

    // ランダム文字列(15文字固定)
    private static String _gmoHeaderNonceStr;

    public static String getGmoHeaderNonceStr() {
        return _gmoHeaderNonceStr;
    }

    public static void setGmoHeaderNonceStr(String str) {
        _gmoHeaderNonceStr = str;
        p.edit().putString("gmo_header_nonce_str_key", str).apply();
    }

    // 通信トークンハッシュ値
    private static String _gmoHeaderSign;

    public static String getGmoHeaderSign() {
        return _gmoHeaderSign;
    }

    public static void setGmoHeaderSign(String sign) {
        _gmoHeaderSign = sign;
        p.edit().putString("gmo_header_sign_key", sign).apply();
    }

    // 開局時にしか使わないのでRAMに保持しない
    public static String getNanacoDaTermFrom() {
        return p.getString("nanaco_da_term_from", null);
    }

    public static void setNanacoDaTermFrom(String value) {
        p.edit().putString("nanaco_da_term_from", value).apply();
    }

    // 開局時にしか使わないのでRAMに保持しない
    public static String getQuicpayDaTermFrom() {
        return p.getString("qp_da_term_from", null);
    }

    public static void setQuicpayDaTermFrom(String value) {
        p.edit().putString("qp_da_term_from", value).apply();
    }

    private static String _productCode;

    public static String getProductCode() {
        return _productCode;
    }

    public static int getConfirmMeterCharge() {
        int value = p.getInt("confirm_meter_charge", 0);
        p.edit().remove("confirm_meter_charge").apply();
        return value;
    }

    public static int getConfirmFlatRateAmount() {
        int value = p.getInt("confirm_flat_rate_amount", 0);
        p.edit().remove("confirm_flat_rate_amount").apply();
        return value;
    }

    public static int getConfirmTotalChangeAmount() {
        int value = p.getInt("confirm_total_change_amount", 0);
        p.edit().remove("confirm_total_change_amount").apply();
        return value;
    }

    public static int getConfirmFixedAmount() {
        int value = p.getInt("confirm_fixed_amount", 0);
        p.edit().remove("confirm_fixed_amount").apply();
        return value;
    }

    public static void setConfirmAmount() {
        p.edit().putInt("confirm_meter_charge", Amount.getMeterCharge()).apply();
        p.edit().putInt("confirm_flat_rate_amount", Amount.getFlatRateAmount()).apply();
        p.edit().putInt("confirm_total_change_amount", Amount.getTotalChangeAmount()).apply();
        p.edit().putInt("confirm_fixed_amount", Amount.getFixedAmount()).apply();
    }

    public static void removeConfirmAmount() {
        p.edit().remove("confirm_meter_charge").apply();
        p.edit().remove("confirm_flat_rate_amount").apply();
        p.edit().remove("confirm_total_change_amount").apply();
        p.edit().remove("confirm_fixed_amount").apply();
    }

    private static boolean _isDeveloperMode;

    public static boolean isDeveloperMode() {
        return _isDeveloperMode;
    }

    public static void set_isDemoMode(boolean status) {
        _isDemoMode = status;
        final String key = MainApplication.getInstance().getString(R.string.setting_key_demo_mode);
        p.edit().putBoolean(key, status).apply();
    }

    private static boolean _isAggregateDetail;

    public static boolean isAggregateDetail() {
        return _isAggregateDetail;
    }

    private static boolean _isMcEcho = false;

    public static boolean isMcEcho() {
        return _isMcEcho;
    }

    public static void execMcEcho() {
        _isMcEcho = true;
    }

    private static boolean _available = false;

    public static boolean isAvailable() {
        return _available;
    }

    public static void setIsAvailable(boolean status) {
        _available = status;
    }

    private static boolean _detachJR = false;

    public static boolean isDetachJR() {
        return _detachJR;
    }

    public static void setDetachJR(boolean status) {
        _detachJR = status;
    }

    private static boolean _detachQR = false;

    public static boolean isDetachQR() {
        return _detachQR;
    }

    public static void setDetachQR(boolean status) {
        _detachQR = status;
    }

    private static String _organizationId;

    public static String getOrganizationId() {
        return _organizationId;
    }

    private static Long _prepaidAppVersionCode = null;

    public static Long getPrepaidAppVersionCode() {
        return _prepaidAppVersionCode;
    }

    public static void setPrepaidAppVersionCode(Long versionCode) {
        _prepaidAppVersionCode = versionCode;
    }

    private static String _prepaidAppVersionName = null;

    public static String getPrepaidAppVersionName() {
        return _prepaidAppVersionName;
    }

    public static void setPrepaidAppVersionName(String versionName) {
        _prepaidAppVersionName = versionName;
    }

    private static FirmWareInfo _ifBoxOTAInfo = null;

    public static FirmWareInfo getIFBoxOTAInfo() {
        return _ifBoxOTAInfo;
    }

    public static void setIFBoxOTAInfo(FirmWareInfo info) {
        _ifBoxOTAInfo = info;
        p.edit().putString("ifbox_firmware_info", info != null ? _gson.toJson(info) : null).apply();
    }

    public static boolean isIFBoxSetupFinished() {
        return _ifBoxOTAInfo != null;
    }

    private static Version.Response _ifBoxVersionInfo = null;

    public static Version.Response getIFBoxVersionInfo() {
        return _ifBoxVersionInfo;
    }

    public static void setIFBoxVersionInfo(Version.Response info) {
        _ifBoxVersionInfo = info;
        p.edit().putString("ifbox_version_info", info != null ? _gson.toJson(info) : null).apply();
//        if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D)) {
//            /* ヤザキLT27双方向連動ではOKICA未対応のため、OKICA利用フラグを無効にする */
//            p.edit().putBoolean((getKey(R.string.setting_key_money_okica)), false).apply();
//            _isMoneyOkica = false;
//        }
    }

    public static void clearWifiP2pDeviceInfo() {
        setIFBoxOTAInfo(null);
        setIFBoxVersionInfo(null);
        setTabletLinkInfo(null);
        setIFBoxServiceInfo(null);
        setTabletVersionInfo(null);
    }

    //前回起動時のバージョン 異常終了検知でしか使わないのでRAMに保持しない
    public static int getPrevAppVersionCode() {
        return p.getInt("prev_app_version_code", BuildConfig.VERSION_CODE);
    }

    public static void setPrevAppVersionCode() {
        p.edit().putInt("prev_app_version_code", BuildConfig.VERSION_CODE).apply();
    }

    // 有効性確認の通番
    private static int _validationCheckTermSequence;

    public static int getValidationCheckTermSequence() {
        return _validationCheckTermSequence;
    }

    public static int getNextValidationCheckTermSequence() {
        return _validationCheckTermSequence < 999 ? _validationCheckTermSequence + 1 : 1;
    }

    public static void incrementValidationCheckTermSequence() {
        _validationCheckTermSequence = _validationCheckTermSequence < 999 ? _validationCheckTermSequence + 1 : 1;
        p.edit().putInt("validation_check_term_sequence", _validationCheckTermSequence).apply();
    }

    public static void setValidationCheckTermSequence(int value) {
        _validationCheckTermSequence = value;
        p.edit().putInt("validation_check_term_sequence", value).apply();
    }

    private static TabletLinkInfo _tabletLinkInfo = null;

    public static TabletLinkInfo getTabletLinkInfo() {
        return _tabletLinkInfo;
    }

    public static void setTabletLinkInfo(TabletLinkInfo info) {
        _tabletLinkInfo = info;
        p.edit().putString("tablet_link_info", info != null ? _gson.toJson(info) : null).apply();
    }

    private static jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version.Response _tabletVersionInfo = null;

    public static jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version.Response getTabletVersionInfo() {
        return _tabletVersionInfo;
    }

    public static void setTabletVersionInfo(jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version.Response version) {
        _tabletVersionInfo = version;
        p.edit().putString("tablet_version_info", version != null ? _gson.toJson(version) : null).apply();
    }

    public static boolean isIFBoxHost() {
        return _ifBoxOTAInfo != null && _tabletLinkInfo == null;
    }

    private static String _datetimeLt27Printable = null;

    public static String getDatetimeLt27Printable() {
        return _datetimeLt27Printable;
    }

    public static void setDatetimeLt27Printable(String datetime) {
        _datetimeLt27Printable = datetime;
        p.edit().putString("datetime_lt27_printable", datetime).apply();
    }

    private static String _ifBoxServiceInfo = null;

    public static String getIFBoxServiceInfo() {
        return _ifBoxServiceInfo;
    }

    public static void setIFBoxServiceInfo(String json) {
        _ifBoxServiceInfo = json;
        p.edit().putString("ifbox_service_info", json).apply();
    }

    private static String _okicaAccessToken = null;

    public static String getOkicaAccessToken() {
        return _okicaAccessToken;
    }

    public static void setOkicaAccessToken(String token) {
        _okicaAccessToken = token;
        p.edit().putString(getKey(R.string.setting_key_okica_access_token), token).apply();
    }

    private static String _okicaAuthCode = null;

    public static String getOkicaAuthCode() {
        return _okicaAuthCode;
    }

    public static void setOkicaAuthCode(String token) {
        _okicaAuthCode = token;
        p.edit().putString(getKey(R.string.setting_key_okica_auth_code), token).apply();
    }

    private static jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo.Response _okicaTerminalInfo = null;

    public static jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo.Response getOkicaTerminalInfo() {
        return _okicaTerminalInfo;
    }

    public static void setOkicaTerminalInfo(jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo.Response info) {
        _okicaTerminalInfo = info;
        p.edit().putString(getKey(R.string.setting_key_okica_terminal_info), info != null ? _gson.toJson(info) : null).apply();
    }

    public static void clearOkica() {
        setOkicaAccessToken(null);
        setOkicaAuthCode(null);
        setOkicaTerminalInfo(null);
        setOkicaICMasterInfo(null);
        setOkicaAccessKeyInfo(null);
        Timber.i("OKICA 情報クリア(AccessToken,AuthCode,TerminalInfo,ICMasterInfo,AccessKeyInfo)");
    }

    public static void clearOkicaMasterInfo() {
        setOkicaICMasterInfo(null);
        setOkicaAccessKeyInfo(null);
        Timber.i("OKICA 情報クリア(ICMasterInfo,AccessKeyInfo)");
    }

    private static ICMasterInfo _okicaICMasterInfo = null;

    public static ICMasterInfo getOkicaICMasterInfo() {
        return _okicaICMasterInfo;
    }

    public static void setOkicaICMasterInfo(ICMasterInfo masterInfo) {
        _okicaICMasterInfo = masterInfo;
        p.edit().putString(getKey(R.string.setting_key_okica_ic_master_info), masterInfo != null ? _gson.toJson(masterInfo) : null).apply();
    }

    private static AccessKeyInfo _okicaAccessKeyInfo = null;

    public static AccessKeyInfo getOkicaAccessKeyInfo() {
        return _okicaAccessKeyInfo;
    }

    public static void setOkicaAccessKeyInfo(AccessKeyInfo info) {
        _okicaAccessKeyInfo = info;
        p.edit().putString(getKey(R.string.setting_key_okica_access_key_info), info != null ? _gson.toJson(info) : null).apply();
    }

    public static boolean isOkicaAvailable() {
        String errorMsg = "";
        if (_okicaAccessToken == null) {
            errorMsg += "AccessToken ";
        }
        if (_okicaTerminalInfo == null) {
            errorMsg += "TerminalInfo ";
        }
        if (_okicaICMasterInfo == null) {
            errorMsg += "ICMasterInfo ";
        }
        if (_okicaAccessKeyInfo == null) {
            errorMsg += "AccessKeyInfo ";
        }

        if (!errorMsg.equals("")) {
            Timber.e("okicaInfoNull(%s)", errorMsg);
        }
        return _isMoneyOkica && _okicaAccessToken != null && _okicaTerminalInfo != null && _okicaICMasterInfo != null && _okicaAccessKeyInfo != null;
    }

    private static String _okicaNegaDatetime = null;

    public static String getOkicaNegaDatetime() {
        return _okicaNegaDatetime;
    }

    public static void setOkicaNegaDatetime(String datetime) {
        if (datetime != null) {
            _okicaNegaDatetime = datetime.substring(0, 4) + "/" + datetime.substring(4, 6) + "/" + datetime.substring(6, 8);
        } else {
            _okicaNegaDatetime = datetime;
        }
        p.edit().putString("setting_okica_nega_dadetime", _okicaNegaDatetime).apply();
    }

    public static boolean isOkicaCommunicationAvailable() {
        // TODO M.Kodama デモモード対応
        return _isMoneyOkica && _okicaAccessToken != null;
    }

    private static void jremActivateIdcheck() {

        if (_jremActivateId == null || _jremActivateId.equals("")) {
            /* アクティベーションIDが存在しない場合、端末保持情報クリア(JremInfo) */
            final File dir = MainApplication.getInstance().getFilesDir();
            final File certification = new File(dir, BuildConfig.JREM_CLIENT_CERTIFICATE);

            if (certification.exists()) {
                certification.delete();
            }

            setNanacoDaTermFrom(null);
            setQuicpayDaTermFrom(null);
            Timber.i("端末保持情報クリア(JremInfo)");
        }
    }

    private static boolean _isServicePos = false;

    public static boolean isServicePos() {
        // TODO:POSサービスのデモモード動作は要検討
        return _isServicePos;
    }

    public static void set_isServicePos(Boolean bool) {
        _isServicePos = bool;
        p.edit().putBoolean(getKey(R.string.setting_key_service_pos), bool).apply();
    }

    private static String _servicePosAccessToken = null;

    public static String get_servicePosAccessToken() {
        return _servicePosAccessToken;
    }

    public static void setServicePosAccessToken(String token) {
        _servicePosAccessToken = token;
        p.edit().putString(getKey(R.string.setting_key_service_pos_access_token), token).apply();
    }

    private static String _servicePosRefreshToken = null;

    public static String get_servicePosRefreshToken() {
        return _servicePosRefreshToken;
    }

    public static void set_servicePosRefreshToken(String token) {
        _servicePosRefreshToken = token;
        p.edit().putString(getKey(R.string.setting_key_service_pos_refresh_token), token).apply();
    }

    public static void set_servicePos(String accessToken, String refreshToken) {
        set_isServicePos(true);
        setServicePosAccessToken(accessToken);
        set_servicePosRefreshToken(refreshToken);
    }

    public static void clearServicePos() {
        set_isServicePos(false);
        if (isServiceTicket()) {
            // チケットサービスが有効な場合、トークン情報をクリアせずに残す
        } else {
            Timber.i("トークン情報クリア");
            setServicePosAccessToken("");
            set_servicePosRefreshToken("");
            setServiceTicketAccessToken("");
            set_serviceTicketRefreshToken("");
        }
    }

    private static String _posMerchantName;
    public static String getPosMerchantName() { return _posMerchantName; }

    private static String _posMerchantOffice;
    public static String getPosMerchantOffice() {
        return _posMerchantOffice;
    }

    private static String _posMerchantTelnumber;

    public static String getPosMerchantTelnumber() {
        return _posMerchantTelnumber;
    }

    private static String _posAddress;
    public static String getPosAddress() {
        return _posAddress;
    }

    private static Integer _standard_tax_rate;
    public static Integer getStandardTaxRate() {
        return _standard_tax_rate;
    }

    private static Integer _reduced_tax_rate;
    public static Integer getReducedTaxRate() {
        return _reduced_tax_rate;
    }

    private static Integer _taxRounding;
    public static Integer getTaxRounding() {
        return _taxRounding;
    }
    private static String TaxRoundingToString(int value) {
        if (TaxRoundings.FLOOR.value == value) {
            return getKey(R.string.setting_tax_roundings_floor);
        } else if (TaxRoundings.CEILING.value == value) {
            return getKey(R.string.setting_tax_roundings_ceiling);
        }
        else {
            return getKey(R.string.setting_tax_roundings_round);
        }
    }
    private static String _address;
    public static String getAddress() {
        return _address;
    }

    private static String _slipTitle;
    public static String getSlipTitle() {
        return _slipTitle;
    }

    private static Integer _receiptCount;
    public static Integer getReceiptCount() {
        return _receiptCount;
    }

    private static boolean _isProductCategory;
    public static boolean getIsProductCategory() {
        return _isProductCategory;
    }

    private static boolean _isPosReceipt;
    public static boolean getIsPosReceipt() {
        return _isPosReceipt;
    }

    private static boolean _isManualAmount;
    public static boolean getIsManualAmount() {
        return _isManualAmount;
    }

    private static String BooleanToStringOnOff(boolean flg) {
        if (flg) {
            return getKey(R.string.setting_boolean_true_on);
        }
        else {
            return getKey(R.string.setting_boolean_false_off);
        }
    }

    private static boolean _isServiceTicket = false;

    public static boolean isServiceTicket() {
        return _isServiceTicket;
    }

    public static void set_isServiceTicket(Boolean bool) {
        _isServiceTicket= bool;
        p.edit().putBoolean(getKey(R.string.setting_key_service_ticket), bool).apply();
    }

    private static String _serviceTicketAccessToken = null;

    public static String get_serviceTicketAccessToken() {
        return _serviceTicketAccessToken;
    }

    public static void setServiceTicketAccessToken(String token) {
        _serviceTicketAccessToken = token;
        p.edit().putString(getKey(R.string.setting_key_service_ticket_access_token), token).apply();
    }

    private static String _serviceTicketRefreshToken = null;

    public static String get_serviceTicketRefreshToken() {
        return _serviceTicketRefreshToken;
    }

    public static void set_serviceTicketRefreshToken(String token) {
        _serviceTicketRefreshToken = token;
        p.edit().putString(getKey(R.string.setting_key_service_ticket_refresh_token), token).apply();
    }

    public static boolean isServiceTaxi() {
        return !isServicePos() && !isServiceTicket();
    }

    //CHG-S BMT S.Oyama 2024/09/10 フタバ双方向向け改修
    public static boolean isAmountInput() {
        boolean isServicePos = isServicePos();
        boolean isPosAmount = Amount.isPosAmount();
        boolean isAmountInputCancel = isAmountInputCancel();

        if (isAmountInputCancel == true) {
            return false;
        }

        //return !isServicePos() || !Amount.isPosAmount();
        return !isServicePos || !isPosAmount;
    }
    //CHG-E BMT S.Oyama 2024/09/10 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/09/10 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  金額入力画面の表示／非表示の管理
     * @note   金額入力画面の表示／非表示の管理
     */
    /******************************************************************************/
    private static boolean _isAmountInputCancel = false;
    public static void setAmountInputCancel(boolean bool) {
        _isAmountInputCancel = bool;
    }
    public static boolean isAmountInputCancel() {
        return _isAmountInputCancel;
    }
    //ADD-S BMT S.Oyama 2024/09/10 フタバ双方向向け改修

    private static String _meterStatus = null;
    public static void setMeterStatus(String status) { _meterStatus = status; }
    public static String getMeterStatus() {
        if (_meterStatus != null) {
            return "";
        } else {
            return _meterStatus;
        }
    }
    public static boolean isMeterStatusGeisya() {
        if (_meterStatus != null) {
            return _meterStatus.equals("GEISYA");
        } else {
            return false;
        }
    }
    public static boolean isMeterStatusJissya() {
        if (_meterStatus != null) {
            return _meterStatus.equals("JISSYA");
        } else {
            return false;
        }
    }
    public static boolean isMeterStatusSiharai() {
        if (_meterStatus != null) {
            return _meterStatus.equals("SIHARAI");
        } else {
            return false;
        }
    }

    /*** チケット販売関連 ***/
    // 選択されたチケット分類
    private static TicketClassData _selectedTicketClassData = null;

    public static TicketClassData getSelectedTicketClassData() {
        return _selectedTicketClassData;
    }

    public static void setSelectedTicketClassData(TicketClassData data) { _selectedTicketClassData = data; }

    // 選択されたのりば
    private static TicketEmbarkData _selectedTicketEmbarkData = null;

    public static TicketEmbarkData getSelectedTicketEmbarkData() { return _selectedTicketEmbarkData; }

    public static void setSelectedTicketEmbarkData(TicketEmbarkData data) { _selectedTicketEmbarkData = data; }

    // 選択されたおりば
    private static TicketEmbarkData _selectedTicketDisembarkData = null;

    public static TicketEmbarkData getSelectedTicketDisembarkData() { return _selectedTicketDisembarkData; }

    public static void setSelectedTicketDisembarkData(TicketEmbarkData data) { _selectedTicketDisembarkData = data; }

    public static void set_serviceTicket(String accessToken, String refreshToken) {
        set_isServiceTicket(true);
        setServiceTicketAccessToken(accessToken);
        set_serviceTicketRefreshToken(refreshToken);
    }

    public static void clearServiceTicket() {
        set_isServiceTicket(false);
        if (isServicePos()) {
            // POSサービスが有効な場合、トークン情報をクリアせずに残す
        } else {
            Timber.i("トークン情報クリア");
            setServiceTicketAccessToken("");
            set_serviceTicketRefreshToken("");
            setServicePosAccessToken("");
            set_servicePosRefreshToken("");
        }
    }

    // チケット検索結果
//    public static TicketSearchResults _ticketSearchResults = null;
//    public static TicketSearchResults getTicketSearchResults() { return _ticketSearchResults; }
//    public static void setTicketSearchResults(TicketSearchResults results) { _ticketSearchResults = results; }

    // 動的チケットの応答結果
    public static DynamicTicketItem _dynamicTicketItem = null;
    public static DynamicTicketItem getDynamicTicketItem() { return _dynamicTicketItem; }
    public static void setDynamicTicketItem(DynamicTicketItem ticketItem) { _dynamicTicketItem = ticketItem; }

    // gtfs feed version
    private static String _gtfsCurrentFeedId = "";
    public static void setGTFSCurrentFeedId(String feedId) {
        _gtfsCurrentFeedId = feedId;
        Timber.i("GTFS feed id = %s", _gtfsCurrentFeedId);
    }
    public static String getGTFSCurrentFeedId() { return _gtfsCurrentFeedId; }

    // チケット販売マスタデータ取得結果
    private static boolean _ticketDataInit = false;
    public static boolean isTicketDataInit() { return _ticketDataInit; }
    public static void isTicketDataInit(boolean b) { _ticketDataInit = b; }

    // 取引時の端末タイプ
    private static Integer _transactionTerminalType = 0;
    public static boolean isPosTransaction() {
        return _transactionTerminalType == TerminalType.Pos.ordinal();
    }
    public static boolean isTicketTransaction() {
        return _transactionTerminalType == TerminalType.Ticket.ordinal();
    }
    // 決済時
    public static void setTransactionTerminalType() {
        if (isServicePos() && Amount.isPosAmount()) {
            _transactionTerminalType = TerminalType.Pos.ordinal();
        } else if (isServiceTicket() && !Amount.isPosAmount()) {
            _transactionTerminalType = TerminalType.Ticket.ordinal();
        } else {
            _transactionTerminalType = TerminalType.Taxi.ordinal();
        }
    }
    // 取消時
    public static void setTransactionTerminalType(int type) {
        _transactionTerminalType = type;
    }
    public enum TerminalType{
        Taxi,
        Pos,
        Ticket;
    }
    // 取消対象の伝票データID番号
    private static int _cancelTargetSlipId = 0;
    public static int getCancelTargetSlipId () {
        //Timber.d("getCancelTargetSlipId:%s",_cancelTargetSlipId);
        return _cancelTargetSlipId;
    }
    public static void setCancelTargetSlipId (int slipId) {
        _cancelTargetSlipId = slipId;
        //Timber.d("setCancelTargetSlipId:%s",slipId);
    }

    public static boolean isTicketIssueCancel() {
        return _isTicketIssueCancel;
    }
    private static boolean _isTicketIssueCancel = false;
    public static void isTicketIssueCancel(boolean cancel) {
        _isTicketIssueCancel = cancel;
    }

    public static boolean ticketGateScanUseLight() {
        return _ticketGateScanUseLight;
    }
    private static boolean _ticketGateScanUseLight = true;
    public static void ticketGateScanUseLight(boolean val) {
        _ticketGateScanUseLight = val;
    }

    // 手続き開始時刻（一時的に保持用）
    private static String _nextTripGateCheckStartTime = null;
    public static String getNextTripGateCheckStartTime() {
        return _nextTripGateCheckStartTime;
    }
    public static void setNextTripGateCheckStartTime(String time) {
        _nextTripGateCheckStartTime = time;
    }

    // チケット改札設定データ（一時的に保持用）
    private static TicketGateSettingsData _ticketGateSettingsData = null;
    public static TicketGateSettingsData getTicketGateSettingsData() {
        return _ticketGateSettingsData;
    }
    public static void setTicketGateSettingsData(TicketGateSettingsData data) {
        _ticketGateSettingsData = data;
    }

    /* 自動つり銭機連動 */
    private static boolean _isCashChanger;
    public static void setIsCashChanger(boolean status) {
        _isCashChanger = status;
        final String key = MainApplication.getInstance().getString(R.string.setting_key_cashchanger);
        p.edit().putBoolean(key, status).apply();
    }
    public static boolean getIsCashChanger() { return _isCashChanger; }

    /* キャッシュドロア連動 */
    private static int _cashDrawerType;  // 0:ドロアなし, 1:プリンタのみ, 2:ドロアのみ, 3:両方
    public static void setCashDrawerType(int type) {
        _cashDrawerType = type;
        final String key = MainApplication.getInstance().getString(R.string.setting_key_cashdrawer_type);
        p.edit().putInt(key, type).apply();
    }
    public static int getCashDrawerType() { return _cashDrawerType; }
    public static boolean getIsCashDrawerTypePonly() { return(_cashDrawerType == 1); }
    public static boolean getIsCashDrawerTypeDonly() { return(_cashDrawerType == 2); }
    public static boolean getIsCashDrawerTypeAll() { return(_cashDrawerType == 3); }

    /* PT-750をクレードルに載せる構成であるかを判定 */
    public static boolean getIsOnCradle() {
        if (getIsCashChanger() || getCashDrawerType() != 0) {
            return true;
        }
        return false;
    }
    /* 外部プリンタを使用する構成であるかを判定 */
    public static boolean getIsExternalPrinter() {
        if (getIsCashChanger() || getCashDrawerType() == 1 || getCashDrawerType() == 3) {
            return true;
        }
        return false;
    }

    private static String _cashChangerPrinterType;

    private static boolean _isPrepaid;
    public static boolean getIsPrepaid() {
        return _isPrepaid;
    }
    public static void setIsPrepaid(boolean isPrepaid) {
        _isPrepaid = isPrepaid;
    }

    private static String _prepaidServiceDomain;
    public static String getPrepaidServiceDomain() {
        return _prepaidServiceDomain;
    }
    public static void setPrepaidServiceDomain(String prepaidServiceDomain) {
        _prepaidServiceDomain = prepaidServiceDomain;
    }

    private static String _prepaidServiceKey;
    public static String getPrepaidServiceKey() {
        return _prepaidServiceKey;
    }
    public static void setPrepaidServiceKey(String prepaidServiceKey) {
        _prepaidServiceKey = prepaidServiceKey;
    }

    private static boolean _isTemporaryManualMode = false;
    public static boolean isTemporaryManualMode() {
        return _isTemporaryManualMode;
    }
    public static void setIsTemporaryManualMode(boolean mode) {
        _isTemporaryManualMode = mode;
    }
}
