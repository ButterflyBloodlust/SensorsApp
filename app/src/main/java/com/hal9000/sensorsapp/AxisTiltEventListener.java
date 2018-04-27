package com.hal9000.sensorsapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AxisTiltEventListener implements SensorEventListener {

    // Gravity rotational data
    private float mRotationMatrix[];
    // Magnetic rotational data
    //private float magnetic[]; //for magnetic rotational data
    private float mAccelerometerReading[] = new float[3];
    private float mMagnetometerReading[] = new float[3];
    private float[] mOrientationAngles = new float[3];

    // azimuth, pitch and roll
    private float azimuth;
    private float pitch;
    private float roll;

    OnTiltListener mAxisTiltListener = null;

    private long mFirstConditionPassedTime = 0;
    private boolean mIsFaceDown = false;
    private static final int MIN_TIME_FACE_DOWN = 1500;  // in ms
    private static final float MIN_Z_AXIS_DEGREES = 155.0f;

    public interface OnTiltListener {
        void onFaceDown();
        //void onSensorChange(String data); // for debugging / data display purposes (e.g. "azimuth, pitch, roll")
    }

    public void setOnTiltListener(OnTiltListener listener) {
        mAxisTiltListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerReading = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerReading = event.values.clone();
                break;
        }

        if (mMagnetometerReading != null && mAccelerometerReading != null) {
            mRotationMatrix = new float[9];
            //magnetic = new float[9];

            SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);

            //float[] outGravity = new float[9];
            //SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
            //SensorManager.getOrientation(outGravity, mOrientationAngles);

            mOrientationAngles = SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

            azimuth = (float) Math.toDegrees(mOrientationAngles[0]);    // x
            pitch = (float) Math.toDegrees(mOrientationAngles[1]);  // y
            roll = (float) Math.toDegrees(mOrientationAngles[2]);   // z
            mMagnetometerReading = null;
            mAccelerometerReading = null;

            if (Math.abs(roll) > MIN_Z_AXIS_DEGREES && !mIsFaceDown){

                long now = System.currentTimeMillis();

                if (mFirstConditionPassedTime == 0) {
                    mFirstConditionPassedTime = now;
                }

                long relativeTime = now - mFirstConditionPassedTime;
                if (relativeTime > MIN_TIME_FACE_DOWN) {
                    mAxisTiltListener.onFaceDown();
                    mIsFaceDown = true;
                    mFirstConditionPassedTime = 0;
                }
            }
            else{
                mFirstConditionPassedTime = 0;
            }
        }
        //mAxisTiltListener.onSensorChange(String.format(Locale.ROOT, "%.2f, %.2f, %.2f", azimuth, pitch, roll));   // for debugging purposes
    }

    public void notifyFaceUp(){
        mIsFaceDown = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
