package com.example.cunbangbang.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cunbangbang.AppConstant;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, AppConstant.DB_NAME, null, AppConstant.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + AppConstant.USER_TABLE + " (" +
                AppConstant.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + AppConstant.USER_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + AppConstant.HELP_TABLE);
        onCreate(db);
    }

    // User methods
    public long insertUser(String name, String village, String role) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_NAME, name);
        values.put(AppConstant.COL_VILLAGE, village);
        values.put(AppConstant.COL_ROLE, role);
        values.put(AppConstant.COL_POINTS, 0);
        return db.insert(AppConstant.USER_TABLE, null, values);
    }

    public UserBean getUserByNameAndVillage(String name, String village) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(AppConstant.USER_TABLE, null,
                AppConstant.COL_NAME + "=? AND " + AppConstant.COL_VILLAGE + "=?",
                new String[]{name, village}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            UserBean user = new UserBean();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
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

    public UserBean getUserById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(AppConstant.USER_TABLE, null,
                AppConstant.COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            UserBean user = new UserBean();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
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

    public void updateUserPoints(int userId, int newPoints) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_POINTS, newPoints);
        db.update(AppConstant.USER_TABLE, values, AppConstant.COL_ID + "=?", new String[]{String.valueOf(userId)});
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
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_ID)));
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_NAME)));
                user.setVillage(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_VILLAGE)));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(AppConstant.COL_ROLE)));
                user.setPoints(cursor.getInt(cursor.getColumnIndexOrThrow(AppConstant.COL_POINTS)));
                list.add(user);
            }
            cursor.close();
        }
        return list;
    }

    // HelpRecord methods
    public long insertHelpRecord(String helperName, String helperVillage, long timestamp, String fileName, String status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_HELPER_NAME, helperName);
        values.put(AppConstant.COL_HELPER_VILLAGE, helperVillage);
        values.put(AppConstant.COL_TIMESTAMP, timestamp);
        values.put(AppConstant.COL_FILE_NAME, fileName);
        values.put(AppConstant.COL_STATUS, status);
        return db.insert(AppConstant.HELP_TABLE, null, values);
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
        return list;
    }

    public void updateHelpRecordStatus(int id, String newStatus) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppConstant.COL_STATUS, newStatus);
        db.update(AppConstant.HELP_TABLE, values, AppConstant.COL_ID + "=?", new String[]{String.valueOf(id)});
    }
}