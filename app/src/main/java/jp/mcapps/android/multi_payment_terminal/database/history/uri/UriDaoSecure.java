package jp.mcapps.android.multi_payment_terminal.database.history.uri;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.DemoDatabase;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.util.Crypto;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class UriDaoSecure extends UriDao {

    private final UriDao _dao;
    private final MainApplication _app;

    public UriDaoSecure(boolean isDemoMode) {
        _dao = isDemoMode ? DemoDatabase.getInstance().uriDao() : LocalDatabase.getInstance().uriDao();
        _app = MainApplication.getInstance();
    }

    //POS未送信の売上を取得
    @Override
    public List<UriData> getUnsentData(){
        List<UriData> uriDataList = _dao.getUnsentData();
        List<UriData> result = new ArrayList<>();

        for (UriData data : uriDataList) {
            data.cardId = decrypt(data.cardId, data.encryptType);
            data.icIdm = decrypt(data.icIdm, data.encryptType);
            data.waonIdm = decrypt(data.waonIdm, data.encryptType);

            result.add(data);
        }

        return result;
    }

    //未送信のクレジット売上データがないか確認
    @Override
    public Integer getUnsentCreditData(){
        return _dao.getUnsentCreditData();
    }

    //未送信売上データの件数を取得
    @Override
    public int getUnsentCnt() {
        return _dao.getUnsentCnt();
    }

    //全件削除
    @Override
    public void deleteAll(){
        _dao.deleteAll();
    }

    //POS送信済フラグがたっているデータを削除 ダミー決済データ
    @Override
    public void deletePosSentData(){
        _dao.deletePosSentData();
    }

    //売上履歴の挿入
    @Override
    public void insertUriData(UriData uriData){
        uriData.cardId = encrypt(uriData.cardId, uriData.encryptType);
        uriData.icIdm = encrypt(uriData.icIdm, uriData.encryptType);
        uriData.waonIdm = encrypt(uriData.waonIdm, uriData.encryptType);

        _dao.insertUriData(uriData);
    }

    //送信済みの売上データを削除し、紐づいた印字データを取消不可に変更
    @Override
    public void posSendCompleted(String date, int seq) {
        _dao.posSendCompleted(date, seq);
    }

    //一件削除 不正なブランド名が入っているデータ用
    @Override
    public void deleteUriDataList(List<UriData> dataList) {
        _dao.deleteUriDataList(dataList);
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
