package jp.mcapps.android.multi_payment_terminal.thread;

import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationDao;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationData;

public class InsertOperationThread extends Thread {
    private final OperationData _operationData;

    public InsertOperationThread(OperationData operationData) {
        this._operationData = operationData;
    }

    public void run() {
        LocalDatabase db = LocalDatabase.getInstance();
        OperationDao dao = db.operationDao();
        dao.insertOperationData(_operationData);
    }
}
