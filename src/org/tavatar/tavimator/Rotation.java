package org.tavatar.tavimator;

public class Rotation {
	public float x;
	public float y;
	public float z;
	
	public Rotation() {
		x=0;
		y=0;
		z=0;
	}

	public Rotation(float rx, float ry, float rz) {
		x=rx;
		y=ry;
		z=rz;
	}

	public static Rotation difference(Rotation rot1, Rotation rot2) {
		return new Rotation(rot2.x-rot1.x,rot2.y-rot1.y,rot2.z-rot1.z);
	}
}