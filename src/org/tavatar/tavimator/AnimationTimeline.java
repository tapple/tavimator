/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tavatar.tavimator;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Filter;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A widget that enables the user to select a number form a predefined range.
 * There are two flavors of this widget and which one is presented to the user
 * depends on the current theme.
 * <ul>
 * <li>
 * If the current theme is derived from {@link android.R.style#Theme} the widget
 * presents the current value as an editable input field with an increment button
 * above and a decrement button below. Long pressing the buttons allows for a quick
 * change of the current value. Tapping on the input field allows to type in
 * a desired value.
 * </li>
 * <li>
 * If the current theme is derived from {@link android.R.style#Theme_Holo} or
 * {@link android.R.style#Theme_Holo_Light} the widget presents the current
 * value as an editable input field with a lesser value above and a greater
 * value below. Tapping on the lesser or greater value selects it by animating
 * the number axis up or down to make the chosen value current. Flinging up
 * or down allows for multiple increments or decrements of the current value.
 * Long pressing on the lesser and greater values also allows for a quick change
 * of the current value. Tapping on the current value allows to type in a
 * desired value.
 * </li>
 * </ul>
 * <p>
 * For an example of using this widget, see {@link android.widget.TimePicker}.
 * </p>
 */

public class AnimationTimeline extends LinearLayout implements Animation.OnAnimationChangeListener {

	private static final String TAG = "AnimationTimeline";

	/**
	 * The coefficient by which to adjust (divide) the max fling velocity.
	 */
	private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8;

	/**
	 * The strength of fading in the top and bottom while drawing the selector.
	 */
	private static final float LEFT_AND_RIGHT_FADING_EDGE_STRENGTH = 0.9f;

	/**
	 * The resource id for the default layout.
	 */
	private static final int DEFAULT_LAYOUT_RESOURCE_ID = R.layout.timeline;

	/**
	 * Constant for unspecified size.
	 */
	private static final int SIZE_UNSPECIFIED = -1;

	/**
	 * Holds all playback state and some screen state
	 */
	private PlaybackController playback;

	/**
	 * The text for showing the current value.
	 */
	private final EditText mInputText;

	/**
	 * The min height of this widget.
	 */
	private final int mMinHeight;

	/**
	 * The max height of this widget.
	 */
	private int mMaxHeight;

	/**
	 * The max width of this widget.
	 */
	private final int mMinWidth;

	/**
	 * The max width of this widget.
	 */
	private final int mMaxWidth;

	/**
	 * Flag whether to compute the max height.
	 */
	private final boolean mComputeMaxHeight;

	/**
	 * The height of the text.
	 */
	private final int mTextSize;

	/**
	 * The {@link Paint} for drawing the selector.
	 */
	private final Paint mSelectorWheelPaint;

	/**
	 * The {@link Paint} for drawing the current time hairline.
	 */
	private final Paint mHairlinePaint;

	/**
	 * Handle to the reusable command for setting the input text selection.
	 */
	private SetSelectionCommand mSetSelectionCommand;

	/**
	 * The Y position of the last down or move event.
	 */
	private float mLastDownOrMoveEventX;

	/**
	 * Determines speed during touch scrolling.
	 */
	private VelocityTracker mVelocityTracker;

	/**
	 * @see ViewConfiguration#getScaledTouchSlop()
	 */
	private int mTouchSlop;

	/**
	 * @see ViewConfiguration#getScaledMinimumFlingVelocity()
	 */
	private int mMinimumFlingVelocity;

	/**
	 * @see ViewConfiguration#getScaledMaximumFlingVelocity()
	 */
	private int mMaximumFlingVelocity;

	/**
	 * The back ground color used to optimize scroller fading.
	 */
	private final int mSolidColor;

	/**
	 * Interface used to format current value into a string for presentation.
	 */
	public interface Formatter {

		/**
		 * Formats a string representation of the current value.
		 *
		 * @param value The currently selected value.
		 * @return A formatted string representation.
		 */
		public String format(int value);
	}

	/**
	 * Create a new number picker.
	 *
	 * @param context The application environment.
	 */
	public AnimationTimeline(Context context) {
		this(context, null);
	}

	/**
	 * Create a new number picker.
	 *
	 * @param context The application environment.
	 * @param attrs A collection of attributes.
	 */
	public AnimationTimeline(Context context, AttributeSet attrs) {
		super(context, attrs);

		// process style attributes
		TypedArray attributesArray = context.obtainStyledAttributes(
				attrs, R.styleable.AnimationTimeline, R.attr.timelineStyle, 0);
		final int layoutResId = DEFAULT_LAYOUT_RESOURCE_ID;

		mSolidColor = attributesArray.getColor(R.styleable.AnimationTimeline_solidColor, 0);

		mMinHeight = attributesArray.getDimensionPixelSize(
				R.styleable.AnimationTimeline_internalMinHeight, SIZE_UNSPECIFIED);

		mMaxHeight = attributesArray.getDimensionPixelSize(
				R.styleable.AnimationTimeline_internalMaxHeight, SIZE_UNSPECIFIED);
		if (mMinHeight != SIZE_UNSPECIFIED && mMaxHeight != SIZE_UNSPECIFIED
				&& mMinHeight > mMaxHeight) {
			throw new IllegalArgumentException("minHeight > maxHeight");
		}

		mMinWidth = attributesArray.getDimensionPixelSize(
				R.styleable.AnimationTimeline_internalMinWidth, SIZE_UNSPECIFIED);

		mMaxWidth = attributesArray.getDimensionPixelSize(
				R.styleable.AnimationTimeline_internalMaxWidth, SIZE_UNSPECIFIED);
		if (mMinWidth != SIZE_UNSPECIFIED && mMaxWidth != SIZE_UNSPECIFIED
				&& mMinWidth > mMaxWidth) {
			throw new IllegalArgumentException("minWidth > maxWidth");
		}

		mComputeMaxHeight = (mMaxHeight == SIZE_UNSPECIFIED);

		attributesArray.recycle();

		// By default Linearlayout that we extend is not drawn. This is
		// its draw() method is not called but dispatchDraw() is called
		// directly (see ViewGroup.drawChild()). However, this class uses
		// the fading edge effect implemented by View and we need our
		// draw() method to be called. Therefore, we declare we will draw.
		setWillNotDraw(false);

		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(layoutResId, this, true);

		// input text
		mInputText = (EditText) findViewById(R.id.numberpicker_input);
		mInputText.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					mInputText.selectAll();
				} else {
					mInputText.setSelection(0, 0);
					validateInputTextView(v);
				}
			}
		});
		mInputText.setFilters(new InputFilter[] {
				new InputTextFilter()
		});

		mInputText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		mInputText.setImeOptions(EditorInfo.IME_ACTION_DONE);

		// initialize constants
		ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity()
				/ SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT;
		mTextSize = (int) mInputText.getTextSize();

		// create the selector wheel paint
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.LEFT);
		paint.setTextSize(mTextSize);
		paint.setTypeface(mInputText.getTypeface());
		ColorStateList colors = mInputText.getTextColors();
		int color = colors.getColorForState(ENABLED_STATE_SET, Color.WHITE);
		paint.setColor(color);
		mSelectorWheelPaint = paint;

		mHairlinePaint = new Paint(paint);
		mHairlinePaint.setColor(Color.YELLOW);
	}

	public void setPlayback(PlaybackController playback) {
		this.playback = playback;
		updateInputTextView();
		invalidate();
	}

	private float dp2px(float dp) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int msrdWdth = getMeasuredWidth();
		final int msrdHght = getMeasuredHeight();

		// Input text centered horizontally.
		final int inptTxtMsrdWdth = mInputText.getMeasuredWidth();
		final int inptTxtMsrdHght = mInputText.getMeasuredHeight();
		final int inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2;
		final int inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2;
		final int inptTxtRight = inptTxtLeft + inptTxtMsrdWdth;
		final int inptTxtBottom = inptTxtTop + inptTxtMsrdHght;
		mInputText.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom);

		if (changed) {
			// need to do all this when we know our size
			initializeSelectorWheel();
			initializeFadingEdges();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Try greedily to fit the max width and height.
		final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth);
		final int newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight);
		super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
		// Flag if we are measured with width or height less than the respective min.
		final int widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth, getMeasuredWidth(),
				widthMeasureSpec);
		final int heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight, getMeasuredHeight(),
				heightMeasureSpec);
		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		final int action = event.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			removeAllCallbacks();
			mLastDownOrMoveEventX = event.getX();
			// Handle pressed state before any state change.
			// Make sure we support flinging inside scrollables.
			getParent().requestDisallowInterceptTouchEvent(true);
			playback.pause();
			return true;
		}
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
		int action = event.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_MOVE: {
			float currentMoveX = event.getX();
			if (true) { // if (moved enough to scroll) {
				int deltaMoveX = (int) ((currentMoveX - mLastDownOrMoveEventX));
				scrollBy(deltaMoveX, 0);
				invalidate();
			}
			mLastDownOrMoveEventX = currentMoveX;
		} break;
		case MotionEvent.ACTION_UP: {
			VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
			int initialVelocity = (int) velocityTracker.getXVelocity();
			if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
				fling(initialVelocity);
			} else {
				playback.snapIfNeeded();
			}
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		} break;
		}
		return true;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		final int action = event.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			removeAllCallbacks();
			break;
		}
		return super.dispatchTouchEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		final int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			removeAllCallbacks();
			break;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchTrackballEvent(MotionEvent event) {
		final int action = event.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			removeAllCallbacks();
			break;
		}
		return super.dispatchTrackballEvent(event);
	}

	@Override
	protected boolean dispatchHoverEvent(MotionEvent event) {
		return false;
	}

	@Override
	public void computeScroll() {
		if (!playback.isFinished()) {
			playback.update();
			invalidate();
			updateInputTextView();
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mInputText.setEnabled(enabled);
	}

	@Override
	public void scrollBy(int x, int y) {
		playback.scrollBy(x, y);
		updateInputTextView();
		invalidate();
	}

	@Override
	public int getSolidColor() {
		return mSolidColor;
	}

	/**
	 * Shows the soft input for its input text.
	 */
	private void showSoftInput() {
		InputMethodManager inputMethodManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			mInputText.setVisibility(View.VISIBLE);
			mInputText.requestFocus();
			inputMethodManager.showSoftInput(mInputText, 0);
		}
	}

	/**
	 * Hides the soft input if it is active for the input text.
	 */
	private void hideSoftInput() {
		InputMethodManager inputMethodManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null && inputMethodManager.isActive(mInputText)) {
			inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
			mInputText.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Computes the max width if no such specified as an attribute.
	 */
	private void tryComputeMaxHeight() {
		// Text Height is constant, in mTextSize
		if (!mComputeMaxHeight) {
			return;
		}
		/*
        int maxTextWidth = 0;
        if (mDisplayedValues == null) {
            float maxDigitWidth = 0;
            for (int i = 0; i <= 9; i++) {
                final float digitWidth = mSelectorWheelPaint.measureText(formatNumberWithLocale(i));
                if (digitWidth > maxDigitWidth) {
                    maxDigitWidth = digitWidth;
                }
            }
            int numberOfDigits = 0;
            int current = mMaxValue;
            while (current > 0) {
                numberOfDigits++;
                current = current / 10;
            }
            maxTextWidth = (int) (numberOfDigits * maxDigitWidth);
        } else {
            final int valueCount = mDisplayedValues.length;
            for (int i = 0; i < valueCount; i++) {
                final float textWidth = mSelectorWheelPaint.measureText(mDisplayedValues[i]);
                if (textWidth > maxTextWidth) {
                    maxTextWidth = (int) textWidth;
                }
            }
        }
        maxTextWidth += mInputText.getPaddingLeft() + mInputText.getPaddingRight();
        if (mMaxWidth != maxTextWidth) {
            if (maxTextWidth > mMinWidth) {
                mMaxWidth = maxTextWidth;
            } else {
                mMaxWidth = mMinWidth;
            }
            invalidate();
        }
		 */
		int maxTextHeight = mTextSize + mInputText.getPaddingTop() + mInputText.getPaddingBottom();
		if (mMaxHeight != maxTextHeight) {
			if (maxTextHeight > mMinHeight) {
				mMaxHeight = maxTextHeight;
			} else {
				mMaxHeight = mMinHeight;
			}
			invalidate();
		}
	}

	/**
	 * Sets the max value of the picker.
	 *
	 * @param maxValue The max value.
	 */
	@Override
	public void numberOfFrames(int maxValue) {
		updateInputTextView();
		tryComputeMaxHeight();
		invalidate();
	}

	@Override
	public void animationDirty(boolean dirty) {

	}

	@Override
	public void frameChanged(int frame) {

	}

	@Override
	public void redrawTrack(int track) {

	}

	@Override
	protected float getLeftFadingEdgeStrength() {
		return LEFT_AND_RIGHT_FADING_EDGE_STRENGTH;
	}

	@Override
	protected float getRightFadingEdgeStrength() {
		return LEFT_AND_RIGHT_FADING_EDGE_STRENGTH;
	}

	@Override
	protected void onDetachedFromWindow() {
		removeAllCallbacks();
	}

	private float pixelToTime(float x) {
		return playback.getTime() + (x - getWidth()/2f) * playback.getScreenDensity();
	}

	private float timeToPixel(float t) {
		return getWidth()/2f + (t - playback.getTime()) / playback.getScreenDensity();
	}

	@Override
	protected void onDraw(Canvas canvas) {
//		if (true) return;
 		float leftTime = Math.max(pixelToTime(0f), playback.playbackStartTime());
 		float rightTime = Math.min(pixelToTime(getWidth()), playback.playbackEndTime());
 		float minorFrameDuration = playback.getAnimation().frameTime() * minorEvery;
 		float minorFrameSpacing = minorFrameDuration / playback.getScreenDensity();
 		float leftFrameTime = playback.roundTimeToMultipleOf(leftTime, minorFrameDuration, -1);
 		float right = timeToPixel(rightTime);

 		float x = timeToPixel(leftFrameTime);
 		int frame = playback.getNearestFrame(leftFrameTime);
 /*
 		Log.d(TAG, "minorFrameSpacing: " + minorFrameSpacing + "; left: " + x + "; right: "
 		+ right + "; minorFrameDuration: " + minorFrameDuration +
 		"; leftTime: " + leftTime + "; rightTime: " + rightTime);
 */

		for (; x <= right; frame += minorEvery, x += minorFrameSpacing) {
			float top;
			if (frame % majorEvery == 0) {
				top = majorTop;
				canvas.drawText(Integer.toString(frame), x + textOffset, textBottom, mSelectorWheelPaint);
			} else {
				top = minorTop;
			}

			canvas.drawLine(x, bottom, x, top, mSelectorWheelPaint);
		}

		canvas.drawLine(getWidth()/2f, 0f, getWidth()/2f, getHeight(), mHairlinePaint);
		canvas.drawText("f=" + playback.getNearestFrame(playback.getTime()) + " t=" + playback.getTime(),
				getWidth()/2, mTextSize, mSelectorWheelPaint);
	}

	/**
	 * Makes a measure spec that tries greedily to use the max value.
	 *
	 * @param measureSpec The measure spec.
	 * @param maxSize The max value for the size.
	 * @return A measure spec greedily imposing the max size.
	 */
	private int makeMeasureSpec(int measureSpec, int maxSize) {
		if (maxSize == SIZE_UNSPECIFIED) {
			return measureSpec;
		}
		final int size = MeasureSpec.getSize(measureSpec);
		final int mode = MeasureSpec.getMode(measureSpec);
		switch (mode) {
		case MeasureSpec.EXACTLY:
			return measureSpec;
		case MeasureSpec.AT_MOST:
			return MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), MeasureSpec.EXACTLY);
		case MeasureSpec.UNSPECIFIED:
			return MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY);
		default:
			throw new IllegalArgumentException("Unknown measure mode: " + mode);
		}
	}

	/**
	 * Utility to reconcile a desired size and state, with constraints imposed
	 * by a MeasureSpec. Tries to respect the min size, unless a different size
	 * is imposed by the constraints.
	 *
	 * @param minSize The minimal desired size.
	 * @param measuredSize The currently measured size.
	 * @param measureSpec The current measure spec.
	 * @return The resolved size and state.
	 */
	private int resolveSizeAndStateRespectingMinSize(
			int minSize, int measuredSize, int measureSpec) {
		if (minSize != SIZE_UNSPECIFIED) {
			final int desiredWidth = Math.max(minSize, measuredSize);
			return resolveSize(desiredWidth, measureSpec);
		} else {
			return measuredSize;
		}
	}

	float bottom;
	float minorTop;
	float majorTop;
	float textBottom;
	float textOffset;
	int minorEvery; // frames / tick
	int majorEvery; // frames / tick

	private void updateCachedMetrics() {
		textOffset = dp2px(2);
		bottom = getHeight();
		minorTop = bottom - dp2px(10);
		majorTop = bottom - dp2px(20);
		textBottom = getHeight() - dp2px(15);
		minorEvery = 1; // frames / marker
		majorEvery = 5; // frames / marker
	}

	private void initializeSelectorWheel() {
		playback.setScreenDensity(1f/30/dp2px(10)); // 10 dp between frames at 30fps

		updateCachedMetrics();
		updateInputTextView();
	}

	private void initializeFadingEdges() {
		setHorizontalFadingEdgeEnabled(true);
		setFadingEdgeLength((getWidth() - mTextSize) / 2);
	}

	/**
	 * Flings the selector with the given <code>velocityY</code>.
	 */
	private void fling(int velocityX) {
		playback.fling(velocityX);
		invalidate();
	}

	private String formatNumber(float mValue2) {
		return formatNumberWithLocale(mValue2);
	}

	private void validateInputTextView(View v) {
		String str = String.valueOf(((TextView) v).getText());
		if (TextUtils.isEmpty(str)) {
			// Restore to the old value as we don't allow empty values
			updateInputTextView();
		} else {
			// Check the new value and ensure it's in range
			float current = getSelectedPos(str.toString());
			playback.setTime(current);
			invalidate();
		}
	}

	/**
	 * Updates the view of this NumberPicker. If displayValues were specified in
	 * the string corresponding to the index specified by the current value will
	 * be returned. Otherwise, the formatter specified in {@link #setFormatter}
	 * will be used to format the number.
	 *
	 * @return Whether the text was updated.
	 */
	private boolean updateInputTextView() {
		/*
		 * If we don't have displayed values then use the current number else
		 * find the correct value in the displayed values for the current
		 * number.
		 */
		String text = formatNumber(playback.getNearestFrame(playback.getTime()));
		if (!TextUtils.isEmpty(text) && !text.equals(mInputText.getText().toString())) {
			mInputText.setText(text);
			return true;
		}

		return false;
	}

	/**
	 * Removes all pending callback from the message queue.
	 */
	private void removeAllCallbacks() {
		if (mSetSelectionCommand != null) {
			removeCallbacks(mSetSelectionCommand);
		}
	}

	/**
	 * @return The selected index given its displayed <code>value</code>.
	 */
	private float getSelectedPos(String value) {

		try {
			return playback.getFrameTime(Integer.parseInt(value));
		} catch (NumberFormatException e) {
			// Ignore as if it's not a number we don't care
		}
		return playback.getTime();
	}

	/**
	 * Posts an {@link SetSelectionCommand} from the given <code>selectionStart
	 * </code> to <code>selectionEnd</code>.
	 */
	private void setSelection(int selectionStart, int selectionEnd) {
		if (mSetSelectionCommand == null) {
			mSetSelectionCommand = new SetSelectionCommand();
		} else {
			removeCallbacks(mSetSelectionCommand);
		}
		mSetSelectionCommand.mSelectionStart = selectionStart;
		mSetSelectionCommand.mSelectionEnd = selectionEnd;
		post(mSetSelectionCommand);
	}

	/**
	 * The numbers accepted by the input text's {@link Filter}
	 */
	private static final char[] DIGIT_CHARACTERS = new char[] {
		// Latin digits are the common case
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		// Arabic-Indic
		'\u0660', '\u0661', '\u0662', '\u0663', '\u0664', '\u0665', '\u0666', '\u0667', '\u0668'
		, '\u0669',
		// Extended Arabic-Indic
		'\u06f0', '\u06f1', '\u06f2', '\u06f3', '\u06f4', '\u06f5', '\u06f6', '\u06f7', '\u06f8'
		, '\u06f9'
	};

	/**
	 * Filter for accepting only valid indices or prefixes of the string
	 * representation of valid indices.
	 */
	class InputTextFilter extends NumberKeyListener {

		// XXX This doesn't allow for range limits when controlled by a
		// soft input method!
		public int getInputType() {
			return InputType.TYPE_CLASS_TEXT;
		}

		@Override
		protected char[] getAcceptedChars() {
			return DIGIT_CHARACTERS;
		}

		@Override
		public CharSequence filter(
				CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
			if (filtered == null) {
				filtered = source.subSequence(start, end);
			}

			String result = String.valueOf(dest.subSequence(0, dstart)) + filtered
					+ dest.subSequence(dend, dest.length());

			if ("".equals(result)) {
				return result;
			}
			float val = getSelectedPos(result);

			/*
			 * Ensure the user can't type in a value greater than the max
			 * allowed. We have to allow less than min as the user might
			 * want to delete some numbers and then type a new number.
			 */
			if (val > playback.animEndTime()) {
				return "";
			} else {
				return filtered;
			}
		}
	}

	/**
	 * Command for setting the input text selection.
	 */
	class SetSelectionCommand implements Runnable {
		private int mSelectionStart;

		private int mSelectionEnd;

		public void run() {
			mInputText.setSelection(mSelectionStart, mSelectionEnd);
		}
	}

	/**
	 * @hide
	 */
	public static class CustomEditText extends EditText {

		public CustomEditText(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		public void onEditorAction(int actionCode) {
			super.onEditorAction(actionCode);
			if (actionCode == EditorInfo.IME_ACTION_DONE) {
				clearFocus();
			}
		}
	}

	static private String formatNumberWithLocale(float mValue2) {
		return String.format(Locale.getDefault(), "%f", mValue2);
	}
}
