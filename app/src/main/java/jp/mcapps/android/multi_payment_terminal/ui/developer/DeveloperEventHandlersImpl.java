package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.DemoTransCredit;
import jp.mcapps.android.multi_payment_terminal.data.DemoTransEMoney;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioDao;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverDao;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDaoSecure;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDaoSecure;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.ui.history.DateTimePickerDialog;
import timber.log.Timber;

public class DeveloperEventHandlersImpl implements DeveloperEventHandlers {
    @Override
    public void onDatePickerClick(View view, Date date, int month) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        DateTimePickerDialog dialog = new DateTimePickerDialog(view.getContext(), date, month);
        dialog.showDialog();
    }

    @Override
    public void onAddDummyTransactionClick(View view, DeveloperAddDummyTransactionViewModel viewModel, SharedViewModel sharedViewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        String transCntStr = viewModel.getTransCnt().getValue();
        if (transCntStr == null){
            Timber.e("transCnt is empty");
            return;
        }
        int transCnt = Integer.parseInt(transCntStr);

        Integer transBrandNumber = viewModel.getTransBrand().getValue();
        if (transBrandNumber == null){
            Timber.e("transBrand is empty");
            return;
        }
        String[] transBrands = view.getResources().getStringArray(R.array.trans_brand);
        String transBrand = transBrands[transBrandNumber];

        sharedViewModel.setLoading(true);

        List<SlipData> slipDataList = new ArrayList<>();
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        Random rand = new Random();
        int termSequence = AppPreference.getTermSequence();

        for (int i = 0; i < transCnt; i++) {
            SlipData slipData;
            UriData uriData;

            termSequence = termSequence < 999 ? termSequence + 1 : 1;

            if (transBrand.equals("クレジット")) {
                DemoTransCredit transData = new DemoTransCredit();
                slipData = transData.getSlipData();
                uriData = transData.getUriData();
                //slipData.authId = rand.nextInt(9000) + 1000;
                slipData.printingAuthId = "A" + String.valueOf(rand.nextInt(9000) + 1000) + "Z";
                slipData.slipNumber = termSequence;   //伝票番号
            } else {
                DemoTransEMoney transData = new DemoTransEMoney();
                slipData = transData.getSlipData();
                uriData = transData.getUriData();
            }

            uriData.termSequence = termSequence;
            slipData.termSequence = termSequence;   //端末通番

            String transDate = dateFormat.format(date);
            slipData.transDate = transDate;
            uriData.transDate = transDate;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MINUTE, 5);
            date = calendar.getTime();

            slipData.cancelFlg = 1;

            new Thread(() -> {
                //ダミー決済はデモ用DBには入れない
                UriDaoSecure uriDao = new UriDaoSecure(false);
                SlipDaoSecure slipDao = new SlipDaoSecure(false);

                uriDao.insertUriData(uriData);
                slipDao.insertSlipData(slipData);
            }).start();
        }

        AppPreference.setTermSequence(termSequence);

        sharedViewModel.setLoading(false);
        Toast.makeText(view.getContext(), "ダミー決済を追加しました", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChangePeriodClick(View view, Date date, int type, int time) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(type, time);

        Intent intent = new Intent();
        intent.setAction("SEND_PICKED_DATE");
        intent.putExtra("date", calendar.getTimeInMillis());

        LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(intent);
    }

    @Override
    public void onDeleteHistoryClick(View view, String target) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        String dialogName = target + "削除確認";
        new AlertDialog.Builder(view.getContext())
                .setTitle("削除確認")
                .setMessage(target + "を削除します。\nよろしいですか？")
                .setPositiveButton("はい", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        CommonClickEvent.RecordClickOperation("はい", dialogName, true);
                        deleteHistory(view, target);
                    }
                })
                .setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        CommonClickEvent.RecordClickOperation("いいえ", dialogName, true);
                    }
                })
                .show();
    }

    private void deleteHistory(View view, String target) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LocalDatabase db = LocalDatabase.getInstance();
                if (target.equals("電波履歴")) {
                    RadioDao dao = db.radioDao();
                    dao.deleteAll();
                } else if (target.equals("GPS履歴")) {
                    GpsDao dao = db.gpsDao();
                    dao.deleteAll();
                }
            }
        });
        thread.start();

        try {
            thread.join();

            new AlertDialog.Builder(view.getContext())
                    .setTitle("削除成功")
                    .setMessage(target + "を削除しました")
                    .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            CommonClickEvent.RecordClickOperation("確認", target + "削除成功", true);
                        }
                    })
                    .show();

            //履歴表示のクリア　現在時刻でブロードキャスト
            Intent intent = new Intent();
            intent.setAction("SEND_PICKED_DATE");
            intent.putExtra("date", new Date().getTime());

            LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(intent);

        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(view.getContext())
                    .setTitle("削除失敗")
                    .setMessage(target + "の削除に失敗しました")
                    .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            CommonClickEvent.RecordClickOperation("確認", target + "削除失敗", true);
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onAddDummyDriverClick(View view, DeveloperAddDummyDriverViewModel viewModel) {
        CommonClickEvent.RecordButtonClickOperation(view, false);

        final LocalDatabase db = LocalDatabase.getInstance();
        final boolean isSuccess[] = {false};

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final DriverDao driverDao = db.driverDao();
                final DriverData driverData = new DriverData();

                driverData.driverCode = viewModel.getDriverCode().getValue();
                if (driverData.driverCode == ""){
                    Timber.e("driverCode is empty");
                    return;
                }

                driverData.driverName = viewModel.getDriverName().getValue();
                if (driverData.driverName == ""){
                    Timber.e("driverName is empty");
                    return;
                }

                final Date createdAt = viewModel.getCreatedAt().getValue();
                driverData.createdAt = createdAt != null ? createdAt : new Date();

                driverDao.addDriverHistory(driverData);

                isSuccess[0] = true;
                Timber.d("history_driver insert finish");
            }
        });
        thread.start();
        try {
            thread.join();
            if(isSuccess[0]) {
                Toast.makeText(view.getContext(), "ダミー乗務員を追加しました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(view.getContext(), "ダミー乗務員の追加に失敗しました", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
