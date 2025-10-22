package jp.mcapps.android.multi_payment_terminal.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlementAdapter;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;

/**
 * 電マネ等の支払い完了後の処理をMenuFragmentから分離(onViewCreated)
 */
public class PostPaymentProcess {

    //
    // ↓ シングルトン(static) ↓
    //

    static private PostPaymentProcess _instance;

    static public PostPaymentProcess getInstance() {
        if (_instance == null) {
            _instance = new PostPaymentProcess();
        }
        return _instance;
    }

    //
    // ↓ クラスの実装 ↓
    //

    private PostPaymentProcess() {}

    private final MainApplication _app = MainApplication.getInstance();

//    private final CreditSettlementAdapter creditListener = new CreditSettlementAdapter() {};

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void execute(Activity activity, Integer slipId) {
        final View view = activity.findViewById(R.id.fragment_main_nav_host);

        // 現金併用時の現金受け取り額を表示
        if (_app.getCashValue() > 0) {
            //ADD-S BMT S.Oyama 2025/03/05 フタバ双方向向け改修
            String tmpCashMesStr = "";
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
                tmpCashMesStr = "残金があります。\n領収書を印刷します。\n残金 %s円";
            }
            else {
                tmpCashMesStr = "以下の金額を\n現金で頂いて下さい。\n%s円";
            }
            //ADD-E BMT S.Oyama 2025/03/05 フタバ双方向向け改修
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    //ADDCHG-S BMT S.Oyama 2025/03/05 フタバ双方向向け改修
                    .setMessage(String.format(tmpCashMesStr, Converters.integerToNumberFormat(_app.getCashValue())))
                    //ADDCHG-E BMT S.Oyama 2025/03/05 フタバ双方向向け改修
                    .setCancelable(false)
                    .setPositiveButton("はい", (dialog, which) -> {
                        if (slipId != null && slipId != 0) {
                            printTrans(view, slipId);
                        }
                    });

            final AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            alertDialog.show();

            TextView message = alertDialog.findViewById(android.R.id.message);
            message.setTextSize(24);

            // これをshow()の前でやるとエラーになる
            alertDialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );

            alertDialog.getWindow().
                    clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            _app.setCashValue(0);
        } else {
            if (slipId != null && slipId != 0) {
                printTrans(view, slipId);
            }

            final String errorCode = _app.getErrorCode();
            if (errorCode != null) {
                if (errorCode.equals(_app.getString(R.string.error_type_okica_common_judge_nega_check_error))) {
                    // OKICAネガヒット時の売上送信
                    new Thread(() -> {
                        if (AppPreference.isOkicaCommunicationAvailable()) {
                            String mcTerminalErrCode = new McTerminal().postOkicaPayment();
                        }
                    }).start();
                }
                final CommonErrorDialog dialog = new CommonErrorDialog();
                dialog.ShowErrorMessage(activity, errorCode);

                _app.setErrorCode(null);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void printTrans(View view, int slipId) {
        //伝票印刷
        //CreditSettlement.getInstance().setListener(creditListener);
        final PrinterManager printerManager = PrinterManager.getInstance();
        printerManager.print_trans(view, slipId);

        //MC認証成功している場合は売上送信と疎通確認 エラーは無視する
        if (_app.isMcAuthSuccess()) {
            new Thread(() -> {
                final McTerminal mcTerminal = new McTerminal();
                mcTerminal.postPayment();
                mcTerminal.echo();
                // OKICA売上送信
                if (AppPreference.isOkicaCommunicationAvailable()) {
                    String mcTerminalErrCode = new McTerminal().postOkicaPayment();
                }

                if (AppPreference.isServiceTicket()) {
                    mcTerminal.postTicketCancel();
                }
            }).start();
        } else {
            new Thread(() -> {
                // OKICA売上送信
                if (AppPreference.isOkicaCommunicationAvailable()) {
                    String mcTerminalErrCode = new McTerminal().postOkicaPayment();
                }
            }).start();
        }
    }
}
