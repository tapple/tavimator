package org.tavatar.tavimator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

public class Gyroscope implements SensorEventListener {
	
	private static final String TAG = "Gyroscope";
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;

    private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private Sensor gyrometer;
	
	private float[] gravity  = null;
	private float[] magnetic = null;
	private float[] angular  = new float[3];
	
    // rotation matrix from gyro data
    private float[] prevGyroMatrix = new float[16];
    private float[] gyroMatrix = new float[16];
 
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
 
	private float timestamp;

	private float[] accelMagOrientation = new float[16];
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
	
	public Gyroscope(Context context) {
		Matrix.setIdentityM(orientation, 0);
		Matrix.setIdentityM(gyroMatrix, 0);
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyrometer     = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // uncomment to disable gyro
//        	deviceOrientation = accelMagOrientation;
	}
	
	public boolean hasGyroscope() {
		return deviceOrientation != accelMagOrientation;
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
            if (hasGyroscope()) {
            	sensorManager.registerListener(this, gyrometer, SensorManager.SENSOR_DELAY_GAME);
            }
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
				System.arraycopy(evt.values, 0, gravity, 0, 3);
			} else if (hasGyroscope()) { // don't smooth if doing sensor fusion
				System.arraycopy(evt.values, 0, gravity, 0, 3);
			} else { // smooth if device has no gyroscope
				gravity [0] = gravity [0] * (1.0f - smoothing) + evt.values[0] * smoothing;
				gravity [1] = gravity [1] * (1.0f - smoothing) + evt.values[1] * smoothing;
				gravity [2] = gravity [2] * (1.0f - smoothing) + evt.values[2] * smoothing;
			}
		} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if (magnetic == null) {
				magnetic = new float[3];
				System.arraycopy(evt.values, 0, magnetic, 0, 3);
			} else if (hasGyroscope()) { // don't smooth if doing sensor fusion
				System.arraycopy(evt.values, 0, magnetic, 0, 3);
			} else { // smooth if device has no gyroscope
				magnetic[0] = magnetic[0] * (1.0f - smoothing) + evt.values[0] * smoothing;
				magnetic[1] = magnetic[1] * (1.0f - smoothing) + evt.values[1] * smoothing;
				magnetic[2] = magnetic[2] * (1.0f - smoothing) + evt.values[2] * smoothing;
			}
		} else if (evt.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			Log.d(TAG, "gyroEvent");
	        gyroFunction(evt);
		}

	}

	// This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
	private void getRotationVectorFromGyro(float[] gyroValues,
			float[] deltaRotationVector,
			float timeFactor)
	{
		// Calculate the angular speed of the sample
		float angularSpeed = Matrix.length(gyroValues[0], gyroValues[1], gyroValues[2]);

		if(angularSpeed > EPSILON) {
			Matrix.rotateM(gyroMatrix, 0, (float) (- angularSpeed * timeFactor * 180f / Math.PI * 2.0f), gyroValues[0], gyroValues[1], gyroValues[2]);
		}
	}

	// This function performs the integration of the gyroscope data.
	// It writes the gyroscope based orientation into gyroOrientation.
	public void gyroFunction(SensorEvent event) {
		// don't start until first accelerometer/magnetometer orientation has been acquired
		if (deviceOrientation == null)
			return;
		
//		if (true) return;

		final float dT = (event.timestamp - timestamp) * NS2S;
		if(0.0f < dT && dT < 0.5f) {
			float angularSpeed = Matrix.length(event.values[0], event.values[1], event.values[2]);
			if(angularSpeed > EPSILON) {
				float[] temp = new float[16];
				Matrix.transposeM(temp, 0, deviceOrientation, 0);
				Matrix.rotateM(temp, 0, (float) (angularSpeed * dT * 180 / Math.PI),
						event.values[0], event.values[1], event.values[2]);
				Matrix.transposeM(deviceOrientation, 0, temp, 0);
			}
		}

		// measurement done, save current time for next interval
		timestamp = event.timestamp;
	}
    
	public float[] getOrientation() {
		return orientation;
	}
	
	public void fuseSensorData() {
		final float gyroCoeff = 0.98f;
		final float accelCoeff = 1.0f - gyroCoeff;

		Rotation gyroAngles  = new Rotation();
		Rotation accelAngles = new Rotation();
		Rotation fusedAngles = new Rotation();

		gyroAngles = Math3D.toEulerAngles(gyroAngles, deviceOrientation, BVHOrderType.BVH_XYZ);
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

		Matrix.setIdentityM(deviceOrientation, 0);
		Matrix.rotateM(deviceOrientation, 0, fusedAngles.x, 1, 0, 0);
		Matrix.rotateM(deviceOrientation, 0, fusedAngles.y, 0, 1, 0);
		Matrix.rotateM(deviceOrientation, 0, fusedAngles.z, 0, 0, 1);
        
//        System.arraycopy(deviceOrientation, 0, accelMagOrientation, 0, 16);
//        Matrix.transposeM(deviceOrientation, 0, accelMagOrientation, 0);
	}
	
	public void updateOrientation() {
		if (gravity == null || magnetic == null) {
			Matrix.setIdentityM(orientation, 0);
			return;
		}
		
		SensorManager.getRotationMatrix(accelMagOrientation, null, gravity, magnetic);
		if (deviceOrientation == null) {
			deviceOrientation = new float[16];
			System.arraycopy(accelMagOrientation, 0, deviceOrientation, 0, 16);				
		}
		
		if (hasGyroscope()) {
			fuseSensorData();
		}

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
