package org.tavatar.tavimator;

public abstract class SLPartsRenderer {
	protected AnimationRenderer renderer;

	public SLPartsRenderer(AnimationRenderer renderer) {
		this.renderer = renderer;
	}

	public abstract void load();
	public abstract void drawPartNamed(String name);
	public abstract void release();
}