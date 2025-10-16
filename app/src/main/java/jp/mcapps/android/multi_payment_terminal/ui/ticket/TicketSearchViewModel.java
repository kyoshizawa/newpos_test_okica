package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.content.res.Resources;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;

public class TicketSearchViewModel extends ViewModel {

    private Resources _resources = MainApplication.getInstance().getResources();
    private final MutableLiveData<Boolean> _isProcessing = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isProcessing() {
        return _isProcessing;
    }
    public void setProcessing(boolean b) {
        _isProcessing.setValue(b);
    }

    // チケット分類(名称)
    TicketClassData _ticketClassData = null;
    private final MutableLiveData<String> _nameTicketClass = new MutableLiveData<String>("");
    public MutableLiveData<String> getNameTicketClass() {

        if (AppPreference.getSelectedTicketClassData() == null) {
            // チケット分類(名称)が選択されていない場合
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _ticketClassData  = DBManager.getTicketClassDao().getInitTicketClassData();
                }
            });
            thread.start();

            try {
                thread.join();
                setNameTicketClass(_ticketClassData.ticket_class_name);
                AppPreference.setSelectedTicketClassData(_ticketClassData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(AppPreference.getSelectedTicketClassData().ticket_class_name != null) {
            // チケット分類(名称)が選択されている場合
            setNameTicketClass(AppPreference.getSelectedTicketClassData().ticket_class_name);
        }
        searchEnabledUpdate();
        return _nameTicketClass;
    }
    public void setNameTicketClass(String value) {
        _nameTicketClass.setValue(value);
    }

    // のりば(名称)
    private final MutableLiveData<String> _nameTicketEmbark = new MutableLiveData<String>("");
    public MutableLiveData<String> getNameTicketEmbark() {
        if (AppPreference.getSelectedTicketEmbarkData() == null) {
            // のりば(名称)が選択されていない場合
            setNameTicketEmbark(_resources.getString(R.string.text_ticket_embark_selection));
        } else {
            // のりば(名称)が選択されている場合
            setNameTicketEmbark(AppPreference.getSelectedTicketEmbarkData().stop_name);
        }
        return _nameTicketEmbark;
    }
    public void setNameTicketEmbark(String value) {
        _nameTicketEmbark.setValue(value);
    }
    public boolean isSelectedNameTicketEmbark() {
        return AppPreference.getSelectedTicketEmbarkData() != null;
    }

    // おりば(名称)
    private final MutableLiveData<String> _nameTicketDisembark = new MutableLiveData<String>("");
    public MutableLiveData<String> getNameTicketDisembark() {
        if (AppPreference.getSelectedTicketDisembarkData() == null) {
            // おりば(名称)が選択されていない場合
            if (AppPreference.getSelectedTicketEmbarkData() == null) {
                // のりば(名称)が選択されていない場合
                setNameTicketDisembark("");
            } else {
                // のりば(名称)が選択されている場合
                setNameTicketDisembark(_resources.getString(R.string.text_ticket_disembark_selection));
            }
        } else {
            // おりば(名称)が選択されている場合
            setNameTicketDisembark(AppPreference.getSelectedTicketDisembarkData().stop_name);
        }
        return _nameTicketDisembark;
    }
    public void setNameTicketDisembark(String value) {
        _nameTicketDisembark.setValue(value);
    }
    public boolean isSelectedNameTicketDisembark() {
        return AppPreference.getSelectedTicketDisembarkData() != null;
    }

    // 大人(人数)
    private final MutableLiveData<Integer> _countAdult = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getCountAdult() { return _countAdult; }
    public void minusCountAdult() {
        _countAdult.setValue(_countAdult.getValue() - 1);
    }
    public void plusCountAdult() {
        _countAdult.setValue(_countAdult.getValue() + 1);
    }

    // 小人(人数)
    private final MutableLiveData<Integer> _countChild = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getCountChild() { return _countChild; }
    public void minusCountChild() {
        _countChild.setValue(_countChild.getValue() - 1);
    }
    public void plusCountChild() {
        _countChild.setValue(_countChild.getValue() + 1);
    }

    // 乳幼児(人数)
    private final MutableLiveData<Integer> _countBaby = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getCountBaby() { return _countBaby; }
    public void minusCountBaby() {
        _countBaby.setValue(_countBaby.getValue() - 1);
    }
    public void plusCountBaby() {
        _countBaby.setValue(_countBaby.getValue() + 1);
    }

    // 障がい者 大人(人数)
    private final MutableLiveData<Integer> _countAdultDisability = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getCountAdultDisability() { return _countAdultDisability; }
    public void minusCountAdultDisability() {
        _countAdultDisability.setValue(_countAdultDisability.getValue() - 1);
    }
    public void plusCountAdultDisability() {
        _countAdultDisability.setValue(_countAdultDisability.getValue() + 1);
    }

    // 障がい者 小人(人数)
    private final MutableLiveData<Integer> _countChildDisability = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getCountChildDisability() { return _countChildDisability; }
    public void minusCountChildDisability() {
        _countChildDisability.setValue(_countChildDisability.getValue() - 1);
    }
    public void plusCountChildDisability() {
        _countChildDisability.setValue(_countChildDisability.getValue() + 1);
    }

    // 介助者(人数)
    private final MutableLiveData<Integer> _countCaregiver = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getCountCaregiver() { return _countCaregiver; }
    public void minusCountCaregiver() {
        _countCaregiver.setValue(_countCaregiver.getValue() - 1);
    }
    public void plusCountCaregiver() {
        _countCaregiver.setValue(_countCaregiver.getValue() + 1);
    }

    // 大人および障がい者　大人1名につき、乳幼児は1名まで乗船可能
    public boolean isBabyCheckBoarding() {
        return _countAdult.getValue() + _countAdultDisability.getValue() >= _countBaby.getValue();
    }

    // 障がい者　大人または障がい者　小人1名につき、介助者は1名まで乗船可能
    // PT750は制限なしで乗船可能
    public boolean isCaregiverCheckBoarding() {
        return true;
        // return _countAdultDisability.getValue() + _countChildDisability.getValue() >= _countCaregiver.getValue();
    }

    // 検索ボタンの活性化
    private final MutableLiveData<Boolean> _isSearchEnabled = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isSearchEnabled() {
        return _isSearchEnabled;
    }
    public void searchEnabledUpdate() {
        int count = _countAdult.getValue() + _countChild.getValue() + _countAdultDisability.getValue() + _countChildDisability.getValue();
        _isSearchEnabled.setValue(isSelectedNameTicketEmbark() && isSelectedNameTicketDisembark() && isBabyCheckBoarding() && isCaregiverCheckBoarding() && count > 0);
    }

    // 検索画面初期化結果
    private final MutableLiveData<Boolean> _isInitResult = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> isInitResult() { return _isInitResult; }
    public void isInitResult(boolean b) {
        _isInitResult.setValue(b);

        if (!b) {
            setErrorCode(MainApplication.getInstance().getString(R.string.error_type_ticket_8098));
            setErrorMessage(MainApplication.getInstance().getString(R.string.error_message_ticket_8098));
            setErrorMessageInformation(MainApplication.getInstance().getString(R.string.error_detail_ticket_8098));
        }
    }

    // エラーコード
    private final MutableLiveData<String> _errorCode = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorCode() { return _errorCode; }
    public void setErrorCode(String msg) {
        _errorCode.setValue("コード：" + msg);
    }

    // エラーメッセージ
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorMessage() { return _errorMessage; }
    public void setErrorMessage(String msg) {
        _errorMessage.setValue(msg);
    }

    // エラーメッセージ(補足)
    private final MutableLiveData<String> _errorMessageInformation = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorMessageInformation() { return _errorMessageInformation; }
    public void setErrorMessageInformation(String msg) {
        _errorMessageInformation.setValue(msg);
    }

    public void clear(){
        _nameTicketClass.setValue("");
        _nameTicketEmbark.setValue("");
        _nameTicketDisembark.setValue("");
        _countAdult.setValue(0);
        _countChild.setValue(0);
        _countBaby.setValue(0);
        _countAdultDisability.setValue(0);
        _countChildDisability.setValue(0);
        _countCaregiver.setValue(0);
    }
}
