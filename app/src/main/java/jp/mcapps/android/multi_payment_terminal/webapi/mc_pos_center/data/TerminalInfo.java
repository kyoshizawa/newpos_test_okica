package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class TerminalInfo {
    public static class Request {
    }

    public static class Response {
        @Expose
        public Boolean result;  // 処理の成否。正常：true 、 異常： false

        @Expose
        public String errorCode;  // 処理が異常な場合のコード。 Result : true の場合、本項目は "" となる。

        /*** 以降は、Result: true の場合のみ付与。***/

        @Expose
        public String terminalNo;  // 端末番号

        @Expose
        public Boolean isInputDriverCd;  // 乗務員コード入力が可能か。

        @Expose
        public Integer fixedDriverCd;  // 乗務員入力しない場合、使用する乗務員コード。乗務員入力する場合は NULL。

        @Expose
        public Integer carNo;  // 端末に設定されている号機番号（車両番号）。設定されていない場合 NULL。

        @Expose
        public Boolean isHistoryDriverCd;  // 乗務員コードの入力履歴が閲覧可能か。

        @Expose
        public Boolean isAntennaLog;  // アンテナレベルロギングの有効/無効

        @Expose
        public Integer antennaLogIntervalSeconds;  // アンテナレベルロギングの間隔（秒）

        @Expose
        public Boolean isGpsLog;  // GPS情報ロギングの有効/無効

        @Expose
        public Integer gpsLogIntervalSeconds;  // GPS情報ロギングの間隔（秒）

        @Expose
        public Integer gpsLogIntervalMeter;  // GPS情報ロギングの間隔（m）

        @Expose
        public Boolean isFareUnit1Yen;  // 1円単位の金額設定の有効/無効

        @Expose
        public Boolean isCashTogetherFareUnit1Yen;  // 1円単位の現金併用設定の有効/無効

        @Expose
        public String activateId;  // クライアント証明書のダウンロードに必要な情報

        @Expose
        public String password;  // クライアント証明書のダウンロードに必要な情報

        @Expose
        public Boolean isCredit;  // クレジット決済の有効/無効

        @Expose
        public Boolean isContactless;  // 非接触ICカード決済の有効/無効

        @Expose
        public Boolean isUnionPay;  // 銀聯決済の有効・無効

        @Expose
        public Boolean isJr;  // 交通系の有効/無効

        @Expose
        public Boolean isId;  // iDの有効/無効

        @Expose
        public Boolean isWaon;  // WAONの有効/無効

        @Expose
        public Boolean isNanaco;  // nanacoの有効/無効

        @Expose
        public Boolean isEdy; // edyの有効/無効

        @Expose
        public Boolean isOkica; // okicaの有効/無効

        @Expose
        public Boolean isQuicPay; // QuicPayの有効/無効

        @Expose
        public Boolean isQr; // QR決済の有効/無効

        @Expose
        public Boolean isWatariPoint; // 和多利ポイントの有効/無効

        @Expose
        public String productCd; // 商品区分コード

        @Expose
        public String branchOfficeName; // 会社名（支社名）

        @Expose
        public String salesOfficeName; // 営業所名

        @Expose
        public String telNumber; // 電話番号

        @Expose
        public Boolean isInputCarNo; // 号機番号（車番）入力の有無の有効/無効

        @Expose
        public Boolean isScreenLock; // 画面ロックの有効/無効

        @Expose
        public String screenPass; // 画面ロック時のPINパスワード（4桁以上の数字）

        @Expose
        public Integer screenTimeUpSec; // 画面オフまでの時間

        @Expose
        public Integer screenLockSec; //画面オフからロックがかかるまでの時間

        @Expose
        public Boolean isDayTotalDetail; // 日計の詳細印字

        @Expose
        public String customerCd; // 顧客コード

        @Expose
        public String qrUserId; // QR決済認証用の端末ID

        @Expose
        public String qrPassword; // QR決済認証用のパスワード

        @Expose
        public Boolean isDeveloperMode; //  開発者モード

        @Expose
        public String optionServiceDomain; //  オプションサービスのアクセス先エンドポイント情報

        @Expose
        public String optionServiceKey; //  オプションサービスのアクセスに使用するためのキー情報

        @Expose
        public OptionServiceFunc[] optionServiceFuncs; // 端末に付加する１機能を オブジェクト型 で表した配列。利用可能な分の [オプション機能オブジェクト] が設定される。

        @Expose
        public String invoiceNo; // レシートに表記するインボイス番号。

        @Expose
        public Float receiptTax; // レシートに表記する消費税率。小数点で表記し、小数点以下は最大5桁まで。例）消費税 10.5% の場合は、0.105。

        @Expose
        public String supplierCd; // 取引先コード。このコードにより、ペイメントシステム側の店舗情報を識別する。

        @Expose
        public Integer maxAmountType; // 利用可能な最大取引金額のタイプ。 0:100万円未満 1:1000万円未満

        @Expose
        public PosServiceFunc posServiceFunc; // 特定の機能で使用するデータを集約したオブジェクト型。[POS機能オブジェクト] が設定される。

        @Expose
        public Boolean isPrepaid; // プリペイドの有効/無効

        @Expose
        public String prepaidServiceDomain; // プリペイドのアクセス先エンドポイント情報

        @Expose
        public String prepaidServiceKey; // プリペイドのアクセスに使用するためのキー情報

        @Expose
        public AdditionalSettings[] additionalSettings; // ["端末拡張設定"] の配列が設定される。
    }

    public static class OptionServiceFunc {
        @Expose
        public int funcID;

        @Expose
        public String displayName;
    }

    public static class PosServiceFunc {
        @Expose
        public Boolean isProductCategory; // 商品カテゴリでグルーピングするかどうか。

        @Expose
        public Boolean isPosReceipt; // 領収書発行ボタンを表示するかどうか。

        @Expose
        public Boolean isManualAmount; // 金額の手入力を可能にするかどうか。

        @Expose
        public String slipTitle; // レシートに表示するタイトル文字列。

        @Expose
        public Integer taxRounding; // 消費税計算後に端数が発生した場合の、処理方法。0:切り捨て 1:切り上げ 2:四捨五入

        @Expose
        public TaxRate[] taxList; // システム時間を基準として、現在有効な消費税率をオブジェクト型 で表した配列。[消費税率オブジェクト] が設定される。

        @Expose
        public Integer receiptCounts; // レシートを印字する枚数。有効範囲、0~10。

        @Expose
        public Boolean isFixedAmountPostalOrder; // 郵便小為替の有効/無効
    }

    public static class TaxRate {
        @Expose
        public Integer taxClass; // 消費税の属性。 0:一般税率 1:軽減税率

        @Expose
        public Float tax; // 消費税計算に使用する消費税率。小数点で表記し、小数点以下は最大5桁まで。例）消費税 10.5% の場合は、0.105 。
    }

    public static class AdditionalSettings {
        @Expose
        public String paramName;    // 設定パラメータ名

        @Expose
        public String value; // 値

        public String getParamName() {
            return paramName;
        }

        public String getValue() {
            return value;
        }
    }
}
