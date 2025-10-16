package jp.mcapps.android.multi_payment_terminal.ui.amount_input;
//ADD-S BMT S.Oyama 2024/08/27 フタバ双方向向け改修

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import timber.log.Timber;

import static java.lang.Integer.parseInt;

import android.os.Build;
import android.view.View;

@SuppressWarnings("ALL")
public class AmountInputSeparationPayFDViewModel extends ViewModel {
    private static final int MAX_AMOUNT_DIGITS = 6;

    public static class InputModes {
        public static final String SEPARATIONPAY = "SEPARATIONPAY";       //
        //public static final String FLAT_RATE = "FLAT_RATE";
    };

    public static class AmountInputConverters {

        public static Boolean isSelectedSeparationPay(String mode) {
            return mode == InputModes.SEPARATIONPAY;
        }

        //public static Boolean isSelectedFlatRate(String mode) {
        //    return mode == InputModes.FLAT_RATE;
        //}

        public static Boolean isNoSelected(String mode) {
            return mode == null;
        }
    }

    //分別払い表示モード
    public static final int  AMOUNTINPUT_SEPARATIONMODE_NONE    = 0;            //なし(あるいはエラー終了)
    public static final int  AMOUNTINPUT_SEPARATIONMODE_TICKET  = 1;            //チケット
    public static final int  AMOUNTINPUT_SEPARATIONMODE_CREDIT  = 2;            //クレジット
    public static final int  AMOUNTINPUT_SEPARATIONMODE_EMONEY  = 3;            //電子マネー
    public static final int  AMOUNTINPUT_SEPARATIONMODE_PREPAID = 4;            //プリペイド
    public static final int  AMOUNTINPUT_SEPARATIONMODE_QR      = 5;            //QR

    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_NONE      = 0;            //なし
    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_SUICA     = 1;            //SUICA
    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_ID        = 2;            //ID
    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_WAON      = 3;            //WAON
    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_EDY       = 4;            //EDY
    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_QUICPAY   = 5;            //QUICPAY
    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_NANACO    = 6;            //NANACO
    public static final int  AMOUNTINPUT_SEPARATIONEMONEYMODE_OKICA     = 7;            //OKICA

    //private final IFBoxManager _ifBoxManager;
    //public IFBoxManager getIfBoxManager() {
//        return _ifBoxManager;
//    }

    public static Disposable _meterDataV4InfoDisposable = null;
    public static Disposable _meterDataV4ErrorDisposable = null;

    public AmountInputSeparationPayFDViewModel() {
        //_ifBoxManager = ifBoxManager;
    }

    //InputMode
    private MutableLiveData<String> _inputMode = new MutableLiveData<>(InputModes.SEPARATIONPAY);
    public MutableLiveData<String> getInputMode() {
        return _inputMode;
    }
    public void setInputMode(String mode) {
        _inputMode.setValue(mode);
    }

    //JOBMODE
    private MutableLiveData<Integer> _jobMode = new MutableLiveData<>(AMOUNTINPUT_SEPARATIONMODE_NONE);
    public MutableLiveData<Integer> getJobMode() {
        return _jobMode;
    }
    public void setJobMode(int mode) {
        _jobMode.setValue(mode);
    }
    //選択された電子マネー種モード
    private MutableLiveData<Integer> _jobEmoneyMode = new MutableLiveData<>(AMOUNTINPUT_SEPARATIONEMONEYMODE_NONE);
    public MutableLiveData<Integer> getJobEmoneyMode() {
        return _jobEmoneyMode;
    }
    public void setJobEmoneyMode(int mode) {
        _jobEmoneyMode.setValue(mode);
    }

    //合計金額変更
//    private final MutableLiveData<Integer> _totalChangeAmount = new MutableLiveData<>(0);
//    public MutableLiveData<Integer> getTotalChangeAmount() { return _totalChangeAmount; }
//    public void setTotalChangeAmount(int amount) {
//        _totalChangeAmount.setValue(amount);
//    }
//    public void addTotalChange(int amount) {
//        _totalChangeAmount.setValue(_totalChangeAmount.getValue() + amount);
//    }

