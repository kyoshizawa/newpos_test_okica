package jp.mcapps.android.multi_payment_terminal.data.okica;

import com.google.gson.annotations.Expose;

public class ICMasterInfo {
    @Expose
    public String fileName;

    @Expose
    public int version;

    @Expose
    public long checkDate;
}