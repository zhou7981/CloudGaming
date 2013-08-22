package com.zyh.util;

import java.nio.ByteBuffer;

public class RawKeyboardInput {
	final static int len = RawInputHeader.len + 24; // size of header + 18  bytes of RawKeyboardInput
	
	private RawInputHeader header;
	private short makeCode = 0;
	private short flags;
	private short reserved = 0;
	private short vKey;
	private int message = 0;
	private int extraInfo = 0;
//	private 
	
	public RawKeyboardInput() {
		this.header = new RawInputHeader(1);
	}
	
	
	public short getFlags() {
		return flags;
	}
	public void setFlags(short flags) {
		this.flags = flags;
	}
	
	public short getvKey() {
		return vKey;
	}
	public void setvKey(short vKey) {
		this.vKey = vKey;
	}
	
	
	public byte[] getKeyboardInputStream() {
		byte[] rawstream = new byte[len];
		ByteBuffer buffer = ByteBuffer.wrap(rawstream);
		buffer.put(header.serialize());
		buffer.put(Misc.ushort2bytes(makeCode));
		buffer.put(Misc.ushort2bytes(flags));
		buffer.put(Misc.ushort2bytes(reserved));
		buffer.put(Misc.ushort2bytes(vKey));
		buffer.put(Misc.int2bytes(message));
		buffer.put(Misc.int2bytes(extraInfo));
		
		
		return rawstream;
	}
	
}
