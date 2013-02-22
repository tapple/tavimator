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

public class FramePicker extends LinearLayout {

	/**
	 * The number of items show in the selector wheel.
	 */
	private int SELECTOR_WHEEL_ITEM_COUNT = 3;

	/**
	 * The default update interval during long press.
	 */
	private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300;

	/**
	 * The index of the middle selector item.
	 */
	private int SELECTOR_MIDDLE_ITEM_INDEX = SELECTOR_WHEEL_ITEM_COUNT / 2;

	/**
	 * The coefficient by which to adjust (divide) the max fling velocity.
	 */
	private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8;

	/**
	 * The the duration for adjusting the selector wheel.
	 */
	private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800;

	/**
	 * The duration of scrolling while snapping to a given position.
	 */
	private static final int SNAP_SCROLL_DURATION = 300;

	/**
	 * The strength of fading in the top and bottom while drawing the selector.
	 */
	private static final float LEFT_AND_RIGHT_FADING_EDGE_STRENGTH = 0.9f;

	/**
	 * The default unscaled height of the selection divider.
	 */
	private static final int UNSCALED_DEFAULT_SELECTION_DIVIDER_WIDTH = 2;

	/**
	 * The default unscaled distance between the selection dividers.
	 */
	private static final int UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE = 48;

	/**
	 * The resource id for the default layout.
	 */
	private static final int DEFAULT_LAYOUT_RESOURCE_ID = R.layout.frame_picker;

	/**
	 * Constant for unspecified size.
	 */
	private static final int SIZE_UNSPECIFIED = -1;

	/**
	 * The text for showing the current value.
	 */
	private final EditText mInputText;

	/**
	 * The distance between the two selection dividers.
	 */
	private final int mSelectionDividersDistance;

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
	 * Flag whether to compute the max width.
	 */
	private final boolean mComputeMaxHeight;

	/**
	 * The height of the text.
	 */
	private final int mTextSize;

	/**
	 * Lower value of the range of numbers allowed for the NumberPicker
	 */
	private int mMinValue;

	/**
	 * Upper value of the range of numbers allowed for the NumberPicker
	 */
	private int mMaxValue;

	/**
	 * Current value of this NumberPicker
	 */
	private float mValue;

	/**
	 * Listener to be notified upon current value change.
	 */
	private OnValueChangeListener mOnValueChangeListener;

	/**
	 * Listener to be notified upon scroll state change.
	 */
	private OnScrollListener mOnScrollListener;

	/**
	 * The speed for updating the value form long press.
	 */
	private long mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;

	/**
	 * The {@link Paint} for drawing the selector.
	 */
	private final Paint mSelectorWheelPaint;

	/**
	 * The {@link Drawable} for pressed virtual (increment/decrement) buttons.
	 */
	private final Drawable mVirtualButtonPressedDrawable;

	/**
	 * The {@link Scroller} responsible for flinging the selector.
	 */
	private final Scroller mFlingScroller;

	/**
	 * The {@link Scroller} responsible for adjusting the selector.
	 */
	private final Scroller mAdjustScroller;

	/**
	 * The previous Y coordinate while scrolling the selector.
	 */
	private int mPreviousScrollerX;

	/**
	 * Handle to the reusable command for setting the input text selection.
	 */
	private SetSelectionCommand mSetSelectionCommand;

	/**
	 * Handle to the reusable command for changing the current value from long
	 * press by one.
	 */
	private ChangeCurrentByOneFromLongPressCommand mChangeCurrentByOneFromLongPressCommand;

	/**
	 * Command for beginning an edit of the current value via IME on long press.
	 */
	private BeginSoftInputOnLongPressCommand mBeginSoftInputOnLongPressCommand;

	/**
	 * The Y position of the last down event.
	 */
	private float mLastDownEventX;

	/**
	 * The time of the last down event.
	 */
	private long mLastDownEventTime;

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
	 * Flag whether the selector should wrap around.
	 */
	private boolean mWrapSelectorWheel;

	/**
	 * The back ground color used to optimize scroller fading.
	 */
	private final int mSolidColor;

	/**
	 * Divider for showing item to be selected while scrolling
	 */
	private final Drawable mSelectionDivider;

	/**
	 * The height of the selection divider.
	 */
	private final int mSelectionDividerWidth;

	/**
	 * The current scroll state of the number picker.
	 */
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	/**
	 * Flag whether to ignore move events - we ignore such when we show in IME
	 * to prevent the content from scrolling.
	 */
	private boolean mIngonreMoveEvents;

