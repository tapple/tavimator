package org.tavatar.tavimator;

import java.util.List;

import android.content.Context;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Scroller;

/**
 * Trackball is a type of joint who's orientation is calculated from user input. It is always the sole occupant of it's JointStore
 * 
 * @author tapple
 *
 */
public class Trackball extends Joint {
	private static final String TAG = "Trackball";
	static final float ZOOM_FACTOR = 100.0f;
	
	protected Scroller mScroller;
	
	protected float[] scrollVelocity = new float[4];
	protected float[] scrollAxis = new float[4];
	protected float[] flingAxis = new float[4];
	private float zoomRate = 1.0f;
	protected int prevFlingX;
	protected int prevFlingY;
	private long prevZoomTime;

	private Joint camera;
	private float[] cameraToTrackball = new float[16];
	
	public Trackball(Context context) {
		super();
		setUniStore(new JointStore(context));
		getStore().useEulerAngles = false;
		mScroller = new Scroller(context);
	}

	public static String arrayToString(float[] array, int start, int length) {
		String ans = "[";
		int end = start + length;
		for (int i = start; i < end; i++) {			
			ans += array[i];
			ans += i==end-1 ? "]" : ", ";
		}
		return ans;
	}

	public static String arrayToString(float[] array) {
		return arrayToString(array, 0, array.length);}

	public WindowManager getWindowManager() {
		return ((WindowManager) getStore().context.getSystemService(Context.WINDOW_SERVICE));
	}

	public synchronized void setLookAt(float eyeX, float eyeY,
			float eyeZ, float lookX, float lookY, float lookZ, float upX,
			float upY, float upZ) {
		Quaternion.setIdentity(getStore().value, getRotationIndex());
/* not really lookAt. Should remove this method
		Matrix.setLookAtM(orientation, 0, eyeX, eyeY, eyeZ, lookX, lookY,
						lookZ, upX, upY, upZ);
				Matrix.translateM(orientation, 0, eyeX, eyeY, eyeZ);
*/
			}

	public void setZoomRate(float zoomRate) {
		this.zoomRate = zoomRate;
		prevZoomTime = SystemClock.uptimeMillis();
	}

	public void basicUpdateOrientation() {
		mScroller.computeScrollOffset();
		int x = mScroller.getCurrX();
		int y = mScroller.getCurrY();
		long time = SystemClock.uptimeMillis();
	
		if (prevFlingX == x && prevFlingY == y && zoomRate == 1.0f)
			return;
	
		basicRotateAbout(flingAxis, 0, x - prevFlingX, getStore().tempUpdateMatrix1, getStore().tempUpdateMatrix2);
		float zoomDelta = 1.0f;
		zoomDelta *= (float)Math.exp((y - prevFlingY) / ZOOM_FACTOR);
		zoomDelta *= (float)Math.pow(zoomRate, (time - prevZoomTime) / 1000.0);
		scaleBy(zoomDelta);
		prevFlingX = x;
		prevFlingY = y;
		prevZoomTime = time;
	}

	public float[] getCameraToTrackballOrientation() {
		return cameraToTrackball;
	}

	public void fling(float[] angularVelocity) {
/*
		//		Log.d(TAG, "fling(" + arrayToString(angularVelocity) + ");");
		Matrix.multiplyMV(flingAxis, 0, cameraToTrackball, 0, angularVelocity, 0);
	
		prevFlingX = 0;
		prevFlingY = 0;
	
		mScroller.fling(0, 0,
				(int) Matrix.length(angularVelocity[0], angularVelocity[1], angularVelocity[2]),
				(int) (Math.log(angularVelocity[3]) * ZOOM_FACTOR),
				0, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
*/
	}

	public void stopFling() {
/*
		//		Log.d(TAG, "stopFling();");
		mScroller.forceFinished(true);
*/
	}
}
