package org.tavatar.tavimator;

/**
 * adapted from bvhnode.h in QAvimator
 * @author tapple
 *
 */
public enum BVHChannelType {
	BVH_XPOS ("Xposition"),
	BVH_YPOS ("Yposition"),
	BVH_ZPOS ("Zposition"),
	BVH_XROT ("Xrotation"),
	BVH_YROT ("Yrotation"),
	BVH_ZROT ("Zrotation");

	public final String name;

	BVHChannelType(String name) {
		this.name = name;
	}
}