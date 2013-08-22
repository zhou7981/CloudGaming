package com.example.yungame;

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

import org.apache.http.util.EncodingUtils;
import com.example.yungame.ServerNode;
import com.udt.android.jni.AndroidUdt;
import com.vp8.decoder.VP8Decoder;
import com.vp8.decoder.VPXDecoder;
import com.vp8.decoder.YUVConverter;
import com.vp8.decoder.YUVData;
import com.vp8.decoder.YV12ToRGB32;
import com.zyh.util.RawKeyboardInput;
import com.zyh.util.RawMouseInput;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private String gameName;
	private AndroidUdt myUdt;
	private String masterIp;
	private int theClient;
	private ServerNode node;
	MyRect theRect;
	//private VP8Decoder theDecoder;
	//private YV12ToRGB32 yvconv;
	private VPXDecoder vpxDecoder;
	private AudioTrack trackPlayer;
	static final int MASTER_PORT = 2000;
	static final int SERVER_PORT = 1001;
	private static final int MSG_SUCCESS = 0;// 获取图片成功的标识
	private static final int MSG_FAILURE = 1;// 获取图片失败的标识
	private long count = 0;
	private long decodecost = 0;
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
	static int lButtonState = 0;
	
	synchronized static byte[] dataPoll() {
		if (dataQueue.size() <= 0) {
			return null;
		}
		Log.e("Container:", "PollData");
		return dataQueue.poll();
	}
	byte[] mbuf;
	//int mbuf_loc;
	int bufsize;
	static Bitmap bmpback;
	int[] RGBData;
	//YUVData yuvData;
	YUVConverter converter;
	private static long time1 = 0;
	private static long time2 = 0;
	private static long delay = 0;
	private static long starttime = 0;
	private static long endtime = 0;
	private static int videoLoc = 0;
	private static long forwardcount = 0;
	private static long backcount = 0;
	private counter countVideo = new counter();
	
	boolean recode = true;

	final int audioRatio = 8000;

	//private static ImageView mImageView;
	private static ImageView mRenderView;
	private Thread mThread;
	private Thread mThread1;
	private Thread mThread2;
	private Thread mThread3;
	
	private static Handler mRecvHandler = new Handler() {
		public void handleMessage(Message msg) {// 此方法在ui线程运行
			switch (msg.what) {
			case MSG_SUCCESS:
				//TODO
				dataQueue.add((byte[]) msg.obj);
				Log.e("RECVH", "" + dataQueue.size());
				break;
			case MSG_FAILURE:
				break;
			}
		}
	};
	
	private static Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {// 此方法在ui线程运行
			switch (msg.what) {
			case MSG_SUCCESS:
				//TODO
				time2 = System.currentTimeMillis();
				if (time1 != 0) {
					long time = time2 - time1;
					delay += time;
				}
				time1 = time2;
				//mImageView.setImageBitmap((Bitmap) msg.obj);
				// imageview显示从网络获取到的logo
//				try {
//					MainActivity.saveMyBitmap((Bitmap)msg.obj, "test5");
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				Log.e("Handler", "Show");
				mRenderView.setImageBitmap((Bitmap) msg.obj);
				if (bmpback != null) {
					bmpback.recycle();
					bmpback = null;
				}
				bmpback = (Bitmap)msg.obj;
				break;
			case MSG_FAILURE:
				break;
			}
		}
	};

	Runnable runnable = new Runnable() {
		@Override
		public void run() {// run()在新的线程中运行
			Bitmap bm;
			try {
				while (!mFinished) {
					//TODO:
					Log.e("Vedio", "About Video");
					long beforeTime = System.currentTimeMillis();
					bm = playGame();
					long afterTime = System.currentTimeMillis();
					long timeDistance = afterTime - beforeTime;
					timecost += timeDistance;
					count++;
					if (bm != null) {
						//MainActivity.saveMyBitmap(bm, "test4");
						synchronized (mPauseLock) {
							while (mPaused) {
								mPauseLock.wait();
							}
						}
						mHandler.obtainMessage(MSG_SUCCESS, bm).sendToTarget();// 获取图片成功，向ui线程发送MSG_SUCCESS标识和bitmap对象
						if (count % 2000 == 0) {
							Log.e("Time Per Frame", "Delay: " + delay/count +" Decode: " + decodecost/count + " Trans: " + transcost/count + "  Convert: " + convertcost/count + "  Net: " + netcost/count + "  Total: " + timecost/count);
							Log.e("Time Per Frame", "Trans1: " + transcost1/count + " Trans2: " + transcost2/count + "  Trans3: " + transcost3/count);
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

			// mImageView.setImageBitmap(bm); //出错！不能在非ui线程操作ui元素
			// mImageView.post(new Runnable() {// 另外一种更简洁的发送消息给ui线程的方法。
			// @Override
			// public void run() {// run()方法会在ui线程执行
			// // mImageView.setImageBitmap(bm);
			// }
			// });
		}
	};

	Runnable runRecv = new Runnable() {
		@Override
		public void run() {// run()在新的线程中运行
			try {
				while (!mFinished) {
					//TODO:
					Log.e("Video", "About Video");
					int iSize = 0, rSize = 0;
					// int rs = 0;

					long beforeTime = System.currentTimeMillis();
					byte[] size = new byte[4];
					size = myUdt.recv(theClient, size, size.length, 0);
					if (size != null) {
						iSize = AndroidUdt.byte2int(size);
						iSize = iSize & 0x3fffffff;
						Log.e("Size:", "" + iSize);

						byte[] videoData = new byte[iSize];
						//TODO
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
						if (rSize < iSize) {
							Log.e("RECV NOT ENOUGH:", "" + rSize + " " + iSize);
						} else {
							//mRecvHandler.obtainMessage(MSG_SUCCESS, videoData).sendToTarget();
							while(!TempContiner.dataAdd(videoData)) {
								Thread.sleep(30);
								Log.e("Thread", "Recv Error");
								recvwait++;
							}
						}
					}
					Log.e("Audio", "About Audio");
//					while (MainActivity.videoLoc - countVideo.getNum() > 15) {
//						Thread.sleep(20);
//					}
					playMusic();
					long afterTime = System.currentTimeMillis();
					long timeDistance = afterTime - beforeTime;
					netcost += timeDistance;
					
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
					//TODO
					
					byte[] videoData1;
					while ((videoData1 = TempContiner.dataPoll()) == null) {
						Thread.sleep(20);
						Log.e("Thread", "Decode Error");
						decodewait++;
					}
					Log.e("Decode", "Start");
					
					long beforeTime = System.currentTimeMillis();
					YUVData yuvData1 = vpxDecoder.decode(videoData1, videoData1.length,
							theRect.getH() * theRect.getW(), theRect.getH()
									* theRect.getW() / 4, theRect.getH()
									* theRect.getW() / 4);
					long afterTime = System.currentTimeMillis();
					long timeDistance = afterTime - beforeTime;
					decodecost += timeDistance;
					if (yuvData1.success) {
						while(!TempContiner.picAdd(yuvData1)) {
							Thread.sleep(30);
							Log.e("Thread", "Decode Wait");
							decodewait2++;
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
	
	Runnable runConvert = new Runnable() {
		@Override
		public void run() {// run()在新的线程中运行
			try {
				while (!mFinished) {
					//TODO
					
					YUVData yuvData1;
					while ((yuvData1 = TempContiner.picPoll()) == null) {
						Thread.sleep(30);
						Log.e("Thread", "Convert Error");
						convertwait++;
					}
					countVideo.adder();
					int curLoc = countVideo.getNum();
					while (MainActivity.videoLoc - curLoc > 3) {
						forwardcount++;
						while ((yuvData1 = TempContiner.picPoll()) == null) {
							Thread.sleep(30);
							Log.e("Thread", "Convert Error");
							convertwait++;
						}
						countVideo.adder();
						curLoc = countVideo.getNum();
					}
					
					while (curLoc - MainActivity.videoLoc > 3) {
						backcount++;
						Thread.sleep(10);
					}
					Log.e("Decode", "Start");
					count ++;
					if (yuvData1.success) {

						Log.e("Decoder", "start");
						long beforeTime = System.currentTimeMillis();
					
						Bitmap bmp2 = Bitmap.createBitmap(theRect.getW(), theRect.getH(), Bitmap.Config.ARGB_8888);
						converter.getFrame(bmp2, yuvData1.y, yuvData1.u, yuvData1.v, theRect.getW(), theRect.getH());
						
						long afterTime = System.currentTimeMillis();
						long timeDistance = afterTime - beforeTime;
						convertcost += timeDistance;
						
						mHandler.obtainMessage(MSG_SUCCESS, bmp2).sendToTarget();// 获取图片成功，向ui线程发送MSG_SUCCESS标识和bitmap对象
						if (count % 20000 == 0) {
							endtime = System.currentTimeMillis();
							Log.e("Time Per Frame", "Delay: " + delay/count +" Decode1: " + decodewait + " Decode2: " + decodewait2 + "  Convert: " + convertwait + "  Net: " + recvwait);
							Log.e("Time Per Frame", "net: " + netcost/count + " decode: " + decodecost/count + "  Trans: " + convertcost/count);
							Log.e("Time Per Frame", "time: " + (endtime -starttime)/count);
							Log.e("Time Per Frame", "unsync: " + forwardcount + " + " + backcount);
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
		//setContentView(R.layout.activity_main);
		super.onCreate(savedInstanceState);
		/* force 32-bit display for better display quality */
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(getWindow().getAttributes());
		lp.format = PixelFormat.RGBA_8888;
		getWindow().setBackgroundDrawable(new ColorDrawable(0xff000000));
		getWindow().setAttributes(lp);
		/* copy the test yuv file */
		
		mRenderView = new ImageView(this);
		setContentView(mRenderView);

		theClient = 0;
		masterIp = "10.0.0.2";
		Intent i = getIntent();
		gameName = i.getStringExtra("gameName");
		//mImageView = (ImageView) findViewById(R.id.imageView);
		//mRenderView = (RenderView) findViewById(R.id.drawView);
		node = new ServerNode();
		myUdt = new AndroidUdt();
		theRect = new MyRect();
		mPauseLock = new Object();
		mPaused = false;
		mFinished = false;
//		masterIp = readFileSdcard("ip.txt");
		//Log.e("IP", masterIp);

		if (0 <= connectMaster(masterIp, gameName)) {
			// node.ip = "59.66.131.29";
			// node.port = 9003;
			Log.e("Server Info:", node.ip + " " + node.port);
			if (0 <= connectServer(node.ip, node.port, "yhx")) {
				theClient = connectGame(node.ip, node.port, theRect);
				//theDecoder = new VP8Decoder(theRect.getW(), theRect.getH());
				vpxDecoder = new VPXDecoder();
				bufsize = AudioTrack.getMinBufferSize(audioRatio, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
				mbuf = new byte[0];
				//mbuf_loc = 0;
				trackPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, audioRatio, AudioFormat.CHANNEL_OUT_STEREO, 
						AudioFormat.ENCODING_PCM_16BIT, bufsize, AudioTrack.MODE_STREAM);
				converter = new YUVConverter();
				//yvconv = new YV12ToRGB32();
				if (theClient > 0) {
					vpxDecoder.init();
					trackPlayer.play();
//					mThread = new Thread(runnable);
//					mThread.start();// 线程启动
					mThread1 = new Thread(runRecv);
					mThread2 = new Thread(runDecode);
					mThread3 = new Thread(runConvert);
					starttime = System.currentTimeMillis();
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
			AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
			ad.setTitle("Error");
			ad.setMessage("Connect to Master Failed! @" + masterIp);
			ad.setPositiveButton("确定", null);
			ad.show();
		}
	}
	
//	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		synchronized(mPauseLock) {
			mPaused = true;
		}
		Log.e("Thread", "Pause");
		try {
			mThread1.wait();
			mThread2.wait();
			mThread3.wait();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		super.onPause();
	}
	
	@Override  
	protected void onStop() {
		synchronized(mPauseLock) {
			mPaused = true;
		}
		Log.e("Thread", "Pause");
		mFinished = true;
		super.onStop();
	}
	
	@Override
	protected void onResume() {
		Log.e("Thread", "Resume");
		super.onResume();
		synchronized(mPauseLock) {
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

	public Bitmap playGame() {
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
			//TODO
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
//				if (recode) {
//					writeFileToSD(videoData, "file.txt");
//					// recode = false;
//				}

				// libvpx
				try {
					//TODO
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
//						if (recode) {
//							writeFileToSD(yuvData.y, "y.txt");
//							// recode = false;
//						}
//						if (recode) {
//							writeFileToSD(yuvData.u, "u.txt");
//							// recode = false;
//						}
//						if (recode) {
//							writeFileToSD(yuvData.v, "v.txt");
//							recode = false;
//						}
//						Log.e("Data:" , "" + theRect.width + " " + theRect.height);
//						byte[] yuvDatas1 = new byte[yuvData.y.length
//								+ yuvData.u.length + yuvData.v.length];
//						System.arraycopy(yuvData.y, 0, yuvDatas1, 0,
//								yuvData.y.length);
//						System.arraycopy(yuvData.u, 0, yuvDatas1, yuvData.y.length,
//								yuvData.u.length);
//						System.arraycopy(yuvData.v, 0, yuvDatas1, yuvData.y.length + yuvData.u.length,
//								yuvData.v.length);
//						
//						writeFileToSD(yuvDatas1, "test.yuv");
						
						//YUVImage
						//TODO
						long beforeTime = System.currentTimeMillis();

						//Another conveter
						Bitmap bmp2 = Bitmap.createBitmap(theRect.getW(), theRect.getH(), Bitmap.Config.ARGB_8888);
						//converter.getFrame(bmp2, yuvData.y, yuvData.u, yuvData.v, theRect.getW(), theRect.getH());
						bmp2.copyPixelsFromBuffer(ByteBuffer.wrap(videoData));
						
						long afterTime = System.currentTimeMillis();
						long timeDistance = afterTime - beforeTime;
//						transcost3 += (afterTime - afterTime4);
						convertcost += timeDistance;
						return bmp2;
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
		
		byte[] loc = new byte[4];
		loc = myUdt.recv(theClient, loc, loc.length, 0);
		MainActivity.videoLoc = AndroidUdt.byte2int(loc);
		Log.e("Video Loc", "" + videoLoc);
		
		iAudioSize = AndroidUdt.byte2int(size) - 4;
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
//				byte[] buf_t = new byte[mbuf.length + rAudioSize];
//				if (mbuf.length != 0) {
//					System.arraycopy(mbuf, 0, buf_t, 0, mbuf.length);
//				}
//				System.arraycopy(audioData, 0, buf_t, mbuf.length, rAudioSize);
//				mbuf = buf_t;
//				
//				if (mbuf.length > bufsize) {
//					trackPlayer.write(mbuf, 0, mbuf.length);
//					mbuf = new byte[0];
//				}
//				if (recode) {
//					writeFileToSD(audioData, "audio.txt");
//					recode = false;
//				}
				// TODO
//				android.os.Process.killProcess(android.os.Process
//						.myPid());
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
			String pathName = "/sdcard/yungame/";
			FileInputStream fin = new FileInputStream(pathName + fileName);
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			new String(buffer);
			res = EncodingUtils.getString(buffer, "UTF-8");  
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

	public int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {

		final int frameSize = width * height;

		int rgb[] = new int[width * height];
		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

			}
		}
		return rgb;
	}

	@SuppressLint("SdCardPath")
	public static void saveMyBitmap(Bitmap mBitmap, String bitName) throws IOException {
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

	final private String TAG = "mouse";
	final private String KTAG = "key";
	
	@SuppressLint("NewApi")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int touchEvent = event.getAction();
		int buttonState = event.getButtonState();
		String actionType = "<DUM>";
		switch (touchEvent) {
		case MotionEvent.ACTION_DOWN:
			actionType = "down";
			if (buttonState == 0) {
				buttonState = 2;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			actionType = "move";			
			break;
		case MotionEvent.ACTION_UP:
			actionType = "up";
			if (buttonState == 0) {
				buttonState = lButtonState;
			}
			break;
		default:
			break;
		}
		
		String buttonFlag = "<LRM>";
		switch (buttonState) {
		case MotionEvent.BUTTON_PRIMARY:
			buttonFlag = "left";
			break;
		case MotionEvent.BUTTON_SECONDARY:
			buttonFlag = "right";
			break;
		case MotionEvent.BUTTON_TERTIARY:
			buttonFlag = "middle";
			break;
		default:
			buttonState = lButtonState;//Since buttonstate = 0 while mouse-up event fired, assume mouse button is the same as latest one.
			break;
		};
		lButtonState = buttonState;
		buttonFlag += " " + buttonState;
		display(TAG, "Action " + actionType + "..." + buttonFlag);

		packMouseInput(event);
		return super.onTouchEvent(event);

	}
	
	@Override
	
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		
		return super.onKeyLongPress(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {

		case KeyEvent.KEYCODE_DPAD_CENTER:
			display(KTAG, "middle key");
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			display(KTAG, "Left key");
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			display(KTAG, "right key");
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			display(KTAG, "down key");
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
			display(KTAG, "up key");
			break;
		case KeyEvent.KEYCODE_1:
			display(KTAG, "松开了数字键1");
			break;
		case KeyEvent.KEYCODE_3:
			display(KTAG, "松开了数字键3");
			break;
		case KeyEvent.KEYCODE_7:
			display(KTAG, "松开了数字键7");
			break;
		default:
			break;
		}
		packKeyInput((short)event.getUnicodeChar(), (short)1);
		return super.onKeyUp(keyCode, event);
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		display(KTAG, "..." + keyCode);
		switch (keyCode) {

		case KeyEvent.KEYCODE_DPAD_CENTER:
			display(KTAG, "middle key");
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			display(KTAG, "Left key");
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			display(KTAG, "right key");
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			display(KTAG, "down key");
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
			display(KTAG, "up key");
			break;
		case KeyEvent.KEYCODE_1:
			display(KTAG, "按下了数字键1");
			break;
		case KeyEvent.KEYCODE_3:
			display(KTAG, "按下了数字键3");
			break;
		case KeyEvent.KEYCODE_7:
			display(KTAG, "按下了数字键7");
			break;
		default:
			break;
		}
		
		packKeyInput((short)event.getUnicodeChar(), (short)0);
		return super.onKeyDown(keyCode, event);
	}
	
	private void packKeyInput(short keyCode, short flag) {
		
		RawKeyboardInput keyInput = new RawKeyboardInput();
		keyInput.setFlags(flag);
		keyInput.setvKey(keyCode);
		
		byte[] serialize = keyInput.getKeyboardInputStream();
		//myUdt.send(thecli, len, flags)
		myUdt.send(theClient, serialize, serialize.length, 0);
		printHexString(serialize);
	}
	
	@SuppressLint("NewApi")
	private void packMouseInput(MotionEvent event) {
		RawMouseInput mouseInput = new RawMouseInput();
		// which button: Left, Right, Middle
		int buttonState = event.getButtonState();
		if (buttonState > 0) {
			lButtonState = buttonState;
		} else if (event.getAction() == MotionEvent.ACTION_DOWN){
			buttonState = 2;//right button
			lButtonState = buttonState;
		} else //action_up 
		{
			buttonState = lButtonState;
		}
		// up or down
		int buttonFlag = 1;
		if (buttonState == 1) {
			buttonFlag = (event.getAction() == MotionEvent.ACTION_DOWN) ? 1 : 2;
		} else if (buttonState == 2) {
			buttonFlag = (event.getAction() == MotionEvent.ACTION_DOWN) ? 4 : 8;
		}
		mouseInput.setUButtonFlag((short)buttonFlag);
		// point (x, y) in dwSize
		mouseInput.setDwSize((short)event.getX(), (short)event.getY());

		// delta X, delta Y
		mouseInput.setlLastX(0);
		mouseInput.setlLastY(0);
		
		byte[] serialize = mouseInput.getMouseInputStream();
		//printHexString(serialize);
		
//		checkHistoricValue(event);
		myUdt.send(theClient, serialize, serialize.length, 0);
//		this.checkAxixValue(event);
	}
	
	
	@SuppressLint("NewApi")
	private void checkAxixValue(MotionEvent event) {
		float valueOfAxisX = event.getAxisValue(MotionEvent.AXIS_X);
		float valueOfAxisY = event.getAxisValue(MotionEvent.AXIS_Y);
		
		float valueOfX = event.getX();
		float valueOfY = event.getY();
		//event.getr
		
		display("MotionEvent", "(" + valueOfX +", "+ valueOfY + ") vs (" + valueOfAxisX + ", " + valueOfAxisY +")");
	}
	
	@SuppressLint("NewApi")
	private void checkHistoricValue(MotionEvent event) {
		int hSize = event.getHistorySize();
		
		
		float valueOfAxisX = hSize > 1 ? event.getHistoricalX(1) : -1;
		float valueOfAxisY = hSize > 1 ? event.getHistoricalY(1) : -1;
		
		float valueOfX = event.getX();
		float valueOfY = event.getY();
		
		float deltaX = hSize > 1 ? (event.getHistoricalX(0) - event.getHistoricalX(1)) : 0;
		float deltaY = hSize > 1 ? (event.getHistoricalY(0) - event.getHistoricalY(1)) : 0;
		display ("MotionEvent", "(" + deltaX +", "+ deltaY + ")");
//		display("MotionEvent", "(" + valueOfX +", "+ valueOfY + ") vs (" + valueOfAxisX + ", " + valueOfAxisY +")");
	}
	
	public void printHexString( byte[] b) {
		String output = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}			
			output += hex + " ";
		}
//		myUdt.send(theClient, serialize, serialize.length, 0);
		display("key input serialize: ", output);
		

	}

	public void display(String tag, String text) {
		//mTextView.setText(tag + ": " + text);
		Log.e(tag + ":", text);
		System.out.println("mylog: " + tag + ": " + text);

	}
}
