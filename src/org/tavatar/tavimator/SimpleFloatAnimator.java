package org.tavatar.tavimator;

import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class SimpleFloatAnimator {
	public Interpolator interpolator = new AccelerateDecelerateInterpolator();
	public int duration = 300; // ms

	public float startValue = 0.0f;
	public float endValue = 1.0f;

	public long startTime = 0; // ms
	public float progress = 0.0f;

	public float value = 0.0f;

	public SimpleFloatAnimator() {
	}

	public SimpleFloatAnimator(int duration) {
		this.duration = duration;
	}

	public SimpleFloatAnimator(Interpolator interpolator) {
		this.interpolator = interpolator;
	}

	public SimpleFloatAnimator(int duration, Interpolator interpolator) {
		this.duration = duration;
		this.interpolator = interpolator;
	}

	public SimpleFloatAnimator(float startValue, float endValue) {
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
		this.startValue = startValue;
		this.endValue = endValue;
	}

	public SimpleFloatAnimator(int duration, Interpolator interpolator, float startValue, float endValue) {
		this.duration = duration;
		this.interpolator = interpolator;
		this.startValue = startValue;
		this.endValue = endValue;
	}

	public void start() {
		startTime = SystemClock.uptimeMillis();
	}

	public boolean isFinished() {
		long time = SystemClock.uptimeMillis() - startTime;
		return time > duration;
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