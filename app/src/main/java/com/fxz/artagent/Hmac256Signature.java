package com.fxz.artagent;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Hmac256Signature {
    private final String id;
    private final String key;
    private final String url;
    private String requestMethod = "GET";
    private final String ts;
    private String originSign;
    private String signa;

    public Hmac256Signature(String id, String key, String url) {
        this.id = id;
        this.key = key;
        this.url = url;
        this.ts = this.generateTs();
    }

    public Hmac256Signature(String id, String key, String url, String method) {
        this.id = id;
        this.key = key;
        this.url = url;
        this.requestMethod = method;
        this.ts = this.generateTs();
    }

    public String generateTs() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date());
    }

    public String generateOriginSign() throws SignatureException {
        try {
            URL url = new URL(this.url);
            return "host: " + url.getHost() + "\ndate: " + this.ts + "\n" + this.requestMethod + " " + url.getPath() + " HTTP/1.1";
        } catch (MalformedURLException var2) {
            throw new SignatureException("MalformedURLException:" + var2.getMessage());
        }
    }

    public String getSigna() throws SignatureException {
        if (this.signa == null || this.signa.isEmpty()) {
            this.originSign = this.generateOriginSign();
            this.signa = this.generateSignature();
        }
        return this.signa;
    }

    public String generateSignature() throws SignatureException {
        return CryptTools.hmacEncrypt("HmacSHA256", this.originSign, this.key);
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTs() {
        return ts;
    }
}