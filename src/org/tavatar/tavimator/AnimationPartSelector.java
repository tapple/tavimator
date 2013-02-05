package org.tavatar.tavimator;

import android.os.Handler;
import android.os.Message;

public class AnimationPartSelector implements AnimationTapHandler, Handler.Callback {
	
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
		switch (status) {
		case DOWN:
			view.selectPart(pickResult);
			if (view.getSelectedPart() != null) {
				view.getRenderer().getCamera().moveToOrigin(view.getSelectedPart().cachedOrigin());
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