package com.fxz.artagent;

import android.media.AudioFormat;
import android.media.AudioRecord;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MP3Recorder {

    static {
        System.loadLibrary("mp3lame");
    }

    public static void convertPcmToMp3(byte[] pcmData, String mp3Path, int sampleRate) {
        final int BUFFER_SIZE = 8192;

        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        short[] buffer = new short[minBufferSize];

        File file = new File(mp3Path);
        if (file.exists()) {
            file.delete();
        }

        SimpleLame.init(sampleRate, 1, sampleRate, 32);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(pcmData);
        DataInputStream dis = new DataInputStream(bis);
        byte[] mp3buffer = new byte[BUFFER_SIZE];

        int bytesRead = 0;
        try {
            while ((bytesRead = dis.read(buffer, 0, minBufferSize)) != -1) {
                int bytesEncoded = SimpleLame.encode(buffer, buffer, bytesRead, mp3buffer);
                if (bytesEncoded != 0) {
                    os.write(mp3buffer, 0, bytesEncoded);
                }
            }
            int outputMp3buf = SimpleLame.flush(mp3buffer);
            if (outputMp3buf != 0) {
                os.write(mp3buffer, 0, outputMp3buf);
            }
            os.close();
            dis.close();
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}