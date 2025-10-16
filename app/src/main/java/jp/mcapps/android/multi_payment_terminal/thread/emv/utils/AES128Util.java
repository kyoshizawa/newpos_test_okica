package jp.mcapps.android.multi_payment_terminal.thread.emv.utils;


import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES128Util {
    //算法名

    public static final String KEY_ALGORITHM = "AES";

    //加解密算法/模式/填充方式

    //可以任意选择，为了方便后面与iOS端的加密解密，采用与其相同的模式与填充方式

    //ECB模式只用密钥即可对数据进行加密解密，CBC模式需要添加一个参数iv

    public static final String CIPHER_ALGORITHM_CBC = "AES/CBC/NoPadding";
    public static final String CIPHER_ALGORITHM_ECB = "AES/ECB/NoPadding";
    //生成iv

    private static AlgorithmParameters generateIV(String ivVal) throws Exception{

        //iv 为一个 16 字节的数组,数据全为0

        //byte[] iv = new byte[16];

        //Arrays.fill(iv, (byte) 0x00);

        //Arrays.fill(iv,ivVal.getBytes());

        byte[]iv=ISOUtil.hex2byte(ivVal);

        AlgorithmParameters params = AlgorithmParameters.getInstance(KEY_ALGORITHM);

        params.init(new IvParameterSpec(iv));

        return params;

    }



    //转化成JAVA的密钥格式

    private static Key convertToKey(byte[] keyBytes) throws Exception{

        SecretKey secretKey = new SecretKeySpec(keyBytes,KEY_ALGORITHM);

        return secretKey;

    }



    //加密

    public static byte[] encrypt(int mode,byte[] plainText,byte[] aesKey,String ivVal) throws Exception {

        AlgorithmParameters iv=generateIV(ivVal);
        //转化为密钥
        Key key = convertToKey(aesKey);
        Security.addProvider(new BouncyCastleProvider());
        Cipher cipher ;
        if(mode==0)
            cipher= Cipher.getInstance(CIPHER_ALGORITHM_ECB);
        else
            cipher= Cipher.getInstance(CIPHER_ALGORITHM_CBC);

        //设置为加密模式

        cipher.init(Cipher.ENCRYPT_MODE, key,iv);

        byte[] encryptData= cipher.doFinal(plainText);
        return encryptData;

    }



    //解密

    public static byte[] decrypt(int mode,byte[] encryptedData,byte[] aesKey,String ivVal) throws Exception{
        Key key = convertToKey(aesKey);
        Cipher cipher ;
        if(mode==0)
            cipher= Cipher.getInstance(CIPHER_ALGORITHM_ECB);
        else
            cipher= Cipher.getInstance(CIPHER_ALGORITHM_CBC);

        AlgorithmParameters iv=generateIV(ivVal);

        //设置为解密模式

        cipher.init(Cipher.DECRYPT_MODE, key,iv);

        byte[] decryptData=cipher.doFinal(encryptedData);
        return decryptData;

    }
}
