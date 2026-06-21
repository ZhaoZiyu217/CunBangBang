package com.example.cunbangbang.activity;

import android.os.Bundle;
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
import com.example.cunbangbang.util.PermissionUtil;

public class LoginRegisterActivity extends AppCompatActivity {

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

        // 请求权限
        PermissionUtil.checkAndRequestPermissions(this);

        etName = findViewById(R.id.et_name);
        spinnerVillage = findViewById(R.id.spinner_village);
        rgRole = findViewById(R.id.rg_role);
        btnEnter = findViewById(R.id.btn_enter);

        // 设置村落下拉选项
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

            // 检查用户是否已存在
            UserBean existingUser = dbHelper.getUserByNameAndVillage(name, village);
            if (existingUser != null) {
                // 直接登录
                Toast.makeText(this, "欢迎回来，" + name, Toast.LENGTH_SHORT).show();
                navigateToMain(existingUser);
                return;
            }

            // 注册新用户
            long id = dbHelper.insertUser(name, village, role);
            if (id != -1) {
                UserBean newUser = dbHelper.getUserByNameAndVillage(name, village);
                Toast.makeText(this, "注册成功，欢迎 " + name, Toast.LENGTH_SHORT).show();
                navigateToMain(newUser);
            } else {
                Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });
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