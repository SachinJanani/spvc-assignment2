package dk.itu.spvc.android.bt;

/**
 * @author sterz
 */
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent; //import android.content.Context;
import android.content.IntentFilter; //import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import dk.itu.android.bluetooth.BluetoothAdapter;
import dk.itu.android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class DeviceListActivity extends Activity {

	static final String DEBUG_TAG = "DEBUG";
	/* used when requesting the bluetooth-enabling activity */
	static final int REQUEST_BLUETOOTH_ENABLE = 1;
	/* reference to the button used to start the discovery phase */
	Button startDiscoveryButton;
	/* the local bluetooth adapter */
	BluetoothAdapter btadapter;

	/* view adapter for the list of devices */
	ArrayAdapter<String> devicesArrayAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// dk.itu.android.bluetooth.BluetoothAdapter is:
		BluetoothAdapter.SetContext(this);

		/* create the list view adapter */
		devicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);

		/* get a reference to the list */
		ListView devicesListView = (ListView) findViewById(R.id.DevicesListView);

		/* set the adapter */
		devicesListView.setAdapter(devicesArrayAdapter);

		/* get a reference to the StartDiscoveryButton */
		startDiscoveryButton = (Button) findViewById(R.id.StartDiscoveryButton);

	}

	BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				// #extract the device from the intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
					// #device is already listed
					setup();
				} else {
					devicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
				}
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				// #re-enabling startDiscovery button
				startDiscoveryButton.setEnabled(true);
			}
		}

	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		btadapter.cancelDiscovery();
		unregisterReceiver(discoveryReceiver);
	}

	@Override
	public void onStart() {
		super.onStart();
		btadapter = BluetoothAdapter.getDefaultAdapter();
		if (btadapter == null) {
			/* uh-oh.. no bluetooth module found! */
			Toast.makeText(this, "Sorry, no bluetooth module found!",
					Toast.LENGTH_SHORT);

			/* terminate the activity */
			finish();
		} else {
			if (!btadapter.isEnabled()) {
				startActivityForResult(new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE),
						REQUEST_BLUETOOTH_ENABLE);
			} else {
				setup();
			}
		}
	}

	// #i guess when a device already exists in the list, i call this method
	/**
	 * In this method we just add the already paired devices to the
	 * devicesArrayAdapter
	 */
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

	// @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (REQUEST_BLUETOOTH_ENABLE == requestCode) {
			if (RESULT_OK == resultCode) {
				setup();
			} else {
				Toast.makeText(this,
						"Cannot do anything if bluetooth is disabled :(",
						Toast.LENGTH_SHORT);
				finish();
			}
		}
	}

	/**
	 * This method will be called when the user click on the Start Discovery
	 * button.
	 * 
	 * If the system is currently discovering, we cancel the current discovery
	 * and restart it (we don’t want to lose some devices):
	 */
	public void startDiscovery(View view) {
		if (btadapter.isDiscovering()) {
			btadapter.cancelDiscovery();
		}
		btadapter.startDiscovery();
		startDiscoveryButton.setEnabled(false);

	}
}