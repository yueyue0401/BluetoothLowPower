package com.example.bleconnect;

import java.util.List;
import java.util.UUID;

import com.example.bleconnect.device.AbstractDevice;
import com.example.bleconnect.device.Device8ZPF;
import com.example.bleconnect.device.DeviceBloodPressure;
import com.example.bleconnect.device.DeviceHealthTherometer;
import com.example.bleconnect.device.DeviceUnknown;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;
import android.util.Log;

/**
 * This class is responsible for something will be expanded and changed.
 * 
 * @author 2055
 *
 */
public class DeviceCreator {

	// Maybe these can be rewrite to enum.
	private final String UNKNOWN_DEVICE = "unknown device";
	private final String BLOOD_PRESURE_DEIVCE = "Blood Pressure Device";
	private final String THERMOMETER_DEVICE = "Health Thermometer Device";
	private final String DEVICE_8ZPF = "8ZPF Device";

	public DeviceCreator() {

	}

	/**
	 * get instance
	 * @param gatt
	 * @return the class that be Instantiated
	 */
	public AbstractDevice createDevice(BluetoothGatt gatt) {
		AbstractDevice connectDevice;

		for (BluetoothGattService service : gatt.getServices()) {
			if (getDeviceType(service.getUuid()) == null)
				continue;

			switch (getDeviceType(service.getUuid())) {
			case BLOOD_PRESURE_DEIVCE:
				connectDevice = new DeviceBloodPressure(gatt);
				return connectDevice;
				
			case THERMOMETER_DEVICE:
				connectDevice = new DeviceHealthTherometer(gatt);
				return connectDevice;
				
			case DEVICE_8ZPF:
				connectDevice = new Device8ZPF(gatt);
				return connectDevice;
				
			case UNKNOWN_DEVICE:				
			}
		}
		return connectDevice = new DeviceUnknown(gatt);
	}

	public String getType(List<ParcelUuid> uuids) {
		if (uuids != null)
			for (ParcelUuid uuid : uuids) {
				if(getDeviceType(uuid.getUuid()) != null)
					return getDeviceType(uuid.getUuid());
			}
			
		return UNKNOWN_DEVICE;
	}

	private String getDeviceType(UUID uuid) {
		if (uuid.toString().contains("1810")) {
			return BLOOD_PRESURE_DEIVCE;
		} else if (uuid.toString().contains("1809")) {
			return THERMOMETER_DEVICE;
		} else if (uuid.toString().contains("6e40")) {
			return DEVICE_8ZPF;
		}
		return null;
	}

}
