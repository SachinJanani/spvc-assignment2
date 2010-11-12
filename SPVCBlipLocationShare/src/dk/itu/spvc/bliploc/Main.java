package dk.itu.spvc.bliploc;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {
	
	/** Emil Start */
	
	/* used when requesting the bluetooth-enabling activity */
	static final int REQUEST_BLUETOOTH_ENABLE = 1;
		Button startDiscoveryButton;
	/* the local bluetooth adapter */
	BluetoothAdapter btadapter;
	/* view adapter for the list of devices */
	ArrayAdapter<String> devicesArrayAdapter;
	ArrayAdapter<String> updatesAdapter;
	Button updateButton;

	/** Emil End */
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        /** Emil Start */
        
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
	
		
		
		// Initialize Device Discovery
		
//	    int delay = 30000;   // delay for 30 sec.
//	    int interval = 1000;  // iterate every sec.
//	    Timer timer = new Timer();
	    
//	    timer.scheduleAtFixedRate(new TimerTask() {

	private void setup() {
		for (BluetoothDevice device : btadapter.getBondedDevices()) {
			updatesAdapter.add(device.getName() + "\n" + device.getAddress());
		}
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(discoveryReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(discoveryReceiver, filter);
	}
	

	/**
	 * BlipNode Discovery
	 */
	public void startDiscovery(View view) {
		if (btadapter.isDiscovering()) {
			btadapter.cancelDiscovery();
		}
		btadapter.startDiscovery();
		updateButton.setEnabled(false);
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
					updatesAdapter.add(device.getName() + "\n"
							+ device.getAddress());
					String name = device.getName();
					TextView locationTextView = (TextView) findViewById(R.id.DevicesListView);
					if (name != null && name.matches("ITU-.*")) {
						locationTextView.setText(name);
					} else {
						locationTextView.setText("unknown");
					}
				}
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				updateButton.setEnabled(true);
			}
		}
	};



}