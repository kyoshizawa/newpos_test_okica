package jp.mcapps.android.multi_payment_terminal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jp.mcapps.android.multi_payment_terminal.data.ActionBarColors;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.discount.DiscountJobFutabaDViewModel;
import timber.log.Timber;

public class SharedViewModel extends ViewModel {
    public enum IndicatorArrows {
        NONE,
        WHITE,
        BLUE,
    }


    public enum ScreenMode {
        MAIN,
        POS
    }
    public enum ActionBarMode{
        MAIN,
        POS
    }
    private  Logger logger = Logger.getLogger("aaaa");

    private final Handler _uiHandler = new Handler(Looper.getMainLooper());
    private final MainApplication _app = MainApplication.getInstance();

    private final MutableLiveData<Boolean> _updatedFlag = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> getUpdatedFlag() { return _updatedFlag; }
    public void setUpdatedFlag(boolean b) { _updatedFlag.setValue(b); }

    private final MutableLiveData<Boolean> _backVisibleFlag = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> getBackVisibleFlag() { return _backVisibleFlag; }
    public void setBackVisibleFlag(boolean b) { _backVisibleFlag.setValue(b); }

    private final MutableLiveData<String> _driverCode = new MutableLiveData<String>("");
    public MutableLiveData<String> getDriverCodeEnteredFlag() { return _driverCode; }
    public void setDriverCode(String driverCode) { _driverCode.setValue(driverCode); }

    private final ObservableBoolean _isLoading = new ObservableBoolean(false);
    public ObservableBoolean isLoading() { return _isLoading; }
    public void setLoading(boolean b) { _isLoading.set(b); }

    private final ObservableBoolean _isScreenInversion = new ObservableBoolean(false);
    public ObservableBoolean isScreenInversion() { return _isScreenInversion; }
    public void setScreenInversion(boolean b) { _isScreenInversion.set(b); }

    private final MutableLiveData<Boolean> _cashMenu = new MutableLiveData<>(false);
    public LiveData<Boolean> getCashMenu() { return _cashMenu; }
    public void setCashMenu(boolean cashMenu) {
        _cashMenu.setValue(cashMenu);
        // setActionBarMode(_screenMode.getValue() , cashMenu);
    }

    // top barの制御 ※ default:Main
    private final MutableLiveData<ScreenMode> _screenMode = new MutableLiveData<>(ScreenMode.POS);
    public LiveData<ScreenMode> getScreenMode() {
        return _screenMode;
    }
    public void setScreenMode(ScreenMode mode) {
        _screenMode.setValue(mode);
        // setActionBarMode(mode , _cashMenu.getValue());
    }

    // TopBarの表示制御（true:表示 false:非表示）
    private final MutableLiveData<Boolean> _topBarView = new MutableLiveData<Boolean>(true);
    public MutableLiveData<Boolean> isTopBarView() { return _topBarView; }
    public void setTopBarView(boolean value) { _topBarView.setValue(value); }

    private ObservableBoolean _showBarTitleTicketClass = new ObservableBoolean(false);
    public ObservableBoolean getShowBarTitleTicketClass() { return _showBarTitleTicketClass; }
    public void setShowBarTitleTicketClass(boolean b) { _showBarTitleTicketClass.set(b); }

//    private final MutableLiveData<String> _barTitle = new MutableLiveData<String>("");
//    public MutableLiveData<String> getBarTitle() { return _barTitle; }
//    public void setBarTitle(String title) { _barTitle.setValue(title); }

    private ObservableBoolean _showBarTitleTicketEmbark = new ObservableBoolean(false);
    public ObservableBoolean getShowBarTitleTicketEmbark() { return _showBarTitleTicketEmbark; }
    public void setShowBarTitleTicketEmbark(boolean b) { _showBarTitleTicketEmbark.set(b); }

    private ObservableBoolean _showBarTitleTicketDisembark = new ObservableBoolean(false);
    public ObservableBoolean getShowBarTitleTicketDisembark() { return _showBarTitleTicketDisembark; }
    public void setShowBarTitleTicketDisembark(boolean b) { _showBarTitleTicketDisembark.set(b); }

