package com.fxz.artagent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    public static final String CHANNEL_ID = "FloatingWindowServiceChannel";
    Button faceButton;
    TextView faceView;
    private Button musicButton;
    private TextView musicView;
    Button mapButton;
    TextView mapView;
    private MusicRecognitionService musicRecognitionService;
    private boolean isMusicRecording = false;
    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    OkHttpClient client = buildHttpClient();
    private AMapLocationClient mLocationClient = null;

    private static final int SAMPLE_RATE = 16000;
    private static boolean startRecord = false;
    private static AudioRecord record = null;
    private static int miniBufferSize = 0;  // 1280 bytes 648 byte 40ms, 0.04s
    static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; //中英文，http url 不支持解析 ws/wss schema
    private static final String appid = "705c5357"; //在控制台-我的应用获取
//    private static final String appid = "6c157d10"; //在控制台-我的应用获取
    static final String apiSecret = "N2JlZTdlZWJhY2UyMDJiOTZkMDUxYzM3"; //在控制台-我的应用-语音听写（流式版）获取
//    static final String apiSecret = "MGY0YjM4NWMyZDYyYWRlMmI2MTlhZmZk"; //在控制台-我的应用-语音听写（流式版）获取
    static final String apiKey = "f6ba7d5007621a7c7570c2d39437e861"; //在控制台-我的应用-语音听写（流式版）获取
