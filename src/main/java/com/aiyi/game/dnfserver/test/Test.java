package com.aiyi.game.dnfserver.test;

import com.aiyi.game.dnfserver.utils.MinFieldUtil;
import com.aiyi.game.dnfserver.utils.RSATool;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class Test {
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        int uid = 18000002;
        String data = String.format("%08x010101010101010101010101010101010101010101010101010101010101010155914510010403030101", uid);
        byte[] dataBytes = hex2tobin(data);
        String private_key = new String(MinFieldUtil.readResource("private.key"))
                .replace("\r", "")
                .replace("\n", "");
        byte[] resultByte = RSATool.encryptByPrivateKey(dataBytes, private_key);
        String s = Base64.getEncoder().encodeToString(resultByte);
//        out.print(strEncode(s, msgkey, rand));
        System.out.println(s);
    }

    /**
     * 将十六进制字符串转换为字节数组
     * 相当于PHP中的hex2tobin函数
     *
     * @param hexStr 十六进制字符串
     * @return 转换后的字节数组
     */
    public static byte[] hex2tobin(String hexStr) {
        if (hexStr == null || hexStr.length() % 2 != 0) {
            throw new IllegalArgumentException("输入的十六进制字符串长度必须是偶数");
        }

        int len = hexStr.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexStr.charAt(i), 16) << 4)
                    + Character.digit(hexStr.charAt(i + 1), 16));
        }

        return data;
    }
}
