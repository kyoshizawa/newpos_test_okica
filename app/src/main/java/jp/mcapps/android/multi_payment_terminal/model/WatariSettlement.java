package jp.mcapps.android.multi_payment_terminal.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.pos.device.SDKException;
import com.pos.device.magcard.MagCardCallback;
import com.pos.device.magcard.MagCardReader;
import com.pos.device.magcard.MagneticCard;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.core.Single;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPoint;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPointCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.ValidationCheckApi;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.ValidationCheckApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data.CardValidation;
import timber.log.Timber;

public class WatariSettlement {
    public static class WatariException extends Throwable {
        private final int _reason;
        public int getReason() {
            return _reason;
        }
        public WatariException(int reason) {
            _reason = reason;
        }
    }

    // 追加のカード読み込みエラー
    public static class WatariCardError {
        public static final int READ_ERROR = 901;
    }

    public static class WatariResult {
        @NonNull
        public WatariPoint.Request addReq;
        public WatariPoint.Response addRes;
        public WatariPointCancel.Request cancelReq;
        public WatariPointCancel.Response cancelRes;
        public Throwable ex;
        public String payTime;
        public int amount;
    }

    private final MagCardReader _magCardReader = MagCardReader.getInstance();

    private final CreditSettlement _creditSettlement = CreditSettlement.getInstance();

    private McPosCenterApi _api = null; // デモモードでインスタンス生成させない

    private final Gson _gson = new Gson();

    public Single<String[]> searchCard() {
        return searchCard(30 * 1000);
    }

    public Single<String[]> searchCard(int timeoutMills) {
        Timber.d("start search card");
        return Single.create(emitter -> {
            // カード読み込み
            _magCardReader.startSearchCard(timeoutMills, (result, magneticCard) -> {
                if (result == MagCardCallback.SUCCESS) {
                    if (!emitter.isDisposed()) {

                        // 取得したストライプは解析する
                        String[] sData = new String[]{
                                magneticCard.getTrackInfos((MagneticCard.TRACK_1)).getData(),
                                magneticCard.getTrackInfos((MagneticCard.TRACK_2)).getData(),
                                magneticCard.getTrackInfos((MagneticCard.TRACK_3)).getData()
                        };

                        String jis1_1 = "";
                        String jis1_2 = "";
                        String jis2 = "";

                        // JIS1トラック1
                        if (0 < sData[0].length()) {
                            int idx = sData[0].indexOf("B");
                            if (idx == 0) {
                                // 1バイト目がFC（"B"）であればJIS1トラック1
                                // FC（"B"）は省いて保存
                                jis1_1 = sData[0].substring(idx + 1);
                                byte[] bData = jis1_1.getBytes();
                                jis1_1 = ISOUtil.hexString(bData);
                            } else {
                                // JIS2と判定
                                jis2 = sData[0];
                                byte[] bData = jis2.getBytes();
                                jis2 = ISOUtil.hexString(bData);
                            }
                        }

                        if (0 < sData[1].length()) {
                            int idx = sData[1].indexOf("=");
                            if (idx >= 0) {
                                // セパレータ（"="）が含まれていればJIS1トラック2
                                jis1_2 = sData[1];
                                byte[] bData = jis1_2.getBytes();
                                jis1_2 = ISOUtil.hexString(bData);
                            } else {
                                // JIS2と判定
                                jis2 = sData[1];
                                byte[] bData = jis2.getBytes();
                                jis2 = ISOUtil.hexString(bData);
                            }
                        }

                        // 最大サイズに満たない分は "0" で埋める
                        while (jis1_1.length() < (CreditSettlement.k_JIS1_TRACK1_SIZE * 2)) {
                            jis1_1 += "0";
                        }
                        while (jis2.length() < (CreditSettlement.k_JIS2_SIZE * 2)) {
                            jis2 += "0";
                        }
                        while (jis1_2.length() < (CreditSettlement.k_JIS1_TRACK2_SIZE * 2)) {
                            jis1_2 += "0";
                        }

                        emitter.onSuccess(new String[]{ jis1_1, jis1_2, jis2 });
                    }
                } else {
                    Timber.d("search card error. reason: %s", result);
                    if (!emitter.isDisposed()) {
                        emitter.onError(new WatariException(result));
                    }
                }
            });
        });
    }

