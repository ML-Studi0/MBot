package com.hbrs;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

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
        Log.i("minfps",String.valueOf(minsupportedfps));
        if(max * 1000 >= minsupportedfps){
            params.setPreviewFpsRange(minsupportedfps, max*1000);
        }else{
            params.setPreviewFpsRange(minsupportedfps, minsupportedfps);
        }
        mCamera.setParameters(params);
    }
}
