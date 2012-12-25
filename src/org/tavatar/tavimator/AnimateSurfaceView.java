package org.tavatar.tavimator;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class AnimateSurfaceView extends GLSurfaceView 
{
	private AnimateRenderer renderer;
	
	public AnimateSurfaceView(Context context) 
	{
		super(context);	
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
}
