package com.fxz.artagent;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class TextAccessibilityService extends AccessibilityService {
    public static List<String> getLatestTexts() {
        return self.getAllTexts();
    }
    private static TextAccessibilityService self;
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        self = this;
    }
    private List<String> getAllTexts() {
        List<String> ret = new ArrayList<>();
        List<AccessibilityWindowInfo> windows = getWindows();
        for (AccessibilityWindowInfo window : windows) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                if (window.getRoot() != null) {
                    ret.addAll(getAllTexts(window.getRoot()));
                }
            }
        }
        return ret;
    }

    public List<String> getAllTexts(AccessibilityNodeInfo node) {
        // 所有ui部件都会展开，甚至不包括出现在用户眼前的文字
        List<String> texts = new ArrayList<>();
        if (node != null) {
            if (node.getText() != null) {
                texts.add(node.getText().toString());
            }
            if (node.getChildCount() > 0) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    texts.addAll(getAllTexts(node.getChild(i)));
                }
            }
        }
        return texts;
    }

    // 定义回调接口
    public interface TextCallback {
        void onTextReceived(List<String> texts);
    }

    // 在类里面添加一个回调实例
    private static TextCallback textCallback;

    // 添加一个方法，让外部类可以设置回调
    public static void setTextCallback(TextCallback callback) {
        textCallback = callback;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {  // 中断时的操作，比如关闭文件，停止线程等
        Toast.makeText(this, "Service has been interrupted", Toast.LENGTH_SHORT).show();
    }
}