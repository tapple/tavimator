package org.tavatar.tavimator;

import android.util.Log;

/**
 * adapted from bvhnode.cpp and bvhnode.h in QAvimator
 * @author tapple
 *
 */
public class FrameData {
	private static final String TAG = "FrameData";

	private int m_frameNumber;

	private Rotation m_rotation;
	private Position m_position;

	private boolean m_easeIn;
	private boolean m_easeOut;

	public FrameData() {
		//  qDebug(QString("FrameData(%1)").arg((unsigned long)this));
		m_frameNumber=0;
		m_easeIn=false;
		m_easeOut=false;
	}

	public FrameData(int num,Position pos,Rotation rot)	{
		//  qDebug(QString("FrameData(%1): frame %2  pos %3,%4,%5 rot %6,%7,%8").arg((unsigned long) this).arg(frame).arg(pos.x).arg(pos.y).arg(pos.z).arg(rot.x).arg(rot.y).arg(rot.z));
		m_frameNumber=num;
		m_rotation=rot;
		m_position=pos;
		m_easeIn=false;
		m_easeOut=false;
	}

	public int frameNumber() {
		return m_frameNumber;
	}

	public void setFrameNumber(int frame) {
		m_frameNumber=frame;
	}


	public Position position() {
		return m_position;
	}

	public Rotation rotation() {
		return m_rotation;
	}

	public void setPosition(Position pos) {
		m_position=pos;
	}

	public void setRotation(Rotation rot) {
		//  qDebug(QString("FrameData::setRotation(<%1,%2,%3>)").arg(m_rotation.x).arg(m_rotation.y).arg(m_rotation.z));
		//  qDebug(QString("FrameData::setRotation(<%1,%2,%3>)").arg(rot.x).arg(rot.y).arg(rot.z));
		m_rotation.x=rot.x;
		m_rotation.y=rot.y;
		m_rotation.z=rot.z;
		//  qDebug(QString("FrameData::setRotation(<%1,%2,%3>)").arg(m_rotation.x).arg(m_rotation.y).arg(m_rotation.z));
		//  m_rotation=rot;
	}


	public boolean easeIn() {
		return m_easeIn;
	}

	public boolean easeOut() {
		return m_easeOut;
	}

	public void setEaseIn(boolean state) {
		m_easeIn=state;
	}

	public void setEaseOut(boolean state) {
		m_easeOut=state;
	}


	// for debugging purposes, dumps all frame data to debug console
	public void dump() {
		Log.d(TAG, "FrameData::dump()");
		Log.d(TAG, "Frame Number: " + m_frameNumber);
		Log.d(TAG, "Rotation: " + m_rotation.x + ", " + m_rotation.y + ", " + m_rotation.z);
		Log.d(TAG, "Position: " + m_position.x + ", " + m_position.y + ", " + m_position.z);
		Log.d(TAG, "Ease in/out: " + m_easeIn + " / " + m_easeOut);
	}
}
