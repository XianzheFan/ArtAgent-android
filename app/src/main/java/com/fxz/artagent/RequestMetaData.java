package com.fxz.artagent;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestMetaData {
    private static final String hostUrl = "https://cn-east-1.api.xf-yun.com/v1/private/s29ebee0d";
    private static final Map<String, String> mediaTypePathMap = new HashMap<>();
    private static final List<String> responsePathList = new ArrayList<>();

    static {
        mediaTypePathMap.put("$.payload.data.audio", "input/guang16k_mono.mp3");
        responsePathList.add("$.payload.output_text");
    }

    public static Map<String, String> getRequestPathMap() {
        return mediaTypePathMap;
    }

    public static String getHostUrl() {
        return hostUrl;
    }

    public static List<String> getResponsePathList() {
        return responsePathList;
    }

    /**
     * 获取JsonObject形式的请求协议
     *
     */
    public static JSONObject getJsonObjectRequestData() {
        JSONObject jsonObjectRequestData = new JSONObject();
        JSONObject header = new JSONObject();
        jsonObjectRequestData.put("header", header);
        header.put("app_id", "123456");
        header.put("status", 3);
        JSONObject payload = new JSONObject();
        jsonObjectRequestData.put("payload", payload);
        JSONObject data = new JSONObject();
        payload.put("data", data);
        data.put("sample_rate", 16000);
        data.put("channels", 1);
        data.put("audio", "");
        data.put("encoding", "lame");
        data.put("bit_depth", 16);
        data.put("frame_size", 0);
        data.put("status", 3);
        JSONObject parameter = new JSONObject();
        jsonObjectRequestData.put("parameter", parameter);
        JSONObject acr_music = new JSONObject();
        parameter.put("acr_music", acr_music);
        acr_music.put("mode", "music");
        JSONObject output_text = new JSONObject();
        acr_music.put("output_text", output_text);
        output_text.put("format", "json");
        output_text.put("encoding", "utf8");
        output_text.put("compress", "raw");
        return jsonObjectRequestData;
    }

    /**
     * 获取Json字符串形式的请求协议
     *
     */
    public static String getJsonStringRequestData() {
        String jsonStringRequestData = "{\n" +
                "    \"header\": {\n" +
                "        \"app_id\": \"123456\",\n" +
                "        \"status\": 3\n" +
                "    },\n" +
                "    \"payload\": {\n" +
                "        \"data\": {\n" +
                "            \"sample_rate\": 16000,\n" +
                "            \"channels\": 1,\n" +
                "            \"audio\": \"\",\n" +
                "            \"encoding\": \"lame\",\n" +
                "            \"bit_depth\": 16,\n" +
                "            \"frame_size\": 0,\n" +
                "            \"status\": 3\n" +
                "        }\n" +
                "    },\n" +
                "    \"parameter\": {\n" +
                "        \"acr_music\": {\n" +
                "            \"mode\": \"music\",\n" +
                "            \"output_text\": {\n" +
                "                \"format\": \"json\",\n" +
                "                \"encoding\": \"utf8\",\n" +
                "                \"compress\": \"raw\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        return jsonStringRequestData;
    }
}