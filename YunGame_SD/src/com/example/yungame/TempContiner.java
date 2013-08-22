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
	
	synchronized static boolean dataAdd(byte[] d) {
		if (data.size() > 70) {
			return false;
		}
		boolean res = data.add(d);
		Log.e("Container:", "ADDData");
		return  res;
	}
	
	synchronized static boolean picAdd(YUVData p) {
		if (pic.size() > 50) {
			return false;
		}
		boolean res = pic.add(p);
		Log.e("Container:", "ADDPic");
		return  res;
	}
	
	synchronized static byte[] dataPoll() {
		if (data.size() <= 0) {
			return null;
		}
		Log.e("Container:", "PollData");
		return data.poll();
	}
	
	synchronized static YUVData picPoll() {
		if (pic.size() <= 0) {
			return null;
		}
		Log.e("Container:", "PollPic");
		return pic.poll();
	}
}