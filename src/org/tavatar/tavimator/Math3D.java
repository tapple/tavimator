package org.tavatar.tavimator;

import android.opengl.Matrix;

public class Math3D {
	/**
	 * Input: a 4x4 or 3x3 matrix in column-major order. It is assumed to be an orientation matrix (that is, orthonormal)
	 * Output: a length 4 vector: <axisX, axisY, axisZ, angleDegrees>
	 * 
	 * Code taken from http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToAngle/index.htm
	*/
	public static void toAxisAngle(float [] axisAngle, float[] matrix) {
		double epsilon = 0.01; // margin to allow for rounding errors
		double epsilon2 = 0.1; // margin to distinguish between 0 and 180 degrees
		// optional check that input is pure rotation, 'isRotationMatrix' is defined at:
		// http://www.euclideanspace.com/maths/algebra/matrix/orthogonal/rotation/
		// assert isRotationMatrix(m) : "not valid rotation matrix" ;// for debugging
		
		float m00 = matrix[ 0];	float m01 = matrix[ 4]; float m02 = matrix[ 8];
		float m10 = matrix[ 1];	float m11 = matrix[ 5]; float m12 = matrix[ 9];
		float m20 = matrix[ 2];	float m21 = matrix[ 6]; float m22 = matrix[10];

		if ( (Math.abs(m01-m10)< epsilon)
		  && (Math.abs(m02-m20)< epsilon)
		  && (Math.abs(m12-m21)< epsilon)) {
			// singularity found
			// first check for identity matrix which must have +1 for all terms
			//  in leading diagonaland zero in other terms
			if ( (Math.abs(m01+m10) < epsilon2)
			  && (Math.abs(m02+m20) < epsilon2)
			  && (Math.abs(m12+m21) < epsilon2)
			  && (Math.abs(m00+m11+m22-3) < epsilon2)) {
				// this singularity is identity matrix so angle = 0
				axisAngle[3] = 0.0f;
				axisAngle[0] = 1.0f;
				axisAngle[1] = 0.0f;
				axisAngle[2] = 0.0f;
				return; // zero angle, arbitrary axis
			}
			// otherwise this singularity is angle = 180
			axisAngle[3] = 180f;
			double xx = (m00+1)/2;
			double yy = (m11+1)/2;
			double zz = (m22+1)/2;
			double xy = (m01+m10)/4;
			double xz = (m02+m20)/4;
			double yz = (m12+m21)/4;
			if ((xx > yy) && (xx > zz)) { // m00 is the largest diagonal term
				if (xx< epsilon) {
					axisAngle[0] = 0.0f;
					axisAngle[1] = 0.7071f;
					axisAngle[2] = 0.7071f;
				} else {
					double x = Math.sqrt(xx);
					axisAngle[0] = (float) x;
					axisAngle[1] = (float) (xy/x);
					axisAngle[2] = (float) (xz/x);
				}
			} else if (yy > zz) { // m11 is the largest diagonal term
				if (yy< epsilon) {
					axisAngle[0] = 0.7071f;
					axisAngle[1] = 0.0f;
					axisAngle[2] = 0.7071f;
				} else {
					double y = Math.sqrt(yy);
					axisAngle[0] = (float) (xy/y);
					axisAngle[1] = (float) y;
					axisAngle[2] = (float) (yz/y);
				}	
			} else { // m22 is the largest diagonal term so base result on this
				if (zz< epsilon) {
					axisAngle[0] = 0.7071f;
					axisAngle[1] = 0.7071f;
					axisAngle[2] = 0.0f;
				} else {
					double z = Math.sqrt(zz);
					axisAngle[0] = (float) (xz/z);
					axisAngle[1] = (float) (yz/z);
					axisAngle[2] = (float) z;
				}
			}
			return; // return 180 deg rotation
		}
		// as we have reached here there are no singularities so we can handle normally
		double s = Math.sqrt(
			 (m21 - m12)*(m21 - m12)
			+(m02 - m20)*(m02 - m20)
			+(m10 - m01)*(m10 - m01)); // used to normalise
		if (Math.abs(s) < 0.001) s=1; 
			// prevent divide by zero, should not happen if matrix is orthogonal and should be
			// caught by singularity test above, but I've left it in just in case
		axisAngle[3] = (float) (Math.acos(( m00 + m11 + m22 - 1)/2) * 180/Math.PI);
		axisAngle[0] = (float) ((m21 - m12)/s);
		axisAngle[1] = (float) ((m02 - m20)/s);
		axisAngle[2] = (float) ((m10 - m01)/s);
		return;
	}

