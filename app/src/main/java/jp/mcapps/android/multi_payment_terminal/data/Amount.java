package jp.mcapps.android.multi_payment_terminal.data;

import androidx.lifecycle.MutableLiveData;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import timber.log.Timber;

public class Amount {
    private static int _meterCharge = AppPreference.getConfirmMeterCharge();
    public static int getMeterCharge() { return _meterCharge; }
    public static void setMeterCharge(int meterCharge) {
        if (meterCharge >= 0){
            /* メーター料金が0円以上の場合、正常なメーター料金として取り込む */
            _meterCharge = meterCharge;
        } else {
            /* メーター料金が0円未満の場合、異常なメーター料金として取り込まずに0円設定 */
            _meterCharge = 0;
        }
    }

    // 定額で入力された料金
    private static int _flatRateAmount = AppPreference.getConfirmFlatRateAmount();
    public static int getFlatRateAmount() { return _flatRateAmount; }
    public static void setFlatRateAmount(int flatRateAmount) { _flatRateAmount = flatRateAmount; }

    // メータからもらった金額 or 定額で設定した金額
    public static int getBaseAmount() { return _flatRateAmount > 0 ? _flatRateAmount : _meterCharge; }

    // 増減額の合計
    private static int _totalChangeAmount = AppPreference.getConfirmTotalChangeAmount();
    public static int getTotalChangeAmount() { return _totalChangeAmount; }
    public static void setTotalChangeAmount(int totalChangeAmount) { _totalChangeAmount = totalChangeAmount; }

    // 現金分割
    private static int _cashAmount = 0;
    public static int getCashAmount() { return _cashAmount; }
    public static void setCashAmount(int cashAmount) {
        Timber.d("setCashAmount %d", cashAmount);
        _cashAmount = cashAmount;
    }

    // チケット金額
    private static int _ticketAmount = 0;
    public static int getTicketAmount()
    {
        //Timber.i("[FUTABA-D]**! getTicketAmount %d !**", _ticketAmount);
        return _ticketAmount;
    }
    public static void setTicketAmount(int ticketAmount) {
        //Timber.d("setTicketAmount %d", ticketAmount);
        Timber.i("[FUTABA-D]*** setTicketAmount %d ***", ticketAmount);
        _ticketAmount = ticketAmount;
    }

    // POS金額
    private static int _posAmount = 0;
    public static int getPosAmount() { return _posAmount; }
    public static void setPosAmount(int amount) { _posAmount = amount; }
    private static boolean _isPosAmount = false;
    public static boolean isPosAmount() { return  _isPosAmount; }
    public static void isPosAmount(boolean b) { _isPosAmount = b; }

    // 支払済金額追加　20231127 t.wada
    private static int _PaidAmount = 0;
    public static int getPaidAmount() { return _PaidAmount; }
    public static void setPaidAmount(int paidAmount) { _PaidAmount = paidAmount; }

    //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
    //立替金額
    private static int _tatekae = 0;
    public static int getTatekae() { return _tatekae; }
    public static void setTatekae(int tatekae) { _tatekae = tatekae; }
    //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

    // 支払い金額
    public static int getTotalAmount() {
        if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
            if (isPosAmount()) {
                return getPosAmount();
            } else {
                return getBaseAmount() + getTotalChangeAmount();
            }
        }
        //CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        //if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
        //return getMeterCharge() - getCashAmount() - getTicketAmount() - getPaidAmount(); // 支払済金額追加　20231127 t.wada getPaidAmount
        if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
            //ADD-S BMT S.Oyama 2024/12/26 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)
            {
                if (AppPreference.isMeterStatusSiharai() && Amount.getPaymented() != 0) {
                    // メーターが支払で決済済の場合
                    Timber.i("[FUTABA-D]*** TOTAL AMOUNT 0 ***");
                    return 0;
                } else {
                    Timber.i("[FUTABA-D]*** TOTAL AMOUNT = %d ***", getMeterCharge() - getCashAmount() - getPaidAmount());
                    return getMeterCharge() - getCashAmount();         // 立替金額追加 240930 S.Oyama getTatekae()削除
                }

            }
            else
            {
                return getMeterCharge() - getCashAmount() - getTicketAmount() - getPaidAmount() ;         // 立替金額追加 240930 S.Oyama getTatekae()削除
            }
            //ADD-E BMT S.Oyama 2024/12/26 フタバ双方向向け改修
        //CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
        } else {
            return getBaseAmount() + getTotalChangeAmount();
        }
    }

    // 営業回数
    private static int _eigyoCount = 0;
    public static int getEigyoCount() { return _eigyoCount; }
    public static void setEigyoCount(int eigyoCount) { _eigyoCount = eigyoCount; }


    // 確定した金額 合計金額は決済途中で値が変わらない可能性が0ではないので画面を開いたときのスナップショットを保存する
    private static int _fixedAmount;
    private static int _fixedEigyoCount;
    public static int getFixedAmount() { return _fixedAmount; }
    public static int getFixedEigyoCount() { return _fixedEigyoCount; }

    // 金額決定するときに呼び出す
    public static void fix() { _fixedAmount = getTotalAmount(); _fixedEigyoCount = getEigyoCount();}

    //ADD-S BMT S.Oyama 2024/12/04 フタバ双方向向け改修
    //割引実施済みフラグ
    public static int _DiscountAvailable = 0;
    public static int getDiscountAvailable(){
        return _DiscountAvailable;
    }
    public static void setDiscountAvailable(int mode){       //modeは0～4(配列インデックス)
        _DiscountAvailable = mode;
    }
    //ADD-E BMT S.Oyama 2024/12/04 フタバ双方向向け改修

    //支払実施済みフラグ
    public static int _paymented = 0;
    public static int getPaymented() {
        return _paymented;
    }
    public static void setPaymented(int mode) {
        _paymented = mode;
    }

    public static void reset() {
        _meterCharge = 0;
        _totalChangeAmount = 0;
        _flatRateAmount = 0;
        _fixedAmount = 0;
        _cashAmount = 0;
        _ticketAmount = 0;
        _eigyoCount = 0;
        _posAmount = 0;
        _PaidAmount = 0; // 支払済金額追加　20231127 t.wada
    }
}
