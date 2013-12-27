package org.tavatar.tavimator;

public class Quaternion {
	public static final int VX = 0;
	public static final int VY = 1;
	public static final int VZ = 2;
	public static final int VW = 3;
	public static final int VS = 3;
	public static final int LENGTH = 4;
	
	public static final float DEGREES_TO_RADIANS = (float)(Math.PI / 180);
	public static final float RADIANS_TO_DEGREES = (float)(180 / Math.PI);

	public static void setIdentity(float[] sq, int sqOffset) {
		sq[sqOffset + VX] = 0.0f;
		sq[sqOffset + VY] = 0.0f;
		sq[sqOffset + VZ] = 0.0f;
		sq[sqOffset + VW] = 1.0f;
	}
	
	private static final float FP_MAG_THRESHOLD = 0.0000001f;
	private static final float ONE_PART_IN_A_MILLION = 0.000001f;

	public static float magnitude(float[] q, int qOffset) {
		return (float)Math.sqrt(
				q[qOffset+VX]*q[qOffset+VX] + q[qOffset+VY]*q[qOffset+VY] +
				q[qOffset+VZ]*q[qOffset+VZ] + q[qOffset+VS]*q[qOffset+VS]);
	}

	
	// Normalizes Quaternion and returns magnitude
	public static void normalize(float[] q, int qOffset) {
		float mag = magnitude(q, qOffset);
	
		if (mag > FP_MAG_THRESHOLD) {
			// Floating point error can prevent some quaternions from achieving
			// exact unity length.  When trying to renormalize such quaternions we
			// can oscillate between multiple quantized states.  To prevent such
			// drifts we only renomalize if the length is far enough from unity.
			if (Math.abs(1.f - mag) > ONE_PART_IN_A_MILLION) {
				float oomag = 1.f/mag;
				q[qOffset+VX] *= oomag;
				q[qOffset+VY] *= oomag;
				q[qOffset+VZ] *= oomag;
				q[qOffset+VS] *= oomag;
			}
		} else {
			// we were given a very bad quaternion so we set it to identity
			setIdentity(q, qOffset);
		}
	}

	// angle in radians
	public static void fromAxisAngle(float sq[], int sqOffset, float[] av, int avOffset, float angle) {
		if (angle == 0.0f) {
			setIdentity(sq, sqOffset);
			return;
		}
		float avMag = Vector3.magnitude(av, avOffset);
	
		float c, s;
		c = (float)Math.cos(angle*0.5f);
		s = (float)Math.sin(angle*0.5f);
	
		sq[sqOffset + VX] = av[avOffset + VX] * s / avMag;
		sq[sqOffset + VY] = av[avOffset + VY] * s / avMag;
		sq[sqOffset + VZ] = av[avOffset + VZ] * s / avMag;
		sq[sqOffset + VW] = c;
		normalize(sq, sqOffset);
	}
	
	// angle is magnitude, in radians
	public static void fromAngularVelocity(float sq[], int sqOffset, float[] av, int avOffset) {
		fromAxisAngle(sq, sqOffset, av, avOffset, Vector3.magnitude(av, avOffset));
	}
	
