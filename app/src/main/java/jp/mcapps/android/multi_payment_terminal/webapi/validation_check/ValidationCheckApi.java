package jp.mcapps.android.multi_payment_terminal.webapi.validation_check;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data.*;

public interface ValidationCheckApi {
    CardValidation.Response cardValidation(CardValidation.Request request) throws IOException;
    ProcessResult.Response processResult(ProcessResult.Request request) throws IOException;
}
