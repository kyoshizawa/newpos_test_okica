package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import static jp.mcapps.android.multi_payment_terminal.data.okica.Constants.COMPANY_CODE_BUPPAN;

import androidx.annotation.IdRes;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessControlInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.CardBasicInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.Constants;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.IDi;
import jp.mcapps.android.multi_payment_terminal.data.okica.KaisatsuLogInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaNegaFile;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFBalanceInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFLogInfo;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.*;
import timber.log.Timber;

/**
 * 共通判定を行うクラスです
 */
public class CommonJudge {
    /**
     * 共通判定の結果
     */
    public enum Result {
        Success(null, "成功"),
        MultipleCardsError(_app.getString(R.string.error_type_okica_common_judge_detect_num_error), "処理枚数判定異常"),
        ReadError(_app.getString(R.string.error_type_okica_common_judge_read_error), "読取異常"),
        IDiError(_app.getString(R.string.error_type_okica_common_judge_idi_error), "IDi判定異常"),
        VersionError(_app.getString(R.string.error_type_okica_common_judge_version_error), "バージョン判定異常"),
        MasterDataError(_app.getString(R.string.error_type_okica_common_judge_master_data_error), "マスタデータ判定異常"),
        ICTypeError(_app.getString(R.string.error_type_okica_common_judge_ic_type_error), "IC種別判定異常"),
        DataItemError(_app.getString(R.string.error_type_okica_common_judge_data_item_error), "データ項目判定異常"),
        ActivationError(_app.getString(R.string.error_type_okica_common_judge_activation_error), "活性化判定異常"),
        TenYearsUnusedError(_app.getString(R.string.error_type_okica_common_judge_10_years_unused_error), "10年失効判定異常"),
        UnusableCardError(_app.getString(R.string.error_type_okica_common_judge_card_unavailable_error), "カード使用不可判定異常"),
        CardExpirationError(_app.getString(R.string.error_type_okica_common_judge_card_expired_error), "カード有効期間判定異常"),
        NegaCheckError(_app.getString(R.string.error_type_okica_common_judge_nega_check_error), "ネガチェック判定異常"),
        MasterDetailError(_app.getString(R.string.error_type_okica_data_error), "マスタデータ値異常");
        ;

        private final String errorCode;
        public String getErrorCode() {
            return errorCode;
        }

        private final String message;
        public String getMessage() {
            return message;
        }

        Result(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }
    }

    private static final byte P = 0b0001_1001;  // チェックデジット生成多項式
    private static final int P_LENGTH = 5;      // チェックデジット生成多項式のビットの長さ

    private static final MainApplication _app = MainApplication.getInstance();

