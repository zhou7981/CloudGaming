 package com.example.yungame;

//import io.vec.demo.mediacodec.DecodeActivity.PlayerThread;

//import io.vec.demo.mediacodec.DecodeActivity.PlayerThread;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.util.EncodingUtils;

import com.example.yungame.ServerNode;
import com.udt.android.jni.AndroidUdt;
import com.vp8.decoder.VPXDecoder;
import com.vp8.decoder.YUVConverter;
import com.vp8.decoder.YUVData;
import com.zyh.util.Misc;
import com.zyh.util.RawKeyboardInput;
import com.zyh.util.RawMouseInput;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnHoverListener;
import android.widget.ImageView;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements SurfaceHolder.Callback {
	
	private static final String MYPATH = Environment.getExternalStorageDirectory() + "/";
//	private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/video.mp4";
	private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/1.webm";

	
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
	private float ratioX, ratioY;
//	static int lButtonState = 0;
	
	synchronized static byte[] dataPoll() {
		if (dataQueue.size() <= 0) {
			return null;
		}
		YLog.e("Container:", "PollData");
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
	private static SurfaceView surfaceView;
	private Thread mThread;
	private Thread mThread1;
	private Thread mThread2;
	private Thread mThread3;
	
	//Interactive
	static int lButtonState = 0;
	float lastX = 0, lastY = 0;
	boolean isRightClicked = false;
	
	private static Handler mRecvHandler = new Handler() {
		public void handleMessage(Message msg) {// 此方法在ui线程运行
			switch (msg.what) {
			case MSG_SUCCESS:
				//TODO
				dataQueue.add((byte[]) msg.obj);
				YLog.e("RECVH", "" + dataQueue.size());
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
				YLog.e("Handler", "Show");
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
					YLog.e("Vedio", "About Video");
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
							YLog.e("Time Per Frame", "Delay: " + delay/count +" Decode: " + decodecost/count + " Trans: " + transcost/count + "  Convert: " + convertcost/count + "  Net: " + netcost/count + "  Total: " + timecost/count);
							YLog.e("Time Per Frame", "Trans1: " + transcost1/count + " Trans2: " + transcost2/count + "  Trans3: " + transcost3/count);
							Thread.sleep(10000); 
							// TODO
							android.os.Process.killProcess(android.os.Process
									.myPid());
						}
					} else {
						mHandler.obtainMessage(MSG_FAILURE).sendToTarget();// 获取图片失败
						break;
					}
					YLog.e("Audio", "About Audio");
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
					YLog.y("Video", "About Video");
					int iSize = 0, rSize = 0;
					// int rs = 0;

					long beforeTime = System.currentTimeMillis();
					byte[] size = new byte[4];
					size = myUdt.recv(theClient, size, size.length, 0);
					if (size != null) {
						iSize = AndroidUdt.byte2int(size);
						iSize = iSize & 0x3fffffff;
						YLog.y("Size:", "" + iSize);

						byte[] videoData = new byte[iSize];
						//TODO
						while (rSize < iSize) {
							byte[] buf = new byte[iSize - rSize];
							YLog.e("To Recv", "" + (iSize - rSize));
							buf = myUdt.recv(theClient, buf, buf.length, 0);
							if (buf == null) {
								YLog.y("Recv Error", "Received NULL");
								break;
							}
							YLog.y("Recved", "" + buf.length);
							for (int i1 = 0; i1 < buf.length; i1++) {
								if (rSize + i1 < iSize) {
									videoData[rSize + i1] = buf[i1];
								}
							}
							rSize = rSize + buf.length;
						}
						if (rSize < iSize) {
							YLog.y("RECV NOT ENOUGH:", "" + rSize + " " + iSize);
						} else {
							//mRecvHandler.obtainMessage(MSG_SUCCESS, videoData).sendToTarget();
							while(!TempContiner.dataAdd(videoData)) {
								Thread.sleep(30);
								YLog.y("Thread", "Recv Error");
								recvwait++;
							}
						}
					}
					YLog.y("Audio", "About Audio");
//					while (MainActivity.videoLoc - countVideo.getNum() > 15) {
//						Thread.sleep(20);
//					}
//					playMusic();
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
						YLog.e("Thread", "Decode Error");
						decodewait++;
					}
					YLog.e("Decode", "Start");
					
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
							YLog.e("Thread", "Decode Wait");
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
				YLog.e("Decode", "" + e);
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
						YLog.e("Thread", "Convert Error");
						convertwait++;
					}
					countVideo.adder();
					int curLoc = countVideo.getNum();
					while (MainActivity.videoLoc - curLoc > 3) {
						forwardcount++;
						while ((yuvData1 = TempContiner.picPoll()) == null) {
							Thread.sleep(30);
							YLog.e("Thread", "Convert Error");
							convertwait++;
						}
						countVideo.adder();
						curLoc = countVideo.getNum();
					}
					
					while (curLoc - MainActivity.videoLoc > 3) {
						backcount++;
						Thread.sleep(10);
					}
					YLog.e("Decode", "Start");
					count ++;
					if (yuvData1.success) {

						YLog.e("Decoder", "start");
						long beforeTime = System.currentTimeMillis();
					
						Bitmap bmp2 = Bitmap.createBitmap(theRect.getW(), theRect.getH(), Bitmap.Config.ARGB_8888);
						converter.getFrame(bmp2, yuvData1.y, yuvData1.u, yuvData1.v, theRect.getW(), theRect.getH());
						
						long afterTime = System.currentTimeMillis();
						long timeDistance = afterTime - beforeTime;
						convertcost += timeDistance;
						
						mHandler.obtainMessage(MSG_SUCCESS, bmp2).sendToTarget();// 获取图片成功，向ui线程发送MSG_SUCCESS标识和bitmap对象
						if (count % 20000 == 0) {
							endtime = System.currentTimeMillis();
							YLog.e("Time Per Frame", "Delay: " + delay/count +" Decode1: " + decodewait + " Decode2: " + decodewait2 + "  Convert: " + convertwait + "  Net: " + recvwait);
							YLog.e("Time Per Frame", "net: " + netcost/count + " decode: " + decodecost/count + "  Trans: " + convertcost/count);
							YLog.e("Time Per Frame", "time: " + (endtime -starttime)/count);
							YLog.e("Time Per Frame", "unsync: " + forwardcount + " + " + backcount);
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
				YLog.e("Decode", "" + e);
				return;
			}
		}
	};
	
	private PlayerThread mPlayer = null;

	private SurfaceHolder sHolder; 
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		sHolder = holder;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mPlayer == null) {
			mPlayer = new PlayerThread(holder.getSurface());
			mPlayer.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mPlayer != null) {
			mPlayer.interrupt();
		}
	}

	private class PlayerThread extends Thread {
		private MediaCodec decoder;
		private Surface surface;
		private MediaExtractor extractor;

		public PlayerThread(Surface surface) {
			this.surface = surface;
		}
		
		public MediaCodecInfo selectCodec(String mimeType) {
			MediaCodecInfo ret = null;
			int numCodecs = MediaCodecList.getCodecCount();
			for (int i = 0; i < numCodecs; i++) {
				MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

				if (codecInfo.isEncoder()) {
					continue;
				}

				String[] types = codecInfo.getSupportedTypes();
				for (int j = 0; j < types.length; j++) {
					if (types[j].equalsIgnoreCase(mimeType)) {
						ret = codecInfo;
					}
				}
			}
			return ret;
		}

		public void run02() {
			long lastPresentionTime = 0l;

			extractor = new MediaExtractor();
			extractor.setDataSource(SAMPLE);

			for (int i = 0; i < extractor.getTrackCount(); i++) {
				MediaFormat format = extractor.getTrackFormat(i);
				String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("video/")) {
					extractor.selectTrack(i);
//					decoder = MediaCodec.createDecoderByType(mime);
//					MediaCodecInfo codecInfo = selectCodec(mime);
//		            if (codecInfo == null) {
////		            	decoder = MediaCodec.createDecoderByType(mime);
//		            	return;
//		            } else {
//		            	decoder = MediaCodec.createByCodecName(codecInfo.getName());	
//		            }					
//					decoder.configure(format, surface, null, 0);
					break;
				}
			}
//			MediaFormat format = MediaFormat.createVideoFormat("video/x-vnd.on2.vp8", 1024, 768);
//			MediaFormat format = MediaFormat.createVideoFormat("video/x-vnd.on2.vp8", 640,480);
			MediaFormat format = MediaFormat.createVideoFormat("video/avc", 640, 480);
//			format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
				String mime = format.getString(MediaFormat.KEY_MIME);
				decoder = MediaCodec.createDecoderByType(mime);
				decoder.configure(format, surface, null, 0);

			if (decoder == null) {
				Log.e("DecodeActivity", "Can't find video info!");
				return;
			}

			decoder.start();

			ByteBuffer[] inputBuffers = decoder.getInputBuffers();
			ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
			BufferInfo info = new BufferInfo();
			boolean isEOS = false;
			long startMs = System.currentTimeMillis();

			while (!Thread.interrupted()) {
				if (!isEOS) {
					int inIndex = decoder.dequeueInputBuffer(10000);
					if (inIndex >= 0) {
						ByteBuffer buffer = inputBuffers[inIndex];
						int sampleSize = extractor.readSampleData(buffer, 0);
						if (sampleSize < 0) {
							// We shouldn't stop the playback at this point, just pass the EOS
							// flag to decoder, we will get it again from the
							// dequeueOutputBuffer
							Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
							decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
							isEOS = true;
						} else {
							long sTime = extractor.getSampleTime();							
							decoder.queueInputBuffer(inIndex, 0, sampleSize, sTime, 0);
							Log.e("presentation time: @", sTime - lastPresentionTime + " us");
							lastPresentionTime = sTime;
							extractor.advance();
						}
					}
				}

				int outIndex = decoder.dequeueOutputBuffer(info, 10000);
				switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffers = decoder.getOutputBuffers();
					break;
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
					break;
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
					break;
				default:
					ByteBuffer buffer = outputBuffers[outIndex];
					Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);
					
					Log.v("output buffer", buffInfo(info));

					// We use a very simple clock to keep the video FPS, or the video
					// playback will be too fast
					while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
						try {
							sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
					decoder.releaseOutputBuffer(outIndex, true);
					break;
				}

				// All decoded frames have been rendered, we can stop playing now
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
					break;
				}
			}

			decoder.stop();
			decoder.release();
			extractor.release();
		}
		private String buffInfo(BufferInfo info) {
			String ret = "";
			
			ret += "offset: " + info.offset + ", ";
			ret += "preTime: " + info.presentationTimeUs + ", ";
			ret += "size: " + info.size + ".";
			
			return ret;
		}
		
		
		@SuppressLint({ "InlinedApi", "NewApi" })
		@Override
		public void run() {

//			if (true) {
//				while (true) {
//					try {
//						if (TempContiner.dataPoll() == null) {
//							Thread.sleep(20);
//						}
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					//YLog.e("Thread", "Decode Error");
//					decodewait++;
//				}			
//			}
			
			YLog.y("Size of Rect: ", " " + theRect.getW() + ", " + theRect.getH());
			
//				MediaFormat format = MediaFormat.createVideoFormat("video/x-vnd.on2.vp8", theRect.getW(), theRect.getH());
//			MediaFormat format = MediaFormat.createVideoFormat("video/x-vnd.on2.vp8", 640,480);
			MediaFormat format = MediaFormat.createVideoFormat("video/avc", 640, 480);
			format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
				String mime = format.getString(MediaFormat.KEY_MIME);
				MediaCodecInfo codecInfo = selectCodec(mime);
	            if (codecInfo == null) {
	            	decoder = MediaCodec.createDecoderByType(mime);
	            } else {
	            	decoder = MediaCodec.createByCodecName(codecInfo.getName());	
	            }
//					decoder = MediaCodec.createDecoderByType(mime);
	            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
	                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
//	            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
				
					decoder.configure(format, surface, null, 0);

			if (decoder == null) {
				YLog.e("YUNGame", "Can't find video info!");
				return;
			}

			decoder.start();

			ByteBuffer[] inputBuffers = decoder.getInputBuffers();
			ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
			BufferInfo info = new BufferInfo();
			boolean isEOS = false;
			long startMs = System.currentTimeMillis();
long ds = 30000;
long iCounter = 0;
byte[] lastVideoData = new byte[]{1, 2, 3};;
			while (!Thread.interrupted()) {
				if (!isEOS) {
					int inIndex = decoder.dequeueInputBuffer(10000);
					if (inIndex < 0) continue;
					ByteBuffer buffer = inputBuffers[inIndex];
					buffer.clear();
					if (inIndex >= 0) {
//						ByteBuffer buffer = inputBuffers[inIndex];
						YLog.y("input buffer index", inIndex + "");
						byte[] videoData1;
						while ((videoData1 = TempContiner.dataPoll()) == null) {
							try {
								Thread.sleep(20);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							//YLog.e("Thread", "Decode Error");
							decodewait++;
						}
						if (iCounter++ >= 0 && iCounter < 20) {
//							videoData1 = lastVideoData;
						} else {
//							lastVideoData = videoData1;
						}
						
//						YLog.y("Exception", "Exception: inIndex: " + inIndex );
						inputBuffers[inIndex].put(videoData1);
						YLog.y("BufferInfo: ", "len of @index: " + inputBuffers[inIndex].toString() + ", @rawdata: " + videoData1.toString() + "(" + videoData1.length + ") counter: " + iCounter );
						
//						YLog.e("BufferInfo: ", "len of @index: " + inIndex + ", capacity: "+ inputBuffers[inIndex].capacity() + ", @bytebuffer: " + buffer.capacity() + ", @rawdata: " + videoData1.length);
//						int sampleSize = extractor.readSampleData(buffer, 0);
						if (false) {
							// We shouldn't stop the playback at this point, just pass the EOS
							// flag to decoder, we will get it again from the
							// dequeueOutputBuffer
							YLog.d("YUNGametivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
							decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
							isEOS = true;
						} else {
							decoder.queueInputBuffer(inIndex, 0, videoData1.length, 0, 0);
							ds += 40000;
//							extractor.advance();
						}
					}
				}

				int outIndex = decoder.dequeueOutputBuffer(info, 10000);
				YLog.y("Exception", "Exception: outIndex: " + outIndex );
				switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					YLog.y("YUNGametivity", "len of @outIndex: INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffers = decoder.getOutputBuffers();
					break;
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					YLog.y("YUNGametivity", "New format " + decoder.getOutputFormat());
					break;
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					YLog.y("YUNGametivity", "dequeueOutputBuffer timed out!");
					break;
				default:
					if (outIndex >= 0) {
					ByteBuffer buffer = outputBuffers[outIndex];
					
					
//					final byte[] chunk = new byte[info.size];
//                    buffer.get(chunk);
//                    buffer.clear();
                    
                          //  audioTrack.write(chunk,0,chunk.length);
//                    	YLog.y("BufferInfo: ", "len of out@index: " + outputBuffers[outIndex].capacity() + ", @dedata: " + chunk.length + ": "+ chunk.toString());
                    
					
					Log.v("YUNGametivity", "We can't use this buffer but render it due to the API limit, info.size: " + info.size);
					YLog.y("BufferInfo: ", "len of out@index: " + outputBuffers[outIndex].capacity() + ", @dedata: " + buffInfo(info).toString());
//					
					// We use a very simple clock to keep the video FPS, or the video
					// playback will be too fast
					while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
						try {
							sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
					decoder.releaseOutputBuffer(outIndex, true);
					}
					break;
				}

				// All decoded frames have been rendered, we can stop playing now
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					YLog.d("YUNGametivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
					break;
				}
			}

			decoder.stop();
			decoder.release();
		}
	}
	
	
	
	@SuppressLint("NewApi")
	private String buffInfo(BufferInfo info) {
		String ret = "";
		
		ret += "offset: " + info.offset + ", ";
		ret += "preTime: " + info.presentationTimeUs + ", ";
		ret += "size: " + info.size + ".";
		
		return ret;
	}
	
	
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
		
		//mRenderView = new ImageView(this);
		surfaceView = new SurfaceView(this);
		surfaceView.getHolder().addCallback(this);
		setContentView(surfaceView);
		Misc.buildKeyMap();

		theClient = 0;
		masterIp = "10.0.0.11";
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
		//YLog.e("IP", masterIp);

//		View container = findViewById(R.id.container1);
		surfaceView.setOnHoverListener(new YOnHoverListener());
		
		if (0 <= connectMaster(masterIp, gameName)) {
			// node.ip = "59.66.131.29";
			// node.port = 9003;
			YLog.e("Server Info:", node.ip + " " + node.port);
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
					if (mPlayer == null && sHolder != null) {
						mPlayer = new PlayerThread(sHolder.getSurface());
						mPlayer.start();
					}
					
//					mThread2 = new Thread(runDecode);
//					mThread3 = new Thread(runConvert);
					starttime = System.currentTimeMillis();
					mThread1.start();
//					mThread2.start();
//					mThread3.start();
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
		YLog.e("Thread", "Pause");
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
		YLog.e("Thread", "Pause");
		mFinished = true;
		super.onStop();
	}
	
	@Override
	protected void onResume() {
		YLog.e("Thread", "Resume");
		super.onResume();
		synchronized(mPauseLock) {
			mPaused = false;
			mPauseLock.notifyAll();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_main, menu);
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
			YLog.e("Size:", "" + iSize);

			byte[] videoData = new byte[iSize];
			//TODO
			long beforeTime0 = System.currentTimeMillis();
			while (rSize < iSize) {
				byte[] buf = new byte[iSize - rSize];
				YLog.e("To Recv", "" + (iSize - rSize));
				buf = myUdt.recv(theClient, buf, buf.length, 0);
				if (buf == null) {
					YLog.e("Recv Error", "Received NULL");
					break;
				}
				YLog.e("Recved", "" + buf.length);
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
				YLog.e("RECV NOT ENOUGH:", "" + rSize + " " + iSize);
				return null;
			} else {
				// Decode videoData
				YLog.e("RECV ENOUGH:", "" + rSize + " " + iSize);
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

						YLog.e("Decoder", "Success");
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
//						YLog.e("Data:" , "" + theRect.width + " " + theRect.height);
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
					YLog.e("yuv", "Failed: " + e);
				}
				YLog.e("Decoder", "Failed!");
				// MediaCodec

				// YLog.e("Start Decode", "Here");
				// byte[] yuvData = theDecoder.startDecode(videoData);
				// YLog.e("Start YUV", "Here");
				// if (yuvData == null) {
				// YLog.e("ERROR","YUV is null");
				// } else {
				// YLog.e("YUV size:", "" + yuvData.length);
				// }
				//
				// // YuvImage yuvimage=new YuvImage(yuvData, ImageFormat.NV21,
				// theRect.getW(), theRect.getH(), null);
				// // YLog.e("YUV finish", "Here");
				// // ByteArrayOutputStream baos = new ByteArrayOutputStream();
				// // YLog.e("Start compress", "Here");
				// // yuvimage.compressToJpeg(new Rect(0, 0, theRect.getW(),
				// theRect.getH()), 80, baos);
				// // byte[] jdata = baos.toByteArray();
				// // Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0,
				// jdata.length);
				//
				// int[] jdata = decodeYUV420SP(yuvData, theRect.getW(),
				// theRect.getH());
				// YLog.e("Convert Bitmap", "Here");

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
		YLog.y("Video Loc", "" + videoLoc);
		
		iAudioSize = AndroidUdt.byte2int(size) - 4;
		YLog.y("TOTAL AUDIO", "" + iAudioSize);
		byte[] audioData = new byte[iAudioSize];
		while (rAudioSize < iAudioSize) {
			byte[] buf = new byte[iAudioSize - rAudioSize];
			YLog.e("To Recv", "" + (iAudioSize - rAudioSize));
			buf = myUdt.recv(theClient, buf, buf.length, 0);
			if (buf == null) {
				YLog.e("Recv Error", "Received NULL");
				break;
			}
			YLog.e("Recved", "" + buf.length);
			for (int i1 = 0; i1 < buf.length; i1++) {
				if (rAudioSize + i1 < iAudioSize) {
					audioData[rAudioSize + i1] = buf[i1];
				}
			}
			rAudioSize = rAudioSize + buf.length;
		}

		if (rAudioSize < iAudioSize) {
			YLog.e("RECV NOT ENOUGH:", "" + rAudioSize + " " + iAudioSize);
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
				YLog.e("Music:", "Error:" + e);
			}
			return;
		}
	}

	public int connectMaster(String ip, String name) {
		int client = myUdt.socket(ip, MainActivity.MASTER_PORT,
				AndroidUdt.SOCK_DGRAM);

		if (AndroidUdt.ERROR == client) {
			YLog.e("UDT ERROR", "socket error client=" + client);
			return -1;
		}

		int res = -1;
		for (int i = 0; i < 4; i++) {
			if (AndroidUdt.ERROR == myUdt.connect(client, ip,
					MainActivity.MASTER_PORT)) {
				YLog.e("Debug:", "here");
			} else {
				res = 0;
				break;
			}
		}

		if (res == AndroidUdt.ERROR) {
			YLog.e("UDT ERROR", "connect error");
			return -1;
		}

		byte[] tmp = name.getBytes();
		byte[] tmpSend = new byte[tmp.length + 1];
		System.arraycopy(tmp, 0, tmpSend, 0, tmp.length);
		tmpSend[tmp.length] = 0;
		YLog.e("Send", new String(tmpSend));
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
		YLog.e("K", "" + k);
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
			YLog.e("UDT ERROR", "socket error client=" + client);
			return -1;
		}

		YLog.e("ConnectServer", ip + " " + port);
		int res = -1;
		for (int i = 0; i < 4; i++) {
			if (AndroidUdt.ERROR == myUdt.connect(client, ip,
					MainActivity.SERVER_PORT)) {
				YLog.e("Debug:", "here");
			} else {
				res = 0;
				break;
			}
		}

		if (res == AndroidUdt.ERROR) {
			YLog.e("UDT ERROR", "connect error");
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
			YLog.e("UDT ERROR", "socket error client=" + client);
			return -1;
		}

		int res = -1;
		for (int i = 0; i < 4; i++) {
			if (AndroidUdt.ERROR == myUdt.connect(client, szIP, iPort)) {
				YLog.e("UDT ERROR", "connect error");
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
			YLog.d("TestFile", "SD card is not avaiable/writeable right now.");
			return;
		}
		try {
			String pathName = "/sdcard/Download/test/";
			String fileName = name;
			File path = new File(pathName);
			File file = new File(pathName + fileName);
			if (!path.exists()) {
				YLog.d("TestFile", "Create the path:" + pathName);
				path.mkdir();
			}
			if (!file.exists()) {
				YLog.d("TestFile", "Create the file:" + fileName);
				file.createNewFile();
			}
			FileOutputStream stream = new FileOutputStream(file);
			stream.write(buf);
			stream.close();

		} catch (Exception e) {
			YLog.e("TestFile", "Error on writeFilToSD.");
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
			YLog.e("Pic: ", "" + e);
			e.printStackTrace();
		}
		mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
		YLog.e("Pic: ", "Saved!");
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
	
	@SuppressLint("NewApi")
	@Override
	public boolean onTouchEvent(MotionEvent event) {		
		YMouseEvent mouseEvt = new YMouseEvent(event);
		mouseEvt.getMouseEvt();
		return super.onTouchEvent(event);
//		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (isMouseRightClick(keyCode, event)) {
			handleMouseRightClick(true);
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyUp(keyCode, event);
		}
		
		short vKeyCode = Misc.getVirtualKeyCode(keyCode, event);
		if (vKeyCode == 0) return super.onKeyUp(vKeyCode, event);
		
		packKeyInput(vKeyCode, (short)1);
		return true;
	}	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isMouseRightClick(keyCode, event)) {
			handleMouseRightClick(false);
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyUp(keyCode, event);
		}
		
		short vKeyCode = Misc.getVirtualKeyCode(keyCode, event);
		if (vKeyCode == 0) return super.onKeyDown(vKeyCode, event);
		packKeyInput(vKeyCode, (short)0);
		return super.onKeyDown(keyCode, event);
