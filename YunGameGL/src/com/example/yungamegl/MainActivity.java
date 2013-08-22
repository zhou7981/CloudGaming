package com.example.yungamegl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.example.yungamegl.GLRenderer;
import com.example.yungamegl.ServerNode;

import com.udt.android.jni.AndroidUdt;
import com.vp8.decoder.VP8Decoder;
import com.vp8.decoder.VPXDecoder;
import com.vp8.decoder.YUVConverter;
import com.vp8.decoder.YUVData;
import com.vp8.decoder.YV12ToRGB32;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private boolean testgl = false;
	private String gameName;
	private AndroidUdt myUdt;
	private String masterIp;
	private int theClient;
	private ServerNode node;
	MyRect theRect;
	// private VP8Decoder theDecoder;
	// private YV12ToRGB32 yvconv;
	private VPXDecoder vpxDecoder;
	private AudioTrack trackPlayer;
	static final int MASTER_PORT = 2000;
	static final int SERVER_PORT = 1001;
	public static final int MSG_SUCCESS = 0;// 获取图片成功的标识
	//public static final int MSG_START = 1;
	public static final int MSG_FAILURE = 1;// 获取图片失败的标识
	private long count = 0;
	private static long decodecost = 0;
	private long convertcost = 0;
	private long transcost = 0;
	private long timecost = 0;
	private long netcost = 0;
	private long transcost1 = 0;
	private long transcost2 = 0;
	private long transcost3 = 0;
	private long recvwait = 0;
	private long decodewait = 0;
	private long decodewait2 = 0;
	private long convertwait = 0;
	private Object mPauseLock;
	private boolean mPaused;
	private boolean mFinished;
	private static Queue<byte[]> dataQueue = new LinkedList<byte[]>();

	synchronized static byte[] dataPoll() {
		if (dataQueue.size() <= 0) {
			return null;
		}
		Log.e("Container:", "PollData");
		return dataQueue.poll();
	}

	byte[] mbuf;
	// int mbuf_loc;
	int bufsize;
	static Bitmap bmpback;
	int[] RGBData;
	// YUVData yuvData;
	YUVConverter converter;
	private static long time1 = 0;
	private static long time2 = 0;
	private static long delay = 0;

	boolean recode = true;

	final int audioRatio = 8000;

	private static ImageView mRenderView;
	private MyGLSurfaceView glSurfaceView;
	private GLRenderer renderer;

	private Thread mThread;
	private Thread mThread1;
	private Thread mThread2;
	private Thread mThread3;

