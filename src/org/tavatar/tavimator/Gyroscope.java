package org.tavatar.tavimator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;

public class Gyroscope implements SensorEventListener {
	
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	
	private float[] gravity = new float[3];
	private float[] magnetic  = new float[3];
	private float[] inclination = new float [16];
	private float[] orientation = new float [16];
	
	public Gyroscope(Context context) {
		Matrix.setIdentityM(orientation, 0);
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}
 
	protected void onResume() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        sensorManager.unregisterListener(this);
    }

 
	public void onSensorChanged(SensorEvent evt) {
		final float smoothing = 1.0f / 4.0f;
		if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			gravity [0] = gravity [0] * (1.0f - smoothing) + evt.values[0] * smoothing;
			gravity [1] = gravity [1] * (1.0f - smoothing) + evt.values[1] * smoothing;
			gravity [2] = gravity [2] * (1.0f - smoothing) + evt.values[2] * smoothing;
		} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			magnetic[0] = magnetic[0] * (1.0f - smoothing) + evt.values[0] * smoothing;
			magnetic[1] = magnetic[1] * (1.0f - smoothing) + evt.values[1] * smoothing;
			magnetic[2] = magnetic[2] * (1.0f - smoothing) + evt.values[2] * smoothing;
		}

	}

	public float[] getOrientation() {
		return orientation;
	}
	
	public void updateOrientation() {
		SensorManager.getRotationMatrix(orientation, inclination, gravity, magnetic);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
}
