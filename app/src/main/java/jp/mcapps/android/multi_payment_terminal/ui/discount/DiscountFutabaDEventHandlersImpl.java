package jp.mcapps.android.multi_payment_terminal.ui.discount;
//ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修

import android.app.ProgressDialog;
import android.view.View;

import androidx.fragment.app.Fragment;

import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.ConfirmDialog;

public class DiscountFutabaDEventHandlersImpl implements DiscountFutabaDEventHandlers{
    private final Fragment _fragment;
    @SuppressWarnings("deprecation")
    ProgressDialog progressDialog;
    Thread thread;

    public DiscountFutabaDEventHandlersImpl(Fragment fragment) {
        _fragment = fragment;
    }
    @Override
    public void onTestClick(View view, DiscountFutabaDViewModel _discountFutabaDViewModel) {
        //CommonClickEvent.RecordClickOperation("閉じる", "割引確認結果", false);
        //_discountFutabaDViewModel.stop();
        //NavigationWrapper.navigateUp(view);

        switch(_discountFutabaDViewModel.getJobStatus().getValue())
        {
            case Started:
                _discountFutabaDViewModel.setJobStatus(DiscountFutabaDViewModel.JOBSTATUS.Connecting);
                break;
            case Connecting:
                _discountFutabaDViewModel.setJobStatus(DiscountFutabaDViewModel.JOBSTATUS.Error);
                break;
            case Error:
                _discountFutabaDViewModel.setJobStatus(DiscountFutabaDViewModel.JOBSTATUS.Finish);
                break;
            case Finish:
                testFinish(view);
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void testFinish(View view) {
        progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setTitle("通信中");
        progressDialog.setMessage("割引情報を通信中");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // todo
                try {
                    thread.sleep(5000);
                } catch (InterruptedException e) {

                }

                progressDialog.dismiss();
                progressDialog = null;

                view.post(() -> {
                    NavigationWrapper.navigateUp(view);
                });
            }
        }).start();
    }


    @Override
    public void onCancelClick(View view , DiscountFutabaDViewModel _discountFutabaDViewModel) {
        CommonClickEvent.RecordClickOperation("キャンセル", "割引確認中", false);
        ConfirmDialog.newInstance("割引確認キャンセル", "割引確認処理をキャンセルしますか？", () -> {
            CommonClickEvent.RecordClickOperation("はい", "割引確認キャンセル", false);
            //_discountFutabaDViewModel.stop();

            NavigationWrapper.navigateUp(view);
        }, () -> {
            CommonClickEvent.RecordClickOperation("いいえ", "割引確認キャンセル", false);
        }).show(_fragment.getChildFragmentManager(), null);
    }
}
//ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修