    private ObservableBoolean _showBarTitleTicketIssue = new ObservableBoolean(false);
    public ObservableBoolean getShowBarTitleTicketIssue() { return _showBarTitleTicketIssue; }
    public void setShowBarTitleTicketIssue(boolean b) { _showBarTitleTicketIssue.set(b); }

    private ObservableBoolean _showBarTicketIssueCancel = new ObservableBoolean(false);
    public ObservableBoolean getShowBarTicketIssueCancel() { return _showBarTicketIssueCancel; }
    public void setShowBarTicketIssueCancel(boolean b) { _showBarTicketIssueCancel.set(b); }

    private final LiveData<ActionBarMode> _actionBarMode = createActionBarMode();

    public LiveData<ActionBarMode> getActionBarMode() {
        return _actionBarMode;
    }

    private LiveData<ActionBarMode> createActionBarMode() {
        final MediatorLiveData<ActionBarMode> result = new MediatorLiveData<>();
        final List<Object> listOfSources = new ArrayList<>();
        listOfSources.add(_screenMode);
        listOfSources.add(_cashMenu);
        for (Object source : listOfSources) {
            result.addSource((LiveData) source, (value) -> {
                ScreenMode screenMode = _screenMode.getValue();
                Boolean cashMenu = _cashMenu.getValue();
                ActionBarMode current = result.getValue();
                ActionBarMode next;
                if (screenMode == ScreenMode.MAIN && !cashMenu) {
                    next = ActionBarMode.MAIN;
                } else {
                    next = ActionBarMode.POS;
                }
                if (current != next) {
                    Timber.d("ActionBarMode: %s (screen: %s, cash: %s)", current, screenMode, cashMenu);
                    result.setValue(next);
                }
            });
        }
        result.setValue(ActionBarMode.MAIN);
        return result;
    }

    private ObservableBoolean _showIndicatorArrowWhite = new ObservableBoolean(false);
    public ObservableBoolean getShowIndicatorArrowWhite() { return _showIndicatorArrowWhite; }

    public ObservableBoolean _showIndicatorArrowBlue = new ObservableBoolean(false);
    public ObservableBoolean getShowIndicatorArrowBlue() { return _showIndicatorArrowBlue; }

    public void setIndicatorArrow(IndicatorArrows arrow) {
        _showIndicatorArrowWhite.set(arrow == IndicatorArrows.WHITE);
        _showIndicatorArrowBlue.set(arrow == IndicatorArrows.BLUE);
    }

    private ObservableBoolean _showMarkNfc = new ObservableBoolean(false);
    public ObservableBoolean getShowMarkNfc() { return _showMarkNfc; }
    public void setShowMarkNfc(boolean b) { _showMarkNfc.set(b); }
    private ObservableBoolean _showMarkFelica = new ObservableBoolean(false);
    public ObservableBoolean getShowMarkFelica() { return _showMarkFelica; }
    public void setShowMarkFelica(boolean b) { _showMarkFelica.set(b); }

    private Runnable _backAction = null;
    public Runnable getBackAction() {
        return _backAction;
    }
    // フラグメントから抜ける時に必ずnullにする
    public void setBackAction(Runnable backAction) {
        _backAction = backAction;
        Timber.i("[FUTABA-D]setBackAction(): is null %s", (backAction == null) ? "true" : "false");
    }

    // タブレットから送られてきた乗務員コードを反映するかどうかのフラグ
    private boolean _allowDriverSignIn = false;
    public boolean allowDriverSignIn() {
        return _allowDriverSignIn;
    }
    public void allowDriverSignIn(boolean b) {
        _allowDriverSignIn = b;
    }

    public void inverseScreen() {
        _isScreenInversion.set(!_isScreenInversion.get());
    }

    private MutableLiveData<Integer> _actionBarColor =
            new MutableLiveData<>(null);

    public MutableLiveData<Integer> getActionBarColor() {
        return _actionBarColor;
    }

    // ActionBarの色の変更がonDestroyViewで上書きされないようにするためのLock
    private boolean _isActionBarLock = false;
    public boolean isActionBarLock() {
        return _isActionBarLock;
    }
    public void isActionBarLock(boolean b) {
        _isActionBarLock = b;
    }

