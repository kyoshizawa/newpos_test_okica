package jp.mcapps.android.multi_payment_terminal.database.validation_check;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.DemoValidationCheckDatabase;
import jp.mcapps.android.multi_payment_terminal.database.ValidationCheckDatabase;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class ValidationCheckDaoSecure extends ValidationCheckDao {
    private final ValidationCheckDao _dao;
    private final MainApplication _app = MainApplication.getInstance();

    public ValidationCheckDaoSecure(boolean isDemoMode) {
        _dao = isDemoMode ? DemoValidationCheckDatabase.getInstance().validationCheckDao() : ValidationCheckDatabase.getInstance().validationCheckDao();
    }

    //履歴を全件取得
    @Override
    public List<ValidationCheckHistoryData> getAllHistory() {
        List<ValidationCheckHistoryData> history = _dao.getAllHistory();
        List<ValidationCheckHistoryData> result = new ArrayList<>();

        for (ValidationCheckHistoryData data : history) {
            data.cardId = decrypt(data.cardId, data.encryptType);

            result.add(data);
        }

        return result;
    }

    //POS未送信の売上を取得
    @Override
    public List<ValidationCheckPaymentData> getUnsentPayment() {
        return _dao.getUnsentPayment();
    }

    @Override
    public List<ValidationCheckPaymentData> getUnsentPaymentWithLimit(int limit) {
        return _dao.getUnsentPaymentWithLimit(limit);
    }

    //POS未送信件数を取得
    @Override
    public int getUnsentCnt() {
        return _dao.getUnsentCnt();
    }

    //POS送信済の売上を削除
    @Override
    public void deletePosSentPayment() {
        _dao.deletePosSentPayment();
    }

    @Override
    public void deletePaymentById(long paymentId) {
        _dao.deletePaymentById(paymentId);
    }

    //指定した日時以前の履歴を削除
    @Override
    public void deleteOldHistory(String date) {
        _dao.deleteOldHistory(date);
    }

    //売上を挿入
    @Override
    public void insertPayment(ValidationCheckPaymentData payment) {
        _dao.insertPayment(payment);
    }

    //履歴を挿入
    @Override
    public void insertHistory(ValidationCheckHistoryData history) {
        history.cardId = encrypt(history.cardId, history.encryptType);
        _dao.insertHistory(history);
    }

    @Override
    public void posSent(long paymentId) {
        _dao.posSent(paymentId);
    }

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
                byte[] encryptBytes = McUtils.hexStringToBytes(encryptText);
                return new String(Crypto.AES128.decrypt(encryptBytes, _app.getRoomAesKye()));
            default :
                Timber.d("暗号化パターン不明：%d", encryptType);
                return encryptText;
        }
    }
}
