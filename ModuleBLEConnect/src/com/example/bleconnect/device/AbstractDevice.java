package com.example.bleconnect.device;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

/**
 * Set basic data in this class and all device class need to extend this class.
 * 
 * @author 2055
 *
 */
public abstract class AbstractDevice {
	private final UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

	private int batteryLevel;
	private Map<String, BluetoothGattCharacteristic> characteristics;
	private BluetoothGatt gatt;
	private String name, address;
	private BluetoothDevice device;

	public AbstractDevice() {

	}

	public AbstractDevice(BluetoothGatt gatt) {
		initial(gatt);
	}

	/**
	 * 
	 * @param gatt
	 *            the gatt of connected device
	 */
	protected void initial(BluetoothGatt gatt) {
		this.gatt = gatt;
		name = gatt.getDevice().getName();
		address = gatt.getDevice().getAddress();
		device = gatt.getDevice();
		batteryLevel = 0;
		characteristics = new HashMap<String, BluetoothGattCharacteristic>();
		setCharacteristics(gatt);
	}

	/**
	 * Set the date that callback from device.
	 * 
	 * @param characteristic
	 *            the characteristic that callback from device and have data
	 */
	public void setData(BluetoothGattCharacteristic characteristic) {
		if (BATTERY_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid())) {
			setBatteryLevel(characteristic);
		}
	}

	/**
	 * Store all characteristic of device into map<UUID, characteristic>.
	 * 
	 * @param gatt
	 *            the gatt of connected device
	 */
	protected void setCharacteristics(BluetoothGatt gatt) {
		for (Iterator i = gatt.getServices().iterator(); i.hasNext();) {
			for (Iterator ich = ((BluetoothGattService) i.next()).getCharacteristics().iterator(); ich.hasNext();) {
				BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) ich.next();
				characteristics.put(characteristic.getUuid().toString(), characteristic);
			}
		}
	}

	/**
	 * 
	 * @param characteristic
	 *            the characteristic that callback from device and have battery
	 *            level
	 */
	protected void setBatteryLevel(BluetoothGattCharacteristic characteristic) {
		batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
	}

	/**
	 * 
	 * @return the integer of battery level
	 */
	public int getBatteryLevel() {
		return batteryLevel;
	}

	/**
	 * 
	 * @return the gatt of device
	 */
	public BluetoothGatt getGatt() {
		return gatt;
	}

	/**
	 * Get all characteristics of the device.
	 * 
	 * @return the map that have all characteristics
	 */
	public Map<String, BluetoothGattCharacteristic> getCharacteristics() {
		return characteristics;
	}

	/**
	 * 
	 * @return the String of address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * 
	 * @return the device of connected
	 */
	public BluetoothDevice getDevice() {
		return device;
	}

	/**
	 * 
	 * @return the name of the device
	 */
	public String getDeviceName() {
		return name;
	}
}
