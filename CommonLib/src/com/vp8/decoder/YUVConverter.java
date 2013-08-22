package com.vp8.decoder;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.opengl.GLES20;

public class YUVConverter{
	private static native void naGetConvertedFrame(Bitmap _bitmap, byte[] y,
			byte[] u, byte[] v, int _width, int _height);

	public void getFrame(Bitmap _bitmap, byte[] y,
			byte[] u, byte[] v, int _width, int _height) {
		naGetConvertedFrame(_bitmap, y, u, v, _width, _height);
	}
	
	static {
		System.loadLibrary("yuv2rgb");
	}
}