    /**
     * カードの共通判定を行います
     * 最初に異常を検出した時点で総合判定NGとし判定処理を終了します
     *
     * @return 総合判定結果
     */
    public static Result execute(
            Polling polling,
            MutualAuthenticationRWSAM auth,
            ReadBlock readBlock,
            CardBasicInfo.Block3 cardBasicInfoB3,
            AccessControlInfo accessControlInfo,
            SFBalanceInfo sfBalanceInfo,
            SFLogInfo sfLogInfo,
            KaisatsuLogInfo kaisatsuLogInfo,
            boolean negaCheck
    ) {
        Result result = Result.Success;
        final IDi IDi = new IDi(auth.getIDi());
        final ICMaster.Activator activator = _app.getOkicaICMaster() != null
                ? _app.getOkicaICMaster().getData().getActivator(COMPANY_CODE_BUPPAN)
                : null;

        // カードの活性事業者コードに該当する活性事業者データを、IC運用マスタから取得
        ICMaster.Activator cardActivator = _app.getOkicaICMaster().getData().getActivator(cardBasicInfoB3.getCompanyCode());

        if (activator == null) {
            Timber.e("CommonJudge 事業者コード0x%04Xのマスタデータなし", COMPANY_CODE_BUPPAN);
            return Result.MasterDetailError;
        }

        if (cardActivator == null) {
            Timber.e("CommonJudge カードの事業者コード0x%04Xに該当するマスタデータなし", cardBasicInfoB3.getCompanyCode());
            return Result.MasterDetailError;
        }

//        if (!judgeMultipleCards(polling)) {  // 処理枚数判定
//            // 複数枚の検出の時には先にSDKエラーが発生するのでここがfalseになることはない
//            result = Result.MultipleCardsError;
//        }
//        else if (!judgeRead(auth, readBlock)) {  // 読取判定
        if (!judgeRead(auth, readBlock)) {
            result = Result.ReadError;
        }
        else if (!judgeIDi(auth)) {  // IDi判定
            result = Result.IDiError;
        }
        else if (!judgeVersion()) {  // バージョン判定
            result = Result.VersionError;
        }
        else if (!judgeMasterData(cardBasicInfoB3, IDi, activator)) {  // マスタデータ判定
            result = Result.MasterDataError;
        }
        else if (!judgeICType(IDi, cardBasicInfoB3)) {  // IC種別判定
            result = Result.ICTypeError;
        }
        else if(!judgeDataItem(cardBasicInfoB3, accessControlInfo, sfBalanceInfo, activator)) {  // データ項目判定
            result = Result.DataItemError;
        }
        else if (!judgeActivation(cardBasicInfoB3)) {  // 活性化判定
            result = Result.ActivationError;
        }
        else if (!judge10YearsUnused(cardBasicInfoB3, sfLogInfo, kaisatsuLogInfo, activator)) {  // 10年失効判定
            result = Result.TenYearsUnusedError;
        }
        else if (!judgeCardUnavailable(accessControlInfo)) { // カード使用不可判定
            result = Result.UnusableCardError;
        }
        else if (!judgeCardExpiration(cardBasicInfoB3)) {  // カード有効期間判定
            result = Result.CardExpirationError;
        }
        else if (negaCheck == true && !judgeNegaCheck(auth)) {  // ネガチェック判定
            result = Result.NegaCheckError;
        }

        return result;
    }

