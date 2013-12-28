package org.tavatar.tavimator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class TweenAnimation implements JointStore.Animation {
	private static final String TAG = "TweenAnimation";
	/**
	 * The a vector from the old origin + distance to the new origin + distance.
	 * the camera slides along this vector when moving between origins
	 */
	private float[] startValue;
	private SimpleFloatAnimator transition;

	public TweenAnimation(JointStore store) {
		this(300, new AccelerateDecelerateInterpolator(), store);
	}

	public TweenAnimation(int duration, JointStore store) {
		this(duration, new AccelerateDecelerateInterpolator(), store);
	}

	public TweenAnimation(int duration, Interpolator interpolator, JointStore store) {
		startValue = new float[store.value.length];
		System.arraycopy(store.value, 0, startValue, 0, store.value.length);
		transition = new SimpleFloatAnimator(duration, interpolator, 0f, 1f);
		transition.start();
	}

	@Override
	public boolean updateTweenedValue(JointStore store) {
		boolean isFinished = transition.update();
		for (int i = 0; i < startValue.length; i++) {
			store.tweenedValue[i] = transition.value * store.tweenedValue[i] + 
					(1f - transition.value * startValue[i]);
		}
		return isFinished;
	}
}