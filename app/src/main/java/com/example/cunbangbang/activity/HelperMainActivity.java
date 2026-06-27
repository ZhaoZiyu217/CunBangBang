package com.example.cunbangbang.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cunbangbang.AppConstant;
import com.example.cunbangbang.R;
import com.example.cunbangbang.db.DBHelper;
import com.example.cunbangbang.db.UserBean;
import com.example.cunbangbang.fragment.AudioListFragment;
import com.example.cunbangbang.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HelperMainActivity extends AppCompatActivity {

    private static final String TAG = "HelperMainActivity";
    private UserBean currentUser;
    private BottomNavigationView bottomNav;
    private ProfileFragment profileFragment;

    public static void start(Context context, UserBean user) {
        Intent intent = new Intent(context, HelperMainActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER, user);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_main);

        currentUser = (UserBean) getIntent().getSerializableExtra(AppConstant.EXTRA_USER);
        if (currentUser == null) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            int userId = prefs.getInt("user_id", -1);
            if (userId != -1) {
                DBHelper dbHelper = new DBHelper(this);
                currentUser = dbHelper.getUserById(userId);
            }
            if (currentUser == null) {
                finish();
                return;
            }
        }

        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AudioListFragment.newInstance(currentUser))
                    .commit();
        }

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                if (item.getItemId() == R.id.nav_audio_list) {
                    fragment = AudioListFragment.newInstance(currentUser);
                } else if (item.getItemId() == R.id.nav_profile) {
                    fragment = ProfileFragment.newInstance(currentUser);
                    if (fragment instanceof ProfileFragment) {
                        profileFragment = (ProfileFragment) fragment;
                    }
                }
                if (fragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }


    /**
     * 刷新个人主页数据
     */
    public void refreshProfile() {
        Log.d(TAG, "refreshProfile: 刷新个人主页");

        // 重新查询最新数据
        DBHelper dbHelper = new DBHelper(this);
        UserBean latestUser = dbHelper.getUserById(currentUser.getId());
        if (latestUser != null) {
            currentUser = latestUser;
            Log.d(TAG, "refreshProfile: 最新积分 = " + currentUser.getPoints());
        }
        // 直接重新创建 ProfileFragment 并替换
        ProfileFragment newProfile = ProfileFragment.newInstance(currentUser);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, newProfile)
                .commit();

        profileFragment = newProfile;


    }

    public void clearLoginState() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        finish();
    }
}