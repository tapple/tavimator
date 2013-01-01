package org.tavatar.tavimator;

/**
 * adapted from bvhnode.h in QAvimator
 * @author tapple
 *
 */
public enum BVHOrderType {
	BVH_XYZ, // This said = 1 in the original C++ enum. watch out in case someone cares 
	BVH_ZYX, BVH_XZY, BVH_YZX, BVH_YXZ, BVH_ZXY;
}
