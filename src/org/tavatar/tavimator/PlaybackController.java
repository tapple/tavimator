package org.tavatar.tavimator;

import android.content.Context;
import android.os.SystemClock;
import android.widget.Scroller;

/**
 * MVC pattern: Animation is the Model, AnimationView and Timeline are the
 * views, I am the controller. I provide the logic on translating playback rate,
 * flings, scrolls, and frame snaps into animation times. I use a continuous
 * time model, and support frames on top of that
 *
 * @author tapple
 *
 */
public class PlaybackController {
	private static final String TAG = "AnimationController";

	private static final int DEFAULT_SNAP_DURATION = 200; // milliseconds

	/**
	 * if stopAtEnd is false and loopedPlayback is false, we will loop the full
	 * animation, but hold the first and last frames for this long
	 */
	private static final float OVERPLAY_DURATION = 1.0f; // seconds

	/**
	 *  The animation I control playback for
	 */
	private Animation animation;

	/**
	 *  The animation time, in seconds, as of the last frame update
	 */
	private float animTime;

	/**
	 * The wall clock time, in millisecond, as of the last frame update. This is
	 * taken from SystemClock.uptimeMillis
	 */
	private long realTime;

	/**
	 * The screen coordinate, in pixels, of the current time, as of the last
	 * frame update. Not meaningful during playback or snapping
	 */
	private int x;

	/**
	 *  animation time advances this much as wall time progresses:, in animation seconds per wall millisecond
	 *  	0: stopped
	 *  	1000: playing at regular speed
	 *  	-1000: playing backward
	 *  	anything else: playing fast or slow
	 */
	private float playbackRate;

	/**
	 * An animator used for snapping to particular times or keyframes
	 */
	private SimpleFloatAnimator snapper = new SimpleFloatAnimator();

	/**
	 * Scroller for timeline flings currently in progress. x axis represents
	 * time. I don't use the y axis, but Timeline does. X=0 corresponds to
	 * t=screenOriginTime. Each pixel on the scroller cooresponds to screenDensity
	 * seconds of animation time
	 */
	private Scroller flinger;

	/**
	 * The animation time, in seconds, at pixel x=0 in screen coordinates. This
	 * is reset to the current time whenever something happens that would cause times that
	 * previously fell on integral pixel to no longer do so. Events that can
	 * cause this include:
	 *
	 * - timeline zooming
	 * - snapping the timeline to a particular time or keyframe
	 * - realtime playback
	 */
	private float screenOriginTime;

	/**
	 * The on-screen density of the animation, in seconds per pixel. Must be
	 * positive. This controls how densely the timeline is drawn, and also
	 * translates the scrollers actions from pixels to time each pixel on the
	 * scroller cooresponds to this much animation time. Must be positive. Also
	 * cooresponds to the screen density of the animation on the timeline.
	 * Timeline will initialize this to something respecting the device
	 * resolution
	 */
	private float screenDensity = -0.003f; // invalid value to check for initialization

	public static final int UNLOOPED = 1;
	public static final int ENDLESS_LOOPING = -2;
	public static final int ENDLESS_LOOPING_AFTER_LOOP_IN = -3;
	public static final int ZERO_LOOPING = 0;
	public static final int ZERO_LOOPING_AFTER_LOOP_OUT = -1;

	/**
	 * A non-negative number specifying how many times the looped portion of the
	 * animation should be repeated before the animation ends. Can be a
	 * non-negative value or one of the following special values:
	 * 
	 * UNLOOPED: The default. the looped portion will be played once. Equivalent
	 * to loop count of 1;
	 * 
	 * ENDLESS_LOOPING: The looped portion repeats endlessly both forward and
	 * backward in time. The loop in and out portions never play
	 * 
	 * ENDLESS_LOOPING_AFTER_LOOP_IN: Playback will switch to ENDLESS_LOOPING
	 * once we reach the looped portion of the animation. either by going
	 * forward or backward. Timewise, equivalent to UNLOOPED
	 * 
	 * ZERO_LOOPING: The looped portion will be skipped entirely. Only the loop
	 * in and out portions will be played. Equivalent to a loop count of 0
	 * 
	 * ZERO_LOOPING_AFTER_LOOP_OUT: Playback will switch to ZERO_LOOPING once it
	 * leaves the looped portion, either forwards or backwards. Timewise,
	 * equivalent to UNLOOPED
	 * 
	 * This is independent of whether or not the animation file requests looped
	 * playback within SecondLife
	 */
	private int loopCount = UNLOOPED;

	/**
	 * If isPlaybackLooping is true, no effect. Otherwise:
	 *
	 * If true; stop playback once the end of the animation is reached
	 *
	 * If false, hold the last frame for one second, then the first frame for one second, then restart playback
	 */
	private boolean shouldStopAtEnd;

