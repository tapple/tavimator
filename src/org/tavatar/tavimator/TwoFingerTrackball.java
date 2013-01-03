package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Scroller;
import android.widget.TextView;
import android.app.Activity;

public class TwoFingerTrackball {

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

	/**
	 * A text view at the top of the screen for printing debugging messages
	 */
	private TextView debugLabel;

	public TwoFingerTrackball(Context context) {
		mContext = context;
		debugLabel = (TextView) ((Activity) context)
				.findViewById(R.id.debugLabel);

		Matrix.setIdentityM(orientation, 0);
		Matrix.setIdentityM(cameraToTrackball, 0);

		mScroller = new Scroller(mContext);
		final ViewConfiguration configuration = ViewConfiguration.get(mContext);
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

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

	// //////////////////////////////////////////////////////////////////////////////////////////
	// ////////////////////////// BELOW ADAPTED FROM SCROLLVIEW
	// /////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////

	private static final String TAG = "TwoFingerTrackball";

	private Scroller mScroller;

	/**
	 * Position of the last motion event.
	 */
	private int mLastMotionX1;
	private int mLastMotionY1;
	private int mLastMotionX2;
	private int mLastMotionY2;

	/**
	 * True if the user is currently dragging this ScrollView around. This is
	 * not the same as 'is being flinged', which can be checked by
	 * mScroller.isFinished() (flinging begins when the user lifts his finger).
	 */
	private boolean mIsBeingDragged = false;

	/**
	 * Determines speed during touch scrolling
	 */
	private VelocityTracker mVelocityTracker;

	/**
	 * ID of the active pointer. This is used to retain consistency during
	 * drags/flings if multiple pointers are used.
	 */
	private int activePointer1Id = INVALID_POINTER;

	/**
	 * ID of the second finger pointer during two finger gestures.
	 */
	private int activePointer2Id = INVALID_POINTER;
	
	/**
	 * true if the previous gesture ended in a fling
	 */
	private boolean wasFlinging = false;

	private int mTouchSlop;
	private int mMinimumVelocity;
	private int mMaximumVelocity;

	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #activePointer1Id}.
	 */
	private static final int INVALID_POINTER = -1;
	
	private void debug(String message) {
		Log.d(TAG, message);
		debugLabel.setText(message);
	}

	/**
	 * user touched the screen at x, y. If he lifts there, onTap will be called
	 * later with the same coordinates. otherwise, onTapCancel will be called,
	 * followed by onOneFingerMove or onTwoFingerMove
	 * 
	 * @param x
	 * @param y
	 */
	public void onFingerDown(int x, int y) {
		debug("onFingerDown(" + x + ", " + y + ")");
	}

	/**
	 * user touched then released the screen at x, y.
	 * 
	 * @param x
	 * @param y
	 */
	public void onTap(int x, int y) {
		debug("onTap(" + x + ", " + y + ")");
	}

	/**
	 * user touched the screen, but then decided to move her finger
	 */
	public void onTapCancel() {
		debug("onTapCancel()");
	}

	/**
	 * user moved her finger to x, y. She moved her finger dx, dy pixels since the last call
	 * 
	 * @param x
	 * @param y
	 * @param dx
	 * @param dy
	 */
	public void onOneFingerMove(int x, int y, int dx, int dy) {
		debug("onOneFingerMove(" + x + ", " + y + ", " + dx + ", " + dy + ")");
		scrollBy(dx, dy);
	}

	/**
	 * user lifted her finger at x, y, while it was moving at vx, vy pixels per second
	 * 
	 * @param x
	 * @param y
	 * @param vx
	 * @param vy
	 */
	public void onOneFingerFling(int x, int y, float vx, float vy) {
		debug("onOneFingerFling(" + x + ", " + y + ", " + vx + ", " + vy + ")");
		fling(vx, vy);
	}

	/**
	 * one finger move gesture was canceled
	 */
	public void onOneFingerMoveCancel() {
		debug("onOneFingerMoveCancel()");
		mScroller.forceFinished(true);
	}

	/**
	 * user moved her two fingers to x1, y1 and x2, y2. She moved her fingers dx1, dy1 and dx2, dy2 pixels since the last call
	 * 
	 * @param x1
	 * @param y1
	 * @param dx1
	 * @param dy1
	 * @param x2
	 * @param y2
	 * @param dx2
	 * @param dy2
	 */
	public void onTwoFingerMove(int x1, int y1, int dx1, int dy1, int x2, int y2, int dx2, int dy2) {
		debug("onTwoFingerMove(" + 
				x1 + ", " + y1 + ", " + dx1 + ", " + dy1 + ", "  + 
				x2 + ", " + y2 + ", " + dx2 + ", " + dy2 + ")");
		scrollBy(dx1, dy1);
	}

	/**
	 * user lifted her two fingers at x1, y1 and x2, y2, while they were moving at vx1, vy1 and vx2, vy2 pixels per second
	 * 
	 * @param x1
	 * @param y1
	 * @param vx1
	 * @param vy1
	 * @param x2
	 * @param y2
	 * @param vx2
	 * @param vy2
	 */
	public void onTwoFingerFling(int x1, int y1, float vx1, float vy1, int x2, int y2, float vx2, float vy2) {
		debug("onTwoFingerFling(" + 
				x1 + ", " + y1 + ", " + vx1 + ", " + vy1 + ", "  + 
				x2 + ", " + y2 + ", " + vx2 + ", " + vy2 + ")");
		fling(vx1, vy1);
	}

	/**
	 * two finger move gesture was canceled
	 */
	public void onTwoFingerMoveCancel() {
		debug("onTwoFingerMoveCancel()");
		mScroller.forceFinished(true);
	}

	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int actionMask = action & MotionEvent.ACTION_MASK;
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

		if (activePointer1Id == INVALID_POINTER
				&& actionMask != MotionEvent.ACTION_DOWN)
			return true;

		initVelocityTrackerIfNotExists();
		mVelocityTracker.addMovement(ev);

		switch (actionMask) {
		case MotionEvent.ACTION_DOWN: {
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			if (wasFlinging) {
				if (activePointer2Id == INVALID_POINTER) {
					onOneFingerMoveCancel();
				} else {
					onTwoFingerMoveCancel();
				}
			}
			wasFlinging = false;
			activePointer2Id = INVALID_POINTER;

			// Remember where the motion event started
			activePointer1Id = ev.getPointerId(0);
			saveLastMotion(ev);
			onFingerDown(mLastMotionX1, mLastMotionY1);
			break;
		}
		case MotionEvent.ACTION_MOVE:
			final int index1 = ev.findPointerIndex(activePointer1Id);
			final int x1 = (int) ev.getX(index1);
			final int y1 = (int) ev.getY(index1);
			int deltaX1 = x1 - mLastMotionX1;
			int deltaY1 = y1 - mLastMotionY1;

			int x2 = 0;
			int y2 = 0;
			int deltaX2 = 0;
			int deltaY2 = 0;

			if (activePointer2Id != INVALID_POINTER) {
				final int index2 = ev.findPointerIndex(activePointer1Id);
				x2 = (int) ev.getX(index2);
				y2 = (int) ev.getY(index2);
				deltaX2 = x2 - mLastMotionX2;
				deltaY2 = y2 - mLastMotionY2;
			}
			if (!mIsBeingDragged) {
				if (deltaX1 > mTouchSlop) {
					mIsBeingDragged = true;
					deltaX1 -= mTouchSlop;
				}
				if (deltaX1 < -mTouchSlop) {
					mIsBeingDragged = true;
					deltaX1 += mTouchSlop;
				}
				if (deltaY1 > mTouchSlop) {
					mIsBeingDragged = true;
					deltaY1 -= mTouchSlop;
				}
				if (deltaY1 < -mTouchSlop) {
					mIsBeingDragged = true;
					deltaY1 += mTouchSlop;
				}
				if (deltaX2 > mTouchSlop) {
					mIsBeingDragged = true;
					deltaX2 -= mTouchSlop;
				}
				if (deltaX2 < -mTouchSlop) {
					mIsBeingDragged = true;
					deltaX2 += mTouchSlop;
				}
				if (deltaY2 > mTouchSlop) {
					mIsBeingDragged = true;
					deltaY2 -= mTouchSlop;
				}
				if (deltaY2 < -mTouchSlop) {
					mIsBeingDragged = true;
					deltaY2 += mTouchSlop;
				}
				
				if (mIsBeingDragged) {
					onTapCancel();
				}
			}
			if (mIsBeingDragged) {
				mLastMotionX1 = x1;
				mLastMotionY1 = y1;
				mLastMotionX2 = x2;
				mLastMotionY2 = y2;
				if (activePointer2Id == INVALID_POINTER) {
					onOneFingerMove(x1, y1, deltaX1, deltaY1);
				} else {
					onTwoFingerMove(x1, y1, deltaX1, deltaY1, x2, y2, deltaX2, deltaY2);
				}
			}
			break;
		case MotionEvent.ACTION_UP: // the last finger was lifted
			if (mIsBeingDragged) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				float velocityX = velocityTracker
						.getXVelocity(activePointer1Id);
				float velocityY = velocityTracker
						.getYVelocity(activePointer1Id);

				if (Matrix.length(velocityX, velocityY, 0.0f) > mMinimumVelocity) {
					onOneFingerFling(mLastMotionX1, mLastMotionY1, velocityX, velocityY);
					wasFlinging = true;
				}

				endDrag();
			} else if (activePointer1Id != INVALID_POINTER) {
				onTap(mLastMotionX1, mLastMotionY1);
				endDrag();
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (!mIsBeingDragged) {
				onTapCancel();
			} else if (activePointer2Id == INVALID_POINTER) {
				onOneFingerMoveCancel();
			} else {
				onTwoFingerMoveCancel();
			}
			endDrag();
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			final int pointerId = ev.getPointerId(pointerIndex);
			if (activePointer2Id == INVALID_POINTER) {
				if (mIsBeingDragged) {
					activePointer1Id = pointerId;
				} else {
					activePointer2Id = pointerId;
				}
				saveLastMotion(ev);
			}
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			final int pointerId = ev.getPointerId(pointerIndex);
			final int newPointerId = unusedPointerId(ev);
			if (newPointerId == INVALID_POINTER) { // one of the last two
													// fingers was lifted
				if (!mIsBeingDragged) {
					if (pointerId == activePointer1Id) {
						activePointer1Id = activePointer2Id;
						onTapCancel();
						int index = ev.findPointerIndex(activePointer1Id);
						onFingerDown((int)ev.getX(index), (int)ev.getY(index));
					}
					activePointer2Id = INVALID_POINTER;
					mVelocityTracker.clear();
				} else if (activePointer2Id == INVALID_POINTER) {
					Log.e(TAG, "got to end 2 finger drag, but only one finger is active");
					endDrag();
				} else {
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
					float vx1 = velocityTracker.getXVelocity(activePointer1Id);
					float vy1 = velocityTracker.getYVelocity(activePointer1Id);
					float vx2 = velocityTracker.getXVelocity(activePointer2Id);
					float vy2 = velocityTracker.getYVelocity(activePointer2Id);

					if (
							vx1 > mMinimumVelocity ||
							vy1 > mMinimumVelocity ||
							vx2 > mMinimumVelocity ||
							vy2 > mMinimumVelocity) {
						onTwoFingerFling(
								mLastMotionX1, mLastMotionY1, vx1, vy1,
								mLastMotionX2, mLastMotionY2, vx2, vy2);
						wasFlinging = true;
					}

					endDrag();
				}
			} else { // 3 or more fingers were present, and one was lifted
				if (pointerId == activePointer1Id) {
					activePointer1Id = newPointerId;
					mVelocityTracker.clear();
				} else if (pointerId == activePointer2Id) {
					activePointer2Id = newPointerId;
					mVelocityTracker.clear();
				}
			}
			saveLastMotion(ev);
			break;
		}
		return true;
	}

	private void saveLastMotion(MotionEvent ev) {
		int index;
		if (activePointer1Id != INVALID_POINTER) {
			index = ev.findPointerIndex(activePointer1Id);
			mLastMotionX1 = (int) ev.getX(index);
			mLastMotionY1 = (int) ev.getY(index);
		}
		if (activePointer2Id != INVALID_POINTER) {
			index = ev.findPointerIndex(activePointer2Id);
			mLastMotionX2 = (int) ev.getX(index);
			mLastMotionY2 = (int) ev.getY(index);
		}
	}

	private int unusedPointerId(MotionEvent ev) {
		for (int i = 0; i < ev.getPointerCount(); i++) {
			int id = ev.getPointerId(i);
			if (id != activePointer1Id && id != activePointer2Id) {
				return id;
			}
		}
		return INVALID_POINTER;
	}

	private void initVelocityTrackerIfNotExists() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
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

	private void endDrag() {
		mIsBeingDragged = false;
		activePointer1Id = INVALID_POINTER;

		recycleVelocityTracker();
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
