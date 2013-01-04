package org.tavatar.tavimator;

public interface AnimationTwoFingerDragHandler extends AnimationTouchHandler {
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
	public void onTwoFingerMove(int x1, int y1, int dx1, int dy1, int x2, int y2, int dx2, int dy2);

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
	public void onTwoFingerFling(int x1, int y1, float vx1, float vy1, int x2, int y2, float vx2, float vy2);
}