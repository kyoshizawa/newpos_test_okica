package jp.mcapps.android.multi_payment_terminal.thread.emv;


import com.newpos.emvl2.EMV_ENUM;
import com.newpos.emvl2.EMV_L2_CallBack;
import com.newpos.emvl2.EMV_PROGRAM;
import com.newpos.emvl2.EMV_PUBLIC_KEY;
import com.newpos.emvl2.EMV_PUBLIC_KEY_RSA;
import com.newpos.emvl2.EMV_UI_REQUEST_DATA;
import com.pos.device.SDKException;
import com.pos.device.config.DevConfig;
import com.pos.device.icc.SlotType;
import com.pos.device.ped.IccOfflinePinApdu;
import com.pos.device.ped.KeySystem;
import com.pos.device.ped.Ped;
import com.pos.device.ped.PedRetCode;
import com.pos.device.ped.PinBlockCallback;
import com.pos.device.ped.PinBlockFormat;
import com.pos.device.ped.RsaPinKey;
import com.secure.api.PadView;

import org.dtools.ini.IniFile;
import org.dtools.ini.IniItem;
import org.dtools.ini.IniSection;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CardManager;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.AES128Util;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.util.DebugLog;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;

public class EmvCLCallBackHandle implements EMV_L2_CallBack {
    private static final DebugLog _debugLog = new DebugLog(EmvCLCallBackHandle.class.getSimpleName());
    private CardManager _cm;
    private AidParam[] _aidParam;
    private int _aidCount;
    private final byte[] apduRcvData = new byte[512];
    private int apduRcvLen;

    private static final MainApplication _app = MainApplication.getInstance();

    public void init(CardManager cm) {
        _cm = cm;
        _aidCount =0;
        loadAidParam();
    }