	// Returns the Matrix3 equivalent of Quaternion
	// SJB: This code is correct for a logicly stored (non-transposed) matrix;
//	Our matrices are stored transposed, OpenGL style, so this generates the
//	INVERSE matrix, or the CORRECT matrix form an INVERSE quaternion.
//	Because we use similar logic in LLMatrix3::quaternion(),
//	we are internally consistant so everything works OK :)
	public static void toMatrix(float[]q, int qOffset, float[] m, int mOffset) {
		float		xx, xy, xz, xw, yy, yz, yw, zz, zw;
	
	    xx = q[qOffset+VX] * q[qOffset+VX];
	    xy = q[qOffset+VX] * q[qOffset+VY];
	    xz = q[qOffset+VX] * q[qOffset+VZ];
	    xw = q[qOffset+VX] * q[qOffset+VW];
	
	    yy = q[qOffset+VY] * q[qOffset+VY];
	    yz = q[qOffset+VY] * q[qOffset+VZ];
	    yw = q[qOffset+VY] * q[qOffset+VW];
	
	    zz = q[qOffset+VZ] * q[qOffset+VZ];
	    zw = q[qOffset+VZ] * q[qOffset+VW];
	
	    m[mOffset+ 0]  = 1.f - 2.f * ( yy + zz );
	    m[mOffset+ 1]  =	   2.f * ( xy + zw );
	    m[mOffset+ 2]  =	   2.f * ( xz - yw );
	    m[mOffset+ 3]  =	   0.f;
	
	    m[mOffset+ 4]  =	   2.f * ( xy - zw );
	    m[mOffset+ 5]  = 1.f - 2.f * ( xx + zz );
	    m[mOffset+ 6]  =	   2.f * ( yz + xw );
	    m[mOffset+ 7]  =	   0.f;
	
	    m[mOffset+ 8]  =	   2.f * ( xz + yw );
	    m[mOffset+ 9]  =	   2.f * ( yz - xw );
	    m[mOffset+10]  = 1.f - 2.f * ( xx + yy );
	    m[mOffset+11]  =	   0.f;

	    m[mOffset+12]  =	   0.f;
	    m[mOffset+13]  =	   0.f;
	    m[mOffset+14]  =	   0.f;
	    m[mOffset+15]  =	   1.f;
	}

	// Multiply two quaternions. q = a * b
	// Does NOT renormalize the result
	public static void multiply(float[] q, int qOffset, float[] a, int aOffset, float[] b, int bOffset) {
	//	LLQuaternion::mMultCount++;
	
		q[0] = b[bOffset+3] * a[aOffset+0] + b[bOffset+0] * a[aOffset+3] + b[bOffset+1] * a[aOffset+2] - b[bOffset+2] * a[aOffset+1];
		q[1] = b[bOffset+3] * a[aOffset+1] + b[bOffset+1] * a[aOffset+3] + b[bOffset+2] * a[aOffset+0] - b[bOffset+0] * a[aOffset+2];
		q[2] = b[bOffset+3] * a[aOffset+2] + b[bOffset+2] * a[aOffset+3] + b[bOffset+0] * a[aOffset+1] - b[bOffset+1] * a[aOffset+0];
		q[3] = b[bOffset+3] * a[aOffset+3] - b[bOffset+0] * a[aOffset+0] - b[bOffset+1] * a[aOffset+1] - b[bOffset+2] * a[aOffset+2];
	}

	// Rotates vector a by quaternion rot, and stores the new vector in v. v = rot * a * rot'. Assumes vectors have 3 components. 
	public static void rotate3(float[] v, int vOffset, float[] a, int aOffset, float[] rot, int rotOffset) {
	    float rw = - rot[rotOffset+VX] * a[aOffset+VX] - rot[rotOffset+VY] * a[aOffset+VY] - rot[rotOffset+VZ] * a[aOffset+VZ];
	    float rx =   rot[rotOffset+VW] * a[aOffset+VX] + rot[rotOffset+VY] * a[aOffset+VZ] - rot[rotOffset+VZ] * a[aOffset+VY];
	    float ry =   rot[rotOffset+VW] * a[aOffset+VY] + rot[rotOffset+VZ] * a[aOffset+VX] - rot[rotOffset+VX] * a[aOffset+VZ];
	    float rz =   rot[rotOffset+VW] * a[aOffset+VZ] + rot[rotOffset+VX] * a[aOffset+VY] - rot[rotOffset+VY] * a[aOffset+VX];
	
	    v[vOffset+VX] = - rw * rot[rotOffset+VX] +  rx * rot[rotOffset+VW] - ry * rot[rotOffset+VZ] + rz * rot[rotOffset+VY];
	    v[vOffset+VY] = - rw * rot[rotOffset+VY] +  ry * rot[rotOffset+VW] - rz * rot[rotOffset+VX] + rx * rot[rotOffset+VZ];
	    v[vOffset+VZ] = - rw * rot[rotOffset+VZ] +  rz * rot[rotOffset+VW] - rx * rot[rotOffset+VY] + ry * rot[rotOffset+VX];
	}
	    
	// same as rotate3, but for 4-component vectors
	public static void rotate4(float[] v, int vOffset, float[] a, int aOffset, float[] rot, int rotOffset) {
		rotate3(v, vOffset, a, aOffset, rot, rotOffset);
	    v[vOffset+3]=a[aOffset+3];
	}
}
