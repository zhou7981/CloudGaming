package com.zyh.util;

import java.nio.ByteBuffer;

public class RawMouseInput {
	final static int len = RawInputHeader.len + 24; // size of header + 66  bytes of RawMouseInput
	
	private RawInputHeader header;
	final short usFlags = 0;
	final short padding = 0;
	private short ulButtonFlag;
	private short ulButtonData = 0;
	private int ulRawButtons = 0;
	private int lLastX;
	private int lLastY;
	private int extraInfo = 0;
	
	public RawMouseInput() {
		this.header = new RawInputHeader(0);
	}
	
	public void setDwSize(short pointX, short pointY) {
		if (this.header != null) {
			this.header.setPoint(pointX, pointY);
		}
	}
	
	public void setUButtonFlag(short buttonFlag) {
		this.ulButtonFlag = buttonFlag;
	}
	
	public int getlLastX() {
		return lLastX;
	}
	public void setlLastX(int lLastX) {
		this.lLastX = lLastX;
	}
	public int getlLastY() {
		return lLastY;
	}
	public void setlLastY(int lLastY) {
		this.lLastY = lLastY;
	}
	
	public static int getLen() {
		return len;
	}

	
	public byte[] getMouseInputStream() {
		byte[] rawstream = new byte[len];
		ByteBuffer buffer = ByteBuffer.wrap(rawstream);
		
		buffer.put(header.serialize());
		buffer.put(Misc.ushort2bytes(usFlags));
		buffer.put(Misc.ushort2bytes(padding));
		buffer.put(Misc.ushort2bytes(ulButtonFlag));
		buffer.put(Misc.ushort2bytes(ulButtonData));
		buffer.put(Misc.int2bytes(ulRawButtons));
		buffer.put(Misc.int2bytes(lLastX));
		buffer.put(Misc.int2bytes(lLastY));
		buffer.put(Misc.int2bytes(extraInfo));
		
		return rawstream;
		
	}
	
}
