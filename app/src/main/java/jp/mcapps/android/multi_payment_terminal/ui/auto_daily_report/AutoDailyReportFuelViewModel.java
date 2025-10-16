package jp.mcapps.android.multi_payment_terminal.ui.auto_daily_report;
//ADD-S BMT S.Oyama 2024/11/21 フタバ双方向向け改修

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import timber.log.Timber;

import static java.lang.Integer.parseInt;
import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;

@SuppressWarnings("ALL")
public class AutoDailyReportFuelViewModel extends ViewModel {
    private static final int MAX_FUEL_DIGITS = 6;           //最大桁数（小数込で6桁：実数は5桁）

    private final IFBoxManager _ifBoxManager;
    public IFBoxManager getIfBoxManager() { return _ifBoxManager; }

    private MutableLiveData<String> _inputValueTxt = new MutableLiveData<>("");
    public MutableLiveData<String> getInputValueTxt() { return _inputValueTxt; }
    public void setInputValueTxt(String value) {
        _inputValueTxt.setValue(value);
    }

    private BigDecimal _inputValue = new BigDecimal(0);
    private int        _inputValueInt = 0;
    public int getInputValueInt() { return _inputValueInt; }

    private MutableLiveData<Boolean> _isRegistJob = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> getIsRegistJob() { return _isRegistJob; }
    public void setIsRegistJob(boolean value) {
        _isRegistJob.setValue(value);
    }

    public static Disposable meterDataV4ErrorDisposable = null;
    //public static Disposable meterDataV4InfoDisposable = null;


    public AutoDailyReportFuelViewModel(IFBoxManager ifBoxManager) {
        _ifBoxManager = ifBoxManager;
    }


    { // initializer
        clearInputValueTxt();
    }

    /******************************************************************************/
    /*!
     * @brief 入力数値の正当性チェック
     * @note　入力数値の正当性チェック
     * @param [in] String stringNumber
     * @retval なし
     * @return　正当値の場合はtrue
     * @private
     */
    /******************************************************************************/
    public boolean isNumericalValidity(String stringNumber)
    {
        boolean result = false;

        String tmpValueText = _inputValueTxt.getValue();

        tmpValueText += stringNumber;

        switch(tmpValueText.length())
        {
            case 0:
                result = false;
                break;
            case 1:
                result = true;
                break;
            default:
                if (tmpValueText.charAt(0) == '.' )
                {
                    tmpValueText = "0" + tmpValueText;
                }
                else if (tmpValueText.charAt(tmpValueText.length() - 1) == '.' )
                {
                    tmpValueText = tmpValueText + "0" ;
                }

                result = isDecimal(tmpValueText);
                break;
        }

        return result;
    }

    public boolean isNumericalValidity()
    {
        boolean result = false;

        String tmpValueText = _inputValueTxt.getValue();

        switch(tmpValueText.length())
        {
            case 0:
                result = false;
                break;
            case 1:
                result = true;
                break;
            default:
                if (tmpValueText.charAt(0) == '.' )
                {
                    tmpValueText = "0" + tmpValueText;
                }
                else if (tmpValueText.charAt(tmpValueText.length() - 1) == '.' )
                {
                    tmpValueText = tmpValueText + "0" ;
                }

                result = isDecimal(tmpValueText);
                break;
        }

        return result;
    }

    /******************************************************************************/
    /*!
     * @brief 入力数値長のチェック
     * @note　入力数値長のチェック
     * @param [in] String stringNumber
     * @retval なし
     * @return　数値長が超えてなければTrue
     * @private
     */
    /******************************************************************************/
    public boolean isNumericalLength(String stringNumber)
    {
        boolean result = false;

        String tmpValueText = _inputValueTxt.getValue();

        tmpValueText += stringNumber;

        if (tmpValueText.length() <= MAX_FUEL_DIGITS)
        {
            result = true;
        }

        return result;
    }
    public boolean isNumericalLength()
    {
        boolean result = false;

        String tmpValueText = _inputValueTxt.getValue();

        if (tmpValueText.length() <= MAX_FUEL_DIGITS)
        {
            result = true;
        }

        return result;
    }

    /******************************************************************************/
    /*!
     * @brief 実際の数値文字列の結合処理
     * @note　
     * @param [in] String stringNumber
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void inputNumber(String stringNumber) {
        String tmpValueText = _inputValueTxt.getValue();

        tmpValueText += stringNumber;

        setInputValueTxt(tmpValueText);
    }




    /******************************************************************************/
    /*!
     * @brief 数値文字列の右端を１文字削除
     * @note
     * @param [in]
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void deleteNumberStrRight() {
        String tmpValueText = _inputValueTxt.getValue();

        if (tmpValueText.length() > 0)
        {
            tmpValueText = tmpValueText.substring(0, tmpValueText.length() - 1);
        }

        setInputValueTxt(tmpValueText);
    }

    /******************************************************************************/
    /*!
     * @brief 数値文字列をdecimalへ変換
     * @note
     * @param [in]
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void convertDecimalFromInputValueTxt()
    {
        _inputValue = new BigDecimal(0);
        String tmpValueText = _inputValueTxt.getValue();

        if (isNumericalValidity() == false)
        {
            return;
        }

        if (isNumericalLength() == false)
        {
            return;
        }

        try
        {
            _inputValue = new BigDecimal(tmpValueText);
        }
        catch (NumberFormatException e)
        {
            return;
        }

        BigDecimal tmpMul100 = new BigDecimal(100);
        BigDecimal tmpValue = _inputValue.multiply(tmpMul100);

        _inputValueInt = tmpValue.intValue();               //小数点以下２桁有効　１００倍した整数値を保持
        tmpValue = new BigDecimal(_inputValueInt);
        _inputValue = tmpValue.divide(tmpMul100);

        DecimalFormat decimalFormat = new DecimalFormat("###0.00");
        setInputValueTxt( decimalFormat.format(_inputValue));
    }

    /******************************************************************************/
    /*!
     * @brief 数値のマックス値チェック
     * @note
     * @param [in]
     * @retval なし
     * @return　MAX値を超えてたらtrue
     * @private
     */
    /******************************************************************************/
    public boolean isMaxOver() {
        String tmpValueText = _inputValueTxt.getValue();

        BigDecimal tmpValue;
        BigDecimal tmpMaxValue = new BigDecimal("999.99");
        try
        {
            tmpValue = new BigDecimal(tmpValueText);
        }
        catch (NumberFormatException e)
        {
            return true;
        }

        if (tmpValue.compareTo( tmpMaxValue) > 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }



    /******************************************************************************/
    /*!
     * @brief 数値文字列クリア
     * @note
     * @param [in]
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    public void clearInputValueTxt() {
        setInputValueTxt("");
        _inputValue = new BigDecimal(0);
        _inputValueInt = 0;

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
     * @brief  数値文字列の正当化チェック
     * @note
     * @param [in] String str 数値化文字列
     * @retval なし
     * @return　なし
     * @private
     */
    /******************************************************************************/
    private boolean isDecimal(String str) {
        // 正規表現: - または + が前に来てもよい、数字が1つ以上、小数点、そして数字が1つ以上
        String regex = "^[+-]?\\d+(\\.\\d+)?$";
        return str.matches(regex);
    }
}
//ADD-E BMT S.Oyama 2024/11/21 フタバ双方向向け改修


