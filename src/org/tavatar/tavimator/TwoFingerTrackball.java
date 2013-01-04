package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Scroller;

public class TwoFingerTrackball {

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

	/**
	 * The rotation that happened since the last frame
	 */
	private float[] frameRotation = new float[16];

	private float[] temporaryMatrix = new float[16];
	private float[] temporaryVector = new float[4];

	private float[] scrollAxis = new float[4];
	private float[] flingAxis = new float[4];

	private int prevFlingX;
	private int prevFlingY;

	private float mDensity = 1.0f;

	private Context mContext;

	public TwoFingerTrackball(Context context) {
		mContext = context;

		Matrix.setIdentityM(orientation, 0);
		Matrix.setIdentityM(cameraToTrackball, 0);

		mScroller = new Scroller(mContext);

		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		this.mDensity = displayMetrics.density;
	}

	public WindowManager getWindowManager() {
		return ((WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE));
	}

	public void setLookAt(float eyeX, float eyeY, float eyeZ, float lookX,
			float lookY, float lookZ, float upX, float upY, float upZ) {
		Matrix.setLookAtM(orientation, 0, eyeX, eyeY, eyeZ, lookX, lookY,
				lookZ, upX, upY, upZ);
		Matrix.translateM(orientation, 0, eyeX, eyeY, eyeZ);
	}

	/**
	 * make a copy if you intend to share the result
	 * 
	 * @return
	 */
	public float[] getOrientation() {
		return orientation;
	}

	public float[] getCameraToTrackballOrientation() {
		return cameraToTrackball;
	}

	public void updateOrientation() {
		mScroller.computeScrollOffset();
		int x = mScroller.getCurrX();
		int y = mScroller.getCurrY();

		if (prevFlingX == x && prevFlingY == y)
			return;

		rotateAboutCameraAxis(x - prevFlingX, flingAxis);
		prevFlingX = x;
		prevFlingY = y;
	}

	public void setOrientation(float[] anOrientationMatrix) {
		System.arraycopy(anOrientationMatrix, 0, orientation, 0, 16);
	}

	public AnimationOneFingerDragHandler getOneFingerDragHandler() {
		class OneFingerDragHandler implements AnimationOneFingerDragHandler {
			@Override
			public String shortToolName() {
				return mContext.getResources().getString(R.string.short_tool_name_orbit_camera);
			}
	
			@Override
			public String toolName() {
				return mContext.getResources().getString(R.string.one_finger_tool_name_orbit_camera);
			}
	
			@Override
			public void onOneFingerMove(int x, int y, int dx, int dy) {
				scrollBy(dx, dy);
			}
		
			@Override
			public void onOneFingerFling(int x, int y, float vx, float vy) {
				fling(vx, vy);
			}
		
			@Override
			public void onCancel() {
				mScroller.forceFinished(true);
			}
		}
		return new OneFingerDragHandler();
	}

	public AnimationTwoFingerDragHandler getTwoFingerDragHandler() {
		class TwoFingerDragHandler implements AnimationTwoFingerDragHandler {
			@Override
			public String shortToolName() {
				return mContext.getResources().getString(R.string.short_tool_name_orbit_camera);
			}
	
			@Override
			public String toolName() {
				return mContext.getResources().getString(R.string.two_finger_tool_name_orbit_camera);
			}
	
			@Override
			public void onTwoFingerMove(int x1, int y1, int dx1, int dy1, int x2, int y2, int dx2, int dy2) {
				scrollBy(dx1, dy1);
			}

			@Override
			public void onTwoFingerFling(int x1, int y1, float vx1, float vy1, int x2, int y2, float vx2, float vy2) {
				fling(vx1, vy1);
			}

			@Override
			public void onCancel() {
				mScroller.forceFinished(true);
			}
		}
		return new TwoFingerDragHandler();
	}

	private void scrollBy(float deltaX, float deltaY) {
		// Log.v(TAG, "scrollBy(" + deltaX + ", " + deltaY + ")");
		if (deltaX == 0.0f && deltaY == 0.0f)
			return;
		temporaryVector[0] = deltaY / mDensity / 2f;
		temporaryVector[1] = deltaX / mDensity / 2f;
		temporaryVector[2] = 0.0f;
		temporaryVector[3] = 1.0f;
		Matrix.multiplyMV(scrollAxis, 0, cameraToTrackball, 0, temporaryVector,
				0);
		rotateAboutCameraAxis(
				Matrix.length(temporaryVector[0], temporaryVector[1], 0.0f),
				scrollAxis);
	}

	private void rotateAboutCameraAxis(float angle, float[] axis) {
		Matrix.setIdentityM(frameRotation, 0);
		Matrix.rotateM(frameRotation, 0, angle, axis[0], axis[1], axis[2]);

		// Multiply the current rotation by the accumulated rotation, and then
		// set the accumulated rotation to the result.
		Matrix.multiplyMM(temporaryMatrix, 0, frameRotation, 0, orientation, 0);
		setOrientation(temporaryMatrix);
	}

	private void fling(float velocityX, float velocityY) {
		float angularVelocityX = velocityX / mDensity / 2f;
		float angularVelocityY = velocityY / mDensity / 2f;

		temporaryVector[0] = angularVelocityY;
		temporaryVector[1] = angularVelocityX;
		temporaryVector[2] = 0.0f;
		temporaryVector[3] = 1.0f;
		Matrix.multiplyMV(flingAxis, 0, cameraToTrackball, 0, temporaryVector,
				0);

		prevFlingX = 0;
		prevFlingY = 0;

		mScroller.fling(0, 0,
				(int) Matrix.length(angularVelocityX, angularVelocityY, 0.0f),
				0, 0, Integer.MAX_VALUE, 0, 0);
	}

}
