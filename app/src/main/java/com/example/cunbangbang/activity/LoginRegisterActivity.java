package com.example.cunbangbang.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cunbangbang.AppConstant;
import com.example.cunbangbang.R;
import com.example.cunbangbang.db.DBHelper;
import com.example.cunbangbang.db.UserBean;
import com.example.cunbangbang.util.CloudManager;
import com.example.cunbangbang.util.PermissionUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class LoginRegisterActivity extends AppCompatActivity {

    private static final String TAG = "LoginRegister";
    private EditText etName;
    private Spinner spinnerVillage;
    private RadioGroup rgRole;
    private Button btnEnter;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this);

        PermissionUtil.checkAndRequestPermissions(this);

        // 检查是否已登录
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        if (userId != null) {
            UserBean savedUser = dbHelper.getUserById(userId);
            if (savedUser != null) {
                navigateToMain(savedUser);
                return;
            }
        }

        etName = findViewById(R.id.et_name);
        spinnerVillage = findViewById(R.id.spinner_village);
        rgRole = findViewById(R.id.rg_role);
        btnEnter = findViewById(R.id.btn_enter);

        String[] villages = {"张庄村", "李庄村", "王庄村"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, villages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVillage.setAdapter(adapter);

        btnEnter.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show();
                return;
            }

            String village = spinnerVillage.getSelectedItem().toString();
            int selectedRoleId = rgRole.getCheckedRadioButtonId();
            String role;
            if (selectedRoleId == R.id.radio_seeker) {
                role = AppConstant.ROLE_SEEKER;
            } else if (selectedRoleId == R.id.radio_helper) {
                role = AppConstant.ROLE_HELPER;
            } else {
                Toast.makeText(this, "请选择身份", Toast.LENGTH_SHORT).show();
                return;
            }

            checkUserExistsAndLogin(name, village, role);
        });

        testCloudConnection();
    }

    private void checkUserExistsAndLogin(String name, String village, String role) {
        Map<String, Object> where = new HashMap<>();
        where.put("name", name);
        where.put("village", village);

        CloudManager.getInstance().query("users", where, new CloudManager.CloudCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                try {
                    Log.d(TAG, "查询用户响应: " + result.toString());
                    JsonObject data = result.getAsJsonObject("data");
                    JsonArray records = data.getAsJsonArray("data");

                    if (records != null && records.size() > 0) {
                        JsonObject user = records.get(0).getAsJsonObject();
                        runOnUiThread(() -> {
                            Toast.makeText(LoginRegisterActivity.this, "欢迎回来，" + name, Toast.LENGTH_SHORT).show();
                            UserBean userBean = parseUserBean(user);
                            Log.d(TAG, "用户登录: " + userBean);
                            saveLoginState(userBean);
                            navigateToMain(userBean);
                        });
                    } else {
                        registerNewUser(name, village, role);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析用户数据失败", e);
                    runOnUiThread(() -> Toast.makeText(LoginRegisterActivity.this, "查询失败，请重试", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "查询用户失败: " + error);
                runOnUiThread(() -> Toast.makeText(LoginRegisterActivity.this, "网络异常，请检查连接", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void registerNewUser(String name, String village, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("village", village);
        userData.put("role", role);
        userData.put("points", 0);
        userData.put("createdAt", System.currentTimeMillis());

        CloudManager.getInstance().add("users", userData, new CloudManager.CloudCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "注册响应: " + result.toString());

                        UserBean newUser = new UserBean();
                        newUser.setName(name);
                        newUser.setVillage(village);
                        newUser.setRole(role);
                        newUser.setPoints(0);

                        String userId = null;
                        if (result.has("data")) {
                            JsonObject data = result.getAsJsonObject("data");
                            if (data.has("id")) {
                                userId = data.get("id").getAsString();
                            } else if (data.has("_id")) {
                                userId = data.get("_id").getAsString();
                            }
                        }
                        if (userId != null) {
                            newUser.setId(userId);
                        } else {
                            newUser.setId(String.valueOf(System.currentTimeMillis()));
                        }

                        // 同步保存到本地 SQLite
                        dbHelper.insertUser(newUser.getId(), newUser.getName(), newUser.getVillage(), newUser.getRole());

                        Toast.makeText(LoginRegisterActivity.this, "注册成功，欢迎 " + name, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "注册成功: " + newUser);
                        saveLoginState(newUser);
                        navigateToMain(newUser);

                    } catch (Exception e) {
                        Log.e(TAG, "注册成功但处理数据失败", e);
                        Toast.makeText(LoginRegisterActivity.this, "注册数据解析失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "注册失败: " + error);
                runOnUiThread(() -> Toast.makeText(LoginRegisterActivity.this, "注册失败：" + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private UserBean parseUserBean(JsonObject user) {
        UserBean bean = new UserBean();
        try {
            if (user.has("_id")) {
                bean.setId(user.get("_id").getAsString());
            }
            if (user.has("name")) {
                bean.setName(user.get("name").getAsString());
            }
            if (user.has("village")) {
                bean.setVillage(user.get("village").getAsString());
            }
            if (user.has("role")) {
                bean.setRole(user.get("role").getAsString());
            }
            if (user.has("points")) {
                bean.setPoints(user.get("points").getAsInt());
            }
        } catch (Exception e) {
            Log.e(TAG, "解析用户数据失败", e);
        }
        return bean;
    }

    private void testCloudConnection() {
        CloudManager.getInstance().testConnection(new CloudManager.CloudCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                Log.d(TAG, "✅ 云连接成功！");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ 云连接失败：" + error);
            }
        });
    }

    private void saveLoginState(UserBean user) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_id", user.getId());
        editor.putString("user_name", user.getName());
        editor.putString("user_village", user.getVillage());
        editor.putString("user_role", user.getRole());
        editor.putInt("user_points", user.getPoints());
        editor.apply();
        Log.d(TAG, "保存登录状态: " + user.getName());
    }

    private void navigateToMain(UserBean user) {
        if (AppConstant.ROLE_SEEKER.equals(user.getRole())) {
            SeekerMainActivity.start(this, user);
        } else {
            HelperMainActivity.start(this, user);
        }
        finish();
    }
}