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
	 * The base orientation matrix. Should contain only orientation; no scale or translation
	 */
	private float[] orientation = new float[16];

	/**
	 * A transform from camera coordinates to my local coordinates. Used for touch processing
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
		debugLabel = (TextView)((Activity)context).findViewById(R.id.debugLabel);

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
		return ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE));
    }

	public void setLookAt(
			float eyeX, float eyeY, float eyeZ, 
			float lookX, float lookY, float lookZ,
			float upX, float upY, float upZ) {
		Matrix.setLookAtM(orientation, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
		Matrix.translateM(orientation, 0, eyeX, eyeY, eyeZ);
	}

	/**
	 * make a copy if you intend to share the result
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

        if (prevFlingX == x && prevFlingY == y) return;

        rotateAboutCameraAxis(x - prevFlingX, flingAxis);
        prevFlingX = x;
        prevFlingY = y;
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

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #activePointer1Id}.
     */
    private static final int INVALID_POINTER = -1;

    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final int actionMask = action & MotionEvent.ACTION_MASK;
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        
        if (activePointer1Id == INVALID_POINTER && actionMask != MotionEvent.ACTION_DOWN) return true;
        
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        switch (actionMask) {
            case MotionEvent.ACTION_DOWN: {
            	debugLabel.setText("Down");
                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }

                // Remember where the motion event started
                activePointer1Id = ev.getPointerId(0);
                saveLastMotion(ev);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(activePointer1Id);
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
                	if (activePointer2Id == INVALID_POINTER) {
                		debugLabel.setText("move 1 finger");
                	} else {
                		debugLabel.setText("move 2 finger");
                	}
                    // Scroll to follow the motion event
                    mLastMotionX = x;
                    mLastMotionY = y;
                    scrollBy(deltaX, deltaY);
                }
                break;
            case MotionEvent.ACTION_UP: // the last finger was lifted
                if (mIsBeingDragged) {
                	debugLabel.setText("Fling 1 finger");
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    float velocityX = velocityTracker.getXVelocity(activePointer1Id);
                    float velocityY = velocityTracker.getYVelocity(activePointer1Id);

                    if (Matrix.length(velocityX, velocityY, 0.0f) > mMinimumVelocity) {
                    	fling(velocityX, velocityY);
                    }
  
                    endDrag();
                } else if (activePointer1Id != INVALID_POINTER) {
                	debugLabel.setText("Tap");
                	endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            	debugLabel.setText("Cancel");
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
                if (newPointerId == INVALID_POINTER) { // one of the last two fingers was lifted
                	if (!mIsBeingDragged) {
                		if (pointerId == activePointer1Id) {
                			activePointer1Id = activePointer2Id;
                		}
                		activePointer2Id = INVALID_POINTER;
                		mVelocityTracker.clear();
                	} else if (activePointer2Id == INVALID_POINTER) {
                		Log.e(TAG, "got to end 2 finger drag, but only one finger is active");
                		endDrag();
                	} else {
                    	debugLabel.setText("Fling 2 finger");
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
    		mLastMotionX = (int) ev.getX(index);
    		mLastMotionY = (int) ev.getY(index);
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
    	//Log.v(TAG, "scrollBy(" + deltaX + ", " + deltaY + ")");
    	if (deltaX == 0.0f && deltaY == 0.0f) return;
    	temporaryVector[0] = deltaY / mDensity / 2f;
    	temporaryVector[1] = deltaX / mDensity / 2f;
    	temporaryVector[2] = 0.0f;
    	temporaryVector[3] = 1.0f;
    	Matrix.multiplyMV(scrollAxis, 0, cameraToTrackball, 0, temporaryVector, 0);
		rotateAboutCameraAxis(Matrix.length(temporaryVector[0], temporaryVector[1], 0.0f), scrollAxis);
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
        activePointer2Id = INVALID_POINTER;

        recycleVelocityTracker();
    }

    private void fling(float velocityX, float velocityY) {
	    float angularVelocityX = velocityX / mDensity / 2f;
	    float angularVelocityY = velocityY / mDensity / 2f;
	    
	    temporaryVector[0] = angularVelocityY;
	    temporaryVector[1] = angularVelocityX;
	    temporaryVector[2] = 0.0f;
	    temporaryVector[3] = 1.0f;
	    Matrix.multiplyMV(flingAxis, 0, cameraToTrackball, 0, temporaryVector, 0);
		
		prevFlingX = 0;
		prevFlingY = 0;
	
		mScroller.fling(0, 0, (int) Matrix.length(angularVelocityX, angularVelocityY, 0.0f), 0, 0, Integer.MAX_VALUE, 0, 0);
	}
	
}
