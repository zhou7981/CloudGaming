package com.example.yungame;

public class ServerNode {
	public int port;
	public String ip;
	
	public ServerNode() {
		port = 0;
		ip = "0.0.0.0";
	}
}

class MyRect {
	int width;
	int height;
	
	public MyRect() {
		width = 0;
		height = 0;
	}
	
	public void setValue(int w, int h) {
		width = w;
		height = h;
	}
	
	public int getW() {
		return width;
	}
	
	public int getH() {
		return height;
	}
}