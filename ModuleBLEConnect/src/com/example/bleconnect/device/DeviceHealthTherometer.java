package com.example.bleconnect.device;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public class DeviceHealthTherometer extends AbstractDevice {
	private static final UUID HT_MEASUREMENT_CHARACTERISTIC_UUID = UUID
			.fromString("00002A1C-0000-1000-8000-00805f9b34fb");

	private float healthThermometerCelsius; // 
	private float healthThermometerFahrenheit;// 
	private Calendar calendar;

	/**
	 * After connected, using this method to initial class.
	 * 
	 * @param gatt
	 *            the gatt of FORAIR20
	 */
	public DeviceHealthTherometer(BluetoothGatt gatt) {
		healthThermometerCelsius = 0;
		healthThermometerFahrenheit = 0;

		calendar = null;

		initial(gatt);
	}

	@Override
	public void setData(BluetoothGattCharacteristic characteristic) {
		super.setData(characteristic);
		UUID thisUuid = characteristic.getUuid();
		
		if (HT_MEASUREMENT_CHARACTERISTIC_UUID.equals(thisUuid)) {
			parseHealthThermometer(characteristic);
		} 
	}

	/**
	 * Parse the byte of characteristic that have health thermometer.
	 * 
	 * @param characteristic
	 *            the characteristic to parse
	 */
	private void parseHealthThermometer(BluetoothGattCharacteristic characteristic) {
		int offset = 0;
		final int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset++);
		final int unit = flags & 0x01;
		final boolean timestampPresent = (flags & 0x02) > 0;

		if (unit == 0) {
			healthThermometerCelsius = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
			healthThermometerFahrenheit = (float) (healthThermometerCelsius * 1.8 + 32);
		} else {
			healthThermometerFahrenheit = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
			healthThermometerCelsius = (float) ((healthThermometerFahrenheit - 32) / 1.8);
		}

		if (timestampPresent) {
			final Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.YEAR, characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset));
			calendar.set(Calendar.MONTH,
					characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2) - 1); // months
																											// are
																											// 1-based
			calendar.set(Calendar.DAY_OF_MONTH,
					characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3));
			calendar.set(Calendar.HOUR_OF_DAY,
					characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 4));
			calendar.set(Calendar.MINUTE,
					characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 5));
			calendar.set(Calendar.SECOND,
					characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 6));
		}
	}

	/**
	 * Get the health thermometer Celsius.
	 * 
	 * @return float that health thermometer Celsius
	 */
	public float getHealthThermometerCelsius() {
		return healthThermometerCelsius;
	}

	/**
	 * Get the health thermometer Fahrenheit.
	 * 
	 * @return float that health thermometer Fahrenheit
	 */
	public float getHealthThermometerFahrenheit() {
		return healthThermometerFahrenheit;
	}

	/**
	 * Get the calendar of measuring.
	 * 
	 * @return the calendar of measuring
	 */
	public Calendar getCalendar() {
		return calendar;
	}

}
