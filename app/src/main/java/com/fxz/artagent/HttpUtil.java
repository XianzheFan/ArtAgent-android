package com.fxz.artagent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpUtil {

    public static String doPost(String url, Map<String, String> headers, byte[] body) throws IOException {
        URL realUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
        for (String key : headers.keySet()) {
            connection.setRequestProperty(key, headers.get(key));
        }
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);

        connection.getOutputStream().write(body);
        connection.getOutputStream().flush();
        connection.getOutputStream().close();

        StringBuilder result = new StringBuilder();
        int responseCode = connection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == responseCode) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }
}