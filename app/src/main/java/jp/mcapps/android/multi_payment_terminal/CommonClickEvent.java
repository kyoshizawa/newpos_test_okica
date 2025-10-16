package jp.mcapps.android.multi_payment_terminal;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationData;
import jp.mcapps.android.multi_payment_terminal.thread.InsertOperationThread;
import timber.log.Timber;

public abstract class CommonClickEvent {

    //タップ操作記録
    public static void RecordButtonClickOperation(View view, boolean isRecordDB) {
        OperationData operationData = new OperationData();

        //操作名
        if (view instanceof Button) {
            operationData.operationName = (String) ((Button) view).getText();
        }
        //Button以外(TextViewなど) or テキストなしButtonの場合
        if (operationData.operationName == null || operationData.operationName.equals("")) {
            operationData.operationName
                    = view.getTag() != null
                    ? (String)view.getTag() //tagがあれば取得
                    : view.getContext().getResources().getResourceEntryName(view.getId()); //tagがなければID名を取得
        }

        RecordClickOperation(operationData, isRecordDB);
    }

    //viewを取得できないタップ操作記録
    public static void RecordClickOperation(String operationName, boolean isRecordDB) {
        OperationData operationData = new OperationData();
        operationData.operationName = operationName;

        RecordClickOperation(operationData, isRecordDB);
    }

    //ダイアログ内のタップ操作
    public static void RecordClickOperation(String operationName, String dialogName, boolean isRecordDB) {
        OperationData operationData = new OperationData();
        operationData.operationName = operationName;
        operationData.dialogName = dialogName;

        RecordClickOperation(operationData, isRecordDB);
    }

    private static void RecordClickOperation(OperationData operationData, boolean isRecordDB) {
        operationData.operationDate = new Date();
        operationData.operationType = 0; //operationType 0 : タップ操作
        operationData.screenName = MainApplication.getInstance().getScreenData().getScreenName();

        if (isRecordDB) {
            InsertOperationThread thread = new InsertOperationThread(operationData);
            thread.start();
        }
        OutputIntoTimber(operationData);
    }

    //入力操作記録
    public static void RecordInputOperation(View view ,String inputData, boolean isRecordDB) {
        OperationData operationData = new OperationData();
        operationData.operationDate = new Date();
        operationData.operationType = 1; //operationType 1 : 入力内容を記録する入力操作

        if (view instanceof Button) {
            operationData.operationName = (String) ((Button) view).getText();
        }
        //Button以外(TextViewなど) or テキストなしButtonの場合
        if (operationData.operationName == null || operationData.operationName.equals("")) {
            operationData.operationName
                    = view.getTag() != null
                    ? (String)view.getTag() //tagがあれば取得
                    : view.getContext().getResources().getResourceEntryName(view.getId()); //tagがなければID名を取得
        }

        operationData.screenName = MainApplication.getInstance().getScreenData().getScreenName();
        operationData.inputData = inputData;

        if (isRecordDB) {
            InsertOperationThread thread = new InsertOperationThread(operationData);
            thread.start();
        }
        OutputIntoTimber(operationData);
    }

    //入力操作記録(桁数のみ)
    public static void RecordInputOperation(View view ,int inputDigitNumber, boolean isRecordDB) {
        OperationData operationData = new OperationData();
        operationData.operationDate = new Date();
        operationData.operationType = 2; //operationType 2 : 入力桁数を記録する入力操作

        if (view instanceof Button) {
            operationData.operationName = (String) ((Button) view).getText();
        }
        //Button以外(TextViewなど) or テキストなしButtonの場合
        if (operationData.operationName == null || operationData.operationName.equals("")) {
            operationData.operationName
                    = view.getTag() != null
                    ? (String)view.getTag() //tagがあれば取得
                    : view.getContext().getResources().getResourceEntryName(view.getId()); //tagがなければID名を取得
        }

        operationData.screenName = MainApplication.getInstance().getScreenData().getScreenName();
        operationData.inputDigitNumber = inputDigitNumber;

        if (isRecordDB) {
            InsertOperationThread thread = new InsertOperationThread(operationData);
            thread.start();
        }
        OutputIntoTimber(operationData);
    }

    //Timber出力
    private static void OutputIntoTimber (OperationData operationData) {
        String operationDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE).format(operationData.operationDate);

        //Timber出力 カンマ区切り、値がない場合はnull
        //操作履歴: 操作日時 画面名 ダイアログ名 操作名 入力内容 入力桁数
        Timber.tag("操作履歴").i("%s,%s,%s,%s,%s,%d",
                operationDate,
                operationData.screenName,
                operationData.dialogName,
                operationData.operationName,
                operationData.inputData,
                operationData.inputDigitNumber
        );
    }
}
