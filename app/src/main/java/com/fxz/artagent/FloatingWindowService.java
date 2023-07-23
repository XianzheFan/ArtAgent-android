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

import java.util.List;

public class FloatingWindowService extends Service implements TextAccessibilityService.TextCallback {
    private WindowManager windowManager;
    private ImageView floatingIcon;
    public static final String CHANNEL_ID = "FloatingWindowServiceChannel";

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
                            // 在点击事件中，我们调用 getLatestTexts()
                            getLatestTexts();
                        }
                        lastAction = motionEvent.getAction();
                        return lastAction != MotionEvent.ACTION_MOVE;
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

    private void getLatestTexts() {
        List<String> latestTexts = TextAccessibilityService.getLatestTexts();
        Log.e("AccessibilityService", "AllText: " + latestTexts);
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                Toast.makeText(FloatingWindowService.this, "您生成的图片将保存至相册", Toast.LENGTH_SHORT).show(), 0);
    }

    @Override
    public void onTextReceived(List<String> allText) {
    }
}