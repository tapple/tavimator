package org.tavatar.tavimator;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.Matrix;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Scroller;

public class Trackball {
	
	/**
	 * Implement this interface to have your orientation be updated with the trackball. All methods will be called from the render thread only
	 * @author tapple
	 *
	 */
	public interface UpdateListener {
		
		/**
		 * Answer an orientation as a 4x4 matrix. This will be called once from
		 * the UI thread during initialization, then subsequently only from
		 * 
		 * @return a pointer to a 4x4 matrix
		 */
		public float[] getOrientation();
		
		/**
		 * Set the orientation for this object. This method is guaranteed to be
		 * called after every call to getOrientation(), except the first time
		 * 
		 * @param orientation
		 *            The new orientation as a 4x4 matrix. This is guaranteed to
		 *            be the same array as was returned by getOrientation. It will have been modified
		 */
		public void setOrientation(float[] orientation);
		
		/**
		 * 
		 * Zoom in or out
		 * @param a zoom fraction. 1 means no change. Less than one: zoom in. Greater than 1: zoom out. Strictly positive
		 */ 
		 public void zoomBy(float fraction);
	}

	private static String TAG = "Trackball";

	private Scroller mScroller;

	/**
	 * The base orientation matrix. Should contain only orientation; no scale or
	 * translation
	 */
	private float[] orientation = new float[16];

	/**
	 * A transform from camera coordinates to my local coordinates. Used for
	 * touch processing
	 */
	private float[] cameraToTrackball = new float[16];
	private float[] gyroToTrackball = new float[16];
	private float[] trackballToGyro = new float[16];

	/**
	 * The rotation that happened since the last frame
	 */
	private float[] localFrameRotation = new float[16];
	private float[] globalFrameRotation = new float[16];
	private float[] newOrientation = new float[16];

	private float[] scrollVelocity = new float[4];
	private float[] scrollAxis = new float[4];
	private float[] flingAxis = new float[4];
	private static final float ZOOM_FACTOR = 100.0f;

	private float zoomRate = 1.0f; // per second

	private Gyroscope trackingGyroscope = null;
	private float[] gyroOffset = new float[16];
	private float[] localGyroOrientation = new float[16];
	private float[] inverseLocalGyroOrientation = new float[16];
	private boolean invertGyro = false;

	private int prevFlingX;
	private int prevFlingY;
	private long prevZoomTime;
	
	private UpdateListener listener;

	private Context mContext;

	public void dumpMatrices() {
		Log.d(TAG, "gyroToTrackball:");
		AnimationRenderer.printMatrix(gyroToTrackball);
		Log.d(TAG, "trackballToGyro:\n");
		AnimationRenderer.printMatrix(trackballToGyro);
		Log.d(TAG, "gyroOffset:\n");
		AnimationRenderer.printMatrix(gyroOffset);
		Log.d(TAG, "orientation:\n");
		AnimationRenderer.printMatrix(orientation);
	}

	public static String arrayToString(float[] array) {
		String ans = "[";
		for (int i = 0; i < array.length; i++) {			
			ans += array[i];
			ans += i==array.length-1 ? "]" : ", ";
		}
		return ans;
	}

	public void angularVelocityToRotationMatrix(float[] matrix, float[] angularVelocity) {
		Matrix.setIdentityM(matrix, 0);
		Matrix.rotateM(matrix, 0, angularVelocity[0], 1, 0, 0);
		Matrix.rotateM(matrix, 0, angularVelocity[1], 0, 1, 0);
		Matrix.rotateM(matrix, 0, angularVelocity[2], 0, 0, 1);
	}

	public Trackball(Context context) {
		mContext = context;

		Matrix.setIdentityM(orientation, 0);
		Matrix.setIdentityM(cameraToTrackball, 0);
		Matrix.setIdentityM(gyroToTrackball, 0);

		mScroller = new Scroller(mContext);
	}

	public WindowManager getWindowManager() {
		return ((WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE));
	}

	public synchronized void setLookAt(float eyeX, float eyeY, float eyeZ, float lookX,
			float lookY, float lookZ, float upX, float upY, float upZ) {
		Matrix.setLookAtM(orientation, 0, eyeX, eyeY, eyeZ, lookX, lookY,
				lookZ, upX, upY, upZ);
		Matrix.translateM(orientation, 0, eyeX, eyeY, eyeZ);
	}
	
	public void addUpdateListener(UpdateListener listener) {
		this.listener = listener;
	}

	public synchronized float[] getOrientation(float[] dest) {
		System.arraycopy(orientation, 0, dest, 0, 16);
		return dest;
	}

	public synchronized float[] getInverseOrientation(float[] dest) {
		Matrix.transposeM(dest, 0, orientation, 0);
		return dest;
	}

	public synchronized float[] setOrientation(float[] src) {
		System.arraycopy(src, 0, orientation, 0, 16);
		return src;
	}

	public synchronized void setIdentity() {
		Matrix.setIdentityM(orientation, 0);
	}

	public synchronized float[] rotateMatrix (float[] dest, float[] lhs) {
		Matrix.multiplyMM(dest, 0, lhs, 0, orientation, 0);
		return dest;
	}

	public float[] getCameraToTrackballOrientation() {
		return cameraToTrackball;
	}

	public void trackGyroscope(Gyroscope newTrackingGyroscope, boolean newInvert) {
		if (trackingGyroscope != null) {
			float[] angularVelocity = new float[4];
			System.arraycopy(trackingGyroscope.getAngularVelocity(), 0, angularVelocity, 0, 4);
			if (!invertGyro) {
				angularVelocity[0] *= -1;
				angularVelocity[1] *= -1;
				angularVelocity[2] *= -1;
			}
			fling(angularVelocity);
		}

		trackingGyroscope = newTrackingGyroscope;
		invertGyro = newInvert;

		if (trackingGyroscope != null) {
			stopFling();
		}

		updateGyroOffset();
	}

