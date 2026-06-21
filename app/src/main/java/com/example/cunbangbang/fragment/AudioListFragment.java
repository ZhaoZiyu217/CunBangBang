package com.example.cunbangbang.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cunbangbang.R;
import com.example.cunbangbang.adapter.AudioListAdapter;
import com.example.cunbangbang.db.DBHelper;
import com.example.cunbangbang.db.HelpRecordBean;
import com.example.cunbangbang.db.UserBean;

import java.util.List;

public class AudioListFragment extends Fragment {

    private UserBean currentUser;
    private RecyclerView rvAudioList;
    private AudioListAdapter adapter;
    private DBHelper dbHelper;

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
        dbHelper = new DBHelper(getContext());
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
        List<HelpRecordBean> records = dbHelper.getAllHelpRecords();
        adapter = new AudioListAdapter(getContext(), records);
        rvAudioList.setAdapter(adapter);
    }
}