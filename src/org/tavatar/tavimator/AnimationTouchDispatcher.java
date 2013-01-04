package org.tavatar.tavimator;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

public class AnimationTouchDispatcher {

	private static final String TAG = "AnimationTouchDispatcher";
	
	private Context mContext;
	
	private AnimationTapHandler tapHandler;
	private AnimationOneFingerDragHandler oneFingerDragHandler;
	private AnimationTwoFingerDragHandler twoFingerDragHandler;

	public AnimationTouchDispatcher(Context context) {
		mContext = context;
	}

	private void debug(String message) {
		Log.d(TAG, message);
		((TextView) ((Activity) mContext)
		.findViewById(R.id.debugLabel)).setText(message);
	}

	public AnimationTapHandler getTapHandler() {
		return tapHandler;
	}

	public void setTapHandler(AnimationTapHandler tapHandler) {
		this.tapHandler = tapHandler;
	}

	public AnimationOneFingerDragHandler getOneFingerDragHandler() {
		return oneFingerDragHandler;
	}

	public void setOneFingerDragHandler(AnimationOneFingerDragHandler oneFingerDragHandler) {
		this.oneFingerDragHandler = oneFingerDragHandler;
	}

	public AnimationTwoFingerDragHandler getTwoFingerDragHandler() {
		return twoFingerDragHandler;
	}

	public void setTwoFingerDragHandler(AnimationTwoFingerDragHandler twoFingerDragHandler) {
		this.twoFingerDragHandler = twoFingerDragHandler;
	}

	public void onFingerDown(int x, int y) {
		debug("onFingerDown(" + x + ", " + y + ")");		
		if (tapHandler != null) tapHandler.onFingerDown(x, y);
	}

	public void onTap(int x, int y) {
		debug("onTap(" + x + ", " + y + ")");
		if (tapHandler != null) tapHandler.onTap(x, y);
	}

	public void onTapCancel() {
		debug("onTapCancel()");
		if (tapHandler != null) tapHandler.onCancel();
	}

	public void onOneFingerMove(int x, int y, int dx, int dy) {
		debug("onOneFingerMove(" + x + ", " + y + ", " + dx + ", " + dy + ")");
		if (oneFingerDragHandler != null) oneFingerDragHandler.onOneFingerMove(x, y, dx, dy);
	}

	public void onOneFingerFling(int x, int y, float vx, float vy) {
		debug("onOneFingerFling(" + x + ", " + y + ", " + vx + ", " + vy + ")");
		if (oneFingerDragHandler != null) oneFingerDragHandler.onOneFingerFling(x, y, vx, vy);
	}

	/**
	 * one finger move gesture was canceled
	 */
	public void onOneFingerMoveCancel() {
		debug("onOneFingerMoveCancel()");
		if (oneFingerDragHandler != null) oneFingerDragHandler.onCancel();
	}

	public void onTwoFingerMove(int x1, int y1, int dx1, int dy1, int x2, int y2, int dx2, int dy2) {
		debug("onTwoFingerMove(" + 
				x1 + ", " + y1 + ", " + dx1 + ", " + dy1 + ", "  + 
				x2 + ", " + y2 + ", " + dx2 + ", " + dy2 + ")");
		if (twoFingerDragHandler != null) twoFingerDragHandler.onTwoFingerMove(x1, y1, dx1, dy1, x2, y2, dx2, dy2);
	}

	public void onTwoFingerFling(int x1, int y1, float vx1, float vy1, int x2, int y2, float vx2, float vy2) {
		debug("onTwoFingerFling(" + 
				x1 + ", " + y1 + ", " + vx1 + ", " + vy1 + ", "  + 
				x2 + ", " + y2 + ", " + vx2 + ", " + vy2 + ")");
		if (twoFingerDragHandler != null) twoFingerDragHandler.onTwoFingerFling(x1, y1, vx1, vy1, x2, y2, vx2, vy2);
	}

	public void onTwoFingerMoveCancel() {
		debug("onTwoFingerMoveCancel()");
		if (twoFingerDragHandler != null) twoFingerDragHandler.onCancel();
	}
}