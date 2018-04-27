package com.hal9000.sensorsapp;

import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class AudioRecService extends Service {
    private static final String LOG_TAG = "AudioRecordTest";
    private MediaRecorder mRecorder = null;
    private MediaRecorder.OnInfoListener mRecorderOnInfo = null;   // for handling max file size / duration reached
    private String mFileName = null;
    private static final String KEY_FILE_NAME = "fileName";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        showLocationNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);

        // Gets data from the incoming Intent
        mFileName = intent.getStringExtra(KEY_FILE_NAME);

        mRecorderOnInfo = new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    stopRecording();
                }
            }
        };

        startRecording();
        return START_NOT_STICKY;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setMaxDuration(20000);  // in ms
        mRecorder.setMaxFileSize(50000000); // Approximately 50 megabytes
        mRecorder.setOnInfoListener(mRecorderOnInfo);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        stopSelf();
    }

    private void showLocationNotification()
    {
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Audio Rec Service")
                .setContentText("Service Started Successfully")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
    
    public static void start(Context context, String fileName) {
        Intent starter = new Intent(context, AudioRecService.class);
        starter.putExtra(KEY_FILE_NAME, fileName);
        context.startService(starter);
    }

    public static void stop(Context context){
        context.stopService(new Intent(context, AudioRecService.class));
    }
}
