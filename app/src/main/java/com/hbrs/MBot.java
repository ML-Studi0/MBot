//*******************************************************************
/*!
\file   MBot.java
\author Thomas Breuer
\date   07.09.2021
\brief
*/

//*******************************************************************
package com.hbrs;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.lang.Thread.NORM_PRIORITY;

//*************************************************************************************************
/**
 * Bluetooth interface to MBot (Makeblock ME Auriga)
 */
public class MBot  implements Runnable
{
    //*********************************************************************************************
    private static final String TAG = "MBot";

    private BluetoothGatt    bluetoothGatt;

    private final BlockingQueue<byte[]> queue;

    private int powerLeft =0;
    private int powerRight = 0;
    private boolean isDriveChanged = true;
    private boolean isConnected = false;
    private boolean isAcknowledged = true;

    private java.lang.Thread mainThread;

    //*********************************************************************************************
    //---------------------------------------------------------------------------------------------
    /**
     * Constructor
     */
    public MBot()
    {
        queue = new ArrayBlockingQueue<byte[]>( 100);

        mainThread = new Thread(this);
        if( mainThread.getState() == Thread.State.NEW )
        {
            mainThread.start();
            mainThread.setPriority( NORM_PRIORITY );
        }
    }

    //---------------------------------------------------------------------------------------------
    /**
     * Connects to a Bluetooth device
     *
     * @param context Context used for bluetooth callbacks
     * @param addr string formated MAC address of the bluetooth device
     */
    public void connect(Context context, String addr )
    {
        if( addr.length() > 0 )
        {
            if( bluetoothGatt != null )
            {
                bluetoothGatt.close();
            }
            bluetoothGatt = null;

            BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
                    addr );
            if( bluetoothDevice != null )
            {
                bluetoothDevice.connectGatt( context, false, bluetoothGattCallback );
            }
        }
    }

    //---------------------------------------------------------------------------------------------
    /**
     * Sets the power of the motors at port M1 and M2
     *
     * @param  powerLeft  Power of the left motor (port M1)
     * @param  powerRight  Power of the right motor (port M2)
     */
    public void setDrive(int powerLeft, int powerRight)
    {
        this.powerLeft      = powerLeft;
        this.powerRight     = powerRight;
        isDriveChanged = true;
    }

    //---------------------------------------------------------------------------------------------
    /**
     * Sets the color (RGB) of the on board LED panel
     * @param id    Identifier of a dedicated LED (1,...,12). Use 0 to set all LEDs
     * @param red   Red (0,...,255)
     * @param green Green (0,...,255)
     * @param blue  Blue (0,...,255)
     */
    public void setLight(int id, int red, int green, int blue)
    {
        byte[] data = new byte[12];

        data[ 0] = (byte)0xff;
        data[ 1] = (byte)0x55;
        data[ 2] = (byte)0x09;
        data[ 3] = (byte)0x00;
        data[ 4] = (byte)0x02;
        data[ 5] = (byte)0x08;
        data[ 6] = (byte)0x00;
        data[ 7] = (byte)0x02;
        data[ 8] = (byte)id;
        data[ 9] = (byte)red;
        data[10] = (byte)green;
        data[11] = (byte)blue;

        queue.offer( data );
    }

    //*********************************************************************************************
    //---------------------------------------------------------------------------------------------
    @Override
    public void run()
    {
        while( true )
        {
            if( isDriveChanged )
            {
                isDriveChanged = false;
                sendDrive();
            }

            byte[] value = queue.poll();
            if( value != null)
            {
                send(value);
            }
        }
    }

    //---------------------------------------------------------------------------------------------
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange( gatt, status, newState );
            Log.i( TAG, "Connect " + status + " " + newState );
            if( status == BluetoothGatt.GATT_SUCCESS )
            {
                if( newState == BluetoothGatt.STATE_CONNECTED )
                {
                    bluetoothGatt = gatt;
                    bluetoothGatt.discoverServices();
                    return;
                }
            }
            if( bluetoothGatt != null )
            {
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
            return;
        }

        //---------------------------------------------------------------------------------------------
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            super.onServicesDiscovered( gatt, status );

            List<BluetoothGattService> lstService = gatt.getServices();
            for( BluetoothGattService aService : lstService )
            {
                Log.i( TAG, "SERVICE " + aService.getUuid() );
                List<BluetoothGattCharacteristic> lstChar = aService.getCharacteristics();
                for( BluetoothGattCharacteristic aChar : lstChar )
                {
                    Log.i( TAG, "  CHAR " + aChar.getUuid() + " prop " + aChar.getProperties() + " perm " + aChar.getPermissions() );

                    if( (aChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 )
                    {
                        List<BluetoothGattDescriptor> lstDesc = aChar.getDescriptors();
                        for( BluetoothGattDescriptor aDesc : lstDesc )
                        {
//                            Log.i( TAG, "     DESC " + aDesc.getValue() + " " + aDesc.describeContents() );
                            aDesc.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );
                            gatt.writeDescriptor( aDesc );
                        }
                        gatt.setCharacteristicNotification( aChar, true );
                    }
                }
            }
            isConnected = true;
        }

        //---------------------------------------------------------------------------------------------
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic cha, int status)
        {
            super.onCharacteristicRead( gatt, cha, status );
            if( status != BluetoothGatt.GATT_SUCCESS )
            {
                Log.i( TAG, " READ no success " + status );
            }
        }

        //---------------------------------------------------------------------------------------------
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic cha, int status)
        {
            super.onCharacteristicWrite( gatt, cha, status );

            if( status != BluetoothGatt.GATT_SUCCESS )
            {
                Log.i( TAG, " WRITE no success " + status );
            }
        }

        //---------------------------------------------------------------------------------------------
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic cha)
        {
            super.onCharacteristicChanged( gatt, cha );
            //Log.i( TAG, " CHANGED " + cha.getValue().length );
            isAcknowledged = true;
        }
    };

    //---------------------------------------------------------------------------------------------
    /*
    public void testRead()
    {
        if(bluetoothGatt !=null)
        {
            BluetoothGattService        lService = bluetoothGatt.getService  (
                    UUID.fromString( "0000ffe4-0000-1000-8000-00805f9b34fb"));
            // ffec: SW-Version        BluetoothGattCharacteristic lChar    = lService.getCharacteristic(UUID.fromString("0000ffec-0000-1000-8000-00805f9b34fb"));
            BluetoothGattCharacteristic lChar    = lService.getCharacteristic(UUID.fromString("0000ffe8-0000-1000-8000-00805f9b34fb"));

            bluetoothGatt.readCharacteristic(lChar);
        }
    }
     */

    //---------------------------------------------------------------------------------------------
    private void send(byte[] payload)
    {
        if(isConnected && bluetoothGatt !=null)
        {
            BluetoothGattService        lService = bluetoothGatt.getService  (UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
            BluetoothGattCharacteristic lChar    = lService.getCharacteristic(UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb"));

            lChar.setValue(payload);
            lChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            bluetoothGatt.writeCharacteristic(lChar);
            isAcknowledged = false;
            int cnt =0;
            while( !isAcknowledged && cnt < 50)
            {
                cnt++;
                try
                {
                    Thread.sleep(1);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

    //---------------------------------------------------------------------------------------------
    private void sendDrive()
    {
        byte[] data = new byte[10];

        data[0] = (byte)0xff;
        data[1] = (byte)0x55;
        data[2] = (byte)0x07;
        data[3] = (byte)0x00;
        data[4] = (byte)0x02;
        data[5] = (byte)0x05;
        data[6] = (byte)(((short)powerLeft    )&0xff);
        data[7] = (byte)(((short)powerLeft >>8)&0xff);
        data[8] = (byte)(((short)powerRight   )&0xff);
        data[9] = (byte)(((short)powerRight>>8)&0xff);

        send(data);
    }
};
