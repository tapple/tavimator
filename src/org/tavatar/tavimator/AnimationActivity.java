package org.tavatar.tavimator;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

public class AnimationActivity extends Activity 
{
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
		mGLSurfaceView.requestRender();
        System.out.println("grab constrain");
	}
}