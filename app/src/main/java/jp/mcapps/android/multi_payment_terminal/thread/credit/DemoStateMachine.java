package jp.mcapps.android.multi_payment_terminal.thread.credit;

import com.pos.device.SDKException;

import java.util.HashMap;

import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;

public class DemoStateMachine {
    private static final HashMap<Status, Status> _statusMap = new HashMap<>();
    private CreditSettlement _creditSettlement;
    private CardListener _cardListener;
    private EmvProcessing _emvProc;
    private final EmvCLProcess _emvCLProc;
    private Status _status = Status.STATUS_NONE;
    private Status _nextStatus = Status.STATUS_NONE;
    private CreditState _exeClass = null;

    public DemoStateMachine(CreditSettlement creditSettlement, CardListener cardListener, EmvProcessing emvProc, EmvCLProcess emvCLProc) {
        _creditSettlement = creditSettlement;
        _cardListener = cardListener;
        _emvProc = emvProc;
        _emvCLProc = emvCLProc;
    }

    public enum Status
    {
        STATUS_NONE(0),
        STATUS_DETECT_CARD(1),  // カード検出
        STATUS_DEMO_IC_PROC(2), // デモICカード処理
        STATUS_DEMO_CL_PROC(2), // デモ非接触ICカード処理
        STATUS_DEMO_AUTH(3),    // デモオーソリ
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
        /* 遷移先のステータスを定義 */
        _statusMap.put(Status.STATUS_DETECT_CARD, Status.STATUS_NONE);
        _statusMap.put(Status.STATUS_DEMO_IC_PROC, Status.STATUS_NONE);
        _statusMap.put(Status.STATUS_DEMO_CL_PROC, Status.STATUS_NONE);
        _statusMap.put(Status.STATUS_DEMO_AUTH, Status.STATUS_NONE);
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
            case STATUS_DETECT_CARD:
                exeClass = new StateDetectCard(_cardListener, _creditSettlement);
                break;
            case STATUS_DEMO_IC_PROC:
                exeClass = new StateDemoIcProc(_creditSettlement, _emvProc);
                break;
            case STATUS_DEMO_CL_PROC:
                exeClass = new StateDemoCLProc(_creditSettlement, _emvCLProc);
                break;
            case STATUS_DEMO_AUTH:
                exeClass = new StateDemoOnlineAuth(_creditSettlement, _emvProc, _emvCLProc);
                break;
            default:
                break;
        }
        return exeClass;
    }
}
