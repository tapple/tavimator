package org.tavatar.tavimator;

import android.opengl.Matrix;

public class Vector3 {
	public static float magnitude(float[] v, int vOffset) {
		return Matrix.length(v[vOffset + 0], v[vOffset + 1], v[vOffset + 2]);
	}

	public static void scaleBy(float[] v, int vOffset, float scale) {
		v[vOffset + 0] *= scale;
		v[vOffset + 1] *= scale;
		v[vOffset + 2] *= scale;
	}
}
