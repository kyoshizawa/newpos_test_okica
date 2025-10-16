package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Locale;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import timber.log.Timber;

public class InputCartViewModel extends ViewModel {
    private static int _maxInputDigits = 0;
    private final long _delayMillis = 1000;
    private long _pushedMillis = 0;
    // 数値入力画面タイトル
    private final MutableLiveData<String> _title = new MutableLiveData<>("");
    public LiveData<String> getTitle() {
        return _title;
    }

    private final MutableLiveData<String> _originalNumber = new MutableLiveData<>("");

    public LiveData<String> getOriginalNumber() {
        if(_isInputTypePrice.getValue()) {
            // 単価入力の場合はカンマ区切り
            return Transformations.map(_originalNumber, input -> {
                if (input.length() == 0) {
                    return "";
                }
                return String.format(Locale.JAPANESE, "%,d", Integer.parseInt(input));
            });
        } else {
            // 数量入力の場合はそのまま
            return _originalNumber;
        }
    }


    public void setOriginalNumber(String value) {
        _originalNumber.setValue(value);
    }

    private final MutableLiveData<String> _inputNumber = new MutableLiveData<>("");

    public LiveData<String> getInputNumber() {
        if(_isInputTypePrice.getValue()) {
            // 単価入力の場合はカンマ区切り
            return Transformations.map(_inputNumber, input -> {
                if (input.length() == 0) {
                    return "";
                }
                return String.format(Locale.JAPANESE, "%,d", Integer.parseInt(input));
            });
        } else {
            // 数量入力の場合はそのまま
            return _inputNumber;
        }
    }

    private final MutableLiveData<Integer> _id = new MutableLiveData<>(0);

    public void setId(Integer id) {
        _id.setValue(id);
    }

    private Fragment inputFragment = null;

    public void setInputFragment(Fragment fragment) {
        inputFragment = fragment;
    }


    // 入力タイプに応じた設定
    private final MutableLiveData<Boolean> _isInputTypePrice = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsInputTypePrice() {
        return _isInputTypePrice;
    }
    public void setInputCartType(InputCartTypes value) {
        switch (value) {
            case COUNT:
                _title.setValue("数量の変更");
                _maxInputDigits = 3;
                _isInputTypePrice.setValue(false);
                break;
            case PRICE:
                _title.setValue("単価の変更");
                _maxInputDigits = 6;
                _isInputTypePrice.setValue(true);
                break;
            default:
                _title.setValue("UNKNOWN");
                _maxInputDigits = 0;
                _isInputTypePrice.setValue(false);
        }
    }

    // 数値入力
    public void inputNumber(String number) {
        CommonClickEvent.RecordClickOperation(number, true);
        if (Objects.requireNonNull(_inputNumber.getValue()).length() >= _maxInputDigits) {
            Timber.e("%s桁数入力オーバー: 入力値無視（%s）",_maxInputDigits ,number);
            return;
        }

        final String driverCode = _inputNumber.getValue() + number;
        int value = Integer.parseInt(driverCode);
        _inputNumber.setValue(Integer.toString(value));
    }

    // クリア
    public void correct() {
        CommonClickEvent.RecordClickOperation("クリア", true);
        _inputNumber.setValue("");
    }

    // 取消
    public void backDelete() {
        CommonClickEvent.RecordClickOperation("取消", true);
        String value = _inputNumber.getValue();
        if (value == null || value.length() == 0) {
            return;
        }
        _inputNumber.setValue(value.substring(0, value.length() - 1));
    }

    // 決定（数量の変更）
    public void enterCount() {
        CommonClickEvent.RecordClickOperation("決定", true);
        if (Objects.requireNonNull(_inputNumber.getValue()).length() == 0) {
            return;
        }

        // 連続タップ防止
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - _pushedMillis < _delayMillis)
        {
            Timber.e("１秒以内に連続タップ発生");
            return;
        }
        _pushedMillis = timeMillis;
        //Timber.d("push time millis:%s",_pushedMillis);

        Single.fromCallable(() -> {
                    // バックグラウンドスレッドでデータベース挿入を実行します
                    DBManager.getCartDao().updateCountById(_id.getValue(), Integer.parseInt(_inputNumber.getValue()));
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            // 挿入成功時の処理を記述します
                            // このコードはメインスレッドで実行されます
                            NavigationWrapper.navigate(inputFragment, R.id.action_inputCartFragment_to_cartConfirmFragment);
                            Timber.d("数量の変更　成功！！");
                        },
                        error -> {
                            // 挿入中に発生したエラーを処理します
                            // このコードはメインスレッドで実行されます
                            Timber.e(error);
                            _inputNumber.setValue("");
                            Timber.e("「処理失敗しました」表示");
                            Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                        }
                );
    }

    // 決定（単価の変更）
    public void enterUnitPrice() {
        CommonClickEvent.RecordClickOperation("決定", true);
        if (Objects.requireNonNull(_inputNumber.getValue()).length() == 0) {
            return;
        }

        // 連続タップ防止
        long timeMillis = System.currentTimeMillis();
        if (timeMillis - _pushedMillis < _delayMillis)
        {
            Timber.e("１秒以内に連続タップ発生");
            return;
        }
        _pushedMillis = timeMillis;
        //Timber.d("push time millis:%s",_pushedMillis);

        Single.fromCallable(() -> {
                    // バックグラウンドスレッドでデータベース挿入を実行します
                    DBManager.getCartDao().updateUnitPriceById(_id.getValue(), Integer.parseInt(_inputNumber.getValue()));
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            // 挿入成功時の処理を記述します
                            // このコードはメインスレッドで実行されます
                            NavigationWrapper.navigate(inputFragment, R.id.action_inputCartFragment_to_cartConfirmFragment);
                            Timber.d("単価の変更　成功！！");
                        },
                        error -> {
                            // 挿入中に発生したエラーを処理します
                            // このコードはメインスレッドで実行されます
                            Timber.e(error);
                            _inputNumber.setValue("");
                            Timber.e("「処理失敗しました」表示");
                            Toast.makeText(MainApplication.getInstance(), "処理失敗しました", Toast.LENGTH_LONG).show();
                        }
                );

    }
}
