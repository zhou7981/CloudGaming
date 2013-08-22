package com.example.yungame;

import android.util.Log;


public class YLog {
	
	public static boolean isOn = false;
			
	public static void e (String tag, String msg) {
		if (isOn) {
			Log.e(tag, msg);
		}
		
	}
	
	public static void d(String tag, String msg) {
		if (isOn) Log.d(tag, msg);
	}
	
	public static void i (String tag, String msg) {
		if (isOn) {
			Log.i(tag, msg);
		}
		
	}
	
	public static void v(String tag, String msg) {
		if (isOn) Log.v(tag, msg);
	}
	
	public static void w(String tag, String msg) {
		if (isOn) Log.w(tag, msg);
	}
	
	public static void y(String tag, String msg) {
		 Log.i(tag, msg);
	}
}