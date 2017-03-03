# BluetoothScan
Bluetooth scan Android app for Walking Bus project.

All Walking Bus project SensorTags have the name "WalkingBus"
Look at 
public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
for examples how to extract data.

1) Extract manufacturer specific data from scanRecord
2) Use TI manufacturer ID of 0d00 or integer value 13 to get manufacturer specific data
3) Manufacturer Specific Data Format: 2 bytes manufacturer ID + 6 bytes MAC address + 1 byte battery level