//    static final String apiKey = "8735f05eb184366efebb03483591ff41"; //在控制台-我的应用-语音听写（流式版）获取
    private static final String TAG = "MainActivity";
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();
    private static final int MAX_QUEUE_SIZE = 2500;  // 100 seconds audio, 1 / 0.04 * 100
    static WebIATWS.Decoder decoder = new WebIATWS.Decoder();
    private static final BlockingQueue<byte[]> bufferQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void requestPermissions() {
        // 权限列表
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
        };

        // 检查权限是否已经授权，如果未授权，则动态请求
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    String permission = permissions[i];
                    boolean showRationale = shouldShowRequestPermissionRationale(permission);
                    if (!showRationale) {
                        // 用户选择了“不再提醒”选项，你可以在这里提示用户手动开启权限
                        openAppSetting();
                    } else {
                        Toast.makeText(this, permission + " permission denied", Toast.LENGTH_SHORT).show();
                        Button record_button = findViewById(R.id.record_button);
                        record_button.setText("Start Record");
                        record_button.setEnabled(false);
                    }
                }
            }
        }
    }

    private void openAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        initRecorder();
    }

    private void promptForAccessibility() {
        new AlertDialog.Builder(this)
                .setTitle("无障碍服务")
                .setMessage("请设置“交互控制-无障碍快捷方式”为“ArtAgent”，并开启本软件“下载服务”权限")
                .setPositiveButton("好的", (dialog, which) -> {
                    // 用户点击了“好的”按钮，引导他们到无障碍设置页面
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("以后再说", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // 检查并请求SYSTEM_ALERT_WINDOW权限
    public void checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            promptForDrawOverlayPermission();
        } else {
            // 如果已经有了这个权限，启动浮动窗口服务
            Intent intent = new Intent(this, FloatingWindowService.class);
            startForegroundService(intent);
        }
    }

    // 引导用户开启SYSTEM_ALERT_WINDOW权限
    private void promptForDrawOverlayPermission() {
        new AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("本应用需要悬浮窗权限以提供某些功能。请点击“设置”以开启此权限。")
                .setPositiveButton("设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    mPermissionResult.launch(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private final ActivityResultLauncher<Intent> mPermissionResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Settings.canDrawOverlays(this)) {
                    // 用户已经授予了 SYSTEM_ALERT_WINDOW 权限，你可以在这里进行你的操作。
                    // 启动悬浮窗服务
                    Intent floatWindowIntent = new Intent(this, FloatingWindowService.class);
                    startForegroundService(floatWindowIntent);
                } else {
                    // 用户没有授予权限，你可以在这里继续引导用户，或者做一些适当的处理。
                    promptForDrawOverlayPermission();
                }
            }
    );

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    ActivityResultLauncher<Intent> mPermissionResult1 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (!Settings.System.canWrite(this)) {
                        Toast.makeText(this, "You denied the write settings permission.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void requestWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            mPermissionResult1.launch(intent);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        requestWriteSettingsPermission();

        final EditText editText = findViewById(R.id.edit_text);
        final TextView textView = findViewById(R.id.text_view);
        Button button = findViewById(R.id.button);
        faceButton = findViewById(R.id.face_button);
        faceView = findViewById(R.id.face_view);

        musicButton = findViewById(R.id.music_button);
        musicView = findViewById(R.id.music_view);
        musicRecognitionService = new MusicRecognitionService(MainActivity.this, result -> musicView.setText(result));
        musicButton.setOnClickListener(v -> {
            if (!isMusicRecording) {
                // 开始录音
                try {
                    musicRecognitionService.startRecording();
                    musicButton.setText("停止录音，开始识别");
                    isMusicRecording = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // 停止录音并识别音乐
                musicRecognitionService.stopRecording();
                musicButton.setText("听歌识曲");
                isMusicRecording = false;
            }
        });


        faceButton.setOnClickListener(view -> dispatchTakePictureIntent());  // 跳转至相机界面

        Button record_button = findViewById(R.id.record_button);
        record_button.setText("Start Record");
        record_button.setOnClickListener(view -> {
            if (!startRecord) {
                startRecord = true;
                startRecordThread();
                startAsrThread();
                record_button.setText("识别中，请稍等");
                record_button.setEnabled(false);
                Log.e(TAG, "onCreate: 3");
            }
        });


        mapButton = findViewById(R.id.map_button);
        mapView = findViewById(R.id.map_view);
        mapButton.setOnClickListener(view -> {
            AMapLocationClient.updatePrivacyShow(getApplicationContext(), true, true);
            AMapLocationClient.updatePrivacyAgree(getApplicationContext(), true);
            try {
                mLocationClient = new AMapLocationClient(getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setOnceLocation(true);
            mLocationOption.setOnceLocationLatest(true);
            mLocationOption.setNeedAddress(true);
            mLocationClient.setLocationOption(mLocationOption);

            if (mLocationClient != null) {
                mLocationClient.setLocationListener(amapLocation -> {
                    if (amapLocation != null) {
                        if (amapLocation.getErrorCode() == 0) {
                            String address = amapLocation.getAddress();  //获取详细地址信息
                            mapView.setText(address); // 在地图视图上设置获取到的地址
                        }
                    } else {
                        Log.e("AmapError", "location Error, ErrCode:" + amapLocation.getErrorCode() + ", errInfo:"
                                + amapLocation.getErrorInfo());
                    }
                });
                mLocationClient.startLocation();
            } else {
                Log.e("Amap null", "onCreate: Amap null");
            }
        });


        button.setOnClickListener(v -> {
            String input = editText.getText().toString();

            // 构建发送的数据
            JSONObject data = new JSONObject();
            try {
                data.put("input", input);
                data.put("chatbot", new JSONArray());
                data.put("history", new JSONArray());
                data.put("userID", 123456);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // 发送请求到服务器
            // 不要在主线程中执行网络请求，因为这可能导致应用的用户界面无响应。OkHttp库已经在新的线程中处理了这个问题
            post("http://166.111.139.118:22231/gpt4_predict", data.toString(), new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    // 将服务器的响应显示给用户
                    final String resStr = Objects.requireNonNull(response.body()).string();

                    runOnUiThread(() -> {
                        textView.setText(resStr); // 更新TextView的内容
                    });
                }
            });
        });
        promptForAccessibility();
        checkDrawOverlayPermission();
        createNotificationChannel();
    }


    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bundle extras = data.getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        new PhotoUploader(imageBitmap, result1 -> faceView.setText(result1), MainActivity.this).execute();
                    }
                }
            });

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            takePictureLauncher.launch(takePictureIntent);
        }
    }


    private void initRecorder() {  // 采样率16k或8K、位长16bit、单声道
        // buffer size in bytes 1280
        miniBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (miniBufferSize == AudioRecord.ERROR || miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Audio buffer can't initialize!");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, miniBufferSize);
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            return;
        }
        Log.i(TAG, "Record init okay");
    }


    private void startRecordThread() {
        new Thread(() -> {
            startRecord = true;
            initRecorder();
            record.startRecording();
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            while (startRecord) {
                byte[] buffer = new byte[miniBufferSize / 2];
                int read = record.read(buffer, 0, buffer.length);
                if (read == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Invalid operation error");
                    break;
                }
                try {
                    bufferQueue.put(buffer);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                    break;
                }
                // Check if recording should be stopped
                if (!startRecord) {
                    break;
                }
            }
            record.stop();
            record.release();
            record = null;
        }).start();
    }


    // 整个会话时长最多持续60s，或者超过10s未发送数据，服务端会主动断开连接
    void startAsrThread() {
        // 构建鉴权url
        String authUrl = null;
        try {
            authUrl = WebIATWS.getAuthUrl(hostUrl, apiKey, apiSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)  // 设置超时时间
                .build();
        String url = Objects.requireNonNull(authUrl).replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();

        // 录音关闭了，还在发送中间帧
        WebSocket webSocket = client.newWebSocket(request,  // 建立连接
                new WebSocketListener() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                        super.onOpen(webSocket, response);
                        new Thread(() -> {

                            //连接成功，开始发送数据
                            int frameSize = 1280; //每一帧音频的大小,建议每 40ms 发送 122B
                            int intervel = 40;
                            int status = StatusFirstFrame;  // 音频的状态

                            try {
                                byte[] buffer;  // 发送音频
                                end:
                                while (true) {
                                    buffer = null;
                                    try {
                                        buffer = bufferQueue.take();
                                    } catch (InterruptedException e) {
                                        status = StatusLastFrame;
                                        Thread.currentThread().interrupt();
                                        Log.e(TAG,"interrupt");
                                        break;  // 线程被阻塞时停止音频
                                    }
                                    int len = 0;
                                    if (buffer == null) {
                                        status = StatusLastFrame;

                                        startRecord = false;  // !!!
                                        if (record != null) {
                                            record.stop();
                                            record.release();
                                            record = null;
                                        }


                                        Log.e(TAG,"last");
                                    } else {
                                        len = buffer.length;
                                    }

                                    switch (status) {
                                        case StatusFirstFrame:   // 第一帧音频status = 0
                                            JsonObject frame = new JsonObject();
                                            JsonObject business = new JsonObject();  //第一帧必须发送
                                            JsonObject common = new JsonObject();  //第一帧必须发送
                                            JsonObject data = new JsonObject();  //每一帧都要发送
                                            // 填充common
                                            common.addProperty("app_id", appid);
                                            //填充business
                                            business.addProperty("language", "zh_cn");
                                            business.addProperty("domain", "iat");
                                            business.addProperty("accent", "mandarin");
                                            //business.addProperty("nunum", 0);
                                            //business.addProperty("ptt", 0);//标点符号
                                            //business.addProperty("vinfo", 1);
                                            business.addProperty("dwa", "wpgs");//动态修正(若未授权不生效，在控制台可免费开通)
                                            //填充data
                                            data.addProperty("status", StatusFirstFrame);
                                            data.addProperty("format", "audio/L16;rate=16000");
                                            data.addProperty("encoding", "raw");
                                            data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                                            //填充frame
                                            frame.add("common", common);
                                            frame.add("business", business);
                                            frame.add("data", data);
                                            webSocket.send(frame.toString());
                                            status = StatusContinueFrame;  // 发送完第一帧改变status 为 1
                                            break;
                                        case StatusContinueFrame:  //中间帧status = 1
                                            JsonObject frame1 = new JsonObject();
                                            JsonObject data1 = new JsonObject();
                                            data1.addProperty("status", StatusContinueFrame);
                                            data1.addProperty("format", "audio/L16;rate=16000");
                                            data1.addProperty("encoding", "raw");
                                            data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                                            frame1.add("data", data1);
                                            webSocket.send(frame1.toString());
                                            break;
                                        case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                                            JsonObject frame2 = new JsonObject();
                                            JsonObject data2 = new JsonObject();
                                            data2.addProperty("status", StatusLastFrame);
                                            data2.addProperty("audio", "");
                                            data2.addProperty("format", "audio/L16;rate=16000");
                                            data2.addProperty("encoding", "raw");
                                            frame2.add("data", data2);
                                            webSocket.send(frame2.toString());


                                            startRecord = false;  // !!!
                                            if (record != null) {
                                                record.stop();
                                                record.release();
                                                record = null;
                                            }


                                            break end;
                                    }
                                    Thread.sleep(intervel); //模拟音频采样延时
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }

                    @Override
                    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                        super.onMessage(webSocket, text);
                        WebIATWS.ResponseData resp = json.fromJson(text, WebIATWS.ResponseData.class);
                        if (resp != null) {
                            if (resp.getCode() != 0) {
                                Log.e(TAG, "code=>" + resp.getCode() + " error=>" + resp.getMessage() + " sid=" + resp.getSid());
                                Log.e(TAG, "错误码查询链接：https://www.xfyun.cn/document/error-code");
                                startRecord = false;
                                // 停止并释放录音
                                if (record != null) {
                                    record.stop();
                                    record.release();
                                    record = null;
                                }
                                // 在UI线程上启用按钮
                                runOnUiThread(() -> {
                                    Button record_button = findViewById(R.id.record_button);
                                    record_button.setText("Start Record");
                                    record_button.setEnabled(true); // 启用按钮，以供下一次录音使用
                                    Log.e(TAG, "onCreate: 5");
                                });
                                return;
                            }
                            if (resp.getData() != null) {
                                if (resp.getData().getResult() != null) {
                                    WebIATWS.Text te = resp.getData().getResult().getText();
                                    try {
                                        decoder.decode(te);
                                        runOnUiThread(() -> {
                                            TextView record_view = findViewById(R.id.record_view);
                                            record_view.setText(decoder.toString());
                                            Log.e(TAG, resp.getMessage() + " " + resp.getData().getResult().getText());
                                            Button record_button = findViewById(R.id.record_button);
                                            record_button.setText("识别中，请稍等");
                                            record_button.setEnabled(false);
                                            Log.e(TAG, "onCreate: 6");
                                        });
                                    } catch (Exception e) {
                                        runOnUiThread(() -> {
                                            Button record_button = findViewById(R.id.record_button);
                                            record_button.setText("Start Record");
                                            record_button.setEnabled(true); // 启用按钮，以供下一次录音使用
                                            Log.e(TAG, "onCreate: 7");
                                        });
                                        e.printStackTrace();
                                    }
                                }
                                if (resp.getData().getStatus() == 2) {
                                    startRecord = false;  // 停止录音
                                    // 停止并释放录音
                                    if (record != null) {
                                        record.stop();
                                        record.release();
                                        record = null;
                                    }
                                    Log.e(TAG, "语音识别结果：" + decoder.toString());
                                    runOnUiThread(() -> {
                                        Button record_button = findViewById(R.id.record_button);
                                        record_button.setText("Start Record");
                                        record_button.setEnabled(true); // 启用按钮，以供下一次录音使用
                                        Log.e(TAG, "onCreate: 8");
                                    });

                                    decoder.discard();
                                    webSocket.close(1000, "");
                                    // 在UI线程上启用按钮
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                        super.onFailure(webSocket, t, response);
                        startRecord = false;
                        // 停止并释放录音
                        if (record != null) {
                            record.stop();
                            record.release();
                            record = null;
                        }
                        try {
                            if (null != response) {
                                int code = response.code();
                                Log.e(TAG, "onFailure code: " + code);
                                Log.e(TAG, "onFailure body:" + Objects.requireNonNull(response.body()).string());
                                if (101 != code) {
                                    Log.e(TAG, "connection failed");
                                    System.exit(0);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // 在UI线程上启用按钮
                        runOnUiThread(() -> {
                            Button record_button = findViewById(R.id.record_button);
                            record_button.setText("Start Record");
                            record_button.setEnabled(true); // 启用按钮，以供下一次录音使用
                        });
                    }
                });
    }


    // 发送POST请求的方法
    void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }
}