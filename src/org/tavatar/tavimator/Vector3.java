package org.tavatar.tavimator;

public class Vector3 {
	public static float magnitude(float[] v, int vOffset) {
		return (float)Math.sqrt(
				v[vOffset + 0] * v[vOffset + 0] +
				v[vOffset + 1] * v[vOffset + 1] +
				v[vOffset + 2] * v[vOffset + 2]);
	}
}
