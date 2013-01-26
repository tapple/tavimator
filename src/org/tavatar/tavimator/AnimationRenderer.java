package org.tavatar.tavimator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import org.tavatar.tavimator.R;
import com.learnopengles.android.common.RawResourceReader;
import com.learnopengles.android.common.ShaderHelper;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class AnimationRenderer implements GLSurfaceView.Renderer {
	private final String TAG = "AnimationRenderer";
	private final AnimationView mView;
	private final Context mActivityContext;
	
	// defines where we start counting opengl ids for parts with multiple animations
	// first animation counts 0-ANIMATION_INCREMENT-1, next ANIMATION_INCREMENT++
	public static final int ANIMATION_INCREMENT = 100;

    private enum DrawMode {
      MODE_PARTS,
      MODE_SKELETON,
      MODE_ROT_AXES
    };
	
	/**
	 * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
	 * of being located at the center of the universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
	 * it positions things relative to our eye.
	 */
	private Camera mCamera;
	private float[] tempViewMatrix = new float[16];

	/** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
	private float[] mProjectionMatrix = new float[16];
	
	/** Allocate storage for the final combined matrix. This will be passed into the shader program. */
	private float[] mMVPMatrix = new float[16];
	
	/** Store our model data in a float buffer. */
	private final FloatBuffer mCubePositions;
	private final FloatBuffer mCubeNormals;
		
	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;				
	
	/** This will be used to pass in the modelview matrix. */
	private int mMVMatrixHandle;
	
	/** This will be used to pass in model position information. */
	private int mPositionHandle;
	
	/** This will be used to pass in model color information. */
	private int mColorHandle;
	
	/** This will be used to enable or disable lighting. */
	private int mLightingHandle;
	
	/** This will be used to pass in model normal information. */
	private int mNormalHandle;

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;	
	
	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;	
	
	/** Size of the normal data in elements. */
	private final int mNormalDataSize = 3;
	
	/** This is a handle to our per-vertex cube shading program. */
	private int mPerVertexProgramHandle;
		
	private SLPartsRenderer figureRenderer = new SLPartsFemale(this);
	
    private boolean skeleton;
    private boolean selecting;
    private int selectName;

    /**
	 * Initialize the model data.
	 */
	public AnimationRenderer(AnimationView view) {
		mView = view;
        mActivityContext = view.getContext();
		mCamera = new Camera(mActivityContext);
		resetCamera();

		// Define points for a cube.		
		
		// X, Y, Z
		final float[] cubePositionData =
		{
				// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
				// if the points are counter-clockwise we are looking at the "front". If not we are looking at
				// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
				// usually represent the backside of an object and aren't visible anyways.
				
				// Front face
				-1.0f, 1.0f, 1.0f,				
				-1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f, 
				-1.0f, -1.0f, 1.0f, 				
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				
				// Right face
				1.0f, 1.0f, 1.0f,				
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, 1.0f,				
				1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f,
				
				// Back face
				1.0f, 1.0f, -1.0f,				
				1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,				
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				
				// Left face
				-1.0f, 1.0f, -1.0f,				
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f, 
				-1.0f, -1.0f, -1.0f,				
				-1.0f, -1.0f, 1.0f, 
				-1.0f, 1.0f, 1.0f, 
				
				// Top face
				-1.0f, 1.0f, -1.0f,				
				-1.0f, 1.0f, 1.0f, 
				1.0f, 1.0f, -1.0f, 
				-1.0f, 1.0f, 1.0f, 				
				1.0f, 1.0f, 1.0f, 
				1.0f, 1.0f, -1.0f,
				
				// Bottom face
				1.0f, -1.0f, -1.0f,				
				1.0f, -1.0f, 1.0f, 
				-1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, 1.0f, 				
				-1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f,
		};	
		
		// X, Y, Z
		// The normal is used in light calculations and is a vector which points
		// orthogonal to the plane of the surface. For a cube model, the normals
		// should be orthogonal to the points of each face.
		final float[] cubeNormalData =
		{												
				// Front face
				0.0f, 0.0f, 1.0f,				
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,				
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				
				// Right face 
				1.0f, 0.0f, 0.0f,				
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,				
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				
				// Back face 
				0.0f, 0.0f, -1.0f,				
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,				
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				
				// Left face 
				-1.0f, 0.0f, 0.0f,				
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,				
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				
				// Top face 
				0.0f, 1.0f, 0.0f,			
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,				
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				
				// Bottom face 
				0.0f, -1.0f, 0.0f,			
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,				
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f
		};
		
		// Initialize the buffers.
		mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
	    .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubePositions.put(cubePositionData).position(0);		
		
		mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
	    .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubeNormals.put(cubeNormalData).position(0);
		
		figureRenderer.load();
		loadFloor();
	}
	
	public void onResume() {
		mCamera.onResume();
	}

	public void onPause() {
		mCamera.onPause();
	}	

	public Camera getCamera() {
		return mCamera;
	}
	
	protected String getVertexShader()
	{
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.color_vertex_shader);
	}
	
	protected String getFragmentShader()
	{
		return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.color_fragment_shader);
	}
	
	/** This will be used to pass in model position information. */
	public int getPositionHandle() {
		return mPositionHandle;
	}
	
	/** This will be used to pass in model color information. */
	public int getColorHandle() {
		return mColorHandle;
	}
	
	/** This will be used to pass in model normal information. */
	public int getNormalHandle() {
		return mNormalHandle;
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) 
	{				
		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		
//		  GLES20.glEnable(GLES20.GL_BLEND);
//		  GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glDisable(GLES20.GL_DITHER);


			
		final String vertexShader = getVertexShader();   		
 		final String fragmentShader = getFragmentShader();			
		
		final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
		final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		
		
		mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, 
				new String[] {"a_Position",  "a_Color", "a_Normal"});								                                							       

		// Set our per-vertex lighting program.
        GLES20.glUseProgram(mPerVertexProgramHandle);
        
        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix"); 
        mColorHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_Color");
        mLightingHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_Lighting");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal");
	}	

