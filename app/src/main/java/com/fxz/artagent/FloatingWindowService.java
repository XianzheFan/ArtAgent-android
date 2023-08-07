package com.fxz.artagent;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


public class FloatingWindowService extends Service implements TextAccessibilityService.TextCallback {
    private WindowManager windowManager;
    private View layout;  // 整个工具栏
    public static final String CHANNEL_ID = "FloatingWindowServiceChannel";
    public static final String PREFERENCES_NAME = "SavedTexts";
    public static final String PREFERENCES_KEY = "texts";
    EditText etInput;
    ImageView ivSend, ivMirco, ivDotView;
    TextView return_chat, tv1, tv2, tv3, tv4, tv5, tv6, tv7, tvTitle;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isOtherLayoutVisible = true;
    private LinearLayout llCon1, llTool, ll0;
    FrameLayout fl1, flFrame;
    View.OnTouchListener onTouchListener;
    Handler handler = new Handler();

    private OkHttpClient buildHttpClient() {  // 天气client
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(600, TimeUnit.SECONDS)
                .build();
    }

    OkHttpClient weatherClient = buildHttpClient();
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

    List<MessageBean> messageBeanList = new ArrayList<>();  // 不需要输出图片路径，在 content 里面就是路径，只是显示成图片而已
    ChatAdapter chatAdapter;
    RecyclerView recyclerView;

    private BroadcastReceiver imageSelectedReceiver;  // 从相册里选取图片发送

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        imageSelectedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String selectedImageUri = intent.getStringExtra("selectedImageUri");
                if (selectedImageUri != null) {
                    Uri imageUri = Uri.parse(selectedImageUri);
                    handler.post(() -> {
                        messageBeanList.add(new MessageBean("user", imageUri.toString(), true));
                        int positionInserted = chatAdapter.getItemCount() - 1;
                        chatAdapter.notifyItemInserted(positionInserted);
                        recyclerView.scrollToPosition(positionInserted);
                    });
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(imageSelectedReceiver, new IntentFilter("com.fxz.artagent.ACTION_IMAGE_SELECTED"));

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Notification notification = new Notification.Builder(this, CHANNEL_ID).setContentTitle("ArtAgent").setContentText("ArtAgent is running.").setSmallIcon(R.drawable.newpen).build();

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.talkway, null);
        flFrame = layout.findViewById(R.id.fl_frame);
        ivDotView = layout.findViewById(R.id.iv_dot_view);
        etInput = layout.findViewById(R.id.et_input);
        ivSend = layout.findViewById(R.id.btn_send);
        ivMirco = layout.findViewById(R.id.btn_mirco);
        llCon1 = layout.findViewById(R.id.ll_con_1);
        llTool = layout.findViewById(R.id.ll_tool);
        return_chat = layout.findViewById(R.id.return_chat);
        tv1 = layout.findViewById(R.id.tv_1);
        tv2 = layout.findViewById(R.id.tv_2);
        tv3 = layout.findViewById(R.id.tv_3);
        tv4 = layout.findViewById(R.id.tv_4);
        tv5 = layout.findViewById(R.id.tv_5);
        tv6 = layout.findViewById(R.id.tv_6);
        tv7 = layout.findViewById(R.id.tv_7);
        ll0 = layout.findViewById(R.id.ll_0);
        fl1 = layout.findViewById(R.id.fl_1);
        tvTitle = layout.findViewById(R.id.tv_title);

