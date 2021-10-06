//*******************************************************************
/*!
\file   BT_DeviceListActivity.java
\author Thomas Breuer
\date   07.09.2021
\brief
*/

//*******************************************************************
package com.hbrs;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

//*************************************************************************************************
public class BT_DeviceListActivity extends Activity
{
    //---------------------------------------------------------------------------------------------
    ArrayList<BluetoothDevice> listDevices = new ArrayList<>();
    BluetoothAdapter           BT_Adapter;
    Set<BluetoothDevice>       BT_PairedDevices;
    BluetoothDevice            BT_Device;

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);

        BT_Adapter       = BluetoothAdapter.getDefaultAdapter();
        BT_PairedDevices = BT_Adapter.getBondedDevices();

        for(BluetoothDevice BT_Device : BT_PairedDevices)
        {
            if( BT_Device.getName().startsWith( "Makeblock" ))
            {
                listDevices.add( BT_Device );
            }
        }

        //-----------------------------------------------------------------------------------------
        ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, listDevices)
        {
            //---------------------------------------------------------------
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                BluetoothDevice d = (BluetoothDevice) super.getItem( position );
                text1.setText(d.getName());
                return view;
            }
        };

        //-----------------------------------------------------------------------------------------
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener()
        {
            //-----------------------------------------------------------
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
            {
                BT_Device = listDevices.get(pos);

                // Create the result Intent and include the MAC address
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, BT_Device.getAddress());

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        };

        ListView lv = (ListView)findViewById(R.id.paired_devices);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener( listener );
    } // EOF onCreate()

    //---------------------------------------------------------------
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    //---------------------------------------------------------------------------------------------
    static void connect( Activity activity, int resultID_BluetoothEnable, int resultID_BluetoothGetAddr)
    {
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService( Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, resultID_BluetoothEnable);
        }
        else
        {
            Intent serverIntent = new Intent( activity, com.hbrs.BT_DeviceListActivity.class );
            activity.startActivityForResult( serverIntent, resultID_BluetoothGetAddr );
        }
    }

    //---------------------------------------------------------------
    static String getAddr(Intent data)
    {
        String addr;
        addr = data.getExtras().getString( BT_DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        return( addr );
    }
 }
