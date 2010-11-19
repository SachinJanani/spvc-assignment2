package dk.itu.spvc.bliploc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import dk.itu.android.bluetooth.BluetoothAdapter;
import dk.itu.android.bluetooth.BluetoothDevice;
import dk.itu.android.bluetooth.BluetoothServerSocket;
import dk.itu.android.bluetooth.BluetoothSocket;
import dk.itu.spvc.bliploc.provider.BlipLocationUtil;
import dk.itu.spvc.bliploc.provider.BlipLocationUtil.BlipLocationDO;

public class Main extends Activity {

	static final String TAG = "APP";

	/* used when requesting the bluetooth-enabling activity */
	static final int REQUEST_BLUETOOTH_ENABLE = 1;
	/* request to make the device discoverable */
	static final int REQUEST_ENABLE_DISCOVERABLE = 2;
	/* request to select a device to connect to */
	static final int REQUEST_CONNECT_DEVICE = 3;
	/* the local bluetooth adapter */
	BluetoothAdapter btadapter;
	/* BlipLocation stuff */
	String myLocation;
	BlipLocationUtil utils;
	SimpleCursorAdapter scadapter;
	/* the layout's items */
	Button setDiscoverable;
	/* timer, counter */
	private MyTimer timer;
	/* the server instance */
	Server server;
	/* the client instance */
	Client client;
	/* view adapter for the list of devices */
	ArrayList<String> devices;
	/* the service UUID and name */
	static final UUID EchoServiceUUID = UUID
			.fromString("419bbc68-c365-4c5e-8793-5ebff85b908c");
	static final String EchoServiceName = "SPVCBlipLocation";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		dk.itu.android.bluetooth.BluetoothAdapter.SetContext(this);

