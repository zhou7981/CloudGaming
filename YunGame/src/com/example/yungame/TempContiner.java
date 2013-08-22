package com.example.yungame;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.vp8.decoder.YUVData;

class counter {
	int count = 0;
	synchronized void adder() {
		count ++;
	}
	synchronized int getNum() {
		return count;
	}
}

public class TempContiner {
	static Queue<byte[]> data = new ConcurrentLinkedQueue<byte[]>();
	static Queue<YUVData> pic = new ConcurrentLinkedQueue<YUVData>();
	final static public int MAXSIZE = 700;
	
	synchronized static boolean dataAdd(byte[] d) {
		if (data.size() > MAXSIZE) {
			return false;
		}
		boolean res = data.add(d);
		YLog.y("Container:", "ADDData" + d);
		return  res;
	}
	
	synchronized static boolean picAdd(YUVData p) {
		if (pic.size() > 50) {
			return false;
		}
		boolean res = pic.add(p);
		YLog.e("Container:", "ADDPic");
		return  res;
	}
	
	synchronized static byte[] dataPoll() {
		if (data.size() <= 0) {
			return null;
		}
		YLog.e("Container:", "PollData");
		YLog.y("Data pool", data.size() + " in the pool");
		return data.poll();
	}
	
	synchronized static YUVData picPoll() {
		if (pic.size() <= 0) {
			return null;
		}
		YLog.e("Container:", "PollPic");
		return pic.poll();
	}
}