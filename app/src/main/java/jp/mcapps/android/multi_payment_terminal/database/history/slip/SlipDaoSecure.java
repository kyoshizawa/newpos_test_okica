package jp.mcapps.android.multi_payment_terminal.database.history.slip;

import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_CANCEL;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc.MANUALMODE_TRANS_TYPE_CODE_SALES;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.TransHead;
import jp.mcapps.android.multi_payment_terminal.database.DemoDatabase;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class SlipDaoSecure extends SlipDao {

    private final SlipDao _dao;
    private final MainApplication _app;

    public SlipDaoSecure(boolean isDemoMode) {
        _dao = isDemoMode ? DemoDatabase.getInstance().slipDao() : LocalDatabase.getInstance().slipDao();
        _app = MainApplication.getInstance();
    }

    //取引履歴一覧表示に必要なカラムを取得
    @Override
    public List<TransHead> getTransHead() {
        return _dao.getTransHead();
    }

    //最新の印字を1件取得
    @Override
    public SlipData getLatestOne() {
        SlipData data = _dao.getLatestOne();
        if (data != null) {
            data.cardIdCustomer = decrypt(data.cardIdCustomer, data.encryptType);
            data.cardIdMerchant = decrypt(data.cardIdMerchant, data.encryptType);
            if (data.authId != null && data.printingAuthId == null) {
                data.printingAuthId = data.authId.toString();
            }
        }

        return data;
    }

    //特定のIDを持つ印字を1件取得
    @Override
    public SlipData getOneById(int id) {
        SlipData data = _dao.getOneById(id);
        if (data != null) {
            data.cardIdCustomer = decrypt(data.cardIdCustomer, data.encryptType);
            data.cardIdMerchant = decrypt(data.cardIdMerchant, data.encryptType);
            if (data.authId != null && data.printingAuthId == null) {
                data.printingAuthId = data.authId.toString();
            }
        }

        return data;
    }

    //日計印字前の履歴を取得
    @Override
    public List<SlipData> getAggregate() {
        List<SlipData> slipDataList = _dao.getAggregate();
        List<SlipData> result = new ArrayList<>();

        for (SlipData data : slipDataList) {
            data.cardIdCustomer = decrypt(data.cardIdCustomer, data.encryptType);
            data.cardIdMerchant = decrypt(data.cardIdMerchant, data.encryptType);
            if (data.authId != null && data.printingAuthId == null) {
                data.printingAuthId = data.authId.toString();
            }
            result.add(data);
        }

        return result;
    }

    //日計印字順を指定して履歴を取得※取引結果：失敗を除く
    @Override
    public List<SlipData> getAggregate(int cnt) {
        List<SlipData> slipDataList = _dao.getAggregate(cnt);
        List<SlipData> result = new ArrayList<>();

        for (SlipData data : slipDataList) {
            data.cardIdCustomer = decrypt(data.cardIdCustomer, data.encryptType);
            data.cardIdMerchant = decrypt(data.cardIdMerchant, data.encryptType);
            if (data.authId != null && data.printingAuthId == null) {
                data.printingAuthId = data.authId.toString();
            }

            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
                // フタバ双方向の場合は手動モードでの取引分のみを印字対象とする（通常の取引分はメーターから印字するので対象外）
                if (data.transTypeCode != null && (data.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_SALES) || data.transTypeCode.equals(MANUALMODE_TRANS_TYPE_CODE_CANCEL))) {
                    result.add(data);
                }
            } else {
                result.add(data);
            }
        }

        return result;
    }

    //印字回数を更新
    @Override
    public void updatePrintCnt(int id) {
        _dao.updatePrintCnt(id);
    }

    //日計印字順が5のデータを削除
    @Override
    void deleteByOldAggregateOrder() {}

    @Override
    public void deleteSpecifiedData(String date, int seq) {
        _dao.deleteSpecifiedData(date, seq);
    }

    //全データの日計印字順をインクリメント
    @Override
    void updateOldAggregateOrder() {}

    //全データの日計印字順をインクリメント(POS用：キャンセルフラグは更新しない)
    @Override
    void updateOldAggregateOrderStayCancelFlg() {}

    //transTypeCodeを更新
    @Override
    public void updateTransTypeCode(int id, String code) { _dao.updateTransTypeCode(id, code); }

    //全件削除
    @Override
    public void deleteAll() {
        _dao.deleteAll();
    }

    //取消可能な取引を検索
    @Override
    Integer getLatestCancelableOrder() { return null; }

    //取消不可に変更
    @Override
    public void updateCancelUriId(int id) { _dao.updateCancelUriId(id); }

    //5回前の日計で印字したデータを削除し、その他のデータの日計印字順をインクリメント
    @Override
    public void updateTableAfterAggregate() {
        _dao.updateTableAfterAggregate();
    }

    //ひとつ前の取消を無効にして印字履歴を挿入
    @Override
    public long insertSlipData(SlipData slipData) {
        slipData.cardIdCustomer = encrypt(slipData.cardIdCustomer, slipData.encryptType);
        slipData.cardIdMerchant = encrypt(slipData.cardIdMerchant, slipData.encryptType);
        return _dao.insertSlipData(slipData);
    }

    //印字履歴の挿入
    @Override
    long insert(SlipData slipData) { return 0; }

    //カード番号マスク更新
    @Override
    public void updateMaskCardId(int id, String mask_card_id) {
        SlipData slipData = _dao.getOneById(id);
        mask_card_id = encrypt(mask_card_id, slipData.encryptType);
        _dao.updateMaskCardId(id, mask_card_id);
    }

    //取消可能な取引を検索
    @Override
    public List<Integer> getIds() { return _dao.getIds(); }

    private String encrypt(String plainText, int encryptType) {
        if (plainText == null) {
            return null;
        }

        switch (encryptType) {
            case 0 :
                return plainText;
            case 1 :
                return McUtils.bytesToHexString(Crypto.AES128.encrypt(plainText, _app.getRoomAesKye()));
            default :
                Timber.d("暗号化パターン不明：%d", encryptType);
                return plainText;
        }
    }

    private String decrypt(String encryptText, int encryptType) {
        if (encryptText == null) {
            return null;
        }

        switch (encryptType) {
            case 0 :
                return encryptText;
            case 1 :
                byte[] encryptByte = McUtils.hexStringToBytes(encryptText);
                return new String(Crypto.AES128.decrypt(encryptByte, _app.getRoomAesKye()));
            default :
                Timber.d("暗号化パターン不明：%d", encryptType);
                return encryptText;
        }
    }

    // チケット販売IDと便予約IDを設定更新※チケット販売のみ使用
    @Override
    public void updateTicketData(int id, String purchased_ticket_deal_id, String trip_reservation_id) {
        _dao.updateTicketData(id, purchased_ticket_deal_id, trip_reservation_id);
    }

    // 未送信のチケット購入の取消を全件取得
    @Override
    public List<SlipData> getUnsentCancelPurchasedTicketData() {
        List<SlipData> slipDataList = _dao.getUnsentCancelPurchasedTicketData();
        return slipDataList;
    }

    // チケット購入の取消を送信済み更新※ID指定
    @Override
    public void updateSentCancelPurchasedTicketData(int id) {
        _dao.updateSentCancelPurchasedTicketData(id);
    }
}
