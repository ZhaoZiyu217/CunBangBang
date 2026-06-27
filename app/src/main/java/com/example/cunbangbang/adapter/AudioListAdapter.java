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
import com.example.cunbangbang.db.UserBean;
import com.example.cunbangbang.util.AudioUtil;
import com.example.cunbangbang.util.FileUtil;

import java.io.File;
import java.util.List;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {

    private static final String TAG = "AudioListAdapter";
    private Context context;
    private List<HelpRecordBean> records;
    private DBHelper dbHelper;
    private AudioUtil audioUtil;
    private int playingPosition = -1;

    // 接口：通知积分更新
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        HelpRecordBean record = records.get(position);
        String name = FileUtil.extractNameFromFileName(record.getFileName());

        holder.tvName.setText(name);
        holder.tvStatus.setText(record.getStatus());

        if (AppConstant.STATUS_HELPED.equals(record.getStatus())) {
            holder.btnHelp.setEnabled(false);
            holder.btnHelp.setText("已帮助");
        } else {
            holder.btnHelp.setEnabled(true);
            holder.btnHelp.setText("帮帮TA");
        }

        // 播放按钮
        holder.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                audioUtil.playAudio(audioFile.getAbsolutePath(), new AudioUtil.PlaybackCallback() {
                    @Override
                    public void onPlaybackComplete() {
                        if (playingPosition != -1) {
                            int oldPos = playingPosition;
                            playingPosition = -1;
                            notifyItemChanged(oldPos);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(context, "播放失败: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
                playingPosition = position;
                holder.btnPlay.setText("播放中");
            }
        });

        // 帮帮TA按钮
        holder.btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppConstant.STATUS_PENDING.equals(record.getStatus())) {
                    Log.d(TAG, "========== 帮帮TA ==========");

                    dbHelper.updateHelpRecordStatus(record.getId(), AppConstant.STATUS_HELPED);
                    Log.d(TAG, "记录状态已更新为: 已帮助");

                    // ⭐ 给当前登录的帮助者加分
                    String currentHelperName = getCurrentHelperName();
                    String currentHelperVillage = getCurrentHelperVillage();

                    Log.d(TAG, "当前帮助者: " + currentHelperName + ", " + currentHelperVillage);

                    UserBean helper = dbHelper.getUserByNameAndVillage(currentHelperName, currentHelperVillage);

                    if (helper != null) {
                        int oldPoints = helper.getPoints();
                        int newPoints = oldPoints + 10;
                        dbHelper.updateUserPoints(helper.getId(), newPoints);
                        Log.d(TAG, "积分更新: " + oldPoints + " -> " + newPoints);
                        Toast.makeText(context, "已帮助 " + FileUtil.extractNameFromFileName(record.getFileName()) + "，积分 +10", Toast.LENGTH_SHORT).show();

                        if (onPointsUpdatedListener != null) {
                            onPointsUpdatedListener.onPointsUpdated();
                        }
                    } else {
                        Log.e(TAG, "未找到当前帮助者用户！");
                        Toast.makeText(context, "未找到用户信息", Toast.LENGTH_SHORT).show();
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

    // 获取当前登录的帮助者姓名
    private String getCurrentHelperName() {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getString("user_name", "");
    }

    // 获取当前登录的帮助者村落
    private String getCurrentHelperVillage() {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getString("user_village", "");
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