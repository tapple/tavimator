package org.tavatar.tavimator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.AssetManager;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.TextView;

public class AnimationView extends GLSurfaceView {
	public final static int PICK_PART_RESULT = 932023;

	private AnimationRenderer renderer;
	private AnimationPartSelector tapHandler;
	private AnimationDragHandler cameraHandler;

	private static final String TAG = "AnimationView";

	private BVH bvh;
	private List<Animation> animList = new ArrayList<Animation>();
	private Animation animation; // this is the "currently selected" animation
	private BVHNode[] joints = new BVHNode[2];
	private PlaybackController playback;
	
	private PointerGroup pointers;

	/**
	 * Position of the last motion event.
	 */
	//	private int mLastMotionX1;
	//	private int mLastMotionY1;
	//	private int mLastMotionX2;
	//	private int mLastMotionY2;

	private int partHighlighted = -1;
	private int partSelected = -1;
	private int mirrorSelected = -1;
	//    private int propSelected;  // needs an own variable, because we will drag the handle, not the prop
	//    private int propDragging;  // holds the actual drag handle id

	public AnimationView(Context context) {
		super(context);
		initialize();
	}

	public AnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	private String printList(String[] strings) {
		StringBuilder ans = new StringBuilder();
		ans.append('[');
		if (strings.length >= 1) ans.append(strings[0]);
		for (int i = 1; i < strings.length; i++) {
			ans.append(", ");
			ans.append(strings[i]);
		}
		ans.append(']');
		return ans.toString();
	}

