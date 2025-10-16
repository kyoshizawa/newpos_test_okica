package jp.mcapps.android.multi_payment_terminal.database.history.uriOkica;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.DemoDatabase;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class UriOkicaDaoSecure extends UriOkicaDao {
    private final UriOkicaDao _dao;
    private final MainApplication _app;

    public UriOkicaDaoSecure(boolean isDemoMode) {
        // TODO m.kodama デモモード対応
        //_dao = isDemoMode ? DemoDatabase.getInstance().uriOkicaDao() : LocalDatabase.getInstance().uriOkicaDao();
        _dao = LocalDatabase.getInstance().uriOkicaDao();
        _app = MainApplication.getInstance();
    }

    // 未送信の売上データを取得
    @Override
    public List<UriOkicaData> getUnsentData() {
        List<UriOkicaData> uriOkicaDataList = _dao.getUnsentData();
        List<UriOkicaData> result = new ArrayList<>();

        for (UriOkicaData data : uriOkicaDataList) {
            data.okicaCardIdi = decrypt(data.okicaCardIdi, data.encryptType);

            result.add(data);
        }

        return result;
    }

    // 未送信売上データの件数を取得
    @Override
    public int getUnsentCnt() {
        return _dao.getUnsentCnt();
    }

    //送信フラグを送信済みに更新
    @Override
    public void setOkicaSent(String date, int seq) { _dao.setOkicaSent(date, seq); }

    //全件削除
    @Override
    public void deleteAll(){
        _dao.deleteAll();
    }

    // 送信済フラグがたっているデータを削除
    @Override
    public void deleteOkicaSentData(){
        _dao.deleteOkicaSentData();
    }

    // 売上履歴の挿入
    @Override
    public void insertUriData(UriOkicaData uriOkicaData){
        uriOkicaData.okicaCardIdi = encrypt(uriOkicaData.okicaCardIdi, uriOkicaData.encryptType);

        _dao.insertUriData(uriOkicaData);
    }

    // 送信済みの売上データを削除し、紐づいた印字データを取消不可に変更
    @Override
    public void posSendCompleted(String date, int seq) {
        _dao.posSendCompleted(date, seq);
    }

    private byte[] encrypt(byte[] plainText, int encryptType) {
        if (plainText == null) {
            return null;
        }

        switch (encryptType) {
            case 0:
                return plainText;
            case 1:
                return Crypto.AES128.encrypt(plainText, _app.getRoomAesKye());
            default:
                Timber.d("暗号化パターン不明：%d", encryptType);
                return plainText;
        }
    }

    private byte[] decrypt(byte[] encryptText, int encryptType) {
        if (encryptText == null) {
            return null;
        }

        switch (encryptType) {
            case 0:
                return encryptText;
            case 1:
                return Crypto.AES128.decrypt(encryptText, _app.getRoomAesKye());
            default:
                Timber.d("暗号化パターン不明：%d", encryptType);
                return encryptText;
        }
    }
}
