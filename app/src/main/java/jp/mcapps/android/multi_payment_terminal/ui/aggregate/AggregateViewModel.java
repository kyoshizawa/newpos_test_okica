package jp.mcapps.android.multi_payment_terminal.ui.aggregate;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDao;
import timber.log.Timber;

public class AggregateViewModel extends ViewModel {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<String> _aggregateStartDate = new MutableLiveData<>("");
    private final MutableLiveData<String> _aggregateEndDate = new MutableLiveData<>("");
    private final MutableLiveData<HashMap<String, AggregateListItem>> _aggregate = new MutableLiveData<>(null);
    private final MutableLiveData<AggregateListItem> _total = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> _unsentCnt = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> _unsentPosCnt = new MutableLiveData<>(null);

    public AggregateViewModel() {}

    public void aggregate() {
        HashMap<String, AggregateListItem> hashMap = new HashMap<>();
        AggregateDao aggregateDao = DBManager.getAggregateDao();
        UriDao uriDao = DBManager.getUriDao();
        UriOkicaDao uriOkicaDao = DBManager.getUriOkicaDao();
        SlipDao slipDao = DBManager.getSlipDao();
        TransactionDao transactionDao = DBManager.getTransactionDao();

        final Runnable run = () -> {
            AggregateListItem total = new AggregateListItem();

            List<SlipData> slipDataList = slipDao.getAggregate();

            // 現金併用分
            int togetherCash_sales = 0;
            int togetherCash_cancel = 0;

            for (SlipData slipData : slipDataList) {
                String transBrand = slipData.transBrand;
                //ToDo 銀聯他 STEP2対応

                AggregateListItem item = hashMap.get(transBrand);
                if (item == null) {
                    item = new AggregateListItem();
                }

                switch (slipData.transResult) {
                    case TransMap.RESULT_SUCCESS :
                        //成功
                        switch (slipData.transType) {
                            case TransMap.TYPE_SALES :
                                //売上
                                item.settlementCnt++;
                                item.subtotal += slipData.transAmount;
                                total.settlementCnt++;
                                total.subtotal += slipData.transAmount;
                                togetherCash_sales += slipData.transCashTogetherAmount;
                                break;
                            case TransMap.TYPE_CANCEL :
                                //取消
                                item.cancelCnt++;
                                item.subtotal -= slipData.transAmount;
                                total.cancelCnt++;
                                total.subtotal -= slipData.transAmount;
                                togetherCash_cancel += slipData.transCashTogetherAmount;
                                break;
                            default:
                                //ToDO その他の種別 STEP2対応
                                break;
                        }
                        break;
                    case TransMap.RESULT_UNFINISHED :
                        //処理未了
                        switch (slipData.transType) {
                            case TransMap.TYPE_SALES :
                                //売上
                                item.unfinishedCnt++;
                                item.unfinishedSubtotal += slipData.transAmount;
                                total.unfinishedCnt++;
                                total.unfinishedSubtotal += slipData.transAmount;
                                break;
                            case TransMap.TYPE_CANCEL :
                                //取消
                                item.unfinishedCnt++;
                                item.unfinishedSubtotal -= slipData.transAmount;
                                total.unfinishedCnt++;
                                total.unfinishedSubtotal -= slipData.transAmount;
                                break;
                            default:
                                //ToDO その他の種別 STEP2対応
                                break;
                        }
                        break;
                    case TransMap.RESULT_ERROR :
                        //失敗は集計しない
                        break;
                }

                hashMap.put(transBrand, item);
            }

            if(AppPreference.isServicePos()) {
                // 現金併用分を現金利用として集計する
                String transBrand = MainApplication.getInstance().getString(R.string.money_brand_cash);
                AggregateListItem item = hashMap.get(transBrand);
                if (item == null) {
                    item = new AggregateListItem();
                }
                item.subtotal = item.subtotal + togetherCash_sales - togetherCash_cancel;
                hashMap.put(transBrand, item);

                total.subtotal = total.subtotal + togetherCash_sales - togetherCash_cancel;
            }

            String dateString = aggregateDao.getCurrentAggregate().aggregateStartDatetime;
            Date aggregateStartDate = null;
            if (dateString != null) {
                SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
                try {
                    aggregateStartDate = stringToDate.parse(dateString);
                } catch (ParseException e) {
                    //ここには来ないはず
                    Timber.e("Date parse error");
                }
            }

            Date finalAggregateStartDate = aggregateStartDate;

            int unsentCnt = uriDao.getUnsentCnt() + uriOkicaDao.getUnsentCnt();
            int unsentPosCnt = transactionDao.getTransactionsUnUpload().length;

            handler.post(() -> {
                SimpleDateFormat dateToString = new SimpleDateFormat("M月d日H時m分s秒", Locale.JAPANESE);
                String startDateString = finalAggregateStartDate != null ? dateToString.format(finalAggregateStartDate) : ""; //業務開始日時の取得に失敗した場合は空欄
                _aggregateStartDate.setValue(startDateString);
                _aggregateEndDate.setValue(dateToString.format(new Date())); //業務終了前なので現在時刻を表示
                _aggregate.setValue(hashMap);
                _total.setValue(total);
                _unsentCnt.setValue(unsentCnt);
                _unsentPosCnt.setValue(unsentPosCnt);
            });
        };

        new Thread(run).start();
    }

    public MutableLiveData<String> getStartDate() {
        return _aggregateStartDate;
    }
    public MutableLiveData<String> getEndDate() {
        return _aggregateEndDate;
    }
    public MutableLiveData<HashMap<String, AggregateListItem>> getAggregate() {
        return _aggregate;
    }
    public MutableLiveData<AggregateListItem> getTotal() {
        return _total;
    }
    public MutableLiveData<Integer> getUnsentCnt() {
        return _unsentCnt;
    }

    public MutableLiveData<Integer> getUnsentPosCnt() {
        return _unsentPosCnt;
    }
}
