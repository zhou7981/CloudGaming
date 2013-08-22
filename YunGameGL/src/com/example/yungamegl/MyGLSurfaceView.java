package com.example.yungamegl;

import com.vp8.decoder.YUVData;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MyGLSurfaceView extends GLSurfaceView {

	private GLRenderC mMyRenderer;
	
    public MyGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        mMyRenderer = new GLRenderC((MainActivity)context);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mMyRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
    
    public void sendMSG(final YUVData data) {	
    	this.queueEvent(new Runnable(){
    		public void run() {
    			mMyRenderer.setBytes(data);
    		}
    	});
    	this.requestRender();
    }
    
    public void sendSize(final MyRect rect) {	
    	this.queueEvent(new Runnable(){
    		public void run() {
    			mMyRenderer.setSize(rect);
    		}
    	});
    	//this.requestRender();
    }
}
