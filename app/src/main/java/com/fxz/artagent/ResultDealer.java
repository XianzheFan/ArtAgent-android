package com.fxz.artagent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ResultDealer {
    private static final List<String> mediaType = Arrays.asList("audio", "video", "text", "image");
    private static String resourcePath;

    static {
        try {
            resourcePath = Objects.requireNonNull(ResultDealer.class.getResource("/")).toURI().getPath();
            if (resourcePath != null) {
                resourcePath = resourcePath.replaceAll("target/classes", "src/main/resources");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void clear() throws IOException {
        File resourceRootPath = new File(resourcePath + "output");
        if (resourceRootPath.exists() && resourceRootPath.isDirectory()) {
            FileUtils.cleanDirectory(resourceRootPath);
        }
    }

    public static void respDataPostProcess(String respData, List<String> responsePathList) throws IOException {
        clear();
        if (respData == null || respData.isEmpty()) {
            return;
        }
        JSONObject respDataJsonObject = JSON.parseObject(respData);
        if (responsePathList == null || responsePathList.size()==0) {
            return;
        }
        for (String responsePath: responsePathList) {
            Object responseData = JSONPath.eval(respDataJsonObject, responsePath);
            if (responseData == null) {
                continue;
            }
            JSONObject jsonObject = (JSONObject) responseData;
            String encoding = jsonObject.getString("encoding");
            if (encoding == null || encoding.isEmpty()) {
                continue;
            }
            String mediaValue = getResponseData(jsonObject);
            if (mediaValue == null || mediaValue.isEmpty()) {
                continue;
            }

            byte[] decode = Base64.getDecoder().decode(mediaValue);
            String fileName = responsePath.substring(responsePath.lastIndexOf(".") + 1);
            String filePath = resourcePath + "output" + File.separator + fileName + "." + encoding;
            FileUtils.writeByteArrayToFile(new File(filePath), decode, true);
        }
    }

    private static String getResponseData(JSONObject jsonObject) {
        Set<String> keySet = jsonObject.keySet();
        if (keySet.isEmpty()) {
            return null;
        }
        for (String key: keySet) {
            if (mediaType.contains(key)) {
                return jsonObject.getString(key);
            }
        }
        return null;
    }
}