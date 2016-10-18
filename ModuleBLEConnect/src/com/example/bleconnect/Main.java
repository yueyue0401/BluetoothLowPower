package com.example.bleconnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.bleconnect.device.DeviceBloodPressure;
import com.example.bleconnect.device.AbstractDevice;
import com.example.bleconnect.device.Device8ZPF;
import com.example.bleconnect.device.DeviceHealthTherometer;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements BLEConnect.Callback, Device8ZPF.dfuProgressCallback {

	private static final String BLE_DEVICE = "ble_device";
	private static final String DEVICE_NAME = "device_name";
	private static final String DEVICE_ADDRESS = "device_address";

	private BluetoothDevice device;
	private BLEConnect bleConnect;
	private Device8ZPF device8ZPF;
	
	private List<Map<String, Object>> list;
	private ArrayList<BluetoothDevice> connectedBleDevices;
	private String[] filter = { "FORA", "8ZPF"};
	private String DfuAddress, DfuName;
	private int listViewMode = 0; // 0: connect, 1: disconnect, 2: select DFU device
	private boolean hasInit = false;
	private int commandNumber = 0;
	
	private ScrollView sv;
	private TextView tvState;
	private EditText etTime, etCommand;
	private ListView BLEListView;
	private ProgressBar progressBar;

	@Override
	protected void onResume() {
		super.onResume();
		bleConnect = BLEConnect.getInstance();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		init();
	}

	private void init() {
		BLEListView = (ListView) findViewById(R.id.listViewBLE);
		etTime = (EditText) findViewById(R.id.EditTextTime);
		etCommand = (EditText) findViewById(R.id.EditTextCommand);
		tvState = (TextView) findViewById(R.id.textViewState);
		sv = (ScrollView) findViewById(R.id.scrollView);
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		
		connectedBleDevices = new ArrayList<BluetoothDevice>();
		tvState.setText("");
	}

	/*------------------------------ BUTTON CLICK ------------------------------*/
	public void onInitClick(View v){
		bleConnect.initialBLE(getApplicationContext(), Main.this);
		hasInit = true;
		
		int i = android.os.Build.VERSION.SDK_INT;
	}
	
	public void onClickScan(View v) {
		if(!checkInitial()) 
			return;
		
		listViewMode = 0;
		BLEListView.setAdapter(null);
		bleConnect.setScanTimeoutInterval(Float.parseFloat(etTime.getText().toString()));
		bleConnect.startingBleScan();
		showText("Scanning starting");
	}

	public void onClickScanWithFilter(View v) {
		if(!checkInitial()) 
			return;
		
		listViewMode = 0;
		BLEListView.setAdapter(null);
		bleConnect.setScanTimeoutInterval(Float.parseFloat(etTime.getText().toString()));
		bleConnect.startingBleScanWithFilter(filter, new String[]{Device8ZPF.UART_UUID.toString()});
		showText("Scanning with filter starting");
	}

	public void onClickConnectMaxRSSI(View v) {
		if(!checkInitial()) 
			return;
		
		bleConnect.connectByMaxRSSI();
	}

	public void onClickConnectAll(View v) {
		if(!checkInitial()) 
			return;
		
		bleConnect.connectToAllDevice();
		showText("ConnectAll");
	}

	public void onClickSend(View v) {
		if(!checkInitial()) 
			return;
		
		if (!search8ZPF())
			return;
		String command = etCommand.getText().toString();
		bleConnect.sendDataToDevice(device8ZPF.getDevice(), command);
	}
	
	public void onClickSendAll(View v) {
		if(!checkInitial()) 
			return;
		
		bleConnect.sendDataToAllDevice(etCommand.getText().toString());
	}

	public void onClickDisconnect(View v) {
		if(!checkInitial()) 
			return;
		
		listViewMode = 1;
		setListView(connectedBleDevices);
	}

	public void onClickDisconnectAll(View v) {
		if(!checkInitial()) 
			return;
		
		if (connectedBleDevices.size() == 0) {
			showText("Not connect to any device.");
			return;
		}
		bleConnect.disconnectAllBleDevice();
		showText("DisconnectAll");
	}

	public void onClickNotify(View v) {
		showText("setNotify");
	}

	public void onClickCommand(View v) {
		if(!checkInitial()) 
			return;
		
		if (!search8ZPF())
			return;

		// device8ZPF.dfuReboot();

		commandNumber++;

		switch (commandNumber) {
		case 1:
			showText("Send: GetHR");
			device8ZPF.getHR();
			break;

		case 2:
			showText("Send: GetScore");
			device8ZPF.getScore();
			break;

		case 3:
			showText("Send: GetBattery");
			device8ZPF.getBattery();
			break;

		case 4:
			showText("Send: GetFWVer");
			device8ZPF.getFWVer();
			break;

		case 5:
			showText("Send: GetPPGIndex");
			device8ZPF.getPPGIndex();
			commandNumber = 0;
			break;
		}
	}

	public void onClickErrClr(View v) {
		if(!checkInitial()) 
			return;
		
		device8ZPF.errClr();
		showText("Send: ErrClr");
	}

	public void onUploadClick(View v) {
		if(!checkInitial()) 
			return;
		
		Device8ZPF Dfu8ZPF = new Device8ZPF(this);
		if (DfuAddress == null || DfuName == null) {
			Toast.makeText(getApplicationContext(), "Unchoose DFU Device", Toast.LENGTH_LONG).show();
			;
			return;
		}

		Dfu8ZPF.sendZipToDfu(DfuName, DfuAddress, "/storage/emulated/0/app_image.zip", getApplicationContext());
	}

	public void onSelcetDFUClick(View v) {
		if(!checkInitial()) 
			return;
		
		bleConnect.setScanTimeoutInterval(Float.parseFloat(etTime.getText().toString()));
		bleConnect.startingBleScan();
		listViewMode = 2;
	}
	
	private boolean checkInitial(){
		if(!hasInit) {
			Toast.makeText(getApplicationContext(), "uninitial BLE.", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	// search the connected 8ZPF device class.
	private boolean search8ZPF() {
		if (bleConnect.connectedBleDevicesClass == null)
			return false;

		for (String address : bleConnect.connectedBleDevicesClass.keySet()) {
			AbstractDevice device = (AbstractDevice) bleConnect.connectedBleDevicesClass.get(address);
			if (device.getDeviceName().contains("8ZPF")) {
				device8ZPF = (Device8ZPF) device;
				return true;
			}
		}
		return false;
	}

	public void setListView(ArrayList<BluetoothDevice> devices) {
		list = new ArrayList<Map<String, Object>>();
//		if(bleConnect.type.size()==0) return;
		
		for (int i = 0; i < devices.size(); i++) {
			Map<String, Object> item = new HashMap<String, Object>();
			String deviceName = devices.get(i).getName();
			String deviceAddress = devices.get(i).getAddress();

			item.put(BLE_DEVICE, devices.get(i));
			item.put(DEVICE_NAME, deviceName);
//			item.put(DEVICE_ADDRESS, bleConnect.type.get(i)); // isn't address, is device's type
			list.add(item);
		}

		ListAdapter adapter = new SimpleAdapter(Main.this, list, R.layout.listview_item,
				new String[] { DEVICE_NAME}, new int[] { R.id.textViewName });

		BLEListView.setAdapter(adapter);

		BLEListView.setOnItemClickListener(BLEListItemClickListener);
	}

	AdapterView.OnItemClickListener BLEListItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

			device = (BluetoothDevice) list.get(position).get(BLE_DEVICE);
			switch (listViewMode) {
			case 0:
				bleConnect.connect(device);
				break;
			case 1:
				bleConnect.disconnectBleDevice(device);
				break;
			case 2:
				DfuAddress = device.getAddress();
				DfuName = device.getName();
				showText("Select DFU Device: " + DfuName + "\n Address: " + DfuAddress);
				break;
			}

		}
	};

	private void showText(final String s) {
		runOnUiThread(new Runnable() {
			public void run() {
				tvState.append(s + "\n");
				sv.fullScroll(View.FOCUS_DOWN);
			}
		});
	}

	/*------------------------------ Listener ------------------------------*/

	@Override
	public void bleDeviceConnected(final BluetoothDevice device) {
		connectedBleDevices.add(device);
		showText("Connected: " + device.getName());
	}

	@Override
	public void bleDeviceConnectingFail() {
		showText("Connect fail");
	}

	@Override
	public void bleDeviceDisconnect(final BluetoothDevice device) {
		runOnUiThread(new Runnable() {
			public void run() {
				showText("Disconnect: " + device.getName());
				connectedBleDevices.remove(device);
				setListView(connectedBleDevices);
			}
		});
	}

	@Override
	public void readDataFromBleDevice(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		AbstractDevice connectDevice;
		
		if (characteristic.getUuid().toString().contains("2a19")) {
			connectDevice = bleConnect.connectedBleDevicesClass.get(gatt.getDevice().getAddress());
			showText("BatteryLevel: " + connectDevice.getBatteryLevel() + "%");
		}
	}

	@Override
	public void getNotifyFromBleDevice(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		String address = gatt.getDevice().getAddress();
		UUID Uuid = characteristic.getUuid();
		AbstractDevice connectDevice;
		
		if (Uuid.toString().contains("2a35")) {
			connectDevice = bleConnect.connectedBleDevicesClass.get(address);
			showText(((DeviceBloodPressure) connectDevice).getSystolicmmHg() + "mmHg, " + ((DeviceBloodPressure) connectDevice).getDiastolicmmHg() + "mmHg, "
					+ ((DeviceBloodPressure) connectDevice).getMeanArterialPressuremmHg() + "mmHg");
			
		}else if (Uuid.toString().contains("2a1c")) {
			connectDevice = bleConnect.connectedBleDevicesClass.get(address);
			showText(((DeviceHealthTherometer) connectDevice).getHealthThermometerCelsius() + "�J");
			
		}else if (gatt.getDevice().getName().contains("8ZPF")) {
			showText("Receive: " + characteristic.getStringValue(0));
		} else {
			Log.e("getNotifyFromBleDevice", "Unknown characteristic.");
		}
	}

	@Override
	public void blePowerState(boolean isON) {
		showText("blePowerState: " + isON);
	}

	@Override
	public void bleScanningStop() {
		showText("bleScanningStop");
		setListView(bleConnect.scanAllBleDevices);
	}

	@Override
	public void deviceConnectingListener(String deviceAddress) {
		showText("DFU: Connecting.");
		progressBar.setIndeterminate(true);
	}

	@Override
	public void DfuProcessStartingListener(String deviceAddress) {
		showText("DFU: Starting.");
		progressBar.setIndeterminate(true);
	}

	@Override
	public void EnablingDfuModeListener(String deviceAddress) {
		showText("DFU: Enable Dfu Mode.");
	}

	@Override
	public void DfuFirmwareValidatingListener(String deviceAddress) {
		showText("DFU: Firmware validating.");
	}

	@Override
	public void DfuCompletedListener(String deviceAddress) {
		showText("DFU: Completed.");
		progressBar.setProgress(0);
		DfuName = null;
		DfuAddress = null;
	}

	@Override
	public void DfuAbortedListener(String deviceAddress) {
		showText("DFU: Aborted.");
		progressBar.setProgress(0);
	}

	@Override
	public void deviceDisconnectingListener(String deviceAddress) {
		progressBar.setProgress(100);
		showText("DFU: Disconnecting");
	}

	@Override
	public void progressChangedListener(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart,
			int partsTotal) {
		if (percent == 1)
			showText("DFU: Updating");

		progressBar.setIndeterminate(false);
		progressBar.setProgress(percent);
	}

	@Override
	public void errorListener(String deviceAddress, int error, int errorType, String message) {
		showText("DFU: Error-" + message);
	}

}
