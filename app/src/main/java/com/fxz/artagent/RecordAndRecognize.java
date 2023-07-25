package com.fxz.artagent;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;


public class RecordAndRecognize implements Runnable {
    private static final String appId = "449dd136";
    private static final String apiKey = "df6860efee87ef69614e0e19459ba472";
    private static final String apiSecret = "NzUxZGM3ZThjNDhjY2QyNGVhZmQ5ZDE3";

    private static final int SAMPLE_RATE_IN_HZ = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final String TAG = "RecordAndRecognize";
    private int bufferSize;
    private Context context;

    private AudioRecord audioRecord;
    private ByteArrayOutputStream byteArrayOutputStream;
    private Handler handler;
    private String hostUrl;
    private JSONObject requestData;
    private String mp3FilePath;

    public RecordAndRecognize(Context context, Handler handler, String hostUrl, String jsonStringRequestData) {
        this.context = context;
        this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        this.handler = handler;
        this.hostUrl = hostUrl;
        this.requestData = JSONObject.parseObject(jsonStringRequestData);
    }

    public void startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            audioRecord.startRecording();
            new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    int read = audioRecord.read(buffer, 0, bufferSize);
                    byteArrayOutputStream.write(buffer, 0, read);
                }
            }).start();
        } else {
            Log.e(TAG, "Record audio permission not granted");
        }
    }

    public void stopRecording() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        // Here you should convert the byteArrayOutputStream.toByteArray() to MP3 format and save to file.
        // byteArrayOutputStream.toByteArray() contains the raw PCM data.

        byte[] pcmData = byteArrayOutputStream.toByteArray();
        mp3FilePath = context.getFilesDir().getAbsolutePath() + "/recordedAudio.mp3";
        MP3Recorder.convertPcmToMp3(pcmData, mp3FilePath, SAMPLE_RATE_IN_HZ);
    }

    public String recognizeMusic() throws IOException, SignatureException {
        // Use the saved MP3 file to recognize music
        Map<String, String> requestPathMap = new HashMap<>();
        requestPathMap.put("$.payload.data.audio", mp3FilePath);
        AppClient appClient = new AppClient(appId, apiKey, apiSecret, hostUrl);
        String respData = appClient.doRequest(requestData, requestPathMap);
        return respData;
    }

    @Override
    public void run() {
        try {
            startRecording();
            Thread.sleep(10000); // record for 10 seconds
            stopRecording();
            String result = recognizeMusic();

            Message message = handler.obtainMessage();
            message.obj = result;
            handler.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}