    //メーター料金
    private final MutableLiveData<Integer> _meterCharge = new MutableLiveData<>(Amount.getMeterCharge());
    public MutableLiveData<Integer> getMeterCharge() { return _meterCharge; }
    public void fetchMeterCharge() {
//        _ifBoxManager.fetchMeter()
//                .timeout(5, TimeUnit.SECONDS)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(() -> {
//                    _meterCharge.setValue(Amount.getMeterCharge());
//                }, error -> {
//                });
    }


    //分別料金
    private final MutableLiveData<Integer> _separationPayAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getSeparationPayAmount() { return _separationPayAmount; }
    public void setSeparationPayAmount(int amount) {
        _separationPayAmount.setValue(amount);
    }

    //入力金額変更
    private final MutableLiveData<Integer> _changeAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getChangeAmount() { return _changeAmount; }
    public void setChangeAmount(int amount) {
        _changeAmount.setValue(amount);
    }

//        Integer amount = _amountInputAdvancePayFDViewModel.getFlatRateAmount().getValue() > 0
//                ? _amountInputAdvancePayFDViewModel.getFlatRateAmount().getValue()
//                : _amountInputAdvancePayFDViewModel.getMeterCharge().getValue();

    private TransLogger _transLogger = null;


    { // initializer

        setSeparationPayAmount(0);
        _transLogger = new TransLogger();               //取引歴管理クラスのインスタンスを生成
    }

    /******************************************************************************/
    /*!
     * @brief  初期化処理(コンストラクタ代替)
     * @note   副次的な初期化処理を実施
     * @param [in] なし
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void constructorEmu()
    {
//        int tmpNowAmount = 0;
//        if (Amount.getFlatRateAmount() > 0) {
//            tmpNowAmount = Amount.getFlatRateAmount();
//        } else {
//            tmpNowAmount = Amount.getMeterCharge();
//        }

        int tmpJobMode = _jobMode.getValue();            //JOBモードを取得
        switch (tmpJobMode) {
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:
                //setSeparationPayAmount(Amount.getTicketAmount());
                setSeparationPayAmount(0);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:
                //setSeparationPayAmount(tmpNowAmount);
                setSeparationPayAmount(0);
                break;
            default:
                break;
        }

    }

    /******************************************************************************/
    /*!
     * @brief  確定金額が０以外か？
     * @note   確定金額が０以外か？
     * @param [in] なし
     * @retval なし
     * @return　boolean _separationPayAmount > 0
     * @private
     */
    /******************************************************************************/
    public boolean isSeparationPayAmountNotZero() {
        return _separationPayAmount.getValue() > 0;
    }

    /******************************************************************************/
    /*!
     * @brief  確定金額が元金額以上か？
     * @note   確定金額が元金額以上か？
     * @param [in] なし
     * @retval なし
     * @return　boolean
     * @private
     */
    /******************************************************************************/
    public boolean isSeparationPayAmountOrMore() {
        boolean result = false;

        int tmpNowAmount = 0;
        if (Amount.getFlatRateAmount() > 0) {
            tmpNowAmount = Amount.getFlatRateAmount() + Amount.getTotalChangeAmount();
        } else {
            tmpNowAmount = Amount.getMeterCharge() + Amount.getTotalChangeAmount();
        }

        int tmpSeparationPayAmount = _separationPayAmount.getValue();   //入力確定分金額

        int tmpJobMode = _jobMode.getValue();       //JOBモードを取得
        switch (tmpJobMode) {
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:
                result = tmpSeparationPayAmount > tmpNowAmount;
                break;
            default:
                result = true;
                break;
        }

        return result;
    }


    public void inputNumber(String stringNumber) {
        final int number = parseInt(stringNumber);
        final int currentVal = _changeAmount.getValue();

        final int shift = (int) Math.pow(10, stringNumber.length());

        final int changeAmount = AppPreference.isInput1yenEnabled()
                ? (currentVal*shift) + number
                : currentVal != 0 ? (currentVal + number)*shift : number * shift;

        if (!checkDigits(changeAmount)) {
            Timber.e("Out of range changeAmount");
            return;
        }

        setChangeAmount(changeAmount);
    }

    public void correct() {
        setChangeAmount(0);
    }


