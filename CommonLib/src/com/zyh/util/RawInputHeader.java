package com.zyh.util;

import java.nio.ByteBuffer;

public class RawInputHeader {
	final static int len = 16; // 32 bit OS
	
	private int dwType;
	private short pointX = 0;
	private short pointY = 0;
	
	public RawInputHeader(int dwType) {
		this.dwType = dwType;
	}
	
	public void setPoint(short pointX, short pointY) {
		this.pointX = pointX;
		this.pointY = pointY;
	} 
	
	public byte[] serialize() {
		byte[] rawstream = new byte[len];
		ByteBuffer buffer = ByteBuffer.wrap(rawstream);
	
		buffer.put(Misc.int2byte(dwType));
		buffer.put(Misc.ushort2bytes(pointX));
		buffer.put(Misc.ushort2bytes(pointY));
		buffer.put(Misc.int2byte(0));
		buffer.put(Misc.int2byte(0));
		
		return rawstream;
	}
}
