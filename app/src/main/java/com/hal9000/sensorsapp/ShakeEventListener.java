package com.hal9000.sensorsapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Listener that detects shake gesture.
 */
public class ShakeEventListener implements SensorEventListener {


    /** Minimum movement force to consider. */
    private static final int MIN_MOVEMENT = 15; // default 10

    /**
     * Minimum times in a shake gesture that the direction of movement needs to
     * change.
     */
    private static final int MIN_DIRECTION_CHANGE = 6;

    /** Maximum pause between movements. */
    private static final int MAX_PAUSE_BETWEEN_DIRECTION_CHANGE = 500;  // in ms

    /** Minimum allowed time for shake gesture. */
    private static final int MIN_TOTAL_DURATION_OF_SHAKE = 500;  // in ms

    /** Minimum time between shake gestures. */
    private static final int MIN_TIME_BETWEEN_SHAKES = 1500;  // in ms

    /** Time when the gesture started. */
    private long mFirstDirectionChangeTime = 0;

    /** Time when the last movement started. */
    private long mLastDirectionChangeTime;

    /** How many movements are considered so far. */
    private int mDirectionChangeCount = 0;

    /** Time when the last shake occured. */
    private long mLastShakeTime = 0;

    /** The last x position. */
    private float lastX = 0;

    /** The last y position. */
    private float lastY = 0;

    /** The last z position. */
    private float lastZ = 0;

    /** OnShakeListener that is called when shake is detected. */
    private OnShakeListener mShakeListener;

    /**
     * Interface for shake gesture.
     */
    public interface OnShakeListener {

        /**
         * Called when shake gesture is detected.
         */
        void onShake();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        mShakeListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        // get sensor data
        float x = se.values[0];
        float y = se.values[1];
        float z = se.values[2];

        // calculate movement : SHAKE EVENT
        float totalMovement = Math.abs(x + y + z - lastX - lastY - lastZ);

        if (totalMovement > MIN_MOVEMENT) {

            // get time
            long now = System.currentTimeMillis();

            // store first movement time
            if (mFirstDirectionChangeTime == 0) {
                mFirstDirectionChangeTime = now;
                mLastDirectionChangeTime = now;
            }// if (mFirstDirectionChangeTime == 0)

            // check if the last movement was not long ago
            long lastChangeWasAgo = now - mLastDirectionChangeTime;
            if (lastChangeWasAgo < MAX_PAUSE_BETWEEN_DIRECTION_CHANGE) {

                // store movement data
                mLastDirectionChangeTime = now;
                mDirectionChangeCount++;

                // store last sensor data
                lastX = x;
                lastY = y;
                lastZ = z;

                // check how many movements are so far
                if (mDirectionChangeCount >= MIN_DIRECTION_CHANGE) {

                    // check total duration
                    long totalDuration = now - mFirstDirectionChangeTime;
                    long timeSinceLastShake = now - mLastShakeTime;
                    if (totalDuration >= MIN_TOTAL_DURATION_OF_SHAKE && timeSinceLastShake >= MIN_TIME_BETWEEN_SHAKES) {
                        mShakeListener.onShake();
                        mLastShakeTime = now;
                        resetShakeParameters();
                    }// if (totalDuration >= MIN_TOTAL_DURATION_OF_SHAKE)
                }// if (mDirectionChangeCount >= MIN_DIRECTION_CHANGE)

            }// if (lastChangeWasAgo < MAX_PAUSE_BETWEEN_DIRECTION_CHANGE)
            else {
                resetShakeParameters();
            }// else
        }// if (totalMovement > MIN_MOVEMENT)
    }

    /**
     * Resets the shake parameters to their default values.
     */
    private void resetShakeParameters() {
        mFirstDirectionChangeTime = 0;
        mDirectionChangeCount = 0;
        mLastDirectionChangeTime = 0;

        lastX = 0;
        lastY = 0;
        lastZ = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