//		return true;
	}
	
	private void packKeyInput(short keyCode, short flag) {		
		
		RawKeyboardInput keyInput = new RawKeyboardInput();
		keyInput.setFlags(flag);
		keyInput.setvKey(keyCode);
		byte[] serialize = keyInput.getKeyboardInputStream();
		myUdt.send(theClient, serialize, serialize.length, 0);
//		printHexString(serialize);
	}
	
	@SuppressLint("NewApi")
	private void packMouseInput(MotionEvent event, short usButtonFlags) {
		if (theClient == 0) return;
		
		RawMouseInput mouseInput = new RawMouseInput();					
		mouseInput.setUButtonFlag(usButtonFlags);
		// point (x, y) in dwSize
		
		float x, y;
		x = event.getX() * theRect.getW() / surfaceView.getWidth();
		y = event.getY() * theRect.getH() / surfaceView.getHeight();
		mouseInput.setDwSize((short)x, (short)y);
		// delta X, delta Y		
		mouseInput.setlLastX(usButtonFlags == 0 ? (short)(x - lastX) : 0);
		mouseInput.setlLastY(usButtonFlags == 0 ? (short)(y - lastY) : 0);
//		display(TAG, "(dx, dy): " + "(" +  mouseInput.getlLastX() + "," + mouseInput.getlLastY() + ")");
		lastX = x;
		lastY = y;	
		byte[] serialize = mouseInput.getMouseInputStream();
		myUdt.send(theClient, serialize, serialize.length, 0);
		//printHexString(serialize);
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
//		display("key input serialize: ", output);
		

	}

	
	@SuppressLint("NewApi")
	public class YOnHoverListener implements OnHoverListener{
		 @SuppressLint("NewApi")
		@Override
         public boolean onHover(View v, MotionEvent event) {
             if (event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                 	packMouseInput(event, (short)0);
             }
             return true;
         }		
	}
	
	public class YMouseEvent {
		private MotionEvent event;
		private short updown = 1; //1X down, 2X up
		private short leftrightmid = 1; //1 left; 4 right; 16 middle
		
		public YMouseEvent(MotionEvent event) {		
			this.event = event;
		}
		
		@SuppressLint("NewApi")
		public void getMouseEvt() {
			int touchEvent = event.getAction();
			int buttonState = event.getButtonState();
			
			switch (touchEvent) {
			case MotionEvent.ACTION_DOWN:
				updown = 1;
				if (buttonState == 0) {
					buttonState = 2;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				updown = 0;			
				break;
			case MotionEvent.ACTION_UP:
				updown = 2;
				if (buttonState == 0) {
					buttonState = lButtonState;
				}
				break;
			default:
				updown = 0;
				break;
			}
			
			switch (buttonState) {
			case MotionEvent.BUTTON_PRIMARY:
				leftrightmid = 1;
				break;
			case MotionEvent.BUTTON_SECONDARY:
				leftrightmid = 4;
				isRightClicked = true;
				break;
			case MotionEvent.BUTTON_TERTIARY:
				leftrightmid = 16;
				break;
			default:
				buttonState = lButtonState;//Since buttonstate = 0 while mouse-up event fired, assume mouse button is the same as latest one.
				break;
			};
			lButtonState = buttonState;
			
//			display2(TAG, "Action " + getUsButtonFlags() + "; (l/r)" + isRightClicked + " by " + event.getDevice().toString());
			
			packMouseInput(event, this.getUsButtonFlags());			
		}
		
		public short getUsButtonFlags() {
			return (short)(updown * leftrightmid);
		}			
	}
	
	public boolean isMouseRightClick(int keyCode, KeyEvent event) {		
		return (keyCode == KeyEvent.KEYCODE_BACK && event.getSource() == InputDevice.SOURCE_MOUSE);
	}
	
	public void handleMouseRightClick(boolean isUpDown) {
		RawMouseInput mouseInput = new RawMouseInput();					
		mouseInput.setUButtonFlag((short) (isUpDown ? 8 : 4));
		// point (x, y) in dwSize
		mouseInput.setDwSize((short)lastX, (short)lastY);
		// delta X, delta Y		
		mouseInput.setlLastX(0);
		mouseInput.setlLastY(0);
//			
		byte[] serialize = mouseInput.getMouseInputStream();
		myUdt.send(theClient, serialize, serialize.length, 0);
			
	}
		
	
}