        return_chat.setOnClickListener(view -> {
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Chat");
            fl1.removeAllViews();
            fl1.addView(getViewChat(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        });

        tv1.setOnClickListener(view -> {
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Location");
            fl1.removeAllViews();
            View view1 = getView1();
            fl1.addView(view1, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            btnLocationClick(view1);
        });
        tv2.setOnClickListener(view -> {
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Emotion");
            fl1.removeAllViews();
            fl1.addView(getView4(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        });
        tv3.setOnClickListener(view -> {
            getLatestTexts();
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Content");
            fl1.removeAllViews();
            fl1.addView(getView3(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        });
        tv4.setOnClickListener(view -> {
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Camera");
            fl1.removeAllViews();
            fl1.addView(getView6(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        });
        tv5.setOnClickListener(view -> {  // 找到系统相册
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Chat");
            fl1.removeAllViews();
            fl1.addView(getViewChat(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

            Intent intent = new Intent(this, ImagePickerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        tv6.setOnClickListener(view -> {
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Music");
            fl1.removeAllViews();
            fl1.addView(getView2(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        });
        tv7.setOnClickListener(view -> {
            ll0.setVisibility(View.GONE);
            fl1.setVisibility(View.VISIBLE);
            tvTitle.setText("Action");
            fl1.removeAllViews();
            fl1.addView(getView2(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        });

        ivMirco.setOnClickListener(view -> btnRecordClick());

        ivSend.setOnClickListener(view -> {
            if (fl1.getVisibility() == View.GONE) {
                ll0.setVisibility(View.GONE);
                fl1.setVisibility(View.VISIBLE);
                tvTitle.setText("Chat");
                fl1.removeAllViews();
                fl1.addView(getViewChat(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            }
            String userInput = etInput.getText().toString();

            handler.post(() -> {
                messageBeanList.add(new MessageBean("user", userInput));
                int positionInserted = chatAdapter.getItemCount() - 1;
                chatAdapter.notifyItemInserted(positionInserted);
                recyclerView.scrollToPosition(positionInserted);
            });

            if (messageBeanList.size() == 0) {
                AMapLocationClient.updatePrivacyShow(getApplicationContext(), true, true);
                AMapLocationClient.updatePrivacyAgree(getApplicationContext(), true);
                try {
                    mLocationClient = new AMapLocationClient(getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
                mLocationOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.EN);  // 设置为英文
                mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                mLocationOption.setOnceLocation(true);
                mLocationOption.setOnceLocationLatest(true);
                mLocationOption.setNeedAddress(true);
                mLocationClient.setLocationOption(mLocationOption);
                mLocationClient.setLocationListener(amapLocation -> {
                    if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                        double latitude = Math.round(amapLocation.getLatitude() * 100.0) / 100.0;  // 保留两位小数
                        double longitude = Math.round(amapLocation.getLongitude() * 100.0) / 100.0;  // 保留两位小数
                        String address = amapLocation.getAddress();

                        fetchWeatherData(latitude, longitude, new WeatherTextCallback() {
                            @Override
                            public void onWeatherTextReceived(String weatherText) {
                                getLatestTexts();
                                // 从SharedPreferences中获取文本
                                SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
                                String screenText = prefs.getString(PREFERENCES_KEY, "");

                                // 所有数据都已经获取到，调用 requestTopic
                                requestTopic(userInput, weatherText, address, screenText);
                            }
                            @Override
                            public void onError(Exception e) {
                                Log.e("WeatherError", "Failed to get weather data", e);
                            }
                        });
                    } else {
                        Log.e("AmapError", "location Error, ErrCode:" + amapLocation.getErrorCode() + ", errInfo:" + amapLocation.getErrorInfo());
                    }
                });
                mLocationClient.startLocation();
            } else {
                requestArgue();
            }
            etInput.setText("");  // 请求完再清空etInput
            Log.e(TAG, String.valueOf(messageBeanList.size()));
        });

        chatAdapter = new ChatAdapter(messageBeanList);

        etInput.setOnClickListener(view -> {
            etInput.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT);
        });

        startForeground(1, notification);

        final WindowManager.LayoutParams params;

        params = new WindowManager.LayoutParams(  // 设置聊天框大小
                dpToPx(344), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        params.x = 0;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        params.y = screenHeight / 8;

        onTouchListener = (view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 记录初始位置和触摸点的坐标
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = motionEvent.getRawX();
                    initialTouchY = motionEvent.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    // 计算偏移量
                    int xOffset = (int) (motionEvent.getRawX() - initialTouchX);
                    int yOffset = (int) (motionEvent.getRawY() - initialTouchY);
                    // 更新布局位置
                    params.x = initialX + xOffset;
                    params.y = initialY + yOffset;
                    // 更新视图
                    windowManager.updateViewLayout(layout, params);
                    return true;
            }
            return false;
        };

        ivDotView.setOnTouchListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick(View v) {
                super.onDoubleClick(v);
                toggleOtherLayout();
            }

            @Override
            public void onSingleClick(View v) {
                super.onSingleClick(v);
                if (!isOtherLayoutVisible) {
                    return;
                }
                if (llTool.getVisibility() == View.VISIBLE) {
                    animateCollapseV(llTool);  // 关上工具栏
                } else {
                    if (fl1.getVisibility() == View.VISIBLE) {
                        fl1.setVisibility(View.GONE);
                        ll0.setVisibility(View.VISIBLE);
                    }
                    animateExpandV(llTool);  // 展开工具栏
                }
            }
        });

        flFrame.setOnTouchListener(onTouchListener);

        // 将水平线性布局添加到悬浮窗口中
        windowManager.addView(layout, params);
        TextAccessibilityService.setTextCallback(this);
    }

    private void requestImage() {
        JSONObject data = new JSONObject();
        try {
            data.put("userID", 123456);
            data.put("cnt", 0);
            data.put("width", 200);
            data.put("height", 200);

            JSONArray history = new JSONArray();
            for (MessageBean messageBean : messageBeanList) {
                JSONObject historyItem = new JSONObject();
                historyItem.put("role", messageBean.getRole());
                historyItem.put("content", messageBean.getMessage());
                Log.e(TAG, messageBean.getMessage());
                history.put(historyItem);
            }
            data.put("history", history);
            Log.e(TAG, String.valueOf(data));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("http://166.111.139.116:22231/gpt4_sd_draw", data.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: gpt4_sd_draw", e);
                e.printStackTrace();
                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant","gpt4_sd_draw error"));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                // 解析服务器的响应
                final String resStr = Objects.requireNonNull(response.body()).string();

                // 使用 Gson 解析 JSON 数据
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> resMap = gson.fromJson(resStr, type);


                String imageUrl = (String) resMap.get("image_url");
                Log.e(TAG, imageUrl);
                // 使用Glide或者其他库从imageUrl加载图像
                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant", imageUrl, true));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });


                List<Map<String, Object>> history = (List<Map<String, Object>>) resMap.get("history");
                if (history != null && !history.isEmpty()) {
                    Map<String, Object> lastAssistantMessage = null;

                    // 逆向遍历历史记录，寻找最后一条"assistant"的消息
                    for (int i = history.size() - 1; i >= 0; i--) {
                        Map<String, Object> record = history.get(i);
                        if ("assistant".equals(record.get("role"))) {
                            lastAssistantMessage = record;
                            break;
                        }
                    }

                    // 如果找到了
                    if (lastAssistantMessage != null) {
                        String lastAssistantContent = (String) lastAssistantMessage.get("content");
                        handler.post(() -> {
                            messageBeanList.add(new MessageBean("assistant", lastAssistantContent));
                            int positionInserted = chatAdapter.getItemCount() - 1;
                            chatAdapter.notifyItemInserted(positionInserted);
                            recyclerView.scrollToPosition(positionInserted);
                        });
                    }
                }
            }
        });
    }

    private void requestTopic(String input, String weatherText, String mapText, String writeText) {  // 第一次请求：返回推荐的主题
//        String faceText = faceView.getText().toString();
//        String musicText = musicView.getText().toString();
        String faceText = "";
        String musicText = "";

        //合并所有视图中的文本
        String combinedText = "Location:["+mapText+"],Phone-Content:["+writeText+"],Facial Expression:["+faceText+"],Weather:["+weatherText+"],Music:["+musicText+"],User command:["+input+"]";
        Log.e(TAG, "predict: " + combinedText);

        // 构建发送的数据
        JSONObject data = new JSONObject();
        try {
            data.put("input", combinedText);
            data.put("userID", 123456);

            JSONArray history = new JSONArray();
            data.put("history", history);

            Log.e(TAG, "onCreate: data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 发送请求到服务器
        // 不要在主线程中执行网络请求，因为这可能导致应用的用户界面无响应。OkHttp库已经在新的线程中处理了这个问题
        post("http://166.111.139.116:22231/gpt4_mode_2", data.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: gpt4_mode_2");
                e.printStackTrace();
                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant", "gpt4_mode_2 onFailure Error"));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    messageBeanList.add(new MessageBean("assistant", "gpt4_mode_2 onResponse Error"));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                    throw new IOException("Unexpected code " + response);
                }

                // 将服务器的响应显示给用户
                final String resStr = Objects.requireNonNull(response.body()).string();

                // 使用 Gson 解析 JSON 数据
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> resMap = gson.fromJson(resStr, type);

                List<Map<String, String>> history = (List<Map<String, String>>) resMap.get("history");
                String assistantContent = "";
                for (Map<String, String> item : history) {
                    if (item.get("role").equals("assistant")) {
                        assistantContent = item.get("content");
                    }
                }

                final String displayContent = assistantContent;
                Log.e(TAG, "onResponse: " + displayContent);

                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant", displayContent)); // bot回复在左侧
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });
            }
        });
    }

    private void requestArgue() {  // 非第一次请求：返回绘画建议
        String combinedText = etInput.getText().toString();
        Log.e(TAG, "predict: " + combinedText);

        // 构建发送的数据
        JSONObject data = new JSONObject();
        try {
            data.put("input", combinedText);
            data.put("userID", 123456);

            JSONArray history = new JSONArray();
            for (MessageBean messageBean : messageBeanList) {
                JSONObject historyItem = new JSONObject();
                historyItem.put("role", messageBean.getRole());
                historyItem.put("content", messageBean.getMessage());
                history.put(historyItem);
            }
            data.put("history", history);

            Log.e(TAG, String.valueOf(history));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 发送请求到服务器
        // 不要在主线程中执行网络请求，因为这可能导致应用的用户界面无响应。OkHttp库已经在新的线程中处理了这个问题
        post("http://166.111.139.116:22231/gpt4_predict", data.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: gpt4_predict");
                e.printStackTrace();
                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant", "gpt4_predict Error"));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    messageBeanList.add(new MessageBean("assistant", "gpt4_mode_2 Error"));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                    throw new IOException("Unexpected code " + response);
                }

                // 将服务器的响应显示给用户
                final String resStr = Objects.requireNonNull(response.body()).string();

                // 使用 Gson 解析 JSON 数据
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> resMap = gson.fromJson(resStr, type);

                List<Map<String, String>> history = (List<Map<String, String>>) resMap.get("history");
                String assistantContent = "";
                for (Map<String, String> item : history) {
                    if (item.get("role").equals("assistant")) {
                        assistantContent = item.get("content");
                    }
                }

                final String displayContent = assistantContent;
                Log.e(TAG, "onResponse: " + displayContent);

                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant", displayContent)); // 更新TextView的内容
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });
            }
        });
    }

    private void imageEditTopic(String input, String weatherText, String mapText, File imageFile) {  // 暂时不考虑user command只给评价和推荐
        // 上传图片给建议，和屏幕文本无关，但为了不重写prompt，直接令其为空
//        String faceText = faceView.getText().toString();
//        String musicText = musicView.getText().toString();
        String faceText = "";
        String musicText = "";
        //合并所有视图中的文本
        String combinedText = "Location:["+mapText+"],Phone-Content:[],Facial Expression:["+faceText+"],Weather:["+weatherText+"],Music:["+musicText+"],User command:["+input+"]";
        Log.e(TAG, "predict: " + combinedText);

        // 构建发送的数据
        JSONObject data = new JSONObject();
        try {
            data.put("input", combinedText);
            JSONArray history = new JSONArray();
            data.put("history", history);
            data.put("userID", 123456);
            Log.e(TAG, "onCreate: data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/jpg"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(filePart)
                .addFormDataPart("data", data.toString());
        MultipartBody multipartBody = multipartBodyBuilder.build();
        Log.e(TAG, String.valueOf(multipartBody));
        Request request = new Request.Builder()
                .url("http://166.111.139.116:22231/image_edit_topic")
                .post(multipartBody)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // 连接超时时间
                .writeTimeout(600, TimeUnit.SECONDS) // 写操作超时时间
                .readTimeout(600, TimeUnit.SECONDS) // 读操作超时时间
                .callTimeout(1200, TimeUnit.SECONDS) // 增加全局调用超时
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            // 不要在主线程中执行网络请求，因为这可能导致应用的用户界面无响应。OkHttp库已经在新的线程中处理了这个问题
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: image_edit_topic");
                e.printStackTrace();
                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant", "image_edit_topic onFailure Error"));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    handler.post(() -> {
                        messageBeanList.add(new MessageBean("assistant", "image_edit_topic onResponse Error"));
                        int positionInserted = chatAdapter.getItemCount() - 1;
                        chatAdapter.notifyItemInserted(positionInserted);
                        recyclerView.scrollToPosition(positionInserted);
                    });
                    throw new IOException("Unexpected code " + response);
                }

                // 将服务器的响应显示给用户
                final String resStr = Objects.requireNonNull(response.body()).string();

                // 使用 Gson 解析 JSON 数据
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> resMap = gson.fromJson(resStr, type);

                List<Map<String, String>> history = (List<Map<String, String>>) resMap.get("history");
                String assistantContent = "";
                for (Map<String, String> item : history) {
                    if (item.get("role").equals("assistant")) {
                        assistantContent = item.get("content");
                    }
                }

                final String displayContent = assistantContent;
                Log.e(TAG, "onResponse: " + displayContent);

                handler.post(() -> {
                    messageBeanList.add(new MessageBean("assistant", displayContent)); // 对图片的评价和修改建议
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);
                });
            }
        });
    }

    private View getViewChat() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_chat, null);
        recyclerView = view.findViewById(R.id.rv_list);
        TextView ivPaint = view.findViewById(R.id.btn_paint);
        TextView tvBackTool = view.findViewById(R.id.btn_back_tool);
        TextView tvClear = view.findViewById(R.id.btn_clear);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
        ivPaint.setOnClickListener(v -> {
            handler.post(() -> {
                messageBeanList.add(new MessageBean("user", "Please generate an image based on our previous art discussion."));
                int positionInserted = chatAdapter.getItemCount() - 1;
                chatAdapter.notifyItemInserted(positionInserted);
                recyclerView.scrollToPosition(positionInserted);
            });
            requestImage();
        });
        tvBackTool.setOnClickListener(v -> {
            llTool.setVisibility(View.GONE);
            fl1.setVisibility(View.GONE);
            ll0.setVisibility(View.VISIBLE);
            llTool.setVisibility(View.VISIBLE);
            tvTitle.setText("Tools");
        });
        tvClear.setOnClickListener(v -> {
            handler.post(() -> {
                int itemCount = messageBeanList.size();
                messageBeanList.clear();
                chatAdapter.notifyItemRangeRemoved(0, itemCount);
            });
        });
        return view;
    }

    private void btnRecordClick() {
        if (!startRecord) {
            startRecord = true;
            startRecordThread();
            startAsrThread();
            showDialog();
            ivMirco.setEnabled(false);
            Log.e(TAG, "onCreate: 3");
        }
    }

    AlertDialog dialog;

    private void showDialog() {
//        if (dialog != null && dialog.isShowing()) {
//            return;
//        }
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
//                builder.setTitle("提示");
//                builder.setMessage("识别中，请稍等");
//                dialog = builder.create();
//                builder.show();
//            }
//        });

    }

    private void dismissDialog() {
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                if(dialog != null){
//                    dialog.dismiss();
//                }
//            }
//        });
    }


    // 发送POST请求的方法
    void post(String url, String json, Callback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // 连接超时时间
                .writeTimeout(600, TimeUnit.SECONDS) // 写操作超时时间
                .readTimeout(600, TimeUnit.SECONDS) // 读操作超时时间
                .callTimeout(1200, TimeUnit.SECONDS) // 增加全局调用超时
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
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
                .readTimeout(20, TimeUnit.SECONDS)
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
                                        Log.e(TAG, "interrupt");
                                        break;  // 线程被阻塞时停止音频
                                    }
                                    int len = 0;
                                    if (buffer == null) {
                                        status = StatusLastFrame;
                                        startRecord = false;
                                        if (record != null) {
                                            record.stop();
                                            record.release();
                                            record = null;
                                        }
                                        Log.e(TAG, "last");
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
                                            startRecord = false;
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
                                handler.post(() -> {
                                    ivMirco.setEnabled(true); // 启用按钮，以供下一次录音使用
                                    Log.e(TAG, "onCreate: 5");
                                });
                                return;
                            }
                            if (resp.getData() != null) {
                                if (resp.getData().getResult() != null) {
                                    WebIATWS.Text te = resp.getData().getResult().getText();
                                    try {
                                        decoder.decode(te);
                                        handler.post(() -> {
                                            etInput.setText(decoder.toString());
                                            Log.e(TAG, resp.getMessage() + " " + resp.getData().getResult().getText());
                                            showDialog();
                                            ivMirco.setEnabled(false);
                                            Log.e(TAG, "onCreate: 6");
                                        });
                                    } catch (Exception e) {
                                        handler.post(() -> {
                                            dismissDialog();
                                            ivMirco.setEnabled(true); // 启用按钮，以供下一次录音使用
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
                                    handler.post(() -> {
                                        dismissDialog();
                                        ivMirco.setEnabled(true); // 启用按钮，以供下一次录音使用
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
                        handler.post(() -> {
                            dismissDialog();
                            ivMirco.setEnabled(true); // 启用按钮，以供下一次录音使用
                        });
                    }
                });
    }


    private void btnLocationClick(View view) {
        AMapLocationClient.updatePrivacyShow(getApplicationContext(), true, true);
        AMapLocationClient.updatePrivacyAgree(getApplicationContext(), true);
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        mLocationOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.EN);  // 设置为英文
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
                        ((EditText) view.findViewById(R.id.et_location)).setText(address);
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
    }

    private View getView1() {
        View view = LayoutInflater.from(this).inflate(R.layout.view_location, null);
        return view;
    }

    TextView tvText;

    private View getView2() {  // 简单的文本框
        View view = LayoutInflater.from(this).inflate(R.layout.view_emotion2, null);
        tvText = view.findViewById(R.id.tv_text);
        return view;
    }

    private View getView3() {
        View view = LayoutInflater.from(this).inflate(R.layout.view_content, null);
        loadTextsFromPreferences(view);
        return view;
    }

    private void loadTextsFromPreferences(View view) {
        SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String savedTexts = prefs.getString(PREFERENCES_KEY, "");

        EditText writeGet3View = view.findViewById(R.id.write_get3_view);
        writeGet3View.setText(savedTexts);
    }


    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private TextureView textureView;
    TextView tvTake;
    RelativeLayout rlCamara;
    private boolean usingFrontCamera = true;

    private void switchCamera(boolean isEmotion) {
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        usingFrontCamera = !usingFrontCamera;
        // Open the new camera
        String newCameraId = null;
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == (usingFrontCamera ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK)) {
                    newCameraId = cameraId;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (newCameraId == null) {
            Log.e("CameraActivity", "Desired camera not available.");
        } else {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(newCameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            cameraDevice = camera;
                            startCameraPreview(isEmotion);
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            cameraDevice.close();
                            cameraDevice = null;
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            cameraDevice.close();
                            cameraDevice = null;
                        }
                    }, null);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private View getView4() { // emotion
        View view = LayoutInflater.from(this).inflate(R.layout.view_camera, null);
        tvTake = view.findViewById(R.id.tv_emotion);
        rlCamara = view.findViewById(R.id.rl_camera);
        view.findViewById(R.id.iv_switch_camera).setOnClickListener(v -> switchCamera(true));
        view.findViewById(R.id.iv_take).setOnClickListener(v -> captureImage());
        textureView = view.findViewById(R.id.textureView);

        setupCamera(true);
        return view;
    }

    private View getView6() { // camera
        View view = LayoutInflater.from(this).inflate(R.layout.view_camera, null);
        tvTake = view.findViewById(R.id.tv_emotion);
        rlCamara = view.findViewById(R.id.rl_camera);
        view.findViewById(R.id.iv_switch_camera).setOnClickListener(v -> switchCamera(false));
        view.findViewById(R.id.iv_take).setOnClickListener(v -> captureImage());
        textureView = view.findViewById(R.id.textureView);

        setupCamera(false);
        return view;
    }

    private void setupCamera(boolean isEmotion) {
        try {
            // Get the front camera ID
            if (frontCameraId == null) {
                try {
                    for (String cameraId : cameraManager.getCameraIdList()) {
                        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            frontCameraId = cameraId;
                            break;
                        }
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            // Check if the front camera is available
            if (frontCameraId == null) {
                Log.e("CameraActivity", "Front camera not available.");
                frontCameraId = cameraManager.getCameraIdList()[0];
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(frontCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        startCameraPreview(isEmotion);
                    }
                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }
                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureImage() {
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureRequestBuilder.addTarget(imageReader.getSurface());
            int rotation = windowManager.getDefaultDisplay().getRotation();
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(c, rotation));
            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(imageReader.getSurface());

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void saveImage(Image image, boolean isEmotion) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            buffer.rewind();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            rlCamara.setVisibility(View.GONE);

            if (isEmotion) {
                tvTake.setVisibility(View.VISIBLE);
                tvTake.setText("Processing...");
                new PhotoUploader(compress(bitmap, 80), result1 -> tvTake.setText(result1), getApplicationContext()).execute();
            } else {
                File imageFile = saveImageToFile(bytes);
                if (imageFile != null) {
                    ll0.setVisibility(View.GONE);
                    fl1.setVisibility(View.VISIBLE);
                    tvTitle.setText("Chat");
                    fl1.removeAllViews();
                    fl1.addView(getViewChat(), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
                    messageBeanList.add(new MessageBean("user", imageFile.getAbsolutePath(), true));
                    int positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    messageBeanList.add(new MessageBean("user", "Please provide suggestions for this image."));
                    positionInserted = chatAdapter.getItemCount() - 1;
                    chatAdapter.notifyItemInserted(positionInserted);
                    recyclerView.scrollToPosition(positionInserted);

                    AMapLocationClient.updatePrivacyShow(getApplicationContext(), true, true);
                    AMapLocationClient.updatePrivacyAgree(getApplicationContext(), true);
                    try {
                        mLocationClient = new AMapLocationClient(getApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
                    mLocationOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.EN);
                    mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                    mLocationOption.setOnceLocation(true);
                    mLocationOption.setOnceLocationLatest(true);
                    mLocationOption.setNeedAddress(true);
                    mLocationClient.setLocationOption(mLocationOption);
                    mLocationClient.setLocationListener(amapLocation -> {
                        if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                            double latitude = Math.round(amapLocation.getLatitude() * 100.0) / 100.0;
                            double longitude = Math.round(amapLocation.getLongitude() * 100.0) / 100.0;
                            String address = amapLocation.getAddress();
                            fetchWeatherData(latitude, longitude, new WeatherTextCallback() {
                                @Override
                                public void onWeatherTextReceived(String weatherText) {
                                    imageEditTopic("", weatherText, address, imageFile);
                                }
                                @Override
                                public void onError(Exception e) {
                                    Log.e("WeatherError", "Failed to get weather data", e);
                                }
                            });
                        } else {
                            Log.e("AmapError", "location Error, ErrCode:" + amapLocation.getErrorCode() + ", errInfo:" + amapLocation.getErrorInfo());
                        }
                    });
                    mLocationClient.startLocation();
                    saveImageToGallerySWN(getApplicationContext(), bitmap);
                }
            }
        } finally {
            image.close();
        }
    }

    private File saveImageToFile(byte[] bytes) {
        File imageDirectory = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (imageDirectory != null) {
            String fileName = "tempImage" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(imageDirectory, fileName);

            try (OutputStream outputStream = new FileOutputStream(imageFile)) {
                outputStream.write(bytes);
                return imageFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static Bitmap compress(Bitmap bitmap, int quality){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] bytes = baos.toByteArray();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    private boolean saveImageToGallerySWN(Context context, Bitmap bitmap) {
        if (bitmap == null) return false;
        if (Build.VERSION.SDK_INT < 29) {
            return saveImageToGallery0(context, bitmap);
        } else {
            return saveImageToGallery1(context, bitmap);
        }
    }

    // android 10 以下版本
    public static boolean saveImageToGallery0(Context context, Bitmap image) {
        // 首先保存图片
        String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "dearxy";

        File appDir = new File(storePath);
        if (!appDir.exists()) appDir.mkdir();
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            // 通过io流的方式来压缩保存图片
            boolean isSuccess = image.compress(Bitmap.CompressFormat.JPEG, 60, fos);
            fos.flush();
            fos.close();

            // 保存图片后发送广播通知更新数据库
            Uri uri = Uri.fromFile(file);
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            return isSuccess;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // android 10 以上版本
    public static boolean saveImageToGallery1(Context context, Bitmap image) {
        Long mImageTime = System.currentTimeMillis();
        String imageDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(mImageTime));
        String SCREENSHOT_FILE_NAME_TEMPLATE = "winetalk_%s.png";  // 图片名称，以"winetalk"+时间戳命名
        String mImageFileName = String.format(SCREENSHOT_FILE_NAME_TEMPLATE, imageDate);

        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES
                + File.separator + "winetalk");  // Environment.DIRECTORY_SCREENSHOTS:截图,图库中显示的文件夹名。"dh"
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, mImageFileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.DATE_ADDED, mImageTime / 1000);
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, mImageTime / 1000);
        values.put(MediaStore.MediaColumns.DATE_EXPIRES, (mImageTime + DateUtils.DAY_IN_MILLIS) / 1000);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        ContentResolver resolver = context.getContentResolver();
        final Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            // First, write the actual data for our screenshot
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    return false;
                }
            }
            // Everything went well above, publish it!
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            values.putNull(MediaStore.MediaColumns.DATE_EXPIRES);
            resolver.update(uri, values, null, null);
        } catch (IOException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                resolver.delete(uri, null);
            }
            return false;
        }
        return true;
    }


    CameraCaptureSession cameraCaptureSession;
    ImageReader imageReader;
    private String frontCameraId = null;
    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private void startCameraPreview(boolean isEmotion) {
        if (cameraDevice == null || !textureView.isAvailable()) {
            return;
        }
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if (surfaceTexture == null) {
            return;
        }
        android.util.Size previewSize = getOptimalPreviewSize();
        int desiredHeight = Objects.requireNonNull(previewSize).getWidth() * 4 / 3;
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = previewSize.getWidth();
        layoutParams.height = desiredHeight;
        textureView.setLayoutParams(layoutParams);

        try {
            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Log.d(TAG, "onImageAvailable: ");
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    saveImage(image, isEmotion);
                    image.close();
                }
            }, null);

            Surface previewSurface = new Surface(surfaceTexture);
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(previewSurface);

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        cameraCaptureSession = session;
                        // Start the camera preview
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                        Log.d(TAG, "onConfigured: ");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // Handle configuration failure
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getOptimalPreviewSize() {
        if (cameraDevice == null) {
            return null;
        }
        CameraCharacteristics characteristics = null;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        // 确定你的目标宽高比。这可能需要你调查系统相机应用的默认设置或用户可能的自定义设置。
        double targetRatio = 3.0 / 4.0; // 根据需要更改

        Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);

        Size targetPreviewSize = null;
        double closestRatioDiff = Double.MAX_VALUE;
        for (Size size : previewSizes) {
            double ratio = (double) size.getHeight() / size.getWidth();
            double diff = Math.abs(ratio - targetRatio);
            if (diff < closestRatioDiff) {
                closestRatioDiff = diff;
                targetPreviewSize = size;
            }
        }
        return targetPreviewSize;
    }

    private void toggleOtherLayout() {
        if (isOtherLayoutVisible) {
            if (llTool.getVisibility() == View.VISIBLE) {
                animateCollapseV(llTool);
            }
            // 隐藏其他布局
            animateCollapse(llCon1);
        } else {
            // 显示其他布局
            animateExpand(llCon1);
        }
        isOtherLayoutVisible = !isOtherLayoutVisible;
    }

    private void animateCollapse(final View view) {
        int originalWidth = view.getMeasuredWidth();
        ViewPropertyAnimator animator = view.animate().translationXBy(-originalWidth).setDuration(300);

        animator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });

