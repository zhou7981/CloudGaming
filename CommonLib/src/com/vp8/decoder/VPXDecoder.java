package com.vp8.decoder;

public class VPXDecoder {
	public VPXDecoder() {
	}
	
	public native void init();
	
	public native YUVData decode(byte[] szSrc, int iLenSrc, int lenY, int lenU, int lenV);
	
	public native void destroy();
	
	static {
		System.loadLibrary("vpx");
		System.loadLibrary("myvpx");
	}
}