//*
	public static void printMatrix(float[] m) {
		System.out.println("" + 
			m[ 0]+" "+ m[ 4]+" "+m[ 8]+" "+m[12]+"\n"+
			m[ 1]+" "+ m[ 5]+" "+m[ 9]+" "+m[13]+"\n"+
			m[ 2]+" "+ m[ 6]+" "+m[10]+" "+m[14]+"\n"+
			m[ 3]+" "+ m[ 7]+" "+m[11]+" "+m[15]);
	}

	public static void printVector(float[] m) {
		for (float f:m) {
			System.out.print(" " + f);
		}
		System.out.println();
	}
//*/

	public void resetCamera() {
		mCamera.initializeCamera(
				 0, 40, 100,
				 0, 40,   0,
				 0,  1,   0);

	}
	
	/**
	 * Answers a color for color picking, basis. Works on a color buffer with as
	 * little as 12 bits of precision, thus supporting 4096 pick indices. The
	 * color is suitable for passing directly to OpenGL
	 * 
	 * @param index
	 * @return
	 */
	public static float[] indexToColor(int index) {
		float[] pickColor = new float[4];
		pickColor[0] = (index >> 4 & 0xF0) / 255f;
		pickColor[1] = (index << 0 & 0xF0) / 255f;
		pickColor[2] = (index << 4 & 0xF0) / 255f;
		pickColor[3] = 1f;
		return pickColor;
	}

	/**
	 * Answers a pick index for the given color for color picking, basis. Works
	 * on a color buffer with as little as 12 bits of precision, thus supporting
	 * 4096 pick indices. Uses the color format as read from glReadPixels(...,
	 * RGBA, UNSIGNED_BYTE, ...)
	 * 
	 * @param index
	 * @return
	 */
	public static int colorToIndex(byte r, byte g, byte b, byte a) {
		return 
				(r & 0xF0) << 4 |
				(g & 0xF0) >> 0 |
				(b & 0xF0) >> 4 ;
	}

	// x and y are already converted to GL pixel coordinates
	public int pickPart(int x, int y) {
		final int SIZE = 5;

		//	  glMatrixMode(GL_PROJECTION);
		//	  glPushMatrix();
		//	  glLoadIdentity();
		//	  gluPickMatrix(x,(viewport[3]-y),5.0,5.0,viewport);
		//	  setProjection();

		GLES20.glScissor(x - SIZE/2, y - SIZE/2, SIZE, SIZE);
		GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // white
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		selecting = true;
		GLES20.glUniform1i(mLightingHandle, 0);        

		drawAnimations();
		//	  drawProps();

		GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

		final ByteBuffer colorBuffer = ByteBuffer.allocate(4);
		GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, colorBuffer);
		colorBuffer.position(0);
		int selection = colorToIndex(colorBuffer.get(), colorBuffer.get(), colorBuffer.get(), colorBuffer.get());
		
		// special case: white (the clear color) is no selection
		if (selection == 4095) selection = -1;
		return selection;

		//  qDebug("AnimationView::pickPart(): %d",name);
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) 
	{
		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);

		// Create a new perspective projection matrix. The height will stay the same
		// while the width will vary as per aspect ratio.
		final float ratio = (float) width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 2000.0f;
		
		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}	

	@Override
	public void onDrawFrame(GL10 glUnused) 
	{
		GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.3f); /* fog color */
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		
		mView.updateSelectionOrientation();
		updateAnimationsTransforms();

		BVHNode selectedNode = mView.getSelectedPart();
        if (selectedNode != null) {
        	mCamera.setOrigin(selectedNode.cachedOrigin());
        }
		
		
		mCamera.updateViewMatrix();
                
        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;        
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);                
        
        GLES20.glUniform1i(mLightingHandle, 1);
        selecting = false;
        
        // Draw some cubes.        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 20.0f, 0.0f, -7.0f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);        
        GLES20.glUniform4f(mColorHandle, 1.0f, 0.0f, 0.0f, 1.0f); // red
        updateUniforms();
        figureRenderer.drawPartNamed("lHand");
                        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, -20.0f, 0.0f, -7.0f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);        
        GLES20.glUniform4f(mColorHandle, 0.0f, 1.0f, 0.0f, 1.0f); // green
        updateUniforms();
        figureRenderer.drawPartNamed("hip");
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 20.0f, -7.0f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);        
        GLES20.glUniform4f(mColorHandle, 0.0f, 0.0f, 1.0f, 1.0f); // blue
        updateUniforms();
        figureRenderer.drawPartNamed("head");
        
        if (selectedNode != null) {
        	float[] selectedOrigin = selectedNode.cachedOrigin();
        	Matrix.setIdentityM(mModelMatrix, 0);
        	Matrix.translateM(mModelMatrix, 0, selectedOrigin[0], selectedOrigin[1], selectedOrigin[2]);
//        	Matrix.translateM(mModelMatrix, 0, 0.0f, -20.0f, -7.0f);
        	System.arraycopy(mModelMatrix, 0, tempViewMatrix, 0, 16);
        	Matrix.multiplyMM(mModelMatrix, 0, tempViewMatrix, 0, mView.getSelectionTrackball().getOrientation(), 0);
        	GLES20.glUniform4f(mColorHandle, 1.0f, 1.0f, 0.0f, 1.0f); // yellow
        	updateUniforms();
        	figureRenderer.drawPartNamed(selectedNode.name());
        }
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);        
        GLES20.glUniform4f(mColorHandle, 0.0f, 1.0f, 1.0f, 1.0f); // cyan
        updateUniforms();
        figureRenderer.drawPartNamed("chest");
        
        drawAnimations();

        Matrix.setIdentityM(mModelMatrix, 0);
        updateUniforms();
        drawFloor();
  
        // uncomment to debug picking
