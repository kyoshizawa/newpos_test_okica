package jp.mcapps.android.multi_payment_terminal.ui.pos_activation;

import android.view.View;

import androidx.fragment.app.Fragment;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;

class PosActivationEventHandlersImpl implements PosActivationEventHandlers {
    private final Fragment _fragment;

    public PosActivationEventHandlersImpl(Fragment fragment) {
        _fragment = fragment;
    }
    @Override
    public void onCloseClick(View view, PosActivationViewModel _posActivationViewModel) {
        CommonClickEvent.RecordClickOperation("閉じる", "POS機能認証結果", false);
        _posActivationViewModel.stop();
        NavigationWrapper.navigateUp(view);
    }

    @Override
    public void onCancelClick(View view , PosActivationViewModel _posActivationViewModel) {
        CommonClickEvent.RecordClickOperation("キャンセル", "POS機能認証中", false);
        ConfirmDialog.newInstance("アクティベーションキャンセル", "アクティベーション処理をキャンセルしますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "POS機能認証中キャンセル", false);
            _posActivationViewModel.stop();

            NavigationWrapper.navigateUp(view);
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "POS機能認証中キャンセル", false);
        }).show(_fragment.getChildFragmentManager(), null);
    }


}
