package com.fxz.artagent;

import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Base64;
import android.content.Context;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MusicRecognitionService {
    private static final String WEBSING_URL = "https://webqbh.xfyun.cn/v1/service/v1/qbh";
    private static final String APPID = "449dd136";
    private static final String API_KEY = "686388524c9a0470fb5283d09804e88f";
    private static final String ENGINE_TYPE = "afs";
    private static final String AUE = "aac";

    private MediaRecorder recorder;
    private File audioFile;
    private RecognitionCallback callback;
    private Context context;

    public MusicRecognitionService(Context context, RecognitionCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public interface RecognitionCallback {
        void onRecognitionResult(String result);
    }

    public void startRecording() throws IOException {
        // 初始化录音器
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(8000);

        // 在缓存目录中创建音频文件
        audioFile = new File(context.getCacheDir(), "audio.aac");
        recorder.setOutputFile(audioFile.getAbsolutePath());

        // 开始录音
        recorder.prepare();
        recorder.start();
    }

    public void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;

            // 开始音乐识别任务
            RecognitionTask recognitionTask = new RecognitionTask();
            recognitionTask.execute(audioFile);
        }
    }

    private class RecognitionTask extends AsyncTask<File, Void, String> {
        @Override
        protected String doInBackground(File... files) {
            File audioFile = files[0];

            // 读取音频文件为字节数组
            byte[] audioBytes;
            try {
                audioBytes = FileUtil.read(audioFile.getAbsolutePath());
            } catch (IOException e) {
                return null;
            }

            // 创建HTTP请求头
            Map<String, String> header;
            try {
                header = buildHttpHeader();
            } catch (UnsupportedEncodingException e) {
                // handle error
                return null;
            }

            // 使用HTTP工具类发送POST请求
            String result = null;
            try {
                result = HttpUtil.doPost(WEBSING_URL, header, audioBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            callback.onRecognitionResult(result);
        }
    }

    private Map<String, String> buildHttpHeader() throws UnsupportedEncodingException {
        String curTime = System.currentTimeMillis() / 1000L + "";
        String param = "{\"aue\":\"" + AUE + "\",\"engine_type\":\"" + ENGINE_TYPE + "\"}";
        String paramBase64 = Base64.encodeToString(param.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        String checkSum = DigestUtils.md5Hex(API_KEY + curTime + paramBase64);
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        header.put("X-Param", paramBase64);
        header.put("X-CurTime", curTime);
        header.put("X-CheckSum", checkSum);
        header.put("X-Appid", APPID);
        return header;
    }
}