    /******************************************************************************/
    /*!
     * @brief  分割払い処理主処理
     * @note   分割払い時の金額確定処理を行う
     * @param [in] なし
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void SeparationPay()
    {
        final int changeAmount = _changeAmount.getValue();

        setSeparationPayAmount(changeAmount);
        setChangeAmount(0);

    }

    public void enter() {
        //Amount.setTotalChangeAmount(_totalChangeAmount.getValue());
        //Amount.setFlatRateAmount(_flatRateAmount.getValue());

        int tmpNowAmount = 0;

        int tmpSeparationPayAmount = _separationPayAmount.getValue();   //入力確定分金額   （例：100円

        int tmpJobMode = _jobMode.getValue();       //JOBモードを取得
        switch (tmpJobMode) {
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:
                Amount.setTicketAmount(tmpSeparationPayAmount);
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:
                if (Amount.getFlatRateAmount() > 0) {
                    tmpNowAmount = Amount.getFlatRateAmount() + Amount.getTotalChangeAmount();      //(例：300円
                    tmpNowAmount -= tmpSeparationPayAmount;                                         //(例：300円 - 100円 = 200円

                    Amount.setTotalChangeAmount(-tmpNowAmount);                                     //（例：-200円
                    Amount.setCashAmount(tmpNowAmount);                                             //（例：200円

                } else {
                    tmpNowAmount = Amount.getMeterCharge() + Amount.getTotalChangeAmount();
                    tmpNowAmount -= tmpSeparationPayAmount;

                    Amount.setTotalChangeAmount(-tmpNowAmount);
                    Amount.setCashAmount(tmpNowAmount);
                }

                Amount.fix();           //料金確定処理
                break;
            default:
                break;
        }


    }

    /******************************************************************************/
    /*!
     * @brief  分割払い処理キャンセル処理
     * @note   分割払い処理キャンセル処理を行う．金額を元に戻す
     * @param [in] なし
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void RollbackAmount()
    {
        int tmpNowAmount = 0;

        int tmpSeparationPayAmount = Amount.getTotalChangeAmount();   //分別で入力された金額

        int tmpJobMode = _jobMode.getValue();       //JOBモードを取得
        switch (tmpJobMode) {
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:
                if (Amount.getFlatRateAmount() > 0) {
                    Amount.setTotalChangeAmount(0);
                    Amount.setTicketAmount(0);
                } else {
                    Amount.setTotalChangeAmount(0);
                    Amount.setTicketAmount(0);
                }
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:
                if (Amount.getFlatRateAmount() > 0) {
                    Amount.setTotalChangeAmount(0);
                    Amount.setCashAmount(0);

                } else {
                    Amount.setTotalChangeAmount(0);
                    Amount.setCashAmount(0);
                }

                Amount.fix();           //料金確定処理
                break;
            default:
                break;
        }

    }

    /******************************************************************************/
    /*!
     * @brief  ８２０から送られてきた分別払い確定金額をセット
     * @note   ８２０から送られてきた分別払い確定金額をセット
     * @param [in] int tmpRecvAmount 820から送られてきた確定金額
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void SetFixAmountRecv820(int tmpRecvAmount)
    {
        int tmpNowAmount = 0;

        int tmpJobMode = _jobMode.getValue();       //JOBモードを取得
        switch (tmpJobMode) {
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:

                if (Amount.getFlatRateAmount() > 0) {

                    Amount.setTotalChangeAmount(-tmpRecvAmount);
                    Amount.setTicketAmount(tmpRecvAmount);

                } else {

                    Amount.setTotalChangeAmount(-tmpRecvAmount);
                    Amount.setTicketAmount(tmpRecvAmount);
                }
                break;
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:
                break;
            default:
                break;
        }

    }

    public void cancel() {
    }

    public void reset() {
        //setChangeHistory("");
        //setTotalChangeAmount(0);
        //setFlatRateAmount(0);
        setSeparationPayAmount(0);
    }

    public void changeBack() {
        final int currentAmount = _changeAmount.getValue();
        final int amount = AppPreference.isInput1yenEnabled()
                ? currentAmount/10
                : (currentAmount/100 != 0) ? (currentAmount/100)*10 : 0;

        setChangeAmount(amount);
    }

    public void apply() {
        final String mode = _inputMode.getValue();

        if (mode == null) return;
        else if (mode.equals(InputModes.SEPARATIONPAY))  SeparationPay();
        //else if (mode.equals(InputModes.FLAT_RATE)) flatRate();

//        _inputMode.setValue(null);
    }

    private boolean checkDigits(int amount) {
        return (amount / ((int) Math.pow(10, MAX_AMOUNT_DIGITS))) == 0;
    }

    /******************************************************************************/
    /*!
     * @brief チケット時　チケット金額を取引歴管理クラスに渡し，HistrySlipに記録する
     * @note
     * @param 無し
     * @retval なし
     * @return　int slipID
     * @private
     */
    /******************************************************************************/
    public int fixAmountRegistHistrySlipTicket()
    {
        if (_transLogger == null) {
            return -1;
        }

        Date exDate = new Date();   // 取引時間
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        String payTime = dateFmt.format(exDate);

        _transLogger.separateTicket(payTime, Amount.getTicketAmount());     //分別チケットモードで取引歴登録
        int tmpSlipID =  _transLogger.insert();
        return tmpSlipID;
    }

}

