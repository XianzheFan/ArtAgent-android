package com.fxz.artagent;

import android.content.Context;
import android.widget.Toast;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.IACRCloudListener;
import com.acrcloud.rec.utils.ACRCloudLogger;

public class MusicRecognition {
    private ACRCloudConfig mConfig;
    private ACRCloudClient mClient;
    private boolean initState = false;
    private boolean mProcessing = false;
    private Context mContext;

    public MusicRecognition(Context context) {
        mContext = context;
        this.mConfig = new ACRCloudConfig();

        this.mConfig.context = context;
        // Please create project in "http://console.acrcloud.cn/service/avr".
        this.mConfig.host = "identify-cn-north-1.acrcloud.cn";
        this.mConfig.accessKey = "2c36746314076087ac396fa950691f80";
        this.mConfig.accessSecret = "3ptFNQPgtn3LOCFiEyL2prDRPUwGqWmhfAqzvSWa";

        // auto recognize access key
        this.mConfig.hostAuto = "identify-cn-north-1.acrcloud.cn";
        this.mConfig.accessKeyAuto = "2c36746314076087ac396fa950691f80";
        this.mConfig.accessSecretAuto = "3ptFNQPgtn3LOCFiEyL2prDRPUwGqWmhfAqzvSWa";

        this.mConfig.recorderConfig.rate = 8000;
        this.mConfig.recorderConfig.channels = 1;
        this.mConfig.recorderConfig.isVolumeCallback = false;

        this.mClient = new ACRCloudClient();
        ACRCloudLogger.setLog(true);


    }

    public void setListener(IACRCloudListener listener) {
        this.mConfig.acrcloudListener = listener;
        this.initState = this.mClient.initWithConfig(this.mConfig);
    }

    public void reset() {
        mProcessing = false;
    }
    public void startRecognize() {
        if (!this.initState) {
            Toast.makeText(mContext, "music recognition init error", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mProcessing) {
            mProcessing = true;
            if (this.mClient == null || !this.mClient.startRecognize()) {
                mProcessing = false;
                Toast.makeText(mContext, "music recognition start error", Toast.LENGTH_SHORT).show();
            }
        }

    }

}