//	private static Handler mRecvHandler = new Handler() {
//		public void handleMessage(Message msg) {// 此方法在ui线程运行
//			switch (msg.what) {
//			case MSG_SUCCESS:
//				// TODO
//				dataQueue.add((byte[]) msg.obj);
//				Log.e("RECVH", "" + dataQueue.size());
//				break;
//			case MSG_FAILURE:
//				break;
//			}
//		}
//	};

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {// 此方法在ui线程运行
			switch (msg.what) {
			case MSG_SUCCESS:
				// TODO
				time2 = System.currentTimeMillis();
				if (time1 != 0) {
					long time = time2 - time1;
					delay += time;
				}
				time1 = time2;

				Log.e("Handler", "Show");
				YUVData mydata = (YUVData) msg.obj;
				// renderer.setBuffer(mydata.y, mydata.u, mydata.v);\

				if (mydata != null) {
					Log.e("mhandle", "send data");
					glSurfaceView.sendMSG(mydata);
				}

				break;
			case MSG_FAILURE:
				break;
			}
		}
	};

	Runnable runnable = new Runnable() {
		@Override
		public void run() {// run()在新的线程中运行
			YUVData data;
			try {
				while (!mFinished) {
					// TODO:
					Log.e("Vedio", "About Video");
					long beforeTime = System.currentTimeMillis();
					data = playGame();
					long afterTime = System.currentTimeMillis();
					long timeDistance = afterTime - beforeTime;
					timecost += timeDistance;
					count++;
					if (data != null) {
						// MainActivity.saveMyBitmap(bm, "test4");
						synchronized (mPauseLock) {
							while (mPaused) {
								mPauseLock.wait();
							}
						}
						Log.e("Message:", "Send Data");
						mHandler.obtainMessage(MSG_SUCCESS, data)
								.sendToTarget();// 获取图片成功，向ui线程发送MSG_SUCCESS标识和bitmap对象
						Log.e("Message:", "Send finish");
						if (count % 200 == 0) {
							Log.e("Time Per Frame", "Delay: " + delay / count
									+ " Decode: " + decodecost / count
									+ " Trans: " + transcost / count
									+ "  Convert: " + convertcost / count
									+ "  Net: " + netcost / count + "  Total: "
									+ timecost / count);
							Log.e("Time Per Frame", "Trans1: " + transcost1
									/ count + " Trans2: " + transcost2 / count
									+ "  Trans3: " + transcost3 / count);
							Thread.sleep(10000);
							// TODO
							android.os.Process.killProcess(android.os.Process
									.myPid());
						}
					} else {
						mHandler.obtainMessage(MSG_FAILURE).sendToTarget();// 获取图片失败
						break;
					}
					Log.e("Audio", "About Audio");
					playMusic();

					synchronized (mPauseLock) {
						while (mPaused) {
							mPauseLock.wait();
						}
					}
				}
			} catch (Exception e) {
				mHandler.obtainMessage(MSG_FAILURE).sendToTarget();// 获取图片失败
				return;
			}
		}
	};

	Runnable runRecv = new Runnable() {
		@Override
		public void run() {// run()在新的线程中运行
			try {
				while (!mFinished) {
					// TODO:
					Log.e("Video", "About Video");
					int iSize = 0, rSize = 0;
					// int rs = 0;

					byte[] size = new byte[4];
					size = myUdt.recv(theClient, size, size.length, 0);
					if (size != null) {
						long beforeTime0 = System.currentTimeMillis();
						iSize = AndroidUdt.byte2int(size);
						iSize = iSize & 0x7fffffff;
						Log.e("Size:", "" + iSize);

						byte[] videoData = new byte[iSize];
						// TODO
						while (rSize < iSize) {
							byte[] buf = new byte[iSize - rSize];
							Log.e("To Recv", "" + (iSize - rSize));
							buf = myUdt.recv(theClient, buf, buf.length, 0);
							if (buf == null) {
								Log.e("Recv Error", "Received NULL");
								break;
							}
							Log.e("Recved", "" + buf.length);
							for (int i1 = 0; i1 < buf.length; i1++) {
								if (rSize + i1 < iSize) {
									videoData[rSize + i1] = buf[i1];
								}
							}
							rSize = rSize + buf.length;
						}
						long afterTime0 = System.currentTimeMillis();
						long timeDistance0 = afterTime0 - beforeTime0;
						netcost += timeDistance0;
						if (rSize < iSize) {
							Log.e("RECV NOT ENOUGH:", "" + rSize + " " + iSize);
						} else {
							// mRecvHandler.obtainMessage(MSG_SUCCESS,
							// videoData).sendToTarget();
							while (!TempContiner.dataAdd(videoData)) {
								Thread.sleep(30);
								//Log.e("Thread", "Recv Error");
								recvwait++;
							}
						}
					}
					Log.e("Audio", "About Audio");
					playMusic();

					synchronized (mPauseLock) {
						while (mPaused) {
							mPauseLock.wait();
						}
					}
				}
			} catch (Exception e) {
				mHandler.obtainMessage(MSG_FAILURE).sendToTarget();// 获取图片失败
				return;
			}
		}
	};

	Runnable runDecode = new Runnable() {
		@Override
		public void run() {// run()在新的线程中运行
			try {
				while (!mFinished) {
					// TODO

					byte[] videoData1;
					while ((videoData1 = TempContiner.dataPoll()) == null) {
						Thread.sleep(20);
						//Log.e("Thread", "Decode Error");
						decodewait++;
					}
					Log.e("Decode", "Start");

					long beforeTime1 =  System.currentTimeMillis();
					YUVData yuvData1 = vpxDecoder.decode(videoData1,
							videoData1.length, theRect.getH() * theRect.getW(),
							theRect.getH() * theRect.getW() / 4, theRect.getH()
									* theRect.getW() / 4);
					long afterTime1 = System.currentTimeMillis();
					long timeDistance1 = afterTime1 - beforeTime1;
					decodecost += timeDistance1;
					Log.e("decodetime", "" + decodecost);
					if (yuvData1.success) {
						 while(!TempContiner.picAdd(yuvData1)) {
						 Thread.sleep(30);
						 //Log.e("Thread", "Decode Wait");
						 decodewait2++;
						 }
//						count++;
//						if (count % 1000 == 0) {
//							Log.e("Time Per Frame", "Delay: " + delay / count
//									+ " Decode: " + decodecost / count
//									+ " Trans: " + transcost / count
//									+ "  Convert: " + convertcost / count
//									+ "  Net: " + netcost / count + "  Total: "
//									+ timecost / count);
//							Log.e("Time Per Frame", "Trans1: " + transcost1
//									/ count + " Trans2: " + transcost2 / count
//									+ "  Trans3: " + transcost3 / count);
//							Thread.sleep(10000);
//							// TODO
//							android.os.Process.killProcess(android.os.Process
//									.myPid());
//						}
//						mHandler.obtainMessage(MSG_SUCCESS, yuvData1)
//								.sendToTarget();// 获取图片成功，向ui线程发送MSG_SUCCESS标识和bitmap对象
					}
					synchronized (mPauseLock) {
						while (mPaused) {
							mPauseLock.wait();
						}
					}
				}
			} catch (Exception e) {
				Log.e("Decode", "" + e);
				return;
			}
		}
	};

	Runnable runConvert = new Runnable() {
		@Override
		public void run() {// run()在新的线程中运行
			try {
				while (!mFinished) {
					// TODO

					YUVData yuvData1;
					while ((yuvData1 = TempContiner.picPoll()) == null) {
						Thread.sleep(30);
						//Log.e("Thread", "Convert Error");
						convertwait++;
					}
					Log.e("Decode", "Start");

					count++;
					if (yuvData1.success) {

						Log.e("Decoder", "start");

						mHandler.obtainMessage(MSG_SUCCESS, yuvData1).sendToTarget();
						if (count % 1000 == 0) {
							Log.e("Time Per Frame", "Delay: " + delay / count
									+ " Decode1: " + decodewait + " Decode2: "
									+ decodewait2 + "  Convert: " + convertwait
									+ "  Net: " + recvwait);
							Log.e("Time Per Frame", "Decode: " + decodecost
									/ count + " Net: " + netcost / count
									+ "  Trans3: " + transcost3 / count);
							Thread.sleep(10000);
							// TODO
							android.os.Process.killProcess(android.os.Process
									.myPid());
						}
					}
					synchronized (mPauseLock) {
						while (mPaused) {
							mPauseLock.wait();
						}
					}
				}
			} catch (Exception e) {
				Log.e("Decode", "" + e);
				return;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);
		/* force 32-bit display for better display quality */

		mPauseLock = new Object();
		mPaused = false;
		mFinished = false;
		theClient = 0;
		masterIp = "192.168.0.103";
		Intent i = getIntent();
		gameName = i.getStringExtra("gameName");
		node = new ServerNode();
		myUdt = new AndroidUdt();
		theRect = new MyRect();

		glSurfaceView = new MyGLSurfaceView(this);
		setContentView(glSurfaceView);

		if (testgl) {
		} else {
			// mImageView = (ImageView) findViewById(R.id.imageView);
			// mRenderView = (RenderView) findViewById(R.id.drawView);

			if (0 <= connectMaster(masterIp, gameName)) {
				// node.ip = "59.66.131.29";
				// node.port = 9003;
				Log.e("Server Info:", node.ip + " " + node.port);
				if (0 <= connectServer(node.ip, node.port, "yhx")) {
					theClient = connectGame(node.ip, node.port, theRect);
					// theDecoder = new VP8Decoder(theRect.getW(),
					// theRect.getH());
					vpxDecoder = new VPXDecoder();
					bufsize = AudioTrack.getMinBufferSize(audioRatio,
							AudioFormat.CHANNEL_OUT_STEREO,
							AudioFormat.ENCODING_PCM_16BIT);
					mbuf = new byte[0];
					// mbuf_loc = 0;
					trackPlayer = new AudioTrack(AudioManager.STREAM_MUSIC,
							audioRatio, AudioFormat.CHANNEL_OUT_STEREO,
							AudioFormat.ENCODING_PCM_16BIT, bufsize,
							AudioTrack.MODE_STREAM);
					converter = new YUVConverter();
					// yvconv = new YV12ToRGB32();
					if (theClient > 0) {
						vpxDecoder.init();
						trackPlayer.play();
						// mThread = new Thread(runnable);
						// mThread.start();// 线程启动

						mThread1 = new Thread(runRecv);
						mThread2 = new Thread(runDecode);
						mThread3 = new Thread(runConvert);
						mThread1.start();
						mThread2.start();
						mThread3.start();
					} else {
						AlertDialog.Builder ad = new AlertDialog.Builder(
								MainActivity.this);
						ad.setTitle("Error");
						ad.setMessage("Receive Game Failed!");
						ad.setPositiveButton("确定", null);
						ad.show();
					}
				} else {
					AlertDialog.Builder ad = new AlertDialog.Builder(
							MainActivity.this);
					ad.setTitle("Error");
					ad.setMessage("Connect to Server  Failed!");
					ad.setPositiveButton("确定", null);
					ad.show();
				}
			} else {
				AlertDialog.Builder ad = new AlertDialog.Builder(
						MainActivity.this);
				ad.setTitle("Error");
				ad.setMessage("Connect to Master Failed!");
				ad.setPositiveButton("确定", null);
				ad.show();
			}
		}
	}

	// @SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		synchronized (mPauseLock) {
			mPaused = true;
		}
		Log.e("Thread", "Pause");
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.e("Thread", "Resume");
		super.onResume();
		synchronized (mPauseLock) {
			mPaused = false;
			mPauseLock.notifyAll();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public YUVData playGame() {
		int iSize = 0, rSize = 0;
		// int rs = 0;

		byte[] size = new byte[4];
		size = myUdt.recv(theClient, size, size.length, 0);
		if (size == null) {
			return null;
		} else {
			iSize = AndroidUdt.byte2int(size);
			iSize = iSize & 0x7fffffff;
			Log.e("Size:", "" + iSize);

			byte[] videoData = new byte[iSize];
			// TODO
			long beforeTime0 = System.currentTimeMillis();
			while (rSize < iSize) {
				byte[] buf = new byte[iSize - rSize];
				Log.e("To Recv", "" + (iSize - rSize));
				buf = myUdt.recv(theClient, buf, buf.length, 0);
				if (buf == null) {
					Log.e("Recv Error", "Received NULL");
					break;
				}
				Log.e("Recved", "" + buf.length);
				for (int i1 = 0; i1 < buf.length; i1++) {
					if (rSize + i1 < iSize) {
						videoData[rSize + i1] = buf[i1];
					}
				}
				rSize = rSize + buf.length;
			}
			long afterTime0 = System.currentTimeMillis();
			long timeDistance0 = afterTime0 - beforeTime0;
			netcost += timeDistance0;

			if (rSize < iSize) {
				Log.e("RECV NOT ENOUGH:", "" + rSize + " " + iSize);
				return null;
			} else {
				// Decode videoData
				Log.e("RECV ENOUGH:", "" + rSize + " " + iSize);
				// if (recode) {
				// writeFileToSD(videoData, "file.txt");
				// // recode = false;
				// }

				// libvpx
				try {
					// TODO
					long beforeTime1 = System.currentTimeMillis();
					YUVData yuvData = vpxDecoder.decode(videoData, rSize,
							theRect.getH() * theRect.getW(), theRect.getH()
									* theRect.getW() / 4, theRect.getH()
									* theRect.getW() / 4);
					long afterTime1 = System.currentTimeMillis();
					long timeDistance1 = afterTime1 - beforeTime1;
					decodecost += timeDistance1;
					if (yuvData.success) {

						Log.e("Decoder", "Success");
						return yuvData;
					}
				} catch (Exception e) {
					Log.e("yuv", "Failed: " + e);
				}
				Log.e("Decoder", "Failed!");
				// MediaCodec

				// Log.e("Start Decode", "Here");
				// byte[] yuvData = theDecoder.startDecode(videoData);
				// Log.e("Start YUV", "Here");
				// if (yuvData == null) {
				// Log.e("ERROR","YUV is null");
				// } else {
				// Log.e("YUV size:", "" + yuvData.length);
				// }
				//
				// // YuvImage yuvimage=new YuvImage(yuvData, ImageFormat.NV21,
				// theRect.getW(), theRect.getH(), null);
				// // Log.e("YUV finish", "Here");
				// // ByteArrayOutputStream baos = new ByteArrayOutputStream();
				// // Log.e("Start compress", "Here");
				// // yuvimage.compressToJpeg(new Rect(0, 0, theRect.getW(),
				// theRect.getH()), 80, baos);
				// // byte[] jdata = baos.toByteArray();
				// // Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0,
				// jdata.length);
				//
				// int[] jdata = decodeYUV420SP(yuvData, theRect.getW(),
				// theRect.getH());
				// Log.e("Convert Bitmap", "Here");

			}

		}
		return null;
	}

	public void playMusic() {
		int iAudioSize = 0;
		int rAudioSize = 0;
		byte[] size = new byte[4];

		size = myUdt.recv(theClient, size, 4, 0);
		if (size == null) {
			return;
		}
		iAudioSize = AndroidUdt.byte2int(size);
		Log.e("TOTAL AUDIO", "" + iAudioSize);
		byte[] audioData = new byte[iAudioSize];
		while (rAudioSize < iAudioSize) {
			byte[] buf = new byte[iAudioSize - rAudioSize];
			Log.e("To Recv", "" + (iAudioSize - rAudioSize));
			buf = myUdt.recv(theClient, buf, buf.length, 0);
			if (buf == null) {
				Log.e("Recv Error", "Received NULL");
				break;
			}
			Log.e("Recved", "" + buf.length);
			for (int i1 = 0; i1 < buf.length; i1++) {
				if (rAudioSize + i1 < iAudioSize) {
					audioData[rAudioSize + i1] = buf[i1];
				}
			}
			rAudioSize = rAudioSize + buf.length;
		}

		if (rAudioSize < iAudioSize) {
			Log.e("RECV NOT ENOUGH:", "" + rAudioSize + " " + iAudioSize);
			return;
		} else {
			// playAudio
			try {
				// byte[] buf_t = new byte[mbuf.length + rAudioSize];
				// if (mbuf.length != 0) {
				// System.arraycopy(mbuf, 0, buf_t, 0, mbuf.length);
				// }
				// System.arraycopy(audioData, 0, buf_t, mbuf.length,
				// rAudioSize);
				// mbuf = buf_t;
				//
				// if (mbuf.length > bufsize) {
				// trackPlayer.write(mbuf, 0, mbuf.length);
				// mbuf = new byte[0];
				// }
				if (recode) {
					writeFileToSD(audioData, "audio.txt");
					recode = false;
				}
				// TODO
				// android.os.Process.killProcess(android.os.Process
				// .myPid());
				trackPlayer.write(audioData, 0, audioData.length);
			} catch (Exception e) {
				Log.e("Music:", "Error:" + e);
			}
			return;
		}
	}

	public int connectMaster(String ip, String name) {
		int client = myUdt.socket(ip, MainActivity.MASTER_PORT,
				AndroidUdt.SOCK_DGRAM);

		if (AndroidUdt.ERROR == client) {
			Log.e("UDT ERROR", "socket error client=" + client);
			return -1;
		}

		int res = -1;
		for (int i = 0; i < 4; i++) {
			if (AndroidUdt.ERROR == myUdt.connect(client, ip,
					MainActivity.MASTER_PORT)) {
				Log.e("Debug:", "here");
			} else {
				res = 0;
				break;
			}
		}

		if (res == AndroidUdt.ERROR) {
			Log.e("UDT ERROR", "connect error");
			return -1;
		}

		byte[] tmp = name.getBytes();
		byte[] tmpSend = new byte[tmp.length + 1];
		System.arraycopy(tmp, 0, tmpSend, 0, tmp.length);
		tmpSend[tmp.length] = 0;
		Log.e("Send", new String(tmpSend));
		res = myUdt.sendmsg(client, tmpSend, tmpSend.length, -1, true);

		byte[] buf = new byte[68];
		buf = myUdt.recvmsg(client, buf, 68);
		if (buf == null) {
			myUdt.close(client);
			return -1;
		}
		node.port = AndroidUdt.byte2int(buf);
		int k = 0;
		for (k = 4; k < buf.length; k++) {
			if (buf[k] == 0) {
				break;
			}
		}
		Log.e("K", "" + k);
		node.ip = new String(buf, 4, k - 4);

		myUdt.close(client);

		if (node.port == 0) {
			AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
			ad.setTitle("Error");
			ad.setMessage("No such game or no free slot!");
			ad.setPositiveButton("确定", null);
			ad.show();
			return -1;
		}

		return res;
	}

	public int connectServer(String ip, int port, String user) {
		int client = myUdt.socket(ip, MainActivity.SERVER_PORT,
				AndroidUdt.SOCK_STREAM);

		if (AndroidUdt.ERROR == client) {
			Log.e("UDT ERROR", "socket error client=" + client);
			return -1;
		}

		Log.e("ConnectServer", ip + " " + port);
		int res = -1;
		for (int i = 0; i < 4; i++) {
			if (AndroidUdt.ERROR == myUdt.connect(client, ip,
					MainActivity.SERVER_PORT)) {
				Log.e("Debug:", "here");
			} else {
				res = 0;
				break;
			}
		}

		if (res == AndroidUdt.ERROR) {
			Log.e("UDT ERROR", "connect error");
			return -1;
		}

		byte buf[] = AndroidUdt.int2byte(port);
		res = myUdt.send(client, buf, buf.length, 0);
		res = myUdt.send(client, user.getBytes(), user.getBytes().length, 0);

		return res;
	}

	public int connectGame(String szIP, int iPort, MyRect rect) {
		int client = myUdt.socket(szIP, iPort, AndroidUdt.SOCK_STREAM);

		if (AndroidUdt.ERROR == client) {
			Log.e("UDT ERROR", "socket error client=" + client);
			return -1;
		}

		int res = -1;
		for (int i = 0; i < 4; i++) {
			if (AndroidUdt.ERROR == myUdt.connect(client, szIP, iPort)) {
				Log.e("UDT ERROR", "connect error");
			} else {
				res = 0;
				break;
			}
		}

		if (res == AndroidUdt.ERROR) {
			AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
			ad.setTitle("Error");
			ad.setMessage("Connect to Game Failed!");
			ad.setPositiveButton("确定", null);
			ad.show();
			return -1;
		}

		byte[] buf = new byte[4];
		buf = myUdt.recv(client, buf, buf.length, 0);

		if (buf == null) {
			return -1;
		}

		int iResolution = AndroidUdt.byte2int(buf);

		rect.setValue(iResolution & 0xffff, (iResolution & 0xffff0000) >> 16);
		// renderer.setRect(iResolution & 0xffff, (iResolution & 0xffff0000) >>
		// 16);
		glSurfaceView.sendSize(rect);
		
		return client;
	}

	public void writeFileSdcard(String fileName, String message) {
		try {
			FileOutputStream fout = new FileOutputStream(fileName);
			byte[] bytes = message.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String readFileSdcard(String fileName) {
		String res = "";
		try {
			FileInputStream fin = new FileInputStream(fileName);
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			res = new String(buffer);
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	@SuppressLint("SdCardPath")
	private void writeFileToSD(byte[] buf, String name) {
		String sdStatus = Environment.getExternalStorageState();
		if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
			Log.d("TestFile", "SD card is not avaiable/writeable right now.");
			return;
		}
		try {
			String pathName = "/sdcard/Download/test/";
			String fileName = name;
			File path = new File(pathName);
			File file = new File(pathName + fileName);
			if (!path.exists()) {
				Log.d("TestFile", "Create the path:" + pathName);
				path.mkdir();
			}
			if (!file.exists()) {
				Log.d("TestFile", "Create the file:" + fileName);
				file.createNewFile();
			}
			FileOutputStream stream = new FileOutputStream(file);
			stream.write(buf);
			stream.close();

		} catch (Exception e) {
			Log.e("TestFile", "Error on writeFilToSD.");
			e.printStackTrace();
		}
	}

	@SuppressLint("SdCardPath")
	public static void saveMyBitmap(Bitmap mBitmap, String bitName)
			throws IOException {
		File f = new File("/sdcard/Download/test/" + bitName + ".png");
		f.createNewFile();
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			Log.e("Pic: ", "" + e);
			e.printStackTrace();
		}
		mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
		Log.e("Pic: ", "Saved!");
		try {
			fOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
