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
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;
import io.github.controlwear.virtual.joystick.android.JoystickView;

//*************************************************************************************************
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MBot";

    private MBot mbot;

    private Button up;
    private Button down;
    private Button left;
    private Button center;
    private Button right;

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

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                double anglerad = angle;
                double v_a,v_b;
                anglerad = ((anglerad + 180) % 360) - 180;
                Log.i("movement",String.format("%f",anglerad));
                v_a = strength * (45 - anglerad % 90) / 45;
                v_b = Math.min(100.0, Math.min(2 * strength + v_a, 2 * strength - v_a));
                if(anglerad < -90){
                    mbot.setDrive(-1 * (int) (v_a),-1 * (int) (v_a));
                }else if (anglerad < 0){
                    mbot.setDrive(-1 * (int) (v_a),(int) (v_b));
                }else if (anglerad < 90){
                    mbot.setDrive((int) (v_b), (int) (v_a));
                }else{
                    mbot.setDrive((int) (v_a), -1 * (int) (v_b));
                }
            }
        });

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
    //*********************************************************************************************
}
