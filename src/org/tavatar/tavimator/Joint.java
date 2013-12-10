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
	
	public int getIndex() {
		return index;
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

	public void updateTweenedGlobalTransform() {
		if (parent == null) {
			Matrix.setIdentityM(store.tweenedGlobalTransform, index * JointStore.MATRIX_STRIDE);
		} else {
			System.arraycopy(parent.store.tweenedGlobalTransform, parent.index * JointStore.MATRIX_STRIDE,
					store.tweenedGlobalTransform, index * JointStore.MATRIX_STRIDE, JointStore.MATRIX_STRIDE);
		}

		Matrix.translateM(store.tweenedGlobalTransform, index * JointStore.MATRIX_STRIDE, xOrigin(), yOrigin(), zOrigin());
		rotateMatrix(store.tweenedGlobalTransform, index * JointStore.MATRIX_STRIDE);
		Matrix.translateM(store.tweenedGlobalTransform, index * JointStore.MATRIX_STRIDE, xPosition(), yPosition(), zPosition());

		for(Joint child : children) {
			child.updateTweenedGlobalTransform();
		}
	}

	public void rotateMatrix(float[] matrix, int matrixIndex) {
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
			throw new UnsupportedOperationException("Quaternions not yet implemented");
		}
	}

	public float xAngle() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.ROTATION_INDEX + 0];
	}

	public float yAngle() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.ROTATION_INDEX + 1];
	}

	public float zAngle() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.ROTATION_INDEX + 2];
	}

	public float xOrigin() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.ORIGIN_INDEX + 0];
	}

	public float yOrigin() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.ORIGIN_INDEX + 1];
	}

	public float zOrigin() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.ORIGIN_INDEX + 2];
	}

	public float xPosition() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.POSITION_INDEX + 0];
	}

	public float yPosition() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.POSITION_INDEX + 1];
	}

	public float zPosition() {
		return store.value[index * JointStore.VALUE_STRIDE + JointStore.POSITION_INDEX + 2];
	}


}
