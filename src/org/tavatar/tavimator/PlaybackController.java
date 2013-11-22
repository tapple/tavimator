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
	private static String TAG = "AnimationController";

	private static int DEFAULT_SNAP_DURATION = 200; // milliseconds

	/**
	 * if stopAtEnd is false and loopedPlayback is false, we will loop the full
	 * animation, but hold the first and last frames for this long
	 */
	private static float OVERPLAY_DURATION = 1.0f; // seconds

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

	/**
	 * If true, the animation will play between loopStartTime and loopEndTime,
	 * and immediately repeat. Scrolling will loop
	 *
	 * If false, the animation will play between 0 and endTime. It will repeat
	 * with a 2 second delay in some cases. Scrolling will not loop.
	 *
	 * This is independent of whether or not the animation file requests looped
	 * playback within SecondLife
	 */
	private boolean isPlaybackLooping;

	/**
	 * If isPlaybackLooping is true, or animation is past the loop in point no effect. Otherwise:
	 *
	 * If true; playbackLooped will become true once the animation passes the loop in point
	 */
	private boolean shouldStartLoopingAtLoopIn;

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

	/**
	 * If true, the animation is running linearly against the scroller
	 */
	private boolean flingActive = false;

	/**
	 * If true, the animation is in the process of snapping to a particular time
	 */
	private boolean snapActive;

	public PlaybackController(Context context) {
		realTime = SystemClock.uptimeMillis();
		flinger = new Scroller(context);
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
			if (!isPlaybackLooping && shouldStopAtEnd && (animTime <= animStartTime() || animEndTime() <= animTime)) {
				playbackRate = 0.0f;
				snapIfNeeded();
			}
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

		checkLoopIn();
		if (isFinished()) {
			normalizeTime();
		}
	}

	public boolean isFinished() {
		return !playbackActive() && flinger.isFinished() && snapper.isFinished();
	}

	private void checkLoopIn() {
		if (shouldStartLoopingAtLoopIn && animTime >= animLoopInTime()) {
			isPlaybackLooping = true;
		}
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

	public float animStartTime() {
		return 0.0f;
	}

	public float animEndTime() {
		return animation.getNumberOfFrames() * animation.frameTime();
	}

	public float animLoopInTime() {
		return animation.getLoopInPoint() * animation.frameTime();
	}

	public float animLoopOutTime() {
		return animation.getLoopOutPoint() * animation.frameTime();
	}

	private float animOverplayStartTime() {
		return animStartTime() - OVERPLAY_DURATION;
	}

	private float animOverplayEndTime() {
		return animEndTime() + OVERPLAY_DURATION;
	}

	/**
	 *
	 * @return The time before which the animation could not rewind, starting from here. Overplay is not considered
	 */
	public float playbackStartTime() {
		if (isPlaybackLooping) return Float.NEGATIVE_INFINITY;
		else return animStartTime();
	}

	/**
	 * @return The time after which the animation could not play forward, starting from here. Overplay is not considered
	 */
	public float playbackEndTime() {
		if (isPlaybackLooping) return Float.POSITIVE_INFINITY;
		else if (shouldStartLoopingAtLoopIn) return Float.POSITIVE_INFINITY;
		else return animEndTime();
	}

	/**
	 * convert a playback time of any real number to an animation time between the start and end time, based on the current settings for playbackLooped and stopAtEnd
	 * @param a playback time
	 * @return an animation time
	 */
	public float normalizedTime(float time) {
		if (animStartTime() <= time && time <= animEndTime()) return time;
		else if (isPlaybackLooping || shouldStartLoopingAtLoopIn && time >= animLoopInTime()) time = mod(time-animLoopInTime(), animLoopOutTime()-animLoopInTime());
		else if (!shouldStopAtEnd) time = mod(time-animOverplayStartTime(), animOverplayEndTime()-animOverplayStartTime());
		if (time > animEndTime()) time = animEndTime();
		if (time < animStartTime()) time = animStartTime();
		return time;
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
		checkLoopIn();
	}

	private void normalizeTime() {
		setTime(getNormalizedTime());
	}

	/**
	 * make newTime the animation time within duration milliseconds
	 * @param newTime The new animation time
	 * @param duration the duration of the snap animation, in milliseconds
	 */
	public void snapTo(float newTime, int duration) {
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
		normalizeTime();
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
		if (!snapToFrames) return;
		if (animation.fps() == 0) return;
		normalizeTime();
		snapTo(getFrameTime(getNearestFrame(getTime())));
	}

	public void scrollBy(int xDelta, int yDelta) {
		x -= xDelta;
		animTime -= xDelta * screenDensity;
		checkLoopIn();
	}

	private int timeToX(float time, boolean roundUp) {
		if (time == Float.POSITIVE_INFINITY) return Integer.MAX_VALUE;
		if (time == Float.NEGATIVE_INFINITY) return Integer.MIN_VALUE;
		int ans = (int)((time - screenOriginTime) / screenDensity);
		if (roundUp) ans += 1;
		return ans;
	}

	public void fling(int velocityX, int startY, int velocityY, int minY, int maxY) {
		flinger.fling(x, startY, -velocityX, -velocityY, timeToX(playbackStartTime(), false), timeToX(playbackEndTime(), true), minY, maxY);
	}

	public void fling(int velocityX) {
		fling(velocityX, 0, 0, 0, 0);
	}

	public void playFromStart() {
		setTime(0f);
		isPlaybackLooping = false;
		shouldStartLoopingAtLoopIn = animation.getLoop();
		shouldStopAtEnd = true;
		play();
	}

	public void continueToEnd() {
		shouldStopAtEnd = true;
		setLooping(false);
		play();
	}

	public void setLooping(boolean looping) {
		isPlaybackLooping = looping;
		if (!isPlaybackLooping) {
			normalizeTime();
			shouldStartLoopingAtLoopIn = false;
		}
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
		this.animation = animation;
		normalizeTime();
		if (isFinished()) snapIfNeeded();
	}
}
