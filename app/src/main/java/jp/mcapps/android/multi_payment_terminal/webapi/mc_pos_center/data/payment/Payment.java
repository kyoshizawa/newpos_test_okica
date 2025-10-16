package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class Payment {
    public static class Request {
        @Expose
        public List<RequestCr> cr = new ArrayList<>();  //クレジット売上 存在しない場合は[]

        @Expose
        public List<RequestJr> jr = new ArrayList<>();  //交通系売上 存在しない場合は[]

        @Expose
        public List<RequestQp> qp = new ArrayList<>();  //QuicPay売上 存在しない場合は[]

        @Expose
        public List<RequestId> id = new ArrayList<>();  //iD売上 存在しない場合は[]

        @Expose
        public List<RequestEd> ed = new ArrayList<>();  //Edy売上 存在しない場合は[]

        @Expose
        public List<RequestWn> wn = new ArrayList<>();  //WAON売上 存在しない場合は[]

        @Expose
        public List<RequestNn> nn = new ArrayList<>();  //nanaco売上 存在しない場合は[]

        @Expose
        public List<RequestUp> up = new ArrayList<>();  //銀聯売上 存在しない場合は[]

        @Expose
        public List<RequestQr> qr = new ArrayList<>();  //QR売上 存在しない場合は[]
    }

    public static class Response {
        @Expose
        public Boolean result;  //処理の成否。正常:true、異常：false

        @Expose
        public String errorCode;    //処理が異常な場合のコード。Result：trueの場合、本項目は""となる。

        //以降は、Result：trueの場合のみ付与。

        @Expose
        public List<ResultInfo> cr; //1クレジット売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> jr; //1交通系売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> qp; //1QuicPay売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> iD; //1iD売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> ed; //1Edy売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> wn; //1WAON売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> nn; //1nanaco売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> up; //1銀聯売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。

        @Expose
        public List<ResultInfo> qr; //1QR売上毎の保存結果をResultInfo型で表した配列。存在しない場合は[]。
    }
}
