package org.tavatar.tavimator;

import java.io.IOException;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class AnimationView extends GLSurfaceView 
{
	private AnimationRenderer renderer;
	private AnimationTouchDispatcher touchDispatcher;
	
	private static final String TAG = "AnimationView";

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

	/**
	 * true if the current or previous gesture was a two finger one
	 */
	private boolean isTwoFingerGesture = false;

	private int mTouchSlop;
	private int mMinimumVelocity;
	private int mMaximumVelocity;

	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #activePointer1Id}.
	 */
	private static final int INVALID_POINTER = -1;
	
	public AnimationView(Context context) {
		super(context);
		initialize();
	}

	public AnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	private String printList(String[] strings) {
		StringBuilder ans = new StringBuilder();
		ans.append('[');
		if (strings.length >= 1) ans.append(strings[0]);
		for (int i = 1; i < strings.length; i++) {
			ans.append(", ");
			ans.append(strings[i]);
		}
		ans.append(']');
		return ans.toString();
	}
	
	private void initialize() {
		try {
			AssetManager assets = getContext().getAssets();
			Log.d("Test", printList(assets.list("data")));
			//assets.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BVH bvh = new BVH();
		AssetManager assets = getContext().getAssets();
		try {
			BVHNode node = bvh.animRead(assets.open("data/SLFemale.bvh"), assets.open(Animation.LIMITS_FILE), false);
			bvh.dumpNodes(node, "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Check if the system supports OpenGL ES 2.0.
		final ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

		if (supportsEs2) {
			// Request an OpenGL ES 2.0 compatible context.
			setEGLContextClientVersion(2);

			// Set the renderer to our demo renderer, defined below.
			renderer = new AnimationRenderer(getContext());
			setRenderer(renderer);
		} else {
			// This is where you could create an OpenGL ES 1.x compatible
			// renderer if you wanted to support both ES 1 and ES 2.
			return;
		}

		touchDispatcher = new AnimationTouchDispatcher(getContext());
		touchDispatcher.setOneFingerDragHandler(renderer.getCamera().getTrackball().getOneFingerDragHandler());
		touchDispatcher.setTwoFingerDragHandler(renderer.getCamera().getTrackball().getTwoFingerDragHandler());

		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}
	
	public AnimationRenderer getRenderer() {
		return renderer;
	}

	@Override
	public void onResume() 
	{
		// The activity must call the GL surface view's onResume() on activity onResume().
		super.onResume();
		renderer.onResume();
	}

	@Override
	public void onPause() 
	{
		// The activity must call the GL surface view's onPause() on activity onPause().
		super.onPause();
		renderer.onPause();
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
				if (isTwoFingerGesture) {
					touchDispatcher.onTwoFingerMoveCancel();
				} else {
					touchDispatcher.onOneFingerMoveCancel();
				}
			}
			wasFlinging = false;
			isTwoFingerGesture = false;

			// Remember where the motion event started
			activePointer1Id = ev.getPointerId(0);
			saveLastMotion(ev);
			touchDispatcher.onFingerDown(mLastMotionX1, mLastMotionY1);
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
				final int index2 = ev.findPointerIndex(activePointer2Id);
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
					touchDispatcher.onTapCancel();
				}
			}
			if (mIsBeingDragged) {
				mLastMotionX1 = x1;
				mLastMotionY1 = y1;
				mLastMotionX2 = x2;
				mLastMotionY2 = y2;
				if (!isTwoFingerGesture && activePointer2Id == INVALID_POINTER) {
					touchDispatcher.onOneFingerMove(x1, y1, deltaX1, deltaY1);
				} else {
					isTwoFingerGesture = true;
					touchDispatcher.onTwoFingerMove(x1, y1, deltaX1, deltaY1, x2, y2, deltaX2, deltaY2);
				}
			}
			break;
		case MotionEvent.ACTION_UP: // the last finger was lifted
			if (mIsBeingDragged) { // end of one finger drag
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				float velocityX = velocityTracker
						.getXVelocity(activePointer1Id);
				float velocityY = velocityTracker
						.getYVelocity(activePointer1Id);

				if (Matrix.length(velocityX, velocityY, 0.0f) > mMinimumVelocity) {
					touchDispatcher.onOneFingerFling(mLastMotionX1, mLastMotionY1, velocityX, velocityY);
					wasFlinging = true;
				}

				endDrag();
			} else if (!isTwoFingerGesture) { // end of tap
				touchDispatcher.onTap(mLastMotionX1, mLastMotionY1);
				endDrag();
			}
			activePointer1Id = INVALID_POINTER;
			break;
		case MotionEvent.ACTION_CANCEL:
			if (!mIsBeingDragged) {
				touchDispatcher.onTapCancel();
			} else if (isTwoFingerGesture) {
				touchDispatcher.onTwoFingerMoveCancel();
			} else {
				touchDispatcher.onOneFingerMoveCancel();
			}
			endDrag();
			activePointer1Id = INVALID_POINTER;
			activePointer2Id = INVALID_POINTER;
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			final int pointerId = ev.getPointerId(pointerIndex);
			if (activePointer2Id == INVALID_POINTER) {
				if (mIsBeingDragged) { // switch active fingers during a one finger gesture
					activePointer1Id = pointerId;
				} else { 
					activePointer2Id = pointerId;
					if (isTwoFingerGesture) { // put the second finger back down after lifting it to complete a two finger gesture
						touchDispatcher.onTwoFingerMoveCancel();
					}
				}
				saveLastMotion(ev);
			}
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			final int pointerId = ev.getPointerId(pointerIndex);
			final int newPointerId = unusedPointerId(ev);
			if (newPointerId == INVALID_POINTER) { // exactly two fingers were on the screen and active, and one of them was lifted
				if (!mIsBeingDragged) { // two fingers were placed down without moving, and now one is being lifted
					if (pointerId == activePointer1Id) {
						activePointer1Id = activePointer2Id;
						touchDispatcher.onTapCancel();
						int index = ev.findPointerIndex(activePointer1Id);
						touchDispatcher.onFingerDown((int)ev.getX(index), (int)ev.getY(index));
					}
					activePointer2Id = INVALID_POINTER;
					mVelocityTracker.clear();
				} else if(isTwoFingerGesture) { // one of the two fingers in a two finger drag was lifted; end gesture
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
						touchDispatcher.onTwoFingerFling(
								mLastMotionX1, mLastMotionY1, vx1, vy1,
								mLastMotionX2, mLastMotionY2, vx2, vy2);
						wasFlinging = true;
					}

					endDrag();
					if (pointerId == activePointer1Id) {
						activePointer1Id = activePointer2Id;
					}
					activePointer2Id = INVALID_POINTER;
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

	private void endDrag() {
		mIsBeingDragged = false;
		recycleVelocityTracker();
	}
}
