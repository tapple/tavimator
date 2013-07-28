package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class Pointer {
	private static String TAG = "Pointer";
	private PointerGroup group;
	//	slopiness = configuration.getScaledTouchSlop();
	//	mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
	//	mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

	public int id;
	public boolean up;

	public int x;
	public int y;

	public int prevX;
	public int prevY;
	
	public int startX;
	public int startY;

	public int dx;
	public int dy;

	public float velocityX;
	public float velocityY;

	public Pointer(PointerGroup group) {
		this.group = group;
	}

	public boolean isDragging() {
		return group.isDragging();
	}

 	public void down(MotionEvent ev, int index) {
 		up = false;
		id = ev.getPointerId(index);
		startX = prevX = (int) ev.getX(index);
		startY = prevY = (int) ev.getY(index);
	}
 	
 	public void up() {
 		up = true;
 	}

	public void update(MotionEvent event) {
		final int index = event.findPointerIndex(id);
		if (index < 0) return;
		
		if (isDragging()) {
			prevX = x;
			prevY = y;
		}

		x = (int) event.getX(index);
		y = (int) event.getY(index);

		dx = x - prevX;
		dy = y - prevY;
	}

	public void update(VelocityTracker velocityTracker) {
		velocityX = velocityTracker.getXVelocity(id);
		velocityY = velocityTracker.getYVelocity(id);
	}

	public boolean shouldStartDrag(int slopiness) {
		boolean shouldStart = false;
		if (dx > slopiness) {
			shouldStart = true;
			dx -= slopiness;
		}
		if (dx < -slopiness) {
			shouldStart = true;
			dx += slopiness;
		}
		if (dy > slopiness) {
			shouldStart = true;
			dy -= slopiness;
		}
		if (dy < -slopiness) {
			shouldStart = true;
			dy += slopiness;
		}
		return shouldStart;
	}

	public boolean shouldFling(float minFlingVelocity) {
		return  velocityX > minFlingVelocity ||
				velocityY > minFlingVelocity;
	}
}