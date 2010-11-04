package dk.itu.spvc.dragdrop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * "Android-friendly" network client. Calls to this client will be asynchronous.
 * If you'll like to know when and if some command succeeded, use the methods
 * that accept an "OnResult" object
 * 
 * @author frza
 * 
 */
public class AndroidClient {
	static final String DEBUG_TAG = "IMAGER";
	/**
	 * Callback interface
	 * 
	 * @author frza
	 * 
	 * @param <T>
	 */
	public interface OnResult<T> {
		void onResult(T res);
	}

	public interface OnImage {
		void onImage(byte[] imageBytes);
	}

	public interface OnCoords {
		void onCoords(float x, float y);
	}

	// just a record-like class to store a single coordinate
	public class Coord {
		float x;
		float y;

		Coord(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}

	// async task that reads the server commands
	class ReceiveDataAsyncTask extends AsyncTask<Client, Void, Void> {
		@Override
		protected Void doInBackground(Client... params) {
			Log.v("IMAGER_01", "doInBackground method");
			Client c = params[0];

			while (true) {
				try {
					byte cmd = c.nextCommand();
					if (cmd == 0) {
						break;
					}
					switch (cmd) {
					case (byte) 49:
						imageCallback.onImage(c.readImage());
						break;
					case (byte) 50:
						float[] coords = c.readCoord();
						coordsCallback.onCoords(coords[0], coords[1]);
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				// break;
			}

			return null;
		}
	}

	// the asynchronous task to send an image
	class SendFileImageTask extends AsyncTask<File, Void, Boolean> {
		OnResult<Boolean> onResult;

		@Override
		protected Boolean doInBackground(File... params) {
			try {
				client.sendImage(params[0]);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (onResult != null) {
				onResult.onResult(result);
			}
		}
	}

	class SendInputStreamImageTask extends
			AsyncTask<InputStream, Void, Boolean> {
		OnResult<Boolean> onResult;

		@Override
		protected Boolean doInBackground(InputStream... params) {
			try {
				client.sendImage(params[0]);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (onResult != null) {
				onResult.onResult(result);
			}
		}
	}

	// the asynchronous task to send a coordinate
	class SendCoordsTask extends AsyncTask<Coord, Void, Boolean> {
		OnResult<Boolean> onResult;

		@Override
		protected Boolean doInBackground(Coord... params) {
			try {
				client.sendCoordinates(params[0].x, params[0].y);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (onResult != null) {
				onResult.onResult(result);
			}
		}
	}

	Client client;
	OnImage imageCallback;
	OnCoords coordsCallback;

	/**
	 * Create the network client to the serverIp:serverPort endpoint
	 * 
	 * @param serverIp
	 * @param serverPort
	 * @throws IOException
	 */
	public AndroidClient(String serverIp, int serverPort) throws IOException {
		this(serverIp, serverPort, null, null);
	}

	public AndroidClient(String serverIp, int serverPort,
			OnImage imageCallback, OnCoords coordsCallback) throws IOException {
		this.client = new Client(serverIp, serverPort);
		this.imageCallback = imageCallback != null ? imageCallback
				: new OnImage() {
					@Override
					public void onImage(byte[] imageBytes) {
						Log.d("EMPTY_IMGCALLBACK", "recevied image of length: "
								+ imageBytes.length);
					}
				};
		this.coordsCallback = coordsCallback != null ? coordsCallback
				: new OnCoords() {
					@Override
					public void onCoords(float x, float y) {
						Log.d("EMPTY_COORDSCALLBACK", "received coords: " + x
								+ ", " + y);
					}
				};
		new ReceiveDataAsyncTask().execute(this.client);
	}

	/**
	 * Send the image asynchronously. Discard results.
	 * 
	 * @param file
	 */
	public void sendImage(File file) {
		sendImage(file, null);
	}

	/**
	 * Send the image asynchronously. Discard results.
	 * 
	 * @param is
	 */
	public void sendImage(InputStream is) {
		sendImage(is, null);
	}

	/**
	 * Send the coordinates asynchronously. Discard results.
	 * 
	 * @param x
	 * @param y
	 */
	public void sendCoordinates(float x, float y) {
		sendCoordinates(x, y, null);
	}

	/**
	 * Send the image asynchronously. The callback object will be called when
	 * the task finishes.
	 * 
	 * @param file
	 * @param callback
	 */
	public void sendImage(File file, OnResult<Boolean> callback) {
		SendFileImageTask task = new SendFileImageTask();
		task.onResult = callback;
		task.execute(file);
	}

	/**
	 * Send the image asynchronously. The callback object will be called when
	 * the task finishes.
	 * 
	 * @param is
	 * @param callback
	 */
	public void sendImage(InputStream is, OnResult<Boolean> callback) {
		// # implement me!
		/*
		 * create a SendInputStreamImageTask, set its onResult callback and
		 * execute it with the InputStream parameter
		 */
		SendInputStreamImageTask sisi = new SendInputStreamImageTask();
		sisi.onResult = callback;
		sisi.execute(is);
	}

	/**
	 * Send the coordinates asynchronously. The callback object will be called
	 * when the task finishes.
	 * 
	 * @param x
	 * @param y
	 * @param callback
	 */
	public void sendCoordinates(float x, float y, OnResult<Boolean> callback) {
		SendCoordsTask task = new SendCoordsTask();
		task.onResult = callback;
		task.execute(new Coord(x, y));
	}

	public void close() throws IOException {
		client.close();
	}
}
