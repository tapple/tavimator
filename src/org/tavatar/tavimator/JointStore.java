package org.tavatar.tavimator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.animation.Interpolator;
import android.opengl.Matrix;

/**
 * I allocate the storage for a set of joints that are animated as a unit
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
	
	
	public static interface Animation {
		/**
		 * Read the store's tweenedValue and update it. Must read tweenedValue so animations can be chained
		 * @param store
		 * @return isFinished
		 */
		public boolean updateTweenedValue(JointStore store);
	}
	
	/*
	 * The joints I store
	 */
	private Joint[] joint;
	
	/**
	 * if true, all rotations are stored, interpolated, and animated as euler angles (in DEGREES) (a, b, c, 0) (the angle order is stored in the joint)
	 * if false, as quaternions (i, j, k, real)
	 */
	public boolean useEulerAngles = false;
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
	 * The animations applied to my tweenedValue
	 */
	public List<Animation> animations = new ArrayList<Animation>();
	
	/**
	 * The android context for all my joints and animations
	 */
	public Context context;
	
	/**
	 * Creates an empty "unistore", a store with capacity for a single joint. 
	 * @param jointCount
	 * @param context
	 */
	public JointStore(Context context) {
		this(1, context);
	}
	
	/**
	 * Creates an empty store with capacity of jointCount
	 * @param jointCount
	 * @param context
	 */
	public JointStore(int jointCount, Context context) {
		this.context = context;
		joint = new Joint[jointCount];
		value = new float[jointCount * VALUE_STRIDE];
		tweenedValue = new float[jointCount * VALUE_STRIDE];
		global = new float[jointCount * GLOBAL_STRIDE];
		tweenedGlobal = new float[jointCount * GLOBAL_STRIDE];
		tweenedGlobalTransform = new float[jointCount * MATRIX_STRIDE];

		for (int i = 0; i < jointCount; i++) {
			Quaternion.setIdentity(value, VALUE_STRIDE * i + ROTATION_INDEX);
			Matrix.setIdentityM(tweenedGlobalTransform, MATRIX_STRIDE * i);
		}
	}
	
	/**
	 * Populates myself with clones of jointTemplate. jointTemplate itself is not stored
	 * @param jointTemplate The joint that will be cloned to fill me up
	 * @param jointCount
	 * @param context
	 */
	public JointStore(Joint jointTemplate, int jointCount, Context context) {
		this(jointCount, context);
		for (int i = 0; i < jointCount; i++) {
			setJoint(i, jointTemplate.clone());
		}
	}
	
	public Joint getJoint(int i) {
		return joint[i];
	}
	
	public void setJoint(int i, Joint j) {
		joint[i] = j;
		j.setStore(this);
		j.setIndex(i);
	}

	/**
	 * Compute a new frame. Should only be called once, from the root of the joint heiarchy
	 */
	public void update() {
		updateValue();
		updateTweenedValue();
	}
	
	public void updateValue() {
		// Override me
	}
	
	public void updateTweenedValue() {
		System.arraycopy(value, 0, tweenedValue, 0, value.length);
		for (Animation animation : this.animations) {
			if (animation.updateTweenedValue(this)) {
				this.animations.remove(animation);
			}
		}
	}
	
	public void startTweeneAnimation(int duration, Interpolator interpolator) {
		animations.add(new TweenAnimation(duration, interpolator, this));
	}
}
