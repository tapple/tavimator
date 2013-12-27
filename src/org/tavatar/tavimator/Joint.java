package org.tavatar.tavimator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.opengl.Matrix;

/**
 * I am an abstract object in 3d space. I am made up of two vectors connected by
 * a free-spinning ball joint. I can be assembled in hearchies and animated
 */
public class Joint implements Cloneable {

	/**
	 * Where all my numbers are stored
	 */
	private JointStore store;

	/**
	 * My index within my JointStore. Multiply by the appropriate stride for each array
	 */
	private int index;

	private Joint parent;

	/**
	 * the order of my euler angles, if the store is using euler angles;
	 */
	private BVHOrderType order = BVHOrderType.BVH_XYZ;

	private List<Joint> children;

	public Joint() {
		children = new ArrayList<Joint>();
	}

	public Joint clone() {
		try {
			Joint ans = (Joint)super.clone();
			ans.children = new ArrayList<Joint>();
			return ans;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Joint cloning should never fail", e);
		}
	}

	public JointStore getStore() {
		return store;
	}
	
	public void setStore(JointStore store) {
		this.store = store;
	}
	
	/**
	 * Convenience method for initializing me as the only joint in my joint store.
	 * @param store
	 */
	public void setUniStore(JointStore store) {
		store.setJoint(0, this);
	}
	
	public int getIndex() {
		return index;
	}
	
	public int getValueIndex() {
		return index * JointStore.VALUE_STRIDE;
	}
	
	public int getMatrixIndex() {
		return index * JointStore.MATRIX_STRIDE;
	}
	
	public int getRotationIndex() {
		return getValueIndex() + JointStore.ROTATION_INDEX;
	}
	
	public int getOriginIndex() {
		return getValueIndex() + JointStore.ORIGIN_INDEX;
	}
	
	public int getPositionIndex() {
		return getValueIndex() + JointStore.POSITION_INDEX;
	}
	
	public int getScaleIndex() {
		return getValueIndex() + JointStore.SCALE_INDEX;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}

	public Joint getParent() {
		return parent;
	}
	
	public void setParent(Joint parent) {
		this.parent = parent;
	}

	public List<Joint> getChildren() {
		return children;
	}

	/**
	 * compute my global transform, and all my children. Should only be called from the render thread
	 */
	public void updateTweenedGlobalTransform() {
		if (parent == null) {
			Matrix.setIdentityM(store.tweenedGlobalTransform, getMatrixIndex());
		} else {
			parent.getTweenedGlobalTransform(store.tweenedGlobalTransform, getMatrixIndex());
		}

		Matrix.translateM(store.tweenedGlobalTransform, getMatrixIndex(), xOrigin(), yOrigin(), zOrigin());
		basicTweenedRotateMatrix(store.tweenedGlobalTransform, getMatrixIndex(), store.tempUpdateMatrix1, store.tempUpdateMatrix2);
		Matrix.translateM(store.tweenedGlobalTransform, getMatrixIndex(), xPosition(), yPosition(), zPosition());

		for(Joint child : children) {
			child.updateTweenedGlobalTransform();
		}
	}

	public void basicRotateMatrix(float[] matrix, int matrixIndex, float[] tempMatrix1, float[] tempMatrix2) {
		if (store.useEulerAngles) {
			for(int i = 0; i < BVHOrderType.NUM_AXES; i++) {
				// need to do rotations in the right order
				switch(order.channelTypeAt(i)) {
				case BVH_XROT: Matrix.rotateM(matrix, matrixIndex, xAngle(), 1, 0, 0); break;
				case BVH_YROT: Matrix.rotateM(matrix, matrixIndex, yAngle(), 0, 1, 0); break;
				case BVH_ZROT: Matrix.rotateM(matrix, matrixIndex, zAngle(), 0, 0, 1); break;
				default: break;
				}
			}
		} else {
			Quaternion.toMatrix(store.value, getRotationIndex(), tempMatrix1, 0);
			Matrix.multiplyMM(tempMatrix2, 0, tempMatrix1, 0, matrix, matrixIndex);
			System.arraycopy(tempMatrix2, 0, matrix, matrixIndex, JointStore.MATRIX_STRIDE);
		}
	}

