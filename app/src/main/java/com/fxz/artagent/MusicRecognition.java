package com.fxz.artagent;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import androidx.annotation.RequiresPermission;
import androidx.annotation.WorkerThread;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.FutureTask;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MusicRecognition {
    private OkHttpClient client;
    private ByteBuffer destination;
    private AudioRecord audioRecord;

    public MusicRecognition() {
        this.client = new OkHttpClient();
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @WorkerThread
    public byte[] recordAudio(int seconds) {
        int audioSource = MediaRecorder.AudioSource.UNPROCESSED;

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(48000)
                .build();

        audioRecord = new AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(audioFormat)
                .build();

        // Allocate buffer size
        int size = audioFormat.getSampleRate() * 2 * seconds;
        destination = ByteBuffer.allocate(size);

        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        audioRecord.startRecording();
        return destination.array();
    }

    public void stopRecording() {
        if(audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }

    public String fetchMusicInfo(byte[] audioData) throws Exception {
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            MediaType textPlain = MediaType.parse("text/plain; charset=utf-8");
            RequestBody requestBody = RequestBody.create(base64Audio, textPlain);

            Request request = new Request.Builder()
                    .url("https://shazam.p.rapidapi.com/shazam-events/list?artistId=73406786&l=en-US&limit=50&offset=0")
                    .post(requestBody)
                    .addHeader("X-RapidAPI-Key", "4cd7730b18msh29e02859895e882p17faf2jsnf2c416434990")
                    .addHeader("X-RapidAPI-Host", "shazam.p.rapidapi.com")
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        });

        new Thread(futureTask).start();
        return futureTask.get();
    }
}