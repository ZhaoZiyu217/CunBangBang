package com.example.cunbangbang.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cunbangbang.R;
import com.example.cunbangbang.db.UserBean;

import java.util.List;

public class RankAdapter extends RecyclerView.Adapter<RankAdapter.ViewHolder> {

    private List<UserBean> rankList;

    public RankAdapter(List<UserBean> rankList) {
        this.rankList = rankList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rank, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserBean user = rankList.get(position);

        holder.tvRankNumber.setText((position + 1) + "");
        holder.tvRankName.setText(user.getName());
        holder.tvRankPoints.setText(user.getPoints() + "分");

        // 前三名特殊样式
        if (position == 0) {
            holder.tvRankNumber.setTextColor(0xFFFFD700);
        } else if (position == 1) {
            holder.tvRankNumber.setTextColor(0xFFC0C0C0);
        } else if (position == 2) {
            holder.tvRankNumber.setTextColor(0xFFCD7F32);
        } else {
            holder.tvRankNumber.setTextColor(0xFF666666);
        }
    }

    @Override
    public int getItemCount() {
        return rankList != null ? rankList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRankNumber, tvRankName, tvRankPoints;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRankNumber = itemView.findViewById(R.id.tv_rank_number);
            tvRankName = itemView.findViewById(R.id.tv_rank_name);
            tvRankPoints = itemView.findViewById(R.id.tv_rank_points);
        }
    }
}