	/**
	 * If true, the animation will snap to integral increments of its frame
	 * duration when idle. Only makes sense for frame-based animations
	 */
	private boolean snapToFrames = false;

	public PlaybackController(Context context) {
		realTime = SystemClock.uptimeMillis();
		flinger = new Scroller(context);
	}

	public float animDuration() {
		return animation.getNumberOfFrames() * animation.frameTime();
	}
	
	public float animStartTime() {
		return 0f;
	}
	
	public float animEndTime() {
		return animDuration();
	}

	public float animLoopInTime() {
		return animation.getLoopInPoint() * animation.frameTime();
	}

	public float animLoopOutTime() {
		return animation.getLoopOutPoint() * animation.frameTime();
	}

	public float animLoopInDuration() {
		return animation.getLoopInPoint() * animation.frameTime();
	}

	public float animLoopOutDuration() {
		return (animation.getNumberOfFrames() - animation.getLoopOutPoint()) * animation.frameTime();
	}

	public float animLoopDuration() {
		return (animation.getLoopOutPoint() - animation.getLoopInPoint()) * animation.frameTime();
	}

	private int ignoreBoundaryTransitions(int loopCount) {
		switch (loopCount) {
		case ZERO_LOOPING_AFTER_LOOP_OUT:
		case ENDLESS_LOOPING_AFTER_LOOP_IN:
			return UNLOOPED;
		}
		return loopCount;
	}

	private float basicPlaybackStartTime(int loopCount, boolean ignoreBoundaryTransitions) {
		if (ignoreBoundaryTransitions) loopCount = ignoreBoundaryTransitions(loopCount);
		switch (loopCount) {
		case ENDLESS_LOOPING: return Float.NEGATIVE_INFINITY;
		case ENDLESS_LOOPING_AFTER_LOOP_IN:
			if (animTime < animLoopInTime()) return animStartTime();
			else return Float.NEGATIVE_INFINITY;
		case ZERO_LOOPING_AFTER_LOOP_OUT: return animStartTime();
		default: return animStartTime();
		}
	}

	private float basicPlaybackEndTime(int loopCount, boolean ignoreBoundaryTransitions) {
		if (ignoreBoundaryTransitions) loopCount = ignoreBoundaryTransitions(loopCount);
		switch (loopCount) {
		case ENDLESS_LOOPING: return Float.POSITIVE_INFINITY;
		case ENDLESS_LOOPING_AFTER_LOOP_IN:
			if (animTime > animLoopOutTime()) return animEndTime();
			else return Float.POSITIVE_INFINITY;
		case ZERO_LOOPING_AFTER_LOOP_OUT:
			if (animTime > animLoopInTime()) return animEndTime();
			else return animLoopInDuration() + animLoopOutDuration();
		default: return animLoopInDuration() + animLoopDuration() * loopCount + animLoopOutDuration();
		}
	}

	private float basicPlaybackLoopInTime(int loopCount, boolean ignoreBoundaryTransitions) {
		return basicPlaybackStartTime(loopCount, ignoreBoundaryTransitions) + animLoopInDuration(); 
	}
	
	private float basicPlaybackLoopOutTime(int loopCount, boolean ignoreBoundaryTransitions) {
		return basicPlaybackEndTime(loopCount, ignoreBoundaryTransitions) - animLoopOutDuration();
	}

	private float basicNormalizedTime(float time, int loopCount, boolean keepInRange, boolean ignoreBoundaryTransitions) {
		if (time <= basicPlaybackLoopInTime(loopCount, ignoreBoundaryTransitions)) {
			//do nothing
		} else if (time >= basicPlaybackLoopOutTime(loopCount, ignoreBoundaryTransitions)) {
			time += animEndTime() - basicPlaybackEndTime(loopCount, ignoreBoundaryTransitions);
		} else {
			time = animLoopInDuration() + mod(time-animLoopInDuration(), animLoopDuration());
		}
		if (keepInRange) {
			if (time > animEndTime()) time = animEndTime();
			if (time < animStartTime()) time = animStartTime();
		}
		return time;
	}

	/**
	 *
	 * @return The time before which the animation could not rewind, starting from here. Overplay is not considered
	 */
	public float playbackStartTime() {
		return basicPlaybackStartTime(loopCount, false);
	}

	/**
	 * @return The time after which the animation could not play forward, starting from here. Overplay is not considered
	 */
	public float playbackEndTime() {
		return basicPlaybackEndTime(loopCount, false);
	}
	
	public float playbackLoopInTime() {
		return playbackStartTime() + animLoopInDuration();
	}
	
	public float playbackLoopOutTime() {
		return playbackEndTime() - animLoopOutDuration();
	}

