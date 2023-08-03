package com.fxz.artagent;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Map;

public class AppClient {
    private static final Logger logger = LoggerFactory.getLogger(AppClient.class);
    public static final int STATUS = 3;
    private static String resourcePath;

    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final String host;

    static {
        try {
            resourcePath = AppClient.class.getResource("/").toURI().getPath();
            if (resourcePath != null) {
                resourcePath = resourcePath.replaceAll("target/classes", "src/main/resources");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    public AppClient(String appId, String apiKey, String apiSecret, String host) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.host = host;
    }


    public String doRequest(JSONObject requestData, Map<String, String> requestPathMap) throws IOException, SignatureException {
        URL realUrl = new URL(buildAuthRequestUrl());
        URLConnection connection = realUrl.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-type","application/json");

        OutputStream out = httpURLConnection.getOutputStream();
        String newRequestData = this.buildRequestData(requestData, requestPathMap);
        out.write(newRequestData.getBytes());
        out.flush();

        logger.info("send data is :{}", newRequestData);

        InputStream is;
        try{
            is = httpURLConnection.getInputStream();
        }catch (Exception e){
            is = httpURLConnection.getErrorStream();
            logger.error("request message is {}, code is:{}", httpURLConnection.getResponseMessage(), readAllBytes(is));
        }

        String respData = readAllBytes(is);
        logger.info("respData:{}", respData);

        JSONObject jsonObject = JSONObject.parseObject(respData);
        JSONObject payload = jsonObject.getJSONObject("payload");
        JSONObject output_text = payload.getJSONObject("output_text");
        String text = output_text.getString("text");
        String textBase64Decode=new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
        System.out.println("\ntext字段解析结果：");
        System.out.println(textBase64Decode);

        return respData;
    }

    public String buildAuthRequestUrl() throws MalformedURLException, SignatureException {
        Hmac256Signature signature = new Hmac256Signature(this.apiKey, this.apiSecret, this.host, "POST");
        return AuthUtil.generateRequestUrl(signature);
    }

    public String buildRequestData(JSONObject requestData, Map<String, String> requestPathMap) throws IOException {
        for (Map.Entry<String, String> entry : requestPathMap.entrySet()) {
            String jsonPath = entry.getKey();
            String filePath = entry.getValue();

            File file = new File(resourcePath + filePath);
            byte[] fileByteFromFile = FileUtils.readFileToByteArray(file);

            JSONPath.set(requestData, jsonPath, Base64.getEncoder().encodeToString(fileByteFromFile));
            String statusJsonPath = jsonPath.substring(0, jsonPath.lastIndexOf(".")) + ".status";
            JSONPath.set(requestData, statusJsonPath, STATUS);
        }

        JSONPath.set(requestData, "$.header.app_id", this.appId);
        JSONPath.set(requestData, "$.header.status", STATUS);

        return requestData.toString();
    }

    private String readAllBytes(InputStream is) throws IOException {
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
}