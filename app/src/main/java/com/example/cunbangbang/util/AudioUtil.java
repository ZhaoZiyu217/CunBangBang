package com.example.cunbangbang.util;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
        mediaRecorder.setAudioSamplingRate(44100);
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

        try {
            mediaRecorder.stop();
            isRecording = false;
            Log.d(TAG, "录音停止: " + currentFilePath);
            if (callback != null) callback.onRecordingComplete(currentFilePath);
        } catch (IllegalStateException e) {
            Log.e(TAG, "录音停止异常", e);
            if (callback != null) callback.onError("录音停止异常: " + e.getMessage());
        } finally {
            releaseRecorder();
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
}