	public float overplayStartTime() {
		return basicPlaybackStartTime(loopCount, true) - OVERPLAY_DURATION; 
	}

	public float overplayEndTime() {
		return basicPlaybackEndTime(loopCount, true) + OVERPLAY_DURATION;
	}

	/**
	 * convert a playback time of any real number to an animation time between
	 * the start and end time, based on the current settings for loopCount. Does
	 * not handle overplay
	 * 
	 * @param a
	 *            playback time
	 * @return an animation time
	 */
	public float normalizedTime(float time) {
		return basicNormalizedTime(time, loopCount, true, false);
	}

	public void setLoopCount(int newCount, boolean snapIfImpossible) {
		if (loopCount == newCount) return;

		animTime = basicNormalizedTime(animTime, loopCount, false, false);
		switch (newCount) {
		case ENDLESS_LOOPING:
		case ENDLESS_LOOPING_AFTER_LOOP_IN:
			if (animTime < animLoopInTime()) {
				loopCount = ENDLESS_LOOPING_AFTER_LOOP_IN;
				if (snapIfImpossible && newCount == ENDLESS_LOOPING) snapTo(animLoopInTime());
			} else if (animTime <= animLoopOutTime()) {
				loopCount = ENDLESS_LOOPING;
			} else {
				loopCount = ENDLESS_LOOPING_AFTER_LOOP_IN;
				if (snapIfImpossible && newCount == ENDLESS_LOOPING) snapTo(animLoopOutTime());
			}
			break;
		case ZERO_LOOPING:
		case ZERO_LOOPING_AFTER_LOOP_OUT:
			if (animTime <= animLoopInTime()) {
				loopCount = ZERO_LOOPING;
			} else if (animTime < animLoopOutTime()) {
				loopCount = ZERO_LOOPING_AFTER_LOOP_OUT;
				if (snapIfImpossible && newCount == ZERO_LOOPING) snapTo(animLoopOutTime());
			} else {
				loopCount = ZERO_LOOPING;
			}
			break;
		default:
			loopCount = newCount;
		}

		if (loopCount >= 0 && animTime > animLoopOutTime()) {
			animTime += (loopCount-1) * animLoopDuration();
		}
	}
	
	public void setLooping(boolean looping) {
		if (looping) setLoopCount(ENDLESS_LOOPING, false);
		else setLoopCount(UNLOOPED, false );
	}
	
	public boolean getLooping() {
		return loopCount != UNLOOPED;
	}

	private void checkBoundaryConditions() {
		switch (loopCount) {
		case ENDLESS_LOOPING_AFTER_LOOP_IN:
			if (animLoopInTime() <= animTime && animTime <= animLoopOutTime()) {
				setLoopCount(ENDLESS_LOOPING, false);
			}
			break;
		case ZERO_LOOPING_AFTER_LOOP_OUT:
			if (animTime <= animLoopInTime() || animLoopOutTime() <= animTime) {
				setLoopCount(ZERO_LOOPING, false);
			}
			break;
		}

		if (playbackActive() && shouldStopAtEnd /* || !flinger.isFinished() */) {
			if (playbackEndTime() < animTime) {
				playbackRate = 0f;
//				flinger.forceFinished(true);
				animTime = playbackEndTime();
			}
			if (animTime < playbackStartTime()) {
				playbackRate = 0f;
//				flinger.forceFinished(true);
				animTime = playbackStartTime();
			}
		}

		if (playbackActive() && !shouldStopAtEnd) {
			if (overplayEndTime() < animTime) {
				animTime -= overplayEndTime() - overplayStartTime();
			}
			if (animTime < overplayStartTime()) {
				animTime += overplayEndTime() - overplayStartTime();
			}
		}
	}

	public boolean playbackActive() {
		return playbackRate != 0.0f;
	}

	public void update() {
		if (isFinished()) return;

		long prevRealTime = realTime;
		realTime = SystemClock.uptimeMillis();
		long realTimeDelta = realTime - prevRealTime;

		if (playbackActive()) {
			animTime += realTimeDelta * playbackRate;
		}

		if (!flinger.isFinished()) {
			flinger.computeScrollOffset();
			animTime += (flinger.getCurrX() - x) * screenDensity;
			x = flinger.getCurrX();
			if (flinger.isFinished()) snapIfNeeded();
		}

		if (!snapper.isFinished()) {
			snapper.update();
			animTime = snapper.value;
		}

		checkBoundaryConditions();
		if (isFinished()) {
			resetScreenOrigin();
		}
	}

	public boolean isFinished() {
		return !playbackActive() && flinger.isFinished() && snapper.isFinished();
	}

	private float mod(float a, float b) {
		return a - b*(int)(a/b);
	}

