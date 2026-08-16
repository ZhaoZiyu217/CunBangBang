package com.example.cunbangbang.util;

import android.util.Log;

import com.google.gson.Gson;

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

public class CloudTest {
    private static final String TAG = "CloudTest";

    // ⭐ 你的环境ID
    private static final String ENV_ID = "cunbangbang-d9gaknhjpe8dbf8fc";
    // ⭐ 你的 Publishable Key
    private static final String PUBLISHABLE_KEY = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjlkMWRjMzFlLWI0ZDAtNDQ4Yi1hNzZmLWIwY2M2M2Q4MTQ5OCJ9.eyJpc3MiOiJodHRwczovL2N1bmJhbmdiYW5nLWQ5Z2FrbmhqcGU4ZGJmOGZjLmFwLXNoYW5naGFpLnRjYi1hcGkudGVuY2VudGNsb3VkYXBpLmNvbSIsInN1YiI6ImFub24iLCJhdWQiOiJjdW5iYW5nYmFuZy1kOWdha25oanBlOGRiZjhmYyIsImV4cCI6NDA5MDAzMzkxNywiaWF0IjoxNzg2MzUwNzE3LCJub25jZSI6IkJ0cWZSVEx6U3hhZ2dRZFBwRW5UWUEiLCJhdF9oYXNoIjoiQnRxZlRUTHpTeGFnZ1FkUHBFblRZQSIsIm5hbWUiOiJBbm9ueW1vdXMiLCJzY29wZSI6ImFub255bW91cyIsInByb2plY3RfaWQiOiJjdW5iYW5nYmFuZy1kOWdha25oanBlOGRiZjhmYyIsIm1ldGEiOnsicGxhdGZvcm0iOiJQdWJsaXNoYWJsZUtleSJ9LCJ1c2VyX3R5cGUiOiIiLCJjbGllbnRfdHlwZSI6ImNsaWVudF91c2VyIiwiaXNfc3lzdGVtX2FkbWluIjpmYWxzZX0.ht17ggeq36qILUayESI4WZTQniVfJxQYXVDNTA4HD7GOg36EyIEYRx2ZoZ5hI9hyGIxkX7nW65FWeQOXbFel6aTtKkN5PWvTh-Ok9OJ_D2yyUeYRSovBU6YDXsD1iSDieM3mFpDQ9UnYGqidUINsx_BZWAxLA8_MS-BvhoO3au7FEwe3yRd1XXWbPW3IYOwUVrFwdDJBajafPU9h57armjcHc0oXDUi4jyaVmPJEUia56t3OUyQ44FmndgZyIEiyabx_I4aNfR__jpXjEmHKL-8QFb5E4dO9wZMXV2GXILGNWoOHlw_i1QnC4M_HPNHiT5eI-kYovifVVnB0XFcJag";

    private static final String BASE_URL = "https://" + ENV_ID + ".api.tcloudbasegateway.com/v1";

    private OkHttpClient client;
    private Gson gson;

    public CloudTest() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * 测试插入数据到 users 集合
     */
    public void testAddUser() {
        String url = BASE_URL + "/database/add";

        Map<String, Object> body = new HashMap<>();
        body.put("collection", "users");

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Android测试用户");
        data.put("village", "张庄村");
        data.put("role", "求助者");
        data.put("points", 0);
        data.put("createdAt", System.currentTimeMillis());
        body.put("data", data);

        String jsonBody = gson.toJson(body);
        Log.d(TAG, "📤 请求URL: " + url);
        Log.d(TAG, "📤 请求体: " + jsonBody);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + PUBLISHABLE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ 网络请求失败: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "📥 响应码: " + response.code());
                Log.d(TAG, "📥 响应体: " + responseBody);

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ 插入成功！");
                } else {
                    Log.e(TAG, "❌ 请求失败，错误码: " + response.code());
                }
            }
        });
    }
}

