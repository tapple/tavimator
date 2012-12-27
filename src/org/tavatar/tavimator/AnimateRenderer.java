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

import org.tavatar.tavimator.R;
import com.learnopengles.android.common.RawResourceReader;
import com.learnopengles.android.common.ShaderHelper;
import com.learnopengles.android.common.ShapeBuilder;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class AnimateRenderer implements GLSurfaceView.Renderer 
{	
	private final Context mActivityContext;
	
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
	private final FloatBuffer mCubeColors;	
	
	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;				
	
	/** This will be used to pass in model position information. */
	private int mPositionHandle;
	
	/** This will be used to pass in model color information. */
	private int mColorHandle;		

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;	
	
	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;	
	
	/** Size of the color data in elements. */
	private final int mColorDataSize = 4;					
	
	/** This is a handle to our cube shading program. */
	private int mProgramHandle;		
	
	/**
	 * Initialize the model data.
	 */
	public AnimateRenderer(final Context activityContext)
	{	
		mActivityContext = activityContext;
		mCamera = new Camera(activityContext);
		
		// Define points for a cube.
		// X, Y, Z
		final float[] p1p = {-1.0f, 1.0f, 1.0f};					
		final float[] p2p = {1.0f, 1.0f, 1.0f};
		final float[] p3p = {-1.0f, -1.0f, 1.0f};
		final float[] p4p = {1.0f, -1.0f, 1.0f};
		final float[] p5p = {-1.0f, 1.0f, -1.0f};
		final float[] p6p = {1.0f, 1.0f, -1.0f};
		final float[] p7p = {-1.0f, -1.0f, -1.0f};
		final float[] p8p = {1.0f, -1.0f, -1.0f};		
		
		final float[] cubePositionData = ShapeBuilder.generateCubeData(p1p, p2p, p3p, p4p, p5p, p6p, p7p, p8p, p1p.length);
		
		// Points of the cube: color information
		// R, G, B, A
		final float[] p1c = {1.0f, 0.0f, 0.0f, 1.0f};		// red			
		final float[] p2c = {1.0f, 0.0f, 1.0f, 1.0f};		// magenta
		final float[] p3c = {0.0f, 0.0f, 0.0f, 1.0f};		// black
		final float[] p4c = {0.0f, 0.0f, 1.0f, 1.0f};		// blue
		final float[] p5c = {1.0f, 1.0f, 0.0f, 1.0f};		// yellow
		final float[] p6c = {1.0f, 1.0f, 1.0f, 1.0f};		// white
		final float[] p7c = {0.0f, 1.0f, 0.0f, 1.0f};		// green
		final float[] p8c = {0.0f, 1.0f, 1.0f, 1.0f};		// cyan
		
		final float[] cubeColorData = ShapeBuilder.generateCubeData(p1c, p2c, p3c, p4c, p5c, p6c, p7c, p8c, p1c.length);				
		
		// Initialize the buffers.
		mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubePositions.put(cubePositionData).position(0);		
		
		mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubeColors.put(cubeColorData).position(0);
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
	
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) 
	{				
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		
		// Cull back faces
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		
		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		
		// Disable blending
		GLES20.glDisable(GLES20.GL_BLEND);

		System.out.println("----------------------------------------------");
		mCamera.initializeCamera(
				-6.0f, 0.0f, -6.0f,
				 0.0f, 0.0f, -6.0f,
				 0.0f, 1.0f,  0.0f,
				 0.0f, 0.0f, -6.0f);
		System.arraycopy(mCamera.getViewMatrix(), 0, tempViewMatrix, 0, 16);


		final String vertexShader = getVertexShader();   		
 		final String fragmentShader = getFragmentShader();			
		
		final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
		final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		
		
		mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, 
				new String[] {"a_Position",  "a_Color"});								                                							                                            
	}	
	
	public static void printMatrix(float[] m) {
		System.out.println("" + 
			m[ 0]+" "+ m[ 4]+" "+m[ 8]+" "+m[12]+"\n"+
			m[ 1]+" "+ m[ 5]+" "+m[ 9]+" "+m[13]+"\n"+
			m[ 2]+" "+ m[ 6]+" "+m[10]+" "+m[14]+"\n"+
			m[ 3]+" "+ m[ 7]+" "+m[11]+" "+m[15]);
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
		final float far = 50.0f;
		
		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}	

	@Override
	public void onDrawFrame(GL10 glUnused) 
	{
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                
        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;        
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);                
        
        // Set our program
        GLES20.glUseProgram(mProgramHandle);
        
        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");                         
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");                                               
        
        // Draw some cubes.        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);        
        drawCube();
                        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, -4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);        
        drawCube();
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);        
        drawCube();
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -4.0f, -7.0f);
        drawCube();
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);        
        drawCube();                      
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
        
        // Pass in the color information
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
        		0, mCubeColors);        
        
        GLES20.glEnableVertexAttribArray(mColorHandle);               
        
		// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
      Matrix.multiplyMM(mMVPMatrix, 0, mCamera.getViewMatrix(), 0, mModelMatrix, 0);                           
//      Matrix.multiplyMM(mMVPMatrix, 0, tempViewMatrix, 0, mModelMatrix, 0);                           
        
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);               
        
        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);                               
	}			
}
