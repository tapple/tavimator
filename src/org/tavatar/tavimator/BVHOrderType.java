package org.tavatar.tavimator;

/**
 * adapted from bvhnode.h in QAvimator
 * @author tapple
 *
 */
public enum BVHOrderType {
	BVH_XYZ (0x321), 
	BVH_ZYX (0x123),
	BVH_XZY (0x231),
	BVH_YZX (0x132),
	BVH_YXZ (0x312),
	BVH_ZXY (0x213);
	
	public static int NUM_AXES = 3;

	private int order;
	private BVHOrderType(int order) {
		this.order = order;
	}
	
	public BVHChannelType channelTypeAt(int i) {
		switch ((order >>> (i << 4)) & 0xf) {
		case 1: return BVHChannelType.BVH_XROT;
		case 2: return BVHChannelType.BVH_YROT;
		case 3: return BVHChannelType.BVH_ZROT;
		default: throw new IllegalArgumentException("channelTypeAt must be called with 0, 1, or 2 only");
		}
	}
}
