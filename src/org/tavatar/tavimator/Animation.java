package org.tavatar.tavimator;

import java.io.FileNotFoundException;
import java.io.FileReader;

public abstract class Animation {
    public static enum IKPartType {
      IK_LHAND,
      IK_RHAND,
      IK_LFOOT,
      IK_RFOOT,
      NUM_IK
    }

    public static enum FigureType {
      FIGURE_MALE,
      FIGURE_FEMALE,
      NUM_FIGURES
    }

    public abstract int getNumberOfFrames();
    public abstract BVHNode getMotion();
    public abstract BVHNode getNode(int jointNumber);
    public abstract float frameTime();
    public abstract int getLoopInPoint();
    public abstract int getLoopOutPoint();
    public abstract float getAvatarScale();
    public abstract FigureType getFigureType();
    
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
