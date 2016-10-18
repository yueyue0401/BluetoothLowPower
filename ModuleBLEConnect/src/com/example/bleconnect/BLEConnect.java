/**
F  	Callback:
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
 */

package com.example.bleconnect;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.bleconnect.device.AbstractDevice;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

@SuppressLint("NewApi")
public class BLEConnect extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {
	// the unique instance of BLEConnect.this.
	private static BLEConnect uniqueInstance = new BLEConnect();

	// to set notify's client UUID.
	public final static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	// the device that scanning.
	public ArrayList<BluetoothDevice> scanAllBleDevices;
	// the type that scanning (Ex. 8ZPF device) add in DeviceCreator.
	public ArrayList<String> type;
	// the device that connected.
	public Map<String, AbstractDevice> connectedBleDevicesClass; // key:
																	// address,
																	// object:
																	// class.
	// the BluetoothGatt that connected device.
	private ArrayList<BluetoothGatt> gatt;
	// the RSSI that scanning device.
	private ArrayList<Integer> RSSI;

	// for OS version < 21
	private BluetoothAdapter adapter;
	// for OS version >= 21
	private BluetoothLeScanner bleScanner;
	private ScanCallback bleCallback;

	// for Writing Characteristic
	private BluetoothGattCharacteristic tx;

	private Context context;
	private Handler mHandler;

	// is scanning.
	private boolean isScan;
	// use the filter.
	private boolean isFilter;
	// set notify.
	private boolean isON;

	// filter device name, service
	private String[] nameFilter, serviceFilter;
	private long SCAN_PERIOD;
	private int countDisconnected;

	/**
	 * the device OS version. If SDK < 21, use old API. Old API cannot use
	 * service filter.
	 */
	private int osVersion;
	private Callback mCallback;

	public interface Callback {
		void blePowerState(boolean isON);

		void bleScanningStop();

		void bleDeviceConnected(BluetoothDevice device);

		void bleDeviceConnectingFail();

		void bleDeviceDisconnect(BluetoothDevice device);