    @Override
    public EMV_PROGRAM SetEMVProgram() {
        return null;
    }
    @Override
    public int cb_hsm_aes_decrypt(EMV_PROGRAM emv_program, int i, byte[] bytes, int i1, byte[] bytes1, int i2, byte[] bytes2, int[] ints) {
        try {
            byte[]result= AES128Util.decrypt(i,bytes1,bytes,"00000000000000000000000000000000");
            if(result!=null){
                System.arraycopy(result,0,bytes2,0,result.length);
                ints[0]=result.length;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @Override
    public int cb_hsm_aes_encrypt(EMV_PROGRAM emv_program, int i, byte[] bytes, int i1, byte[] bytes1, int i2, byte[] bytes2, int[] ints) {
        try {
            byte[]result=AES128Util.encrypt(i,bytes1,bytes,"00000000000000000000000000000000");
            if(result!=null){
                System.arraycopy(result,0,bytes2,0,result.length);
                ints[0]=result.length;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @Override
    public int cb_hsm_rsa_public_encrypt(EMV_PROGRAM emv_program, byte[] bytes, int i, byte[] bytes1, int i1, byte[] bytes2, int i2, byte[] bytes3, int[] ints) {
        byte[]result= ISOUtil.encryptData(bytes,bytes1,bytes2);
        if(result!=null){
            System.arraycopy(result,0,bytes3,0,result.length);
            ints[0]=result.length;
            return 0;
        }
        return -1;
    }
    @Override
    public void cb_sys_ifd_serial_number_get(EMV_PROGRAM emv_program, String[] strings, int i) {
        String sn = DeviceUtils.getSerial();
        strings[0]=sn;
    }

    @Override
    public void cb_sys_current_time_get(EMV_PROGRAM emv_program, byte[] bytes) {
        long Cutime= System.currentTimeMillis();
        Calendar mCalendar= Calendar.getInstance();
        mCalendar.setTimeInMillis(Cutime);
        int mYear = mCalendar.get(Calendar.YEAR);
        int mMonth = mCalendar.get(Calendar.MONTH)+1;
        int mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
        int mHour=mCalendar.get(Calendar.HOUR_OF_DAY);
        int mMinuts=mCalendar.get(Calendar.MINUTE);
        int mSecond = mCalendar.get(Calendar.SECOND);
        int mWeek = mCalendar.get(Calendar.DAY_OF_WEEK);
        byte[] y = ISOUtil.int2byteboth(mYear,2);
        if (y!=null) {
            bytes[0] = y[0];
            bytes[1] = y[1];
        }
        byte[] m = ISOUtil.int2byteboth(mMonth,1);
        if (m !=null)
            bytes[2]=m[0];
        byte[] d = ISOUtil.int2byteboth(mDay,1);
        if (d !=null)
            bytes[3]=d[0];
        byte[] h = ISOUtil.int2byteboth(mHour,1);
        if (h !=null)
            bytes[4]=h[0];
        byte[] mi = ISOUtil.int2byteboth(mMinuts,1);
        if (mi!=null)
            bytes[5]=mi[0];
        byte[] s = ISOUtil.int2byteboth(mSecond,1);
        if (s!=null)
            bytes[6]=s[0];
    }

    @Override
    public int cb_sys_transacaction_seq_number_get(EMV_PROGRAM emv_program) {
        return 1;
    }

    @Override
    public void cb_sys_trans_amount_total_get(EMV_PROGRAM emv_program, byte[] bytes, int i, byte[] bytes1, int i1,byte[]amount) {
        _debugLog.d("pan ="+ISOUtil.byte2hex(bytes)+" leng ="+i );
        _debugLog.d("bytes1 ="+ISOUtil.byte2hex(bytes1)+" leng ="+i1 );
        System.arraycopy(ISOUtil.hex2byte("000000100000"),0,amount,0,6);
    }

    @Override
    public int cb_sys_break_detect(EMV_PROGRAM emv_program) {
        return 0;
    }

    @Override
    public boolean cb_ct_apdu_exchange(EMV_PROGRAM emv_program, byte[] bytes, int i, byte[] bytes1, int[] ints) {
        // 接触はこのクラスを使わないので実装しない
        return false;
    }
    /***
     *
     * @param emv_program
     * @param bytes
     * @param i
     * @param bytes1
     * @param ints
     * @return
     */
    @Override
    public int cb_cl_apdu_exchange(EMV_PROGRAM emv_program, byte[] bytes, int i, byte[] bytes1, int[] ints)
    {
        int ret=0;
         {
             try {
                 byte[] res = _cm.mEmvContactlessCard.transmit(bytes);
                 byte[] failTemp = {0x11, 0x11};
                 if (res != null && res.length != 2 && Arrays.equals(res, failTemp)) {
                 ret = -1;
                 _debugLog.d("apdu response fail");
                 } else if(res == null){
                 ret = -2;
                 }
                 else {
                 System.arraycopy(res, 0, bytes1, 0, res.length);
                 ints[0] = res.length;
                     _debugLog.d("java apduresp: " + "length: " + ints[0] + " rsep " + ISOUtil.byte2hex(bytes1,0,ints[0]));
                 ret = 1;
                 }
             } catch (SDKException e) {
                e.printStackTrace();
             }
         }
        return ret;
    }
    @Override
    public int cb_cl_apdu_send(EMV_PROGRAM emv_program, byte[] bytes, int i) {
        _debugLog.d("jnilog callback>>>>cb_cl_apdu_send");
        _debugLog.d("java apducmd: " + " length: " + i + " cmd " + ISOUtil.byte2hex(bytes));
        apduRcvLen =0;
        int ret=0;
        {
            try {
                byte[] res = _cm.mEmvContactlessCard.transmit(bytes);
                byte[] failTemp = {0x11, 0x11};
                if (res != null && res.length != 2 && Arrays.equals(res, failTemp)) {
                    ret = -1;
                    _debugLog.d("apdu response fail");
                } else if(res == null){
                    ret = -2;
                }
                else {
                    System.arraycopy(res, 0, apduRcvData, 0, res.length);
                    apduRcvLen = res.length;
                    _debugLog.d("java apduresp: " + "length: " + apduRcvLen + " rsep " + ISOUtil.byte2hex(apduRcvData,0,res.length));
                    ret = 0;//means success
                }
            } catch (SDKException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    @Override
    public int cb_cl_apdu_resp_get(EMV_PROGRAM emv_program, boolean[] booleans, byte[] bytes, int[] ints) {
        _debugLog.d("jnilog callback>>>>cb_cl_apdu_resp_get");
        if(apduRcvLen >0){
            System.arraycopy(apduRcvData,0,bytes,0, apduRcvLen);
            ints[0]= apduRcvLen;
            _debugLog.d("jnilog callback>>>>cb_cl_apdu_resp_get apdu_recv_len ="+ apduRcvLen);
            return 0;
        }
        return 1;
    }

    @Override
    public int cb_kernel_param_count_get(EMV_PROGRAM emv_program) {
        _debugLog.d("jnilog callback>>>>cb_kernel_param_count_get");
        return 0;
    }

    @Override
    public boolean cb_kernel_param_get(EMV_PROGRAM emv_program, int i, byte[] bytes, byte[] bytes1, int[] ints) {
        _debugLog.d("jnilog callback>>>>cb_kernel_param_get");
        return false;
    }

    @Override
    public int cb_app_param_count_get(EMV_PROGRAM emv_program) {
        _debugLog.d("jnilog callback>>>>cb_app_param_count_get ="+ _aidCount);
        return _aidCount;
    }

    @Override
    public boolean cb_app_param_get(EMV_PROGRAM emv_program, int app_index, byte[] kernel_id, byte[] trans_type,
                                    byte[] aid, int[] aid_len, boolean[] asi, byte[] app_param, int[] app_param_len) {
        _debugLog.d("jnilog callback>>>>cb_app_param_get ="+ _aidCount);
        if(_aidCount >=app_index) {
            byte []baid= _aidParam[app_index].GetAid();
            if(baid==null)
                return false;
     //       _debugLog.d("cb_app_param_get baid="+ISOUtil.byte2hex(baid));
            aid_len[0]=baid.length;
        //    _debugLog.d("cb_app_param_get aid_len="+aid_len[0]);
            asi[0] = _aidParam[app_index].GetAsi();
            byte []pparam= _aidParam[app_index].GetParam();
            if(pparam!=null)
                app_param_len[0] = pparam.length;
            else
                app_param_len[0] =0;
     //       _debugLog.d("cb_app_param_get app_param_len="+app_param_len[0]);
            kernel_id[0] = _aidParam[app_index].GetKernelid();
            trans_type[0] = _aidParam[app_index].GetTrantype();
            if (aid_len[0] > 0)
                System.arraycopy(baid, 0, aid, 0, aid_len[0]);
            if (app_param_len[0] > 0)
                System.arraycopy(pparam, 0, app_param, 0, app_param_len[0]);
            _debugLog.d("cb_app_param_get aid ["+app_index+"]: kernelid ="+ISOUtil.hexString(new byte[]{kernel_id[0]}) +"; tran type ="+trans_type[0]+
                    ";aid ="+ISOUtil.byte2hex(aid,0,aid_len[0])+";param ="+ISOUtil.byte2hex(app_param,0,app_param_len[0]));
            return true;
        }
        return false;
    }
    /**
     *
     *
     * @param emv_program
     * @param rid
     * @param data_index
     * @param type
     * @param emv_public_key
     * @return 鍵の存在有無 falseの場合は失敗で処理が終了する
     */
    @Override
    public boolean cb_capk_get(EMV_PROGRAM emv_program, byte[] rid, byte data_index, byte type, EMV_PUBLIC_KEY[] emv_public_key) {
        _debugLog.d("execute cb_capk_get");

        for (CAPK capk : _app.getCAPK()) {
            String sRid = capk.getBlandSign().equals(BrandSign.JCB) ? RID.JCB
                    : capk.getBlandSign().equals(BrandSign.Diners) ? RID.Diners
                    : capk.getBlandSign().equals(BrandSign.Amex) ? RID.Amex
                    : capk.getBlandSign().equals(BrandSign.VISA) ? RID.VISA
                    : capk.getBlandSign().equals(BrandSign.Mastercard) ? RID.Mastercard
                    : null;

            if (sRid == null) {
                continue;
            }

            String sIndex = capk.getCaPublicKeyIndex();
            if(sIndex == null || sIndex.equals("0")) {
                continue;
            }

            byte[] su = ISOUtil.hex2byte(sRid);

            if (Arrays.equals(rid, su) && (ISOUtil.byte2int(new byte[]{data_index}) == ISOUtil.parseInt(sIndex,16))) {
                String sExponent = capk.getCaPublicKeyExponent();

                if(sExponent==null) {
                    continue;
                }

                int ExponentLn=sExponent.length()/2;

                EMV_PUBLIC_KEY emvPublicKey = new EMV_PUBLIC_KEY();
                EMV_PUBLIC_KEY_RSA emv_public_key_rsa = new EMV_PUBLIC_KEY_RSA();
                emv_public_key_rsa.setExponentLn(ExponentLn);

                String sModule = capk.getCaPublicKeyModulus();

                if(sModule==null) {
                    continue;
                }

                int ModuleLn=sModule.length()/2;
                emv_public_key_rsa.setModulusLn(ModuleLn);
                byte []exponent=ISOUtil.hex2byte(sExponent);
                emv_public_key_rsa.setExponent(exponent);
                byte [] module=ISOUtil.hex2byte(sModule);
                emv_public_key_rsa.setModulus(module);
                emvPublicKey.setKeyType(EMV_ENUM.EMV_RSA);
                emvPublicKey.setPublicKeyRsa(emv_public_key_rsa);
                emv_public_key[0]=emvPublicKey;

                return true;
            }
        }

        return false;
    }

    /*
     * Issuer Public Keyの失効リストチェック
     * 失効リストの配信をしないため実装しない
     * RIDとCA公開鍵のインデックスとIssuer Public Keyのシリアル番号
     * シリアル番号はCA公開鍵でIssuer Public Key Certificate(Tag'90')を検証したデータから取得できる
     */
    @Override
    public boolean cb_ipkc_revock_check(EMV_PROGRAM emv_program, byte[] rid, byte index, byte[] serial_num) {
        return false;
    }

    /*
     * 事故カードのチェックはしないため実装しない
     */
    @Override
    public boolean cb_exception_file_check(EMV_PROGRAM emv_program, byte[] pan, int pan_len, byte[] pan_seq, int pan_seq_len) {
        return false;
    }

    @Override
    public void cb_ui_display_processing(EMV_PROGRAM emv_program) {
        _debugLog.d("jnilog callback>>>>cb_ui_display_processing");
    }

    @Override
    public boolean cb_ui_application_select(
            EMV_PROGRAM emv_program,
            boolean contactless,
            int app_count,
            byte[] kernels,
            byte[][] aids,
            byte[][] lables,
            int[][] other_tags_len,
            byte[][] other_tags,
            int[] Selected
    ) {
        if (contactless && aids != null && app_count > 0) {
            // Priority順にソートされていると信じたい
            Selected[0] = 0;
            _debugLog.i("複数アプリケーションを検出 選択されたAID: %s", new String(aids[Selected[0]]));
            return true;
        } else {
            // 接触でこのライブラリは使用しないため実装しない
            return false;
        }
    }

    @Override
    public boolean cb_ui_cardholder_confirm(EMV_PROGRAM emv_program, String s, String s1) {
        _debugLog.d("jnilog callback>>>>cb_ui_cardholder_confirm string1="+s+"  ;string 2 ="+s1);
        return true;
    }

    @Override
    public boolean cb_ui_language_select(EMV_PROGRAM emv_program, String s) {
        // 多言語対応していないため実装しない
        return false;
    }

    @Override
    public boolean cb_ui_credentials_check(EMV_PROGRAM emv_program, byte b, String s) {
        _debugLog.d("jnilog callback>>>>cb_ui_credentials_check");
        return false;
    }

    @Override
    public boolean cb_ped_verify_status_show(EMV_PROGRAM emv_program, boolean b, byte b1) {
        offlinePinTryCnt = ISOUtil.bcd2int(b1);
        _debugLog.d("jnilog callback>>>>cb_ped_verify_status_show verify_success ="+b+" ,pin_try_counter ="+ISOUtil.bcd2int(b1));

        return true;
    }
    int offlinePinTryCnt = Integer.MAX_VALUE;
    @Override
    public boolean cb_ped_plaintext_pin_verify(EMV_PROGRAM emv_program, boolean[] booleans, byte[] i) {
        /***       booleans[0]=false;
         i[0]=(byte)0x90;
         i[1]=0x00;
         return true;
         ***/
        _debugLog.d("jnilog callback>>>>cb_ped_plaintext_pin_verify");
        int slot=Ped.getInstance().getIccSlot(SlotType.USER_CARD);
        IccOfflinePinApdu iccOfflinePinApdu=new IccOfflinePinApdu();
        iccOfflinePinApdu.setCla(0x00);
        iccOfflinePinApdu.setIns(0x20);
        iccOfflinePinApdu.setLe(0x00);
        iccOfflinePinApdu.setLeflg(0x00);
        iccOfflinePinApdu.setP1(0x00);
        iccOfflinePinApdu.setP2(0x80);
        final PadView padView = new PadView();
        padView.setTitleMsg("Newpos Secure Keyboard");
        if(offlinePinTryCnt == Integer.MAX_VALUE) {
            padView.setPinTips("Please Enter Offline PIN\n");
        }else{
            padView.setPinTips("Offline Pin , Remain Time : "+offlinePinTryCnt+"\n");
        }

        Ped.getInstance().setPinPadView(padView);
        final int[] Errno = new int[1];
        final byte[]offlinepin = new byte[8];
        final int[] offlinelen = {0};
        final CountDownLatch cdl=new CountDownLatch(1);
        Ped.getInstance().getOfflinePin(KeySystem.ICC_PLAIN,slot,"0,4,5,6,7,8,9,10,11,12",iccOfflinePinApdu,new PinBlockCallback() {
            @Override
            public void onPinBlock(int i, byte[] bytes) {
                _debugLog.d("errno ="+i);
                Errno[0] =i;
                if(bytes!=null) {
                    _debugLog.d("bytes ="+ISOUtil.byte2hex(bytes));
                    System.arraycopy(bytes, 0, offlinepin, 0, bytes.length);
                    offlinelen[0] =bytes.length;
                }
                cdl.countDown();
            }
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(offlinepin!=null&&offlinelen[0] >=2){
            i[0]=offlinepin[offlinelen[0] -2];
            i[1]=offlinepin[offlinelen[0] -1];
            //           i2[0]=(char)ISOUtil.bcd2int(offlinepin,offlinepin.length-2,2);
        }
        else{
            i[0]=0x00;
            i[1]=0x00;
        }
        if(Errno[0]== PedRetCode.ENTER_CANCEL) {
            booleans[0]=false;
            return false;
        }
        if(Errno[0]== PedRetCode.NO_PIN){
            booleans[0]=true;
            i[0]=(byte)0x90;
            i[1]=0x00;
            return true;
        }
        if(Errno[0]==0){
            booleans[0]=false;
            return true;
        }
        return false;
    }

    @Override
    public boolean cb_ped_encipher_pin_verify(EMV_PROGRAM emv_program, byte[] bytes, byte[] bytes1, int i, byte[] bytes2, int i1, boolean[] booleans, byte[] i2) {
        /**     _debugLog.d("jnilog callback>>>>cb_ped_encipher_pin_verify");
         booleans[0]=false;
         i2[0]=(byte)0x90;
         i2[1]=0x00;
         return true;
         ***/
        int slot=Ped.getInstance().getIccSlot(SlotType.USER_CARD);
        IccOfflinePinApdu iccOfflinePinApdu=new IccOfflinePinApdu();
        iccOfflinePinApdu.setCla(0x00);
        iccOfflinePinApdu.setIns(0x20);
        iccOfflinePinApdu.setLe(0x00);
        iccOfflinePinApdu.setLeflg(0x00);
        iccOfflinePinApdu.setP1(0x00);
        iccOfflinePinApdu.setP2(0x88);
        RsaPinKey rsaPinKey=new RsaPinKey();
        rsaPinKey.setIccrandom(bytes);
        rsaPinKey.setMod(bytes1);
        rsaPinKey.setModlen(i);
        rsaPinKey.setExp(bytes2);
        rsaPinKey.setExplen(i1);
        iccOfflinePinApdu.setRsakey(rsaPinKey);
        //set ped view
        final PadView padView = new PadView();
        padView.setTitleMsg("Newpos Secure Keyboard");
        padView.setPinTips("Please enter offline PIN\n");
        Ped.getInstance().setPinPadView(padView);
        final int[] Errno = new int[1];
        final byte[]offlinepin = new byte[2];
        final int[] offlinelen = {0};
        final CountDownLatch cdl=new CountDownLatch(1);
        Ped.getInstance().getOfflinePin(KeySystem.ICC_CIPHER,slot,"0,4,5,6,7,8,9,10,11,12",iccOfflinePinApdu,new PinBlockCallback() {
            @Override
            public void onPinBlock(int i, byte[] bytes) {
                _debugLog.d("onPinBlock ="+i);
                Errno[0] =i;
                if(bytes!=null) {
                    _debugLog.d("bytes ="+ISOUtil.byte2hex(bytes));
                    if(bytes.length>=2)
                        System.arraycopy(bytes, bytes.length-2, offlinepin, 0, 2);
                    else
                        System.arraycopy(ISOUtil.hex2byte("0000"),0,offlinepin,0,2);
                }
                cdl.countDown();
            }
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(offlinepin!=null&&offlinelen[0] >=2){
            i2[0]=offlinepin[offlinelen[0] -2];
            i2[1]=offlinepin[offlinelen[0] -1];
            //           i2[0]=(char)ISOUtil.bcd2int(offlinepin,offlinepin.length-2,2);
        }
        else{
            i2[0]=0x00;
            i2[1]=0x00;
        }
        if(Errno[0]== PedRetCode.NO_PIN){
            booleans[0]=true;
            i2[0]=(byte)0x90;
            i2[1]=0x00;
            return true;
        }
        if(Errno[0]==0){
            booleans[0]=false;
            return true;
        }
        return false;

    }

    @Override
    public boolean cb_ped_online_pin_get(EMV_PROGRAM emv_program, String s, boolean[] booleans, byte[] bytes) {
        _debugLog.d("jnilog callback>>>>cb_ped_online_pin_get");
        final byte[] pblock = new byte[16];
        final int []result={0};
        final Ped ped = Ped.getInstance() ;
        PadView padView = new PadView();
        padView.setTitleMsg("Newpos Secure Keyboard");
        padView.setPinTips("Please Enter Online PIN:");
        ped.setPinPadView(padView);
        final CountDownLatch cdl=new CountDownLatch(1);
        String pinCardNo;
        if(s.length()>=12) {
            pinCardNo = s.substring(s.length() - 13, s.length() - 1);
            pinCardNo = ISOUtil.padleft(pinCardNo, pinCardNo.length() + 4, '0');
        }
        else
            pinCardNo = ISOUtil.padleft(s, 16, '0');
        final String finalPinCardNo = pinCardNo;
        String expectPinlen="0,4,5,6,7,8,9,10,11,12";
        if(booleans!=null){
            if(booleans[0]){
                expectPinlen="0,4,5,6,7,8,9,10,11,12";
            }else{
                expectPinlen="4,5,6,7,8,9,10,11,12";
            }
        }
        final String finalexpectPinlen = expectPinlen;
        new Thread(){
            @Override
            public void run() {
                ped.getPinBlock(KeySystem.MS_DES,
                        0,
                        PinBlockFormat.PIN_BLOCK_FORMAT_0,
                        finalexpectPinlen,
                        finalPinCardNo,
                        new PinBlockCallback() {
                            @Override
                            public void onPinBlock(int i, byte[] bytes) {
                                result[0]=i;
                                _debugLog.d("cb_ped_online_pin_get errno ="+i);
                                switch (i){
                                    case PedRetCode.NO_PIN :
                                        //listener.showMsg("has not pin input: false",Presenter.MSG_RESULT);
                                        break;
                                    case PedRetCode.TIMEOUT:
                                        //listener.showMsg("pin input timeout: false",Presenter.MSG_RESULT);
                                        break;
                                    case PedRetCode.ENTER_CANCEL:
                                        //listener.showMsg("user cancel pin input: false",Presenter.MSG_RESULT);
                                        break;
                                    case 0:
                                        //listener.showMsg("get pin block: "+ISOUtil.byte2hex(bytes),Presenter.MSG_CONTENT);
                                        //listener.showMsg("test pin end: true ",Presenter.MSG_RESULT);
                                        System.arraycopy(bytes,0,pblock,0,bytes.length);
                                        break;
                                    default:
                                        //listener.showMsg("unknow error: false ",Presenter.MSG_RESULT);
                                        break;
                                }
                                cdl.countDown();
                            }
                        });
            }
        }.start();
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        booleans[0]=false;
        if(result[0]==PedRetCode.NO_PIN){
            _debugLog.d("NO_PIN,bypass");
            booleans[0]=true;
            return true;
        }
        if(result[0]==0){
            if(bytes!=null&&pblock!=null&&pblock.length>8)
                System.arraycopy(pblock,0,bytes,0,8);
            return true;
        }
        return false;
    }

    @Override
    public void cb_user_request_interface_send(EMV_PROGRAM emv_program, byte b, EMV_UI_REQUEST_DATA emv_ui_request_data) {
        _debugLog.d("jnilog callback>>>>cb_user_request_interface_send");
        // Todo 実装する
    }

    /*
     * Dynamic Reader Limits
     * VISA・Amexにおいてリミットセットを上書きするパラメータ
     * 不要なので実装しない
     */
    @Override
    public int cb_drl_count_get(EMV_PROGRAM emv_program, byte kernel_id, int type) {
        _debugLog.d("jnilog callback>>>>cb_drl_count_get");

        return 0;
    }

    /*
     * Dynamic Reader Limits
     * VISA・Amexにおいてリミットセットを上書きするパラメータ
     * 不要なので実装しない
     */
    @Override
    public boolean cb_drl_get(EMV_PROGRAM emv_program, byte kernel_id, int type, int rec_no, byte[] param, int[] param_len) {
        _debugLog.d("jnilog callback>>>>cb_drl_get= %s, type = %s", rec_no, type);

        return true;
    }

    @Override
    public boolean cb_torn_record_send(EMV_PROGRAM emv_program, byte b, int i, int i1, byte[] bytes, int i2) {
        _debugLog.d("jnilog callback>>>>cb_torn_record_send");
        return false;
    }

    @Override
    public boolean cb_mute_record_save(EMV_PROGRAM emv_program, byte b, int i, byte[] bytes, int i1) {
        _debugLog.d("jnilog callback>>>>cb_mute_record_save");
        return false;
    }

    @Override
    public boolean cb_mute_record_delete(EMV_PROGRAM emv_program, byte b, int i, boolean b1) {
        return false;
    }

    @Override
    public boolean cb_dek_send(EMV_PROGRAM exe, byte kernel_id, byte[] data, int data_len) {
        return false;
    }

    @Override
    public boolean cb_det_get(EMV_PROGRAM exe, byte kernel_id, byte[] data, int data_len) {
        return false;
    }

    private String getTagValue(IniFile iniFile,String group, String key, String default_value){
        IniSection iniSection=iniFile.getSection(group);
        if(iniSection==null)
            return default_value;
        IniItem iniItem=iniSection.getItem(key);
        if (iniItem==null) {
            return default_value;
        }
        return iniItem.getValue();
    }
    public void loadAidParam() {
        _aidCount = _app.getRiskManagementParameter() != null
                ? _app.getRiskManagementParameter().length * 2
                : 0;

        _aidParam = new AidParam[_aidCount];
        for (int i = 0; i < _aidCount; i = i+2) {
            final RiskManagementParameter p = _app.getRiskManagementParameter()[i/2];

            _aidParam[i] = new AidParam();
            _aidParam[i].SetAid(ISOUtil.hex2byte(p.getAid()));
            _aidParam[i].SetKernelid(p.getKernelId().byteValue());
            _aidParam[i].SetTrantype(EMV_ENUM.EMV_TRANS_PURCHASE);
            _aidParam[i].SetParam(ISOUtil.hex2byte(p.toTLV(EMV_ENUM.EMV_TRANS_PURCHASE)));
            _aidParam[i].SetAsi(true);

            _aidParam[i+1] = new AidParam();
            _aidParam[i+1].SetAid(ISOUtil.hex2byte(p.getAid()));
            _aidParam[i+1].SetKernelid(p.getKernelId().byteValue());
            _aidParam[i+1].SetTrantype(EMV_ENUM.EMV_TRANS_REFUND);
            _aidParam[i+1].SetParam(ISOUtil.hex2byte(p.toTLV(EMV_ENUM.EMV_TRANS_REFUND)));
            _aidParam[i+1].SetAsi(true);
        }
    }
}
