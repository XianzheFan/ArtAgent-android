package com.fxz.artagent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class ImageSelectorActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> mGetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGetContent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null && intent.getData() != null) {
                            Uri imageUri = intent.getData();

                            // 将图片的Uri发送给FloatingWindowService
                            Intent sendIntent = new Intent("com.fxz.artagent.ACTION_IMAGE_SELECTED");
                            sendIntent.putExtra("IMAGE_URI", imageUri);
                            sendBroadcast(sendIntent);

                            // 关闭Activity
                            finish();
                        }
                    }
                }
        );

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mGetContent.launch(intent);
    }
}