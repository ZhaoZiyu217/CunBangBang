package com.example.cunbangbang.util;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

public class AudioUtil {
    private static final String TAG = "AudioUtil";
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String currentFilePath;
    private boolean isRecording = false;

    public interface RecordingCallback {
        void onRecordingComplete(String filePath);

        void onError(String error);
    }

    public interface PlaybackCallback {
        void onPlaybackComplete();

        void onError(String error);
    }

    public void startRecording(String filePath, RecordingCallback callback) {
        if (isRecording) {
            callback.onError("已经在录音中");
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mediaRecorder.setAudioSamplingRate(44100);
        }
        mediaRecorder.setOutputFile(filePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            currentFilePath = filePath;
            Log.d(TAG, "录音开始: " + filePath);
        } catch (IOException e) {
            Log.e(TAG, "录音准备失败", e);
            callback.onError("录音准备失败: " + e.getMessage());
            releaseRecorder();
        }
    }

    public void stopRecording(RecordingCallback callback) {
        if (!isRecording || mediaRecorder == null) {
            if (callback != null) callback.onError("没有正在进行的录音");
            return;
        }

        String filePath = currentFilePath;

        try {
            mediaRecorder.stop();
            isRecording = false;
            Log.d(TAG, "录音停止: " + filePath);
        } catch (RuntimeException e) {
            // ⭐ 录音太短，stop 会抛异常
            Log.e(TAG, "录音停止异常（可能太短）: " + e.getMessage());
            isRecording = false;
            releaseRecorder();
            if (callback != null) callback.onError("录音太短");
            return;
        }

        releaseRecorder();

        if (callback != null) {
            Log.d(TAG, "调用 onRecordingComplete");
            callback.onRecordingComplete(filePath);
        }


        releaseRecorder();

        // ⭐ 在 finally 外面调用回调
        if (callback != null) {
            Log.d(TAG, "调用 onRecordingComplete");
            callback.onRecordingComplete(filePath);
        }
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        isRecording = false;
    }

    public void playAudio(String filePath, PlaybackCallback callback) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            releasePlayer();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> {
                releasePlayer();
                if (callback != null) callback.onPlaybackComplete();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                releasePlayer();
                if (callback != null) callback.onError("播放错误: " + what + ", " + extra);
                return true;
            });
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "播放失败", e);
            releasePlayer();
            if (callback != null) callback.onError("播放失败: " + e.getMessage());
        }
    }

    public void stopPlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        releasePlayer();
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void releaseAll() {
        releaseRecorder();
        releasePlayer();
    }

    /**
     * 播放云端 URL 音频
     */
    public void playAudioUrl(String url, PlaybackCallback callback) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            releasePlayer();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                Log.d(TAG, "开始播放 URL: " + url);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                releasePlayer();
                if (callback != null) callback.onPlaybackComplete();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                releasePlayer();
                if (callback != null) callback.onError("播放错误: " + what + ", " + extra);
                return true;
            });
        } catch (IOException e) {
            Log.e(TAG, "播放失败", e);
            releasePlayer();
            if (callback != null) callback.onError("播放失败: " + e.getMessage());
        }
    }


    /**
     * 获取音频文件时长（毫秒）
     */
    public long getAudioDuration(String filePath) {
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                return Long.parseLong(durationStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取音频时长失败: " + e.getMessage());
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                // ignore
            }
        }
        return 0;
    }
}
