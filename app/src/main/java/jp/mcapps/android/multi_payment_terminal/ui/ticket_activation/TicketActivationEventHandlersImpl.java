package jp.mcapps.android.multi_payment_terminal.ui.ticket_activation;

import android.view.View;

import androidx.fragment.app.Fragment;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;

public class TicketActivationEventHandlersImpl implements TicketActivationEventHandlers{
    private final Fragment _fragment;

    public TicketActivationEventHandlersImpl(Fragment fragment) {
        _fragment = fragment;
    }
    @Override
    public void onCloseClick(View view, TicketActivationViewModel _ticketActivationViewModel) {
        CommonClickEvent.RecordClickOperation("閉じる", "チケット販売機能認証結果", false);
        _ticketActivationViewModel.stop();
        NavigationWrapper.navigateUp(view);
    }

    @Override
    public void onCancelClick(View view , TicketActivationViewModel _ticketActivationViewModel) {
        CommonClickEvent.RecordClickOperation("キャンセル", "チケット販売機能認証中", false);
        ConfirmDialog.newInstance("アクティベーションキャンセル", "アクティベーション処理をキャンセルしますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "チケット販売機能認証中キャンセル", false);
            _ticketActivationViewModel.stop();

            NavigationWrapper.navigateUp(view);
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "チケット販売機能認証中キャンセル", false);
        }).show(_fragment.getChildFragmentManager(), null);
    }
}
