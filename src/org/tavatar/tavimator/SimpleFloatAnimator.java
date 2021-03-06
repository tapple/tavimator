package org.tavatar.tavimator;

import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class SimpleFloatAnimator {
	public Interpolator interpolator;
	public int duration = 300; // ms

	public float startValue = 0.0f;
	public float endValue = 1.0f;

	public long startTime = 0; // ms
	public float progress = 1.0f; // finished (idle) by default

	public float value = 0.0f;

	public SimpleFloatAnimator() {
		this.interpolator = new AccelerateDecelerateInterpolator();
	}

	public SimpleFloatAnimator(int duration) {
		this.duration = duration;
		this.interpolator = new AccelerateDecelerateInterpolator();
	}

	public SimpleFloatAnimator(Interpolator interpolator) {
		this.interpolator = interpolator;
	}

	public SimpleFloatAnimator(int duration, Interpolator interpolator) {
		this.duration = duration;
		this.interpolator = interpolator;
	}

	public SimpleFloatAnimator(float startValue, float endValue) {
		this.interpolator = new AccelerateDecelerateInterpolator();
		this.startValue = startValue;
		this.endValue = endValue;
	}

	public SimpleFloatAnimator(Interpolator interpolator, float startValue, float endValue) {
		this.interpolator = interpolator;
		this.startValue = startValue;
		this.endValue = endValue;
	}

	public SimpleFloatAnimator(int duration, float startValue, float endValue) {
		this.duration = duration;
		this.interpolator = new AccelerateDecelerateInterpolator();
		this.startValue = startValue;
		this.endValue = endValue;
	}

	public SimpleFloatAnimator(int duration, Interpolator interpolator, float startValue, float endValue) {
		this.duration = duration;
		this.interpolator = interpolator;
		this.startValue = startValue;
		this.endValue = endValue;
	}

	public void start(int duration) {
		this.duration = duration;
		start();
	}

	public void start() {
		startTime = SystemClock.uptimeMillis();
		progress = 0.0f;
	}
	
	public void forceFinished(boolean finish) {
		if (!finish) return;
		startTime = 0;
		progress = 1.0f;
	}

	public boolean isFinished() {
		return progress == 1.0f;
	}

	/**
	 * 
	 * @return isFinished
	 */
	public boolean update() {
		long time = SystemClock.uptimeMillis() - startTime;
		if (time < 0) {
			progress = 0.0f;
		} else if (time > duration) {
			progress = 1.0f;
		} else {
			progress = (float)time / duration;
		}
		value = startValue + interpolator.getInterpolation(progress) * (endValue - startValue);
		return time > duration;
	}
}