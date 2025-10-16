package jp.mcapps.android.multi_payment_terminal.ui.error;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.JremActivationErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.iCASErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import timber.log.Timber;

public class CommonErrorDialog {
    private static CommonErrorDialog _instance = null;
    private static ErrorDao _dao;
    private static ErrorStackingDao _stacking_dao;
    private CommonErrorEventHandlers _commonErrorEventHandlers;
    private ErrorStackingEventHandler _errorStackingEventHandler;
    private ErrorStackingData _errorStackingData;
    private List<ErrorStackingData> _listStackingData;
    private boolean bButtonPush = false;
    private AlertDialog _alertDialog = null;
    //ADD-S BMT S.Oyama 2024/12/19 フタバ双方向向け改修
    private AlertDialog _alertDialogCtrl = null;                        //アラートダイアログ制御用(フタバD勝手印刷用)
    //ADD-E BMT S.Oyama 2024/12/19 フタバ双方向向け改修

    static {
        LocalDatabase db = LocalDatabase.getInstance();
        _dao = db.errorDao();
        _stacking_dao = db.errorStackingDao();
    }

    public CommonErrorDialog() {
    }

//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public static CommonErrorDialog getInstance() {
//        if(_instance == null) {
//            _instance = new CommonErrorDialog();
//        }
//        _instance._commonErrorEventHandlers = null; //エラーイベントハンドラーの初期化
//        return _instance;
//    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void ShowErrorMessage(Context context, String errorCode) {
        Integer history_visible = Integer.parseInt(MainApplication.getInstance().getString(R.string.history_visible));
        Integer history_stacking = Integer.parseInt(MainApplication.getInstance().getString(R.string.history_stacking));
        Integer display_type_invisible = Integer.parseInt(MainApplication.getInstance().getString(R.string.error_dialog_invisible));
        ErrorManage errorManage = ErrorManage.getInstance();

        // 詳細エラーコードを取り出す(未定義エラーの場合に付与)
        // <MCエラーコード>@@@<詳細エラーコード>@@@の形式
        final Pattern detailPtn = Pattern.compile("@@@(.|\n)*@@@$");
        final Matcher matcher = detailPtn.matcher(errorCode);

        // 詳細コードを取り出して前後の"@@@"を削除する
        final String detailCode = matcher.find()
                ? matcher.group().replaceAll("@@@", "")
                : "";

        // 詳細コード部分はエラーコードから削除する
        errorCode = matcher.replaceAll("");

        // エラー情報を取得
        ErrorData errorData = errorManage.getErrorData(errorCode);

        if (errorData == null) {
            Timber.d("error dialog errorCode not found");
            return;
        }

        // エラー詳細に%sが含まれない場合は変換されない
        errorData.detail = String.format(errorData.detail, detailCode);

//        if(CreditErrorMap.getKeyByValue(errorCode) != null) {
//            errorData.detail = String.format(errorData.detail, CreditErrorMap.getKeyByValue(errorCode));
//        }
//        else if(JremRasErrorMap.getKeyByValue(errorCode) != null) {
//            errorData.detail = String.format(errorData.detail, JremRasErrorMap.getKeyByValue(errorCode));
//        }
//        else if(JremActivationErrorMap.getKeyByValue(errorCode) != null) {
//            errorData.detail = String.format(errorData.detail, JremActivationErrorMap.getKeyByValue(errorCode));
//        }
//        else if(iCASErrorMap.getKeyByValue(errorCode) != null) {
//            errorData.detail = String.format(errorData.detail, iCASErrorMap.getKeyByValue(errorCode));
//        }

