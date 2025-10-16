package jp.mcapps.android.multi_payment_terminal.data.okica;

import com.google.gson.annotations.Expose;

public class AccessKeyInfo {
    @Expose
    public int version;

    @Expose
    public int generation;

    @Expose
    public int areaNum;

    @Expose
    public String areaKeyVersions;

    @Expose
    public String areaCodeList;

    @Expose
    public int serviceNum;

    @Expose
    public String serviceKeyVersions;

    @Expose
    public String serviceCodeList;

    /**
     * ミリ秒単位のUNIX時間
     */
    @Expose
    public long checkDate;

    /**
     * ミリ秒単位のUNIX時間
     */
    @Expose
    public long endDate;
}