	public static Rotation toEulerAngles(Rotation angles, float[] matrix, BVHOrderType order) {
	    /*
	    Convert the given Quaternion to Euler angles (degrees), using
	    the given rotation order for the conversion.

	    The order can be 'xyz', 'xzy', 'yxz', 'yzx', 'zxy', or 'zyx'.
	    */

		angles.x = 0;
		angles.y = 0;
		angles.z = 0;

//		float[] inverse = new float[16];
//		float[] v1 = new float[4];
//		float[] v2 = new float[4];
//		Matrix.transposeM(inverse, 0, matrix, 0);

		if (order == BVHOrderType.BVH_XYZ) {
//			vector v1 = zaxis * quat = matrix[8, 9, 10];
//			vector v2 = xaxis * inverse = matrix[0, 4, 8];
//			angles.x = llAtan2(-v1.y, v1.z);
//			angles.y = llAsin(v2.z);
//			angles.z = llAtan2(-v2.y, v2.x);
			angles.x = (float)Math.atan2(-matrix[9], matrix[10]);
			angles.y = (float)Math.asin(matrix[8]);
			angles.z = (float)Math.atan2(-matrix[4], matrix[0]);
		}

		else if (order == BVHOrderType.BVH_XZY) {
//			vector v1 = yaxis * quat = matrix[4, 5, 6];
//			vector v2 = xaxis * inverse = matrix[0, 4, 8];
//			angles.x = llAtan2(v1.z, v1.y);
//			angles.y = -llAsin(v2.y);
//			angles.z = llAtan2(v2.z, v2.x);
			angles.x = (float)Math.atan2(matrix[6], matrix[5]);
			angles.z = -(float)Math.asin(matrix[4]);
			angles.y = (float)Math.atan2(matrix[8], matrix[0]);
}

		else if (order == BVHOrderType.BVH_YXZ) {
//			vector v1 = zaxis * quat = matrix[8, 9, 10];
//			vector v2 = yaxis * inverse = matrix[1, 5, 9];
//			angles.x = llAtan2(v1.x, v1.z);
//			angles.y = -llAsin(v2.z);
//			angles.z = llAtan2(v2.x, v2.y);
			angles.y = (float)Math.atan2(matrix[8], matrix[10]);
			angles.x = -(float)Math.asin(matrix[9]);
			angles.z = (float)Math.atan2(matrix[1], matrix[5]);
		}

		else if (order == BVHOrderType.BVH_YZX) {
//			vector v1 = xaxis * quat = matrix[0, 1, 2];
//			vector v2 = yaxis * inverse = matrix[1, 5, 9];
//			angles.x = llAtan2(-v1.z, v1.x);
//			angles.y = llAsin(v2.x);
//			angles.z = llAtan2(-v2.z, v2.y);
			angles.y = (float)Math.atan2(-matrix[2], matrix[0]);
			angles.z = (float)Math.asin(matrix[1]);
			angles.x = (float)Math.atan2(-matrix[9], matrix[5]);
		}

		else if (order == BVHOrderType.BVH_ZXY) {
//			vector v1 = yaxis * quat = matrix[4, 5, 6];
//			vector v2 = zaxis * inverse = matrix[2, 6, 10];
//			angles.x = llAtan2(-v1.x, v1.y);
//			angles.y = llAsin(v2.y);
//			angles.z = llAtan2(-v2.x, v2.z);
			angles.z = (float)Math.atan2(-matrix[4], matrix[5]);
			angles.x = (float)Math.asin(matrix[6]);
			angles.y = (float)Math.atan2(-matrix[2], matrix[10]);
		}

		else if (order == BVHOrderType.BVH_ZYX) {
//			vector v1 = xaxis * quat = matrix[0, 1, 2];
//			vector v2 = zaxis * inverse = matrix[2, 6, 10];
//			angles.x = llAtan2(v1.y, v1.x);
//			angles.y = -llAsin(v2.x);
//			angles.z = llAtan2(v2.y, v2.z);
			angles.z = (float)Math.atan2(matrix[1], matrix[0]);
			angles.y = -(float)Math.asin(matrix[2]);
			angles.x = (float)Math.atan2(matrix[6], matrix[10]);
	    }

	    angles.x *= 180.0f / Math.PI;
	    angles.y *= 180.0f / Math.PI;
	    angles.z *= 180.0f / Math.PI;
	    return angles;
	}
}