        animator.start();
    }

    private void animateExpand(final View view) {
        view.setVisibility(View.VISIBLE);
        final int targetWidth = view.getMeasuredWidth();

        ViewPropertyAnimator animator = view.animate().translationXBy(targetWidth).setDuration(300);

        animator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });

        animator.start();
    }

    private void animateCollapseV(final View view) {
        int originalWidth = view.getMeasuredHeight();
        ViewPropertyAnimator animator = view.animate().translationYBy(originalWidth).setDuration(300);

        animator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });

        animator.start();
    }

    private void animateExpandV(final View view) {
        view.setVisibility(View.VISIBLE);
        final int targetWidth = view.getMeasuredHeight();

        ViewPropertyAnimator animator = view.animate().translationYBy(-targetWidth).setDuration(300);

        animator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });
        tvTitle.setText("Tools");
        animator.start();
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        // Release the camera resources if needed
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        super.onDestroy();
        if (layout != null) windowManager.removeView(layout);
        // 取消注册BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(imageSelectedReceiver);
    }

    private void getLatestTexts() {
        List<String> latestTexts = TextAccessibilityService.getLatestTexts();
        saveTextsToPreferences(latestTexts);
        new Handler(Looper.getMainLooper()).postDelayed(() -> Toast.makeText(FloatingWindowService.this, "Screen Content Saved", Toast.LENGTH_SHORT).show(), 0);
    }

    // 将最近获取的文本保存到 SharedPreferences
    private void saveTextsToPreferences(List<String> texts) {
        SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFERENCES_KEY, TextUtils.join("\n", texts));
        editor.apply();
    }

    @Override
    public void onTextReceived(List<String> allText) {
    }

    private void fetchWeatherData(double latitude, double longitude, WeatherTextCallback callback) {
        String url = "https://devapi.qweather.com/v7/grid-weather/now?location=" + longitude + "," + latitude + "&key=2d164f0582c247b696e9c0aa6302d874";
        final Request request = new Request.Builder()
                .url(url)
                .build();

        weatherClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONObject nowObject = jsonObject.getJSONObject("now");
                        String text = nowObject.getString("text");
                        Log.e("WeatherAPI", "Weather text: " + text);
                        // 当获取到天气文本后，调用回调
                        if (callback != null) {
                            callback.onWeatherTextReceived(text);
                        }
                    } catch (JSONException e) {
                        callback.onError(e);
                    }
                }
            }
        });
    }
}