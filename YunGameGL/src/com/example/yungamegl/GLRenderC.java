package com.example.yungamegl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.vp8.decoder.YUVData;
import android.opengl.GLSurfaceView.Renderer;

public class GLRenderC implements Renderer{
	private final int width = 1024;
	private final int height = 1024;

	
	public GLRenderC(MainActivity test) {
		nativeInit();
	}
	
	public void setSize(MyRect rect) {
		nativeSetSize(rect.width, rect.height);
	}
	public void setBytes(YUVData data) {
		if (data.success) {
//			for (int i = 0; i < realHeight; i++) {
//				System.arraycopy(data.y, i * realWidth, yBytes, i*width, realWidth);
//			}
//			for (int i = 0; i < (realHeight / 2); i++) {
//				System.arraycopy(data.u, i * realWidth/2, uBytes, i*width/2, realWidth/2);
//				System.arraycopy(data.v, i * realWidth/2, vBytes, i*width/2, realWidth/2);
//			}
			nativeSetData(data.y, data.u, data.v);
		}
	}
	
	
	@Override
	public void onDrawFrame(GL10 arg0) {
		// TODO Auto-generated method stub
		nativeRenderer();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub
		nativeResize(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO Auto-generated method stub
		nativeCreate();
	}
	
	private static native void nativeInit();
	private static native void nativeCreate();
	private static native void nativeResize(int w, int h);
	private static native void nativeRenderer();
	private static native void nativeSetSize(int width, int height);
	private static native void nativeSetData(byte[] y, byte[] u, byte[] v);
	
	static {
		System.loadLibrary("mycrender");
	}
}