//        int tmpNowAmount = 0;
//
//        int tmpSeparationPayAmount = _separationPayAmount.getValue();   //入力確定分金額   （例：100円
//
//        int tmpJobMode = _jobMode.getValue();       //JOBモードを取得
//        switch (tmpJobMode) {
//            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET:

//                if (Amount.getFlatRateAmount() > 0) {
//                    // sute tmpNowAmount = Amount.getFlatRateAmount() + Amount.getTotalChangeAmount();      //(例：300円
//                    // sute tmpNowAmount -= tmpSeparationPayAmount;                                         //(例：300円 - 100円 = 200円
//
//                    Amount.setTotalChangeAmount(-tmpSeparationPayAmount);
//                    // sute Amount.setTotalChangeAmount(-tmpNowAmount);                                     //（例：-200円
//                    // sute Amount.setCashAmount(tmpNowAmount);                                             //（例：200円
//                    Amount.setTicketAmount(tmpSeparationPayAmount);
//
//                } else {
//                    // sute tmpNowAmount = Amount.getMeterCharge() + Amount.getTotalChangeAmount();
//                    // sute tmpNowAmount -= tmpSeparationPayAmount;
//
//                    Amount.setTotalChangeAmount(-tmpSeparationPayAmount);
//                    // sute Amount.setTotalChangeAmount(-tmpNowAmount);
//                    // sute Amount.setCashAmount(tmpNowAmount);
//                    Amount.setTicketAmount(tmpSeparationPayAmount);
//                }

//                //Amount.fix();           //料金確定処理

//                Amount.setSeparationPhase(1);                            //分割払いフェーズを設定  //1:分割払い中
//                break;
//            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_CREDIT:
//            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_EMONEY:
//            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_PREPAID:
//            case AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_QR:
//                if (Amount.getFlatRateAmount() > 0) {
//                    tmpNowAmount = Amount.getFlatRateAmount() + Amount.getTotalChangeAmount();      //(例：300円
//                    tmpNowAmount -= tmpSeparationPayAmount;                                         //(例：300円 - 100円 = 200円
//
//                    //Amount.setTotalChangeAmount(-tmpSeparationPayAmount);
//                    Amount.setTotalChangeAmount(-tmpNowAmount);                                     //（例：-200円
//                    Amount.setCashAmount(tmpNowAmount);                                             //（例：200円
//
//                } else {
//                    tmpNowAmount = Amount.getMeterCharge() + Amount.getTotalChangeAmount();
//                    tmpNowAmount -= tmpSeparationPayAmount;
//
//                    //Amount.setTotalChangeAmount(-tmpSeparationPayAmount);
//                    Amount.setTotalChangeAmount(-tmpNowAmount);
//                    Amount.setCashAmount(tmpNowAmount);
//                }
//
//                Amount.fix();           //料金確定処理
//                //Amount.setSeparationPhase(1);                            //分割払いフェーズを設定  //1:分割払い中
//                break;
//            default:
//                break;
//        }

//ADD-E BMT S.Oyama 2024/08/27 フタバ双方向向け改修

