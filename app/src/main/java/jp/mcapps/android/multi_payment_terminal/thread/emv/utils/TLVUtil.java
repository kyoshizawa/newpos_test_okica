package jp.mcapps.android.multi_payment_terminal.thread.emv.utils;

import com.pos.device.SDKException;
import com.pos.device.emv.EMVHandler;
import com.pos.device.emv.IEMVHandler;

import java.util.ArrayList;
import java.util.List;

public class TLVUtil {
    /**
     * @param src
     * @param totalLen
     * @param tag
     * @param value
     * @param withTL
     * @return
     */
    public static int getTlvData(byte[] src, int totalLen, int tag,
                                 byte[] value, boolean withTL) {
        int i, Tag, Len;
        int T;

        if (totalLen == 0)
            return 0;

        i = 0;
        while (i < totalLen) {
            T = i;

            if ((src[i] & 0x1f) == 0x1f) {
                Tag = ISOUtil.byte2int(src, i, 2);
                i += 2;
            } else {
                Tag = ISOUtil.byte2int(new byte[]{src[i++]});
            }

            Len = ISOUtil.byte2int(new byte[]{src[i++]});
            if ((Len & (byte) 0x80) != 0) {
                int lenL = Len & 3;
                Len = ISOUtil.byte2int(src, i, lenL);
                i += lenL;
            }
            if (tag == Tag) // 找到
            {
                if (withTL) // 包含Tag和Len
                {
                    Len = Len + (i - T);
                    if (value.length >= Len) {
                        System.arraycopy(src, T, value, 0, Len);
                        return Len;
                    } else {
                        return 0;
                    }
                } else // 不包含Tag和Len
                {
                    if (value.length >= Len) {
                        System.arraycopy(src, i, value, 0, Len);
                        return Len;
                    } else {
                        return 0;
                    }
                }
            } else
                i += Len;
        }
        return 0;
    }

    /**
     * 用标签从内核中获得一个数据
     *
     * @param iTag TLV数据的标签
     * @param data 返回的数据
     * @return 真实长度
     */
    public static int getTlvDataKernel(int iTag, byte[] data) {
        IEMVHandler handler = EMVHandler.getInstance();
        int len = 0;
        byte[] Tag;
        if (iTag < 0x100) {
            Tag = new byte[1];
            Tag[0] = (byte) iTag;
        } else {
            Tag = new byte[2];
            Tag[0] = (byte) (iTag >> 8);
            Tag[1] = (byte) iTag;
        }
        //LogUtil.d("Tag = " + ISOUtil.hexString(Tag));
        if (handler.checkDataElement(Tag) == 0) {
            try {
                byte[] result = handler.getDataElement(Tag);
                //LogUtil.d("getTlvDataKernel result = " + ISOUtil.hexString(result));
                System.arraycopy(result, 0, data, 0, result.length);
                len = result.length;
            } catch (SDKException e) {
                e.printStackTrace();
            }
        } else if (iTag == 0xDF31) {// 脚本结果
            byte[] result = handler.getScriptResult();
            if (result != null) {
                System.arraycopy(result, 0, data, 0, result.length);
                len = result.length;
            }
        }
        return len;
    }

    /**
     * 从内核拿一组tag标签的数据，包装成一串tlv
     **/
    public static int packTags(int[] iTags, byte[] dest) {
        int i, iTag_len, len;
        byte[] Tag = new byte[2];
        int offset = 0;
        byte[] ptr = new byte[256];

        i = 0;
        while (iTags[i] != 0) {

            if (iTags[i] < 0x100) {
                iTag_len = 1;
                Tag[0] = (byte) iTags[i];
            } else {
                iTag_len = 2;
                Tag[0] = (byte) (iTags[i] >> 8);
                Tag[1] = (byte) iTags[i];
            }

            len = getTlvDataKernel(iTags[i], ptr);
            if (len > 0) {
                System.arraycopy(Tag, 0, dest, offset, iTag_len);// 拷标签
                offset += iTag_len;

                if (len < 128) {
                    dest[offset++] = (byte) len;
                } else if (len < 256) {
                    dest[offset++] = (byte) 0x81;
                    dest[offset++] = (byte) len;
                }

                System.arraycopy(ptr, 0, dest, offset, len);
                offset += len;
            }

            i++;
        }
        return offset;
    }

    /**
     * pack a _tlv data
     *
     * @param result out
     * @param tag
     * @param len
     * @param value  in
     * @return
     */
    public static int packTlvData(byte[] result, int tag, int len,
                                  byte[] value, int valueOffset) {
        byte[] temp = null;
        int offset = 0;

        if (len == 0 || value == null || result == null)
            return 0;

        temp = result;
        if (tag > 0xff) {
            temp[offset++] = (byte) (tag >> 8);
            temp[offset++] = (byte) tag;
        } else
            temp[offset++] = (byte) tag;

        if (len < 128) {
            temp[offset++] = (byte) len;
        } else if (len < 256) {
            temp[offset++] = (byte) 0x81;
            temp[offset++] = (byte) len;
        } else {
            temp[offset++] = (byte) 0x82;
            temp[offset++] = (byte) (len >> 8);
            temp[offset++] = (byte) len;
        }

        // memmove(p, value, len);
        System.arraycopy(value, valueOffset, temp, offset, len);

        return offset + len;
    }

    /**
     * 根据AID查询内卡还是外卡
     */

    public static String getIssureByAID(String rid) {
        String cardCode = null;
        if (rid.length() < 10)
            return "CUP";
        if (rid.length() > 10) {
            cardCode = rid.substring(0, 10);
        } else
            cardCode = rid;

        if (cardCode.equals("A000000003"))
            return "VIS";
        if (cardCode.equals("A000000004"))
            return "MCC";
        if (cardCode.equals("A000000065"))
            return "JCB";
        if (cardCode.equals("A000000025"))
            return "AEX";
        return "CUP";
    }

    public static void sealAid(String aid) {
        List<EMVTLV> list = new ArrayList<>();
        EMVTLV emvtlv;
        for (int i = 0; i < aid.length(); ) {
            emvtlv = new EMVTLV();
            emvtlv.tag = aid.substring(i, i + 4);
            System.out.println("tag:" + emvtlv.tag);
            System.out.println("length:" + aid.substring(i + 4, i + 6));
            String temp1 = aid.substring(i + 4, i + 5);
            Integer.parseInt(temp1);
            String temp2 = aid.substring(i + 5, i + 6);
            Integer.parseInt(temp2);
            emvtlv.length = Integer.parseInt(temp1) * 16 + Integer.parseInt(temp2);
            emvtlv.value = aid.substring(i + 6, i + 6 + emvtlv.length * 2);
            System.out.println("value:" + emvtlv.value);
            i += 6 + emvtlv.length * 2;
            System.out.println("current:" + i);
        }
    }

    private static class EMVTLV {
        private String tag;
        private String value;
        private int length;
    }
}
