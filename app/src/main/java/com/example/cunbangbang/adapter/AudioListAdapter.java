package com.example.cunbangbang.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
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

    private Context context;
    private List<HelpRecordBean> records;
    private DBHelper dbHelper;
    private AudioUtil audioUtil;
    private int playingPosition = -1;

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
                if (AppConstant.STATUS_PENDING.equals(record.getStatus())) {
                    // 更新状态为已帮助
                    dbHelper.updateHelpRecordStatus(record.getId(), AppConstant.STATUS_HELPED);

                    // 给帮助者积分 +10
                    UserBean helper = dbHelper.getUserByNameAndVillage(
                            FileUtil.extractNameFromFileName(record.getFileName()),
                            record.getHelperVillage()
                    );
                    if (helper != null) {
                        dbHelper.updateUserPoints(helper.getId(), helper.getPoints() + 10);
                    }

                    Toast.makeText(context, "已帮助 " + FileUtil.extractNameFromFileName(record.getFileName()), Toast.LENGTH_SHORT).show();
                    record.setStatus(AppConstant.STATUS_HELPED);
                    notifyItemChanged(position);
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