package org.tavatar.tavimator;

public interface AnimationTapHandler extends AnimationTouchHandler {
	/**
	 * user touched the screen at x, y. If he lifts there, onTap will be called
	 * later with the same coordinates. otherwise, onTapCancel will be called,
	 * followed by onOneFingerMove or onTwoFingerMove
	 * 
	 * @param x
	 * @param y
	 */
	public void onFingerDown(PointerGroup pointers);

	/**
	 * user touched then released the screen at x, y.
	 * 
	 * @param x
	 * @param y
	 */
	public void onTap(PointerGroup pointers);
}