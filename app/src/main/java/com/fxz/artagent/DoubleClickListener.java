package com.fxz.artagent;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class DoubleClickListener implements View.OnTouchListener {
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // 设置双击的时间间隔，单位毫秒
    private long lastClickTime = 0;
    private boolean waitingForSecondClick = false;
    private Handler handler = new Handler();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                // 双击事件
                handler.removeCallbacksAndMessages(null);
                waitingForSecondClick = false;
                onDoubleClick(v);
            } else {
                // 单击事件
                waitingForSecondClick = true;
                handler.postDelayed(() -> {
                    if (waitingForSecondClick) {
                        waitingForSecondClick = false;
                        onSingleClick(v);
                    }
                }, DOUBLE_CLICK_TIME_DELTA);
            }
            lastClickTime = clickTime;
        }
        return true;
    }

    public void onSingleClick(View v) {
        // 处理单击事件
    }

    public void onDoubleClick(View v) {
        // 处理双击事件
    }
}