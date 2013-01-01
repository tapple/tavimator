package org.tavatar.tavimator;

public class Position {
	public float x;
	public float y;
	public float z;
	
	public Position() {
		x=0;
	  	y=0;
	  	z=0;
	}

	public Position(float px, float py, float pz) {
		x=px;
	  	y=py;
	  	z=pz;
	}

	public static Position difference(Position pos1, Position pos2) {
		return new Position(pos2.x-pos1.x,pos2.y-pos1.y,pos2.z-pos1.z);
	}
}