	private void resetScreenOriginTo(float newTime) {
		x = 0;
		screenOriginTime = newTime;
	}

	private void resetScreenOrigin() {
		resetScreenOriginTo(animTime);
	}

	public float getNormalizedTime() {
		return normalizedTime(animTime);
	}

	public float getTime() {
		return animTime;
	}

	public void setTime(float newTime) {
		animTime = newTime;
		resetScreenOrigin();
		checkBoundaryConditions();
	}

	/**
	 * make newTime the animation time within duration milliseconds
	 * @param newTime The new animation time
	 * @param duration the duration of the snap animation, in milliseconds
	 */
	public void snapTo(float newTime, int duration) {
		if (animTime == newTime) return;
		snapper.startValue = animTime;
		snapper.endValue = newTime;
		snapper.start(duration);
	}

	public void snapTo(float newTime) {
		snapTo(newTime, DEFAULT_SNAP_DURATION);
	}

	/**
	 * start realtime playback
	 * @param rate The ratio of animation time to realtime:
	 * 		1.0f: normal speed
	 * 		2.0f: double speed
	 * 		0.5f: half speed
	 * 		-1.0f: backward
	 * 		0.0f: stopped
	 */
	public void play(float rate) {
		playbackRate = rate;
	}

	public void play() {
		play(1.0f);
	}

	/**
	 * Stop changing the animation time, whether it is changing because of playback, fling, or snap
	 */
	public void pause() {
		playbackRate = 0.0f;
		flinger.forceFinished(true);
		snapper.forceFinished(true);
		resetScreenOrigin();
	}

	/**
	 * @return the  frame, or zero if the animation is not frame-based
	 */
	public int getNearestFrame(float time) {
		return (int)(normalizedTime(time) * animation.fps() + 0.5f);
	}

	/**
	 * @return the  frame, or zero if the animation is not frame-based
	 */
	public int getPrevFrame(float time) {
		return (int)(normalizedTime(time) * animation.fps());
	}

	public float getFrameTime(int frame) {
		return animation.frameTime() * frame;
	}

	public void snapIfNeeded() {
		if (animTime < playbackStartTime()) {
			snapTo(playbackStartTime());
		} else if (animTime > playbackEndTime()) {
			snapTo(playbackEndTime());
		} else if (snapToFrames && animation.fps() != 0) {
			snapTo(getFrameTime(getNearestFrame(getTime())));
		}
	}
	
	public void setSnapToFrames(boolean snap) {
		snapToFrames = snap;
	}

	public void scrollBy(int xDelta, int yDelta) {
		x -= xDelta;
		animTime -= xDelta * screenDensity;
		checkBoundaryConditions();
	}

	private int timeToX(float time, boolean roundUp) {
		if (time == Float.POSITIVE_INFINITY) return Integer.MAX_VALUE;
		if (time == Float.NEGATIVE_INFINITY) return Integer.MIN_VALUE;
		int ans = (int)((time - screenOriginTime) / screenDensity);
		if (roundUp) ans += 1;
		return ans;
	}

	public void fling(int velocityX, int startY, int velocityY, int minY, int maxY) {
//		flinger.fling(x, startY, -velocityX, -velocityY, timeToX(playbackStartTime(), false), timeToX(playbackEndTime(), true), minY, maxY);
		flinger.fling(x, startY, -velocityX, -velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, minY, maxY);
	}

	public void fling(int velocityX) {
		fling(velocityX, 0, 0, 0, 0);
	}

	public void playFromStart() {
		setTime(0f);
		setLooping(animation.getLoop());
		shouldStopAtEnd = true;
		play();
	}

	public void continueToEnd() {
		shouldStopAtEnd = true;
		setLooping(false);
		play();
	}

	/**
	 *
	 * @return pixels per frame
	 */
	public float frameSpacing() {
		return animation.frameTime() / screenDensity;
	}

	public float getScreenDensity() {
		return screenDensity;
	}

	public void setScreenDensity(float screenDensity) {
		this.screenDensity = screenDensity;
		resetScreenOrigin();
	}

	public float roundTimeToMultipleOf(float time, float interval, int rounding) {
		float multiple = normalizedTime(time)/interval;
		if (rounding == 0) multiple = (float)Math.floor(multiple + 0.5f);
		else if (rounding < 0) multiple = (float)Math.floor(multiple);
		else multiple = (float)Math.ceil(multiple);
		return multiple * interval;
	}

	public Animation getAnimation() {
		return animation;
	}

	public void setAnimation(Animation animation) {
		if (this.animation != null) animTime = basicNormalizedTime(animTime, loopCount, false, true);
		this.animation = animation;
		if (isFinished()) snapIfNeeded();
	}
}
