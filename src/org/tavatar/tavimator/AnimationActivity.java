package org.tavatar.tavimator;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ToggleButton;

public class AnimationActivity extends Activity 
{
	private static final String TAG = "AnimationActivity";
	/** Hold a reference to our GLSurfaceView */
	private AnimationView mGLSurfaceView;
	
	private static final String SHOWED_TOAST = "showed_toast";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.animation);

		mGLSurfaceView = (AnimationView)findViewById(R.id.gl_surface_view);
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

	public void onTrackingClicked(View view) {
	    // Is the toggle on?
	    boolean on = ((ToggleButton) view).isChecked();
	    
	    mGLSurfaceView.getRenderer().getCamera().getGyroscope().setTracking(on);
	    if (on) {
	        System.out.println("Tracking on");
	    } else {
	        System.out.println("Tracking off");
	    }
	}
	
	public void onGrabClicked(View view) {
	    // Is the toggle on?
	    boolean on = ((ToggleButton) view).isChecked();
	    
	    if (on) {
	        System.out.println("Grab on");
	    } else {
	        System.out.println("Grab off");
	    }
	}
	
	public void onGrabConstrainClicked(View view) {
		mGLSurfaceView.requestRender();
        System.out.println("grab constrain");
	}
}