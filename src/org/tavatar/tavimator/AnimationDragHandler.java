package org.tavatar.tavimator;

public interface AnimationDragHandler extends AnimationTouchHandler {
	/**
	 * user moved her finger to x, y. She moved her finger dx, dy pixels since the last call
	 * 
	 * @param x
	 * @param y
	 * @param dx
	 * @param dy
	 */
	public void onMove(PointerGroup pointers);

	/**
	 * user lifted her finger at x, y, while it was moving at vx, vy pixels per second
	 * 
	 * @param x
	 * @param y
	 * @param vx
	 * @param vy
	 */
	public void onFling(PointerGroup pointers);
}