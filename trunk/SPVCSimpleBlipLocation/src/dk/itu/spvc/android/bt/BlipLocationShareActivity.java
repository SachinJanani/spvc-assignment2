package dk.itu.spvc.android.bt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class BlipLocationShareActivity extends Activity {

	/* used when requesting the bluetooth-enabling activity */
	static final int REQUEST_BLUETOOTH_ENABLE = 1;
	/* request to make the device discoverable */
	static final int REQUEST_ENABLE_DISCOVERABLE = 2;
	/* request to select a device to connect to */
	static final int REQUEST_CONNECT_DEVICE = 3;

	/* the service UUID and name */
	static final UUID EchoServiceUUID = UUID
			.fromString("4db2d0b3-8193-42f7-8cc9-f00d8a060a97");
	static final String EchoServiceName = "Spvc.Echo";

	/* the local bluetooth adapter */
	BluetoothAdapter btadapter;

	/* identify if the user selected the device to be a server or client */
	boolean isServer;

	/* reference to the ResponseTextView */
	TextView responseTextView;

	/* the server instance */
	Server server;
	/* the client instance */
	Client client;

	String currentLocation;
	ArrayAdapter<String> updatesAdapter;
	Button updateButton;

	private void setServer(boolean server) {
		this.isServer = server;
		findViewById(R.id.StartServerButton).setVisibility(View.GONE);
		findViewById(R.id.SelectDeviceButton).setVisibility(View.GONE);
		if (server) {
			findViewById(R.id.StopServerButton).setVisibility(View.VISIBLE);
		}
	}

	public void startServer(View view) {
		setServer(true);
		this.server = new Server();
		startActivityForResult(new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
				REQUEST_ENABLE_DISCOVERABLE);
	}

	public void stopServer(View view) {
		if (server != null)
			server.stop();
	}

	public void selectServerDevice(View view) {
		startActivityForResult(new Intent(this, DeviceListActivity.class),
				REQUEST_CONNECT_DEVICE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		dk.itu.android.bluetooth.BluetoothAdapter.SetContext(this);
		updateButton = (Button) findViewById(R.id.UpdateButton);
		updatesAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		ListView lv = (ListView) findViewById(R.id.UpdatesListView);
		lv.setAdapter(updatesAdapter);
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
		} else if (REQUEST_CONNECT_DEVICE == requestCode) {
			if (RESULT_OK == resultCode) {
				findViewById(R.id.SelectDeviceButton).setVisibility(View.GONE);
				String deviceAddress = data
						.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				currentLocation = data
						.getStringExtra(DeviceListActivity.EXTRA_LAST_LOCATION);
				((TextView) findViewById(R.id.MyBlipLocationTextView))
						.setText(currentLocation);
				setServer(false);
				Client client = new Client(deviceAddress);
				new Thread(client).start();
			}
		} else if (REQUEST_ENABLE_DISCOVERABLE == requestCode) {
			new Thread(server).start();
		}
	}

	private void setup() {
		for (BluetoothDevice device : btadapter.getBondedDevices()) {
			updatesAdapter.add(device.getName() + "\n" + device.getAddress());
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

	/* A class to handle the service server communication */
	private class Server implements Runnable {
		boolean running = true;
		BluetoothServerSocket socket = null;

		public void stop() {
			running = false;
			if (socket != null) {
				try {
					socket.close();
					socket = null;
				} catch (Exception ignored) {
				}
			}
		}

		public void run() {
			try {
				socket = btadapter.listenUsingRfcommWithServiceRecord(
						EchoServiceName, EchoServiceUUID);
			} catch (IOException e) {
				running = false;
			}
			while (running) {
				// declare a socket
				BluetoothSocket clientSocket = null;
				try {
					Log.i("SERVER", "Server tries to accept client socket...");
					clientSocket = socket.accept();
					String reply = btadapter.getName() + " was in location "
							+ currentLocation + " at "
							+ System.currentTimeMillis();
					Log.i("SERVER", "server accepter connection from "
							+ clientSocket.getRemoteDevice().getAddress());
					BufferedReader bufReader = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
					final String line = bufReader.readLine().trim();
					Log.i("SERVER", "Received from client: " + line);
					clientSocket.getOutputStream().write(reply.getBytes());
					clientSocket.getOutputStream().flush();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							updatesAdapter.add(line);
						}
					});
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

	/* A class to handle the service client communication */
	private class Client implements Runnable {
		BluetoothDevice device;
		BluetoothSocket socket;
		String message;

		public Client(String serverDeviceAddress) {
			device = btadapter.getRemoteDevice(serverDeviceAddress);
		}

		public void run() {
			try {
				message = btadapter.getName() + " was in location "
						+ currentLocation + " at " + System.currentTimeMillis();
				socket = device
						.createRfcommSocketToServiceRecord(EchoServiceUUID);
				socket.connect();
				Log.i("CLIENT", "connected to server device, going to write: "
						+ message);
				socket.getOutputStream().write(
						(message + "\r\n").getBytes("UTF-8"));
				socket.getOutputStream().flush();

				Log.i("CLIENT", "going to read the response...");
				BufferedReader bufReader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				final String reply = bufReader.readLine();
				Log.i("CLIENT", "server replied: " + reply);

				socket.close();

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updatesAdapter.add(reply);
					}
				});
			} catch (Exception e) {
				Log.e("CLIENT", "exception in client", e);
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception ignored) {
					}
				}
			}
		}
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
					TextView locationTextView = (TextView) findViewById(R.id.MyBlipLocationTextView);
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