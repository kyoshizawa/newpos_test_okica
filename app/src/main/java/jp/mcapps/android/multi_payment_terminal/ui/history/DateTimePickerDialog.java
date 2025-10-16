package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.TimePicker;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Calendar;
import java.util.Date;

public class DateTimePickerDialog implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener{
    private final Date _date;
    private final Context _context;
    private final Calendar _pickedDateTime;
    private final int _minMonth;

    public DateTimePickerDialog(Context context, Date date, int month) {
        this._context = context;
        this._date = date;
        _pickedDateTime = Calendar.getInstance();
        _minMonth = month;
    }

    public void showDialog() {
        //初期値 現在表示中の日付
        final Calendar c = Calendar.getInstance();
        c.setTime(_date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        //ダイアログのインスタンス化
        DatePickerDialog datePickerDialog = new DatePickerDialog(_context, android.R.style.Theme_Material_Light_Dialog_Alert , this,  year, month, day);
        //今日の日付に設定するボタンの追加
        //ここでクリックイベントを設定するとタップ時にダイアログが閉じるのでダイアログ生成後にオーバーライド
        datePickerDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "今日", (dialog, which) -> {});
        datePickerDialog.show();

        if (_minMonth < 0) {
            //保存期間を過ぎた日付を選択できないように最小値を設定
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.MONTH, _minMonth);
            minDate.set(Calendar.HOUR_OF_DAY, 0);
            minDate.set(Calendar.MINUTE, 0);
            minDate.set(Calendar.SECOND, 0);
            minDate.set(Calendar.MILLISECOND, 0);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        }

        //未来の日付を選択できないように最大値を設定
        Calendar maxDate = Calendar.getInstance();
        maxDate.set(Calendar.HOUR_OF_DAY, 0);
        maxDate.set(Calendar.MINUTE, 0);
        maxDate.set(Calendar.SECOND, 0);
        maxDate.set(Calendar.MILLISECOND, 0);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        //クリックイベントをオーバーライド
        Button todayButton = datePickerDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        todayButton.setOnClickListener(v -> {
            final Calendar today = Calendar.getInstance();
            datePickerDialog.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        });
    }

    @Override
    public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        _pickedDateTime.set(year, monthOfYear, dayOfMonth);
        showTimePickerDialog();
    }

    private void showTimePickerDialog() {
        //初期値 現在表示中の時刻
        final Calendar c = Calendar.getInstance();
        c.setTime(_date);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        //ダイアログのインスタンス化
        TimePickerDialog timePickerDialog = new TimePickerDialog(_context, android.R.style.Theme_Holo_Light_Dialog, this,  hour, minute, true);
        //現在時刻に設定するボタンの追加
        //ここでクリックイベントを設定するとタップ時にダイアログが閉じるのでダイアログ生成後にオーバーライド
        timePickerDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "現在時刻", (dialog, which) -> {});
        timePickerDialog.show();

        //クリックイベントをオーバーライド
        Button currentButton = timePickerDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        currentButton.setOnClickListener(v -> {
            final Calendar current = Calendar.getInstance();
            timePickerDialog.updateTime(current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE));
        });
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        _pickedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        _pickedDateTime.set(Calendar.MINUTE, minute);
        _pickedDateTime.set(Calendar.SECOND, 0);

        Intent intent = new Intent();
        intent.setAction("SEND_PICKED_DATE");
        intent.putExtra("date", _pickedDateTime.getTimeInMillis());
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }
}

