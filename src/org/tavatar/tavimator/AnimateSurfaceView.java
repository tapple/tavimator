package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class AnimateSurfaceView extends GLSurfaceView 
{
	private AnimateRenderer renderer;
	
	public AnimateSurfaceView(Context context) {
		super(context);	
	}

	public AnimateSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) 
	{
		if (renderer.getCamera().getTrackball().onTouchEvent(event)) return true;
		return super.onTouchEvent(event);
	}

	// Hides superclass method.
	public void setRenderer(AnimateRenderer renderer) 
	{
		this.renderer = renderer;
		super.setRenderer(renderer);
	}
	
	public AnimateRenderer getRenderer() {
		return renderer;
	}

	@Override
	public void onResume() 
	{
		// The activity must call the GL surface view's onResume() on activity onResume().
		super.onResume();
		renderer.onResume();
	}

	@Override
	public void onPause() 
	{
		// The activity must call the GL surface view's onPause() on activity onPause().
		super.onPause();
		renderer.onPause();
	}	
}
