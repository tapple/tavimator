package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class Pointer {
	public ViewConfiguration config;
	//	config.getScaledTouchSlop() = configuration.getScaledTouchSlop();
	//	mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
	//	mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();


	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #id}.
	 */
	public static final int INVALID_POINTER = -1;

	public int id = INVALID_POINTER;
	public boolean isDragging = false;

	public int x;
	public int y;

	public int prevX;
	public int prevY;

	public int dx;
	public int dy;

	public float velocityX;
	public float velocityY;

	public Pointer(Context context) {
		config = ViewConfiguration.get(context);
	}

	public boolean isValid() {
		return id != INVALID_POINTER;
	}

	public boolean notValid() {
		return id == INVALID_POINTER;
	}

	public void invalidate() {
		id = INVALID_POINTER;
	}

	public void update(MotionEvent event) {
		update(event, false);
	}

	public void update(MotionEvent event, boolean pointerDown) {
		if (notValid()) return;

		if (isDragging) {
			prevX = x;
			prevY = y;
		}

		final int index = event.findPointerIndex(id);
		x = (int) event.getX(index);
		y = (int) event.getY(index);

		if (pointerDown) {
			prevX = x;
			prevY = y;
		}

		dx = x - prevX;
		dy = y - prevY;
	}

	public void update(VelocityTracker velocityTracker) {
		if (notValid()) return;

		velocityX = velocityTracker.getXVelocity(id);
		velocityY = velocityTracker.getYVelocity(id);
	}

	public boolean shouldStartDrag() {
		if (notValid()) return false;

		boolean shouldStart = false;
		if (dx > config.getScaledTouchSlop()) {
			shouldStart = true;
			dx -= config.getScaledTouchSlop();
		}
		if (dx < -config.getScaledTouchSlop()) {
			shouldStart = true;
			dx += config.getScaledTouchSlop();
		}
		if (dy > config.getScaledTouchSlop()) {
			shouldStart = true;
			dy -= config.getScaledTouchSlop();
		}
		if (dy < -config.getScaledTouchSlop()) {
			shouldStart = true;
			dy += config.getScaledTouchSlop();
		}
		return shouldStart;
	}

	public boolean shouldFling() {
		if (notValid()) return false;

		return  velocityX > config.getScaledMinimumFlingVelocity() ||
				velocityY > config.getScaledMinimumFlingVelocity();
	}

	public float maximumFlingVelocity() {
		return config.getScaledMaximumFlingVelocity();
	}
}