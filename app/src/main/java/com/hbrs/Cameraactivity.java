package com.hbrs;

import android.os.Bundle;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import androidx.appcompat.app.AppCompatActivity;

import com.hbrs.R;

//*************************************************************************************************
public class Cameraactivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2
{
    //-----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        // Initialize OpenCV
        JavaCameraView mCamView = (JavaCameraView)findViewById( R.id.camera_view);
        mCamView.setCameraIndex(mCamView.CAMERA_ID_ANY);
        mCamView.setCvCameraViewListener(this);
        mCamView.enableView();

        OpenCVLoader.initDebug();
    }

    //-----------------------------------------------------------------
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        // Get frame
        Mat mImg = inputFrame.rgba();

        //Mat mRgbaT = mImg.t();
        //Core.flip(mImg.t(), mRgbaT, 1);

        //Imgproc.resize(mRgbaT, mRgbaT, mImg.size());
        // return image to CameraView

        //mRgbaT.release();
        return  mImg;
    }

    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStarted(int width, int height)
    {
    }

    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStopped()
    {
    }
}