package jp.mcapps.android.multi_payment_terminal.model;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;

public class ErrorStackingRepository {

    private final ErrorStackingDao _errorStackingDao;

    public ErrorStackingRepository() {
        _errorStackingDao = LocalDatabase.getInstance().errorStackingDao();
    }

    public void removeErrorStacking(List<String> errorCodeList) {
        for (String errorCode : errorCodeList) {
            ErrorStackingData errorStackingData = _errorStackingDao.getErrorStackingData(errorCode);
            if (errorStackingData != null) {
                _errorStackingDao.deleteErrorStackingData(errorStackingData.id);
            }
        }
    }
}
