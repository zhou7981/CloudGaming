package com.example.yungamegl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.vp8.decoder.YUVData;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GLRendererJava implements Renderer {
	private static final int LENGTH = 115200;
	private static final int LENGTH_4 = 19200;

	private MainActivity activity;

	private FloatBuffer mVertices;
	private ShortBuffer mIndices;

	private int previewFrameWidth = 1024;
	private int previewFrameHeight = 1024;
	private int mProgramObject;
	private int mPositionLoc;
	private int mTexCoordLoc;
//	private int mSamplerLoc;
	private int yTexture;
	private int uTexture;
	private int vTexture;
	private final int width = 1024;
	private final int height = 1024;
	private int realWidth = 1024;
	private int realHeight = 768;

	private final float[] mVerticesData = { -1.f, 1.f, 0.0f, // Position 0
			0.0f, 0.0f, // TexCoord 0
			-1.f, -1.f, 0.0f, // Position 1
			0.0f, 1.0f, // TexCoord 1
			1.f, -1.f, 0.0f, // Position 2
			1.0f, 1.0f, // TexCoord 2
			1.f, 1.f, 0.0f, // Position 3
			1.0f, 0.0f // TexCoord 3
	};

	private final short[] mIndicesData = { 0, 1, 2, 0, 2, 3 };

	private byte[] yBytes;
	private byte[] uBytes;
	private byte[] vBytes;
//	private ByteBuffer yBuffer;
//	private ByteBuffer uBuffer;
//	private ByteBuffer vBuffer;

	byte[] uData = new byte[LENGTH_4 * 2];
	byte[] vData = new byte[LENGTH_4];

	int[] textureNames;
	
	private ByteBuffer createByteBuffer(byte[] a) {
		ByteBuffer res = ByteBuffer.allocateDirect(a.length * 4);
		res.order(ByteOrder.nativeOrder());
		res.put(a);
		res.position(0);
		return res;
	}
	
	public void setSize(MyRect rect) {
		realWidth = rect.width;
		realHeight = rect.height;
		Log.e("GLSize:", "" + realWidth + " " + realHeight);
		
	}
	public void setBytes(YUVData data) {
		if (data.success) {
			for (int i = 0; i < realHeight; i++) {
				System.arraycopy(data.y, i * realWidth, yBytes, i*width, realWidth);
			}
			for (int i = 0; i < (realHeight / 2); i++) {
				System.arraycopy(data.u, i * realWidth/2, uBytes, i*width/2, realWidth/2);
				System.arraycopy(data.v, i * realWidth/2, vBytes, i*width/2, realWidth/2);
			}
		}
	}
	
	public GLRendererJava(MainActivity activity) {
		this.activity = activity;

		mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mVertices.put(mVerticesData).position(0);

		mIndices = ByteBuffer.allocateDirect(mIndicesData.length * 2)
				.order(ByteOrder.nativeOrder()).asShortBuffer();
		mIndices.put(mIndicesData).position(0);
		
		yBytes = new byte[width * height];
		uBytes = new byte[width * height/4];
		vBytes = new byte[width * height/4];
		
		for (int i=0; i<width * height; i++) {
			yBytes[i] = (byte)0;
		}
		for (int i=0; i<width * height/4; i++) {
			uBytes[i] = (byte)128;
		}
		
		for (int i=0; i<width * height/4; i++) {
			vBytes[i] = (byte)128;
		}
//		yBuffer = createByteBuffer(yBytes);
//		uBuffer = createByteBuffer(uBytes);
//		vBuffer = createByteBuffer(vBytes); 
	}

	@Override
	public final void onDrawFrame(GL10 gl) {
		// Clear the color buffer
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		// Use the program object
		GLES20.glUseProgram(mProgramObject);
		
		// Load the vertex position

		GLES20.glUniform1i(yTexture, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
		GLES20.glTexImage2D(   GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
				width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, createByteBuffer(yBytes));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLES20.glUniform1i(uTexture, 1);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[1]);
		GLES20.glTexImage2D(   GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
				width/2, height/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, createByteBuffer(uBytes));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLES20.glUniform1i(vTexture, 2);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[2]);
		GLES20.glTexImage2D(   GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
				width/2, height/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, createByteBuffer(vBytes));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices);
		GLES20.glUseProgram(0);
		Log.e("OpenGLES", "Draw Frame Finish");
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		Log.e("OpenGLES", "Change Surface");
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		// Define a simple shader program for our point.
		final String vShaderStr = readTextFileFromRawResource(activity, R.raw.v_simple);
		final String fShaderStr = readTextFileFromRawResource(activity, R.raw.f_convert_allmat);

		// Load the shaders and get a linked program object
		mProgramObject = loadProgram(vShaderStr, fShaderStr);

		//GLES20.glUseProgram(mProgramObject);
		
		// Get the attribute locations
		mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "position");
		mTexCoordLoc = GLES20.glGetAttribLocation(mProgramObject, "inputTextureCoordinate");
		
		mVertices.position(0);
		GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
		// Load the texture coordinate
		mVertices.position(3);
		GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices);

		GLES20.glEnableVertexAttribArray(mPositionLoc);
		GLES20.glEnableVertexAttribArray(mTexCoordLoc);

		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		textureNames = new int[3];
		GLES20.glGenTextures(3, textureNames, 0);
		
		yTexture = GLES20.glGetUniformLocation(mProgramObject, "yframe");
		int yTextureName = textureNames[0];
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureName);
		GLES20.glTexImage2D(   GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
				width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, createByteBuffer(yBytes));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		//GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		uTexture = GLES20.glGetUniformLocation(mProgramObject, "uframe");
		int uTextureName = textureNames[1];
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureName);
		GLES20.glTexImage2D(   GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
				width/2, height/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, createByteBuffer(uBytes));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		//GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		vTexture = GLES20.glGetUniformLocation(mProgramObject, "vframe");
		int vTextureName = textureNames[2];
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureName);
		GLES20.glTexImage2D(   GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
				width/2, height/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, createByteBuffer(vBytes));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		Log.e("OpenGLES", "Create Surface");

	}

	public void setPreviewFrameSize(int realWidth, int realHeight) {
		previewFrameHeight = realHeight;
		previewFrameWidth = realWidth;

//		frameData = GraphicsUtil.makeByteBuffer(previewFrameHeight * previewFrameWidth * 3);
	}

	public static String readTextFileFromRawResource(final Context context, final int resourceId) {
		final InputStream inputStream = context.getResources().openRawResource(resourceId);
		final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		String nextLine;
		final StringBuilder body = new StringBuilder();

		try {
			while ((nextLine = bufferedReader.readLine()) != null) {
				body.append(nextLine);
				body.append('\n');
			}
		} catch (IOException e) {
			return null;
		}

		return body.toString();
	}

	public static int loadShader(int type, String shaderSrc) {
		int shader;
		int[] compiled = new int[1];

		// Create the shader object
		shader = GLES20.glCreateShader(type);
		if (shader == 0) {
			return 0;
		}
		// Load the shader source
		GLES20.glShaderSource(shader, shaderSrc);
		// Compile the shader
		GLES20.glCompileShader(shader);
		// Check the compile status
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

		if (compiled[0] == 0) {
			Log.e("ESShader", GLES20.glGetShaderInfoLog(shader));
			GLES20.glDeleteShader(shader);
			return 0;
		}
		return shader;
	}

	public static int loadProgram(String vertShaderSrc, String fragShaderSrc) {
		int vertexShader;
		int fragmentShader;
		int programObject;
		int[] linked = new int[1];

		// Load the vertex/fragment shaders
		vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
		if (vertexShader == 0) {
			return 0;
		}

		fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
		if (fragmentShader == 0) {
			GLES20.glDeleteShader(vertexShader);
			return 0;
		}

		// Create the program object
		programObject = GLES20.glCreateProgram();

		if (programObject == 0) {
			return 0;
		}

		GLES20.glAttachShader(programObject, vertexShader);
		GLES20.glAttachShader(programObject, fragmentShader);

		// Link the program
		GLES20.glLinkProgram(programObject);

		// Check the link status
		GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

		if (linked[0] == 0) {
			Log.e("ESShader", "Error linking program:");
			Log.e("ESShader", GLES20.glGetProgramInfoLog(programObject));
			GLES20.glDeleteProgram(programObject);
			return 0;
		}

		// Free up no longer needed shader resources
		GLES20.glDeleteShader(vertexShader);
		GLES20.glDeleteShader(fragmentShader);

		return programObject;
	}
}