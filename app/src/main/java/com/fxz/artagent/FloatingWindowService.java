package com.fxz.artagent;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class FloatingWindowService extends Service implements TextAccessibilityService.TextCallback {
    private WindowManager windowManager;
    private ImageView floatingIcon;
    public static final String CHANNEL_ID = "FloatingWindowServiceChannel";
    private List<String> cachedText = new ArrayList<>(); // 在类开始的地方添加
    private boolean isButtonClicked = false; // 新增标志位，用于跟踪悬浮窗是否被点击

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Window Service")
                .setContentText("This is running in the foreground.")
                .setSmallIcon(R.drawable.pen_icon)
                .build();

        startForeground(1, notification);
        floatingIcon = new ImageView(this);
        floatingIcon.setImageResource(R.drawable.pen_icon);

        final WindowManager.LayoutParams params;
        params = new WindowManager.LayoutParams(  // 设置图标为 60*60 像素
                dpToPx(60),
                dpToPx(60),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        floatingIcon.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        lastAction = motionEvent.getAction();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (motionEvent.getRawX() - initialTouchX);
                        params.y = initialY + (int) (motionEvent.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingIcon, params);
                        lastAction = motionEvent.getAction();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN ||
                                Math.abs(motionEvent.getRawX() - initialTouchX) < 10 &&
                                        Math.abs(motionEvent.getRawY() - initialTouchY) < 10) {
                            // 与原始点击位置相比，只有当没有移动或移动很小的距离时，才认为是点击
                            // 如果文本特别长，会有延迟
                            isButtonClicked = true;
                        }
                        lastAction = motionEvent.getAction();
                        return lastAction != MotionEvent.ACTION_MOVE; // 如果移动了，则不认为是点击
                    default:
                        return false;
                }
            }
        });

        windowManager.addView(floatingIcon, params);
        TextAccessibilityService.setTextCallback(this);
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingIcon != null) windowManager.removeView(floatingIcon);
    }

    @Override
    public void onTextReceived(List<String> allText) {
        if (isButtonClicked) { // 只有当悬浮窗被点击后，才返回无障碍功能获取的所有屏幕文本
            Log.e("AccessibilityService", "All Texts: " + allText);
            for (String every_text : allText) {  // 分行发送
                Log.e("AccessibilityService", "Every: " + every_text);
            }
            isButtonClicked = false; // 返回文本后重置标志位
            // 在主线程中延迟显示 Toast
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    Toast.makeText(FloatingWindowService.this, "您生成的图片将保存至相册", Toast.LENGTH_SHORT).show(), 500);
        }
    }  // 有时候会先返回 [100%, 8:26] 然后再返回文本，很慢
}