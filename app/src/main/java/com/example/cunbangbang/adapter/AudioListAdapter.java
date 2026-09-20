package com.example.cunbangbang.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cunbangbang.AppConstant;
import com.example.cunbangbang.R;
import com.example.cunbangbang.db.DBHelper;
import com.example.cunbangbang.db.HelpRecordBean;
import com.example.cunbangbang.util.AudioUtil;
import com.example.cunbangbang.util.CloudManager;
import com.example.cunbangbang.util.FileUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {

    private static final String TAG = "AudioListAdapter";
    private Context context;
    private List<HelpRecordBean> records;
    private DBHelper dbHelper;
    private AudioUtil audioUtil;
    private int playingPosition = -1;

    private OnPointsUpdatedListener onPointsUpdatedListener;

    public interface OnPointsUpdatedListener {
        void onPointsUpdated();
    }

    public void setOnPointsUpdatedListener(OnPointsUpdatedListener listener) {
        this.onPointsUpdatedListener = listener;
    }

    public AudioListAdapter(Context context, List<HelpRecordBean> records) {
        this.context = context;
        this.records = records;
        this.dbHelper = new DBHelper(context);
        this.audioUtil = new AudioUtil();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 将时间戳格式化为 年月日 时分
     */
    private String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        HelpRecordBean record = records.get(position);
        String name = FileUtil.extractNameFromFileName(record.getFileName());

        holder.tvName.setText(name);
        //holder.tvStatus.setText(record.getStatus());
        // ⭐ 显示时间（替代原来的状态）
        String timeText = formatTime(record.getTimestamp());
        holder.tvStatus.setText(timeText);

        if (AppConstant.STATUS_HELPED.equals(record.getStatus())) {
            holder.btnHelp.setEnabled(false);
            holder.btnHelp.setText("已帮助");
        } else {
            holder.btnHelp.setEnabled(true);
            holder.btnHelp.setText("帮帮TA");
        }

        // ==================== 播放按钮 ====================
        holder.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileUrl = record.getFileUrl();

                if (fileUrl != null && !fileUrl.isEmpty()) {
                    Log.d(TAG, "播放云端文件: " + fileUrl);

                    if (fileUrl.startsWith("cloud://")) {
                        // ⭐ 先获取真实 URL
                        CloudManager.getInstance().getDownloadUrl(fileUrl, new CloudManager.CloudCallback<String>() {
                            @Override
                            public void onSuccess(String realUrl) {
                                Log.d(TAG, "获取真实URL成功: " + realUrl);
                                // ⭐ 主线程播放
                                if (context instanceof android.app.Activity) {
                                    ((android.app.Activity) context).runOnUiThread(() -> {
                                        playAudio(realUrl, holder);
                                    });
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "获取真实URL失败: " + error);
                                if (context instanceof android.app.Activity) {
                                    ((android.app.Activity) context).runOnUiThread(() -> {
                                        Toast.makeText(context, "获取播放地址失败: " + error, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        });
                    } else {
                        // 直接播放
                        playAudio(fileUrl, holder);
                    }

                } else {
                    Log.d(TAG, "没有云端URL，尝试本地播放");
                    playLocalAudio(record, holder);
                }
            }
        });

        // ==================== 帮帮TA ====================
        holder.btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppConstant.STATUS_PENDING.equals(record.getStatus())) {
                    Log.d(TAG, "========== 帮帮TA ==========");
                    Log.d(TAG, "求助者: " + FileUtil.extractNameFromFileName(record.getFileName()));

                    SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    String currentHelperName = prefs.getString("user_name", "");
                    String currentHelperVillage = prefs.getString("user_village", "");
                    String currentHelperId = prefs.getString("user_id", "");
                    int currentPoints = prefs.getInt("user_points", 0);

                    Log.d(TAG, "当前帮助者: " + currentHelperName + ", " + currentHelperVillage);
                    Log.d(TAG, "当前积分: " + currentPoints);

                    if (currentHelperName.isEmpty() || currentHelperVillage.isEmpty()) {
                        Log.e(TAG, "未获取到当前帮助者信息");
                        Toast.makeText(context, "请重新登录", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 1. 更新本地求助记录状态
                    dbHelper.updateHelpRecordStatus(record.getId(), AppConstant.STATUS_HELPED);
                    Log.d(TAG, "本地记录状态已更新");

                    // 2. 更新云端求助记录状态
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("status", AppConstant.STATUS_HELPED);
                    Map<String, Object> where = new HashMap<>();
                    where.put("fileName", record.getFileName());

                    CloudManager.getInstance().query("help_records", where, new CloudManager.CloudCallback<JsonObject>() {
                        @Override
                        public void onSuccess(JsonObject result) {
                            try {
                                if (result.has("data")) {
                                    JsonObject data = result.getAsJsonObject("data");
                                    if (data.has("data")) {
                                        JsonArray recordsArray = data.getAsJsonArray("data");
                                        if (recordsArray != null && recordsArray.size() > 0) {
                                            JsonObject item = recordsArray.get(0).getAsJsonObject();
                                            String docId = item.get("_id").getAsString();
                                            CloudManager.getInstance().update("help_records", docId, updateData,
                                                    new CloudManager.CloudCallback<JsonObject>() {
                                                        @Override
                                                        public void onSuccess(JsonObject result) {
                                                            Log.d(TAG, "✅ 云端记录状态已更新");
                                                        }
                                                        @Override
                                                        public void onFailure(String error) {
                                                            Log.e(TAG, "❌ 云端记录状态更新失败: " + error);
                                                        }
                                                    });
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "解析更新结果失败", e);
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "查询云端记录失败: " + error);
                        }
                    });

                    // 3. 积分 +10
                    int newPoints = currentPoints + 10;
                    Log.d(TAG, "积分更新: " + currentPoints + " -> " + newPoints);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("user_points", newPoints);
                    editor.apply();

                    if (!currentHelperId.isEmpty()) {
                        dbHelper.updateUserPoints(currentHelperId, newPoints);
                        Log.d(TAG, "本地 SQLite 积分已更新");
                    }

                    // 更新云端用户积分
                    Map<String, Object> userWhere = new HashMap<>();
                    userWhere.put("name", currentHelperName);
                    userWhere.put("village", currentHelperVillage);

                    CloudManager.getInstance().query("users", userWhere, new CloudManager.CloudCallback<JsonObject>() {
                        @Override
                        public void onSuccess(JsonObject result) {
                            try {
                                if (result.has("data")) {
                                    JsonObject data = result.getAsJsonObject("data");
                                    if (data.has("data")) {
                                        JsonArray recordsArray = data.getAsJsonArray("data");
                                        if (recordsArray != null && recordsArray.size() > 0) {
                                            JsonObject item = recordsArray.get(0).getAsJsonObject();
                                            String docId = item.get("_id").getAsString();
                                            int oldPoints = item.get("points").getAsInt();
                                            int newPointsCloud = oldPoints + 10;

                                            Map<String, Object> pointUpdate = new HashMap<>();
                                            pointUpdate.put("points", newPointsCloud);

                                            CloudManager.getInstance().update("users", docId, pointUpdate,
                                                    new CloudManager.CloudCallback<JsonObject>() {
                                                        @Override
                                                        public void onSuccess(JsonObject result) {
                                                            Log.d(TAG, "✅ 云端积分已更新");
                                                        }
                                                        @Override
                                                        public void onFailure(String error) {
                                                            Log.e(TAG, "❌ 云端积分更新失败: " + error);
                                                        }
                                                    });
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "解析用户更新结果失败", e);
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "查询用户失败: " + error);
                        }
                    });

                    Toast.makeText(context, "已帮助 " + FileUtil.extractNameFromFileName(record.getFileName()) + "，积分 +10！当前积分：" + newPoints, Toast.LENGTH_LONG).show();

                    if (onPointsUpdatedListener != null) {
                        onPointsUpdatedListener.onPointsUpdated();
                    }

                    record.setStatus(AppConstant.STATUS_HELPED);
                    notifyItemChanged(position);
                }
            }
        });

        if (playingPosition == position && audioUtil.isPlaying()) {
            holder.btnPlay.setText("播放中");
        } else {
            holder.btnPlay.setText("播放");
        }
    }

    /**
     * 播放音频（云端 URL）- 必须在主线程调用
     */
    private void playAudio(String url, ViewHolder holder) {
        if (audioUtil.isPlaying()) {
            audioUtil.stopPlayback();
            if (playingPosition != -1) {
                notifyItemChanged(playingPosition);
            }
        }

        playingPosition = holder.getAdapterPosition();
        holder.btnPlay.setText("播放中");

        audioUtil.playAudioUrl(url, new AudioUtil.PlaybackCallback() {
            @Override
            public void onPlaybackComplete() {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        if (playingPosition != -1) {
                            int oldPos = playingPosition;
                            playingPosition = -1;
                            notifyItemChanged(oldPos);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Log.e(TAG, "播放云端文件失败: " + error);
                        Toast.makeText(context, "播放失败: " + error, Toast.LENGTH_SHORT).show();
                        if (playingPosition != -1) {
                            int oldPos = playingPosition;
                            playingPosition = -1;
                            notifyItemChanged(oldPos);
                        }
                    });
                }
            }
        });
    }

    /**
     * 播放本地音频
     */
    private void playLocalAudio(HelpRecordBean record, ViewHolder holder) {
        File audioDir = FileUtil.getAudioDir(context);
        File audioFile = new File(audioDir, record.getFileName());

        if (!audioFile.exists()) {
            Toast.makeText(context, "音频文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioUtil.isPlaying()) {
            audioUtil.stopPlayback();
            if (playingPosition != -1) {
                notifyItemChanged(playingPosition);
            }
        }

        playingPosition = holder.getAdapterPosition();
        holder.btnPlay.setText("播放中");

        audioUtil.playAudio(audioFile.getAbsolutePath(), new AudioUtil.PlaybackCallback() {
            @Override
            public void onPlaybackComplete() {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        if (playingPosition != -1) {
                            int oldPos = playingPosition;
                            playingPosition = -1;
                            notifyItemChanged(oldPos);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "播放失败: " + error, Toast.LENGTH_SHORT).show();
                        if (playingPosition != -1) {
                            int oldPos = playingPosition;
                            playingPosition = -1;
                            notifyItemChanged(oldPos);
                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (records != null) {
            return records.size();
        }
        return 0;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        audioUtil.releaseAll();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        Button btnPlay, btnHelp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvStatus = itemView.findViewById(R.id.tv_item_status);
            btnPlay = itemView.findViewById(R.id.btn_item_play);
            btnHelp = itemView.findViewById(R.id.btn_item_help);
        }
    }
}