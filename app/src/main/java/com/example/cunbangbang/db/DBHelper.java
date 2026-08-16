package com.example.cunbangbang.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.cunbangbang.AppConstant;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    public DBHelper(Context context) {
        super(context, AppConstant.DB_NAME, null, AppConstant.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // ⭐ id 改为 TEXT
        String createUserTable = "CREATE TABLE " + AppConstant.USER_TABLE + " (" +
                AppConstant.COL_ID + " TEXT PRIMARY KEY, " +
                AppConstant.COL_NAME + " TEXT, " +
                AppConstant.COL_VILLAGE + " TEXT, " +
                AppConstant.COL_ROLE + " TEXT, " +
                AppConstant.COL_POINTS + " INTEGER DEFAULT 0)";
        db.execSQL(createUserTable);

        String createHelpTable = "CREATE TABLE " + AppConstant.HELP_TABLE + " (" +
                AppConstant.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                AppConstant.COL_HELPER_NAME + " TEXT, " +
                AppConstant.COL_HELPER_VILLAGE + " TEXT, " +
                AppConstant.COL_TIMESTAMP + " LONG, " +
                AppConstant.COL_FILE_NAME + " TEXT, " +
                AppConstant.COL_STATUS + " TEXT)";
        db.execSQL(createHelpTable);

        Log.d(TAG, "数据库表创建成功，版本: " + AppConstant.DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "数据库升级: " + oldVersion + " -> " + newVersion);

        // 从旧版本升级到新版本，逐版本处理
        if (oldVersion < 3) {
            // 版本2 → 版本3：添加 fileUrl 列
            try {
                db.execSQL("ALTER TABLE " + AppConstant.HELP_TABLE + " ADD COLUMN fileUrl TEXT");
                Log.d(TAG, "添加 fileUrl 列成功");
            } catch (Exception e) {
                Log.e(TAG, "添加 fileUrl 列失败: " + e.getMessage());
            }
        }

        // 如果有更多版本升级，继续添加
        // if (oldVersion < 4) { ... }
    }

    // ========== User 操作 ==========

    public long insertUser(String id, String name, String village, String role) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_ID, id);
        values.put(AppConstant.COL_NAME, name);
        values.put(AppConstant.COL_VILLAGE, village);
        values.put(AppConstant.COL_ROLE, role);
        values.put(AppConstant.COL_POINTS, 0);
        long result = db.insert(AppConstant.USER_TABLE, null, values);
        Log.d(TAG, "插入用户: " + name + ", id=" + id + ", 结果=" + result);
        return result;
    }

    public UserBean getUserById(String id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(AppConstant.USER_TABLE, null,
                AppConstant.COL_ID + "=?",
                new String[]{id}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            UserBean user = new UserBean();
            user.setId(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_NAME)));
            user.setVillage(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_VILLAGE)));
            user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_ROLE)));
            user.setPoints(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_POINTS)));
            cursor.close();
            Log.d(TAG, "查询用户成功: " + user.getName());
            return user;
        }
        if (cursor != null) cursor.close();
        Log.d(TAG, "未找到用户: id=" + id);
        return null;
    }

    public UserBean getUserByNameAndVillage(String name, String village) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(AppConstant.USER_TABLE, null,
                AppConstant.COL_NAME + "=? AND " + AppConstant.COL_VILLAGE + "=?",
                new String[]{name, village}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            UserBean user = new UserBean();
            user.setId(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_NAME)));
            user.setVillage(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_VILLAGE)));
            user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_ROLE)));
            user.setPoints(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_POINTS)));
            cursor.close();
            return user;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public void updateUserPoints(String userId, int newPoints) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_POINTS, newPoints);
        int rows = db.update(AppConstant.USER_TABLE, values, AppConstant.COL_ID + "=?", new String[]{userId});
        Log.d(TAG, "更新积分: userId=" + userId + ", newPoints=" + newPoints + ", 影响行数=" + rows);
    }

    public List<UserBean> getHelpersByVillage(String village) {
        List<UserBean> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(AppConstant.USER_TABLE, null,
                AppConstant.COL_VILLAGE + "=? AND " + AppConstant.COL_ROLE + "=?",
                new String[]{village, AppConstant.ROLE_HELPER},
                null, null, AppConstant.COL_POINTS + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                UserBean user = new UserBean();
                user.setId(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_NAME)));
                user.setVillage(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_VILLAGE)));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_ROLE)));
                user.setPoints(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_POINTS)));
                list.add(user);
            }
            cursor.close();
        }
        Log.d(TAG, "同村帮助者人数: " + list.size());
        return list;
    }

    // ========== HelpRecord 操作 ==========

    public long insertHelpRecord(String helperName, String helperVillage, long timestamp, String fileName, String status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_HELPER_NAME, helperName);
        values.put(AppConstant.COL_HELPER_VILLAGE, helperVillage);
        values.put(AppConstant.COL_TIMESTAMP, timestamp);
        values.put(AppConstant.COL_FILE_NAME, fileName);
        values.put(AppConstant.COL_STATUS, status);
        long result = db.insert(AppConstant.HELP_TABLE, null, values);
        Log.d(TAG, "插入求助记录: " + fileName + ", 结果=" + result);
        return result;
    }

    public List<HelpRecordBean> getAllHelpRecords() {
        List<HelpRecordBean> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(AppConstant.HELP_TABLE, null,
                null, null, null, null, AppConstant.COL_TIMESTAMP + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HelpRecordBean record = new HelpRecordBean();
                record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
                record.setHelperName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_HELPER_NAME)));
                record.setHelperVillage(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_HELPER_VILLAGE)));
                record.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(AppConstant.COL_TIMESTAMP)));
                record.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_FILE_NAME)));
                record.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_STATUS)));
                list.add(record);
            }
            cursor.close();
        }
        Log.d(TAG, "求助记录总数: " + list.size());
        return list;
    }

    // 查询方法中添加 fileUrl
    public List<HelpRecordBean> getHelpRecordsByVillage(String village) {
        List<HelpRecordBean> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(AppConstant.HELP_TABLE, null,
                AppConstant.COL_HELPER_VILLAGE + "=?",
                new String[]{village},
                null, null, AppConstant.COL_TIMESTAMP + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HelpRecordBean record = new HelpRecordBean();
                record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
                record.setHelperName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_HELPER_NAME)));
                record.setHelperVillage(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_HELPER_VILLAGE)));
                record.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(AppConstant.COL_TIMESTAMP)));
                record.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_FILE_NAME)));
                // ⭐ 新增：读取 fileUrl
                int fileUrlIndex = cursor.getColumnIndex("fileUrl");
                if (fileUrlIndex != -1) {
                    record.setFileUrl(cursor.getString(fileUrlIndex));
                }
                record.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_STATUS)));
                list.add(record);
            }
            cursor.close();
        }
        return list;
    }

    public void updateHelpRecordStatus(int id, String newStatus) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_STATUS, newStatus);
        int rows = db.update(AppConstant.HELP_TABLE, values, AppConstant.COL_ID + "=?", new String[]{String.valueOf(id)});
        Log.d(TAG, "更新求助记录状态: id=" + id + ", newStatus=" + newStatus + ", 影响行数=" + rows);
    }
}