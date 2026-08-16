package com.example.cunbangbang.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cunbangbang.R;
import com.example.cunbangbang.activity.HelperMainActivity;
import com.example.cunbangbang.adapter.AudioListAdapter;
import com.example.cunbangbang.db.HelpRecordBean;
import com.example.cunbangbang.db.UserBean;
import com.example.cunbangbang.util.CloudManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioListFragment extends Fragment {

    private static final String TAG = "AudioListFragment";
    private UserBean currentUser;
    private RecyclerView rvAudioList;
    private AudioListAdapter adapter;

    public static AudioListFragment newInstance(UserBean user) {
        AudioListFragment fragment = new AudioListFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_list, container, false);

        rvAudioList = view.findViewById(R.id.rv_audio_list);
        rvAudioList.setLayoutManager(new LinearLayoutManager(getContext()));

        loadData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (currentUser == null) {
            Log.e(TAG, "currentUser 为空");
            return;
        }

        Log.d(TAG, "查询村落: " + currentUser.getVillage());

        Map<String, Object> where = new HashMap<>();
        where.put("helperVillage", currentUser.getVillage());

        CloudManager.getInstance().query("help_records", where, new CloudManager.CloudCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                List<HelpRecordBean> records = parseHelpRecords(result);
                // ⭐ 按时间戳降序排列（最新的在前）
                records.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                Log.d(TAG, "查询到记录数: " + records.size());

                // ⭐ 打印每条记录的 fileUrl
                for (HelpRecordBean bean : records) {
                    Log.d(TAG, "传给Adapter的fileUrl: " + bean.getFileName() + " -> " + bean.getFileUrl());
                }

                requireActivity().runOnUiThread(() -> {
                    adapter = new AudioListAdapter(getContext(), records);
                    adapter.setOnPointsUpdatedListener(() -> {
                        if (getActivity() instanceof HelperMainActivity) {
                            ((HelperMainActivity) getActivity()).refreshProfile();
                        }
                    });
                    rvAudioList.setAdapter(adapter);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "查询云端求助记录失败: " + error);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "加载失败，请检查网络", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 解析云端返回的帮助记录
     */
    private List<HelpRecordBean> parseHelpRecords(JsonObject result) {
        List<HelpRecordBean> list = new ArrayList<>();
        try {
            if (result.has("data")) {
                JsonObject data = result.getAsJsonObject("data");
                if (data.has("data")) {
                    JsonArray records = data.getAsJsonArray("data");
                    if (records != null) {
                        for (int i = 0; i < records.size(); i++) {
                            JsonObject item = records.get(i).getAsJsonObject();
                            HelpRecordBean bean = new HelpRecordBean();

                            if (item.has("_id")) {
                                try {
                                    bean.setId(Integer.parseInt(item.get("_id").getAsString()));
                                } catch (NumberFormatException e) {
                                    bean.setId(-1);
                                }
                            }
                            if (item.has("helperName")) {
                                bean.setHelperName(item.get("helperName").getAsString());
                            }
                            if (item.has("helperVillage")) {
                                bean.setHelperVillage(item.get("helperVillage").getAsString());
                            }
                            if (item.has("fileName")) {
                                bean.setFileName(item.get("fileName").getAsString());
                            }
                            // ⭐ 关键：解析 fileUrl
                            if (item.has("fileUrl")) {
                                bean.setFileUrl(item.get("fileUrl").getAsString());
                                Log.d(TAG, "解析到 fileUrl: " + item.get("fileUrl").getAsString());
                            } else {
                                Log.d(TAG, "item 没有 fileUrl 字段: " + item.toString());
                            }
                            if (item.has("status")) {
                                bean.setStatus(item.get("status").getAsString());
                            }
                            if (item.has("timestamp")) {
                                bean.setTimestamp(item.get("timestamp").getAsLong());
                            }
                            list.add(bean);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析帮助记录失败", e);
        }
        return list;
    }
}