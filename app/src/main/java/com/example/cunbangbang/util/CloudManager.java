package com.example.cunbangbang.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloudManager {
    private static final String TAG = "CloudManager";
    private static CloudManager instance;
    private final OkHttpClient client;
    private final Gson gson;

    // 环境ID
    private static final String ENV_ID = "cunbangbang-d9gaknhjpe8dbf8fc";

    // API Key（用于调用云函数）
    private static final String PUBLISHABLE_KEY = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjlkMWRjMzFlLWI0ZDAtNDQ4Yi1hNzZmLWIwY2M2M2Q4MTQ5OCJ9.eyJhdWQiOiJjdW5iYW5nYmFuZy1kOWdha25oanBlOGRiZjhmYyIsImV4cCI6MjUzNDAyMzAwNzk5LCJpYXQiOjE3ODY1MDQxNzEsImF0X2hhc2giOiJxMUF6bl9NZVJXLVFyQW9KU1QxOUx3IiwicHJvamVjdF9pZCI6ImN1bmJhbmdiYW5nLWQ5Z2FrbmhqcGU4ZGJmOGZjIiwibWV0YSI6eyJwbGF0Zm9ybSI6IkFwaUtleSJ9LCJhZG1pbmlzdHJhdG9yX2lkIjoiMjA4NTUyMzQyMDU3ODc2Njg1MCIsInVzZXJfdHlwZSI6IiIsImNsaWVudF90eXBlIjoiY2xpZW50X3NlcnZlciIsImlzX3N5c3RlbV9hZG1pbiI6dHJ1ZX0.W8-dOye6KQnCjt_uoWW6yStJLEk73kk64qH0rNxyzVVECMeGq55RLOwLaFkMr2KtjJXkBPwmEHJfXOA2N9bd6Y2v-UoWvE65i6YR8hap_aOzqKYhcVZE2DCYCaIgc89PId87BXuTMN7EoXWxrSufVbNsjySc1qKE3MKy7qvHvgmr7GXsPbw4QgtIqRBt2pD4ocWNv7oMUHGxx_RpiX1LpgutHC7ghSyog_ynwd5Wf9bOw7ekHCVaezfD1ZlWwFi7E5ZTHmyJs4Pk95wGWOF-TKFswz0INvPyLpuU_R_H6yX3tieseQyaZfSsywD0nzW6EwIGWpcGOoBTk-BUD24oNA";

    // ⭐ 云函数调用地址
    private static final String FUNCTIONS_URL = "https://" + ENV_ID + ".api.tcloudbasegateway.com/v1/functions/apiHandler";

    private CloudManager() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public static CloudManager getInstance() {
        if (instance == null) {
            instance = new CloudManager();
        }
        return instance;
    }

    /**
     * 插入数据（调用云函数）
     */
    public void add(String collection, Map<String, Object> data, CloudCallback<JsonObject> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", "add");
        body.put("collection", collection);
        body.put("data", data);
        callFunction(body, callback);
    }

    /**
     * 查询数据（调用云函数）
     */
    public void query(String collection, Map<String, Object> where, CloudCallback<JsonObject> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", "query");
        body.put("collection", collection);
        if (where != null && !where.isEmpty()) {
            body.put("where", where);
        } else {
            body.put("where", new HashMap<>());
        }
        callFunction(body, callback);
    }

    /**
     * 更新数据（调用云函数）
     */
    public void update(String collection, String docId, Map<String, Object> data, CloudCallback<JsonObject> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", "update");
        body.put("collection", collection);
        body.put("docId", docId);
        body.put("data", data);
        callFunction(body, callback);
    }

    /**
     * 测试连接
     */
    public void testConnection(CloudCallback<JsonObject> callback) {
        Log.d(TAG, "开始测试云函数连接...");
        query("users", null, callback);
    }

    /**
     * 通用云函数调用
     */
    private void callFunction(Map<String, Object> body, CloudCallback<JsonObject> callback) {
        String jsonBody = gson.toJson(body);

        Log.d(TAG, "📤 调用云函数: " + FUNCTIONS_URL);
        Log.d(TAG, "📤 请求体: " + jsonBody);

        Request request = new Request.Builder()
                .url(FUNCTIONS_URL)
                .addHeader("Authorization", "Bearer " + PUBLISHABLE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ 网络请求失败: " + e.getMessage());
                if (callback != null) callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "📥 响应码: " + response.code());
                Log.d(TAG, "📥 响应体: " + responseBody);

                try {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    if (callback != null) callback.onSuccess(result);
                } catch (Exception e) {
                    Log.e(TAG, "❌ 解析响应失败", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                }
            }
        });
    }

    public interface CloudCallback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }

    /**
     * 上传文件到云存储（官方两阶段方案）
     */
    public void uploadFile(String localPath, String fileName, CloudCallback<String> callback) {
        File file = new File(localPath);
        if (!file.exists()) {
            Log.e(TAG, "文件不存在: " + localPath);
            if (callback != null) callback.onFailure("文件不存在");
            return;
        }

        try {
            // 读取文件数据
            byte[] fileData = new byte[(int) file.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            fis.read(fileData);
            fis.close();
            Log.d(TAG, "文件大小: " + fileData.length + " bytes");

            // ==================== 第一步：获取上传信息 ====================
            getUploadInfo(fileName, new CloudCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject uploadInfo) {
                    try {
                        String uploadUrl = uploadInfo.get("uploadUrl").getAsString();
                        String authorization = uploadInfo.get("authorization").getAsString();
                        String token = uploadInfo.get("token").getAsString();
                        String cloudObjectMeta = uploadInfo.get("cloudObjectMeta").getAsString();
                        String cloudObjectId = uploadInfo.get("cloudObjectId").getAsString();

                        Log.d(TAG, "获取上传信息成功");
                        Log.d(TAG, "uploadUrl: " + uploadUrl);

                        // ==================== 第二步：上传文件 ====================
                        putFile(uploadUrl, authorization, token, cloudObjectMeta, fileData,
                                new CloudCallback<String>() {
                                    @Override
                                    public void onSuccess(String result) {
                                        // 上传成功，返回 cloudObjectId 作为 URL
                                        Log.d(TAG, "add 成功，响应: " + result.toString());
                                        if (callback != null) callback.onSuccess(cloudObjectId);
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "文件上传失败: " + error);
                                        if (callback != null) callback.onFailure(error);
                                    }
                                });

                    } catch (Exception e) {
                        Log.e(TAG, "解析上传信息失败", e);
                        if (callback != null) callback.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "获取上传信息失败: " + error);
                    if (callback != null) callback.onFailure(error);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "读取文件失败", e);
            if (callback != null) callback.onFailure(e.getMessage());
        }
    }

    /**
     * 第一步：获取上传信息
     */
    private void getUploadInfo(String fileName, CloudCallback<JsonObject> callback) {
        String url = "https://" + ENV_ID + ".api.tcloudbasegateway.com/v1/storages/get-objects-upload-info";

        // 构建请求体
        com.google.gson.JsonArray requestArray = new com.google.gson.JsonArray();
        com.google.gson.JsonObject item = new com.google.gson.JsonObject();
        item.addProperty("objectId", "audio/" + fileName);
        requestArray.add(item);

        String jsonBody = gson.toJson(requestArray);
        Log.d(TAG, "获取上传信息请求: " + jsonBody);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + PUBLISHABLE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取上传信息网络失败: " + e.getMessage());
                if (callback != null) callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "获取上传信息响应: " + responseBody);

                try {
                    if (!response.isSuccessful()) {
                        if (callback != null) callback.onFailure("HTTP " + response.code() + ": " + responseBody);
                        return;
                    }

                    com.google.gson.JsonArray result = gson.fromJson(responseBody, com.google.gson.JsonArray.class);
                    if (result.size() == 0) {
                        if (callback != null) callback.onFailure("返回结果为空");
                        return;
                    }

                    com.google.gson.JsonObject firstItem = result.get(0).getAsJsonObject();

                    // 检查是否有错误
                    if (firstItem.has("code")) {
                        String errorCode = firstItem.get("code").getAsString();
                        String errorMsg = firstItem.has("message") ? firstItem.get("message").getAsString() : "未知错误";
                        if (callback != null) callback.onFailure(errorCode + ": " + errorMsg);
                        return;
                    }

                    if (callback != null) callback.onSuccess(firstItem);

                } catch (Exception e) {
                    Log.e(TAG, "解析获取上传信息响应失败", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                }
            }
        });
    }

    /**
     * 第二步：PUT 上传文件
     */
    private void putFile(String uploadUrl, String authorization, String token,
                         String cloudObjectMeta, byte[] fileData, CloudCallback<String> callback) {
        Log.d(TAG, "开始 PUT 上传文件");

        RequestBody fileBody = RequestBody.create(fileData, MediaType.parse("audio/aac"));

        Request request = new Request.Builder()
                .url(uploadUrl)
                .put(fileBody)
                .addHeader("Authorization", authorization)
                .addHeader("X-Cos-Security-Token", token)
                .addHeader("X-Cos-Meta-Fileid", cloudObjectMeta)
                .addHeader("Content-Type", "audio/aac")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "PUT 上传失败: " + e.getMessage());
                if (callback != null) callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "PUT 上传响应码: " + response.code());
                Log.d(TAG, "PUT 上传响应体: " + responseBody);

                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess("上传成功");
                } else {
                    if (callback != null) callback.onFailure("上传失败，响应码: " + response.code());
                }
            }
        });
    }

    /**
     * 获取云存储文件的真实下载 URL
     */
    public void getDownloadUrl(String cloudFileId, CloudCallback<String> callback) {
        String url = "https://" + ENV_ID + ".api.tcloudbasegateway.com/v1/storages/get-objects-download-info";

        JsonArray requestArray = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("cloudObjectId", cloudFileId);
        requestArray.add(item);

        String jsonBody = gson.toJson(requestArray);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + PUBLISHABLE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取下载URL失败: " + e.getMessage());
                if (callback != null) callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "获取下载URL响应: " + responseBody);
                try {
                    JsonArray result = gson.fromJson(responseBody, JsonArray.class);
                    if (result.size() > 0) {
                        JsonObject first = result.get(0).getAsJsonObject();
                        if (first.has("downloadUrl")) {
                            String downloadUrl = first.get("downloadUrl").getAsString();
                            Log.d(TAG, "✅ 获取下载URL成功: " + downloadUrl);
                            if (callback != null) callback.onSuccess(downloadUrl);
                        } else {
                            if (callback != null) callback.onFailure("未找到 downloadUrl");
                        }
                    } else {
                        if (callback != null) callback.onFailure("返回结果为空");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析下载URL响应失败", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                }
            }
        });
    }
}