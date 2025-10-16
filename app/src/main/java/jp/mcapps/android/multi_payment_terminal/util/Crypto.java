package jp.mcapps.android.multi_payment_terminal.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    public static class RSA {
        private static final String ALGORITHM = "RSA";
        private static final String PADDING_RULE = "RSA/ECB/PKCS1PADDING";

        public static byte[] encrypt(byte[] plainText, BigInteger modulus, BigInteger exponent) {
            try {
                final KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
                final RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);

                final PublicKey key = keyFactory.generatePublic(spec);

                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.ENCRYPT_MODE, key);

                final byte[] cipherText = cipher.doFinal(plainText);

                return cipherText;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static byte[] encrypt(String plainText, BigInteger modulus, BigInteger exponent) {
            return encrypt(plainText.getBytes(), modulus, exponent);
        }

        public static byte[] decrypt(String encrypted, BigInteger modulus, BigInteger exponent) {
            try {
                final KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
                final RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);

                final PublicKey key = keyFactory.generatePublic(spec);

                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.DECRYPT_MODE, key);

                final byte[] encryptedBytes = McUtils.hexStringToBytes(encrypted);

                final byte[] plainText = cipher.doFinal(encryptedBytes);
                return plainText;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class AES256 {
        private static final String ALGORITHM = "AES";
        private static final int KEY_BITS = 256;
        private static final String PADDING_RULE = "AES/CBC/PKCS7Padding";


        public static byte[] encrypt(String plainText, byte[] key, byte[] iv) {
            return encrypt(plainText.getBytes(), key, iv);
        }

        public static byte[] encrypt(byte[] plainText, byte[] key, byte[] iv) {
            try {
                final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
                final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);

                byte[] cipherText = cipher.doFinal(plainText);

                return cipherText;
            } catch (Exception e) {
                return null;
            }
        }

        public static byte[] decrypt(byte[] encrypted, byte[] key, byte[] iv) {
            final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            try {
                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

                byte[] cipherText = cipher.doFinal(encrypted);
                return cipherText;
            } catch (Exception e) {
                return null;
            }
        }

        // 戻り値は第1引数にキー、第2引数に初期化ベクタ
        public static byte[][] generateKeyAndIV() {
            try {
                final KeyGenerator keygen = KeyGenerator.getInstance(ALGORITHM);
                keygen.init(KEY_BITS);

                final SecretKey key = keygen.generateKey();
                final Cipher cipher = Cipher.getInstance(PADDING_RULE);

                cipher.init(Cipher.ENCRYPT_MODE, key);

                final byte[] iv = cipher.getIV();

                return new byte[][] { key.getEncoded(), iv };
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class AES128 {
        private static final String ALGORITHM = "AES";
        private static final int KEY_BITS = 128;
        private static final String PADDING_RULE = "AES/ECB/PKCS5Padding";


        public static byte[] encrypt(String plainText, byte[] key) {
            return encrypt(plainText.getBytes(), key);
        }

        public static byte[] encrypt(byte[] plainText, byte[] key) {
            try {
                final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);

                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);

                byte[] cipherText = cipher.doFinal(plainText);

                return cipherText;
            } catch (Exception e) {
                return null;
            }
        }

        public static byte[] decrypt(byte[] encrypted, byte[] key, byte[] iv) {
            final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            try {
                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

                byte[] cipherText = cipher.doFinal(encrypted);
                return cipherText;
            } catch (Exception e) {
                return null;
            }
        }

        public static byte[] decrypt(byte[] encrypted, byte[] key) {
            final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);

            try {
                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.DECRYPT_MODE, keySpec);

                byte[] cipherText = cipher.doFinal(encrypted);
                return cipherText;
            } catch (Exception e) {
                return null;
            }
        }

        // 戻り値は第1引数にキー、第2引数に初期化ベクタ
        public static byte[][] generateKeyAndIV() {
            try {
                final KeyGenerator keygen = KeyGenerator.getInstance(ALGORITHM);
                keygen.init(KEY_BITS);

                final SecretKey key = keygen.generateKey();
                final Cipher cipher = Cipher.getInstance(PADDING_RULE);

                cipher.init(Cipher.ENCRYPT_MODE, key);

                final byte[] iv = cipher.getIV();

                return new byte[][] { key.getEncoded(), iv };
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class TripleDES {
        private static final String ALGORITHM = "DESede";
        private static final String PADDING_RULE = "DESede/CBC/NoPadding";

        public static byte[] encrypt(String plainText, byte[] key, byte[] iv) {
            return encrypt(plainText.getBytes(), key, iv);
        }

        public static byte[] encrypt(byte[] plainText, byte[] key, byte[] iv) {
            try {
                final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
                final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);

                byte[] cipherText = cipher.doFinal(plainText);

                return cipherText;
            } catch (Exception e) {
                return null;
            }
        }

        public static byte[] decrypt(byte[] encrypted, byte[] key, byte[] iv) {
            final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            try {
                final Cipher cipher = Cipher.getInstance(PADDING_RULE);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

                byte[] cipherText = cipher.doFinal(encrypted);
                return cipherText;
            } catch (Exception e) {
                return null;
            }
        }

        // 戻り値は第1引数にキー、第2引数に初期化ベクタ
        public static byte[][] generateKeyAndIV(int byteSize) {
            try {
                final KeyGenerator keygen = KeyGenerator.getInstance(ALGORITHM);
                keygen.init(byteSize * 8);

                final SecretKey key = keygen.generateKey();
                final Cipher cipher = Cipher.getInstance(PADDING_RULE);

                cipher.init(Cipher.ENCRYPT_MODE, key);

                final byte[] iv = cipher.getIV();

                return new byte[][] { key.getEncoded(), iv };
            } catch (Exception e) {
                return null;
            }
        }
    }
}
