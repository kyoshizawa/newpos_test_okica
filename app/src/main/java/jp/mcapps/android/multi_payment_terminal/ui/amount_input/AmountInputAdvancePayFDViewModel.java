package jp.mcapps.android.multi_payment_terminal.ui.amount_input;
//ADD-S BMT S.Oyama 2024/08/27 フタバ双方向向け改修

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import timber.log.Timber;

import static java.lang.Integer.parseInt;

@SuppressWarnings("ALL")
public class AmountInputAdvancePayFDViewModel extends ViewModel {
    private static final int MAX_AMOUNT_DIGITS = 6;

    public static class InputModes {
        public static final String ADVANCEPAY = "ADVANCEPAY";
        public static final String FLAT_RATE = "FLAT_RATE";
        public static final String SEPARATION_TICKET = "SEPARATION_TICKET";
        public static final String SEPARATION_CASH = "SEPARATION_CASH";
    };

    public static class AmountInputConverters {
        public static Boolean isSelectedAdvancePay(String mode) {
            return mode == InputModes.ADVANCEPAY;
        }

        public static Boolean isSelectedFlatRate(String mode) {
            return mode == InputModes.FLAT_RATE;
        }

        public static Boolean isNoSelected(String mode) {
            return mode == null;
        }

        public static Boolean isSelectedAdvancePayAndNot0(String mode, int amount) {
            boolean isAdvancePay = (mode == InputModes.ADVANCEPAY);
            boolean isNot0 = (amount != 0);
            return isAdvancePay && isNot0;
        }

        public static Boolean isSelectedFlatRateNot0(String mode, int amount) {
            boolean isFlatRate = (mode == InputModes.FLAT_RATE);
            boolean isNot0 = (amount != 0);
            return isFlatRate && isNot0;
        }

    }

    private final IFBoxManager _ifBoxManager;
    public IFBoxManager getIfBoxManager() {
        return _ifBoxManager;
    }

    public static Disposable _meterDataV4InfoDisposable = null;
    public static Disposable _meterDataV4ErrorDisposable = null;
    //ADD-S BMT S.Oyama 2025/02/27 フタバ双方向向け改修
    public static Disposable _meterDataV4InfoDisposableAdvance = null;
    public static Disposable _meterDataV4ErrorDisposableAdvance = null;
    //ADD-E BMT S.Oyama 2025/02/27 フタバ双方向向け改修

    public AmountInputAdvancePayFDViewModel(IFBoxManager ifBoxManager) {
        _ifBoxManager = ifBoxManager;
    }

