package com.example.cunbangbang.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
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

        // 如果已帮助，禁用帮帮TA按钮
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

                // 停止之前的播放
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
                Log.d(TAG, "========== 帮帮TA 点击 ==========");
                Log.d(TAG, "状态: " + record.getStatus());

                if (AppConstant.STATUS_PENDING.equals(record.getStatus())) {
                    Log.d(TAG, "状态为待帮助，开始处理");

                    // 更新状态为已帮助
                    dbHelper.updateHelpRecordStatus(record.getId(), AppConstant.STATUS_HELPED);
                    Log.d(TAG, "记录状态已更新为: 已帮助");

                    // 给帮助者积分 +10
                    String helperName = FileUtil.extractNameFromFileName(record.getFileName());
                    Log.d(TAG, "帮助者姓名: " + helperName);
                    Log.d(TAG, "村落: " + record.getHelperVillage());

                    UserBean helper = dbHelper.getUserByNameAndVillage(helperName, record.getHelperVillage());

                    if (helper != null) {
                        Log.d(TAG, "找到用户: " + helper.getName() + ", 当前积分: " + helper.getPoints());
                        int oldPoints = helper.getPoints();
                        int newPoints = oldPoints + 10;
                        dbHelper.updateUserPoints(helper.getId(), newPoints);
                        Log.d(TAG, "积分更新: " + oldPoints + " -> " + newPoints);
                        Toast.makeText(context, "已帮助 " + helperName + "，积分 +10", Toast.LENGTH_SHORT).show();

                        // ⭐ 通知积分已更新
                        if (onPointsUpdatedListener != null) {
                            Log.d(TAG, "通知监听器: 积分已更新");
                            onPointsUpdatedListener.onPointsUpdated();
                        } else {
                            Log.e(TAG, "监听器为空！");
                        }
                    } else {
                        Log.e(TAG, "未找到帮助者用户！");
                        Toast.makeText(context, "未找到用户信息", Toast.LENGTH_SHORT).show();
                    }

                    record.setStatus(AppConstant.STATUS_HELPED);
                    notifyItemChanged(position);
                } else {
                    Log.d(TAG, "状态不是待帮助: " + record.getStatus());
                }
            }
        });
        // 更新播放按钮文字
        if (playingPosition == position && audioUtil.isPlaying()) {
            holder.btnPlay.setText("播放中");
        } else {
            holder.btnPlay.setText("播放");
        }
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