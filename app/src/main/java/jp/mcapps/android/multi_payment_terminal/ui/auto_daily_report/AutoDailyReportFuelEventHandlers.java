package jp.mcapps.android.multi_payment_terminal.ui.auto_daily_report;
//ADD-S BMT S.Oyama 2024/11/21 フタバ双方向向け改修

import android.view.View;

public interface AutoDailyReportFuelEventHandlers {
    void onInputNumber(View view, String number);

    void onBackDelete(View view);

    void onRegist(View view);

    void onClear(View view);
}
//ADD-E BMT S.Oyama 2024/11/21 フタバ双方向向け改修
