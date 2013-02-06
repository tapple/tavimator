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
	
	private void update() {
		if (!isPickResultReady) return;
		
		Log.d(TAG, "selection update from thread " + Thread.currentThread().hashCode());
		
		switch (status) {
		case DOWN:
			view.selectPart(pickResult);
			BVHNode safeSelectedPart = view.getSelectedPart();
			if (safeSelectedPart != null) {
				int nullCount = 0;
				for (int i = 0; i < 1000; i++) {
					if (view.getSelectedPart() == null) {
						nullCount++;
					}
				}
				Log.d(TAG, nullCount + " of 1000 queries were null");

				float[] origin = safeSelectedPart.cachedOrigin();
				view.getRenderer().getCamera().moveToOrigin(origin);
				view.getSelectionTrackball().trackGyroscope(view.getGyroscope(), true);
			} else {
				view.getCameraTrackball().trackGyroscope(view.getGyroscope(), true);				
			}
			break;
		case FINISHED:
		case CANCELED:
			endGyroGrab();	
		}
	}
	
	public void endGyroGrab() {
		if (pickResult < 0) {
			view.getCameraTrackball().trackGyroscope(null, false);
		} else {
			view.getSelectionTrackball().trackGyroscope(null, false);
		}
	}
}