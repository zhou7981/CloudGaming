package com.vp8.decoder;

public class YV12ToRGB32 {
	public YV12ToRGB32() {
	}
	
	public int[] convert(byte[] y, byte[] u, byte[] v, int width, int height) {
		return convertC(y, u, v, width, height);
	}
	
	native int[] convertC(byte[] y, byte[] u, byte[] v, int width, int height);
	
	static {
		System.loadLibrary("yuvconvert");
	}
}