	/**
	 * Flag whether to show soft input on tap. (aka, the keyboard)
	 */
	private boolean mShowSoftInputOnTap;

	/**
	 * The top of the top selection divider.
	 */
	private int mLeftSelectionDividerLeft;

	/**
	 * The bottom of the bottom selection divider.
	 */
	private int mRightSelectionDividerRight;

	/**
	 * Whether the increment virtual button is pressed.
	 */
	private boolean mIncrementVirtualButtonPressed;

	/**
	 * Whether the decrement virtual button is pressed.
	 */
	private boolean mDecrementVirtualButtonPressed;

	/**
	 * Helper class for managing pressed state of the virtual buttons.
	 */
	private final PressedStateHelper mPressedStateHelper;

	/**
	 * Interface to listen for changes of the current value.
	 */
	public interface OnValueChangeListener {

		/**
		 * Called upon a change of the current value.
		 *
		 * @param picker The NumberPicker associated with this listener.
		 * @param previous The previous value.
		 * @param mValue The new value.
		 */
		void onValueChange(FramePicker picker, float previous, float mValue);
	}

	/**
	 * Interface to listen for the picker scroll state.
	 */
	public interface OnScrollListener {

		/**
		 * The view is not scrolling.
		 */
		public static int SCROLL_STATE_IDLE = 0;

		/**
		 * The user is scrolling using touch, and his finger is still on the screen.
		 */
		public static int SCROLL_STATE_TOUCH_SCROLL = 1;

		/**
		 * The user had previously been scrolling using touch and performed a fling.
		 */
		public static int SCROLL_STATE_FLING = 2;

		/**
		 * Callback invoked while the number picker scroll state has changed.
		 *
		 * @param view The view whose scroll state is being reported.
		 * @param scrollState The current scroll state. One of
		 *            {@link #SCROLL_STATE_IDLE},
		 *            {@link #SCROLL_STATE_TOUCH_SCROLL} or
		 *            {@link #SCROLL_STATE_IDLE}.
		 */
		public void onScrollStateChange(FramePicker view, int scrollState);
	}

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
	public FramePicker(Context context) {
		this(context, null);
	}

