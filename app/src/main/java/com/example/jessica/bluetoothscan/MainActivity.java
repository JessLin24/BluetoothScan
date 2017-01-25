package com.example.jessica.bluetoothscan;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSION_RESPONSE = 2;
    private ArrayList<String> expectedDevices = new ArrayList<String>();
    private ArrayList<BluetoothDevice> sensorTagDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Location access not granted!");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_RESPONSE);
            }
        }
        turnonBLE();
        discoverBLEDevices();
        expectedDevices.add("A0:E6:F8:B6:81:83");
        expectedDevices.add("A0:E6:F8:AE:19:06");
        expectedDevices.add("C4:BE:84:72:9D:81");
        TextView expectedTags = (TextView)findViewById(R.id.expectedTags);
        for (String address:expectedDevices) {
            expectedTags.append(address);
            expectedTags.append("\n");
        }
    }
    private BluetoothAdapter mBLEAdapter;
    private BluetoothManager manager;
    private Handler scanHandler = new Handler();

    @SuppressLint("NewApi")
    private void turnonBLE() {
        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBLEAdapter = manager.getAdapter();
        mBLEAdapter.enable();
        Toast.makeText(getApplicationContext(), "BTLE ON Service",
                Toast.LENGTH_LONG).show();
        Log.e("BLE_Scanner", "TurnOnBLE");}

    @SuppressLint("NewApi")
    private void discoverBLEDevices() {
        startScan.run();
        Log.e("BLE_Scanner", "DiscoverBLE");
    }


    private Runnable startScan = new Runnable() {
        @Override
        public void run() {
            allDevices.clear();
            scanHandler.postDelayed(stopScan, 5000); // invoke stop scan after 1000 ms
            mBLEAdapter.startLeScan(mLeScanCallback);
        }
    };

    // Device scan callback.
    @SuppressLint("NewApi")
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @SuppressLint("NewApi")
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            if(!allDevices.contains(device)){
                allDevices.add(device);
            }
            String Address = device.getAddress();
            String Name = device.getName();
            byte[] data = scanRecord;
            //if(Name != null && (Name.equals("CC2650 SensorTag") || Name.equals("SimpleBLEPeripheral") ||  Name.equals("RUTh"))){
             if(expectedDevices.contains(Address)){
                if(!sensorTagDevices.contains(device)){
                    sensorTagDevices.add(device);
                    String text = Name + ", " + Address;
                    TextView sensortag = (TextView)findViewById(R.id.sensortag);
                    sensortag.append(text);
                    sensortag.append(data.toString());
                    //TextView expectedTags = (TextView)findViewById(R.id.expectedTags);
                    //expectedTags.col(Address);
                    //Log.e("SENSORTAG ADDRESS: ", Address);
                }
            }


            Log.e("mLeScanCallback",""+Address +" : "+Name);
        }
    };


    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            if(!allDevices.contains(sensorTagDevices)) {
                TextView textView = (TextView) findViewById(R.id.sensortag);
                textView.setText("");
                sensorTagDevices.clear();
            }
            mBLEAdapter.stopLeScan(mLeScanCallback);
            scanHandler.postDelayed(startScan, 10); // start scan after 10 ms
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}