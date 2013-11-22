/*
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Copyright (C) 2006 by Vinay Pulim.
 * All rights reserved.
 *
 */

package org.tavatar.tavimator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Animation {
	private static final String TAG = "Animation";

	private static final String DEFAULT_POSE = "data/TPose.avm";
	// private final String DEFAULT_POSE = "data/Relaxed.bvh";
	public static final String LIMITS_FILE = "data/SL.lim";

	/* ###IK###
	public static enum IKPartType {
		IK_LHAND,
		IK_RHAND,
		IK_LFOOT,
		IK_RFOOT,
		NUM_IK
	}
	 */

	public static enum FigureType {
		FIGURE_MALE,
		FIGURE_FEMALE,
		NUM_FIGURES
	}

	//public enum { MAX_PARTS=64 };

	private Context context;

	private BVH bvh;
	private BVHNode frames;
	private BVHNode positionNode;

	private FigureType figureType;

	// this flag shows if the animation has been worked on and not yet saved
	private boolean isDirty;

	// display the avatar at another scale (1.0 is default)
	private float avatarScale;

	private int totalFrames;
	private int framesPerSecond;

	private boolean loop;            // should we loop when using stepForward()?
	private int loopInPoint;
	private int loopOutPoint;

	private boolean mirrored;
	private boolean limits;

	/* ####IK####
    private boolean[] ikOn = new boolean[IKPartType.NUM_IK];
    private IKTree ikTree;
	 */

	private String dataPath;

	public Animation(Context context, BVH newBVH) throws IOException {
		this(context, newBVH, "");
	}

	public Animation(Context context, BVH newBVH, String bvhFile) throws IOException {
		if (bvhFile.length() > 0) {
			initialize(context, newBVH, BVH.openBVHFile(bvhFile), BVH.isAvm(bvhFile));
		} else {
			initialize(context, newBVH, BVH.openBVHFile(context.getAssets().open(DEFAULT_POSE)), true);
		}
	}
	
	public Animation(Context context, BVH newBVH, InputStream bvhFile, boolean isAvm) throws IOException {
		initialize(context, newBVH, BVH.openBVHFile(bvhFile), isAvm);
	}

	public Animation(Context context, BVH newBVH, Reader bvhFile, boolean isAvm) throws IOException {
		initialize(context, newBVH, bvhFile, isAvm);
	}

	private void initialize(Context context, BVH newBVH, Reader bvhFile, boolean isAvm) throws IOException {
		this.context = context;
		totalFrames = 0;
		mirrored = false;

		Log.d(TAG, "Animation.Animation(" + hashCode() + ")");

		bvh=newBVH;
		if(bvh == null) {
			Log.d(TAG, "Animation.Animation(): BVH initialisation failed.");
			return;
		}

		// load BVH that defines motion
		loadBVH(bvhFile, isAvm);

		calcPartMirrors();
		useRotationLimits(true);
		setNumberOfFrames(bvh.lastLoadedNumberOfFrames);
		setAvatarScale(bvh.lastLoadedAvatarScale);
		setFigureType(bvh.lastLoadedFigureType);
		setLoopInPoint(bvh.lastLoadedLoopIn);
		setLoopOutPoint(bvh.lastLoadedLoopOut);
		setFrameTime(bvh.lastLoadedFrameTime);
		positionNode=bvh.lastLoadedPositionNode;
		addKeyFrameAllJoints(0);

		/* ###IK###
		ikTree.set(frames);
		setIK(IK_LHAND, false);
		setIK(IK_RHAND, false);
		setIK(IK_LFOOT, false);
		setIK(IK_RFOOT, false);
		 */

		setLoop(false);
		setDirty(false);
	}

	public void loadBVH(String bvhFile) throws IOException {
		Log.d(TAG, "Animation.loadBVH(" + bvhFile + ")");
		InputStream limFile = context.getAssets().open(LIMITS_FILE);
		frames=bvh.animRead(bvhFile, limFile);
	}

	public void loadBVH(Reader bvhFile, boolean isAvm) throws IOException {
		Log.d(TAG, "Animation.loadBVH(" + bvhFile + ")");
		InputStream limFile = context.getAssets().open(LIMITS_FILE);
		frames=bvh.animRead(bvhFile, limFile, isAvm);
	}

	public void saveBVH(String bvhFile) throws IOException {
		Log.d(TAG, "Animation.saveBVH(" + bvhFile + ")");
		bvh.animWrite(this,bvhFile);
		setDirty(false);
	}

	// get frames per second
	public int fps() {
		if(frames != null) {
			return framesPerSecond;
		} else {
			return 0;
		}
	}

	// set frames per second
	public void setFPS(int fps) {
		if(frames != null) {
			framesPerSecond=fps;
			setDirty(true);
		}
	}

	// convenience
	// convenience functions
	public void setFrameTime(float frameTime) {
		framesPerSecond=(int) (1.0/frameTime+0.5);
	}

	// convenience
	public float frameTime() {
		return 1.0f/framesPerSecond;
	}

	public int getNumberOfFrames() {
		// Log.d(TAG, "Animation.getNumberOfFrames()=%d",totalFrames);
		return totalFrames;
	}

	public void setNumberOfFrames(int num) {
		Log.d(TAG, "Animation.setNumberOfFrames(" + num + ")");
		totalFrames=num;
		setDirty(true);
		emit.numberOfFrames(num);
	}

	public void setMirrored(boolean mirror) {
		mirrored=mirror;
	}

	public boolean getMirrored() {
		return mirrored;
	}

	public void setEaseIn(BVHNode node, int frameNum, boolean state) {
		if(node == null) {
			Log.d(TAG, "Animation.setEaseIn(): node==0!");
			return;
		}

		if(node.isKeyframe(frameNum)) {
			setDirty(true);
			node.setEaseIn(frameNum,state);
			// tell main class that the keyframe has changed
			emit.redrawTrack(getPartIndex(node));
		}
	}

	public void setEaseOut(BVHNode node, int frameNum, boolean state) {
		if(node == null) {
			Log.d(TAG, "Animation.setEaseOut(): node==0!");
			return;
		}

		if(node.isKeyframe(frameNum)) {
			setDirty(true);
			node.setEaseOut(frameNum,state);
			// tell main class that the keyframe has changed
			emit.redrawTrack(getPartIndex(node));
		}
	}

	public boolean easeIn(BVHNode node, int frameNum) {
		if(node == null) {
			Log.d(TAG, "Animation.easeIn(): node==0!");
			return false;
		}

		if(node.isKeyframe(frameNum)) {
			return node.easeIn(frameNum);
		}
		Log.d(TAG, "Animation.easeIn(" + node.name() + "): requested easeIn for non-keyframe!");
		return false;
	}

	public boolean easeOut(BVHNode node, int frameNum) {
		if(node == null) {
			Log.d(TAG, "Animation.easeOut(): node==0!");
			return false;
		}

		if(node.isKeyframe(frameNum)) {
			return node.easeOut(frameNum);
		}
		Log.d(TAG, "Animation.easeOut(" + node.name() + "): requested easeOut for non-keyframe!");
		return false;
	}

	public void setLoopInPoint(int inFrame) {
		//  Log.d(TAG, "Animation.setLoopInPoint(%d)",inFrame);
		loopInPoint=inFrame;
		setDirty(true);
	}

	public int getLoopInPoint() {
		return loopInPoint;
	}

	public void setLoopOutPoint(int outFrame) {
		//  Log.d(TAG, "Animation.setLoopOutPoint(%d)",outFrame);
		loopOutPoint=outFrame;
		setDirty(true);
	}

	public int getLoopOutPoint() {
		return loopOutPoint;
	}

/* ###IK###
	private void applyIK(String name) {
		BVHNode node=bvh.bvhFindNode(frames,name);

		Rotation rot=node.frameData(frame).rotation();

		if(node != null) {
			//    	    for (int i=0; i<3; i++) {
			rot.x+=node.ikRot.x;
			rot.y+=node.ikRot.y;
			rot.z+=node.ikRot.z;


			node.ikRot.x=0;
			node.ikRot.y=0;
			node.ikRot.z=0;
    	      node.frame[frame][i] += node.ikRot[i];
    	      node.ikRot[i] = 0;
			//    	      node.ikOn = false;

			setDirty(true);
			addKeyFrame(node);
			node.setKeyframeRotation(frame,rot);
			emit.redrawTrack(getPartIndex(node));
			//    	    }
		}
	}

	private void setIK(IKPartType part, boolean flag) {
		if(ikOn[part]==flag) return;

		ikOn[part]=flag;

		if(flag)
		{
			switch(part)
			{
			case IK_LHAND: ikTree.setGoal(frame,"lHand"); break;
			case IK_RHAND: ikTree.setGoal(frame,"rHand"); break;
			case IK_LFOOT: ikTree.setGoal(frame,"lFoot"); break;
			case IK_RFOOT: ikTree.setGoal(frame,"rFoot"); break;
			default: break;
			}
		}
		else
		{
			switch(part)
			{
			case IK_LHAND:
				applyIK("lHand");
				applyIK("lForeArm");
				applyIK("lShldr");
				applyIK("lCollar");
				if(!ikOn[IK_RHAND])
				{
					applyIK("chest");
					applyIK("abdomen");
				}
				break;
			case IK_RHAND:
				applyIK("rHand");
				applyIK("rForeArm");
				applyIK("rShldr");
				applyIK("rCollar");
				if (!ikOn[IK_LHAND])
				{
					applyIK("chest");
					applyIK("abdomen");
				}
				break;
			case IK_LFOOT:
				applyIK("lThigh");
				applyIK("lShin");
				applyIK("lFoot");
				break;
			case IK_RFOOT:
				applyIK("rThigh");
				applyIK("rShin");
				applyIK("rFoot");
				break;
			default:
				break;
			}
		}
	}

	public void setIK(BVHNode node, boolean flag) {
		String jointName=node.name();

		if(jointName.equals("lHand") ||
				jointName.equals("lForeArm") ||
				jointName.equals("lShldr") ||
				jointName.equals("lCollar"))
		{
			setIK(IK_LHAND, flag);
		}
		else if(jointName.equals("rHand") ||
				jointName.equals("rForeArm") ||
				jointName.equals("rShldr") ||
				jointName.equals("rCollar"))
		{
			setIK(IK_RHAND, flag);
		}
		else if(jointName.equals("lThigh") ||
				jointName.equals("lShin") ||
				jointName.equals("lFoot"))
		{
			setIK(IK_LFOOT, flag);
		}
		else if(jointName.equals("rThigh") ||
				jointName.equals("rShin") ||
				jointName.equals("rFoot"))
		{
			setIK(IK_RFOOT, flag);
		}
	}

	public boolean getIK(BVHNode node) {
		String jointName=node.name();

		if(jointName.equals("lHand") ||
				jointName.equals("lForeArm") ||
				jointName.equals("lShldr") ||
				jointName.equals("lCollar"))
		{
			return getIK(IK_LHAND);
		}
		else if(jointName.equals("rHand") ||
				jointName.equals("rForeArm") ||
				jointName.equals("rShldr") ||
				jointName.equals("rCollar"))
		{
			return getIK(IK_RHAND);
		}
		else if(jointName.equals("lThigh") ||
				jointName.equals("lShin") ||
				jointName.equals("lFoot"))
		{
			return getIK(IK_LFOOT);
		}
		else if(jointName.equals("rThigh") ||
				jointName.equals("rShin") ||
				jointName.equals("rFoot"))
		{
			return getIK(IK_RFOOT);
		}
		return false;
	}

	private boolean getIK(IKPartType part) {
		return ikOn[part];
	}

	private void solveIK() {
		bvh.bvhResetIK(frames);
		if(ikOn[IK_LFOOT]) getEndSite("lFoot").ikOn=true;
		if(ikOn[IK_RFOOT]) getEndSite("rFoot").ikOn=true;
		if(ikOn[IK_LHAND]) getEndSite("lHand").ikOn=true;
		if(ikOn[IK_RHAND]) getEndSite("rHand").ikOn=true;

		//  ikTree.setJointLimits(true);
		ikTree.solve(frame);
	}
	 */

	public void setRotation(int frame, BVHNode node, float x, float y, float z) {
		setRotation(frame, node, new Rotation(x,y,z));
	}

	public void setRotationFromMatrix(int frame, BVHNode node, float[] matrix) {
		setRotation(frame, node, Math3D.toEulerAngles(new Rotation(), matrix, node.channelOrder));
	}

	public void setRotation(int frame, BVHNode node, Rotation rot) {
		if (node != null) {
			//			Log.v(TAG, "Animation.setRotation(" + node.name() + ")");

			/* ###IK###
			for(int i=0;i<NUM_IK;i++)
			{
				if(ikOn[i])
				{
					solveIK();
					break;
				}
			}
			 */

			if(node.isKeyframe(frame)) {
				node.setKeyframeRotation(frame, rot);
			} else {
				node.addKeyframe(frame,node.frameData(frame).position(), rot);
				setEaseIn(node, frame, Settings.easeIn());
				setEaseOut(node, frame, Settings.easeOut());
			}

			//      node.dumpKeyframes();
			BVHNode mirrorNode=node.getMirror();
			if(mirrored && mirrorNode != null) {
				Rotation mirrorRot = new Rotation(rot.x, -rot.y, -rot.z);
				// new keyframe system
				if(mirrorNode.isKeyframe(frame)) {
					mirrorNode.setKeyframeRotation(frame, mirrorRot);
				} else {
					mirrorNode.addKeyframe(frame,node.frameData(frame).position(), mirrorRot);
					setEaseIn(mirrorNode,frame,Settings.easeIn());
					setEaseOut(mirrorNode,frame,Settings.easeOut());
				}

				// tell timeline that this mirrored keyframe has changed (added or changed is the same here)
				emit.redrawTrack(getPartIndex(mirrorNode));
			}
			setDirty(true);
			// tell timeline that this keyframe has changed (added or changed is the same here)
			emit.redrawTrack(getPartIndex(node));
			emit.frameChanged(frame);
		} else {
			Log.d(TAG, "Animaiton.setRotation(): node==0!");
		}
	}

	public Rotation getRotation(int frame, BVHNode node){
		if(node != null) {
			return node.frameData(frame).rotation();
		}
		Log.d(TAG, "Animation.getRotation(): node==0!");
		return new Rotation();
	}

	public void useRotationLimits(boolean flag) {
		limits=flag;
		/* ###IK###
		ikTree.setJointLimits(flag);
		 */
	}

	public RotationLimits getRotationLimits(BVHNode node) {
		if(node != null) {
			ChannelLimits x, y, z;

			if(node.type == BVHNodeType.BVH_POS) {
				x = new ChannelLimits(0, 0);
				y = new ChannelLimits(0, 0);
				z = new ChannelLimits(0, 0);
			} else if(limits) {
				x = bvh.bvhGetChannelLimits(node, BVHChannelType.BVH_XROT);
				y = bvh.bvhGetChannelLimits(node, BVHChannelType.BVH_YROT);
				z = bvh.bvhGetChannelLimits(node, BVHChannelType.BVH_ZROT);
			} else {
				x = new ChannelLimits(-180, 180);
				y = new ChannelLimits(-180, 180);
				z = new ChannelLimits(-180, 180);
			}

			return new RotationLimits(node.name(), x.min, x.max, y.min, y.max, z.min, z.max);
		}
		Log.d(TAG, "Animation.getRotationLimits(): node==0!");
		return new RotationLimits("", 0, 0, 0, 0, 0, 0);
	}

	public BVHOrderType getRotationOrder(String jointName) {
		BVHNode node=bvh.bvhFindNode(frames,jointName);
		if(node != null) {
			return node.channelOrder;
		}
		return BVHOrderType.BVH_XYZ;
		// return 0; // zero was not one of the options in the C++ enum. 
	}

	public void setPosition(int frame, float x, float y, float z) {
		/* ###IK###
		for(int i=0;i<NUM_IK;i++) {
			if(ikOn[i]) {
				solveIK();
				break;
			}
		}
		 */
		// new keyframe system
		if(positionNode.isKeyframe(frame)) {
			positionNode.setKeyframePosition(frame, new Position(x,y,z));
		} else {
			positionNode.addKeyframe(frame, new Position(x,y,z), new Rotation());
			setEaseIn(positionNode,frame,Settings.easeIn());
			setEaseOut(positionNode,frame,Settings.easeOut());
		}
		setDirty(true);
		// tell timeline that this keyframe has changed (added or changed is the same here)
		emit.redrawTrack(0);
		emit.frameChanged(frame);
	}

	public Position getPosition(int frame) {
		return positionNode.frameData(frame).position();
	}

	public String getPartName(int index) {
		//  exception fot position pseudonode
		if(index==0) return positionNode.name();
		return bvh.bvhGetName(frames,index);
	}

	public int getPartIndex(BVHNode node) {
		if(node==positionNode) return 0;
		return bvh.bvhGetIndex(frames,node.name());
	}

	public BVHNode getMotion() {
		return frames;
	}

	public BVHNode getEndSite(String rootName) {
		BVHNode node=bvh.bvhFindNode(frames,rootName);
		while(node != null && node.numChildren() > 0) {
			node=node.child(0);
		}
		return node;
	}

	private void recursiveAddKeyFrame(int frame, BVHNode joint) {
		if(joint.type != BVHNodeType.BVH_END)
			addKeyFrame(frame, joint);

		for(int i=0;i<joint.numChildren();i++)
			recursiveAddKeyFrame(frame, joint.child(i));

		setDirty(true);
	}

	public void addKeyFrameAllJoints(int frame) {
		addKeyFrame(frame, getNode(0));
		recursiveAddKeyFrame(frame, frames);
	}

	public void addKeyFrame(int frame, BVHNode joint) {
		joint.addKeyframe(frame,getPosition(frame),getRotation(frame, joint));

		setEaseIn(joint, frame, Settings.easeIn());
		setEaseOut(joint, frame, Settings.easeOut());

		setDirty(true);

		emit.redrawTrack(getPartIndex(joint));
		emit.frameChanged(frame);
	}

	private boolean isKeyFrameHelper(int frame, BVHNode joint) {
		if(joint.isKeyframe(frame))
			return true;

		for (int i = 0; i < joint.numChildren(); i++) {
			if(isKeyFrameHelper(frame, joint.child(i))) {
				return true;
			}
		}

		return false;
	}

	public boolean isKeyFrame(int frame, String jointName) {
		if(jointName.length() == 0) {
			return isKeyFrame(frame);
		} else {
			BVHNode node=bvh.bvhFindNode(frames,jointName);
			return node.isKeyframe(frame);
		}
		// Log.d(TAG, "Animation.isKeyFrame('" + jointName + "'): no node found.");
	}

	public boolean isKeyFrame(int frame) {
		if(positionNode.isKeyframe(frame)) return true;
		return isKeyFrameHelper(frame, frames);
	}

	private boolean isKeyFrame(int frame, int jointNumber) {
		final BVHNode joint=getNode(jointNumber);
		return joint.isKeyframe(frame);
	}

	// silent = only send signal to timeline
	public void deleteKeyFrame(int frameNum, BVHNode joint, boolean silent) {
		// never delete first keyframe
		if(frameNum != 0) {
			joint.deleteKeyframe(frameNum);
			setDirty(true);
		}

		// if silent is true then only send a signal to the timeline but not to the animation view
		if(!silent) emit.frameChanged(frameNum);
		emit.redrawTrack(getPartIndex(joint));
	}

	public void deleteKeyFrame(int frame, BVHNode joint) {
		deleteKeyFrame(frame, joint, false);
	}

	// slot
	public void deleteKeyFrame(int frameNum, int jointNumber) {
		if(jointNumber > 0) {
			BVHNode joint=getNode(jointNumber);
			if(joint.isKeyframe(frameNum)) deleteKeyFrame(frameNum, joint);
		}
		else if(isKeyFrame(frameNum)) deleteKeyFrameAllJoints(frameNum);
	}

	private void recursiveDeleteKeyFrame(int frame, BVHNode joint) {
		deleteKeyFrame(frame, joint);

		for(int i=0;i<joint.numChildren();i++)
			recursiveDeleteKeyFrame(frame, joint.child(i));
	}

	public void deleteKeyFrameAllJoints(int frame) {
		// never delete the first keyframe
		if(frame==0) return;
		deleteKeyFrame(frame, getNode(0));
		recursiveDeleteKeyFrame(frame, frames);
	}

	public boolean toggleKeyFrame(int frame, BVHNode node) {
		//  Log.d(TAG, "Animation.toggleKeyFrame(node): node %ld",(unsigned long) node);
		if(node == null) {
			return toggleKeyFrameAllJoints(frame);
		} else {
			if (node.isKeyframe(frame)) {
				deleteKeyFrame(frame, node);
				return false;
			} else {
				addKeyFrame(frame, node);
				return true;
			}
		}
	}

	// returns TRUE if frame is now a keyframe for entire animation, FALSE if not
	public boolean toggleKeyFrameAllJoints(int frame) {
		if(frame==0)
			return true;  // first frame will always stay keyframe

		if(isKeyFrame(frame)) {
			deleteKeyFrameAllJoints(frame);
			return false;
		} else {
			addKeyFrameAllJoints(frame);
			return true;
		}
	}

	public void cutFrame(int frame) {
		// copy frame data into copy buffer
		copyFrame(frame);
		// always delete frame from all tracks
		deleteFrame(frame, 0);
	}

	public void copyFrame(int frame) {
		bvh.bvhGetFrameData(frames,frame);
	}

	public void pasteFrame(int frame) {
		bvh.bvhSetFrameData(frames,frame);
		addKeyFrameAllJoints(frame);
	}

	private void calcPartMirrors() {
		String name;
		String n;

		// start at node index 1
		int i=1;
		// go through all nodes by index, stop if a node has no name (=does not exist =end of list)
		while((n=bvh.bvhGetName(frames, i)).length() > 0) {
			// find node by name
			BVHNode node=bvh.bvhFindNode(frames,n);
			// copy the name
			name=n;
			// create a mirrored name (first letter r becomes l, otherwise first letter becomes r)
			if(n.startsWith("r")) name = "l" + name.substring(1);
			else name = "r" + name.substring(1);

			// check if mirrored name is valid, get the node with that name
			BVHNode m = bvh.bvhFindNode(frames,name);
			if(m != null) {
				// name was valid, record this node as mirror of the current node
				// keep the index number for une in AnimationView later
				node.setMirror(m,bvh.bvhGetIndex(frames,m.name()));
			}
			// next node
			i++;
		}
	}

	public int numKeyFrames(int jointNumber) {
		BVHNode node=bvh.bvhFindNode(frames,getPartName(jointNumber));
		//  Log.d(TAG, String("Animation.numKeyFrames(): joint number %1 has %2 keyframes").arg(jointNumber).arg(node.numKeyFrames));
		return node.numKeyframes();
	}

	// copies the position and rotation data of one body part to another key frame position
	public void copyKeyFrame(int jointNumber, int from, int to) {
		// move keyframe in copy mode
		moveKeyFrame(jointNumber,from,to,true);
	}

	// moves the position and rotation data of one body part to another key frame position
	public void moveKeyFrame(int jointNumber, int from, int to, boolean copy) {
		Log.d(TAG, "Animation.moveKeyFrame(): jointNumber: " + jointNumber);

		// make sure we don't drag a trail of mirror keys behind
		setMirrored(false);

		// get the joint structure
		BVHNode joint=getNode(jointNumber);
		final FrameData frameData = joint.frameData(from);
		//  frameData.dump();

		// block all further signals to avoid flickering
		blockSignals(true);

		// silently (true) delete key frame if not copy mode
		// we do copy mode here to avoid code duplication
		if(!copy) deleteKeyFrame(from, joint, true);

		// move rotation or position of the body part
		if(joint.type == BVHNodeType.BVH_POS) {
			Position pos=frameData.position();
			setPosition(to, pos.x,pos.y,pos.z);
		} else {
			Rotation rot = frameData.rotation();
			setRotation(to, joint, rot.x, rot.y, rot.z);
		}
		// only now set ease in/out, because setRotation/setPosition sets to default when the
		// target position has no keyframe yet
		joint.setEaseIn(to,frameData.easeIn());
		joint.setEaseOut(to,frameData.easeOut());
		// now re-enable signals so we get updates on screen
		blockSignals(false);
	}

	public void moveKeyFrame(int jointNumber, int from, int to) {
		moveKeyFrame(jointNumber, from, to, false);
	}

	public boolean compareFrames(BVHNode node, int key1, int key2) {
		if(node != null) return node.compareFrames(key1,key2);
		Log.d(TAG, "Animation.compareFrames(node): node==0!");
		return false;
	}

	public FrameData keyframeDataByIndex(int jointNumber, int index) {
		BVHNode joint = getNode(jointNumber);
		return joint.keyframeDataByIndex(index);
	}

	public BVHNode getNode(int jointNumber) {
		//####
		// joint number 0 needs to return the hip position pseudonode
		if(jointNumber==0) return positionNode;
		// get the joint structure
		//		Log.v(TAG, "getNode " + jointNumber + " " + getPartName(jointNumber));
		return bvh.bvhFindNode(frames,getPartName(jointNumber));
	}

	private void insertFrameHelper(int frame, BVHNode joint) {
		joint.insertFrame(frame);
		for(int i=0;i<joint.numChildren();i++)
			insertFrameHelper(frame, joint.child(i));
	}

	// slot
	public void insertFrame(int pos, int track) {
		if(track==-1) {
			// insert positional frame
			BVHNode joint = getNode(0);
			if(joint != null) joint.insertFrame(pos);
			// insert all rotational frames
			insertFrameHelper(pos, frames);
		} else {
			BVHNode joint=getNode(track);
			if(joint != null) joint.insertFrame(pos);
		}
		setDirty(true);
	}

	// recursively remove frames from joint and all its children
	private void deleteFrameHelper(int frame, BVHNode joint) {
		//  Log.d(TAG, "Animation.deleteFrameHelper(joint %s,frame %d)",joint.name().toLatin1().constData(),frame);
		joint.deleteFrame(frame);
		for(int i=0;i<joint.numChildren();i++)
			deleteFrameHelper(frame, joint.child(i));
		emit.redrawTrack(getPartIndex(joint));
	}

	// delete frame from a joint, if track==0 recursively delete from all joints
	// slot
	public void deleteFrame(int pos, int track) {
		//  Log.d(TAG, "Animation.deleteFrame(joint %d,frame %d)",track,frame);

		if(track==-1) {
			// delete positional frame
			BVHNode joint=getNode(0);
			if(joint != null) joint.deleteFrame(pos);
			// delete all rotational frames
			deleteFrameHelper(pos, frames);
		} else {
			BVHNode joint=getNode(track);
			if(joint != null) joint.deleteFrame(pos);
		}
		setDirty(true);
	}

	private void optimizeHelper(BVHNode joint) {
		if(joint.type != BVHNodeType.BVH_END) {
			joint.optimize();
			emit.redrawTrack(getPartIndex(joint));
		}

		for(int i=0;i<joint.numChildren();i++)
			optimizeHelper(joint.child(i));
	}

	public void optimize() {
		optimizeHelper(positionNode);
		optimizeHelper(frames);
		setDirty(true);
	}

	private void mirrorHelper(BVHNode joint) {
		// make sure only to mirror one side of l/r joints, and joints that have no mirror node
		if(!joint.name().startsWith("l")) {
			joint.mirror();
			emit.redrawTrack(getPartIndex(joint));
			if(joint.getMirror() != null)
				emit.redrawTrack(joint.getMirrorIndex());
		}

		for(int i=0;i<joint.numChildren();i++)
			mirrorHelper(joint.child(i));
	}

	// mirror a joint or the whole animation, if joint==0
	public void mirror(BVHNode joint) {
		if(joint == null) {
			positionNode.mirror();
			mirrorHelper(frames);
		} else {
			joint.mirror();
			emit.redrawTrack(getPartIndex(joint));
			if(joint.getMirror() != null)
				emit.redrawTrack(joint.getMirrorIndex());
		}
		setDirty(true);
	}

	public boolean dirty() {
		return isDirty;
	}

	public void setDirty(boolean state) {
		isDirty=state;
		emit.animationDirty(state);
	}

	public void setLoop(boolean on) {
		loop=on;
	}

	public boolean getLoop() {
		return loop;
	}

	public float getAvatarScale() {
		return avatarScale;
	}

	public void setAvatarScale(float newScale) {
		avatarScale=newScale;
	}

	public FigureType getFigureType() {
		return figureType;
	}

	public void setFigureType(FigureType type) {
		// safety check if figure is valid
		//if(type>=0 && type<NUM_FIGURES)
		figureType=type;
	}

	/* Qt signals and slots
  public slots:
    void deleteKeyFrame(int frame, int jointNumber);
    void insertFrame(int frame, int jointNumber);
    void deleteFrame(int frame, int jointNumber);

    // advances curentPlayTime and sends appropriate currentFrame signals
    void playbackTimeout();

  signals:
*/

	public interface OnAnimationChangeListener {
		public void numberOfFrames(int num);
		public void redrawTrack(int track);
		public void frameChanged(int frame);
		public void animationDirty(boolean state);
	}

	private class NullListener implements OnAnimationChangeListener {
		public void numberOfFrames(int num) {}
		public void redrawTrack(int track) {}
		public void frameChanged(int frame) {}
		public void animationDirty(boolean state) {}
	}

	private OnAnimationChangeListener emit = new NullListener();

	public void setListener(OnAnimationChangeListener listener) {
		if (listener == null) emit = new NullListener();
		else emit = listener;
	}

	void blockSignals(boolean yes) {}


	/*
    	try {
    		animationFile = new FileReader(file);
    	} catch(FileNotFoundException e) {
    		// TODO: figure out how to handle this
    		QMessageBox.critical(0,QObject.tr("File not found"),QObject.tr("BVH File not found: %1").arg(file.toLatin1().constData()));
    		return NULL;
    	}

    	try {
    		limit = new FileReader(limFile);
    	} catch(FileNotFoundException e) {
    		// TODO: figure out how to handle this
    		QMessageBox.critical(0,QObject.tr("Missing Limits File"),QObject.tr("<qt>Limits file not found at:<br>%1</qt>").arg(limFile));
    		return;
    	}

	 */

}
