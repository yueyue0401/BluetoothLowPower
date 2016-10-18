/*
	Permission:
			android.permission.WRITE_EXTERNAL_STORAGE
			
	Dependent Library:
			gson
			DFULibrary
			
	Service:
			DfuService -> NotificationActivity
 */

package com.example.bleconnect.device;

import java.util.UUID;

import com.example.bleconnect.BLEConnect;
import com.example.bleconnect.dfu.DfuService;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class Device8ZPF extends AbstractDevice {
	
	public final static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
	private final static UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
	
	private dfuProgressCallback callback;
	private Context context;

	// progress callback
	public interface dfuProgressCallback {
		void deviceConnectingListener(String deviceAddress);
		void DfuProcessStartingListener(String deviceAddress);
		void EnablingDfuModeListener(String deviceAddress);
		void DfuFirmwareValidatingListener(String deviceAddress);
		void DfuCompletedListener(String deviceAddress);
		void DfuAbortedListener(String deviceAddress);
		void deviceDisconnectingListener(String deviceAddress);
		void progressChangedListener(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal);
		void errorListener(String deviceAddress, int error, int errorType, String message);
	}

	/**
	 * After connected, using this method to initial class.
	 * 
	 * @param gatt
	 *            the gatt of 8ZPF
	 */
	public Device8ZPF(BluetoothGatt gatt) {
		initial(gatt);
	}

	/**
	 * using this method to register callback and use DFU method.
	 * 
	 * @param callback
	 *            the callback of DFU progress
	 */
	public Device8ZPF(dfuProgressCallback callback) {
		this.callback = callback;
	}

	// Maybe this method can be remove.
	@Override
	public void setData(BluetoothGattCharacteristic characteristic) {
		super.setData(characteristic);
		UUID thisUuid = characteristic.getUuid();
	}

	/*-------------------------- DFU ---------------------------------*/
	/**
	 * Send file(Uri) to the DFU device.
	 * 
	 * @param name
	 *            the name of DFU device
	 * @param address
	 *            the address of DFU device
	 * @param uri
	 *            the uri of file
	 * @param context
	 *            the context of lauching activity
	 */
	public void sendZipToDfu(String name, String address, Uri uri, Context context) {
		this.context = context;
		DfuServiceInitiator starter = setDfuServiceInitiator(name, address, context);
		starter.setZip(uri);
		starter.start(context, DfuService.class);
	}

	/**
	 * Send file(path) to the DFU device.
	 * 
	 * @param name
	 *            the name of DFU device
	 * @param address
	 *            the address of DFU device
	 * @param path
	 *            the path of file
	 * @param context
	 *            the context of lauching activity
	 */
	public void sendZipToDfu(String name, String address, String path, Context context) {
		DfuServiceInitiator starter = setDfuServiceInitiator(name, address, context);
		starter.setZip(path);
		starter.start(context, DfuService.class);
	}

	private DfuServiceInitiator setDfuServiceInitiator(String name, String address, Context context) {
		DfuServiceListenerHelper.registerProgressListener(context, mDfuProgressListener);
		DfuServiceInitiator starter = new DfuServiceInitiator(address).setDeviceName(name).setKeepBond(true);
		return starter;
	}

	/**
	 * Pause the progress of send file to DFU device.
	 */
	public void pauseDfuUpdate() {
		final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
		final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
		pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_PAUSE);
		manager.sendBroadcast(pauseAction);
	}

	/**
	 * Cancel the progress of send file to DFU device.
	 */
	public void cancelDfuUpdate() {
		final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
		final Intent abortAction = new Intent(DfuService.BROADCAST_ACTION);
		abortAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
		manager.sendBroadcast(abortAction);
	}

	/*------------------------------ Command ------------------------------*/
	
	public void getHR() {
		sendCommand("GetHR");
	}

	public void getScore() {
		sendCommand("GetScore");
	}

	public void getBattery() {
		sendCommand("GetBattery");
	}

	public void getFWVer() {
		sendCommand("GetFWVer");
	}
	public void getPPGIndex() {
		sendCommand("GetPPGIndex");
	}

	public void setTime() {
		sendCommand("SetTime");
	}

	public void dfuReboot() {
		sendCommand("DFUReboot");
	}

	public void errClr() {
		sendCommand("ErrClr");
	}

	/**
	 * Send the command to 8ZPF.
	 * 
	 * @param command
	 *            the command to be send
	 */
	private void sendCommand(String command) {
		BLEConnect bleConnect = BLEConnect.getInstance();
		bleConnect.sendDataToDevice(getDevice(), command);
	}

	/*------------------------------ DFU CALLBACK ------------------------------*/
	private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
		@Override
		public void onDeviceConnecting(final String deviceAddress) {
			callback.deviceConnectingListener(deviceAddress);
		}

		@Override
		public void onDfuProcessStarting(final String deviceAddress) {
			callback.DfuProcessStartingListener(deviceAddress);
		}

		@Override
		public void onEnablingDfuMode(final String deviceAddress) {
			callback.EnablingDfuModeListener(deviceAddress);
		}

		@Override
		public void onFirmwareValidating(final String deviceAddress) {
			callback.DfuFirmwareValidatingListener(deviceAddress);
		}

		@Override
		public void onDeviceDisconnecting(final String deviceAddress) {
			callback.deviceDisconnectingListener(deviceAddress);
		}

		@Override
		public void onDfuCompleted(final String deviceAddress) {
			callback.DfuCompletedListener(deviceAddress);
		}

		@Override
		public void onDfuAborted(final String deviceAddress) {
			callback.DfuAbortedListener(deviceAddress);
		}

		@Override
		public void onProgressChanged(final String deviceAddress, final int percent, final float speed,
				final float avgSpeed, final int currentPart, final int partsTotal) {
			callback.progressChangedListener(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);
		}

		@Override
		public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
			callback.errorListener(deviceAddress, error, errorType, message);
		}
	};
}
