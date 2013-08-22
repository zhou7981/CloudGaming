package com.zyh.util;

import java.util.HashMap;
import java.util.Iterator;

import android.view.KeyEvent;

public class Misc {
	public static int byte2int(byte[] res) {
		// 一个byte数据左移24位变成0x??000000，再右移8位变成0x00??0000

		int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) // | 表示安位或
				| ((res[2] << 24) >>> 8) | (res[3] << 24);
		return targets;
	}

	public static byte[] int2byte(int res) {
		byte[] targets = new byte[4];

		targets[0] = (byte) (res & 0xff);// 最低位
		targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
		targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
		targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
		return targets;
	}

	public static byte[] ushort2bytes(int res) {
		byte[] targets = new byte[2];

		targets[0] = (byte) (res & 0xff);// 最低位
		targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
		
		return targets;
	}
	
	
	public static byte[] int2bytes(int res) {
		byte[] targets = new byte[4];

		targets[0] = (byte) (res & 0xff);// 最低位
		targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
		targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
		targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
		
		return targets;
	}
	
	public static byte[] long2bytes(long res) {
		byte[] targets = new byte[8];

		targets[0] = (byte) (res & 0xff);// 最低位
		targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
		targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
		targets[3] = (byte) ((res >> 24) & 0xff);// 次高位
		targets[4] = (byte)((res >> 32) & 0xff);// 次高位
		targets[5] = (byte) ((res >> 40) & 0xff);// 次低位
		targets[6] = (byte) ((res >> 48) & 0xff);// 次高位
		targets[7] = (byte) (res >>> 56);// 最高位,无符号右移。
		
		return targets;
	}
	
	static HashMap keyMap;	
	public static void buildKeyMap() {
		keyMap = new HashMap();
		//0-9, i.e keyCode 7~16 => 0x30 ~ 0x39
		for (short i = 0; i <10; i++) {
			keyMap.put(7 + i, 0x30 + i);
		}
		//A~Z, i.e. keyCode 29~54 => 0x41 ~ 0x5A
		for (short i = 0; i < 26; i++) {
			keyMap.put(29 + i, 0x41 + i);
		}
		//backspace 67 => 0x08
		keyMap.put(67, 0x08);
		//tab 61 => 0x09
		keyMap.put(61, 0x09);
		//enter 66 => 0x0D
		keyMap.put(66,  0x0D);
		//shift left,right: 59,60 =>0xA0, 0xA1
		keyMap.put(59, 0xA0);
		keyMap.put(60, 0xA1);
		//ctrl left, right: 113, 114 => 0xA2, 0xA3
		keyMap.put(113, 0xA2);
		keyMap.put(114, 0xA3);
		// cap lock: 115=>0x14
		keyMap.put(115, 0x14);
		//Alt left, right: 57, 58 =>0xA4, 0xA5
		keyMap.put(57, 0x12);
		keyMap.put(58, 0x12);
		//Esc  111 => 0x1B
		keyMap.put(111, 0x1B);
		//space 62 => 0x20
		keyMap.put(62, 0x20);
		//page up/down:  92, 93 => 0x21, 0x22
		keyMap.put(92, 0x21);
		keyMap.put(93, 0x22);
		// end, home: 123, 122 =>0x23, 24
		keyMap.put(122, 0x24);
		keyMap.put(123, 0x23);
		//arrow left, up, right, down: 0x25, 0x26, 0x27, 0x28
		// to do...
		keyMap.put(21, 0x25);
		keyMap.put(19, 0x26);
		keyMap.put(22, 0x27);
		keyMap.put(20, 0x28);
		
		
		//F1-F12:131~142 => 0x70~0x7B
		for (short i = 0; i < 12; i++) {
			keyMap.put(131 + i, 0x70 + i);
		}
		
		//print screen: 120 =>0x2C
		keyMap.put(120, 0x2C);		
		//ins: 124 => 0x2D
		keyMap.put(124, 0x2D);		
		//delete: 112 => 0x2E
		keyMap.put(112, 0x2E);
		//app switch: 187 =>0x5D
		keyMap.put(187, 0x5D);
		// to do: windows key, left, right
		keyMap.put(117, 0x5B);
		keyMap.put(118, 0x5C);
		
		// to do: Separator key
		
		//`~' key		
		keyMap.put(68, 0xC0);
		
		
		//numeric pad: 0 - 9: 144~153 =>0x60 ~0x69
		for(short i = 0; i < 10; i++) {
			keyMap.put(144 + i, 0x60 + i);
		}
		//multiply 155=> 0x6A
		keyMap.put(155, 0x6A);
		// add key: 157	 =>0x6B
		keyMap.put(157, 0x6B);
		//subtract: 156 => 0x6D
		keyMap.put(156, 0x6D);
		//decimal: 158 => 0x6E
		keyMap.put(158, 0x6E);
		//divide: 154 => 0x6F
		keyMap.put(154, 0x6F);
		// num lock: 143 => 0x90
		keyMap.put(143, 0x90);
		//scroll lock: 116 => 0x91
		keyMap.put(116, 0x91);
		
		//semicomma 74 =>0xBA
		keyMap.put(74, 0xBA);
		//plus: 81 => 0xBB
		keyMap.put(81, 0xBB);
		keyMap.put(70, 0xBB);
		//comma: 55 => 0xBC
		keyMap.put(55, 0xBC);
		//minus: 69 => 0xBD
		keyMap.put(69, 0xBD);
		//period: 56 =>  0xBE
		keyMap.put(56, 0xBE);
		//slash: 76 => 0xBF
		keyMap.put(76, 0xBF);
		//~: => 0xC0
		//[{: 71=> 0xDB
		keyMap.put(71, 0xDB);
		//\|: 73 => 0xDC
		keyMap.put(73, 0xDC);
		//]}: 72 => 0xDD
		keyMap.put(72, 0xDD);
		//': 75=> 0xDE
		keyMap.put(75, 0xDE);
		
//		Iterator it = keyMap.entrySet().iterator();
//		while(it.hasNext()) {
//			System.out.println(it.next());
//		}
	}
	
	public static short getVirtualKeyCode(int keyCode, KeyEvent event) {
		short vKeyCode = 0;
		if (keyMap.containsKey(keyCode)) {
			vKeyCode = ((Integer)keyMap.get(keyCode)).shortValue();
		} else {
			int scanCode = event.getScanCode();
			switch (scanCode) {
			case 105://arrow left
				vKeyCode = 0x25;
				break;
			case 103:// up arrow
				vKeyCode = 0x26;
				break;
			case 106: //right arrow
				vKeyCode = 0x27;
				break;
			case 108: // down arrow
				vKeyCode = 0x28;
				break;
			case 125:// windows key
				vKeyCode = 0x5B;
				break;
			case 41:// ~key
				vKeyCode = 0xC0;
				break;
			default:
				break;
			}
			
		}
		
		return vKeyCode;
	}

}
