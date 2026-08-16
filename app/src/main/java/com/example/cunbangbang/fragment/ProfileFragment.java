package com.example.cunbangbang.fragment;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.cunbangbang.util.CloudManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Log.d(TAG, "onCreate: 用户 " + (currentUser != null ? currentUser.getName() : "null"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
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
        Log.d(TAG, "loadData: 加载数据");

        // ⭐ 从 SharedPreferences 读取用户信息
        SharedPreferences prefs = getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String name = prefs.getString("user_name", "未知");
        String village = prefs.getString("user_village", "未知");
        int points = prefs.getInt("user_points", 0);

        Log.d(TAG, "SharedPreferences 数据: " + name + ", " + village + ", 积分=" + points);

        tvName.setText("姓名：" + name);
        tvVillage.setText("村落：" + village);
        tvPoints.setText("当前积分：" + points);

        // ⭐ 从云端查询同村帮助者排行榜
        Map<String, Object> where = new HashMap<>();
        where.put("village", village);
        where.put("role", "帮助者");

        CloudManager.getInstance().query("users", where, new CloudManager.CloudCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                List<UserBean> rankList = parseRankList(result, village);
                Log.d(TAG, "排行榜人数: " + rankList.size());
                requireActivity().runOnUiThread(() -> {
                    RankAdapter adapter = new RankAdapter(rankList);
                    rvRank.setAdapter(adapter);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "查询排行榜失败: " + error);
            }
        });
    }

    /**
     * 解析排行榜数据
     */
    private List<UserBean> parseRankList(JsonObject result, String village) {
        List<UserBean> list = new ArrayList<>();
        try {
            if (result.has("data")) {
                JsonObject data = result.getAsJsonObject("data");
                if (data.has("data")) {
                    JsonArray records = data.getAsJsonArray("data");
                    if (records != null) {
                        for (int i = 0; i < records.size(); i++) {
                            JsonObject item = records.get(i).getAsJsonObject();
                            UserBean bean = new UserBean();
                            if (item.has("_id")) {
                                bean.setId(item.get("_id").getAsString());
                            }
                            if (item.has("name")) {
                                bean.setName(item.get("name").getAsString());
                            }
                            if (item.has("village")) {
                                bean.setVillage(item.get("village").getAsString());
                            }
                            if (item.has("role")) {
                                bean.setRole(item.get("role").getAsString());
                            }
                            if (item.has("points")) {
                                bean.setPoints(item.get("points").getAsInt());
                            }
                            // 只保留同村帮助者
                            if (village.equals(bean.getVillage()) && "帮助者".equals(bean.getRole())) {
                                list.add(bean);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析排行榜失败", e);
        }
        // 按积分排序
        list.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
        return list;
    }
}