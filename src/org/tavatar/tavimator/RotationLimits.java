package org.tavatar.tavimator;

public class RotationLimits {
	public String jointName;
	public float xMin;
	public float yMin;
	public float zMin;
	public float xMax;
	public float yMax;
	public float zMax;

	public RotationLimits(String joint, float rxMin, float rxMax,
			float ryMin,float ryMax,
			float rzMin,float rzMax) {
		jointName=joint;

		xMin=rxMin;
		yMin=ryMin;
		zMin=rzMin;
		xMax=rxMax;
		yMax=ryMax;
		zMax=rzMax;
	}
}