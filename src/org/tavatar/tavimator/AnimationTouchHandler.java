package org.tavatar.tavimator;

public interface AnimationTouchHandler {
	/**
	 * 
	 * @return a String to be displayed on the button
	 */
	public String shortToolName();

	/**
	 * 
	 * @return a String to be displayed in the list of drag handlers
	 */
	public String toolName();

	/**
	 * the gesture was canceled
	 */
	public void onCancel();
}