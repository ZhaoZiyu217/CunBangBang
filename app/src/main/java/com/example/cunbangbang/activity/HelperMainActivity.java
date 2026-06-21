package com.example.cunbangbang.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cunbangbang.AppConstant;
import com.example.cunbangbang.R;
import com.example.cunbangbang.db.UserBean;
import com.example.cunbangbang.fragment.AudioListFragment;
import com.example.cunbangbang.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HelperMainActivity extends AppCompatActivity {

    private UserBean currentUser;
    private BottomNavigationView bottomNav;

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
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottom_navigation);

        // 默认加载第一个Fragment
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
}