	private void initialize() {
		if (isInEditMode()) return;

		bvh = new BVH();
		AssetManager assets = getContext().getAssets();
		try {
			joints[1] = bvh.animRead(assets.open("data/SLFemale.bvh"), assets.open(Animation.LIMITS_FILE), false);
			bvh.dumpNodes(joints[1], "");
//			setAnimation(new Animation(getContext(), bvh));
//			setAnimation(new Animation(getContext(), bvh, assets.open("data/sl_dance1.bvh"), false));
			setAnimation(new Animation(getContext(), bvh, assets.open("data/avatar_dance1.bvh"), false));
			playback = new PlaybackController(getContext());
			playback.setAnimation(animation);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Check if the system supports OpenGL ES 2.0.
		final ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

		if (supportsEs2) {
			// Request an OpenGL ES 2.0 compatible context.
			setEGLContextClientVersion(2);

			setDebugFlags(DEBUG_CHECK_GL_ERROR);

			// Set the renderer to our demo renderer, defined below.
			renderer = new AnimationRenderer(this);
			setRenderer(renderer);
			//			setRenderMode(RENDERMODE_WHEN_DIRTY);
		} else {
			// This is where you could create an OpenGL ES 1.x compatible
			// renderer if you wanted to support both ES 1 and ES 2.
			return;
		}

		pointers = new PointerGroup(getContext());
	}
	
	public PlaybackController getPlayback() {
		return playback;
	}

	// this code probably would be more appropriate in the activity
	public void initializeTouchDispatcher() {
		tapHandler = new AnimationPartSelector(this);
		cameraHandler = renderer.getCamera().getTrackball().getDragHandler(
				R.string.one_finger_tool_name_orbit_camera, R.string.short_tool_name_orbit_camera);
	}

	public AnimationRenderer getRenderer() {
		return renderer;
	}

	public Camera getCamera() {
		return getRenderer().getCamera();
	}

	public Trackball getCameraTrackball() {
		return getCamera().getTrackball();
	}

	public Gyroscope getGyroscope() {
		return getCamera().getGyroscope();
	}

	public Animation getSelectedAnimation() {
		return animation;
	}

	public Animation getAnimationNumber(int index) {
		return animList.get(index);
	}

	public int getAnimationCount() {
		return animList.size();
	}

	public BVHNode getJoints(int index) {
		return joints[index];
	}

	public Animation getLastAnimation() { 
		return animList.get(animList.size()-1);
	}

	public BVH getBVH() {
		return bvh;
	}

	public void selectAnimation(int index) {
		if(index < animList.size()) {
			animation = animList.get(index);
			emit(animationSelected(getSelectedAnimation()));
			repaint();
		}
	}

	public void setAnimation(Animation anim) {
		clear();

		animation = anim;
		animList.add(anim);
		//connect(anim,SIGNAL(frameChanged()),this,SLOT(repaint()));
		repaint();
	}

	// Adds a new animation without overriding others, and sets it current
	public void addAnimation(Animation anim) {
		if(!inAnimList(anim)) {
			animList.add(anim);
			animation = anim; // set it as the current one
			//connect(anim,SIGNAL(frameChanged()),this,SLOT(repaint()));
			repaint();
		}
	}

	private boolean inAnimList(Animation anim) {
		return animList.contains(anim);
	}

	public void clear() {
		animList.clear();
		animation = null;
	}

	public BVHNode getSelectedPart() {
		return getSelectedAnimation().getNode(partSelected % AnimationRenderer.ANIMATION_INCREMENT);
	}

	public int getSelectedPartIndex() {
		return partSelected % AnimationRenderer.ANIMATION_INCREMENT;
	}

	/*
    public String getPartName(int index) {
      // get part name from animation, with respect to multiple animations in view
      return getSelectedAnimation()->getPartName(index % renderer.ANIMATION_INCREMENT);
    }
	 */

	/*
    // returns the selected prop name or an empty string if none selected
    public String getSelectedPropName() {
    	for(int index = 0; index < propList.count(); index++)
    		if(propList.at(index).id == propSelected) return propList.at(index).name();
    	return "";
    }
	 */

	public void selectPart(int partNum) {
		BVHNode node = getSelectedAnimation().getNode(partNum);
		Log.d(TAG, "AnimationView.selectPart(" + partNum + ")");

		if(node == null) {
			Log.d(TAG, "AnimationView::selectPart(" + partNum + "): node==0!");
			return;
		}

		if(node.type == BVHNodeType.BVH_END) {
			partSelected=0;
			mirrorSelected=0;
			//    		propSelected=0;
			//    		propDragging=0;
			//    		emit backgroundClicked();
			//    		repaint();
		} else {
			selectPart(node);
		}
	}

	public void selectPart(BVHNode node) {
		Log.d(TAG, "selectPart(BVHNode) from thread " + Thread.currentThread().hashCode());

		if(node == null) {
			Log.d(TAG, "AnimationView::selectPart(node): node==0!");
			return;
		}

//		float[] newOrientation = new float[16];
//		Matrix.setIdentityM(newOrientation, 0);
//		node.rotateMatrixForFrame(newOrientation, animation.getFrame());
//		selectionTrackball.setOrientation(newOrientation);

		Log.d(TAG, "AnimationView::selectPart(node): " + node.name());
		// make sure no prop is selected anymore
		//    	propSelected=0;
		//    	propDragging=0;

		// find out the index count of the animation we're working with currently
		int animationIndex = animList.indexOf(getSelectedAnimation());

		// get the part index to be selected, including the proper animation increment
		// FIXME: when we are adding support for removing animations we need to remember
		//        the increment for each animation so they don't get confused
		partSelected = getSelectedAnimation().getPartIndex(node) + AnimationRenderer.ANIMATION_INCREMENT*animationIndex;
		//    	emit partClicked(node,
		//    			Rotation(getSelectedAnimation()->getRotation(node)),
		//    			getSelectedAnimation()->getRotationLimits(node),
		//    			Position(getSelectedAnimation()->getPosition())
		//    			);
		//    	repaint();
	}

	/*
    void selectProp(final String propName) {
    	// make sure no part is selected anymore
    	partSelected=0;
    	mirrorSelected=0;
    	Prop prop=getPropByName(propName);
    	if(prop) propSelected=prop->id;
    	repaint();
    }
	 */

	public int getFrame() {
		return playback.getNearestFrame(playback.getTime());
	}

	// Phase one of selection rotation computing. Save the trackball rotation
	// into the relative rotation of the selection for this frame. Absolute
	// rotations have not been computed yet for this frame
/*
	public void updateSelectionOrientation() {
		BVHNode selection = getSelectedPart();
		if (selection == null) return;
		selectionTrackball.updateOrientation();
		float[] newOrientation = new float[16];
		animation.setRotationFromMatrix(getFrame(), selection, selectionTrackball.getOrientation(newOrientation));
	}
//*/

	// Phase two of selection rotation computing. Orient the selection trackball
	// relative to the camera trackball so that it knows which direction touch
	// events are coming from
/*
	public void updateSelectionTouchOrientation() {
		BVHNode selection = getSelectedPart();
		if (selection == null) return;
		float[] cameraOrientation = new float[16];
		Matrix.transposeM(cameraOrientation, 0, getRenderer().getCamera().getInverseCameraOrientation(), 0);
		Matrix.multiplyMM(selectionTrackball.getCameraToTrackballOrientation(), 0,
				renderer.inverseGlobalParentOrientation, 0,
				cameraOrientation, 0);
		float[] cameraTrackballOrientation = new float[16];
		getRenderer().getCamera().getTrackball().getInverseOrientation(cameraTrackballOrientation);
		Matrix.multiplyMM(selectionTrackball.getGyroToTrackball(), 0,
				renderer.inverseGlobalParentOrientation, 0,
				cameraTrackballOrientation, 0);
	}
//*/

	public void repaint() {
		// do nothing
	}

	@Override
	public void onResume() 
	{
		// The activity must call the GL surface view's onResume() on activity onResume().
		super.onResume();
		renderer.onResume();
	}

	@Override
	public void onPause() 
	{
		// The activity must call the GL surface view's onPause() on activity onPause().
		super.onPause();
		renderer.onPause();
	}

	public void pickPart(final int x, final int y, final Handler resultHandler) {
		queueEvent(new Runnable() {
			@Override public void run() {
				resultHandler.sendMessage(resultHandler.obtainMessage(
						PICK_PART_RESULT, renderer.pickPart(x, getHeight() - y), 0)); 
			}
		});
	}

	private void debug(String message) {
		//Log.d(TAG, message);
		((TextView) ((Activity) getContext())
				.findViewById(R.id.debugLabel)).setText(message);
	}

	private String printMotionEvent(MotionEvent ev) {
		StringBuilder s = new StringBuilder();
		switch (ev.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			s.append("Down");
			break;
		case MotionEvent.ACTION_MOVE:
			s.append("Move");
			break;
		case MotionEvent.ACTION_UP:
			s.append("Up");
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			s.append("Pointer Down");
			break;
		case MotionEvent.ACTION_POINTER_UP:
			s.append("Pointer Up");
			break;
		case MotionEvent.ACTION_CANCEL:
			s.append("Cancel");
			break;
		default:
			s.append("Unknown");
			break;
		}
		s.append(' ').append(ev.getPointerCount());
		return s.toString();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		//		if (action != MotionEvent.ACTION_MOVE) debug(printMotionEvent(ev));
		final int actionMask = action & MotionEvent.ACTION_MASK;
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

		if (pointers.size() == 0 && actionMask != MotionEvent.ACTION_DOWN)
			return true;

		pointers.addMovement(ev);

		switch (actionMask) {
		case MotionEvent.ACTION_DOWN: {
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			cameraHandler.onCancel();
			tapHandler.endGyroGrab();
			tapHandler.onFingerDown(pointers);
			break;
		}
		case MotionEvent.ACTION_MOVE:
			if (!pointers.isDragging()) {
				if (pointers.shouldStartDrag()) pointers.startDrag();
				if (pointers.isDragging()) {
					tapHandler.onCancel();
				}
			}
			if (pointers.isDragging()) {
				cameraHandler.onMove(pointers);
			}
			break;
		case MotionEvent.ACTION_UP: // the last finger was lifted
			if (pointers.isDragging()) { // end of one finger drag
				pointers.computeCurrentVelocity();

				if (pointers.shouldFling()) {
					cameraHandler.onFling(pointers);
				} else {
					cameraHandler.onCancel();
				}

				tapHandler.endGyroGrab();
				pointers.endDrag();
			} else /* if (second finger wasn't just released and we aren't doing a 2 finger fling) */ { // end of tap
				tapHandler.onTap(pointers);
				pointers.endDrag();
			}
			pointers.clearUpPointers();
			break;
		case MotionEvent.ACTION_CANCEL:
			if (!pointers.isDragging()) {
				tapHandler.onCancel();
			} else {
				cameraHandler.onCancel();
			}
			tapHandler.endGyroGrab();
			pointers.endDrag();
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			if (pointers.size() == 2) { // exactly two fingers were on the screen and active, and one of them was lifted
				if (pointers.isDragging()) { // two fingers were placed down without moving, and now one is being lifted
					pointers.computeCurrentVelocity();

					if (pointers.shouldFling()) {
						cameraHandler.onFling(pointers);
					} else {
						cameraHandler.onCancel();
					}

					tapHandler.endGyroGrab();
					pointers.endDrag();
				}
			}
			pointers.clearUpPointers();
			break;
		}
		return true;
	}

	/*
  signals:
    void partClicked(BVHNode* node,Rotation rot,RotationLimits rotLimit,Position pos);
    void partClicked(int part);
    void propClicked(Prop* prop);

    void partDragged(BVHNode* node,double changeX,double changeY,double changeZ);

    void propDragged(Prop* prop,double changeX,double changeY,double changeZ);
    void propRotated(Prop* prop,double changeX,double changeY,double changeZ);
    void propScaled(Prop* prop,double changeX,double changeY,double changeZ);

    void backgroundClicked();
    void animationSelected(Animation* animation);

    void storeCameraPosition(int num);
    void restoreCameraPosition(int num);

  public slots:
    void resetCamera();
    void protectFrame(bool on);
    void selectPart(int part);

  protected slots:
    void draw();

	 */
	void emit(int i) {}
	int partClicked(BVHNode node, Rotation rot, RotationLimits rotLimit, Position pos) { return 0; }
	int partClicked(int part) { return 0; }
	//    int propClicked(Prop* prop) { return 0; }

	int partDragged(BVHNode node,double changeX,double changeY,double changeZ) { return 0; }

	//    int propDragged(Prop* prop,double changeX,double changeY,double changeZ) { return 0; }
	//    int propRotated(Prop* prop,double changeX,double changeY,double changeZ) { return 0; }
	//    int propScaled(Prop* prop,double changeX,double changeY,double changeZ) { return 0; }

	int backgroundClicked() { return 0; }
	int animationSelected(Animation animation) { return 0; }

	int storeCameraPosition(int num) { return 0; }
	int restoreCameraPosition(int num) { return 0; }

	public int getPartHighlighted() {
		return partHighlighted;
	}

	public void setPartHighlighted(int partHighlighted) {
		this.partHighlighted = partHighlighted;
	}

	public int getMirrorSelected() {
		return mirrorSelected;
	}

	public void setMirrorSelected(int mirrorSelected) {
		this.mirrorSelected = mirrorSelected;
	}

}
