package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Scroller;

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
	

    private float mDensity = 1.0f;

	private Context mContext;
        	
	public TwoFingerTrackball() {
		Matrix.setIdentityM(orientation, 0);
	}
	
    public void setContext(Context context) {
    	mContext = context;
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
		return ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE));
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
	public float[] getOrientation() {
		return orientation;
	}
	
	public void setOrientation(float[] anOrientationMatrix) {
		System.arraycopy(anOrientationMatrix, 0, orientation, 0, 16);
	}
	
////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////// BELOW ADAPTED FROM SCROLLVIEW /////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
	
    private static final String TAG = "TwoFingerTrackball";

    private Scroller mScroller;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionX;
    private int mLastMotionY;

	
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
    private int mActivePointerId = INVALID_POINTER;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionX = (int) ev.getX();
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int x = (int) ev.getX(activePointerIndex);
                final int y = (int) ev.getY(activePointerIndex);
                int deltaX = x - mLastMotionX;
                int deltaY = y - mLastMotionY;
                if (!mIsBeingDragged) {
                	if (Math.abs(deltaX) > mTouchSlop) {
	                    mIsBeingDragged = true;
	                    if (deltaX > 0) {
	                        deltaX -= mTouchSlop;
	                    } else {
	                        deltaX += mTouchSlop;
	                    }
                	}
                	if (Math.abs(deltaY) > mTouchSlop) {
	                    mIsBeingDragged = true;
	                    if (deltaY > 0) {
	                        deltaY -= mTouchSlop;
	                    } else {
	                        deltaY += mTouchSlop;
	                    }
                	}
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    mLastMotionX = x;
                    mLastMotionY = y;
                    scrollBy(deltaX, deltaY);
                }
                break;
            case MotionEvent.ACTION_UP:
/*
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);

                    if (getChildCount() > 0) {
                        if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                            fling(-initialVelocity);
                        } else {
                            if (mScroller.springBack(mScrollX, mScrollY, 0, 0, 0,
                                    getScrollRange())) {
                                postInvalidateOnAnimation();
                            }
                        }
                    }

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(mScrollX, mScrollY, 0, 0, 0, getScrollRange())) {
                        postInvalidateOnAnimation();
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
*/
        }
        return true;
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
	    Matrix.setIdentityM(frameRotation, 0);
	    float degreesX = deltaX / mDensity / 2f;
	    float degreesY = deltaY / mDensity / 2f;
		Matrix.rotateM(frameRotation, 0, degreesX, 0.0f, 1.0f, 0.0f);
		Matrix.rotateM(frameRotation, 0, degreesY, 1.0f, 0.0f, 0.0f);
	
		// Multiply the current rotation by the accumulated rotation, and then
		// set the accumulated rotation to the result.
		Matrix.multiplyMM(temporaryMatrix, 0, frameRotation, 0, orientation, 0);
		System.arraycopy(temporaryMatrix, 0, orientation, 0, 16);
    }

}
