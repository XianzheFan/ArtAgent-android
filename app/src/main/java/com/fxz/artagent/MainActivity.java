package com.fxz.artagent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    public static final String CHANNEL_ID = "FloatingWindowServiceChannel";
    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    OkHttpClient client = buildHttpClient();


    private void promptForAccessibility() {
        new AlertDialog.Builder(this)
                .setTitle("无障碍服务")
                .setMessage("请设置“交互控制-无障碍快捷方式”为“ArtAgent”，并开启本软件“下载服务”权限")
                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // 用户点击了“好的”按钮，引导他们到无障碍设置页面
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    }
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
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        mPermissionResult.launch(intent);
                    }
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText editText = findViewById(R.id.edit_text);
        final TextView textView = findViewById(R.id.text_view);
        Button button = findViewById(R.id.button);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        // 将服务器的响应显示给用户
                        final String resStr = Objects.requireNonNull(response.body()).string();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(resStr); // 更新TextView的内容
                            }
                        });
                    }
                });
            }
        });
        promptForAccessibility();
        checkDrawOverlayPermission();
        createNotificationChannel();
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