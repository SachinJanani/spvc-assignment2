package dk.itu.spvc.bliploc;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.Toast;

public class Main extends Activity {

	/* used when requesting the bluetooth-enabling activity */
	static final int REQUEST_BLUETOOTH_ENABLE = 1;
	/* the local bluetooth adapter */
	BluetoothAdapter btadapter;
	/* view adapter for the list of devices */
	ArrayAdapter<String> devicesArrayAdapter;
	/* the layout's items */
	Button setDiscoverable;
	/* timer, counter */
	private int counter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		/* initialize variables */
		counter = 0;
		
		/* create the list view adapter */
		devicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);
		/* get a reference to the list */
		ListView devicesListView = (ListView) findViewById(R.id.DevicesListView);
		/* set the adapter */
		devicesListView.setAdapter(devicesArrayAdapter);
		/* get a reference to the StartDiscoveryButton */
		setDiscoverable = (Button) findViewById(R.id.SetDiscoverable);
	}

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

	private void setup() {
		// TODO: do we need that 'for loop'?
		for (BluetoothDevice device : btadapter.getBondedDevices()) {
			devicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		}
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(discoveryReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(discoveryReceiver, filter);
	}

	// Initialize Device Discovery
	// int delay = 30000; // delay for 30 sec.
	// int interval = 1000; // iterate every sec.
	// Timer timer = new Timer();
	// timer.scheduleAtFixedRate(new TimerTask() {

	/**
	 * BlipNode Discovery
	 */
	// TODO: this stuff goes to the Timer 
	// TODO: the Timer also checks the Counter
	public void startDiscovery(View view) {
		if (btadapter.isDiscovering()) {
			btadapter.cancelDiscovery();
		}
		btadapter.startDiscovery();
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
					String name = device.getName();
					if (name != null && name.matches("ITU-.*")) {
//						devicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
						TextView locationTextView = (TextView) findViewById(R.id.MyLocation);
						locationTextView.setText(name);
						
					}
				}
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				// TODO: add 1 to the Counter?
				counter++;
			}
		}
	};

}