package jp.mcapps.android.multi_payment_terminal.thread.emv;

public class AidParam {
    public AidParam(){};
    private byte kernelid;
    private byte trantype;
    public byte[]aid;
    private boolean asi;
    private byte[]param;

    public void SetKernelid(byte id){
        this.kernelid=id;
    }
    public byte GetKernelid()
    {
        return this.kernelid;
    }
    public void SetTrantype(byte type){
        this.trantype=type;
    }
    public byte GetTrantype()
    {
        return this.trantype;
    }
    public void SetAid(byte []paid){
        this.aid=paid;
    }
    public byte[]GetAid(){
        return this.aid;
    }
    public void SetAsi(boolean pasi)
    {
        this.asi=pasi;
    }
    public boolean GetAsi()
    {
        return this.asi;
    }
    public void SetParam(byte []temp){
        this.param=temp;
    }
    public byte[]GetParam(){
        return this.param;
    }
}