//        pickPart(touchX, touchY);
	}
	
	
	private void updateUniforms() {
		// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mCamera.getViewMatrix(), 0, mModelMatrix, 0);   
        
        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);                
        
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
	}
	
	/**
	 * Draws a cube.
	 */			
	private void drawCube()
	{		
		// Pass in the position information
		mCubePositions.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        		0, mCubePositions);        
                
        GLES20.glEnableVertexAttribArray(mPositionHandle);        
        
        // Pass in the normal information
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
        		0, mCubeNormals);
        
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        
        updateUniforms();
        
        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);                               
	}
	

	private void updateAnimationsTransforms() {
		updateFigureTransforms(mView.getSelectedAnimation(), 0);
/*
		for(int index=0; index < mView.getAnimationCount(); index++) {
			updateFigureTransforms(mView.getAnimationNumber(index), index);
		}
*/
	}
	
	private void updateFigureTransforms(Animation anim, int index) {
	    // int figType = anim.getFigureType().ordinal();
	    int figType = 1;

	    // save current drawing matrix
		float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);

	    // scale drawing matrix to avatar scale specified
	    float scale = anim.getAvatarScale();
	    Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

	    Position pos = anim.getPosition();
	    Matrix.translateM(modelMatrix, 0, pos.x, pos.y, pos.z);

	    // visual compensation
	    Matrix.translateM(modelMatrix, 0, 0, 2, 0);
	    updatePartTransforms(anim.getFrame(), anim.getMotion(), mView.getJoints(figType), modelMatrix);
	}

	private void updatePartTransforms(int frame, BVHNode motion, BVHNode joints, float[] parentMatrix) {
		if(motion == null || joints == null) return;
		System.arraycopy(parentMatrix, 0, motion.cachedTransform, 0, 16);
		Matrix.translateM(motion.cachedTransform, 0, joints.offset[0], joints.offset[1], joints.offset[2]);
		if(motion.type==BVHNodeType.BVH_NO_SL) {
			motion = motion.child(0);
		}

		motion.rotateMatrixForFrame(motion.cachedTransform, frame);

		for(int i = 0; i < motion.numChildren(); i++) {
			updatePartTransforms(frame, motion.child(i), joints.child(i), motion.cachedTransform);
		}
	}
	
	private void drawAnimations() {
		drawFigure(mView.getSelectedAnimation(), 0);
/*
		for(int index=0; index < mView.getAnimationCount(); index++) {
			drawFigure(mView.getAnimationNumber(index), index);
		}
*/
	}
	
	private void drawFigure(Animation anim, int index) {
	    selectName = index*ANIMATION_INCREMENT;
	    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	    GLES20.glDisable(GLES20.GL_CULL_FACE);	    
        drawPart(anim, anim.getMotion(), DrawMode.MODE_PARTS);
	    GLES20.glEnable(GLES20.GL_CULL_FACE);	    
	    selectName = index*ANIMATION_INCREMENT;
        drawPart(anim, anim.getMotion(), DrawMode.MODE_ROT_AXES);
	    selectName = index*ANIMATION_INCREMENT;
	    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        drawPart(anim, anim.getMotion(), DrawMode.MODE_SKELETON);
	    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	}

	private void drawPart(Animation anim, BVHNode motion, DrawMode mode) {
		float[] color = new float[4];

		if(motion == null) return;
		selectName++;
		if(motion.type==BVHNodeType.BVH_NO_SL) {
			selectName++;
			motion = motion.child(0);
		}

/*	
		if(mode == DrawMode.MODE_SKELETON && skeleton && !selecting)
		{
			glColor4f(0,1,1,1);
			glLineWidth(1);
			glBegin(GL_LINES);
			glVertex3f(-joints->offset[0],-joints->offset[1],-joints->offset[2]);
			glVertex3f(0,0,0);
			glEnd();

			if(joints->type!=BVH_ROOT)
			{
				// draw joint spheres in skeleton mode, red for selected parts,
				// blue for hightlighted and green for all others
				if(partSelected==selectName)
					glColor4f(1,0,0,1);
				else if(partHighlighted==selectName)
					glColor4f(0,0,1,1);
				else
					glColor4f(0,1,0,1);

				glutSolidSphere(1,16,16);
			}
		}
*/
		

/*
			if(mode == DrawMode.MODE_ROT_AXES && !selecting && partSelected==selectName)
			{
				switch(motion->channelType[i])
				{
				case BVH_XROT: drawCircle(0,10,xSelect ? 4 : 1); break;
				case BVH_YROT: drawCircle(1,10,ySelect ? 4 : 1); break;
				case BVH_ZROT: drawCircle(2,10,zSelect ? 4 : 1); break;
				default: break;
				}
			}
*/

		if(mode == DrawMode.MODE_PARTS) {
			if(selecting) {
		        GLES20.glUniform4fv(mColorHandle, 1, indexToColor(selectName), 0);
			} else {

				if(anim.getMirrored() && (mView.getMirrorSelected() == selectName || mView.getSelectedPartIndex() == selectName)) {
					GLES20.glUniform4f(mColorHandle, 1.0f, 0.635f, 0.059f, 1.0f); // gold
				} else if(mView.getSelectedPartIndex() == selectName) {
					GLES20.glUniform4f(mColorHandle, 0.6f, 0.3f, 0.3f, 1.0f); // red
				} else if(mView.getPartHighlighted()==selectName) {
					GLES20.glUniform4f(mColorHandle, 0.4f, 0.5f, 0.3f, 1.0f); // green
				} else {
					GLES20.glUniform4f(mColorHandle, 0.6f, 0.5f, 0.5f, 1.0f); // grey peach
//					GLES20.glUniform4f(mColorHandle, 0.9f, 0.667f, 0.561f, 1.0f); // peach
				}
			
/*
			if(anim.getIK(motion)) {
				glGetFloatv(GL_CURRENT_COLOR,color);
				glColor4f(color[0],color[1],color[2]+0.3,1.0f);
			}
*/
			}
			
			System.arraycopy(motion.cachedTransform, 0, mModelMatrix, 0, 16);
		    updateUniforms();
			figureRenderer.drawPartNamed(motion.name());

/*
			for(int index=0; index < propList.size(); index++) {
				Prop* prop=propList.at(index);
				if(prop->isAttached()==selectName) drawProp(prop);
			} // for
*/
		}
		
		for(int i = 0; i < motion.numChildren(); i++) {
			drawPart(anim, motion.child(i), mode);
		}
	}
	
	private FloatBuffer lightTiles;
	private FloatBuffer darkTiles;

	private void loadFloor() {
		final int BYTES_PER_FLOAT = 4;
		final int FLOATS_PER_VEC = 3;
		final int BUFFER_SIZE = 1200;

		lightTiles = ByteBuffer.allocateDirect(BUFFER_SIZE * BYTES_PER_FLOAT * FLOATS_PER_VEC)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		lightTiles.position(0);

		darkTiles = ByteBuffer.allocateDirect(BUFFER_SIZE * BYTES_PER_FLOAT * FLOATS_PER_VEC)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		darkTiles.position(0);

		for(int i = -10; i < 10; i++) {
			for(int j = -10; j < 10; j++) {
				FloatBuffer buf;
				if((i+j) % 2 != 0) { // dark
					buf = darkTiles;
				} else { // light
					buf = lightTiles;
				}

				buf.put( i   *40); buf.put(0); buf.put( j   *40);
				buf.put( i   *40); buf.put(0); buf.put((j+1)*40);
				buf.put((i+1)*40); buf.put(0); buf.put((j+1)*40);

				buf.put((i+1)*40); buf.put(0); buf.put( j   *40);
				buf.put( i   *40); buf.put(0); buf.put( j   *40);
				buf.put((i+1)*40); buf.put(0); buf.put((j+1)*40);
			}
		}
	}
	
	private void releaseFloor() {
		lightTiles.limit(0);
		lightTiles = null;
		darkTiles.limit(0);
		darkTiles = null;
	}
	
	private void drawFloor() {
		//		  float alpha=(100-Settings::floorTranslucency())/100.0; // default is 33% transparent, so 0.67 alpha
		float alpha = 0.67f;

	    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	    GLES20.glVertexAttrib3f(getNormalHandle(), 0, 1, 0);
        GLES20.glDisableVertexAttribArray(getNormalHandle());

		boolean frameProtected = false;

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // dark
		if(frameProtected)
			GLES20.glUniform4f(mColorHandle, 0.3f, 0.0f, 0.0f, alpha);
		else
			GLES20.glUniform4f(mColorHandle, 0.1f, 0.1f, 0.1f, alpha);
        darkTiles.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
        		0, darkTiles);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 1200);

		// light
		if(frameProtected)
			GLES20.glUniform4f(mColorHandle, 0.8f, 0.0f, 0.0f, alpha);
		else
			GLES20.glUniform4f(mColorHandle, 0.6f, 0.6f, 0.6f, alpha);
        lightTiles.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
        		0, lightTiles);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 1200);
	}

	/** 
	 * Helper function to compile a shader.
	 * 
	 * @param shaderType The shader type.
	 * @param shaderSource The shader source code.
	 * @return An OpenGL handle to the shader.
	 */
	private int compileShader(final int shaderType, final String shaderSource) 
	{
		int shaderHandle = GLES20.glCreateShader(shaderType);

		if (shaderHandle != 0) 
		{
			// Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource);

			// Compile the shader.
			GLES20.glCompileShader(shaderHandle);

			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) 
			{
				Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}

		if (shaderHandle == 0)
		{			
			throw new RuntimeException("Error creating shader.");
		}
		
		return shaderHandle;
	}	
	
	/**
	 * Helper function to compile and link a program.
	 * 
	 * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
	 * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
	 * @param attributes Attributes that need to be bound to the program.
	 * @return An OpenGL handle to the program.
	 */
	private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) 
	{
		int programHandle = GLES20.glCreateProgram();
		
		if (programHandle != 0) 
		{
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);			

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);
			
			// Bind attributes
			if (attributes != null)
			{
				final int size = attributes.length;
				for (int i = 0; i < size; i++)
				{
					GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
				}						
			}
			
			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) 
			{				
				Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}
		
		if (programHandle == 0)
		{
			throw new RuntimeException("Error creating program.");
		}
		
		return programHandle;
	}
}
