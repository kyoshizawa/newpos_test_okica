package jp.mcapps.android.multi_payment_terminal.ui.pos_activation;

import android.view.View;


public interface PosActivationEventHandlers{
    void onCloseClick(View view , PosActivationViewModel posActivationViewModel);
    void onCancelClick(View view , PosActivationViewModel posActivationViewModel);
}
