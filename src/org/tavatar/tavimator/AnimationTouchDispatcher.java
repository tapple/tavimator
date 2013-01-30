package org.tavatar.tavimator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class AnimationTouchDispatcher {

	private static final String TAG = "AnimationTouchDispatcher";
	
	private Context mContext;
	
	private class FingerAdapter<T> extends ArrayAdapter<T> {
		private int mFieldId;

		public FingerAdapter(Context context, int resource, int textViewResourceId) {
			super(context, resource, textViewResourceId);
			mFieldId = textViewResourceId;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
            TextView text = (TextView) view.findViewById(mFieldId);
            AnimationTouchHandler item = (AnimationTouchHandler)getItem(position);
			text.setText(item.shortToolName());
			return view;
		}
	}
	
	private AnimationTapHandler tapHandler;
	private FingerAdapter<AnimationOneFingerDragHandler> oneFingerHandlers;
	private Spinner oneFingerSpinner;
	private FingerAdapter<AnimationTwoFingerDragHandler> twoFingerHandlers;
	private Spinner twoFingerSpinner;

	public AnimationTouchDispatcher(Context context) {
		mContext = context;

		oneFingerHandlers = new FingerAdapter<AnimationOneFingerDragHandler>(context, R.layout.one_finger_spinner_item, android.R.id.text1);
		oneFingerHandlers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		oneFingerSpinner = ((Spinner) ((Activity) mContext)
				.findViewById(R.id.button_one_finger_action));
		oneFingerSpinner.setAdapter(oneFingerHandlers);

		twoFingerHandlers = new FingerAdapter<AnimationTwoFingerDragHandler>(context, R.layout.two_finger_spinner_item, android.R.id.text1);
		twoFingerHandlers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		twoFingerSpinner = ((Spinner) ((Activity) mContext)
				.findViewById(R.id.button_two_finger_action));
		twoFingerSpinner.setAdapter(twoFingerHandlers);
	}
	
	public ArrayAdapter<AnimationOneFingerDragHandler> getOneFingerHandlers() {
		return oneFingerHandlers;
	}

	public Spinner getOneFingerSpinner() {
		return oneFingerSpinner;
	}

	public ArrayAdapter<AnimationTwoFingerDragHandler> getTwoFingerHandlers() {
		return twoFingerHandlers;
	}

	public Spinner getTwoFingerSpinner() {
		return twoFingerSpinner;
	}

	private void debug(String message) {
//		Log.d(TAG, message);
//		((TextView) ((Activity) mContext)
//				.findViewById(R.id.debugLabel)).setText(message);
	}

	public AnimationTapHandler getTapHandler() {
		return tapHandler;
	}

	public void setTapHandler(AnimationTapHandler tapHandler) {
		this.tapHandler = tapHandler;
	}

	public AnimationOneFingerDragHandler getOneFingerDragHandler() {
		return (AnimationOneFingerDragHandler) oneFingerSpinner.getSelectedItem();
	}

	public AnimationTwoFingerDragHandler getTwoFingerDragHandler() {
		return (AnimationTwoFingerDragHandler) twoFingerSpinner.getSelectedItem();
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
		oneFingerSpinner.setPressed(true);
		if (getOneFingerDragHandler() != null) getOneFingerDragHandler().onOneFingerMove(x, y, dx, dy);
	}

	public void onOneFingerFling(int x, int y, float vx, float vy) {
		debug("onOneFingerFling(" + x + ", " + y + ", " + vx + ", " + vy + ")");
		oneFingerSpinner.setPressed(false);
		if (getOneFingerDragHandler() != null) getOneFingerDragHandler().onOneFingerFling(x, y, vx, vy);
	}

	/**
	 * one finger move gesture was canceled
	 */
	public void onOneFingerMoveCancel() {
		debug("onOneFingerMoveCancel()");
		oneFingerSpinner.setPressed(false);
		if (getOneFingerDragHandler() != null) getOneFingerDragHandler().onCancel();
	}

	public void onTwoFingerMove(int x1, int y1, int dx1, int dy1, int x2, int y2, int dx2, int dy2) {
		debug("onTwoFingerMove(" + 
				x1 + ", " + y1 + ", " + dx1 + ", " + dy1 + ", "  + 
				x2 + ", " + y2 + ", " + dx2 + ", " + dy2 + ")");
		twoFingerSpinner.setPressed(true);
		if (getTwoFingerDragHandler() != null) getTwoFingerDragHandler().onTwoFingerMove(x1, y1, dx1, dy1, x2, y2, dx2, dy2);
	}

	public void onTwoFingerFling(int x1, int y1, float vx1, float vy1, int x2, int y2, float vx2, float vy2) {
		debug("onTwoFingerFling(" + 
				x1 + ", " + y1 + ", " + vx1 + ", " + vy1 + ", "  + 
				x2 + ", " + y2 + ", " + vx2 + ", " + vy2 + ")");
		twoFingerSpinner.setPressed(false);
		if (getTwoFingerDragHandler() != null) getTwoFingerDragHandler().onTwoFingerFling(x1, y1, vx1, vy1, x2, y2, vx2, vy2);
	}

	public void onTwoFingerMoveCancel() {
		debug("onTwoFingerMoveCancel()");
		twoFingerSpinner.setPressed(false);
		if (getTwoFingerDragHandler() != null) getTwoFingerDragHandler().onCancel();
	}
}