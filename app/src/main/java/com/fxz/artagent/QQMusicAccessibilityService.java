package com.fxz.artagent;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class QQMusicAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName().equals("com.tencent.qqmusic")) {
            AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
            if (rootInActiveWindow != null) {
                performClick(rootInActiveWindow);
            }
        }
    }

    private void performClick(AccessibilityNodeInfo nodeInfo) {
        // 这里需要自行查看QQ音乐界面结构，找出识曲按钮的id，替换"your_id"
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("your_id");
        for (AccessibilityNodeInfo node : list) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // 在这里处理服务被中断的情况
    }
}