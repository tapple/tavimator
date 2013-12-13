package org.tavatar.tavimator;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.Matrix;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.Scroller;

public class TouchTrackball extends Trackball {
	
	private static final String TAG = "TouchTrackball";

	public TouchTrackball(Context context) {
		super(context);
	}

	public AnimationDragHandler getDragHandler(int nameId, int shortNameId) {
		class DragHandler extends AbsTouchHandler implements AnimationDragHandler {
			public DragHandler(Context context, int nameId, int shortNameId) {
				super(context, nameId, shortNameId);
			}

			@Override
			public void onMove(PointerGroup pointers) {
				pointers.getAngularVelocity(scrollVelocity, Pointer.VelocityType.perFrame);
				scrollBy(scrollVelocity);
			}

			@Override
			public void onFling(PointerGroup pointers) {
				pointers.getAngularVelocity(scrollVelocity, Pointer.VelocityType.perSecond);
				fling(scrollVelocity);
			}

			@Override
			public void onCancel() {
				stopFling();
			}
		}
		return new DragHandler(mContext, nameId, shortNameId);
	}

	private synchronized void scrollBy(float[] angularVelocity) {
		//		Log.d(TAG, "scrollBy(" + arrayToString(angularVelocity) + ");");
		Matrix.multiplyMV(scrollAxis, 0, cameraToTrackball, 0, angularVelocity, 0);
		rotateAboutCameraAxis(
				Matrix.length(angularVelocity[0], angularVelocity[1], angularVelocity[2]),
				scrollAxis);
		listener.zoomBy(1.0f / angularVelocity[3]);
	}
}
