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
	
	private float[] gravity  = null;
	private float[] magnetic = null;
	private float[] inclination = new float [16];
	private float[] deviceOrientation = new float [16];
	private float[] orientationOffset = null;
	private float[] orientation = new float [16];
	
	/**
	 * True if receiving sensor data
	 */
	private boolean sensing = false;
	
	/**
	 * True if camera is following the device orientation
	 */
	private boolean tracking = false;
	
	public Gyroscope(Context context) {
		Matrix.setIdentityM(orientation, 0);
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}
 
	protected void onResume() {
        setSensing(true);
    }

    protected void onPause() {
        setSensing(false);
    }
    
    public void setSensing(boolean sensing) {
    	if (this.sensing == sensing) return;
    	this.sensing = sensing;
    	
    	if (sensing) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);    		
    	} else {
    		sensorManager.unregisterListener(this);
    		gravity = null;
    		magnetic = null;
    	}
    }

    public boolean getSensing() {
    	return sensing;
    }

    public void setTracking(boolean tracking) {
    	if (this.tracking == tracking) return;
    	this.tracking = tracking;

    	// calculating the offset is done in updateOrientation. setting it to null triggers the calculation
   		orientationOffset = null;
    }

    public boolean getTracking() {
    	return tracking;
    }

 
	public void onSensorChanged(SensorEvent evt) {
		final float smoothing = 1.0f / 4.0f;
		if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			if (gravity == null) {
				gravity = new float[3];
				gravity [0] =  evt.values[0];
				gravity [1] =  evt.values[1];
				gravity [2] =  evt.values[2];
			} else {
				gravity [0] = gravity [0] * (1.0f - smoothing) + evt.values[0] * smoothing;
				gravity [1] = gravity [1] * (1.0f - smoothing) + evt.values[1] * smoothing;
				gravity [2] = gravity [2] * (1.0f - smoothing) + evt.values[2] * smoothing;
			}
		} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if (magnetic == null) {
				magnetic = new float[3];
				magnetic [0] =  evt.values[0];
				magnetic [1] =  evt.values[1];
				magnetic [2] =  evt.values[2];
			} else {
				magnetic[0] = magnetic[0] * (1.0f - smoothing) + evt.values[0] * smoothing;
				magnetic[1] = magnetic[1] * (1.0f - smoothing) + evt.values[1] * smoothing;
				magnetic[2] = magnetic[2] * (1.0f - smoothing) + evt.values[2] * smoothing;
			}
		}

	}

	public float[] getOrientation() {
		return orientation;
	}
	
	public void updateOrientation() {
		if (gravity == null || magnetic == null) {
			Matrix.setIdentityM(orientation, 0);
			return;
		}
		SensorManager.getRotationMatrix(deviceOrientation, inclination, gravity, magnetic);

		if (tracking) {
			if (orientationOffset == null) {
				orientationOffset = new float[16];
				float[] inverseDeviceOrientation = new float[16];
				Matrix.transposeM(inverseDeviceOrientation, 0, deviceOrientation, 0);
				Matrix.multiplyMM(orientationOffset, 0, inverseDeviceOrientation, 0, orientation, 0);
			}
			Matrix.multiplyMM(orientation, 0, deviceOrientation, 0, orientationOffset, 0);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
}
