package com.hbrs;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;

import org.opencv.android.JavaCameraView;

import java.util.List;

public class CustomFPSCameraView extends JavaCameraView {

    public CustomFPSCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }
    public CustomFPSCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void setPreviewFPS(int max){
        Camera.Parameters params = mCamera.getParameters();
        List<int[]> supportedfps = params.getSupportedPreviewFpsRange();
        int minsupportedfps = supportedfps.get(0)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
        if(max * 1000 >= minsupportedfps){
            params.setPreviewFpsRange(minsupportedfps, max*1000);
        }else{
            params.setPreviewFpsRange(minsupportedfps, minsupportedfps);
        }
        mCamera.setParameters(params);
    }
    public Size setPreviewFrameSize(int width,int height){
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> supportedsize = params.getSupportedPictureSizes();
        int maxwidth = supportedsize.get(0).width;
        int maxheight = supportedsize.get(0).height;
        Log.i("supported",String.format("width %d height %d",maxwidth,maxheight));
        if(width > maxwidth && height > maxheight){
            width = maxwidth;
            height = maxheight;
        }
        disconnectCamera();
        connectCamera(width,height);
        return new Size(width,height);
    }
}
