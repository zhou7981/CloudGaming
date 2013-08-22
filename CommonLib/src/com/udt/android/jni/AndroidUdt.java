package com.udt.android.jni;
public class AndroidUdt {
	public static final char AF_INET = 2;
	public static final int ERROR = -1;
	public static final int SOCK_STREAM = 1;
	public static final int SOCK_DGRAM = 2;

	// public SockAddr addr;

	public AndroidUdt() {
		// addr = new SockAddr();
	}

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
		targets[3] = (byte) (res >> 24);
		targets[4] = (byte) (res >> 32);
		targets[5] = (byte) ((res >> 40) & 0xff);// 次低位
		targets[6] = (byte) ((res >> 48) & 0xff);// 次高位
		targets[7] = (byte) (res >>> 56);// 最高位,无符号右移。
		
		return targets;
	}
	

	
	public native byte[] recv(int client, byte[] buf, int len, int flags);

	public native int socket(String ip, int port, int type);

	public native int close(int u);

	public native int connect(int u, String ip, int port);

	public native int send(int u, byte[] buf, int len, int flags);

	public native int sendmsg(int u, byte[] buf, int len, int ttl,
			boolean inorder);

	public native byte[] recvmsg(int u, byte[] buf, int len);

	static {
		System.loadLibrary("udt");
	}
}