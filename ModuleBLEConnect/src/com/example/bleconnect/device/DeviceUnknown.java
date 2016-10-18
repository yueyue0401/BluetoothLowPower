package com.example.bleconnect.device;

import android.bluetooth.BluetoothGatt;

public class DeviceUnknown extends AbstractDevice {
	public DeviceUnknown(BluetoothGatt gatt){
		initial(gatt);
	}
}