    //InputMode
    private MutableLiveData<String> _inputMode = new MutableLiveData<>(InputModes.FLAT_RATE);
    public MutableLiveData<String> getInputMode() {
        return _inputMode;
    }
    public void setInputMode(String mode) {
        _inputMode.setValue(mode);
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

    // 確定金額表示欄のラベル
    private MutableLiveData<String> _confAmountAreaLabel = new MutableLiveData<>(MainApplication.getInstance().getString(R.string.text_amount_input_flatrate));
    public MutableLiveData<String> getConfAmountAreaLabel() {
        return _confAmountAreaLabel;
    }
    public void setConfAmountAreaLabel(String label) {
        _confAmountAreaLabel.setValue(label);
    }

    //メーター料金
    private final MutableLiveData<Integer> _meterCharge = new MutableLiveData<>(Amount.getMeterCharge());
    public MutableLiveData<Integer> getMeterCharge() { return _meterCharge; }
    public void fetchMeterCharge() {
        _ifBoxManager.fetchMeter()
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    _meterCharge.setValue(Amount.getMeterCharge());
                }, error -> {
                });
    }

    //定額料金
    private final MutableLiveData<Integer> _flatRateAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getFlatRateAmount() { return _flatRateAmount; }
    public void setFlatRateAmount(int amount) {
        _flatRateAmount.setValue(amount);
        _fixAmount.setValue(_flatRateAmount.getValue());
    }
    //立替料金
    private final MutableLiveData<Integer> _advancePayAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getAdvancePayAmount() { return _advancePayAmount; }
    public void setAdvancePayAmount(int amount) {
        _advancePayAmount.setValue(amount);
    }
    public void addAdvancePayAmount(int amount) {
        _advancePayAmount.setValue(_advancePayAmount.getValue() + amount);
        _fixAmount.setValue(_advancePayAmount.getValue());
    }
    //分別料金
    private final MutableLiveData<Integer> _separationPayAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getSeparationPayAmount() { return _separationPayAmount; }
    public void setSeparationPayAmount(int amount) {
        _separationPayAmount.setValue(amount);
        _fixAmount.setValue(_separationPayAmount.getValue());
    }
    //金額入力時の変数
    private final MutableLiveData<Integer> _changeAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getChangeAmount() { return _changeAmount; }
    public void setChangeAmount(int amount) {
        _changeAmount.setValue(amount);
    }
    //確定金額
    private final MutableLiveData<Integer> _fixAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getFixAmount() { return _fixAmount; }
    public void setFixAmount(int amount) {
        _fixAmount.setValue(amount);
    }

    private TransLogger _transLogger = null;

    { // initializer
        setFlatRateAmount(Amount.getFlatRateAmount());
        setAdvancePayAmount(Amount.getTotalChangeAmount());
        setSeparationPayAmount(0);
        _transLogger = new TransLogger();               //取引歴管理クラスのインスタンスを生成
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

        final String mode = _inputMode.getValue();
        if (mode == null) {
            result = true;
        } else if (mode.equals(InputModes.SEPARATION_TICKET) || mode.equals(InputModes.SEPARATION_CASH)) {
            // チケット金額入力 or 決済金額入力
            result = tmpSeparationPayAmount > tmpNowAmount;
        } else {
            result = true;
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
     * @brief  820との通信が確立しているか？
     * @note   820との通信が確立しているか？
     * @param [in] なし
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public boolean isConnected820() {
        return _ifBoxManager.getIsConnected820();
    }

    /******************************************************************************/
    /*!
     * @brief  立替払い処理主処理
     * @note   立て替え払い時の処理を820間と行う
     * @param [in] なし
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void AdvancePay()
    {
        final int changeAmount = _changeAmount.getValue();

        if (changeAmount <= 0) return;

        final int baseAmount = _flatRateAmount.getValue() <= 0
                ? Amount.getMeterCharge()
                : _flatRateAmount.getValue();

        final int currentTotal = baseAmount + _advancePayAmount.getValue();

        if (!checkDigits(currentTotal + changeAmount)) {
            Timber.e("Out of range totalAmount");
            return;
        }

        addAdvancePayAmount(+changeAmount);
        setChangeAmount(0);
    }

    public void flatRate() {
        final int changeAmount = _changeAmount.getValue();

        setFlatRateAmount(changeAmount);
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

        final String mode = _inputMode.getValue();

        if (mode == null) {
            return;
        } else if (mode.equals(InputModes.ADVANCEPAY)) {                //立替
            //if (isDemoMode() == true) {     //デモモード
            //    Amount.setTotalChangeAmount(_advancePayAmount.getValue());
            //}
            //else
            //{
                int tmpAdvancePayAmount = _advancePayAmount.getValue();
                _ifBoxManager.send820_AdvancedPay(IFBoxManager.SendMeterDataStatus_FutabaD.ADVANCEPAY_ADVANCE, tmpAdvancePayAmount);        //820へ送信
            //}
        } else if (mode.equals(InputModes.FLAT_RATE)) {                 //定額
            //if (isDemoMode() == true) {     //デモモード
            //    Amount.setFlatRateAmount(_flatRateAmount.getValue());
            //}
            //else
            //{
                int tmpFlatRateAmount = _flatRateAmount.getValue();
                _ifBoxManager.send820_AdvancedPay(IFBoxManager.SendMeterDataStatus_FutabaD.ADVANCEPAY_FLATRATE, tmpFlatRateAmount);        //820へ送信
            //}
        }
    }

    public void enterSeparation() {
        //Amount.setTotalChangeAmount(_totalChangeAmount.getValue());
        //Amount.setFlatRateAmount(_flatRateAmount.getValue());

        int tmpNowAmount = 0;

        int tmpSeparationPayAmount = _separationPayAmount.getValue();   //入力確定分金額   （例：100円

        final String mode = _inputMode.getValue();
        if (mode == null) {
            // 何もしない
        } else if (mode.equals(InputModes.SEPARATION_TICKET)) {
            // ここでは何もしない（チケット金額の設定は、チケット伝票印刷後に行う）
        } else if (mode.equals(InputModes.SEPARATION_CASH)) {
            // 決済金額入力
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

        final String mode = _inputMode.getValue();
        if (mode == null) {
            // 何もしない
        } else if (mode.equals(InputModes.SEPARATION_TICKET)) {
            // チケット金額入力
            if (Amount.getFlatRateAmount() > 0) {
                Amount.setTotalChangeAmount(0);
                Amount.setTicketAmount(0);
            } else {
                Amount.setTotalChangeAmount(0);
                Amount.setTicketAmount(0);
            }
        } else if (mode.equals(InputModes.SEPARATION_CASH)) {
            // 決済金額入力
            if (Amount.getFlatRateAmount() > 0) {
                Amount.setTotalChangeAmount(0);
                Amount.setCashAmount(0);

            } else {
                Amount.setTotalChangeAmount(0);
                Amount.setCashAmount(0);
            }

            Amount.fix();           //料金確定処理
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

        final String mode = _inputMode.getValue();
        if (mode == null) {
            // 何もしない
        } else if (mode.equals(InputModes.SEPARATION_TICKET)) {
            // チケット金額設定
            Amount.setTicketAmount(tmpRecvAmount);
        }
    }

    public void cancel() {
    }

    public void reset() {
//        final String mode = _inputMode.getValue();
//
//        if (mode == null) {
//            return;
//        } else if (mode.equals(InputModes.ADVANCEPAY)) {                //立替
//            setAdvancePayAmount(0);
//        } else if (mode.equals(InputModes.FLAT_RATE)) {                 //定額
//            setFlatRateAmount(0);
//        }

        setAdvancePayAmount(0);
        setFlatRateAmount(0);
        setSeparationPayAmount(0);
        Amount.setTotalChangeAmount(0);
        Amount.setFlatRateAmount(0);
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
        else if (mode.equals(InputModes.ADVANCEPAY))  AdvancePay();
        else if (mode.equals(InputModes.FLAT_RATE)) flatRate();
        else if (mode.equals(InputModes.SEPARATION_TICKET)) SeparationPay();
        else if (mode.equals(InputModes.SEPARATION_CASH)) SeparationPay();

//        _inputMode.setValue(null);
    }

    private boolean checkDigits(int amount) {
        return (amount / ((int) Math.pow(10, MAX_AMOUNT_DIGITS))) == 0;
    }

    public void flatClear() {
        _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.ADVANCEPAY_CLEAR, true);
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
    public int fixAmountRegistHistrySlipTicket(int amount, String[] transDate, int[] termSequence)
    {
        if (_transLogger == null) {
            return -1;
        }

        Date exDate = new Date();   // 取引時間
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        String payTime = dateFmt.format(exDate);

        _transLogger.separateTicket(payTime, amount);     //分別チケットモードで取引歴登録
        _transLogger.getSlipDataInfo(transDate, termSequence);
        int tmpSlipID =  _transLogger.insert();
        return tmpSlipID;
    }

    public void deleteHistryTicket(String transDate, int termSequence) {
        _transLogger.delete(transDate, termSequence);
    }
}
//ADD-E BMT S.Oyama 2024/08/27 フタバ双方向向け改修