	public void basicTweenedRotateMatrix(float[] matrix, int matrixIndex, float[] tempMatrix1, float[] tempMatrix2) {
		if (store.useEulerAngles) {
			for(int i = 0; i < BVHOrderType.NUM_AXES; i++) {
				// need to do rotations in the right order
				switch(order.channelTypeAt(i)) {
				case BVH_XROT: Matrix.rotateM(matrix, matrixIndex, tweenedXAngle(), 1, 0, 0); break;
				case BVH_YROT: Matrix.rotateM(matrix, matrixIndex, tweenedYAngle(), 0, 1, 0); break;
				case BVH_ZROT: Matrix.rotateM(matrix, matrixIndex, tweenedZAngle(), 0, 0, 1); break;
				default: break;
				}
			}
		} else {
			Quaternion.toMatrix(store.tweenedValue, getRotationIndex(), tempMatrix1, 0);
			Matrix.multiplyMM(tempMatrix2, 0, tempMatrix1, 0, matrix, matrixIndex);
			System.arraycopy(tempMatrix2, 0, matrix, matrixIndex, JointStore.MATRIX_STRIDE);
		}
	}
	
	public void basicRotateBy(float[] q, int qOffset, float[] tempQ) {
		getRotation(tempQ, 0);
		Quaternion.multiply(store.value, getRotationIndex(), q, qOffset, tempQ, 0);
	}
	
	public void basicRotateAbout(float[] axis, int axisIndex, float angle, float[] tempQ1, float[] tempQ2) {
		Quaternion.fromAxisAngle(tempQ1, 0, axis, 0, angle);
		basicRotateBy(tempQ1, 0, tempQ2);
	}

	public void basicRotateAbout(float[] axis, int axisIndex, float[] tempQ1, float[] tempQ2) {
		Quaternion.fromAngularVelocity(tempQ1, 0, axis, 0);
		basicRotateBy(tempQ1, 0, tempQ2);
	}

	public void scaleBy(float fraction) {
		store.value[index * JointStore.VALUE_STRIDE + JointStore.SCALE_INDEX] *= fraction;
	}

	public float xAngle() {
		return store.value[getRotationIndex() + 0];
	}

	public float yAngle() {
		return store.value[getRotationIndex() + 1];
	}

	public float zAngle() {
		return store.value[getRotationIndex() + 2];
	}

	public float xOrigin() {
		return store.value[getOriginIndex() + 0];
	}

	public float yOrigin() {
		return store.value[getOriginIndex() + 1];
	}

	public float zOrigin() {
		return store.value[getOriginIndex() + 2];
	}

	public float xPosition() {
		return store.value[getPositionIndex() + 0];
	}

	public float yPosition() {
		return store.value[getPositionIndex() + 1];
	}

	public float zPosition() {
		return store.value[getPositionIndex() + 2];
	}

	public float tweenedXAngle() {
		return store.tweenedValue[getRotationIndex() + 0];
	}

	public float tweenedYAngle() {
		return store.tweenedValue[getRotationIndex() + 1];
	}

	public float tweenedZAngle() {
		return store.tweenedValue[getRotationIndex() + 2];
	}

	public float tweenedXOrigin() {
		return store.tweenedValue[getOriginIndex() + 0];
	}

	public float tweenedYOrigin() {
		return store.tweenedValue[getOriginIndex() + 1];
	}

	public float tweenedZOrigin() {
		return store.tweenedValue[getOriginIndex() + 2];
	}

	public float tweenedXPosition() {
		return store.tweenedValue[getPositionIndex() + 0];
	}

	public float tweenedYPosition() {
		return store.tweenedValue[getPositionIndex() + 1];
	}

	public float tweenedZPosition() {
		return store.tweenedValue[getPositionIndex() + 2];
	}


	public void getTweenedGlobalTransform(float[] dest, int destOffset) {
		System.arraycopy(store.tweenedGlobalTransform, getMatrixIndex(), dest, destOffset, JointStore.MATRIX_STRIDE);
	}

	public void getRotation(float[] q, int qOffset) {
		if (store.useEulerAngles) {
			throw new UnsupportedOperationException("euler to quat not yet implemented");
		} else {
			System.arraycopy(store.value, getRotationIndex(), q, qOffset, Quaternion.LENGTH);
		}
	}
}
