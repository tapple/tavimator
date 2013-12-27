package org.tavatar.tavimator;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.Matrix;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
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
		return new DragHandler(getStore().context, nameId, shortNameId);
	}

	private void scrollBy(float[] angularVelocity) {
		Log.d(TAG, "scrollBy(" + arrayToString(angularVelocity) + ");");
		Matrix.multiplyMV(scrollAxis, 0, getCameraToTrackballOrientation(), 0, angularVelocity, 0);
		basicRotateAbout(scrollAxis, 0, getStore().tempModificationMatrix1, getStore().tempModificationMatrix2);
		scaleBy(angularVelocity[3]);
	}
}