    public void setActionBarColor(ActionBarColors color) {
        _uiHandler.post(() -> {
            switch (color) {
                case Normal:
                    //_actionBarColor.setValue(AppPreference.isDemoMode() ? R.color.design_default_color_error : R.color.primary);
                    _actionBarColor.setValue(AppPreference.isDemoMode() ? R.color.design_default_color_error : (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) ? R.color.orange_500 : R.color.primary));
                    break;
                case Success:
                    _actionBarColor.setValue(R.color.bar_emoney_blue);
                    break;
                case Error:
                    _actionBarColor.setValue(R.color.bar_emoney_red);
                    break;
                case Unknown:
                    _actionBarColor.setValue(R.color.bar_yellow);
                    break;
            }
        });
    }

    public void navigateBack(View view) {
        NavigationWrapper.navigateUp(view);
    }

    public void navigateHome(View view) {
//        NavigationWrapper.navigateUp(view,R.id.action_menu);
    }

    //ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修
    public final int DISCOUNTJOB_MAXCOUNT = 5;                 //割引モードの最大数
    //割引モード １～５ 以下はサーバーより情報を受け取る
    public MutableLiveData<Integer> _discountJobMode = new MutableLiveData<>(DiscountJobFutabaDViewModel.DISCOUNTMODE_NONE);
    public MutableLiveData<Integer> getDiscountJobMode(){
        return _discountJobMode;
    }
    public void setDiscountJobMode(Integer mode){       //modeは0～4(配列インデックス)
        _discountJobMode.setValue(mode);
    }
    //割引モード使用中情報配列
    public MutableLiveData<Boolean[]> _discountJobActiveFl = new MutableLiveData<>(new Boolean[DISCOUNTJOB_MAXCOUNT]);
    public MutableLiveData<Boolean[]> getDiscountJobActiveFl(){
        return _discountJobActiveFl;
    }
    public void setDiscountJobActiveFl(Boolean[] tmpArray){_discountJobActiveFl.setValue(tmpArray);}
    //割引名称配列
    public MutableLiveData<String[]> _discountJobName = new MutableLiveData<>(new String[DISCOUNTJOB_MAXCOUNT]);
    public MutableLiveData<String[]> getDiscountJobName(){
        return _discountJobName;
    }
    public void setDiscountJobName(String[] tmpStr){_discountJobName.setValue(tmpStr);}

    //割引金額配列
    public MutableLiveData<Integer[]> _discountJobAmount = new MutableLiveData<>(new Integer[DISCOUNTJOB_MAXCOUNT]);
    public MutableLiveData<Integer[]> getDiscountJobAmount(){
        return _discountJobAmount;
    }
    public void setDiscountJobAmount(Integer[] tmpAmount){        _discountJobAmount.setValue(tmpAmount);    }

    // 割引種別
    public MutableLiveData<Integer[]> _discountJobType = new MutableLiveData<>(new Integer[DISCOUNTJOB_MAXCOUNT]);
    public MutableLiveData<Integer[]> getDiscountJobType() {
        return _discountJobType;
    }
    public void setDiscountJobType(Integer[] tmpType) { _discountJobType.setValue(tmpType); }
    //ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/11/27 フタバ双方向向け改修
    public static class  AutoDailyReport_MenuHierarchy {
        public static final int LEVEL0 = 0;             //業務メニューの階層
        public static final int LEVEL1 = 1;             //自動日報メニューの階層
        public static final int LEVEL2 = 2;             //自動日報子画面の階層
    }

    //自動日報メニューの階層遷移情報
    public MutableLiveData<Integer> _autoDailyReportMenuHierarchy = new MutableLiveData<>(AutoDailyReport_MenuHierarchy.LEVEL0);
    public MutableLiveData<Integer> getAutoDailyReportMenuHierarchy(){
        return _autoDailyReportMenuHierarchy;
    }
    public void setAutoDailyReportMenuHierarchy(Integer hierarchy){
        _autoDailyReportMenuHierarchy.setValue(hierarchy);
    }
    //ADD-E BMT S.Oyama 2024/11/27 フタバ双方向向け改修

}
