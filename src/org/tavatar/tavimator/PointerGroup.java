package org.tavatar.tavimator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class PointerGroup {
	private static String TAG = "Pointer";
	private List<Pointer> pointerPool;
	private List<Pointer> pointers;
	private VelocityTracker mVelocityTracker;
	private ViewConfiguration config;

	/**
	 * True if the user is currently dragging this ScrollView around. This is
	 * not the same as 'is being flinged', which can be checked by
	 * mScroller.isFinished() (flinging begins when the user lifts his finger).
	 */
	private boolean mIsBeingDragged = false;
	
	public PointerGroup(Context context) {
		pointerPool = new ArrayList<Pointer>();
		pointers = new ArrayList<Pointer>();
		config = ViewConfiguration.get(context);
		Log.d(TAG, "double tap timeout: " + config.getDoubleTapTimeout() + "; long tap timeout: " + config.getLongPressTimeout());
	}
	
	public int size() {
		return pointers.size();
	}
	
	public Pointer get(int i) {
		return pointers.get(i);
	}
	
	public boolean isDragging() {
		return mIsBeingDragged;
	}
	
	public void addMovement(MotionEvent ev) {
		final int action = ev.getAction();
		final int actionMask = action & MotionEvent.ACTION_MASK;
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		initVelocityTrackerIfNotExists();
		mVelocityTracker.addMovement(ev);

		switch (actionMask) {
		case MotionEvent.ACTION_DOWN: {
			pointerPool.addAll(pointers);
			pointers.clear(); // just in case some up events never cleared or got lost
			pointerDown(ev, 0);
			break;
		}
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_UP: // the last finger was lifted
			pointers.get(0).up();
			break;
		case MotionEvent.ACTION_CANCEL:
			for (Pointer pointer : pointers) {
				pointer.up();
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			pointerDown(ev, pointerIndex);
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			pointerUp(ev, pointerIndex);
			break;
		}
		
		for (Pointer pointer : pointers) {
			pointer.update(ev);
		}
	}
	
	public void clearUpPointers() {
		for (Iterator<Pointer> i = pointers.iterator(); i.hasNext();) {
			Pointer pointer = i.next();
			if (pointer.up) {
				i.remove();
				pointerPool.add(pointer);
			}
		}
	}
	
	private int pointerIndexById(int pointerId) {
		for (int i = 0; i < pointers.size(); i++) {
			if (pointers.get(i).id == pointerId) return i;
		}
		throw new NoSuchElementException();
	}

	private void pointerDown(MotionEvent ev, int index) {
		Pointer pointer;
		if (pointerPool.isEmpty()) {
			pointer = new Pointer(this);
		} else {
			pointer = pointerPool.remove(pointerPool.size() - 1);
		}
		pointer.down(ev, index);
		pointers.add(pointer);
	}
	
	private void pointerUp(MotionEvent ev, int index) {
		int pointerId = ev.getPointerId(index);
		for (Iterator<Pointer> i = pointers.iterator(); i.hasNext();) {
			Pointer pointer = i.next();
			if (pointer.id == pointerId) {
				pointer.up();
				return;
			}
		}
	}
	
	public boolean shouldStartDrag() {
		// note that Pointer.shouldStartDrag has a side effect of making dx smaller. Cannot short circuit
		int slopiness = config.getScaledTouchSlop();
		boolean shouldStart = false;
		for (Pointer pointer : pointers) {
			if (pointer.shouldStartDrag(slopiness)) shouldStart = true;
		}
		return shouldStart;
	}
	
	public boolean shouldFling() {
		int minFlingVelocity = config.getScaledMinimumFlingVelocity();
		for (Pointer pointer : pointers) {
			if (pointer.shouldFling(minFlingVelocity)) return true;
		}
		return false;
	}
	
	public void computeCurrentVelocity() {
		mVelocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());
		for (Pointer pointer : pointers) {
			pointer.update(mVelocityTracker);
		}
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

	public void startDrag() {
		mIsBeingDragged = true;
	}

	public  void endDrag() {
		mIsBeingDragged = false;
		recycleVelocityTracker();
	}
}