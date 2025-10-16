package jp.mcapps.android.multi_payment_terminal.error;

import android.content.res.TypedArray;
import android.os.Build;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.RequiresApi;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import timber.log.Timber;

public class ErrorManage {
    private static ErrorManage _instance = null;
    private static final int POSITION_DISPLAY = 0;
    private static final int POSITION_HISTORY = 1;

    private ErrorStackingDao _stacking_dao;
    private ErrorDao _error_dao;

    // メンバ
    HashMap<String, ErrorData> _hashMap;
    HashMap<ErrorData, Integer[]> _hashMapDisplay;

    @RequiresApi(api = Build.VERSION_CODES.N)
    private ErrorManage() {
        // エラー文言の取得
        TypedArray typedArray = MainApplication.getInstance().getResources().obtainTypedArray(R.array.error_array);
        int error_num = typedArray.length();

        // hash mapの生成
        _hashMap = new HashMap<>(error_num);
        _hashMapDisplay = new HashMap<>(error_num);

        for (int i = 0; i < error_num; i++) {
            int identifier;
            String[] listStr;
            Integer[] listInteger = new Integer[2];
            ErrorData errorData = new ErrorData();
            String errorCode;

            identifier = typedArray.getResourceId(i, 0);
            listStr = MainApplication.getInstance().getResources().getStringArray(identifier);

            errorData.errorCode = listStr[0];

            // 表示用のタイトル（コード）は内部コードから変換したものを表示する
            errorCode = String.valueOf(Integer.parseInt(listStr[0]) % 10000);

            errorData.title = listStr[1] + errorCode;
            errorData.message = listStr[2];
            errorData.detail = listStr[3];
            errorData.level = Integer.parseInt(listStr[4]);

            _hashMap.put(errorData.errorCode, errorData);
            listInteger[POSITION_DISPLAY] = Integer.parseInt(listStr[5]);
            listInteger[POSITION_HISTORY] = Integer.parseInt(listStr[6]);
            _hashMapDisplay.put(errorData, listInteger);

            LocalDatabase db = LocalDatabase.getInstance();
            _error_dao = db.errorDao();
            _stacking_dao = db.errorStackingDao();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static ErrorManage getInstance() {
        if (_instance == null) {
            _instance = new ErrorManage();
        }
        return _instance;
    }

    public ErrorData getErrorData(String errorCode) {
        ErrorData errorData = _hashMap.get(errorCode);
        return errorData != null ? errorData.clone() : null;
    }

    public Integer getDisplayType(ErrorData errorData) {
        ErrorData data = _hashMap.get(errorData.errorCode);
        return _hashMapDisplay.get(data)[POSITION_DISPLAY];
    }

    public Integer getDisplayType(ErrorStackingData errorData) {
        ErrorData data = _hashMap.get(errorData.errorCode);
        return _hashMapDisplay.get(data)[POSITION_DISPLAY];
    }

    public Integer getHistoryVisible(ErrorData errorData) {
        ErrorData data = _hashMap.get(errorData.errorCode);
        return _hashMapDisplay.get(data)[POSITION_HISTORY];
    }

    // xmlの定義に関わらず強制的にスタッキングする
    public void stackingError(String errorCode) {
        Integer history_visible = Integer.parseInt(MainApplication.getInstance().getString(R.string.history_visible));
        Integer history_stacking = Integer.parseInt(MainApplication.getInstance().getString(R.string.history_stacking));

        // 詳細エラーコードを取り出す(未定義エラーの場合に付与)
        // <MCエラーコード>@@@<詳細エラーコード>@@@の形式
        final Pattern detailPtn = Pattern.compile("@@@(.|\n)*@@@$");
        final Matcher matcher = detailPtn.matcher(errorCode);

        // 詳細コードを取り出して前後の"@@@"を削除する
        final String detailCode = matcher.find()
                ? matcher.group().replaceAll("@@@", "")
                : "";

        // 詳細コード部分はエラーコードから削除する
        errorCode = matcher.replaceAll("");

        ErrorData errorData = getErrorData(errorCode);
        if (errorData == null) {
            Timber.d("error dialog errorCode not found: %s", errorCode);
            return;
        }

        if (getHistoryVisible(errorData) == history_visible) {
            // エラーを履歴に登録
            new Thread(() -> {
                // エラーをスタッキングに登録するためにすでに登録されているエラーコードか確認
                try {
                    Date date = new Date();

                    /* こことスタックエラー表示時で2重に履歴保存されていたのでコメントアウト
                    errorData.date = date;
                    errorData.detail = String.format(errorData.detail, detailCode);
                    _error_dao.insertErrorData(errorData);
                    Timber.d("insert finish");
                     */

                    // 送信用のログはスタックされるたびに作成
                    errorData.detail = String.format(errorData.detail, detailCode);
                    String message = String.format("MCエラーコード：%s", errorData.errorCode);
                    if (errorData.detail.contains("詳細コード：")){
                        message = message.concat(",").concat(errorData.detail.split("\n")[0]);
                    }
                    Timber.tag("エラー履歴").i(message);

                    ErrorStackingData data = _stacking_dao.getErrorStackingData(errorData.errorCode);

                    if (data == null) {
                        // 登録がないので新規登録
                        data = new ErrorStackingData();
                        data.errorCode = errorData.errorCode;
                        data.title = errorData.title;
                        data.message = errorData.message;
                        data.detail = errorData.detail;
                        data.level = errorData.level;

                        data.date = date;
                        _stacking_dao.insertErrorData(data);
                        Timber.d("stacking insert finish");
                    } else {
                        // 登録済み 日付を更新
                        _stacking_dao.updateErrorStackingData(data.id, date);
                        Timber.d("stacking update finish");
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            }).start();
        }
    }
}
