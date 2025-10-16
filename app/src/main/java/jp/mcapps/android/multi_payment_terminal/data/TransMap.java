package jp.mcapps.android.multi_payment_terminal.data;

import java.util.HashMap;

public class TransMap {
    private static final HashMap<Integer, String> _typeMap = new HashMap<>();
    private static final HashMap<Integer, String> _resultMap = new HashMap<>();
    private static final HashMap<Integer, String> _resultDetailMap = new HashMap<>();

    //取引種別
    public static final int TYPE_SALES = 0; //売上
    public static final int TYPE_CANCEL = 1;    //取消
    public static final int TYPE_POINT = 2; //ポイント付与
    public static final int TYPE_POINT_CANCEL = 3;  //ポイント取消
    public static final int TYPE_CACHE_CHARGE = 4;  //現金チャージ
    public static final int TYPE_CACHE_CHARGE_CANCEL = 5;   //現金チャージ取消
    public static final int TYPE_POINT_CHARGE = 6;  //ポイントチャージ
    //ADD-S BMT S.Oyama 2024/12/02 フタバ双方向向け改修
    public static final int TYPE_PREPAID_CARDBUY  = 7;  //プリペイド発売
    //ADD-E BMT S.Oyama 2024/12/02 フタバ双方向向け改修
    //ADD-S BMT S.Oyama 2025/02/10 フタバ双方向向け改修
    //public static final int TYPE_ABORT            = 8;  //決済中止（中止ボタン押下，スキャンタイムアウト等）
    //ADD-E BMT S.Oyama 2025/01/10 フタバ双方向向け改修
    //取引結果
    public static final int RESULT_SUCCESS = 0; //成功
    public static final int RESULT_ERROR = 1;   //失敗
    public static final int RESULT_UNFINISHED = 2;  //未了
    public static final int RESULT_NEGA_CHECK_ERROR = 3;  //ネガチェックエラー
    //取引結果詳細
    public static final int DETAIL_NORMAL = 0;  //正常
    public static final int DETAIL_AUTH_RESULT_NG = 1;  //オーソリ結果NG
    public static final int DETAIL_AUTH_VERIFICATION_RESULT_NG = 2; //オーソリ検証結果NG
    public static final int DETAIL_UNFINISHED = 3;  //処理未了
    public static final int DETAIL_COMMUNICATION_FAILURE = 4;   //通信障害
    public static final int DETAIL_NEGA_CHECK_ERROR = 5;  //ネガチェックエラー

    public static String getType(int key) {
        String value = _typeMap.get(key);
        return value != null ? value : "";
    }

    public static String getResult(int key) {
        String value = _resultMap.get(key);
        return value != null ? value : "";
    }

    public static String getResultDetail(int key) {
        String value = _resultDetailMap.get(key);
        return value != null ? value : "";
    }

    static {
        _typeMap.put(TYPE_SALES, "売上");
        _typeMap.put(TYPE_CANCEL, "取消");
        _typeMap.put(TYPE_POINT, "ポイント付与");
        _typeMap.put(TYPE_POINT_CANCEL, "ポイント取消");
        _typeMap.put(TYPE_CACHE_CHARGE, "現金チャージ");
        _typeMap.put(TYPE_CACHE_CHARGE_CANCEL, "現金チャージ取消");
        _typeMap.put(TYPE_POINT_CHARGE, "ポイントチャージ");
        _typeMap.put(TYPE_PREPAID_CARDBUY, "プリペイド発売");

        _resultMap.put(RESULT_SUCCESS, "成功");
        _resultMap.put(RESULT_ERROR, "失敗");
        _resultMap.put(RESULT_UNFINISHED, "未了");
        _resultMap.put(RESULT_NEGA_CHECK_ERROR, "ネガ");

        _resultDetailMap.put(DETAIL_NORMAL, "正常");
        _resultDetailMap.put(DETAIL_AUTH_RESULT_NG, "オーソリ結果NG");
        _resultDetailMap.put(DETAIL_AUTH_VERIFICATION_RESULT_NG, "オーソリ検証結果NG");
        _resultDetailMap.put(DETAIL_UNFINISHED, "処理未了");
        _resultDetailMap.put(DETAIL_COMMUNICATION_FAILURE, "通信障害");
        _resultDetailMap.put(DETAIL_NEGA_CHECK_ERROR, "ネガチェックエラー");
    }
}
