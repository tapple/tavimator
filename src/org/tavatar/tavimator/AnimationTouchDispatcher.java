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

	private AnimationPartSelector tapHandler;
	private AnimationDragHandler cameraHandler;
	private AnimationDragHandler partHandler;

	public AnimationTouchDispatcher(Context context) {
		mContext = context;
	}

	private void debug(String message) {
		Log.d(TAG, message);
		((TextView) ((Activity) mContext)
				.findViewById(R.id.debugLabel)).setText(message);
	}

	private void verbose(String message) {
		Log.v(TAG, message);
		((TextView) ((Activity) mContext)
				.findViewById(R.id.debugLabel)).setText(message);
	}

	public AnimationTapHandler getTapHandler() {
		return tapHandler;
	}

	public void setTapHandler(AnimationPartSelector tapHandler) {
		this.tapHandler = tapHandler;
	}

	public TwoFingerTrackball getTrackball() {
		if (!tapHandler.isPickResultReady) return null;
		if (tapHandler.pickResult < 0) {
			return tapHandler.view.getCameraTrackball();
		} else {
			return tapHandler.view.getSelectionTrackball();
		}		
	}

	public AnimationDragHandler getDragHandler() {
		//		return (AnimationOneFingerDragHandler) oneFingerSpinner.getSelectedItem();
		if (!tapHandler.isPickResultReady) return null;
		if (tapHandler.pickResult < 0) {
			return cameraHandler;
		} else {
			return partHandler;
		}
	}

	public void onFingerDown(PointerGroup pointers) {
		if (getTapHandler() != null) getTapHandler().onFingerDown(pointers);
	}

	public void onTap(PointerGroup pointers) {
		if (getTapHandler() != null) getTapHandler().onTap(pointers);
	}

	public void onTapCancel() {
		debug("onTapCancel()");
		if (getTapHandler() != null) getTapHandler().onCancel();
	}

	public void onMove(PointerGroup pointers) {
		if (getDragHandler() == null) return;
		getDragHandler().onMove(pointers);
	}

	public void onFling(PointerGroup pointers) {
		if (getDragHandler() == null) return;
		getDragHandler().onFling(pointers);
		tapHandler.endGyroGrab();
	}

	/**
	 * one finger move gesture was canceled
	 */
	public void onMoveCancel() {
		debug("onMoveCancel()");
		if (getDragHandler() == null) return;
		getDragHandler().onCancel();
		tapHandler.endGyroGrab();
	}

	public AnimationDragHandler getCameraHandler() {
		return cameraHandler;
	}

	public void setCameraHandler(AnimationDragHandler cameraHandler) {
		this.cameraHandler = cameraHandler;
	}

	public AnimationDragHandler getPartHandler() {
		return partHandler;
	}

	public void setPartHandler(AnimationDragHandler partHandler) {
		this.partHandler = partHandler;
	}
}