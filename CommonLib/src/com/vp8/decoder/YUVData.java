package com.vp8.decoder;

public class YUVData {
	public boolean success;
	public byte[] y;
	public byte[] u;
	public byte[] v;
	
	public void swapuvchannels() {
		byte[] tmp = u;
		u = v;
		v = tmp;
	}
	
}