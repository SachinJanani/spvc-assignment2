package dk.itu.spvc.android.bt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import dk.itu.android.bluetooth.BluetoothAdapter;
import dk.itu.android.bluetooth.BluetoothDevice;
import dk.itu.android.bluetooth.BluetoothServerSocket;
import dk.itu.android.bluetooth.BluetoothSocket;

public class EchoActivity extends Activity {

	/* used when requesting the bluetooth-enabling activity */
	static final int REQUEST_BLUETOOTH_ENABLE = 1;
	/* request to make the device discoverable */
	static final int REQUEST_ENABLE_DISCOVERABLE = 2;
	/* request to select a device to connect to */
	static final int REQUEST_CONNECT_DEVICE = 3;

	/* the service UUID and name */
	static final UUID EchoServiceUUID = UUID
			.fromString("419bbc68-c365-4c5e-8793-5ebff85b908c");
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

	private void setServer(boolean server) {
		this.isServer = server;
		findViewById(R.id.ResponseTextView).setVisibility(View.VISIBLE);
		findViewById(R.id.StartServerButton).setVisibility(View.GONE);
		findViewById(R.id.SelectDeviceButton).setVisibility(View.GONE);
		if (!server) {
			findViewById(R.id.SendEditText).setVisibility(View.VISIBLE);
			findViewById(R.id.SendButton).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.StopServerButton).setVisibility(View.VISIBLE);
		}
	}

	private void appendToResponseView(String line) {
		String curText = responseTextView.getText().toString();
		if (curText.length() != 0) {
			curText += "\n";
		}
		curText += line;
		responseTextView.setText(curText);
	}

	public void deviceSelected(String deviceAddress) {
		setServer(false);
		this.client = new Client(deviceAddress);
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

	public void sendMessage(View view) {
		client.message = ((EditText) findViewById(R.id.SendEditText)).getText()
				.toString();
		new Thread(client).start();
	}

	public void selectServerDevice(View view) {
		Intent intent = new Intent(this, DeviceListActivity.class);
		this.startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		dk.itu.android.bluetooth.BluetoothAdapter.SetContext(this);
		findViewById(R.id.StartServerButton).setEnabled(false);
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
				onBluetoothEnabled();
			}
		}
	}

	private void onBluetoothEnabled() {
		findViewById(R.id.SelectDeviceButton).setEnabled(true);
		findViewById(R.id.StartServerButton).setEnabled(true);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (REQUEST_BLUETOOTH_ENABLE == requestCode) {
			if (RESULT_OK == resultCode) {
				onBluetoothEnabled();
			} else {
				Toast.makeText(this,
						"Cannot do anything if bluetooth is disabled :(",
						Toast.LENGTH_SHORT);
				finish();
			}
		} else if (REQUEST_CONNECT_DEVICE == requestCode) {
			if (RESULT_OK == resultCode) {
				String deviceAddress = data
						.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				deviceSelected(deviceAddress);
			}
		} else if (REQUEST_ENABLE_DISCOVERABLE == requestCode) {
			new Thread(server).start();
		}
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
					clientSocket = socket.accept();
					Log.w("KONRAD", "Accepted a new connection: "
							+ clientSocket);
					BufferedReader bufReader = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
					final String line = bufReader.readLine().trim();
					clientSocket.getOutputStream().write(
							(">" + line + "\r\n").getBytes("UTF-8"));
					clientSocket.getOutputStream().flush();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							appendToResponseView("Received >> " + line);
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
				BluetoothSocket socket = device
						.createRfcommSocketToServiceRecord(EchoServiceUUID);
				socket.getOutputStream().write(
						(message + "\r\n").getBytes("UTF-8"));
				socket.getOutputStream().flush();
				BufferedReader bufReader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				final String echoed = bufReader.readLine();
				appendToResponseView(echoed);
			} catch (IOException e) {
				Log.d("KONRAD", "Failed to create client socket..");
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
}