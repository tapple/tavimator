package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class AnimationView extends GLSurfaceView 
{
	private AnimationRenderer renderer;
	
	public AnimationView(Context context) {
		super(context);	
	}

	public AnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) 
	{
		if (renderer.getCamera().getTrackball().onTouchEvent(event)) return true;
		return super.onTouchEvent(event);
	}

	// Hides superclass method.
	public void setRenderer(AnimationRenderer renderer) 
	{
		this.renderer = renderer;
		super.setRenderer(renderer);
	}
	
	public AnimationRenderer getRenderer() {
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
