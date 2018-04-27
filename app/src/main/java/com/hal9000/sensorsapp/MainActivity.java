package com.hal9000.sensorsapp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static String KEY_REC_BUTTON = "key_rec_button";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String LOG_TAG = "AudioRecordTest";

    boolean isRecServiceRunning = false;   // used while restoring recServiceButton state to prevent starting duplicate service (which causesd crash)
    private ToggleButton playButton = null;
    private ToggleButton recServiceButton = null;

    // Media player
    private String mFileName;
    private MediaPlayer mPlayer = null;
    private MediaPlayer.OnCompletionListener mPlayerOnCompletion = null;    // for handling play button after end of file was reached

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    // Sensor handling
    private SensorManager mSensorManager;
    private ShakeEventListener mShakeSensorListener;
    private AxisTiltEventListener mAxisSensorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";

        initRecButton();
        initPlayButton();

        // Handle sensor data
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        defineOnShakeEvent();
        defineOnTiltEvent();

        restoreState();
    }

    private void defineOnTiltEvent() {
        mAxisSensorListener = new AxisTiltEventListener();
        mAxisSensorListener.setOnTiltListener(new AxisTiltEventListener.OnTiltListener() {
            @Override
            public void onFaceDown() {
                recServiceButton.setChecked(true);  // start recording
            }
        });
    }

    private void defineOnShakeEvent() {
        mShakeSensorListener = new ShakeEventListener();
        mShakeSensorListener.setOnShakeListener(new ShakeEventListener.OnShakeListener() {
            @Override
            public void onShake() {
                recServiceButton.setChecked(! recServiceButton.isChecked());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();
    }

    private void initPlayButton() {
        playButton  = findViewById(R.id.button2_play);
        playButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    startPlaying();
                    recServiceButton.setVisibility(View.GONE);
                }
                else{
                    stopPlaying();
                    recServiceButton.setVisibility(View.VISIBLE);
                }
            }
        });

        mPlayerOnCompletion = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playButton.setChecked(! playButton.isChecked());
            }
        };
    }

    private void initRecButton() {
        recServiceButton = findViewById(R.id.button2_rec);

        recServiceButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveState();
                if(isChecked){
                    if (!isRecServiceRunning) {
                        AudioRecService.start(MainActivity.this, mFileName);
                        Toast.makeText(MainActivity.this, "Recording started", Toast.LENGTH_SHORT).show();
                        isRecServiceRunning = !isRecServiceRunning;
                    }
                    findViewById(R.id.main_activity_layout).setBackgroundResource(R.drawable.bg_rec_on_grad);
                    playButton.setVisibility(View.GONE);
                }
                else
                {
                    if (isRecServiceRunning) {
                        AudioRecService.stop(MainActivity.this);
                        Toast.makeText(MainActivity.this, "Recording stopped", Toast.LENGTH_SHORT).show();
                        isRecServiceRunning = !isRecServiceRunning;
                        mAxisSensorListener.notifyFaceUp();
                    }
                    findViewById(R.id.main_activity_layout).setBackgroundResource(R.drawable.bg_rec_off_grad);
                    playButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {     // for debug purposes only, do not use to control program flow
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(mPlayerOnCompletion);
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void saveState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(KEY_REC_BUTTON, isRecServiceRunning);
        editor.apply();
    }

    private void restoreState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        boolean isChecked = isRecServiceRunning = sharedPref.getBoolean(KEY_REC_BUTTON, false);
        recServiceButton.setChecked(isChecked);
    }

    @Override
    protected void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(mShakeSensorListener);
        mSensorManager.unregisterListener(mAxisSensorListener);

        saveState();
        if (playButton.isChecked()) {
            playButton.setChecked(!playButton.isChecked());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mShakeSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        mSensorManager.registerListener(mAxisSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mAxisSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);
    }
}
