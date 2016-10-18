# SwipeListView
The ListView like ios UI, it can swipe and appear buttons.

Listener:
  			blePowerState(boolean isON);
			bleScanningStop();
			bleDeviceConnected(BluetoothDevice device);
			bleDeviceConnectingFail();
			bleDeviceDisconnect(BluetoothDevice device);
			readDataFromBleDevice(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
			getNotifyFromBleDevice(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
			
Method:	
			initBLE();
			startingBleScan();
			startingBleScanWithFilter();
			connectByMaxRSSI();
			connect(BluetoothDevice);
			connectToAllDevice();
			disconnectBleDevice(BluetoothDevice);
			disconnectAllBleDevice();
			sendDataToDevice(BluetoothDevice, byte[]);
			sendDataToDevice(BluetoothDevice, String);
			sendDataToAllDevice(byte[]);
			sendDataToAllDevice(String);
			setScanTimeoutInterval(float);
			setCurrentDeviceNotify(boolean);
			
public variable:
			ArrayList<BluetoothDevice> scanAllBleDevices	// scanned Device.
			Map<String, Object> connectedbleDevicesClass	// connected Device.
			
Permission:
			android.permission.BLUETOOTH
   			android.permission.BLUETOOTH_ADMIN
			
			