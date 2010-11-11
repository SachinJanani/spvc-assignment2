package dk.itu.spvc.android.bt;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import dk.itu.android.bluetooth.BluetoothAdapter;
import dk.itu.android.bluetooth.BluetoothDevice;

public class DeviceListActivity extends Activity {

	/* reference to the button used to start the discovery phase */
	Button startDiscoveryButton;
	/* the local bluetooth adapter */
	BluetoothAdapter btadapter;
	/* view adapter for the list of devices */
	ArrayAdapter<String> devicesArrayAdapter;

	public static String EXTRA_DEVICE_ADDRESS = "dk.itu.spvc.android.bt.DeviceListActivity.EXTRA_DEVICE_ADDRESS";

	OnItemClickListener deviceClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(android.widget.AdapterView<?> av, View v,
				int arg2, long arg3) {
			// cancel the discovery, as the user already selected a device
			btadapter.cancelDiscovery();

			// get the string in the text view
			String info = ((TextView) v).getText().toString();
			// ..split it and take the address part (the second string in the
			// array)
			String addr = info.split("\n")[1];

			// create a new intent and put the selected device address
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, addr);

			// set the result and finish this activity
			setResult(RESULT_OK, intent);
			finish();
		}
	};

	public void startDiscovery(View view) {
		if (btadapter.isDiscovering()) {
			btadapter.cancelDiscovery();
		}
		btadapter.startDiscovery();
		startDiscoveryButton.setEnabled(false);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list);
		setResult(RESULT_CANCELED);
		dk.itu.android.bluetooth.BluetoothAdapter.SetContext(this);
		/* create the list view adapter */
		devicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);
		/* get a reference to the list */
		ListView devicesListView = (ListView) findViewById(R.id.DevicesListView);
		/* set the adapter */
		// TODO: following 2 lines
		// pairedListView.setOnItemClickListener(deviceClickListener);
		// newDevicesListView.setOnItemClickListener(deviceClickListener);
		devicesListView.setAdapter(devicesArrayAdapter);
		devicesListView.setOnItemClickListener(deviceClickListener);
		/* get a reference to the StartDiscoveryButton */
		startDiscoveryButton = (Button) findViewById(R.id.StartDiscoveryButton);
	}

	BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			/* do something with the intent here */
			String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					devicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
				}
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				startDiscoveryButton.setEnabled(true);
			}
		}
	};

	public void onStart() {
		super.onStart();
		btadapter = BluetoothAdapter.getDefaultAdapter();
		setup();
	}

	private void setup() {
		for (BluetoothDevice device : btadapter.getBondedDevices()) {
			devicesArrayAdapter.add(device.getName() + "\n"
					+ device.getAddress());
		}
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(discoveryReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(discoveryReceiver, filter);
	}

	public void onDestroy() {
		super.onDestroy();
		btadapter.cancelDiscovery();
		unregisterReceiver(discoveryReceiver);
	}
}