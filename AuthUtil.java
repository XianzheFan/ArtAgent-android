package com.fxz.artagent;

import okhttp3.HttpUrl;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SignatureException;

/**
 * 生成签名后的url工具类
 */
public class AuthUtil {
    private static String algorithm = "hmac-sha256";

    public static String generateAuthorization(Hmac256Signature signature) throws SignatureException {
        return generateAuthorization(signature, algorithm);
    }

    public static String generateAuthorization(Hmac256Signature signature, String algorithm) throws SignatureException {
        return String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", signature.getId(), algorithm, "host date request-line", signature.getSigna());
    }

    public static String generateRequestUrl(Hmac256Signature signature) throws MalformedURLException, SignatureException {
        URL url = new URL(signature.getUrl());
        String authorization = generateAuthorization(signature);
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().addQueryParameter("authorization", CryptTools.base64Encode(authorization)).addQueryParameter("date", signature.getTs()).addQueryParameter("host", url.getHost()).build();
        return httpUrl.toString();
    }
}