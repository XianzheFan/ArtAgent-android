package com.fxz.artagent;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;

public class PhotoUploader extends AsyncTask<Void, Void, String> {
    private Bitmap bitmap;
    private PhotoUploadCallback callback;
    private Context context;
    private String API_KEY = "jIXuOarsVibsV_amhwstceRjknZy5jPv";
    private String API_SECRET = "NJisrhSmgDBudgWy7sDJezlaa1r3B6c0";

    public interface PhotoUploadCallback {
        void onPhotoUploaded(String result);
    }

    public PhotoUploader(Bitmap bitmap, PhotoUploadCallback callback, Context context) {
        this.bitmap = bitmap;
        this.callback = callback;
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... voids) {
        File tempFile = new File(context.getCacheDir(), "tempImage" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Response response = FaceUtil.detectFace(tempFile, API_KEY, API_SECRET);
        String result = "";
        try {
            result = formatResponse(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (callback != null) {
            callback.onPhotoUploaded(result);
        }
    }

    private String formatResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray faces = jsonResponse.getJSONArray("faces");
            JSONObject attributes = faces.getJSONObject(0).getJSONObject("attributes");
//            String gender = attributes.getJSONObject("gender").getString("value");
//            String age = attributes.getJSONObject("age").getString("value");
            JSONObject emotionObject = attributes.getJSONObject("emotion");
            String emotion = getDominantEmotion(emotionObject);

            return emotion;
//            return "emotion : " + emotion + ", gender : " + gender + ", age: " + age;
        } catch (Exception e) {
            e.printStackTrace();
            return response;
        }
    }

    private String getDominantEmotion(JSONObject emotionObject) {
        String dominantEmotion = "";
        double maxScore = -1.0;
        Iterator<String> keys = emotionObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            double score = 0;
            try {
                score = emotionObject.getDouble(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (score > maxScore) {
                dominantEmotion = key;
                maxScore = score;
            }
        }
        return dominantEmotion;
    }
}