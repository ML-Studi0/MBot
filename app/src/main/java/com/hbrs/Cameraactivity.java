package com.hbrs;

import android.os.Bundle;
import android.util.Log;

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

import androidx.appcompat.app.AppCompatActivity;

import com.hbrs.R;

import java.util.ArrayList;
import java.util.List;

//*************************************************************************************************
public class Cameraactivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2
{
    Scalar lowerlowerredhue = new Scalar(0, 100, 100);
    Scalar upperlowerredhue = new Scalar(10, 255, 255);

    Scalar lowerupperredhue = new Scalar(170, 100, 100);
    Scalar upperupperredhue = new Scalar(180, 255, 255);

    Mat hsv,greyscale,mask1,mask2,combinedmask,circles;

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
        Imgproc.cvtColor(mImg, hsv, Imgproc.COLOR_RGB2HSV, 3);


        Core.inRange(hsv, lowerlowerredhue, upperlowerredhue, mask1);
        Core.inRange(hsv, lowerupperredhue, upperupperredhue, mask2);

        Core.bitwise_not( mask1, mask1);
        Core.bitwise_not( mask2, mask2);
        Core.bitwise_and( mask1, mask2,combinedmask);

        hsv.setTo(new Scalar(0,0,0), combinedmask);
        Imgproc.cvtColor(hsv, mImg, Imgproc.COLOR_HSV2RGB, 4);

        List<Mat> hsv_channel = new ArrayList<Mat>();
        Core.split(hsv,hsv_channel);
        greyscale = hsv_channel.get(2);

        Imgproc.blur(greyscale, greyscale, new Size(7, 7), new Point(2, 2));
        Imgproc.HoughCircles(greyscale, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 100);

        if (circles.cols() > 0) {
                double circleVec[] = circles.get(0, 0);

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];

                Imgproc.circle(mImg, center, 3, new Scalar(255, 255, 255), 5);
                Imgproc.circle(mImg, center, radius, new Scalar(255, 255, 255), 2);
        }
        return mImg;
    }

    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStarted(int width, int height)
    {
        hsv = new Mat(height, width, CvType.CV_8UC3);
        mask1 = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        mask2 = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        combinedmask = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        greyscale = new Mat(height, width, CvType.CV_8U, new Scalar(0));
        circles = new Mat();;
    }

    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStopped()
    {
        hsv.release();
        mask1.release();
        mask2.release();
        combinedmask.release();
        greyscale.release();
        circles.release();
    }
}