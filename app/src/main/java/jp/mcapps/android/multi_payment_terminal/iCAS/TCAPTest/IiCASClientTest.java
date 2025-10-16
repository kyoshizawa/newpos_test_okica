package jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest;

public interface IiCASClientTest {
    void OnTestFinished(int result, int returnCode, String errorMessage, String testName);
    void OnCancelAvailable();
    void OnCommunicationOff();
}
