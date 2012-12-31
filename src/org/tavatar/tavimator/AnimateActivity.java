package org.tavatar.tavimator;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

public class AnimateActivity extends Activity 
{
	/** Hold a reference to our GLSurfaceView */
	private AnimateSurfaceView mGLSurfaceView;
	
	private static final String SHOWED_TOAST = "showed_toast";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.animate);

		mGLSurfaceView = (AnimateSurfaceView)findViewById(R.id.gl_surface_view);

		// Check if the system supports OpenGL ES 2.0.
		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

		if (supportsEs2) 
		{
			// Request an OpenGL ES 2.0 compatible context.
			mGLSurfaceView.setEGLContextClientVersion(2);

			// Set the renderer to our demo renderer, defined below.
			mGLSurfaceView.setRenderer(new AnimateRenderer(this));
		} 
		else 
		{
			// This is where you could create an OpenGL ES 1.x compatible
			// renderer if you wanted to support both ES 1 and ES 2.
			return;
		}
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
	}	
	
	@Override
	protected void onSaveInstanceState (Bundle outState)
	{
		outState.putBoolean(SHOWED_TOAST, true);
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
        System.out.println("grab constrain");
	}
}