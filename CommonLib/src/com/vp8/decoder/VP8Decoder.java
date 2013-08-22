package com.vp8.decoder;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

public class VP8Decoder {
	MediaCodec codec;
	// MediaExtractor extractor;
	ByteBuffer[] inputBuffers;
	ByteBuffer[] outputBuffers;

	@SuppressLint("NewApi")
	public VP8Decoder(int width, int height) {
		codec = MediaCodec.createDecoderByType("video/x-vnd.on2.vp8");
		MediaFormat format = MediaFormat.createVideoFormat(
				"video/x-vnd.on2.vp8", width, height);
//		format.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
//		format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
//		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
//		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
		
		codec.configure(format, null, null, 0);
		// extractor = new MediaExtractor();
		codec.start();
	}

	@SuppressLint("NewApi")
	public void close() {
		codec.stop();
		codec.release();
		codec = null;
	}
	
	@SuppressLint("NewApi")
	public byte[] startDecode(byte[] input) {
		
		try {
			inputBuffers = codec.getInputBuffers();
			outputBuffers = codec.getOutputBuffers();
			int inputBufferIndex = codec.dequeueInputBuffer(-1);
			Log.e("Input Index" , "" + inputBufferIndex);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				inputBuffer.put(input);
				codec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
			}
 
			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
			Log.e("Output Index" , "" + outputBufferIndex);
			if (outputBufferIndex >= 0) {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				byte[] outData = new byte[bufferInfo.size];
				outputBuffer.get(outData);
//				ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
//				if (spsPpsBuffer.getInt() == 0x00000001) {
//					System.out.println("parsing sps/pps");
//				} else {
//					System.out.println("something is amiss?");
//				}
//				int ppsIndex = 0;
//				while (!(spsPpsBuffer.get() == 0x00
//						&& spsPpsBuffer.get() == 0x00
//						&& spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {
//
//				}
				// ppsIndex = spsPpsBuffer.position();
				// sps = new byte[ppsIndex - 8];
				// System.arraycopy(outData, 4, sps, 0, sps.length);
				// pps = new byte[outData.length - ppsIndex];
				// System.arraycopy(outData, ppsIndex, pps, 0, pps.length);
				// if (null != parameterSetsListener) {
				// parameterSetsListener.avcParametersSetsEstablished(sps, pps);
				// }

				codec.releaseOutputBuffer(outputBufferIndex, false);
				//outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
				return outData;
			}
		} catch (Throwable t) {
			Log.e("Decoder :", "Throw out Error");
			t.printStackTrace();
		}
		
		Log.e("Decoder :", "return null");
		return null;
	}
}