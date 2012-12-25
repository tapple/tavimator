package org.tavatar.tavimator;

import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;

public class TwoFingerTrackball {

	/**
	 * The base orientation matrix. Should contain only orientation; no scale or translation
	 */
	private float[] orientation = new float[16];

	/**
	 * The rotation that happened since the last frame
	 */
	private float[] frameRotation = new float[16];
	
	private float[] temporaryMatrix = new float[16];
	

	// Offsets for touch events	 
    private float previousX;
    private float previousY;
    
    private float density = 1.0f;
        	
	/** Retain the most recent delta for touch events. */
	// These still work without volatile, but refreshes are not guaranteed to
	// happen.
	private float deltaX;
	private float deltaY;
	
	public TwoFingerTrackball() {
		Matrix.setIdentityM(orientation, 0);
	}
	
	public void calibrateForDisplay(Display aDisplay) {
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		aDisplay.getMetrics(displayMetrics);
		this.density = displayMetrics.density;
	}
	
	public void setLookAt(
			float eyeX, float eyeY, float eyeZ, 
			float lookX, float lookY, float lookZ,
			float upX, float upY, float upZ) {
		Matrix.setLookAtM(orientation, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
		Matrix.translateM(orientation, 0, eyeX, eyeY, eyeZ);
		System.out.println("orientation:");
		AnimateRenderer.printMatrix(orientation);
	}

	/**
	 * make a copy if you intend to share the result
	 * @return
	 */
	public synchronized float[] getOrientation() {
		Matrix.setIdentityM(frameRotation, 0);
		Matrix.rotateM(frameRotation, 0, deltaX, 0.0f, 1.0f, 0.0f);
		Matrix.rotateM(frameRotation, 0, deltaY, 1.0f, 0.0f, 0.0f);
		deltaX = 0.0f;
		deltaY = 0.0f;

		// Multiply the current rotation by the accumulated rotation, and then
		// set the accumulated rotation to the result.
		Matrix.multiplyMM(temporaryMatrix, 0, frameRotation, 0, orientation, 0);
		System.arraycopy(temporaryMatrix, 0, orientation, 0, 16);
		return orientation;
	}
	
	public synchronized void setOrientation(float[] anOrientationMatrix) {
		System.arraycopy(anOrientationMatrix, 0, orientation, 0, 16);
	}
	
	public synchronized boolean onTouchEvent(MotionEvent event) 
	{
		if (event == null) return false;

		float x = event.getX();
		float y = event.getY();
		
		if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			deltaX += (x - previousX) / density / 2f;
			deltaY += (y - previousY) / density / 2f;
		}	
		
		previousX = x;
		previousY = y;
		
		return true;
	}


}
