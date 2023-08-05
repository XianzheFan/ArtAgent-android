package com.fxz.artagent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public static final String CHANNEL_ID = "FloatingWindowServiceChannel";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        requestPermissions();
        requestWriteSettingsPermission();
        promptForAccessibility();
        checkDrawOverlayPermission();
        createNotificationChannel();
    }
}