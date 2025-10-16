package jp.mcapps.android.multi_payment_terminal.thread.credit;

import com.pos.device.SDKException;

import java.util.HashMap;

import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;
import jp.mcapps.android.multi_payment_terminal.ui.credit_card.CreditCardScanFragment;

public class MsOnlineAuthStateMachine {
    private static final HashMap<Status, Status> _statusMap = new HashMap<>();
    private CreditSettlement _creditSettlement;
    private CardListener _cardListener;
    private EmvProcessing _emvProc;

    private final EmvCLProcess _emvCLProc;
    private Status _status = Status.STATUS_NONE;
    private Status _nextStatus = Status.STATUS_NONE;
    private CreditState _exeClass = null;

    public MsOnlineAuthStateMachine(CreditSettlement creditSettlement, CardListener cardListener, EmvProcessing emvProc, EmvCLProcess emvCLProc) {
        _creditSettlement = creditSettlement;
        _cardListener = cardListener;
        _emvProc = emvProc;
        _emvCLProc = emvCLProc;
    }

    public enum Status
    {
        STATUS_NONE(0),
        STATUS_MS_ONLINE_AUTH(1),   // オンラインオーソリ
        ;

        private int _val = 0;

        private Status(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    };

    static {
        _statusMap.put(Status.STATUS_MS_ONLINE_AUTH, Status.STATUS_NONE);
    }

    public void changeStatus(Status nextStatus) {
        _nextStatus = nextStatus;
    }

    public int start() throws SDKException {
        int ret = 0;

        do {
            if(_nextStatus != _status) {
                // ステータスが変わったら実行クラスを変更
                if(_exeClass != null) {
                    _exeClass = null;
                }
                // 実行クラスを変更
                _exeClass = createStateClass(_nextStatus);
                _status = _nextStatus;
            }

            if (null != _exeClass) {
                // 処理実行
                ret = _exeClass.stateMethod();
                // 次の状態へ遷移
                _nextStatus = _statusMap.get(_status);
                if (Status.STATUS_NONE == _nextStatus || null == _nextStatus) {
                    // StateMachineを終了
                    _exeClass = null;
                }
                if (_creditSettlement.k_ERROR_UNKNOWN == ret) {
                    // StateMachineを終了
                    _exeClass = null;
                }
            }
        } while(null != _exeClass);

        return ret;
    }

    public CreditState createStateClass(Status nextStatus) {
        CreditState exeClass = null;
        switch(nextStatus) {
            case STATUS_MS_ONLINE_AUTH:
                exeClass = new StateMsOnlineAuth(_creditSettlement, _emvProc, _emvCLProc);
                break;
            default:
                break;
        }
        return exeClass;
    }
}
