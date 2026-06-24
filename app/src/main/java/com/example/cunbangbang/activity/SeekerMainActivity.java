package com.example.cunbangbang.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "SeekerMainActivity";
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
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            int userId = prefs.getInt("user_id", -1);
            if (userId != -1) {
                dbHelper = new DBHelper(this);
                currentUser = dbHelper.getUserById(userId);
            }
            if (currentUser == null) {
                finish();
                return;
            }
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
            clearLoginState();
            audioUtil.releaseAll();
            finish();
        });
    }

    private void clearLoginState() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private void startRecording() {
        if (audioUtil.isRecording()) {
            return;
        }

        File audioDir = FileUtil.getAudioDir(this);
        String fileName = currentUser.getName() + "_" + System.currentTimeMillis() + ".aac";
        currentRecordingPath = new File(audioDir, fileName).getAbsolutePath();

        Log.d(TAG, "========== 开始录音 ==========");
        Log.d(TAG, "录音文件名: " + fileName);
        Log.d(TAG, "录音路径: " + currentRecordingPath);

        tvHint.setText("录音中... 松开发送");
        btnRecord.setBackgroundResource(R.drawable.bg_circle_orange_pressed);

        audioUtil.startRecording(currentRecordingPath, new AudioUtil.RecordingCallback() {
            @Override
            public void onRecordingComplete(String filePath) {
                runOnUiThread(() -> {
                    Log.d(TAG, "========== 录音完成 ==========");
                    Log.d(TAG, "文件路径: " + filePath);

                    // 检查文件是否存在
                    File audioFile = new File(filePath);
                    Log.d(TAG, "文件是否存在: " + audioFile.exists());
                    Log.d(TAG, "文件大小: " + audioFile.length());

                    // 提取文件名
                    String fileNameOnly = new File(filePath).getName();
                    Log.d(TAG, "文件名: " + fileNameOnly);

                    // 插入数据库
                    Log.d(TAG, "开始插入数据库...");
                    Log.d(TAG, "  求助者姓名: " + currentUser.getName());
                    Log.d(TAG, "  村落: " + currentUser.getVillage());
                    Log.d(TAG, "  文件名: " + fileNameOnly);

                    long result = dbHelper.insertHelpRecord(
                            currentUser.getName(),
                            currentUser.getVillage(),
                            System.currentTimeMillis(),
                            fileNameOnly,
                            AppConstant.STATUS_PENDING
                    );

                    Log.d(TAG, "数据库插入结果: " + result);
                    if (result == -1) {
                        Log.e(TAG, "数据库插入失败！");
                    } else {
                        Log.d(TAG, "数据库插入成功，ID: " + result);
                    }

                    // 验证：查询所有记录
                    java.util.List<com.example.cunbangbang.db.HelpRecordBean> records = dbHelper.getAllHelpRecords();
                    Log.d(TAG, "当前数据库总记录数: " + records.size());
                    for (int i = 0; i < records.size(); i++) {
                        Log.d(TAG, "  记录 " + i + ": " + records.get(i).getFileName());
                    }

                    tvHint.setText("录音完成，已发送求助");
                    btnRecord.setBackgroundResource(R.drawable.bg_circle_orange);
                    Toast.makeText(SeekerMainActivity.this, "已发送求助", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "录音错误: " + error);
                    tvHint.setText("松开发送求助");
                    btnRecord.setBackgroundResource(R.drawable.bg_circle_orange);
                    Toast.makeText(SeekerMainActivity.this, "录音失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void stopRecording() {
        if (audioUtil.isRecording()) {
            // 传入回调，而不是 null
            audioUtil.stopRecording(new AudioUtil.RecordingCallback() {
                @Override
                public void onRecordingComplete(String filePath) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "========== 录音完成 ==========");
                        Log.d(TAG, "文件路径: " + filePath);

                        File audioFile = new File(filePath);
                        Log.d(TAG, "文件是否存在: " + audioFile.exists());
                        Log.d(TAG, "文件大小: " + audioFile.length());

                        String fileNameOnly = new File(filePath).getName();
                        Log.d(TAG, "文件名: " + fileNameOnly);

                        Log.d(TAG, "开始插入数据库...");
                        Log.d(TAG, "  求助者姓名: " + currentUser.getName());
                        Log.d(TAG, "  村落: " + currentUser.getVillage());
                        Log.d(TAG, "  文件名: " + fileNameOnly);

                        long result = dbHelper.insertHelpRecord(
                                currentUser.getName(),
                                currentUser.getVillage(),
                                System.currentTimeMillis(),
                                fileNameOnly,
                                AppConstant.STATUS_PENDING
                        );

                        Log.d(TAG, "数据库插入结果: " + result);
                        if (result == -1) {
                            Log.e(TAG, "数据库插入失败！");
                        } else {
                            Log.d(TAG, "数据库插入成功，ID: " + result);
                        }

                        java.util.List<com.example.cunbangbang.db.HelpRecordBean> records = dbHelper.getAllHelpRecords();
                        Log.d(TAG, "当前数据库总记录数: " + records.size());

                        tvHint.setText("录音完成，已发送求助");
                        btnRecord.setBackgroundResource(R.drawable.bg_circle_orange);
                        Toast.makeText(SeekerMainActivity.this, "已发送求助", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "录音停止错误: " + error);
                        tvHint.setText("松开发送求助");
                        btnRecord.setBackgroundResource(R.drawable.bg_circle_orange);
                        Toast.makeText(SeekerMainActivity.this, "录音停止失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
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