package org.tavatar.tavimator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AnimationActivity extends ActionBarActivity implements FramePicker.OnValueChangeListener
{
	private static final String TAG = "AnimationActivity";
	/** Hold a reference to our GLSurfaceView */
	private AnimationView mGLSurfaceView;

	private static final String SHOWED_TOAST = "showed_toast";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.animation);

		mGLSurfaceView = (AnimationView)findViewById(R.id.gl_surface_view);
		mGLSurfaceView.initializeTouchDispatcher();

		findViewById(R.id.button_grab_camera).setOnTouchListener(onGrabCameraTouched);
		findViewById(R.id.button_grab_part).setOnTouchListener(onGrabPartTouched);

/*
		FramePicker framePicker = (FramePicker) findViewById(R.id.frame_picker);
		framePicker.setMaxValue(120);
		framePicker.setWrapSelectorWheel(true);
		framePicker.setOnLongPressUpdateInterval(33);
		framePicker.setOnValueChangedListener(this);

		NumberPicker numberPicker = (NumberPicker) findViewById(R.id.number_picker);
		numberPicker.setMaxValue(120);
		numberPicker.setWrapSelectorWheel(true);
		numberPicker.setOnLongPressUpdateInterval(33);
*/
	}

	@Override
	protected void onResume() 
	{
		// The activity must call the GL surface view's onResume() on activity onResume().
		super.onResume();
		mGLSurfaceView.onResume();
	}

	@Override
	protected void onPause() 
	{
		// The activity must call the GL surface view's onPause() on activity onPause().
		super.onPause();
		mGLSurfaceView.onPause();
		isVolumeDownPressed = false;
		isVolumeUpPressed = false;

	}	

	@Override
	protected void onSaveInstanceState (Bundle outState)
	{
		outState.putBoolean(SHOWED_TOAST, true);
	}

	private boolean isVolumeDownPressed = false;
	private boolean isVolumeUpPressed = false;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return super.onKeyDown(keyCode, event);

		if (event.getRepeatCount() != 0) return true;

		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			isVolumeUpPressed = true;
			mGLSurfaceView.getRenderer().getCamera().getTrackball().setZoomRate(4.0f);
			Log.d(TAG, "keyDown: volume up");
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			isVolumeDownPressed = true;
			mGLSurfaceView.getRenderer().getCamera().getTrackball().setZoomRate(0.25f);
			Log.d(TAG, "keyDown: volume down");
		}

		if (isVolumeUpPressed && isVolumeDownPressed) {
			mGLSurfaceView.getRenderer().resetCamera();
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return super.onKeyUp(keyCode, event);

		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			isVolumeUpPressed = false;
			Log.d(TAG, "keyUp: volume up");
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			isVolumeDownPressed = false;
			Log.d(TAG, "keyUp: volume down");
		}

		mGLSurfaceView.getRenderer().getCamera().getTrackball().setZoomRate(1.0f);
		return true;
	}

	private View.OnTouchListener onGrabCameraTouched = new View.OnTouchListener() {
		@Override public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG, "Grab Camera");
				mGLSurfaceView.getRenderer().getCamera().getTrackball().trackGyroscope(
						mGLSurfaceView.getRenderer().getCamera().getGyroscope(), true);
				break;
			case MotionEvent.ACTION_UP:
				Log.d(TAG, "Release Camera");
				mGLSurfaceView.getRenderer().getCamera().getTrackball().trackGyroscope(null, false);
				break;
			}
			return false;
		}
	};

	private View.OnTouchListener onGrabPartTouched = new View.OnTouchListener() {
		@Override public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG, "Grab Part");
				mGLSurfaceView.getSelectionTrackball().trackGyroscope(
						mGLSurfaceView.getRenderer().getCamera().getGyroscope(), true);
				break;
			case MotionEvent.ACTION_UP:
				Log.d(TAG, "Release Part");
				mGLSurfaceView.getSelectionTrackball().trackGyroscope(null, false);
				break;
			}
			return false;
		}
	};

	public void onTrackingClicked(View view) {
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();


		mGLSurfaceView.getRenderer().getCamera().getGyroscope().setTracking(on);
		mGLSurfaceView.getRenderer().getCamera().getGyroscope().setSensing(on);

		if (on) {
			Log.d(TAG, "Tracking on");
		} else {
			Log.d(TAG, "Tracking off");
		}
	}

	public void onGrabConstrainClicked(View view) {
		//		mGLSurfaceView.requestRender();
		Log.d(TAG, "grab constrain");

	}

	@Override
	public void onValueChange(FramePicker picker, float oldVal, float newVal) {
		// TODO Auto-generated method stub
		((TextView) findViewById(R.id.debugLabel)).setText("frame " + newVal);

	}
}