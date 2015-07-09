package com.honsal.geosource.etc;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by 이 on 2015-06-08.
 */
public class GeomexRC4 {
    // 암호화 및 복호화 키
    private static final String KEY = "5102TFOSXEMOEG";

    /**
     * 암호화 메소드
     * @param plainText 암호화할 문자열의 byte array
     * @return 암호화된 byte array
     */
    public static byte[] encrypt(byte[] plainText) {
        try {
            Cipher rc4 = Cipher.getInstance("RC4");
            SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "RC4");
            rc4.init(Cipher.ENCRYPT_MODE, key);
            return rc4.update(plainText);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 복호화 메소드
     * @param cipherText 복호화할 문자열의 byte array
     * @return 복호화된 byte array
     */
    public static byte[] decrypt(byte[] cipherText) {
        try {
            Cipher rc4 = Cipher.getInstance("RC4");
            SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "RC4");
            rc4.init(Cipher.DECRYPT_MODE, key);
            return rc4.update(cipherText);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