    /**
     * 処理枚数判定を行います
     *
     * @param polling ポーリングデータ
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeMultipleCards(Polling polling) {
        if (polling.getReceiveNum()[0] != 1) { Timber.e("処理枚数判定異常:%02x", polling.getReceiveNum()[0]); };
        return polling.getReceiveNum()[0] == 1;
    }

    /**
     * 読取判定を行います
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeRead(MutualAuthenticationRWSAM auth, ReadBlock readBlock) {
        if ((auth.getResult()[0] != (byte) 0xFF | readBlock.getStatusFlg1()[0] != 0x00) == false) { Timber.e("相互認証異常:%02x 読取異常:%02x", auth.getResult()[0], readBlock.getStatusFlg1()[0]); };
        return auth.getResult()[0] != (byte) 0xFF        // 相互認証処理異常
                | readBlock.getStatusFlg1()[0] != 0x00;  // 読取異常処理
    }

    /**
     * IDiの判定を行います
     * チェックデジットの計算にはmodule2演算を用います
     * IDiがオール0の場合もしくは取得したIDiのチェックデジット値と計算結果が一致しない場合はfalseを返します
     *
     * @param auth カード相互認証データ
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeIDi(MutualAuthenticationRWSAM auth) {
        final byte[] ID1 = auth.getIDi();

        boolean isAll0 = true;

        for (byte b : ID1) {
            if (b != 0) {
                isAll0 = false;
                break;
            }
        }

        if (isAll0) {
            Timber.e("IDi異常:オール0");
            return false;
        }

        final int crc = 0b0000_1111 & ID1[3];

        final byte[] ID0 = Arrays.copyOf(ID1, ID1.length);
        ID0[3] &= 0b1111_0000;

        final byte[] ID2 = Arrays.copyOf(ID0, ID0.length);
        ID2[ID2.length-1] ^= crc;

        int mod = 0;
        int idx = 0;

        while (true) {
            if (idx == ID2.length*8) {
                if (mod > 0) {
                    mod ^= P;
                }
                break;
            }
            else if ((mod & ( 1 << (P_LENGTH-1)) ) == 0) {
                mod = ( mod << 1 ) + ( ( ID2[idx/8] & ( 1 << 7-(idx%8) ) ) >> (7-(idx%8)) );
                idx++;
            }
            else {
                mod ^= P;
            }
        }

        if (mod != 0) { Timber.e("IDi異常(mod):%d", mod); };
        return mod == 0;
    }

    /**
     * バージョン判定を行います
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeVersion() {
        return true;
    }

    /**
     * マスタデータ判定を行います
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeMasterData(CardBasicInfo.Block3 cardBasicInfoB3, IDi IDi, ICMaster.Activator activator) {
        final ICMaster m = _app.getOkicaICMaster();

        // マスタ１の有効開始年月日時分 ＜ マスタ２の有効開始年月日時分	判定ＮＧ
        // 実日付時刻 ＜ マスタ２の有効開始年月日時分
        if (m.getData() == null) {
            return false;
        }

        // [発行ＩＤブロック] 事業者コード≠ＩＣ運用マスタ_一次発行事業者コードの場合、活性事業者判定ＮＧ
        boolean exists = false;
        for (ICMaster.FirstIssuer issuer: m.getData().getFirstIssuers()) {
            if (issuer.getCompanyCode() == IDi.getCompanyCode()) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            Timber.e("IC運用マスタ 一次発行事業者コードと不一致(カード事業者コード:%d)", IDi.getCompanyCode());
            return false;
        }

        // [カード基本情報] カード活性事業者コード≠ＩＣ運用マスタ_活性化事業者コードの場合、活性事業者判定ＮＧ
        if (activator == null) {
            Timber.e("IC運用マスタ 活性化事業者コードと不一致");
            return false;
        }

        // 小児券種受付判定
        if (!activator.allowChildTicket()) {
            int t = cardBasicInfoB3.getSFTicketTypeCode();

            if (t == Constants.TicketTypeCodes.CHILD || t == Constants.TicketTypeCodes.CHILD_DISCOUNT) {
                Timber.e("小児券種受付判定異常(券種コード:%d)", t);
                return false;
            }
        }

        return true;
    }

    /**
     * IC種別判定を行います
     *
     * @param IDi IDi
     * @param cardBasicInfoB3 カード基本情報ブロック3データ
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeICType(IDi IDi, CardBasicInfo.Block3 cardBasicInfoB3) {
        if (((IDi.getType() == 0 && cardBasicInfoB3.hasSF()) || IDi.getType() == 7) ==  false) {
            Timber.e("IC種別判定異常:ICカード種別(%d) SF機能(%s)", IDi.getType(), cardBasicInfoB3.hasSF());
        }
        return (IDi.getType() == 0 && cardBasicInfoB3.hasSF()) || IDi.getType() == 7;
    }

    /**
     * データ項目判定を行います
     *
     * @param cardBasicInfoB3 カード基本情報ブロック3
     * @param accessControlInfo アクセス制御情報
     * @param sfBalanceInfo SF残額情報
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeDataItem(
            CardBasicInfo.Block3 cardBasicInfoB3,
            AccessControlInfo accessControlInfo,
            SFBalanceInfo sfBalanceInfo,
            ICMaster.Activator activator
    ) {
        int t = cardBasicInfoB3.getSFTicketTypeCode();

        if (cardBasicInfoB3.getTypeCode() != 0) {
            Timber.e("機種コードエラー(種別コード:%d)", cardBasicInfoB3.getTypeCode());
            return false;
        }
        else if (!cardBasicInfoB3.useSFTransport()) {
            Timber.e("機種種別エラー(鉄道・バスでのSF利用可能フラグ:%s)", cardBasicInfoB3.useSFTransport());
            return false;
        }

        // パース上限金額エラー
        if (accessControlInfo.getPurseAmount() > activator.getPurseLimitAmount()) {
            Timber.e("パース上限金額エラー(アクセス制御情報:%s円 > IC運用マスタアクティベーター:%s円)",
                    accessControlInfo.getPurseAmount(), activator.getPurseLimitAmount());
            return false;
        }

        if (accessControlInfo.getPurseAmount() != sfBalanceInfo.getBalance()) {
            Timber.e("残額不一致エラー(アクセス制御情報:%s円 != SF残額情報:%s円)",
                    accessControlInfo.getPurseAmount(), sfBalanceInfo.getBalance());
            return false;
        }

        // SF残額情報パース上限金額エラー
        if (sfBalanceInfo.getBalance() > activator.getPurseLimitAmount()) {
            Timber.e("SF残額情報パース上限金額エラー(SF残額情報:%s円 > IC運用マスタアクティベーター:%s円)",
                    sfBalanceInfo.getBalance(), activator.getPurseLimitAmount());
            return false;
        }

        if (0 > t || t > 3) {
            Timber.e("SF券種コードエラー(券種コード:%d)", t);
            return false;
        }

        // 有効期限が必要な券種かつ有効期限が設定されていない場合
        if (t >= 1 && !cardBasicInfoB3.hasExpiration()) {
            Timber.e("有効期限エラー(券種コード:%d 年月日がオール0)", t);
            return false;
        }

        return true;
    }

    /**
     * 活性化判定を行います
     *
     * @param cardBasicInfoB3 カード基本情報ブロック3
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeActivation(CardBasicInfo.Block3 cardBasicInfoB3) {
        if ((cardBasicInfoB3.isActive() && !cardBasicInfoB3.isNonActive()) == false) { Timber.e("活性化判定異常(活性化フラグ:%s 非活性化フラグ:%s)", cardBasicInfoB3.isActive(), cardBasicInfoB3.isNonActive()); };
        return cardBasicInfoB3.isActive() && !cardBasicInfoB3.isNonActive();
    }

    /**
     * 10年失効判定を行います
     *
     * @param cardBasicInfo カード基本情報ブロック3
     * @param sfLogInfo SFログ情報
     * @param kaisatsuLogInfo 改札ログ情報
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judge10YearsUnused(
            CardBasicInfo.Block3 cardBasicInfo,
            SFLogInfo sfLogInfo,
            KaisatsuLogInfo kaisatsuLogInfo,
            ICMaster.Activator activator)
    {
        // 10年失効判定をしない場合は判定スキップ
        if (!activator.check10YearsExpired()) {
            return true;
        }

        boolean isValid = true;

        // カード活性化日チェック
        if (!OkicaDateUtils.validateDate(
                cardBasicInfo.getActivateYear(),
                cardBasicInfo.getActivateMonth(),
                cardBasicInfo.getActivateDate(),
                false))
        {
            Timber.e("カード基本情報日付異常 %s年%s月%s日",
                    cardBasicInfo.getActivateYear(),
                    cardBasicInfo.getActivateMonth(),
                    cardBasicInfo.getActivateDate());

            isValid = false;
        }

        // SFログ情報日付チェック
        if (!OkicaDateUtils.validateDate(
                sfLogInfo.getYear(),
                sfLogInfo.getMonth(),
                sfLogInfo.getDate(),
                true))
        {
            Timber.e("SFログ情報日付異常 %s年%s月%s日",
                    sfLogInfo.getYear(),
                    sfLogInfo.getMonth(),
                    sfLogInfo.getDate());

            isValid = false;
        }

        // 改札ログ情報日付チェック
        if (!OkicaDateUtils.validateDate(
                kaisatsuLogInfo.getYear(),
                kaisatsuLogInfo.getMonth(),
                kaisatsuLogInfo.getDate(),
                true))
        {
            Timber.e("改札ログ情報日付異常 %s年%s月%s日",
                    kaisatsuLogInfo.getYear(),
                    kaisatsuLogInfo.getMonth(),
                    kaisatsuLogInfo.getDate());
            isValid = false;
        }

        if (!isValid) {
            // カード内の日付情報がひとつでもおかしい場合、無条件で10年失効判定NGとする
            return false;
        }

        // 日付比較用
        final int calcSFLogDate = (sfLogInfo.getYear() << 16) +
                (sfLogInfo.getMonth() << 8) +
                sfLogInfo.getDate();

        final int  calcKaisatsuLogDate = (kaisatsuLogInfo.getYear() << 16) +
                (kaisatsuLogInfo.getMonth() << 8) +
                kaisatsuLogInfo.getDate();

        int lastYear = 0;
        int lastMonth = 0;
        int lastDate = 0;

        // 最終利用日判定
        if (calcSFLogDate == 0 && calcKaisatsuLogDate == 0) {
            Timber.d("10年失効判定最終利用日 カード活性化日採用");
            // SFログの日付=0 改札ログの日付=0 -> カード活性日を採用
            lastYear = cardBasicInfo.getActivateYear();
            lastMonth = cardBasicInfo.getActivateMonth();
            lastDate = cardBasicInfo.getActivateDate();
        }
        else if (calcSFLogDate > calcKaisatsuLogDate) {
            Timber.d("10年失効判定最終利用日 SFログ採用");
            // SFログの日付=最新日付 改札ログの日付=0 -> SFログの最新日付を採用
            // SFログの日付=最新日付　改札ログの日付=古い日付 -> SFログの最新日付を採用
            lastYear  = sfLogInfo.getYear();
            lastMonth = sfLogInfo.getMonth();
            lastDate  = sfLogInfo.getDate();
        }
        else {
            Timber.d("10年失効判定最終利用日 改札ログ採用");
            // SFログの日付=0 改札ログの日付=最新日付 -> 改札ログの最新日付を採用
            // SFログの日付=古い日付 改札ログの日付=最新日付 -> 改札ログの最新日付を採用
            // SFログの日付=改札ログの日付=最新日付 -> 改札ログの最新日付を採用
            lastYear  = kaisatsuLogInfo.getYear();
            lastMonth = kaisatsuLogInfo.getMonth();
            lastDate  = kaisatsuLogInfo.getDate();
        }

        final Calendar c = Calendar.getInstance();
        final TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        c.setTimeZone(tz);

        int nowFullYear = c.get(Calendar.YEAR);
        int lastFullYear = lastYear + (nowFullYear/100)*100;

        int judgeDate = OkicaDateUtils.get10yearsJudgeDate(lastYear, lastMonth, lastDate);
        int nowDate = OkicaDateUtils.nowDate();

        // 現在の西暦の上位桁 + 最終利用年が現在年より大きくなる場合は世紀またぎとみなす
        if (nowFullYear < lastFullYear) {
            nowDate += 100 << 16;
        }

        if ((judgeDate >= nowDate) == false) { Timber.e("10年失効判定異常(10年失効判定日付%02d%02d%02d > 現在日付%2d%02d%02d)",
                lastYear, lastMonth, lastDate,
                nowDate >> 16, (nowDate & 0x0000FF00) >> 8, nowDate & 0x000000FF); };

        return judgeDate >= nowDate;
    }

    /**
     * カード使用不可判定を行います
     *
     * @return true: 正常 false: 異常
     */
    private static boolean judgeCardUnavailable(AccessControlInfo accessControlInfo) {
        if (accessControlInfo.cardAvailable() == false) { Timber.e("カード使用不可判定異常(カード使用不可"); };
        return accessControlInfo.cardAvailable();
    }

