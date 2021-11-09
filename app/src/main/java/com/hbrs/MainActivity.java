//*******************************************************************
/*!
\file   MainActivity.java
\author Thomas Breuer
\date   07.09.2021
\brief
*/

//*******************************************************************
package com.hbrs;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.*;

//*************************************************************************************************
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{
    private static final String TAG = "MBot";

    private MBot mbot;

    Scalar lowerlowerredhue = new Scalar(0, 100, 100);
    Scalar upperlowerredhue = new Scalar(10, 255, 255);

    Scalar lowerupperredhue = new Scalar(170, 100, 100);
    Scalar upperupperredhue = new Scalar(180, 255, 255);

    CustomFPSCameraView mCamView;

    Mat hsv,greyscale,mask1,mask2,combinedmask,circles;

    int mindim,framewidth;

    private final static int REQUEST_BLUETOOTH_ENABLE = 1;
    private final static int REQUEST_BLUETOOTH_GET_ADDR = 2;

    //-----------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        switch(requestCode)
        {
            case REQUEST_BLUETOOTH_ENABLE:
                break;
            case REQUEST_BLUETOOTH_GET_ADDR:
                if (resultCode == Activity.RESULT_OK)
                {
                    mbot.connect( this, BT_DeviceListActivity.getAddr( data ) );
                }
                break;
        }
    }

    //-----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mbot = new MBot();

        // Initialize OpenCV
        mCamView = (CustomFPSCameraView) findViewById( R.id.camera_view);
        mCamView.setCameraIndex(mCamView.CAMERA_ID_ANY);
        mCamView.setCvCameraViewListener(this);
        mCamView.enableView();
        OpenCVLoader.initDebug();
    }

    //-----------------------------------------------------------------
    public void onBtnConnect(View view)
    {
        Log.i(TAG, "Connect...");

        BT_DeviceListActivity.connect( this, REQUEST_BLUETOOTH_ENABLE, REQUEST_BLUETOOTH_GET_ADDR );
    }

    //*********************************************************************************************
    // Test
    //-----------------------------------------------------------------
    int ledId = 12;

    //-----------------------------------------------------------------
    public void onBtnLED(View view) 
    {
	   mbot.setLight( 0, 0, 0, 0 );
       mbot.setLight( ledId, 20, 0, 0 );
       ledId = (ledId%12)+1;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        framewidth = width;
        mindim = Math.min(width, height);
        mCamView.setPreviewFPS(15);
        hsv = new Mat(height, width, CvType.CV_8UC3);
        mask1 = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        mask2 = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        combinedmask = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        greyscale = new Mat(height, width, CvType.CV_8U, new Scalar(0));
    }

    @Override
    public void onCameraViewStopped() {
        hsv.release();
        mask1.release();
        mask2.release();
        combinedmask.release();
        greyscale.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // Get frame
        Mat mImg = inputFrame.rgba();
        Imgproc.cvtColor(mImg, hsv, Imgproc.COLOR_RGB2HSV, 3);


        Core.inRange(hsv, lowerlowerredhue, upperlowerredhue, mask1);
        Core.inRange(hsv, lowerupperredhue, upperupperredhue, mask2);

        Core.bitwise_not( mask1, mask1);
        Core.bitwise_not( mask2, mask2);
        Core.bitwise_and( mask1, mask2,combinedmask);

        hsv.setTo(new Scalar(0,0,0), combinedmask);

        List<Mat> hsv_channel = new ArrayList<Mat>();
        Core.split(hsv,hsv_channel);
        greyscale = hsv_channel.get(2);

        circles = new Mat();

        Imgproc.blur(greyscale, greyscale, new Size(7, 7), new Point(2, 2));
        Imgproc.HoughCircles(greyscale, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 100);

        if (circles.cols() > 0) {
            double circleVec[] = circles.get(0, 0);

            Point center = new Point((int) circleVec[0], (int) circleVec[1]);
            double x = (circleVec[0]/framewidth);
            int radius = (int) circleVec[2];

            int speed = Math.max(200 - (int) ((((double)radius * 2)/mindim) * 200),0);

            int speedleft = (int) (speed * Math.sin(x * Math.PI / 2));
            int speedright = (int) -(speed * Math.cos(x * Math.PI / 2));

            Log.i("x",String.valueOf(x));
            Log.i("speed",String.format("speed %d speed left %d speed right %d",speed,speedleft,speedright));

            mbot.setDrive(speedleft,speedright);

            Imgproc.circle(mImg, center, 3, new Scalar(255, 255, 255), 5);
            Imgproc.circle(mImg, center, radius, new Scalar(255, 255, 255), 2);
        }else{
            mbot.setDrive(0,0);
        }

        circles.release();

        return mImg;
    }


    //*********************************************************************************************
}
