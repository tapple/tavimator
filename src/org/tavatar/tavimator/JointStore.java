package org.tavatar.tavimator;

import java.lang.reflect.Array;

import android.content.Context;

/**
 * I allocate the storage for a set of joints that are allocated, deleted, and animated as a unit
 * @author tapple
 *
 */
public class JointStore {
	/**
	 * parameters of my backing storage in an external array
	 */
	public static final int VALUE_STRIDE = 11;
	public static final int GLOBAL_STRIDE = 8;
	public static final int MATRIX_STRIDE = 16;

	public static final int ROTATION_INDEX = 0;
	public static final int SCALE_INDEX = 4;
	public static final int POSITION_INDEX = 5;
	public static final int ORIGIN_INDEX = 8; // not part of global arrays
	
	/*
	 * The joints I store
	 */
	private Joint[] joint;
	
	/**
	 * if true, all rotations are stored, interpolated, and animated as euler angles (a, b, c, 0) (the angle order is stored in the joint)
	 * if false, as quaternions (i, j, k, real)
	 */
	public boolean useEulerAngles = true;
	/*
	 * The true value vectors
	 */
	public float[] value;
	
	/*
	 * The backing store for my animated value, to smooth out transitions
	 */
	public float[] tweenedValue;
	
	/**
	 * backing store for my global value vector. Not sure if this is even needed
	 */
	public float[] global;
	
	/**
	 * backing store for my animated global value vectors, to smooth out transitions
	 */
	public float[] tweenedGlobal;
	
	/** 
	 * backing store for my global transform matrices
	 */
	public float[] tweenedGlobalTransform;
	
	/**
	 * Temporary matrices for my joints to use during updates (and only during
	 * updates). Please only use these in the render thread, to avoid corruption, and only for the duration of 1 method
	 */
	public float[] tempUpdateMatrix1 = new float[16];
	public float[] tempUpdateMatrix2 = new float[16];
	
	/**
	 * Temporary matrices for my joints to use during UI interactions (and only
	 * during updates). Please only use these in the UI thread, to avoid
	 * corruption, and call no user methods between producing and consuming the
	 * value
	 */
	public float[] tempModificationMatrix1 = new float[16];
	public float[] tempModificationMatrix2 = new float[16];
	
	
	
	/**
	 * The android context for all my joints and animations
	 */
	public Context context;
	
	/**
	 * Populates myself with clones of jointTemplate. jointTemplate itself is not stored
	 * @param jointTemplate The joint that will be cloned to fill me up
	 * @param jointCount
	 * @param context
	 */
	public JointStore(Joint jointTemplate, int jointCount, Context context) {
		this.context = context;
		joint = new Joint[jointCount];
		value = new float[jointCount * VALUE_STRIDE];
		tweenedValue = new float[jointCount * VALUE_STRIDE];
		global = new float[jointCount * GLOBAL_STRIDE];
		tweenedGlobal = new float[jointCount * GLOBAL_STRIDE];
		tweenedGlobalTransform = new float[jointCount * MATRIX_STRIDE];
		
		for (int i = 0; i < jointCount; i++) {
			setJoint(i, jointTemplate.clone());
			getJoint(i).setIndex(i);
			getJoint(i).setStore(this);
		}
	}
	
	public Joint getJoint(int i) {
		return joint[i];
	}
	
	public void setJoint(int i, Joint j) {
		joint[i] = j;
	}
}
