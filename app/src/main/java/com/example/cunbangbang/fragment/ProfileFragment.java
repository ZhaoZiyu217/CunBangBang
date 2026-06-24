package com.example.cunbangbang.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cunbangbang.R;
import com.example.cunbangbang.activity.HelperMainActivity;
import com.example.cunbangbang.adapter.RankAdapter;
import com.example.cunbangbang.db.DBHelper;
import com.example.cunbangbang.db.UserBean;

import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private UserBean currentUser;
    private TextView tvName, tvVillage, tvPoints;
    private RecyclerView rvRank;
    private Button btnLogout;
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
        Log.d(TAG, "onCreateView: 创建新视图");
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvVillage = view.findViewById(R.id.tv_profile_village);
        tvPoints = view.findViewById(R.id.tv_profile_points);
        rvRank = view.findViewById(R.id.rv_rank);
        btnLogout = view.findViewById(R.id.btn_profile_logout);

        rvRank.setLayoutManager(new LinearLayoutManager(getContext()));

        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof HelperMainActivity) {
                ((HelperMainActivity) getActivity()).clearLoginState();
            }
        });

        loadData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: 刷新数据");
        loadData();
    }

    public void refreshData() {
        Log.d(TAG, "refreshData: 强制刷新");
        loadData();
    }

    private void loadData() {
        Log.d(TAG, "loadData: 从数据库重新读取用户信息");

        UserBean updatedUser = dbHelper.getUserById(currentUser.getId());
        if (updatedUser != null) {
            currentUser = updatedUser;
            Log.d(TAG, "当前积分: " + currentUser.getPoints());
        } else {
            Log.e(TAG, "无法获取用户信息");
        }

        String nameText = "姓名：" + currentUser.getName();
        String villageText = "村落：" + currentUser.getVillage();
        String pointsText = "当前积分：" + currentUser.getPoints();

        Log.d(TAG, "设置文本: " + pointsText);

        tvName.setText(nameText);
        tvVillage.setText(villageText);
        tvPoints.setText(pointsText);

        List<UserBean> rankList = dbHelper.getHelpersByVillage(currentUser.getVillage());
        Log.d(TAG, "排行榜人数: " + rankList.size());
        RankAdapter adapter = new RankAdapter(rankList);
        rvRank.setAdapter(adapter);
    }
}