package com.fxz.artagent;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

public class CryptTools {
    public static String hmacEncrypt(String encryptType, String plainText, String encryptKey) throws SignatureException {
        try {
            byte[] data = encryptKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(data, encryptType);
            Mac mac = Mac.getInstance(encryptType);
            mac.init(secretKey);
            byte[] text = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] rawHmac = mac.doFinal(text);
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (InvalidKeyException var8) {
            throw new SignatureException("InvalidKeyException:" + var8.getMessage());
        } catch (NoSuchAlgorithmException var9) {
            throw new SignatureException("NoSuchAlgorithmException:" + var9.getMessage());
        }
    }

    public static String base64Encode(String plainText) {
        return Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] base64Decode(String base64Text) {
        return Base64.getDecoder().decode(base64Text);
    }
}