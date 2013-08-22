package com.zyh.util;

import android.view.KeyEvent;
import android.view.MotionEvent;

public class Interaction {
	private MotionEvent mouseEvt;
	private int keyCode;
	private KeyEvent keyEvt;

	public Interaction(MotionEvent event) {
		this.mouseEvt = event;
	}
	
	public Interaction(int keyCode, KeyEvent event) {
		this.keyCode = keyCode;
		this.keyEvt = event;
	}
	
	public RawMouseInput getRawMouseInput() {
		RawMouseInput rawMouse = new RawMouseInput();
		
		return rawMouse;
	}
	
	public RawKeyboardInput getRawKeyboardInput() {
		RawKeyboardInput keyInput = new RawKeyboardInput();
		
		return keyInput;		
	}
	
}