	public void setZoomRate(float zoomRate) {
		this.zoomRate = zoomRate;
		prevZoomTime = SystemClock.uptimeMillis();
	}

	private synchronized void updateGyroTracking() {
		if (trackingGyroscope == null) return;

		if (localGyroOrientation == null) Log.d(TAG, "localGyroOrientation is null");
		if (gyroToTrackball == null) Log.d(TAG, "gyroToTrackball is null");

		if (invertGyro) {
			Matrix.multiplyMM(localGyroOrientation, 0, gyroToTrackball, 0, trackingGyroscope.getInverseOrientation(), 0);
		} else {
			Matrix.multiplyMM(localGyroOrientation, 0, gyroToTrackball, 0, trackingGyroscope.getOrientation(), 0);
		}
		Matrix.multiplyMM(orientation, 0, localGyroOrientation, 0, gyroOffset, 0);
	}

	private synchronized void updateGyroOffset() {
		if (trackingGyroscope == null) return;

		Matrix.transposeM(trackballToGyro, 0, gyroToTrackball, 0);

		if (invertGyro) {
			Matrix.multiplyMM(inverseLocalGyroOrientation, 0, trackingGyroscope.getOrientation(), 0, trackballToGyro, 0);
		} else {
			Matrix.multiplyMM(inverseLocalGyroOrientation, 0, trackingGyroscope.getInverseOrientation(), 0, trackballToGyro, 0);
		}
		Matrix.multiplyMM(gyroOffset, 0, inverseLocalGyroOrientation, 0, orientation, 0);
	}

	public synchronized void basicUpdateOrientation() {
		mScroller.computeScrollOffset();
		int x = mScroller.getCurrX();
		int y = mScroller.getCurrY();
		long time = SystemClock.uptimeMillis();

		if (prevFlingX == x && prevFlingY == y && zoomRate == 1.0f)
			return;

		rotateAboutCameraAxis(x - prevFlingX, flingAxis);
		float zoomDelta = 1.0f;
		zoomDelta /= (float)Math.exp((y - prevFlingY) / ZOOM_FACTOR);
		zoomDelta /= (float)Math.pow(zoomRate, (time - prevZoomTime) / 1000.0);
		listener.zoomBy(zoomDelta);
		prevFlingX = x;
		prevFlingY = y;
		prevZoomTime = time;
	}

	public synchronized void updateOrientation() {
		updateGyroTracking();
		basicUpdateOrientation();
		updateGyroOffset();
	}

	public AnimationDragHandler getDragHandler(int nameId, int shortNameId) {
		class DragHandler extends AbsTouchHandler implements AnimationDragHandler {
			public DragHandler(Context context, int nameId, int shortNameId) {
				super(context, nameId, shortNameId);
			}

			@Override
			public void onMove(PointerGroup pointers) {
				pointers.getAngularVelocity(scrollVelocity, Pointer.VelocityType.perFrame);
				scrollBy(scrollVelocity);
			}

			@Override
			public void onFling(PointerGroup pointers) {
				pointers.getAngularVelocity(scrollVelocity, Pointer.VelocityType.perSecond);
				fling(scrollVelocity);
			}

			@Override
			public void onCancel() {
				stopFling();
			}
		}
		return new DragHandler(mContext, nameId, shortNameId);
	}

	private synchronized void scrollBy(float[] angularVelocity) {
		//		Log.d(TAG, "scrollBy(" + arrayToString(angularVelocity) + ");");
		Matrix.multiplyMV(scrollAxis, 0, cameraToTrackball, 0, angularVelocity, 0);
		rotateAboutCameraAxis(
				Matrix.length(angularVelocity[0], angularVelocity[1], angularVelocity[2]),
				scrollAxis);
		listener.zoomBy(1.0f / angularVelocity[3]);
	}

	private synchronized void rotateAboutCameraAxis(float angle, float[] axis) {
		if (angle == 0.0f) return; // axis is likely to be zero in this case as well. avoids divide by zero errors in rotateM
		Matrix.setIdentityM(globalFrameRotation, 0);
		Matrix.rotateM(globalFrameRotation, 0, angle, axis[0], axis[1], axis[2]);

		// Multiply the current rotation by the accumulated rotation, and then
		// set the accumulated rotation to the result.
		Matrix.multiplyMM(orientation, 0, globalFrameRotation, 0, orientation, 0);
		updateGyroOffset();
	}

	public void fling(float[] angularVelocity) {
		//		Log.d(TAG, "fling(" + arrayToString(angularVelocity) + ");");
		Matrix.multiplyMV(flingAxis, 0, cameraToTrackball, 0, angularVelocity, 0);

		prevFlingX = 0;
		prevFlingY = 0;

		mScroller.fling(0, 0,
				(int) Matrix.length(angularVelocity[0], angularVelocity[1], angularVelocity[2]),
				(int) (Math.log(angularVelocity[3]) * ZOOM_FACTOR), 
				0, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public void stopFling() {
		//		Log.d(TAG, "stopFling();");
		mScroller.forceFinished(true);
	}

	public float[] getGyroToTrackball() {
		return gyroToTrackball;
	}

	public void setGyroToTrackball(float[] gyroToTrackball) {
		this.gyroToTrackball = gyroToTrackball;
	}
}