		/* initialize vars */
		timer = new MyTimer();
		utils = new BlipLocationUtil();
		devices = new ArrayList<String>();
		/* get a reference to the list */
		ListView devicesListView = (ListView) findViewById(R.id.DevicesListView);
		/* set the adapter */
		devicesListView.setAdapter(utils.standardListAdapter(this));
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

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (REQUEST_BLUETOOTH_ENABLE == requestCode) {
			if (RESULT_OK == resultCode) {
				setup();
			} else {
				Toast.makeText(this,
						"Cannot do anything if bluetooth is disabled :(",
						Toast.LENGTH_SHORT);
				finish();
			}
		} else if (REQUEST_ENABLE_DISCOVERABLE == requestCode) {
			setDiscoverable.setEnabled(false);
			new Thread(server).start();
		}
	}

	private void setup() {
		fakeLocation("ITU-4D");
		try {
			timer.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(discoveryReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(discoveryReceiver, filter);
	}

	public void setDiscoverable(View view) {
		this.server = new Server(getApplicationContext());
		startActivityForResult(new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
				REQUEST_ENABLE_DISCOVERABLE);
	}

	public void sync() {
		Log.d(TAG, "Synchronizing...");
		synchronized (devices) {
			for (String addr : devices) {
				this.client = new Client(addr, getApplicationContext());
				new Thread(client).start();
			}
		}
	}

	/* BlipNode Discovery */
	public void startDiscovery() {
		Log.i(TAG, "Discovering...");
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
					if (name != null) {
						if (name.matches("ITU-.*")) {
							Log.v(TAG, "Found a location!");
							TextView locationTextView = (TextView) findViewById(R.id.MyLocation);
							locationTextView.setText(name);
							utils.insertMyLocation(context, btadapter, name);
							myLocation = name;
						} else {
							Log.v(TAG, "Found a phone!");
							synchronized (devices) {
								devices.add(device.getAddr());
							}
						}
					} else {
						Log.v(TAG, "Found a phone!");
						name = "unknown";
						synchronized (devices) {
							devices.add(device.getAddr());
						}
					}
				}
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				Log.i(TAG, "Finished discovery!");
			}
		}
	};

	/* SERVER */
	private class Server implements Runnable {
		boolean running = true;
		BluetoothServerSocket socket = null;
		Collection<BlipLocationDO> db;
		String message;
		Context context;

		public Server(Context context) {
			this.context = context;
		}

		public void run() {
			try {
				socket = btadapter.listenUsingRfcommWithServiceRecord(
						EchoServiceName, EchoServiceUUID);
			} catch (IOException e) {
				running = false;
			}
			while (running) {
				BluetoothSocket clientSocket = null;
				try {
					clientSocket = socket.accept();
					BufferedReader bufReader = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
					String line;
					String[] temp;
					while (!(line = bufReader.readLine().trim()).equals("")) {
						temp = line.split("\\|");
						Log.i(TAG, "Line received by the server: " + line);
						utils.insertNewLocation(context, temp[1], temp[2], temp[0], Long.parseLong(temp[3]));
					}
					Log.i(TAG, "Finished receiving");
					db = utils.loadAll(context);
					for (BlipLocationDO bl : db) {
						message = getMessage(bl);
						Log.i(TAG, "Sending back: " + message);
						clientSocket.getOutputStream().write(
								(message + "\r\n").getBytes("UTF-8"));
					}
					clientSocket.getOutputStream().write(("\r\n").getBytes("UTF-8"));
					clientSocket.getOutputStream().flush();
				} catch (Exception e) {
					Log.e("Server", "Exception in server loop", e);
				} finally {
					if (clientSocket != null) {
						try {
							clientSocket.close();
						} catch (Exception ignored) {
							ignored.printStackTrace();
						}
					}
				}
			}
		}
	}

	/* CLIENT */
	private class Client implements Runnable {
		BluetoothDevice device;
		BluetoothSocket socket;
		Collection<BlipLocationDO> db;
		String message;
		Context context;

		public Client(String addr, Context context) {
			device = btadapter.getRemoteDevice(addr);
			this.context = context;
		}

		public void run() {
			try {
				BluetoothSocket socket = device
						.createRfcommSocketToServiceRecord(EchoServiceUUID);
				socket.connect();
				db = utils.loadAll(context);
				for (BlipLocationDO bl : db) {
					message = getMessage(bl);
					Log.i(TAG, "Sending: " + message);
					socket.getOutputStream().write(
							(message + "\r\n").getBytes("UTF-8"));
				}
				socket.getOutputStream().write(("\r\n").getBytes("UTF-8"));
				socket.getOutputStream().flush();
				BufferedReader bufReader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				String line;
				String[] temp;
				while (!(line = bufReader.readLine()).equals("")) {
					Log.i(TAG, "Received back from the server: " + line);
					temp = line.split("|");
					utils.insertNewLocation(context, temp[1], temp[2], temp[0], Long.parseLong(temp[3]));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

	/* The Timer */
	private class MyTimer {

		int intervalCount = 0;
		int initCount = 1;

		private void execute() throws Exception {
			Log.w(TAG, "Timer called!");
			int initialDelay = 1000; // start after 1 second
			int period = 10000; // repeat every 30 seconds
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					// Increment the intervalCounter and print the count
					intervalCount++;
					// Run the Update Procedure
					startDiscovery();
					// If the intervalCount has reached five, then run method
					// "sync()"
					// and increment the initCount.
					if (intervalCount == 1) { // 5) {
						intervalCount = 0;
						initCount++;
						sync();
					}

				}
			}, initialDelay, period);
		}
	}

	private String getMessage(BlipLocationDO location) {
		return location.getLocation() + "|" + location.getBtaddr() + "|"
				+ location.getName() + "|" + location.getTimestamp();
	}
	
	private void fakeLocation(String location) {
		utils.insertMyLocation(getApplicationContext(), btadapter, location);
		TextView locationTextView = (TextView) findViewById(R.id.MyLocation);
		locationTextView.setText(location);
	}

}