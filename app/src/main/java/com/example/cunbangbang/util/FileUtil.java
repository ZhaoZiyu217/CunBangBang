package com.example.cunbangbang.util;

import android.content.Context;
import android.os.Environment;

import com.example.cunbangbang.AppConstant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtil {

    public static File getAudioDir(Context context) {
        File dir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            dir = new File(Environment.getExternalStorageDirectory(), AppConstant.AUDIO_DIR);
        } else {
            dir = new File(context.getFilesDir(), AppConstant.AUDIO_DIR);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static List<File> getAudioFiles(Context context) {
        List<File> files = new ArrayList<>();
        File dir = getAudioDir(context);
        if (dir.exists() && dir.isDirectory()) {
            File[] list = dir.listFiles((d, name) -> name.endsWith(".aac"));
            if (list != null) {
                files.addAll(Arrays.asList(list));
                // Sort by last modified desc
                files.sort((o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
            }
        }
        return files;
    }

    public static String extractNameFromFileName(String fileName) {
        // Format: 姓名_时间戳.aac
        int underscoreIndex = fileName.indexOf("_");
        if (underscoreIndex != -1) {
            return fileName.substring(0, underscoreIndex);
        }
        return "未知";
    }

    public static long extractTimestampFromFileName(String fileName) {
        // Format: 姓名_时间戳.aac
        int underscoreIndex = fileName.indexOf("_");
        int dotIndex = fileName.lastIndexOf(".");
        if (underscoreIndex != -1 && dotIndex != -1 && dotIndex > underscoreIndex) {
            try {
                return Long.parseLong(fileName.substring(underscoreIndex + 1, dotIndex));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}