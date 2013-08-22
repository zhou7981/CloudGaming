package com.zyh.util;

public class YPoint {
	private int x;
	private int y;
	
	public YPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {return x;}
	public int getY() {return y;}
	
	public YPoint getOffset(YPoint lastP) {
		int offsetX = this.x - lastP.x;
		int offsetY = this.y - lastP.y;
		return new YPoint(offsetX, offsetY);
	}
	
	public int getOffsetX(int lastX) {
		return this.x - lastX;
	}
	
	public int getOffsetY(int lastY) {
		return this.y - lastY;
	}
}