    public Single<WatariResult> sendAddApi(String[] tracks) {
        return Single.create(emitter -> {
            // 通番は和多利専用のものを振る（1 ~ 999）
            int slipNo = AppPreference.getSlipNoWatari();
            String jis1Track1 = tracks[0];
            String jis1Track2 = tracks[1];
            String jis2 = tracks[2];

            // 結合したトラックデータ
            String resData = jis1Track1 + jis1Track2 + jis2;
            // 暗号化
            String rsaData = _creditSettlement._mcCenterCommManager.getEncryptData(resData);

            final WatariPoint.Request request = new WatariPoint.Request();
            request.moneyKbn = 0; // マネー区分（0:和多利）
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
            request.authDateTime = dateFmt.format(new Date()); // 操作日時
            request.fare = Amount.getFixedAmount(); // 売上金額
            request.terminalProcNo = slipNo; // 通番（伝票番号）
            request.payMethod = "10"; // 支払方法（固定）
            request.productCd = AppPreference.getProductCode(); // 商品コード
            request.driverCd = AppPreference.getMcDriverId(); // 乗務員コード
            request.keyType = _creditSettlement._mcCenterCommManager.getKeyTypeCredit(); // 鍵種別
            request.keyVersion = _creditSettlement._mcCenterCommManager.getKeyVerCredit(); // 鍵バージョン
            request.rsaData = rsaData; // 暗号化データ

            try {
                if (_api == null) {
                    _api = new McPosCenterApiImpl();
                }
                WatariPoint.Response response = _api.watariAdd(request);

                Timber.d("watari add result %s", _gson.toJson(response));
                Timber.i("ポイント付与のセンタ応答 ⇒ 付与ポイント数: %d, 累計ポイント数: %d", response.addPoint, response.sumPoint);

                final WatariResult result = new WatariResult() {
                    {
                        addReq = request;
                        addRes = response;
                        ex = null;
                        payTime = response.authDateTime;
                        amount = request.fare;
                    }
                };
                if (!emitter.isDisposed()) {
                    Timber.d("point add successful");
                    emitter.onSuccess(result);
                }

            } catch (Exception e) {
                Timber.d(e);
                final WatariResult result = new WatariResult() {
                    {
                        addReq = request;
                        addRes = null;
                        ex = e;
                        payTime = request.authDateTime;
                        amount = request.fare;
                    }
                };
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(result);
                }
            }
        });
    }

    public Single<WatariResult> sendCancelApi(String[] tracks, int cancelAmount, String cancelAuthDateTime, int cancelProcNo) {
        return Single.create(emitter -> {
            // 通番は和多利専用のものを振る（1 ~ 999）
            int slipNo = AppPreference.getSlipNoWatari();
            String jis1Track1 = tracks[0];
            String jis1Track2 = tracks[1];
            String jis2 = tracks[2];

            // 結合したトラックデータ
            String resData = jis1Track1 + jis1Track2 + jis2;
            // 暗号化
            String rsaData = _creditSettlement._mcCenterCommManager.getEncryptData(resData);

            final WatariPointCancel.Request request = new WatariPointCancel.Request();
            request.moneyKbn = 0; // マネー区分（0:和多利）
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
            request.authDateTime = dateFmt.format(new Date()); // 操作日時
            request.fare = cancelAmount; // 売上金額
            request.terminalProcNo = slipNo; // 通番（伝票番号）
            request.payMethod = "10"; // 支払方法（固定）
            request.productCd = AppPreference.getProductCode(); // 商品コード
            request.driverCd = AppPreference.getMcDriverId(); // 乗務員コード
            request.cancelAuthDateTime = cancelAuthDateTime;
            request.cancelProcNo = cancelProcNo;
            request.keyType = _creditSettlement._mcCenterCommManager.getKeyTypeCredit(); // 鍵種別
            request.keyVersion = _creditSettlement._mcCenterCommManager.getKeyVerCredit(); // 鍵バージョン
            request.rsaData = rsaData; // 暗号化データ

            try {
                if (_api == null) {
                    _api = new McPosCenterApiImpl();
                }
                WatariPointCancel.Response response = _api.watariCancel(request);

                Timber.d("watari cancel result %s", _gson.toJson(response));
                Timber.i("ポイント取消のセンタ応答 ⇒ 取消ポイント数: %d, 累計ポイント数: %d", response.addPoint, response.sumPoint);

                final WatariResult result = new WatariResult() {
                    {
                        cancelReq = request;
                        cancelRes = response;
                        ex = null;
                        payTime = response.authDateTime;
                        amount = request.fare;
                    }
                };
                if (!emitter.isDisposed()) {
                    Timber.d("point cancel successful");
                    emitter.onSuccess(result);
                }

            } catch (Exception e) {
                Timber.d(e);
                final WatariResult result = new WatariResult() {
                    {
                        cancelReq = request;
                        cancelRes = null;
                        ex = e;
                        payTime = request.authDateTime;
                        amount = request.fare;
                    }
                };
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(result);
                }
            }
        });
    }

    public void stopSearchCard() {
        try {
            if (_magCardReader != null) {
                _magCardReader.stopSearchCard();
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
    }

}
