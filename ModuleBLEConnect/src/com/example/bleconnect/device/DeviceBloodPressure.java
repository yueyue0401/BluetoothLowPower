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

public class DeviceBloodPressure extends AbstractDevice {

	private final UUID BPM_CHARACTERISTIC_UUID = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb");

	private float systolicmmHg, diastolicmmHg, meanArterialPressuremmHg, pulseRate;
	private float systolicKPa, diastolicKPa, meanArterialPressureKPa;
	private Calendar calendar;

	/**
	 * After connected, using this method to initial class.
	 * 
	 * @param gatt
	 *            the gatt of FORAP30
	 */
	public DeviceBloodPressure(BluetoothGatt gatt) {
		systolicmmHg = 0;
		diastolicmmHg = 0;
		meanArterialPressuremmHg = 0;
		systolicKPa = 0;
		diastolicKPa = 0;
		meanArterialPressureKPa = 0;
		pulseRate = 0;

		calendar = null;

		initial(gatt);
	}

	@Override
	public void setData(BluetoothGattCharacteristic characteristic) {
		super.setData(characteristic);
		UUID thisUuid = characteristic.getUuid();

		if (BPM_CHARACTERISTIC_UUID.equals(thisUuid)) {
			parseBPMValue(characteristic);
		} 
	}

	/**
	 * Parse the byte of characteristic that have BPM value.
	 * 
	 * @param characteristic
	 *            the characteristic to parse
	 */
	private void parseBPMValue(BluetoothGattCharacteristic characteristic) {
		int offset = 0;

		final int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset++);
		final int unit = flags & 0x01;
		final boolean timestampPresent = (flags & 0x02) > 0;
		final boolean pulseRatePresent = (flags & 0x04) > 0;

		if (unit == 0) {
			systolicmmHg = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);
			diastolicmmHg = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset + 2);
			meanArterialPressuremmHg = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT,
					offset + 4);
		} else {
			systolicKPa = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);
			diastolicKPa = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset + 2);
			meanArterialPressureKPa = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT,
					offset + 4);
		}
		convertPressure(unit);

		offset += 6;

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
			offset += 7;
		}

		if (pulseRatePresent) {
			pulseRate = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);
			// offset += 2;
		}
	}

	/**
	 * Convert KPa to mmHg or mmHg to KPa
	 * 
	 * @param unit
	 *            the unit of BPM value
	 */
	private void convertPressure(int unit) {
		if (unit == 0) {
			systolicKPa = (float) (systolicmmHg / 7.5);
			diastolicKPa = (float) (diastolicmmHg / 7.5);
			meanArterialPressureKPa = (float) (meanArterialPressuremmHg / 7.5);
		} else {
			systolicmmHg = (float) (systolicKPa * 7.5);
			diastolicmmHg = (float) (diastolicKPa * 7.5);
			meanArterialPressuremmHg = (float) (meanArterialPressureKPa * 7.5);
		}
	}

	/**
	 * Get systolic that unit is mmHg.
	 * 
	 * @return float of systolic that unit is mmHg
	 */
	public float getSystolicmmHg() {
		return systolicmmHg;
	}

	/**
	 * Get diastolic that unit is mmHg.
	 * 
	 * @return float of diastolic that unit is mmHg
	 */
	public float getDiastolicmmHg() {
		return diastolicmmHg;
	}

	/**
	 * Get Mean Arterial Pressure that unit is mmHg.
	 * 
	 * @return float of Mean Arterial Pressure that unit is mmHg
	 */
	public float getMeanArterialPressuremmHg() {
		return meanArterialPressuremmHg;
	}

	/**
	 * Get systolic that unit is KPa.
	 * 
	 * @return float of systolic that unit is KPa
	 */
	public float getSystolicKPa() {
		return systolicKPa;
	}

	/**
	 * Get diastolic that unit is KPa.
	 * 
	 * @return float of diastolic that unit is KPa
	 */
	public float getDiastolicKPa() {
		return diastolicKPa;
	}

	/**
	 * Get Mean Arterial Pressure that unit is KPa.
	 * 
	 * @return float of Mean Arterial Pressure that unit is KPa
	 */
	public float getMeanArterialPressureKPa() {
		return meanArterialPressureKPa;
	}

	/**
	 * Get pulse rate.
	 * 
	 * @return the float of pulse rate
	 */
	public float getPulseRate() {
		return pulseRate;
	}

	/**
	 * Get calendar of measuring.
	 * 
	 * @return the calendar of measuring
	 */
	public Calendar getCalendar() {
		return calendar;
	}
}
