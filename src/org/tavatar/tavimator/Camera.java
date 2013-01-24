package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;

/**
 * This class manages the camera's view matrix based on user input. It does not manage the perspective matrix
 * @author tapple
 *
 * Camera is on a rig as follows:
 * origin: The point the camera rotates around
 * offset: a vector from the origin to the ideal camera, in camera-local coordinates
 * transitionRail: a vector the camera animates along when changing from one origin to another. It runs from the ideal camera to the actual camera
 * 
 * baseOrientation: the orientation before the device orientation is taken into account. This is animated inertially in response to swipes
 * deviceOrientation: the current orientation of the phone
 * baseDeviceOrientation: the orientation of the phone at which the camera is at its base orientation
 */

public class Camera {
	private static final String TAG = "Camera";

	/**
	 * The computed view matrix
	 */
	private float[] viewMatrix = new float[16];

	private float[] temporaryMatrix = new float[16];
	
	/**
	 * The point about which the camera rotates
	 */
	float originX;
	float originY;
	float originZ;
	
	/**
	 * The a vector from the old origin + distance to the new origin + distance.
	 * the camera slides along this vector when moving between origins
	 */
	float[] transitionRail = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
	float transitionProgress = 0.0f;
	
	/**
	 * Trackball that keeps track of orientation set by touch
	 */
	TwoFingerTrackball trackball;
	
	/**
	 * Gyroscope that keeps track of orientation set by device orientation
	 */
	Gyroscope gyroscope;
	
	public Camera(Context context) {
		trackball = new TwoFingerTrackball(context);
		gyroscope = new Gyroscope(context);
		// Start out facing and rotating about the origin, pointing in the default OpenGL direction of z, with y being up and x right
		initializeCamera(
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f);
	}
	
	public void initializeCamera(
			float eyeX, float eyeY, float eyeZ, 
			float lookX, float lookY, float lookZ,
			float upX, float upY, float upZ) {
		
		setOrigin(lookX, lookY, lookZ);
		transitionRail[0]  = 0.0f;
		transitionRail[1]  = 0.0f;
		transitionRail[2]  = 0.0f;
		transitionRail[3]  = 1.0f;
		transitionProgress = 0.0f;
		trackball.setLookAt(eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
		updateViewMatrix();
	}
	
	public void setOrigin(float lookX, float lookY, float lookZ) {
		this.originX = -lookX;
		this.originY = -lookY;
		this.originZ = -lookZ;
	}
	
	public void setOrigin(float[] look) {
		setOrigin(look[0], look[1], look[2]);
	}
	
	public void moveToOrigin(float[] newOrigin) {
		setOrigin(newOrigin);
		Matrix.multiplyMV(transitionRail, 0, viewMatrix, 0, newOrigin, 0);
		if (transitionRail[2] < 0) {
			// new origin is in front of the camera. Stay within the current camera plane
			trackball.setDistance(-transitionRail[2]);
			transitionRail[2] = 0.0f;
		} else {
			// new origin is behind the camera. Move the camera 10 units behind the new origin
			trackball.setDistance(10.0f);
			transitionRail[2] += 10.0f;
		}
		transitionProgress = 1.0f;
	}
	
	public void onResume() {
		gyroscope.onResume();
	}

	public void onPause() {
		gyroscope.onPause();
	}	

	public void updateViewMatrix() {
		trackball.updateOrientation();
		gyroscope.updateOrientation();
		Matrix.transposeM(trackball.getCameraToTrackballOrientation(), 0, gyroscope.getOrientation(), 0);

		Matrix.setIdentityM(viewMatrix, 0);
		Matrix.translateM(viewMatrix, 0, 
				transitionProgress * transitionRail[0],
				transitionProgress * transitionRail[1],
				transitionProgress * transitionRail[2] - trackball.getDistance());
		Matrix.multiplyMM(temporaryMatrix, 0, viewMatrix, 0, gyroscope.getOrientation(), 0);
		Matrix.multiplyMM(viewMatrix, 0, temporaryMatrix, 0, trackball.getOrientation(), 0);
		Matrix.translateM(viewMatrix, 0, originX, originY, originZ);
	}
	
	// returns the view matrix. Copy it if you intend to pass it around; this one might change
	public float[] getViewMatrix() {
		return viewMatrix;
	}
	
	public TwoFingerTrackball getTrackball() {
		return trackball;
	}
	
	public Gyroscope getGyroscope() {
		return gyroscope;
	}
}
