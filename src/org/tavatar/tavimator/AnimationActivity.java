package org.tavatar.tavimator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AnimationActivity extends ActionBarActivity {
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

		((FramePicker)findViewById(R.id.frame_picker)).setOnValueChangedListener(mGLSurfaceView);
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		Log.d(TAG, "onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.animation_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		boolean on = mGLSurfaceView.getRenderer().getCamera().getGyroscope().getSensing();
		menu.findItem(R.id.action_turn_gyro_on).setVisible(!on);
		menu.findItem(R.id.action_turn_gyro_off).setVisible(on);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_turn_gyro_on:
	    		mGLSurfaceView.getRenderer().getCamera().getGyroscope().setTracking(true);
	    		mGLSurfaceView.getRenderer().getCamera().getGyroscope().setSensing(true);
    			Log.d(TAG, "Tracking on");
    			supportInvalidateOptionsMenu();
	            return true;
	        case R.id.action_turn_gyro_off:
	    		mGLSurfaceView.getRenderer().getCamera().getGyroscope().setTracking(false);
	    		mGLSurfaceView.getRenderer().getCamera().getGyroscope().setSensing(false);
    			Log.d(TAG, "Tracking off");
    			supportInvalidateOptionsMenu();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
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
}