    /**
     * カード有期間判定を行います
     *
     * @param cardBasicInfoB3 カード基本情報ブロック3
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeCardExpiration(CardBasicInfo.Block3 cardBasicInfoB3) {
        // カードの有効年月日の設定無し
        if (!cardBasicInfoB3.hasExpiration()) {
            return true;
        }

        int opDate = OkicaDateUtils.getOperationDate();
        int expireDate = (cardBasicInfoB3.getExpireYear() << 16) +
                (cardBasicInfoB3.getExpireMonth() << 8) +
                cardBasicInfoB3.getExpireDate();

        if ((opDate <= expireDate) == false) { Timber.e("カード有効期間判定異常(運用日付%02d%02d%02d <= 有効日付%2d%02d%02d)",
                opDate >> 16, (opDate & 0x0000FF00) >> 8, opDate & 0x000000FF,
                expireDate >> 16, (expireDate & 0x0000FF00) >> 8, expireDate & 0x000000FF); };

        return opDate <= expireDate;
    }

    /**
     * ネガチェック判定を行います
     *
     * @param auth カード相互認証データ
     *
     * @return 判定結果 true: 正常 false: 異常
     */
    private static boolean judgeNegaCheck(MutualAuthenticationRWSAM auth) {
        byte[] idi = auth.getIDi();
        if (true == OkicaNegaFile.checkNegaHit(idi)){
            Timber.e("ネガチェック判定(ネガヒット)");
            return false;
        }
        return true;
    }
}
