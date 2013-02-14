package org.tavatar.tavimator;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AnimationPartSelector implements AnimationTapHandler, Handler.Callback {
	
	private static String TAG = "AnimationPartSelector";
	
	AnimationView view;
	private Handler pickResultHandler = new Handler(this);
	
	private enum TapStatus {
		DOWN, FINISHED, CANCELED
	}
	
	public boolean isPickResultReady = false;
	private TapStatus status = TapStatus.CANCELED;
	public int pickResult;

	public AnimationPartSelector(AnimationView view) {
		this.view = view;
	}

	@Override
	public String shortToolName() {
		return "Select";
	}

	@Override
	public String toolName() {
		return "Select part";
	}

	@Override
	public void onCancel() {
		status = TapStatus.CANCELED;
		update();
	}

	@Override
	public void onFingerDown(int x, int y) {
		view.pickPart(x, y, pickResultHandler);
		isPickResultReady = false;
		status = TapStatus.DOWN;
	}

	@Override
	public void onTap(int x, int y) {
		status = TapStatus.FINISHED;
		update();
	}

	@Override
	public boolean handleMessage(Message msg) {
		// TODO Auto-generated method stub
		if (msg.what != AnimationView.PICK_PART_RESULT) return false;
		pickResult = msg.arg1;
		isPickResultReady = true;
		update();
		return false;
	}
	
	public String dumpNode(BVHNode node) {
		if (node == null) return null;
		return node.name();
	}
	
	private void update() {
		if (!isPickResultReady) return;
		
		Log.d(TAG, "selection update: " + pickResult + " from thread " + Thread.currentThread().hashCode());
		
		view.setPartHighlighted(pickResult);
		view.selectPart(pickResult);
		BVHNode selectedPart = view.getSelectedPart();
		if (selectedPart != null) {
			float[] origin = selectedPart.cachedOrigin();
			view.getRenderer().getCamera().moveToOrigin(origin);
		}

/*
		switch (status) {
		case DOWN:
			Log.d(TAG, "down; pickResult: " + pickResult);
			if (selectedPart != null) {
				view.getSelectionTrackball().trackGyroscope(view.getGyroscope(), true);
			} else {
				view.getCameraTrackball().trackGyroscope(view.getGyroscope(), true);				
			}
			break;
		case FINISHED:
			Log.d(TAG, "finished; pickResult: " + pickResult);
		case CANCELED:
			Log.d(TAG, "finished/canceled; pickResult: " + pickResult);
			endGyroGrab();	
		}
*/
	}
	
	public void endGyroGrab() {
		if (pickResult < 0) {
			view.getCameraTrackball().trackGyroscope(null, false);
		} else {
			view.getSelectionTrackball().trackGyroscope(null, false);
		}
	}
}