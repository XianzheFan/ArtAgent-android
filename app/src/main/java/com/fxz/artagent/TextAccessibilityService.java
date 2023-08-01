package com.fxz.artagent;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class TextAccessibilityService extends AccessibilityService {
    private static List<String> latestTexts = new ArrayList<>();
    public static List<String> getLatestTexts() {
        return latestTexts;
    }

//    private String latestText = "";
//    public String getLatestText() {
//        return latestText;
//    }  // 如果只返回点击的部分

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
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            try {
                latestTexts = getAllTexts(nodeInfo);
                if (textCallback != null) {
                    textCallback.onTextReceived(latestTexts);
                }

//                latestText = (nodeInfo.getText() != null) ? nodeInfo.getText().toString() : "";  // 只返回点击的部分

                CharSequence text = nodeInfo.getText();
                CharSequence description = nodeInfo.getContentDescription();

//                if (text != null) {  // 用户点击哪里就出现哪里
//                    Log.e("AccessibilityService", "Text: " + text);
//                }
//
//                if (description != null) {  // 部件名称
//                    Log.e("AccessibilityService", "Content Description: " + description);
//                }
//
//                List<String> texts = getAllTexts(nodeInfo);
//                Log.e("AccessibilityService", "All Texts: " + texts);
//
//                for (String every_text : texts) {  // 分行发送
//                    Log.e("AccessibilityService", "Every: " + every_text);
//                }

            } finally {  // 最后需要回收
                nodeInfo.recycle();
            }
        }
    }

    @Override
    public void onInterrupt() {  // 中断时的操作，比如关闭文件，停止线程等
        Toast.makeText(this, "Service has been interrupted", Toast.LENGTH_SHORT).show();
    }
}