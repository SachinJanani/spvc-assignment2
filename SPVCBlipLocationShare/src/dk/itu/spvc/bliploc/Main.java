package dk.itu.spvc.bliploc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import dk.itu.android.bluetooth.BluetoothAdapter;
import dk.itu.android.bluetooth.BluetoothDevice;
import dk.itu.android.bluetooth.BluetoothServerSocket;
import dk.itu.android.bluetooth.BluetoothSocket;
import dk.itu.spvc.bliploc.provider.BlipLocationUtil;

public class Main extends Activity {

	/* used when requesting the bluetooth-enabling activity */
	static final int REQUEST_BLUETOOTH_ENABLE = 1;
	/* request to make the device discoverable */
	static final int REQUEST_ENABLE_DISCOVERABLE = 2;
	/* request to select a device to connect to */
	static final int REQUEST_CONNECT_DEVICE = 3;
	/* the local bluetooth adapter */
	BluetoothAdapter btadapter;
	/* view adapter for the list of devices */
	ArrayAdapter<String> devicesArrayAdapter;
	/* BlipLocation stuff */
	BlipLocation myLocation;
	BlipLocationUtil utils;
	/* the layout's items */
	Button setDiscoverable;
	/* timer, counter */
	private MyTimer timer;
	/* the server instance */
	Server server;
	/* the client instance */
	Client client;
	/* the service UUID and name */
	static final UUID EchoServiceUUID = UUID
			.fromString("419bbc68-c365-4c5e-8793-5ebff85b908c");
	static final String EchoServiceName = "SPVCBlipLocation";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		/* initialize the Timer */
		timer = new MyTimer();

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
			new Thread(server).start();
		}
	}

	private void setup() {
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
		this.server = new Server();
		startActivityForResult(new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
				REQUEST_ENABLE_DISCOVERABLE);
	}

	public void sync() {
		Log.d("KONRAD", "!!! Synchronizing...");
	}

	/* BlipNode Discovery */
	public void startDiscovery() {
		Log.i("KONRAD", "Discovering...");
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
					Log.i("KONRAD", "Name: " + name);
					if (name != null) { // && name.matches("ITU-.*")) {
						// devicesArrayAdapter.add(device.getName() + "\n" +
						// device.getAddress());
						TextView locationTextView = (TextView) findViewById(R.id.MyLocation);
						locationTextView.setText(name);
						utils.insertMyLocation(context, btadapter, name);
					}
				}
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				Log.i("KONRAD", "Finished discovery!");
			}
		}
	};

	/* SERVER */
	private class Server implements Runnable {
		boolean running = true;
		BluetoothServerSocket socket = null;

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
					final String line = bufReader.readLine().trim();
					clientSocket.getOutputStream().write(
							(">" + line + "\r\n").getBytes("UTF-8"));
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
		String message;

		public Client(String serverDeviceAddress) {
			device = btadapter.getRemoteDevice(serverDeviceAddress);
		}

		public void run() {
			try {
				BluetoothSocket socket = device
						.createRfcommSocketToServiceRecord(EchoServiceUUID);
				socket.connect();
				socket.getOutputStream().write(
						(message + "\r\n").getBytes("UTF-8"));
				socket.getOutputStream().flush();
				BufferedReader bufReader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				final String echoed = bufReader.readLine();
			} catch (IOException e) {
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
			Log.i("KONRAD", "Timer called!");
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
					if (intervalCount == 5) {
						intervalCount = 0;
						initCount++;
						sync();
					}

				}
			}, initialDelay, period);
		}
	}

}