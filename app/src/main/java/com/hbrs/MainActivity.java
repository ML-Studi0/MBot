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
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraBridgeViewBase;
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

    Scalar lowerlowerredhue = new Scalar(0, 150, 100); //lower bound of the low red hsv range
    Scalar upperlowerredhue = new Scalar(5, 255, 255); //upper bound of the low red hsv range

    Scalar lowerupperredhue = new Scalar(175, 150, 100); //lower bound of the upper red hsv range
    Scalar upperupperredhue = new Scalar(180, 255, 255); //upper bound of the upper red hsv range

    CustomFPSCameraView mCamView;

    Mat hsv,greyscale,mask1,mask2,combinedmask,circles;

    int mindim, framewidth;

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
        mCamView = (CustomFPSCameraView) findViewById( R.id.camera_view); // get the cameraview from the ui
        mCamView.setCameraIndex(mCamView.CAMERA_ID_ANY); // set the id of the camera to use if device has multiple (by default the read camera)
        mCamView.setCvCameraViewListener(this); // set the listener of the camera view ti this class
        mCamView.enableView();
        OpenCVLoader.initDebug(false); // start the opencv library (false is passed to disable cuda runtime libraries which through testing was found to be faster)
    }

    //-----------------------------------------------------------------
    public void onBtnConnect(View view) //connect to robot view bluetooth
    {
        Log.i(TAG, "Connect...");

        BT_DeviceListActivity.connect( this, REQUEST_BLUETOOTH_ENABLE, REQUEST_BLUETOOTH_GET_ADDR );
    }

    //*********************************************************************************************
    // Test
    //-----------------------------------------------------------------
    int ledId = 12;

    //-----------------------------------------------------------------
    public void onBtnLED(View view) //turn off all leds and set one of them to red in a circular way
    {
	   mbot.setLight( 0, 0, 0, 0 );
       mbot.setLight( ledId, 20, 0, 0 );
       ledId = (ledId%12)+1;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i("resolution",String.format("width %d height %d",width,height));
        // initialize variables used in frame processing
        hsv = new Mat(height, width, CvType.CV_8UC3);
        mask1 = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        mask2 = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        combinedmask = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        greyscale = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        android.util.Size previewframesize = mCamView.setPreviewFrameSize(1280,720); // set the frame size to be 1280*720 (HD resolution)
        mindim = Math.min(previewframesize.getWidth(),previewframesize.getHeight()); // set the variable to be the equal to the minimum between the width and the height
        framewidth = previewframesize.getWidth();
        mCamView.setPreviewFPS(20); // set the framerate to be 20 frames/second in order to minimize the number of frames processed per second
    }

    @Override
    public void onCameraViewStopped() {
        // release all the variables defined in onCameraViewStarted since they have a strong reference and neeed to be released manually
        hsv.release();
        mask1.release();
        mask2.release();
        combinedmask.release();
        greyscale.release();
        mbot.setDrive(0,0); //stop the robot in case it was running since this function is called whenever the camera view isnt visible (onPause or onStop)
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mImg = inputFrame.rgba(); //get the frame in RGBA format
        Imgproc.cvtColor(mImg, hsv, Imgproc.COLOR_RGB2HSV, 3); // convert the rgb channels to hsv format

        Core.inRange(hsv, lowerlowerredhue, upperlowerredhue, mask1); //find the pixels which have a hsv between the defined lower red range
        Core.inRange(hsv, lowerupperredhue, upperupperredhue, mask2); //find the pixels which have a hsv between the defined upper red range

        //flip the masks since it sets the the pixels in range to black hsv (0,0,0) and pixels out range to white hsv(180,255,255) max for each channel
        Core.bitwise_not( mask1, mask1);
        Core.bitwise_not( mask2, mask2);
        Core.bitwise_and( mask1, mask2,combinedmask); // and the two masks together since we want pixels in both the lower and upper red ranges

        hsv.setTo(new Scalar(0,0,0), combinedmask); // apply the mask to the hsv frame

        Imgproc.cvtColor(hsv, greyscale, Imgproc.COLOR_BGR2GRAY); // convert the hsv to greyscale since it is required by the houghcircles function for the frame to be greyscale

        circles = new Mat();

        Imgproc.blur(greyscale, greyscale, new Size(5, 5)); //blur the image to improve the detection of circles using houghcircles
        Imgproc.HoughCircles(greyscale, circles, Imgproc.CV_HOUGH_GRADIENT, 2.5, 1,100,50); //apply the houghcircles function on the greyscale frame

        Log.i("circles circlespeed",circles.size().toString());

        if (circles.cols() > 0) { //check if there are circles detected in the frame otherwise stop the robot
            double circleVec[] = circles.get(0, 0); //get the coordinates and radius of the first detected circle

            Point center = new Point((int) circleVec[0], (int) circleVec[1]); // get the coordinates of the circle
            double x = (circleVec[0]/ framewidth); //scale the location of the x coordinate to a value between 0 and 1
            int radius = (int) circleVec[2]; //get the radius of the cirlce
            if(x != 0.0) { //check if its not a 0 (this is a fix since sometimes the detected circle would get stuck even after it isn't in the frame anymore) and stop the robot otherwise

                int speedexponential = (int) Math.max((200 - Math.pow(200,(((double) radius * 2) / mindim))), 0); //an exponential function to handle the robot's speed towards the circle sa it gets closer (not used)
                int speedlinear = Math.max(200 - (int) ((((double) radius * 2) / mindim) * 200), 0); //a linear function to handle the robot's speed towards the circle sa it gets closer (f(radius) = max(0,200-((radius*2)/mindim))*200)

                //the x coordinate is scaled between 0 and 1/2 pi to calculate the amount of steering with sin and cos
                //x == 0 means the ball is fully on the right side. x==1/2pi means fully on the left side.
                int speedleft = (int) (speedlinear * Math.cos(x * Math.PI / 2)); //for the left motor we use cos, because the further the ball is on the right the more we want to turn the left motor.
                int speedright = (int) -(speedlinear * Math.sin(x * Math.PI / 2)); //for the right motor we use sin, because the further the ball is on the right the less we want to turn the right motor.

                Log.i("y axis", String.valueOf(x));
                Log.i("speed circlespeed", String.format("speed %d speed left %d speed right %d", speedlinear, speedleft, speedright));

                mbot.setDrive(speedleft, speedright); //set the robots left and right speed

                Imgproc.circle(mImg, center, 3, new Scalar(255, 255, 255), 5); //draw a dot at the center of the detected circle
                Imgproc.circle(mImg, center, radius, new Scalar(255, 255, 255), 2); // draw a circle on top of the detected circle
            }else{
                mbot.setDrive(0,0);
            }
        }else{
            mbot.setDrive(0,0);
        }

        circles.release();

        return mImg;
    }


    //*********************************************************************************************
}
