package org.tavatar.tavimator;

/**
 * adapted from bvhnode.h in QAvimator
 * @author tapple
 *
 */
public enum BVHNodeType {
	BVH_POS		("POS"),
	BVH_ROOT 	("ROOT"),
	BVH_JOINT	("JOINT"),
	BVH_END		("End"),
	BVH_NO_SL	("NoSL");
	
	public final String name;

	BVHNodeType(String name) {
		this.name = name;
	}
}