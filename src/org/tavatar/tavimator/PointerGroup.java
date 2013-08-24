package org.tavatar.tavimator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class PointerGroup {
	private static String TAG = "Pointer";
	private List<Pointer> pointerPool;
	private List<Pointer> pointers;
	private VelocityTracker mVelocityTracker;

	private ViewConfiguration config;
	private float displayDensity;

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
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
		this.displayDensity = displayMetrics.density;
		Log.d(TAG, "double tap timeout: " + config.getDoubleTapTimeout() + "; long tap timeout: " + config.getLongPressTimeout());
	}
	
	public int size() {
		return pointers.size();
	}
	
	public Pointer get(int i) {
		return pointers.get(i);
	}
	
	public Pointer first() {
		return get(0);
	}
	
	public Pointer second() {
		return get(1);
	}
	
	public Pointer last() {
		return get(size()-1);
	}
	
	/**
	 * Converts touch data into an angular velocity vector, in degrees per unit
	 * time. The angular velocity vector will be in camera-local coordinates.
	 * Data is provided in screen coordinates (pixels starting at zero in the
	 * top left corner). dx, Unit time is arbitrary; it can be one second, one
	 * frame, or whatever
	 * 
	 * @param angularVelocity The computed angular velocity vector is stored here (length 3 + 1 extra for zoom factor)
	 * @param type perFrame means the angular velocity will be in pixels per frame, based on the delta from the last frame.
	 *  perSecond means the velocity will be in pixels per second, based on the comptuted finger velocity
	 */
	public void getAngularVelocity(float[] angularVelocity, Pointer.VelocityType type) {
		switch (pointers.size()) {
		case 0:
			angularVelocity[0] = 0.0f;
			angularVelocity[1] = 0.0f;
			angularVelocity[2] = 0.0f;
			angularVelocity[3] = 1.0f;
			return;
		case 1:
			angularVelocity[0] = pointers.get(0).vy(type) / displayDensity / 2f;
			angularVelocity[1] = pointers.get(0).vx(type) / displayDensity / 2f;
			angularVelocity[2] = 0.0f;
			angularVelocity[3] = 1.0f;
			return;
		}

		// mathematically, r and v should both be divided by
		// 2, but it ends up canceling out (radius is half
		// the distance, velocity is the average of the two
		// measurements, one of which is inverted)
		Pointer p1 = first();
		Pointer p2 = second();
		int rx = p2.x-p1.x; // radius vector, x component
		int ry = p2.y-p1.y; // radius vector, y component
		int r2 = rx*rx + ry*ry; // radius squared
		float vx = p2.vx(type)-p1.vx(type); // velocity vector, x component
		float vy = p2.vy(type)-p1.vy(type); // velocity vector, y component
		float projection = (rx*vx + ry*vy) / r2; // projection of v onto r, as a fraction of r
		float vxPerp = vx - projection * rx; // tangential velocity vector, x component
		float vyPerp = vy - projection * ry; // tangential velocity vector, y component

		angularVelocity[0] = (p1.vy(type)+p2.vy(type)) / displayDensity / 4f;
		angularVelocity[1] = (p1.vx(type)+p2.vx(type)) / displayDensity / 4f;
		angularVelocity[2] = (float) ((vxPerp*ry - vyPerp*rx) / r2 * 180/Math.PI);
		angularVelocity[3] = (float)Math.exp(projection);
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