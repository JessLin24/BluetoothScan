package com.example.jessica.bluetoothscan;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
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
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSION_RESPONSE = 2;
    private ArrayList<String> expectedDevices = new ArrayList<String>();
    private ArrayList<BluetoothDevice> sensorTagDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<BluetoothDevice>();
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

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
        /*expectedDevices.add("A0:E6:F8:B6:81:83");
        expectedDevices.add("A0:E6:F8:AE:19:06");
        expectedDevices.add("C4:BE:84:72:9D:81");
        expectedDevices.add("A0:E6:F8:C2:5C:01");
        expectedDevices.add("B0:B4:48:D2:E0:83");*/
        expectedDevices.add("WalkingBus");
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
            scanHandler.postDelayed(stopScan, 5000); // invoke stop scan after 5000 ms
            mBLEAdapter.startLeScan(mLeScanCallback);
        }
    };

    // Helper method to convert byte array to string for display
    public static String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");
        return hex.toString();
    }
    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] data, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(data, start, bytes, 0, length);
        return bytes;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[(bytes.length * 2) + bytes.length - 1];
        int i = 0;
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            if (i == 0) {
                hexChars[i] = hexArray[v >>> 4];
                hexChars[i + 1] = hexArray[v & 0x0F];
                i = i + 2;
            } else {
                hexChars[i] = ':';
                hexChars[i+1] = hexArray[v >>> 4];
                hexChars[i+2] = hexArray[v & 0x0F];
                i = i + 3;
            }
        }
        return new String(hexChars);
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public SparseArray<byte[]> getManufacturerData(byte[] rawData){
            if (rawData == null) {
                return null;
            }
            int currentPos = 0;
            SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
            try {
                while (currentPos < rawData.length) {
                    // length is unsigned int.
                    int length = rawData[currentPos++] & 0xFF;
                    if (length == 0) {
                        break;
                    }
                    // Note the length includes the length of the field type itself.
                    int dataLength = length - 1;
                    // fieldType is unsigned int.
                    int fieldType = rawData[currentPos++] & 0xFF;
                    switch (fieldType) {
                        case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                            // The first two bytes of the manufacturer specific data are
                            // manufacturer ids in little endian.
                            int manufacturerId = ((rawData[currentPos + 1] & 0xFF) << 8) + (rawData[currentPos] & 0xFF);
                            //byte[] manufacturerDataBytes = extractBytes(rawData, currentPos + 2, dataLength - 2);
                            byte[] manufacturerDataBytes = extractBytes(rawData, currentPos, dataLength);
                            manufacturerData.put(manufacturerId, manufacturerDataBytes);
                            break;
                        default:
                            // Just ignore, we don't handle such data type.
                            break;
                    }
                    currentPos += dataLength;
                }
                return manufacturerData;
            } catch (Exception e) {
                // As the record is invalid, ignore all the parsed results for this packet
                // and return an empty record
                return null;
            }
        }

        @SuppressLint("NewApi")
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            if(!allDevices.contains(device)){
                allDevices.add(device);
            }
            String Address = device.getAddress();
            String Name = device.getName();
            byte[] data = scanRecord;
            String dataString = "";
            try {
                SparseArray<byte[]> mftData = getManufacturerData(data);
                Log.d("DEBUG","MFT : " + mftData.toString());
                byte[] TImftData = mftData.get(13);    // 13 is the integer ID of TI
                if (TImftData != null){
                    byte[] address = new byte[6];
                    for(int i = 0; i < 6; i++){
                        address[i] = TImftData[i+2];
                    }
                    String MACAdress = bytesToHex(address);
                    Integer battery = ((int)TImftData[8]);
                    // Simply print all raw bytes
                    String decodedRecord = new String(scanRecord,"UTF-8");
                    dataString += (ByteArrayToString(scanRecord));
                    Log.e("mLeScanCallback",""+Address +" : "+Name);
                    Log.d("DEBUG","decoded data : " + ByteArrayToString(scanRecord));
                    //if(expectedDevices.contains(Address) || device.getName().equals("WalkingBus")){
                    if(expectedDevices.contains(Name)){
                        if(!sensorTagDevices.contains(device)){
                            sensorTagDevices.add(device);
                            String text = Name + ", " + Address;
                            TextView sensortag = (TextView)findViewById(R.id.sensortag);
                            sensortag.append(text);
                            sensortag.append("\nad Data: ");
                            sensortag.append(dataString);
                            sensortag.append("\nRaw Mft Data: ");
                            sensortag.append(ByteArrayToString(TImftData));
                            sensortag.append("\nMAC Address: ");
                            sensortag.append(MACAdress);
                            sensortag.append("\nbattery level (%): ");
                            sensortag.append(battery.toString());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            scanHandler.postDelayed(startScan, 1000); // start scan after 10 ms
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