        if (errorManage.getHistoryVisible(errorData) == history_visible) {
            // エラーを履歴に登録
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Date date = new Date();

                    errorData.date = date;
                    _dao.insertErrorData(errorData);
                    Timber.d("insert finish");

                    //端末ログとして送信するデータを作成
                    String message = String.format("MCエラーコード：%s", errorData.errorCode);
                    if (errorData.detail.contains("詳細コード：")){
                        message = message.concat(",").concat(errorData.detail.split("\n")[0]);
                    }
                    Timber.tag("エラー履歴").i(message);
                }
            }).start();
        } else if (errorManage.getHistoryVisible(errorData) == history_stacking) {
            // エラーをスタッキングに登録するためにすでに登録されているエラーコードか確認
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _errorStackingData = _stacking_dao.getErrorStackingData(errorData.errorCode);
                }
            });
            thread.start();
            try {
                thread.join(); //操作履歴を取得するまで待機
                if (_errorStackingData == null) {
                    // 登録がないので新規登録
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Date date = new Date();
                            _errorStackingData = new ErrorStackingData();
                            _errorStackingData.errorCode = errorData.errorCode;
                            _errorStackingData.title = errorData.title;
                            _errorStackingData.message = errorData.message;
                            _errorStackingData.detail = errorData.detail;
                            _errorStackingData.level = errorData.level;

                            _errorStackingData.date = date;
                            _stacking_dao.insertErrorData(_errorStackingData);
                            Timber.d("stacking insert finish");
                        }
                    }).start();
                } else {
                    // 既に登録されているので日付を変更して更新
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Date date = new Date();
                            _stacking_dao.updateErrorStackingData(_errorStackingData.id, date);
                            Timber.d("stacking update finish");
                        }
                    }).start();
                }
                // リストデータを更新する必要があるのでクリア
                _listStackingData.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Warning画像の表示イベント
            if (_errorStackingEventHandler != null) {
                _errorStackingEventHandler.onStacking();
            }
        }

        if(ErrorManage.getInstance().getDisplayType(errorData) != display_type_invisible) {
            showErrorDialog(context, errorData);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void ShowErrorStackMessage(Context context) {
        String btnNamePositive = MainApplication.getInstance().getString(R.string.error_button_positive);
        Integer display_type_positive = Integer.parseInt(MainApplication.getInstance().getString(R.string.error_dialog_positive));
        Integer level_error = Integer.parseInt(MainApplication.getInstance().getString(R.string.level_error));
        ErrorManage errorManage = ErrorManage.getInstance();

        if(_listStackingData == null || _listStackingData.size() == 0) {
            // stackingのテーブルから取得
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _listStackingData = _stacking_dao.getAll();
                }
            });
            thread.start();
            try {
                thread.join(); //操作履歴を取得するまで待機
                if(_listStackingData == null || _listStackingData.size() == 0) {
                    // 表示できるエラーなし
                    _errorStackingEventHandler.onStackClear();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ErrorStackingData _errorStackingData = (ErrorStackingData)_listStackingData.get(0);
        // アラートダイアログ表示
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(_errorStackingData.title)
                .setMessage(_errorStackingData.message + "\n\n" + _errorStackingData.detail);

        // エラー種別
        if(_errorStackingData.level == level_error) {
            // エラー表示
            builder.setIcon(R.drawable.ic_error);
        } else {
            // ワーニング表示
            builder.setIcon(R.drawable.ic_warning);
        }

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {          //フタバ双方向時 コールドスタートを実施（起動時初回１回　or 接続が切れるたび）
            if(_errorStackingData.errorCode.equals(MainApplication.getInstance().getString(R.string.error_type_FutabaD_FareUp_Warning)) &&
               errorManage.getDisplayType(_errorStackingData) == display_type_positive) {
                builder.setPositiveButton(btnNamePositive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (_commonErrorEventHandlers != null) {
                                    _commonErrorEventHandlers.onPositiveClick(_errorStackingData.errorCode);
                                }
                            }
                        });
            } else {
                builder.setPositiveButton(btnNamePositive, null);
            }
        } else {
            builder.setPositiveButton(btnNamePositive, null);
        }

        showAlertDialog(builder);

        // Stackテーブルから表示したエラーを削除
        new Thread(new Runnable() {
            @Override
            public void run() {
                Date date = new Date();
                _stacking_dao.deleteErrorStackingData(_errorStackingData.id);
                Timber.d("stacking delete finish");
            }
        }).start();

        // 表示したエラーをエラー履歴テーブルに登録
        new Thread(new Runnable() {
            @Override
            public void run() {
                ErrorData errorData = new ErrorData();
                errorData.errorCode = _errorStackingData.errorCode;
                errorData.title = _errorStackingData.title;
                errorData.message = _errorStackingData.message;
                errorData.detail = _errorStackingData.detail;
                errorData.level = _errorStackingData.level;
                errorData.date = _errorStackingData.date;

                _dao.insertErrorData(errorData);
                Timber.d("insert finish");

                //端末ログとして送信するデータを作成
                String message = String.format("MCエラーコード：%s", errorData.errorCode);
                if (errorData.detail.contains("詳細コード：")){
                    message = message.concat(",").concat(errorData.detail.split("\n")[0]);
                }
                Timber.tag("エラー履歴").i(message);
            }
        }).start();

        _listStackingData.remove(0);
        if(_listStackingData.size() == 0) {
            _errorStackingEventHandler.onStackClear();
        }
    }

    public Boolean isErrorStack() {
        Boolean isErrorStack = false;

        //if(_listStackingData == null || _listStackingData.size() == 0) {
        // stackingのテーブルから取得
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                _listStackingData = _stacking_dao.getAll();
            }
        });
        thread.start();
        try {
            thread.join(); //操作履歴を取得するまで待機
        } catch (Exception e) {
            e.printStackTrace();
        }
        //}

        if(_listStackingData != null && _listStackingData.size() != 0) {
            isErrorStack = true;
        }
        return isErrorStack;
    }

    public boolean hasError() {
        int level_error = Integer.parseInt(MainApplication.getInstance().getString(R.string.level_error));
        for (ErrorStackingData data : _listStackingData) {
            if (data.level == level_error) {
                return true;
            }
        }
        return false;
    }

    public void setCommonErrorEventHandlers(CommonErrorEventHandlers commonErrorEventHandlers) {
        _commonErrorEventHandlers = commonErrorEventHandlers;
    }

    public void setErrorStackingEventHandler(ErrorStackingEventHandler errorStackingEventHandler) {
        _errorStackingEventHandler = errorStackingEventHandler;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void showErrorDialog(Context context, ErrorData errorData) {
        String btnNamePositive = MainApplication.getInstance().getString(R.string.error_button_positive);
        String btnNameNagative = MainApplication.getInstance().getString(R.string.error_button_negative);
        String btnNameNeutral = MainApplication.getInstance().getString(R.string.error_button_neutral);
        Integer display_type_invisible = Integer.parseInt(MainApplication.getInstance().getString(R.string.error_dialog_invisible));
        Integer display_type_positive = Integer.parseInt(MainApplication.getInstance().getString(R.string.error_dialog_positive));
        Integer display_type_negative = Integer.parseInt(MainApplication.getInstance().getString(R.string.error_dialog_negative));
        Integer display_type_neutral = Integer.parseInt(MainApplication.getInstance().getString(R.string.error_dialog_neutral));
        Integer level_error = Integer.parseInt(MainApplication.getInstance().getString(R.string.level_error));
        ErrorManage errorManage = ErrorManage.getInstance();
        PrinterManager printerManager = PrinterManager.getInstance();
        Boolean isNoPrintingPaperLack = false;

        // プリンター用紙切れ発生
        if(errorData.errorCode.equals(MainApplication.getInstance().getString(R.string.error_type_printer_sts_paper_lack))){
            if(printerManager.getPrintStatus() != PrinterConst.PrintStatus_PAPERLACKING){
                // 印刷中のプリンター用紙切れ発生ではない
                isNoPrintingPaperLack = true;
            }
        }

        if(errorData.errorCode.equals(MainApplication.getInstance().getString(R.string.error_type_cashchanger_printer_connection_error)) ||
                errorData.errorCode.equals(MainApplication.getInstance().getString(R.string.error_type_cashchanger_connection_error))){
            isNoPrintingPaperLack = true;
        }

        bButtonPush = false;

        // アラートダイアログ表示
//        alertDialog.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(errorData.title)
                .setMessage(errorData.message + "\n\n" + errorData.detail);

        // エラー種別
        if(errorData.level == level_error) {
            // エラー表示
            builder.setIcon(R.drawable.ic_error);
        } else {
            // ワーニング表示
            builder.setIcon(R.drawable.ic_warning);
        }
        // 表示種別
        if(errorManage.getDisplayType(errorData) == display_type_positive || errorManage.getDisplayType(errorData) == display_type_invisible || isNoPrintingPaperLack == true) {
            // 確認表示
            builder.setPositiveButton(btnNamePositive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            bButtonPush = true;
                            if(_commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onPositiveClick(errorData.errorCode);
                            }
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // ダイアログが閉じられた際の処理
                            if(bButtonPush == false && _commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onDismissClick(errorData.errorCode);
                            }
                            //ADD-S BMT S.Oyama 2024/12/19 フタバ双方向向け改修
                            _alertDialogCtrl = null;
                            //ADD-E BMT S.Oyama 2024/12/19 フタバ双方向向け改修
                        }
                    });
        } else if(errorManage.getDisplayType(errorData) == display_type_negative) {
            // 確認、取消
            builder.setPositiveButton(btnNamePositive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            bButtonPush = true;
                            if(_commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onPositiveClick(errorData.errorCode);
                            }
                        }
                    })
                    .setNegativeButton(btnNameNagative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            bButtonPush = true;
                            if(_commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onNegativeClick(errorData.errorCode);
                            }
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // ダイアログが閉じられた際の処理
                            if(bButtonPush == false && _commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onDismissClick(errorData.errorCode);
                            }
                            //ADD-S BMT S.Oyama 2024/12/19 フタバ双方向向け改修
                            _alertDialogCtrl = null;
                            //ADD-E BMT S.Oyama 2024/12/19 フタバ双方向向け改修
                        }
                    });
        } else if(errorManage.getDisplayType(errorData) == display_type_neutral) {
            // 確認、取消、その他
            builder.setPositiveButton(btnNamePositive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            bButtonPush = true;
                            if(_commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onPositiveClick(errorData.errorCode);
                            }
                        }
                    })
                    .setNegativeButton(btnNameNagative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            bButtonPush = true;
                            if(_commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onNegativeClick(errorData.errorCode);
                            }
                        }
                    })
                    .setNeutralButton(btnNameNeutral, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            bButtonPush = true;
                            if(_commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onNeutralClick(errorData.errorCode);
                            }
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // ダイアログが閉じられた際の処理
                            if(bButtonPush == false && _commonErrorEventHandlers != null) {
                                _commonErrorEventHandlers.onDismissClick(errorData.errorCode);
                                //ADD-S BMT S.Oyama 2024/12/19 フタバ双方向向け改修
                                _alertDialogCtrl = null;
                                //ADD-E BMT S.Oyama 2024/12/19 フタバ双方向向け改修
                            }
                        }
                    });
        }

        showAlertDialog(builder);
    }

    public void showAlertDialog(AlertDialog.Builder builder) {
        builder.setCancelable(false);
        _alertDialog = builder.create();
        _alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        //ADD-S BMT S.Oyama 2024/12/19 フタバ双方向向け改修
        _alertDialogCtrl = _alertDialog;
        //ADD-E BMT S.Oyama 2024/12/19 フタバ双方向向け改修
        _alertDialog.show();

        // これをshow()の前でやるとエラーになる
        _alertDialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        _alertDialog.getWindow().
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    public void dismiss() {
        if(_alertDialog != null) _alertDialog.dismiss();
        _alertDialog = null;
    }

    //ADD-S BMT S.Oyama 2024/12/19 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  アラートダイアログの強制非表示処理（フタバ双方向用）
     * @note   アラートダイアログの強制非表示処理
     * @param 　なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void dismissAlertDialog()
    {
        if (_alertDialogCtrl != null) {
            _alertDialogCtrl.dismiss();
            _alertDialogCtrl = null;
        }
    }
    //ADD-E BMT S.Oyama 2024/12/19 フタバ双方向向け改修
}
