package com.example.cunbangbang.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cunbangbang.AppConstant;
import com.example.cunbangbang.R;
import com.example.cunbangbang.db.DBHelper;
import com.example.cunbangbang.db.UserBean;
import com.example.cunbangbang.util.AudioUtil;
import com.example.cunbangbang.util.FileUtil;
import com.example.cunbangbang.util.PermissionUtil;

import java.io.File;

public class SeekerMainActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private View btnRecord;
    private TextView tvHint;
    private Button btnLogout;

    private UserBean currentUser;
    private DBHelper dbHelper;
    private AudioUtil audioUtil;
    private String currentRecordingPath;

    public static void start(Context context, UserBean user) {
        Intent intent = new Intent(context, SeekerMainActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER, user);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seeker_main);

        currentUser = (UserBean) getIntent().getSerializableExtra(AppConstant.EXTRA_USER);
        if (currentUser == null) {
            finish();
            return;
        }

        dbHelper = new DBHelper(this);
        audioUtil = new AudioUtil();

        tvGreeting = findViewById(R.id.tv_greeting);
        btnRecord = findViewById(R.id.btn_record);
        tvHint = findViewById(R.id.tv_hint);
        btnLogout = findViewById(R.id.btn_logout);

        tvGreeting.setText("你好，" + currentUser.getName() + "  (" + currentUser.getVillage() + ")");

        btnRecord.setOnTouchListener((v, event) -> {
            if (!PermissionUtil.hasPermissions(this)) {
                PermissionUtil.checkAndRequestPermissions(this);
                Toast.makeText(this, "请授予权限后再试", Toast.LENGTH_SHORT).show();
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    stopRecording();
                    break;
            }
            return true;
        });

        btnLogout.setOnClickListener(v -> {
            audioUtil.releaseAll();
            finish();
        });
    }

    private void startRecording() {
        if (audioUtil.isRecording()) {
            return;
        }

        File audioDir = FileUtil.getAudioDir(this);
        String fileName = currentUser.getName() + "_" + System.currentTimeMillis() + ".aac";
        currentRecordingPath = new File(audioDir, fileName).getAbsolutePath();

        tvHint.setText("录音中... 松开发送");
        btnRecord.setBackgroundResource(R.drawable.bg_circle_orange_pressed);

        audioUtil.startRecording(currentRecordingPath, new AudioUtil.RecordingCallback() {
            @Override
            public void onRecordingComplete(String filePath) {
                runOnUiThread(() -> {
                    tvHint.setText("录音完成，已发送求助");
                    btnRecord.setBackgroundResource(R.drawable.bg_circle_orange);
                    Toast.makeText(SeekerMainActivity.this, "已发送求助", Toast.LENGTH_SHORT).show();

                    // 保存到数据库
                    dbHelper.insertHelpRecord(
                            currentUser.getName(),
                            currentUser.getVillage(),
                            System.currentTimeMillis(),
                            new File(filePath).getName(),
                            AppConstant.STATUS_PENDING
                    );
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvHint.setText("松开发送求助");
                    btnRecord.setBackgroundResource(R.drawable.bg_circle_orange);
                    Toast.makeText(SeekerMainActivity.this, "录音失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void stopRecording() {
        if (audioUtil.isRecording()) {
            audioUtil.stopRecording(null);
        }
        tvHint.setText("松开发送求助");
        btnRecord.setBackgroundResource(R.drawable.bg_circle_orange);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioUtil.releaseAll();
    }
}