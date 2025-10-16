package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeatures;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class ClientStateMachine25a extends ClientStateMachine {
    private static final int PARAM_LIST_MAX = 5;
    private static final int FEATURES_LIST_MAX = 255;

    private final List<FeliCaParam> _paramList;           // PreCommand系にて使用するパラメータリスト
    private final List<MsgFeatures> _featureList;         // Handshake時に使用するFeaturesリスト

    private ClientStateMachine25a(TCAPContext tcapContext, TCAPCommunicationAgent tcapCommunicationAgent) {
        super(tcapContext, tcapCommunicationAgent);

        _paramList = new ArrayList<>(PARAM_LIST_MAX);
        _featureList = new ArrayList<>(FEATURES_LIST_MAX);
    }

    /**
     * インスタンス生成
     * インスタンスを生成します。
     *
     * @param context 通信コンテキスト
     * @param agent   通信エージェント
     *
     * @return インスタンスが生成された場合インスタンス、それ以外はnull
     */
    static ClientStateMachine25a CreateInstance(TCAPContext context, TCAPCommunicationAgent agent) {
        ClientStateMachine25a instance;

        // インスタンス生成
        instance = new ClientStateMachine25a(context, agent);

        // ここからインスタンスの初期化

        MsgFeatures features = new MsgFeatures((char)TCAPPacket.TCAP_VERSION_25);

        // オプションをここで設定する
        instance._featureList.add(features);

        return instance;
    }

    /**
     * ステータスオブジェクトの構築
     * 指定のステータスを構築します。
     *
     * @param createState 構築するステータス
     *
     * @return ステータスオブジェクト
     */
    public IState CreateStatus(status createState) {
        IState retVal = null;

        switch(createState) {
            case STATUS_HANDSHAKE:
                retVal = new StateHandshake25a();
                break;
            case STATUS_NEUTRAL:
                retVal = new StateNeutral25a();
                break;
            default:
                break;
        }

        return retVal;
    }

    /**
     * パラメータリスト取得
     * Preコマンド系にて使用するパラメータリストを取得します。
     *
     * @return パラメータリスト
     */
    public List<FeliCaParam> GetParamList() {
        return _paramList;
    }

    /**
     * Featureリスト取得
     * Handshakeにて使用するFeatureリストを取得します。
     *
     * @return Featureリスト
     */
    public List<MsgFeatures> GetFeatureList() {
        return _featureList;
    }
}

