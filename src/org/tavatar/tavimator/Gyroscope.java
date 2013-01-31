package org.tavatar.tavimator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class Gyroscope implements SensorEventListener {
	
	private static final String TAG = "Gyroscope";
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float MS2S = 1.0f / 1000.0f;

    private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private Sensor gyrometer;
	
	private float[] gravity  = null;
	private float[] magnetic = null;
	private float[] angular  = new float[4];
	
	private int stabilityCountDown = 0;
	
    // rotation matrix from gyro data
//    private float[] prevGyroMatrix = new float[16];
//    private float[] gyroMatrix = new float[16];
 
    // orientation angles from gyro matrix
	private float timestamp;

	private float[] accelMagOrientation = new float[16];
    private float[] gyroOrientation = null;
    private float[] inverseGyroOrientation = new float[16];
    
	private float[] deviceOrientation = null;
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
	private boolean hasGyroscope = false;
	
	public Gyroscope(Context context) {
		Matrix.setIdentityM(orientation, 0);
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyrometer     = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        hasGyroscope = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).size() > 0;
        
        if (!hasGyroscope) {
        	Toast.makeText(context, R.string.no_gyroscope, Toast.LENGTH_LONG).show();
        }
	}
	
	protected void onResume() {
        basicSetSensing(sensing);
    }

    protected void onPause() {
        basicSetSensing(false);
    }
    
    public void setSensing(boolean sensing) {
    	if (this.sensing == sensing) return;
    	this.sensing = sensing;
    	basicSetSensing(sensing);
    }
    
    private void basicSetSensing(boolean sensing) {
    	if (sensing) {
    		stabilityCountDown = 5;
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
            if (hasGyroscope) {
            	sensorManager.registerListener(this, gyrometer, SensorManager.SENSOR_DELAY_GAME);
            }
    	} else {
    		sensorManager.unregisterListener(this);
    	}
		gravity = null;
		magnetic = null;
		gyroOrientation = null;
		deviceOrientation = null;
		orientationOffset = null;
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
			if (stabilityCountDown > 0) {
				gravity = null;
			} else if (gravity == null) {
				gravity = new float[3];
				System.arraycopy(evt.values, 0, gravity, 0, 3);
			} else if (hasGyroscope) { // don't smooth if doing sensor fusion
				System.arraycopy(evt.values, 0, gravity, 0, 3);
			} else { // smooth if device has no gyroscope
				gravity [0] = gravity [0] * (1.0f - smoothing) + evt.values[0] * smoothing;
				gravity [1] = gravity [1] * (1.0f - smoothing) + evt.values[1] * smoothing;
				gravity [2] = gravity [2] * (1.0f - smoothing) + evt.values[2] * smoothing;
			}
		} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if (stabilityCountDown > 0) {
				stabilityCountDown--;
				magnetic = null;
			} else if (magnetic == null) {
				Log.d(TAG, "resetting magnetic");
				magnetic = new float[3];
				System.arraycopy(evt.values, 0, magnetic, 0, 3);
			} else if (hasGyroscope) { // don't smooth if doing sensor fusion
				System.arraycopy(evt.values, 0, magnetic, 0, 3);
			} else { // smooth if device has no gyroscope
				magnetic[0] = magnetic[0] * (1.0f - smoothing) + evt.values[0] * smoothing;
				magnetic[1] = magnetic[1] * (1.0f - smoothing) + evt.values[1] * smoothing;
				magnetic[2] = magnetic[2] * (1.0f - smoothing) + evt.values[2] * smoothing;
			}
		} else if (evt.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
	        gyroFunction(evt);
		}

	}

	// This function performs the integration of the gyroscope data.
	// It writes the gyroscope based orientation into gyroOrientation.
	public void gyroFunction(SensorEvent event) {
		// don't start until first accelerometer/magnetometer orientation has been acquired
		if (gyroOrientation == null)
			return;
		
//		if (true) return;

		final float dT = (event.timestamp - timestamp) * NS2S;
		if(0.0f < dT && dT < 0.5f) {
			System.arraycopy(event.values, 0, angular, 0, 3);
			angular[3] = 1.0f;
			float angularSpeed = Matrix.length(event.values[0], event.values[1], event.values[2]);
			if(angularSpeed > EPSILON) {
				Matrix.transposeM(inverseGyroOrientation, 0, gyroOrientation, 0);
				Matrix.rotateM(inverseGyroOrientation, 0, (float) (angularSpeed * dT * 180 / Math.PI),
						event.values[0], event.values[1], event.values[2]);
				Matrix.transposeM(gyroOrientation, 0, inverseGyroOrientation, 0);
			}
		}

		// measurement done, save current time for next interval
		timestamp = event.timestamp;
	}
    
	public float[] getOrientation() {
		return orientation;
	}
	
	public float[] getAngularVelocity() {
		float dT = SystemClock.uptimeMillis() * MS2S - timestamp * NS2S;
		// if last gyro event was over half a second ago, device is not moving
		if (dT <= 0.0f || 0.5f < dT) {
			angular[0] = 0.0f;
			angular[1] = 0.0f;
			angular[2] = 0.0f;
			angular[3] = 1.0f;
		}
		angular[0] *= 180/Math.PI;
		angular[1] *= 180/Math.PI;
		angular[2] *= 180/Math.PI;
		return angular;
	}
	
	public void fuseSensorData() {
		final float gyroCoeff = 0.99f;
		final float accelCoeff = 1.0f - gyroCoeff;

		Rotation gyroAngles  = new Rotation();
		Rotation accelAngles = new Rotation();
		Rotation fusedAngles = new Rotation();

		gyroAngles = Math3D.toEulerAngles(gyroAngles, gyroOrientation, BVHOrderType.BVH_XYZ);
		accelAngles = Math3D.toEulerAngles(accelAngles, accelMagOrientation, BVHOrderType.BVH_XYZ);

//*
		if ( gyroAngles.x < -90 && accelAngles.x > 0.0f)  gyroAngles.x += 360;
		if (accelAngles.x < -90 &&  gyroAngles.x > 0.0f) accelAngles.x += 360;
		if ( gyroAngles.y < -90 && accelAngles.y > 0.0f)  gyroAngles.y += 360;
		if (accelAngles.y < -90 &&  gyroAngles.y > 0.0f) accelAngles.y += 360;
		if ( gyroAngles.z < -90 && accelAngles.z > 0.0f)  gyroAngles.z += 360;
		if (accelAngles.z < -90 &&  gyroAngles.z > 0.0f) accelAngles.z += 360;

		fusedAngles.x = gyroCoeff * gyroAngles.x + accelCoeff * accelAngles.x;
		fusedAngles.y = gyroCoeff * gyroAngles.y + accelCoeff * accelAngles.y;
		fusedAngles.z = gyroCoeff * gyroAngles.z + accelCoeff * accelAngles.z;

		if (fusedAngles.x > 180) fusedAngles.x -= 360;
		if (fusedAngles.y > 180) fusedAngles.y -= 360;
		if (fusedAngles.z > 180) fusedAngles.z -= 360;


//*/        fusedAngles = gyroAngles;

		Matrix.setIdentityM(gyroOrientation, 0);
		Matrix.rotateM(gyroOrientation, 0, fusedAngles.x, 1, 0, 0);
		Matrix.rotateM(gyroOrientation, 0, fusedAngles.y, 0, 1, 0);
		Matrix.rotateM(gyroOrientation, 0, fusedAngles.z, 0, 0, 1);
        
//        System.arraycopy(deviceOrientation, 0, accelMagOrientation, 0, 16);
//        Matrix.transposeM(deviceOrientation, 0, accelMagOrientation, 0);
	}
	
	public void updateOrientation() {
		if (sensing && gravity != null && magnetic != null) {
			SensorManager.getRotationMatrix(accelMagOrientation, null, gravity, magnetic);
			if (gyroOrientation == null) {
				Log.d(TAG, "resetting gyro");
				gyroOrientation = new float[16];
				System.arraycopy(accelMagOrientation, 0, gyroOrientation, 0, 16);				
			}
			
			if (deviceOrientation == null) {
				deviceOrientation = new float[16];
			}
			
			if (hasGyroscope) {
				fuseSensorData();
				System.arraycopy(gyroOrientation, 0, deviceOrientation, 0, 16);
			} else {
				System.arraycopy(accelMagOrientation, 0, deviceOrientation, 0, 16);
			}
		}

		if (tracking && deviceOrientation != null) {
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