	/**
	 * Create a new number picker.
	 *
	 * @param context The application environment.
	 * @param attrs A collection of attributes.
	 */
	public FramePicker(Context context, AttributeSet attrs) {
		super(context, attrs);

		// process style attributes
		TypedArray attributesArray = context.obtainStyledAttributes(
				attrs, R.styleable.FramePicker, R.attr.framePickerStyle, 0);
		final int layoutResId = DEFAULT_LAYOUT_RESOURCE_ID;

		mSolidColor = attributesArray.getColor(R.styleable.FramePicker_solidColor, 0);

		mSelectionDivider = attributesArray.getDrawable(R.styleable.FramePicker_selectionDivider);

		final int defSelectionDividerWidth = (int) dp2px(UNSCALED_DEFAULT_SELECTION_DIVIDER_WIDTH);
		mSelectionDividerWidth = attributesArray.getDimensionPixelSize(
				R.styleable.FramePicker_selectionDividerWidth, defSelectionDividerWidth);

		final int defSelectionDividerDistance = (int) dp2px(UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE);
		mSelectionDividersDistance = attributesArray.getDimensionPixelSize(
				R.styleable.FramePicker_selectionDividersDistance, defSelectionDividerDistance);

		mMinHeight = attributesArray.getDimensionPixelSize(
				R.styleable.FramePicker_internalMinHeight, SIZE_UNSPECIFIED);

		mMaxHeight = attributesArray.getDimensionPixelSize(
				R.styleable.FramePicker_internalMaxHeight, SIZE_UNSPECIFIED);
		if (mMinHeight != SIZE_UNSPECIFIED && mMaxHeight != SIZE_UNSPECIFIED
				&& mMinHeight > mMaxHeight) {
			throw new IllegalArgumentException("minHeight > maxHeight");
		}

		mMinWidth = attributesArray.getDimensionPixelSize(
				R.styleable.FramePicker_internalMinWidth, SIZE_UNSPECIFIED);

		mMaxWidth = attributesArray.getDimensionPixelSize(
				R.styleable.FramePicker_internalMaxWidth, SIZE_UNSPECIFIED);
		if (mMinWidth != SIZE_UNSPECIFIED && mMaxWidth != SIZE_UNSPECIFIED
				&& mMinWidth > mMaxWidth) {
			throw new IllegalArgumentException("minWidth > maxWidth");
		}

		mComputeMaxHeight = (mMaxHeight == SIZE_UNSPECIFIED);

		mVirtualButtonPressedDrawable = attributesArray.getDrawable(
				R.styleable.FramePicker_virtualButtonPressedDrawable);

		attributesArray.recycle();

		mPressedStateHelper = new PressedStateHelper();

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

		// create the fling and adjust scrollers
		mFlingScroller = new Scroller(getContext(), null);
		mAdjustScroller = new Scroller(getContext(), new DecelerateInterpolator(2.5f));

		updateInputTextView();
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
			mLeftSelectionDividerLeft = (getWidth() - mSelectionDividersDistance) / 2
					- mSelectionDividerWidth;
			mRightSelectionDividerRight = mLeftSelectionDividerLeft + 2 * mSelectionDividerWidth
					+ mSelectionDividersDistance;
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

	/**
	 * Move to the final position of a scroller. Ensures to force finish the scroller
	 * and if it is not at its final position a scroll of the selector wheel is
	 * performed to fast forward to the final position.
	 *
	 * @param scroller The scroller to whose final position to get.
	 * @return True of the a move was performed, i.e. the scroller was not in final position.
	 */
	private boolean moveToFinalScrollerPosition(Scroller scroller) {
		scroller.forceFinished(true);
		int amountToScroll = scroller.getFinalX() - scroller.getCurrX();
		int finalValue = (int) (mValue + amountToScroll/mFrameSpacing + 0.5f);
		if (finalValue != mValue) {
			setValueInternal(finalValue, true);
			return true;
		}
		return false;
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
			mInputText.setVisibility(View.INVISIBLE);
			mLastDownOrMoveEventX = mLastDownEventX = event.getX();
			mLastDownEventTime = event.getEventTime();
			mIngonreMoveEvents = false;
			mShowSoftInputOnTap = false;
			// Handle pressed state before any state change.
			if (mLastDownEventX < mLeftSelectionDividerLeft) {
				if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					mPressedStateHelper.buttonPressDelayed(
							PressedStateHelper.BUTTON_DECREMENT);
				}
			} else if (mLastDownEventX > mRightSelectionDividerRight) {
				if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					mPressedStateHelper.buttonPressDelayed(
							PressedStateHelper.BUTTON_INCREMENT);
				}
			}
			// Make sure we support flinging inside scrollables.
			getParent().requestDisallowInterceptTouchEvent(true);
			if (!mFlingScroller.isFinished()) {
				mFlingScroller.forceFinished(true);
				mAdjustScroller.forceFinished(true);
				onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
			} else if (!mAdjustScroller.isFinished()) {
				mFlingScroller.forceFinished(true);
				mAdjustScroller.forceFinished(true);
			} else if (mLastDownEventX < mLeftSelectionDividerLeft) {
				hideSoftInput();
				postChangeCurrentByOneFromLongPress(
						false, ViewConfiguration.getLongPressTimeout());
			} else if (mLastDownEventX > mRightSelectionDividerRight) {
				hideSoftInput();
				postChangeCurrentByOneFromLongPress(
						true, ViewConfiguration.getLongPressTimeout());
			} else {
				mShowSoftInputOnTap = true;
				postBeginSoftInputOnLongPressCommand();
			}
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
			if (mIngonreMoveEvents) {
				break;
			}
			float currentMoveX = event.getX();
			if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
				int deltaDownX = (int) Math.abs(currentMoveX - mLastDownEventX);
				if (deltaDownX > mTouchSlop) {
					removeAllCallbacks();
					onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
				}
			} else {
				int deltaMoveX = (int) ((currentMoveX - mLastDownOrMoveEventX));
				scrollBy(deltaMoveX, 0);
				invalidate();
			}
			mLastDownOrMoveEventX = currentMoveX;
		} break;
		case MotionEvent.ACTION_UP: {
			removeBeginSoftInputCommand();
			removeChangeCurrentByOneFromLongPress();
			mPressedStateHelper.cancel();
			VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
			int initialVelocity = (int) velocityTracker.getXVelocity();
			if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
				fling(initialVelocity);
				onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
			} else {
				int eventX = (int) event.getX();
				int deltaMoveX = (int) Math.abs(eventX - mLastDownEventX);
				long deltaTime = event.getEventTime() - mLastDownEventTime;
				if (deltaMoveX <= mTouchSlop && deltaTime < ViewConfiguration.getTapTimeout()) {
					if (mShowSoftInputOnTap) {
						mShowSoftInputOnTap = false;
						showSoftInput();
					} else {
						float selectorIndexOffset = (eventX / mFrameSpacing)
								- SELECTOR_MIDDLE_ITEM_INDEX;
						if (selectorIndexOffset > 0) {
							changeValueByOne(true);
							mPressedStateHelper.buttonTapped(
									PressedStateHelper.BUTTON_INCREMENT);
						} else if (selectorIndexOffset < 0) {
							changeValueByOne(false);
							mPressedStateHelper.buttonTapped(
									PressedStateHelper.BUTTON_DECREMENT);
						}
					}
				} else {
					ensureScrollWheelAdjusted();
				}
				onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
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
		Scroller scroller = mFlingScroller;
		if (scroller.isFinished()) {
			scroller = mAdjustScroller;
			if (scroller.isFinished()) {
				return;
			}
		}
		scroller.computeScrollOffset();
		int currentScrollerX = scroller.getCurrX();
		if (mPreviousScrollerX == 0) {
			mPreviousScrollerX = scroller.getStartX();
		}
		scrollBy(currentScrollerX - mPreviousScrollerX, 0);
		mPreviousScrollerX = currentScrollerX;
		if (scroller.isFinished()) {
			onScrollerFinished(scroller);
		} else {
			invalidate();
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mInputText.setEnabled(enabled);
	}

	@Override
	public void scrollBy(int x, int y) {
		scrollBy((float)x, (float)y);
	}

	public void scrollBy(float x, float y) {
		setValueInternal(mValue - x/mFrameSpacing, true);
	}

	@Override
	public int getSolidColor() {
		return mSolidColor;
	}

	/**
	 * Sets the listener to be notified on change of the current value.
	 *
	 * @param onValueChangedListener The listener.
	 */
	 public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
		mOnValueChangeListener = onValueChangedListener;
	}

	/**
	 * Set listener to be notified for scroll state changes.
	 *
	 * @param onScrollListener The listener.
	 */
	 public void setOnScrollListener(OnScrollListener onScrollListener) {
		 mOnScrollListener = onScrollListener;
	 }

	 /**
	  * Set the current value for the number picker.
	  * <p>
	  * If the argument is less than the {@link FramePicker#getMinValue()} and
	  * {@link FramePicker#getWrapSelectorWheel()} is <code>false</code> the
	  * current value is set to the {@link FramePicker#getMinValue()} value.
	  * </p>
	  * <p>
	  * If the argument is less than the {@link FramePicker#getMinValue()} and
	  * {@link FramePicker#getWrapSelectorWheel()} is <code>true</code> the
	  * current value is set to the {@link FramePicker#getMaxValue()} value.
	  * </p>
	  * <p>
	  * If the argument is less than the {@link FramePicker#getMaxValue()} and
	  * {@link FramePicker#getWrapSelectorWheel()} is <code>false</code> the
	  * current value is set to the {@link FramePicker#getMaxValue()} value.
	  * </p>
	  * <p>
	  * If the argument is less than the {@link FramePicker#getMaxValue()} and
	  * {@link FramePicker#getWrapSelectorWheel()} is <code>true</code> the
	  * current value is set to the {@link FramePicker#getMinValue()} value.
	  * </p>
	  *
	  * @param value The current value.
	  * @see #setWrapSelectorWheel(boolean)
	  * @see #setMinValue(int)
	  * @see #setMaxValue(int)
	  */
	 public void setValue(int value) {
		 setValueInternal(value, false);
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
	  * Gets whether the selector wheel wraps when reaching the min/max value.
	  *
	  * @return True if the selector wheel wraps.
	  *
	  * @see #getMinValue()
	  * @see #getMaxValue()
	  */
	 public boolean getWrapSelectorWheel() {
		 return mWrapSelectorWheel;
	 }

	 /**
	  * Sets whether the selector wheel shown during flinging/scrolling should
	  * wrap around the {@link FramePicker#getMinValue()} and
	  * {@link FramePicker#getMaxValue()} values.
	  * <p>
	  * By default if the range (max - min) is more than the number of items shown
	  * on the selector wheel the selector wheel wrapping is enabled.
	  * </p>
	  * <p>
	  * <strong>Note:</strong> If the number of items, i.e. the range (
	  * {@link #getMaxValue()} - {@link #getMinValue()}) is less than
	  * the number of items shown on the selector wheel, the selector wheel will
	  * not wrap. Hence, in such a case calling this method is a NOP.
	  * </p>
	  *
	  * @param wrapSelectorWheel Whether to wrap.
	  */
	 public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
		 mWrapSelectorWheel = wrapSelectorWheel;
	 }

	 /**
	  * Sets the speed at which the numbers be incremented and decremented when
	  * the up and down buttons are long pressed respectively.
	  * <p>
	  * The default value is 300 ms.
	  * </p>
	  *
	  * @param intervalMillis The speed (in milliseconds) at which the numbers
	  *            will be incremented and decremented.
	  */
	 public void setOnLongPressUpdateInterval(long intervalMillis) {
		 mLongPressUpdateInterval = intervalMillis;
	 }

	 /**
	  * Returns the value of the picker.
	  *
	  * @return The value.
	  */
	 public float getValue() {
		 return mValue;
	 }

	 /**
	  * Returns the rounded value of the picker.
	  *
	  * @return The value.
	  */
	 public int getRoundedValue() {
		 return (int)(mValue + 0.5f);
	 }

	 /**
	  * Returns the min value of the picker.
	  *
	  * @return The min value
	  */
	 public int getMinValue() {
		 return mMinValue;
	 }

	 /**
	  * Sets the min value of the picker.
	  *
	  * @param minValue The min value.
	  */
	 public void setMinValue(int minValue) {
		 if (mMinValue == minValue) {
			 return;
		 }
		 if (minValue < 0) {
			 throw new IllegalArgumentException("minValue must be >= 0");
		 }
		 mMinValue = minValue;
		 if (mMinValue > mValue) {
			 mValue = mMinValue;
		 }
		 updateInputTextView();
		 tryComputeMaxHeight();
		 invalidate();
	 }

	 /**
	  * Returns the max value of the picker.
	  *
	  * @return The max value.
	  */
	 public int getMaxValue() {
		 return mMaxValue;
	 }

	 /**
	  * Sets the max value of the picker.
	  *
	  * @param maxValue The max value.
	  */
	 public void setMaxValue(int maxValue) {
		 if (mMaxValue == maxValue) {
			 return;
		 }
		 if (maxValue < 0) {
			 throw new IllegalArgumentException("maxValue must be >= 0");
		 }
		 mMaxValue = maxValue;
		 if (mMaxValue < mValue) {
			 mValue = mMaxValue;
		 }
		 updateInputTextView();
		 tryComputeMaxHeight();
		 invalidate();
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


	 protected int[] PRESSED_STATE_SET = new int[]{android.R.attr.state_pressed};

	 @Override
	 protected void onDraw(Canvas canvas) {
		 int firstFrame = (int)getValue() - SELECTOR_MIDDLE_ITEM_INDEX;
		 int lastFrame = firstFrame + SELECTOR_WHEEL_ITEM_COUNT;
		 float mCurrentScrollOffset = getWidth()/2f + (firstFrame - mValue) * mFrameSpacing;
		 float x = mCurrentScrollOffset;
		 float y = mInputText.getBaseline() + mInputText.getTop();

		 // draw the virtual buttons pressed state if needed
		 if (mVirtualButtonPressedDrawable != null
				 && mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			 if (mDecrementVirtualButtonPressed) {
				 mVirtualButtonPressedDrawable.setState(PRESSED_STATE_SET);
				 mVirtualButtonPressedDrawable.setBounds(0, 0, mLeftSelectionDividerLeft, getBottom());
				 mVirtualButtonPressedDrawable.draw(canvas);
			 }
			 if (mIncrementVirtualButtonPressed) {
				 mVirtualButtonPressedDrawable.setState(PRESSED_STATE_SET);
				 mVirtualButtonPressedDrawable.setBounds(mRightSelectionDividerRight, 0, getRight(),
						 getBottom());
				 mVirtualButtonPressedDrawable.draw(canvas);
			 }
		 }

		 /*
    	// draw the selector wheel
        int[] selectorIndices = mSelectorIndices;
        for (int i = 0; i < selectorIndices.length; i++) {
            int selectorIndex = selectorIndices[i];
            String scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex);
            // Do not draw the middle item if input is visible since the input
            // is shown only if the wheel is static and it covers the middle
            // item. Otherwise, if the user starts editing the text via the
            // IME he may see a dimmed version of the old value intermixed
            // with the new one.
            if (i != SELECTOR_MIDDLE_ITEM_INDEX || mInputText.getVisibility() != VISIBLE) {
                canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint);
            }
            x += mSelectorElementWidth;
        }
		  */

		 for (int i = firstFrame; i < lastFrame; i++) {
			 float top;
			 if (i % majorEvery == 0) {
				 top = majorTop;
				 canvas.drawText(Integer.toString(i), x + textOffset, textBottom, mSelectorWheelPaint);
			 } else if (i % minorEvery == 0) {
				 top = minorTop;
			 } else continue;

			 canvas.drawLine(x, bottom, x, top, mSelectorWheelPaint);

			 x += mFrameSpacing;
		 }

		 // draw the selection dividers
		 if (mSelectionDivider != null) {
			 // draw the top divider
			 int leftOfLeftDivider = mLeftSelectionDividerLeft;
			 int rightOfLeftDivider = leftOfLeftDivider + mSelectionDividerWidth;
			 mSelectionDivider.setBounds(leftOfLeftDivider, 0, rightOfLeftDivider, getBottom());
			 mSelectionDivider.draw(canvas);

			 // draw the bottom divider
			 int rightOfRightDivider = mRightSelectionDividerRight;
			 int leftOfRightDivider = rightOfRightDivider - mSelectionDividerWidth;
			 mSelectionDivider.setBounds(leftOfRightDivider, 0, rightOfRightDivider, getBottom());
			 mSelectionDivider.draw(canvas);
		 }
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

	 /**
	  * Sets the current value of this NumberPicker.
	  *
	  * @param current The new value of the NumberPicker.
	  * @param notifyChange Whether to notify if the current value changed.
	  */
	 private void setValueInternal(float current, boolean notifyChange) {
		 if (mValue == current) {
			 return;
		 }
		 current = getWrappedSelectorIndex(current);
		 float previous = mValue;
		 mValue = current;
		 onValueChanged(previous, current, notifyChange);
	 }
	 
	 private void onValueChanged(float previous, float current, boolean notifyChange) {
		 updateInputTextView();
		 if (notifyChange) {
			 notifyChange(previous, current);
		 }
		 invalidate();
	 }

	 /**
	  * Changes the current value by one which is increment or
	  * decrement based on the passes argument.
	  * decrement the current value.
	  *
	  * @param increment True to increment, false to decrement.
	  */
	 private void changeValueByOne(boolean increment) {
		 // TODO: This is incorrect if the frame spacing is less than one pixel
		 mInputText.setVisibility(View.INVISIBLE);
		 if (!moveToFinalScrollerPosition(mFlingScroller)) {
			 moveToFinalScrollerPosition(mAdjustScroller);
		 }
		 mPreviousScrollerX = 0;
		 if (increment) {
			 mFlingScroller.startScroll(0, 0, -(int)mFrameSpacing, 0, SNAP_SCROLL_DURATION);
		 } else {
			 mFlingScroller.startScroll(0, 0, (int)mFrameSpacing, 0, SNAP_SCROLL_DURATION);
		 }
		 invalidate();
	 }

	 private float mFrameSpacing; // pixels per frame

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
		 mFrameSpacing = dp2px(10);
		 SELECTOR_WHEEL_ITEM_COUNT = (int) (getWidth() / mFrameSpacing) + 2;
		 SELECTOR_MIDDLE_ITEM_INDEX = SELECTOR_WHEEL_ITEM_COUNT / 2;

		 updateCachedMetrics();
		 updateInputTextView();
	 }

	 private void initializeFadingEdges() {
		 setHorizontalFadingEdgeEnabled(true);
		 setFadingEdgeLength((getWidth() - mTextSize) / 2);
	 }

	 /**
	  * Callback invoked upon completion of a given <code>scroller</code>.
	  */
	 private void onScrollerFinished(Scroller scroller) {
		 if (scroller == mFlingScroller) {
			 if (!ensureScrollWheelAdjusted()) {
				 updateInputTextView();
			 }
			 onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
		 } else {
			 if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
				 updateInputTextView();
			 }
		 }
	 }

	 /**
	  * Handles transition to a given <code>scrollState</code>
	  */
	 private void onScrollStateChange(int scrollState) {
		 if (mScrollState == scrollState) {
			 return;
		 }
		 mScrollState = scrollState;
		 if (mOnScrollListener != null) {
			 mOnScrollListener.onScrollStateChange(this, scrollState);
		 }
	 }

	 /**
	  * Flings the selector with the given <code>velocityY</code>.
	  */
	 private void fling(int velocityX) {
		 mPreviousScrollerX = 0;

		 if (velocityX > 0) {
			 mFlingScroller.fling(0, 0, velocityX, 0, 0, Integer.MAX_VALUE, 0, 0);
		 } else {
			 mFlingScroller.fling(Integer.MAX_VALUE, 0, velocityX, 0, 0, Integer.MAX_VALUE, 0, 0);
		 }

		 invalidate();
	 }

	 /**
	  * @return The wrapped index <code>selectorIndex</code> value.
	  */
	 private float getWrappedSelectorIndex(float selectorIndex) {
		 if (selectorIndex > mMaxValue) {
			 if (mWrapSelectorWheel) {
				 return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue);
			 } else {
				 return mMaxValue;
			 }
		 } else if (selectorIndex < mMinValue) {
			 if (mWrapSelectorWheel) {
				 return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue);
			 } else {
				 return mMinValue;
			 }
		 }
		 return selectorIndex;
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
			 int current = getSelectedPos(str.toString());
			 setValueInternal(current, true);
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
		 String text = formatNumber(mValue);
		 if (!TextUtils.isEmpty(text) && !text.equals(mInputText.getText().toString())) {
			 mInputText.setText(text);
			 return true;
		 }

		 return false;
	 }

	 /**
	  * Notifies the listener, if registered, of a change of the value of this
	  * NumberPicker.
	  */
	 private void notifyChange(float previous, float current) {
		 if (mOnValueChangeListener != null) {
			 mOnValueChangeListener.onValueChange(this, previous, mValue);
		 }
	 }

	 /**
	  * Posts a command for changing the current value by one.
	  *
	  * @param increment Whether to increment or decrement the value.
	  */
	 private void postChangeCurrentByOneFromLongPress(boolean increment, long delayMillis) {
		 if (mChangeCurrentByOneFromLongPressCommand == null) {
			 mChangeCurrentByOneFromLongPressCommand = new ChangeCurrentByOneFromLongPressCommand();
		 } else {
			 removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
		 }
		 mChangeCurrentByOneFromLongPressCommand.setStep(increment);
		 postDelayed(mChangeCurrentByOneFromLongPressCommand, delayMillis);
	 }

	 /**
	  * Removes the command for changing the current value by one.
	  */
	 private void removeChangeCurrentByOneFromLongPress() {
		 if (mChangeCurrentByOneFromLongPressCommand != null) {
			 removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
		 }
	 }

	 /**
	  * Posts a command for beginning an edit of the current value via IME on
	  * long press.
	  */
	 private void postBeginSoftInputOnLongPressCommand() {
		 if (mBeginSoftInputOnLongPressCommand == null) {
			 mBeginSoftInputOnLongPressCommand = new BeginSoftInputOnLongPressCommand();
		 } else {
			 removeCallbacks(mBeginSoftInputOnLongPressCommand);
		 }
		 postDelayed(mBeginSoftInputOnLongPressCommand, ViewConfiguration.getLongPressTimeout());
	 }

	 /**
	  * Removes the command for beginning an edit of the current value via IME.
	  */
	 private void removeBeginSoftInputCommand() {
		 if (mBeginSoftInputOnLongPressCommand != null) {
			 removeCallbacks(mBeginSoftInputOnLongPressCommand);
		 }
	 }

	 /**
	  * Removes all pending callback from the message queue.
	  */
	 private void removeAllCallbacks() {
		 if (mChangeCurrentByOneFromLongPressCommand != null) {
			 removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
		 }
		 if (mSetSelectionCommand != null) {
			 removeCallbacks(mSetSelectionCommand);
		 }
		 if (mBeginSoftInputOnLongPressCommand != null) {
			 removeCallbacks(mBeginSoftInputOnLongPressCommand);
		 }
		 mPressedStateHelper.cancel();
	 }

	 /**
	  * @return The selected index given its displayed <code>value</code>.
	  */
	 private int getSelectedPos(String value) {
		 try {
			 return Integer.parseInt(value);
		 } catch (NumberFormatException e) {
			 // Ignore as if it's not a number we don't care
		 }
		 return mMinValue;
	 }

	 /**
	  * Posts an {@link SetSelectionCommand} from the given <code>selectionStart
	  * </code> to <code>selectionEnd</code>.
	  */
	 private void postSetSelectionCommand(int selectionStart, int selectionEnd) {
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
			 int val = getSelectedPos(result);

			 /*
			  * Ensure the user can't type in a value greater than the max
			  * allowed. We have to allow less than min as the user might
			  * want to delete some numbers and then type a new number.
			  */
			 if (val > mMaxValue) {
				 return "";
			 } else {
				 return filtered;
			 }
		 }
	 }

	 /**
	  * Ensures that the scroll wheel is adjusted i.e. there is no offset and the
	  * middle element is in the middle of the widget.
	  *
	  * @return Whether an adjustment has been made.
	  */
	 private boolean ensureScrollWheelAdjusted() {
		 // adjust to the closest value
		 float deltaX = (getRoundedValue() - mValue) * mFrameSpacing;
		 if (deltaX != 0) {
			 mPreviousScrollerX = 0;
			 // TODO: this does not work for motions in fractions of a pixel
			 mAdjustScroller.startScroll(0, 0, (int)deltaX, 0, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
			 invalidate();
			 return true;
		 }
		 return false;
	 }

	 class PressedStateHelper implements Runnable {
		 public static final int BUTTON_INCREMENT = 1;
		 public static final int BUTTON_DECREMENT = 2;

		 private final int MODE_PRESS = 1;
		 private final int MODE_TAPPED = 2;

		 private int mManagedButton;
		 private int mMode;

		 public void cancel() {
			 mMode = 0;
			 mManagedButton = 0;
			 FramePicker.this.removeCallbacks(this);
			 if (mIncrementVirtualButtonPressed) {
				 mIncrementVirtualButtonPressed = false;
				 invalidate(mRightSelectionDividerRight, 0, getRight(), getBottom());
			 }
			 mDecrementVirtualButtonPressed = false;
			 if (mDecrementVirtualButtonPressed) {
				 invalidate(0, 0, mLeftSelectionDividerLeft, getBottom());
			 }
		 }

		 public void buttonPressDelayed(int button) {
			 cancel();
			 mMode = MODE_PRESS;
			 mManagedButton = button;
			 FramePicker.this.postDelayed(this, ViewConfiguration.getTapTimeout());
		 }

		 public void buttonTapped(int button) {
			 cancel();
			 mMode = MODE_TAPPED;
			 mManagedButton = button;
			 FramePicker.this.post(this);
		 }

		 @Override
		 public void run() {
			 switch (mMode) {
			 case MODE_PRESS: {
				 switch (mManagedButton) {
				 case BUTTON_INCREMENT: {
					 mIncrementVirtualButtonPressed = true;
					 invalidate(mRightSelectionDividerRight, 0, getRight(), getBottom());
				 } break;
				 case BUTTON_DECREMENT: {
					 mDecrementVirtualButtonPressed = true;
					 invalidate(0, 0, mLeftSelectionDividerLeft, getBottom());
				 }
				 }
			 } break;
			 case MODE_TAPPED: {
				 switch (mManagedButton) {
				 case BUTTON_INCREMENT: {
					 if (!mIncrementVirtualButtonPressed) {
						 FramePicker.this.postDelayed(this,
								 ViewConfiguration.getPressedStateDuration());
					 }
					 mIncrementVirtualButtonPressed ^= true;
					 invalidate(mRightSelectionDividerRight, 0, getRight(), getBottom());
				 } break;
				 case BUTTON_DECREMENT: {
					 if (!mDecrementVirtualButtonPressed) {
						 FramePicker.this.postDelayed(this,
								 ViewConfiguration.getPressedStateDuration());
					 }
					 mDecrementVirtualButtonPressed ^= true;
					 invalidate(0, 0, mLeftSelectionDividerLeft, getBottom());
				 }
				 }
			 } break;
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
	  * Command for changing the current value from a long press by one.
	  */
	 class ChangeCurrentByOneFromLongPressCommand implements Runnable {
		 private boolean mIncrement;

		 private void setStep(boolean increment) {
			 mIncrement = increment;
		 }

		 @Override
		 public void run() {
			 changeValueByOne(mIncrement);
			 postDelayed(this, mLongPressUpdateInterval);
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

	 /**
	  * Command for beginning soft input on long press.
	  */
	 class BeginSoftInputOnLongPressCommand implements Runnable {

		 @Override
		 public void run() {
			 showSoftInput();
			 mIngonreMoveEvents = true;
		 }
	 }

	 static private String formatNumberWithLocale(float mValue2) {
		 return String.format(Locale.getDefault(), "%f", mValue2);
	 }
}
