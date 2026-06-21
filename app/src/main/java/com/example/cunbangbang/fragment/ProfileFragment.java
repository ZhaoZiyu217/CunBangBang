package com.example.cunbangbang.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cunbangbang.R;
import com.example.cunbangbang.adapter.RankAdapter;
import com.example.cunbangbang.db.DBHelper;
import com.example.cunbangbang.db.UserBean;

import java.util.List;

public class ProfileFragment extends Fragment {

    private UserBean currentUser;
    private TextView tvName, tvVillage, tvPoints;
    private RecyclerView rvRank;
    private DBHelper dbHelper;

    public static ProfileFragment newInstance(UserBean user) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUser = (UserBean) getArguments().getSerializable("user");
        }
        dbHelper = new DBHelper(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvVillage = view.findViewById(R.id.tv_profile_village);
        tvPoints = view.findViewById(R.id.tv_profile_points);
        rvRank = view.findViewById(R.id.rv_rank);

        rvRank.setLayoutManager(new LinearLayoutManager(getContext()));

        loadData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        // 刷新用户信息
        UserBean updatedUser = dbHelper.getUserById(currentUser.getId());
        if (updatedUser != null) {
            currentUser = updatedUser;
        }

        tvName.setText("姓名：" + currentUser.getName());
        tvVillage.setText("村落：" + currentUser.getVillage());
        tvPoints.setText("当前积分：" + currentUser.getPoints());

        // 加载同村排行榜
        List<UserBean> rankList = dbHelper.getHelpersByVillage(currentUser.getVillage());
        RankAdapter adapter = new RankAdapter(rankList);
        rvRank.setAdapter(adapter);
    }
}