		void readDataFromBleDevice(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

		void getNotifyFromBleDevice(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
	}

	private BLEConnect() {
		super();
		isScan = false;
	}

	/**
	 * don't use "= new BLEConnect", use "= BLEConnect.getInstance();" to avoid
	 * getting different instance.
	 * 
	 * @return the unique instance of BLEConnect.
	 */
	public static BLEConnect getInstance() {
		return uniqueInstance;
	}

	/**
	 * Initial BLE.
	 * 
	 * @param context
	 *            the context of launching activity
	 * @param callback
	 *            where to callback
	 */
	@SuppressLint("NewApi")
	public void initialBLE(Context context, Callback callback) {
		this.context = context;
		registerCallback(callback);

		if (!checkBluetoothEnable()) {
			Log.e("initialBLE", "Failure! Please enable Bluetooth and initial again.");
			mCallback.blePowerState(false);
			return;
		}

		SCAN_PERIOD = 5000;
		countDisconnected = 0;

		tx = null;
		isON = true;
		mHandler = new Handler();
		isFilter = false;

		osVersion = android.os.Build.VERSION.SDK_INT;
		if (osVersion > 21) {
			bleScanner = adapter.getBluetoothLeScanner();
		}

		gatt = new ArrayList<BluetoothGatt>();
		RSSI = new ArrayList<Integer>();
		scanAllBleDevices = new ArrayList<BluetoothDevice>();
		type = new ArrayList<String>();
		connectedBleDevicesClass = new HashMap<String, AbstractDevice>();

		if (isScan)
			stopScan();

		mCallback.blePowerState(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
	}

	private boolean checkBluetoothEnable() {
		adapter = BluetoothAdapter.getDefaultAdapter();

		if (adapter.isEnabled() != true) {
			Intent bleEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			bleEnable.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(bleEnable);

			return false;
		}
		return true;
	}

	/**
	 * Scan BLE device.
	 */
	@SuppressLint("NewApi")
	public void startingBleScan() {

		if (adapter == null || adapter.isEnabled() != true) {
			Log.e("startingBleScan", "Bluetooth isn't enable. Please open BT and initial again.");
			return;
		}

		if (osVersion >= 21) {
			bleScanner = adapter.getBluetoothLeScanner();
		}

		scanAllBleDevices = new ArrayList<BluetoothDevice>();
		RSSI = new ArrayList<Integer>();
		type = new ArrayList<String>();

		if (isScan)
			stopScan();
		startScan();

		// Timing for stopping scanning
		mHandler.postDelayed(new Runnable() { 
			@Override
			public void run() {
				stopScan();
				mCallback.bleScanningStop();
				isFilter = false;
			}
		}, SCAN_PERIOD);
	}

	/**
	 * Scan BLE device with filter.
	 * 
	 * @param Filter
	 *            device name to search for
	 */
	public void startingBleScanWithFilter(String[] nameFilter, String[] serviceFilter) {
		isFilter = true;
		this.nameFilter = nameFilter;
		this.serviceFilter = serviceFilter;

		startingBleScan();
	}

	/**
	 * Connect the device that have max RSSI.
	 */
	public void connectByMaxRSSI() {
		int tempRssi = RSSI.get(0);
		BluetoothDevice device = scanAllBleDevices.get(0);
		for (int i = 1; i < RSSI.size(); i++) {
			if (RSSI.get(i) > tempRssi) {
				tempRssi = RSSI.get(i);
				device = scanAllBleDevices.get(i);
			}
		}

		connect(device);
	}

	/**
	 * Connect the device.
	 * 
	 * @param device
	 *            the device to connect
	 */
	public void connect(BluetoothDevice device) {
		for (BluetoothGatt gatt : gatt) {
			if (gatt != null && gatt.getDevice().equals(device))
				return;
		}
		gatt.add(device.connectGatt(context, false, this));
	}

	/**
	 * Connect all devices that are scanned.
	 */
	public void connectToAllDevice() {
		for (BluetoothDevice device : scanAllBleDevices) {
			connect(device);
		}
	}

	/**
	 * Disconnect the BLE device.
	 * 
	 * @param device
	 *            the device to disconnect
	 */
	public void disconnectBleDevice(BluetoothDevice device) {
		for (int i = 0; i < gatt.size(); i++) {
			if (gatt.get(i) != null && gatt.get(i).getDevice().equals(device)) {
				disconnect(i);
				break;
			}
		}
	}

	/**
	 * Disconnect all BLE devices.
	 */
	public void disconnectAllBleDevice() {
		countDisconnected = 0;

		while (countDisconnected < gatt.size()) {
			if (gatt.get(countDisconnected) != null)
				disconnect(countDisconnected);
			countDisconnected++;
		}
	}

	/**
	 * Disconnect specific gatt.
	 * 
	 * @param i
	 *            the index of gatt to disconnect
	 */
	private void disconnect(final int i) {
		for (BluetoothGattCharacteristic characteristic : getAllCharacteristics(gatt.get(i))) {
			setNotify(gatt.get(i), characteristic, false);
		}

		gatt.get(i).disconnect();
		gatt.get(i).close();
		
		connectedBleDevicesClass.remove(gatt.get(i).getDevice().getAddress());
		this.gatt.remove(i);
		countDisconnected--;
	}

	/**
	 * Send data to all characteristics of the device.
	 * 
	 * @param device
	 *            the device to send
	 * @param data
	 *            the data to be send
	 */
	public void sendDataToDevice(BluetoothDevice device, byte[] data) {
		if (data == null || data.length == 0)
			return;

		AbstractDevice connectDevice = connectedBleDevicesClass.get(device.getAddress());
		Map<String, BluetoothGattCharacteristic> characteristics = connectDevice.getCharacteristics();

		// Send data to all characteristics.
		for (String uuid : characteristics.keySet()) {
			tx = characteristics.get(uuid);
			if (tx == null)
				return;

			tx.setValue(data);
			if (writeCharacteristic(connectDevice.getGatt(), tx))
				continue;
		}
	}

	/**
	 * Send String to all characteristics of the device.
	 * 
	 * @param device
	 *            the device to send
	 * @param string
	 *            the String to be send
	 */
	public void sendDataToDevice(BluetoothDevice device, String string) {
		sendDataToDevice(device, stringToByte(string));
	}

	/**
	 * Send data to all characteristics of all devices.
	 * 
	 * @param data
	 *            the data to be send.
	 */
	public void sendDataToAllDevice(byte[] data) {
		for (String address : connectedBleDevicesClass.keySet()) {
			sendDataToDevice((connectedBleDevicesClass.get(address)).getDevice(), data);
		}
	}

	/**
	 * Send String to all characteristics of all devices.
	 * 
	 * @param string
	 *            the String to be send
	 */
	public void sendDataToAllDevice(String string) {
		sendDataToAllDevice(stringToByte(string));
	}

	/**
	 * Convert String to byte[].
	 * 
	 * @param string
	 *            the String to be converted
	 * @return the byte[] of String
	 */
	private byte[] stringToByte(String string) {
		byte[] data = string.getBytes(Charset.forName("UTF-8"));
		return data;
	}

	private boolean writeCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
		// Determining whether there is authority to write.
		final int properties = characteristic.getProperties();
		if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE
				| BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
			return false;

		new Thread(new Runnable() {
			public void run() {
				while (true)
					if (gatt.writeCharacteristic(characteristic))
						break;
			}
		}).start();

		return true;
	}

	/**
	 * Set the length of time that scan BLE device.
	 * 
	 * @param time
	 *            microsecond
	 */
	public void setScanTimeoutInterval(float time) {
		SCAN_PERIOD = (long) time;
	}

	/**
	 * Set whether to notify.
	 * 
	 * @param isON
	 *            true if want to open notify, false otherwise
	 */
	public void setCurrentDeviceNotify(boolean isON) {
		this.isON = isON;
	}

	// private void setNotify(BluetoothGatt gatt, BluetoothGattCharacteristic
	// characteristic, boolean enabled) {
	// for (int i = 0; i < this.gatt.size(); i++) {
	// if (gatt.getDevice().equals(this.gatt.get(i).getDevice())
	// && this.gatt.get(i).setCharacteristicNotification(characteristic, true))
	// {
	//
	// BluetoothGattDescriptor descriptor =
	// characteristic.getDescriptor(CLIENT_UUID);
	//
	// // determining characteristic is indication or notification.
	// if (descriptor != null) {
	// if ((characteristic.getProperties() &
	// BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
	// descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
	// } else if ((characteristic.getProperties() &
	// BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
	// descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
	// }
	//
	//
	// writeDescriptor(this.gatt.get(i), descriptor);
	// }
	// }
	// }
	// }

	private BluetoothGattDescriptor setNotify(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
			boolean enabled) {
		for (int i = 0; i < this.gatt.size(); i++) {
			if (gatt.getDevice().equals(this.gatt.get(i).getDevice())
					&& this.gatt.get(i).setCharacteristicNotification(characteristic, enabled)) {

				BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_UUID);

				// determining characteristic is indication or notification.
				if (descriptor != null) {
					if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
						descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					} else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
						descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
					}

					return descriptor;
				}
			}
		}
		return null;
	}

	// private void writeDescriptor(final BluetoothGatt gatt, final
	// BluetoothGattDescriptor descriptor) {
	// new Thread(new Runnable() {
	// public void run() {
	// while (true)
	// if (gatt.writeDescriptor(descriptor))
	// break;
	// }
	// }).start();
	// }

	synchronized private void writeDescriptor(final BluetoothGatt gatt,
			final ArrayList<BluetoothGattDescriptor> descriptors) {

		new Thread(new Runnable() {
			public void run() {
				for (BluetoothGattDescriptor descriptor : descriptors) {
					if (descriptor == null)
						continue;
					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					gatt.writeDescriptor(descriptor);
				}
			}
		}).start();
	}

	private void readCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
		new Thread(new Runnable() {
			public void run() {
				while (true)
					if (gatt.readCharacteristic(characteristic)) {
						break;
					}
			}
		}).start();

	}

	@SuppressLint("NewApi")
	private void startScan() {
		if (osVersion < 21) {
			startScanLowVersion();
			return;
		}
		
		bleCallback = new ScanCallback() {
			@Override
			public void onBatchScanResults(List<ScanResult> results) {

			}

			@Override
			public void onScanFailed(int errorCode) {

			}

			@Override
			public void onScanResult(int CallbackType, ScanResult result) {
				if (scanAllBleDevices.contains(result.getDevice()))
					return;

				scanAllBleDevices.add(result.getDevice());
				RSSI.add(result.getRssi());

				List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();

				DeviceCreator deviceFactory = new DeviceCreator();
				type.add(deviceFactory.getType(uuids));

			}
		};

		if (!isFilter) {
			bleScanner.startScan(bleCallback);

		} else {

			List<ScanFilter> filters = new ArrayList<>();
			for (String name : nameFilter) {
				filters.add(new ScanFilter.Builder().setDeviceName(name).build());
			}
			for (String serivce : serviceFilter) {
				filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serivce)).build());
			}

			ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
			bleScanner.startScan(filters, settings, bleCallback);
		}

		isScan = true;
		isFilter = false;
	}
	
	

	@SuppressWarnings("deprecation")
	private void startScanLowVersion() {
		if (adapter != null) {
			adapter.startLeScan(this);
			isScan = true;
		}
	}

	@SuppressLint("NewApi")
	private void stopScan() {
		if (osVersion < 21) {
			stopScanLowVersion();
			return;
		}

		bleScanner.stopScan(bleCallback);
		isScan = false;
	}

	@SuppressWarnings("deprecation")
	private void stopScanLowVersion() {
		if (adapter != null) {
			adapter.stopLeScan(this);
			isScan = false;
		}
	}

	public void registerCallback(Callback callback) {
		mCallback = callback;
	}

	public void unregisterCallback(Callback callback) {
		mCallback = null;
	}

	/**
	 * put device class into Map connectedbleDevicesClass.
	 * 
	 * @param gatt
	 */
	private void newDevice(BluetoothGatt gatt) {
		DeviceCreator deviceFactory = new DeviceCreator();
		connectedBleDevicesClass.put(gatt.getDevice().getAddress(), deviceFactory.createDevice(gatt));
	}

	private List<BluetoothGattCharacteristic> getAllCharacteristics(BluetoothGatt gatt) {
		List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
		for (BluetoothGattService service : gatt.getServices()) {
			for (BluetoothGattCharacteristic characteristic : service.getCharacteristics())
				characteristics.add(characteristic);
		}
		return characteristics;
	}

	/*------------------------------ BLE CALLBACK ------------------------------*/

	// After StartScan, it will callback each searching a device.(API < 21)
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		if (!scanAllBleDevices.contains(device)) {
			if (isFilter) {
				for (String s : nameFilter) {
					if (device.getName() != null && device.getName().contains(s)) {
						scanAllBleDevices.add(device);
						RSSI.add(rssi);
					}
				}
			} else {
				scanAllBleDevices.add(device);
				RSSI.add(rssi);
			}
		}
	}

	// After connected, if device's services are discovered, it will callback.
	@Override
	synchronized public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		super.onServicesDiscovered(gatt, status);

		String Address = gatt.getDevice().getAddress();
		if (connectedBleDevicesClass.containsKey(Address))
			return;

		if (status == BluetoothGatt.GATT_FAILURE) {
			mCallback.bleDeviceConnectingFail();
			return;
		}

		newDevice(gatt);
		mCallback.bleDeviceConnected(gatt.getDevice());
		ArrayList<BluetoothGattDescriptor> descriptors = new ArrayList<BluetoothGattDescriptor>();

		for (BluetoothGattCharacteristic characteristic : getAllCharacteristics(gatt)) {
			// use readCharacteristic
			if (characteristic.getUuid().toString().contains("2a19")) // BatteryLevel's
																		// Read
																		// is
																		// Mandatory
				readCharacteristic(gatt, characteristic);

			descriptors.add(setNotify(gatt, characteristic, isON));
		}
		writeDescriptor(gatt, descriptors);
	}

	// After connection state changed will callback.
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
		super.onConnectionStateChange(gatt, status, newState);
		if (newState == BluetoothProfile.STATE_CONNECTED) {
			if (!gatt.discoverServices()) {
				// Error starting service discovery.
				mCallback.bleDeviceConnectingFail();
			} else {
				stopScan();
			}
		} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

			connectedBleDevicesClass.remove(gatt.getDevice().getAddress());
			mCallback.bleDeviceDisconnect(gatt.getDevice());

			// for (int i = 0; i < connectedBleDevicesClass.size(); i++) {
			// if (deviceName.contains(this.gatt.get(i).getDevice().getName()))
			// {
			// this.gatt.remove(i);
			// countDisconnected--;
			// }
			// }
		}
	}

	// readCharacteristic success will callback a characteristic having data.
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicRead(gatt, characteristic, status);

		AbstractDevice device = connectedBleDevicesClass.get(gatt.getDevice().getAddress());
		device.setData(characteristic);

		mCallback.readDataFromBleDevice(gatt, characteristic);
	}

	// writeCharacteristic success will callback
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicWrite(gatt, characteristic, status);

		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d("onCharacteristicWrite", "SECCESS");
		}

	}

	// callback notification.
	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		super.onCharacteristicChanged(gatt, characteristic);
		Log.d("onCharacteristicChanged", "Receive");
		String address = gatt.getDevice().getAddress();

		AbstractDevice device = connectedBleDevicesClass.get(address);
		device.setData(characteristic);

		mCallback.getNotifyFromBleDevice(gatt, characteristic);
	}
}
