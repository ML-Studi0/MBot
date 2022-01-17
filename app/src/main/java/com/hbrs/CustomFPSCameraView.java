package com.hbrs;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;

import org.opencv.android.JavaCameraView;

import java.util.List;

// a custom camera class that extends opencv's JavaCameraView that supports setting a custom frame rate and a custom frame size for the preview
public class CustomFPSCameraView extends JavaCameraView {

    public CustomFPSCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }
    public CustomFPSCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    // set a custom maximum framerate for the camera
    public void setPreviewFPS(int max){
        Camera.Parameters params = mCamera.getParameters(); // get the already set camera parameters
        List<int[]> supportedfps = params.getSupportedPreviewFpsRange(); //get the supported framerates by the device's camera
        int minsupportedfps = supportedfps.get(0)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]; //get the minimum supported framerate
        if(max * 1000 >= minsupportedfps){ // check if passed max framerate is greater than the minimum supported framerate
            params.setPreviewFpsRange(minsupportedfps, max*1000); // set the range of framerate to be between the minimum supported and the maximum defined by the function's argument
        }else{
            params.setPreviewFpsRange(minsupportedfps, minsupportedfps); // set the range of framerate to be exactly equal to the minimum supported framerate
        }
        mCamera.setParameters(params);
    }
    // set a defined framesize for the camera
    public Size setPreviewFrameSize(int width,int height){
        Camera.Parameters params = mCamera.getParameters(); // get the already set camera parameters
        List<Camera.Size> supportedsize = params.getSupportedPictureSizes(); //get the supported frame sizes by the device's camera
        // get the width and height of the maximum supported frame size
        int maxwidth = supportedsize.get(0).width;
        int maxheight = supportedsize.get(0).height;
        Log.i("supported",String.format("width %d height %d",maxwidth,maxheight));
        if(width > maxwidth && height > maxheight){ //check if passed width and height are less than the maximum supported sizes and set them
            width = maxwidth;
            height = maxheight;
        }
        // disconnect and reconnect the camera to apply changes
        disconnectCamera();
        connectCamera(width,height);
        return new Size(width,height);
    }
}
