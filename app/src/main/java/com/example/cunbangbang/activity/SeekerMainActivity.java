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
import com.example.cunbangbang.util.CloudManager;
import com.example.cunbangbang.util.FileUtil;
import com.example.cunbangbang.util.PermissionUtil;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SeekerMainActivity extends AppCompatActivity {

    private static final String TAG = "SeekerMainActivity";
    private TextView tvGreeting;
    private View btnRecord;
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
            String userId = prefs.getString("user_id", null);
            if (userId != null) {
                dbHelper = new DBHelper(this);
                currentUser = dbHelper.getUserById(userId);
            }
            if (currentUser == null) {
                Log.e(TAG, "无法获取用户信息，退出");
                finish();
                return;
            }
        }

        dbHelper = new DBHelper(this);
        audioUtil = new AudioUtil();

        tvGreeting = findViewById(R.id.tv_greeting);
        btnRecord = findViewById(R.id.btn_record);
        btnLogout = findViewById(R.id.btn_logout);

        tvGreeting.setText(currentUser.getName() + "（" + currentUser.getVillage() + "）");
        Log.d(TAG, "当前用户: " + currentUser);

        btnRecord.setOnTouchListener((v, event) -> {
            if (!PermissionUtil.hasPermissions(this)) {
                PermissionUtil.checkAndRequestPermissions(this);
                Toast.makeText(this, "请授予权限后再试", Toast.LENGTH_SHORT).show();
                return true;
            }

            TextView tvHoldSpeak = findViewById(R.id.tv_hold_speak);
            TextView tvHint = findViewById(R.id.tv_hint_inner);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下：缩小到 90%
                    v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(100).start();
                    if (tvHoldSpeak != null) {
                        tvHoldSpeak.setText("录音中...");
                        tvHoldSpeak.setTextSize(100);
                    }
                    if (tvHint != null) {
                        tvHint.setText("松开发送");
                    }
                    startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    // 松开：恢复 100%
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    if (tvHoldSpeak != null) {
                        tvHoldSpeak.setText("按\n住\n说\n话");
                        tvHoldSpeak.setTextSize(100);
                    }
                    if (tvHint != null) {
                        tvHint.setText("松开发送求助");
                    }
                    stopRecording();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    // 取消触摸：恢复
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    if (tvHoldSpeak != null) {
                        tvHoldSpeak.setText("按\n住\n说\n话");
                        tvHoldSpeak.setTextSize(100);
                    }
                    if (tvHint != null) {
                        tvHint.setText("松开发送求助");
                    }
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
        Log.d(TAG, "清除登录状态");
    }

    private void startRecording() {
        if (audioUtil.isRecording()) {
            return;
        }

        File audioDir = FileUtil.getAudioDir(this);
        String fileName = currentUser.getName() + "_" + System.currentTimeMillis() + ".aac";
        currentRecordingPath = new File(audioDir, fileName).getAbsolutePath();

        Log.d(TAG, "开始录音: " + currentRecordingPath);

        audioUtil.startRecording(currentRecordingPath, new AudioUtil.RecordingCallback() {
            @Override
            public void onRecordingComplete(String filePath) {
                // 录音完成交给 stopRecording 处理，这里不做重复操作
                Log.d(TAG, "录音完成，等待停止处理");
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "录音错误: " + error);
                    Toast.makeText(SeekerMainActivity.this, "录音失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void stopRecording() {
        if (audioUtil.isRecording()) {
            audioUtil.stopRecording(new AudioUtil.RecordingCallback() {
                @Override
                public void onRecordingComplete(String filePath) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "录音停止: " + filePath);
                        String fileNameOnly = new File(filePath).getName();

                        // ==================== 第1步：存本地 ====================
                        long localResult = dbHelper.insertHelpRecord(
                                currentUser.getName(),
                                currentUser.getVillage(),
                                System.currentTimeMillis(),
                                fileNameOnly,
                                AppConstant.STATUS_PENDING
                        );
                        Log.d(TAG, "本地插入结果: " + localResult);

                        // ==================== 第2步：上传到云存储 ====================
                        Log.d(TAG, "开始上传: " + fileNameOnly);

                        CloudManager.getInstance().uploadFile(filePath, fileNameOnly,
                                new CloudManager.CloudCallback<String>() {
                                    @Override
                                    public void onSuccess(String fileUrl) {
                                        Log.d(TAG, "✅ 上传成功，URL: " + fileUrl);

                                        // ==================== 第3步：存云端数据库 ====================
                                        Map<String, Object> helpData = new HashMap<>();
                                        helpData.put("helperName", currentUser.getName());
                                        helpData.put("helperVillage", currentUser.getVillage());
                                        helpData.put("timestamp", System.currentTimeMillis());
                                        helpData.put("fileName", fileNameOnly);
                                        helpData.put("fileUrl", fileUrl);
                                        helpData.put("status", AppConstant.STATUS_PENDING);

                                        CloudManager.getInstance().add("help_records", helpData,
                                                new CloudManager.CloudCallback<JsonObject>() {
                                                    @Override
                                                    public void onSuccess(JsonObject result) {
                                                        Log.d(TAG, "✅ 云端记录保存成功");
                                                        runOnUiThread(() -> {
                                                            Toast.makeText(SeekerMainActivity.this,
                                                                    "已发送求助", Toast.LENGTH_SHORT).show();
                                                        });
                                                    }

                                                    @Override
                                                    public void onFailure(String error) {
                                                        Log.e(TAG, "❌ 云端记录保存失败: " + error);
                                                        runOnUiThread(() -> {
                                                            Toast.makeText(SeekerMainActivity.this,
                                                                    "数据保存失败: " + error, Toast.LENGTH_SHORT).show();
                                                        });
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "❌ 上传失败: " + error);
                                        runOnUiThread(() -> {
                                            Toast.makeText(SeekerMainActivity.this,
                                                    "上传失败，请检查网络", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "录音停止错误: " + error);
                        Toast.makeText(SeekerMainActivity.this,
                                "录音失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioUtil != null) {
            audioUtil.releaseAll();
        }
    }
}



