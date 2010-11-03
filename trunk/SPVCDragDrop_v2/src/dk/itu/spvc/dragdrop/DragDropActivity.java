package dk.itu.spvc.dragdrop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Gallery;
import dk.itu.spvc.dragdrop.AndroidClient.OnResult;

/**
 * Entry point of the application. This activity displays a frame containing the
 * selected image and a gallery with the available images.
 * 
 * @author frza
 * 
 */
public class DragDropActivity extends Activity {
	static final String TAG = "DRAGDROP_ACTIVITY";

	/**
	 * IP of the server. Obtained from the SharedPreferences of this application
	 */
	String serverIp;
	/**
	 * port of the server. Obtained from the SharedPreferences of this
	 * application
	 */
	int serverPort;

	/**
	 * The network client. Used to send images and coordinates
	 */
	AndroidClient client;

	/**
	 * The gallery with the available images
	 */
	Gallery gallery;
	/**
	 * The frame layout where to put the selected image
	 */
	FrameLayout frame;
	/**
	 * Custom view that visualize the image and handle basic gestures
	 */
	PlayAreaView dragged = null;
	/**
	 * Menu items associated with the activity to modify preferences and to exit
	 * (hide, really) the application
	 */
	MenuItem preferences, exit;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// we don't want title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
	}

	/**
	 * onStart is called every time the activity is shown (so, the first time
	 * and after returning from a "pause")
	 */
	@Override
	protected void onStart() {
		super.onStart();

		// get the shared preferences object. The name is defined in
		// PreferencesActivity.PREFS_NAME
		SharedPreferences prefs = getSharedPreferences(
				PreferencesActivity.PREFS_NAME, MODE_PRIVATE);

		// if the preferences are not setted, start the PreferencesActivity
		// activity
		if (!prefs.contains("server_ip")) {
			// # start the PreferencesActivity
			startActivity(new Intent(this, PreferencesActivity.class));
		} else {
			// # otherwise, get the server ip and port, and initialize this
			// activity
			// # from the preferences, initialize the serverIp and serverPort
			// fields
			serverIp = prefs.getString("server_ip", null);
			serverPort = Integer.parseInt(prefs.getString("server_port", null));
			//
			initDragDrop();
		}
	}

	/**
	 * Called when the activity needs to create its own menu. Create here the
	 * "preferences" and "exit" menu items, by calling menu.add( <some string>
	 * ). Return true to display the menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// # create the preferences and exit MenuItem fields
		preferences = menu.add("Preferences");
		exit = menu.add("Exit");
		//
		return true;
	}

	/**
	 * Called when a menu item is selected by the user. Handle here the
	 * "preferences" and "exit" menu item
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == preferences) {
			// # start the PreferencesActivity
			startActivity(new Intent(this, PreferencesActivity.class));
			//
		} else {
			this.finish();
		}
		return true;
	}

	/**
	 * Called when an activity is requested to stop. Close the network client
	 */
	@Override
	protected void onStop() {
		super.onStop();
		try {
			client.close();
		} catch (Exception ignored) {
		}
	}

	/**
	 * Fill the frame and gallery fields. Set the gallery adapter (use the
	 * LolImageAdapter for the exercises). Set up a gesture listener, a
	 * GalleryImageThrownDetector, to call "setDragged" when an image is
	 * "thrown" by the user. Create the network client
	 */
	private void initDragDrop() {
		frame = (FrameLayout) findViewById(R.id.Frame);
		gallery = (Gallery) findViewById(R.id.Gallery);

		gallery.setAdapter(new LolImageAdapter(this));

		new GalleryImageThrownDetector(this);
	}

	/**
	 * Called when an image is "thrown" by the user. Since the server accepts
	 * one image at a time, if the "dragged" field is already filled, ignore the
	 * call. Otherwise, send the image to the server using the network client,
	 * and create a PlayAreaView that displays the image itself. For the
	 * exercises, the image is static and the resource id can be found in the
	 * "imageView.resourceId" field.
	 * 
	 * @param imageView
	 */
	public void setDragged(final FileAwareImageView imageView) {
		if (dragged == null) {
			dragged = new PlayAreaView(this, imageView.resourceId, 100, 100);
			try {
				if (client == null) {
					client = new AndroidClient(serverIp, serverPort);
				}
				client.sendImage(imageView.getImageInputStream(),
						new OnResult<Boolean>() {
							@Override
							public void onResult(Boolean res) {
								if (res) {
									frame.addView(dragged);
								}
							}
						});
			} catch (Exception e) {
				e.printStackTrace();
				dragged = null;
			}
		}
	}

	/**
	 * Called when the "dragged" image is moved in the screen. Send the
	 * coordinates to the network client
	 * 
	 * @param x
	 * @param y
	 */
	public void imageMoved(float x, float y) {
		client.sendCoordinates(x, y); // well, here we don't really care too
		// much about when the coords are
		// received
	}

	/**
	 * Called when the user performs the gesture to "clean" the application. If
	 * the "dragged" field is not null, remove all the views from the "frame"
	 * field, and set "dragged" to null. Close also the network client
	 */
	public void resetDragDrop() {
		if (dragged != null) {
			frame.removeAllViews();
			dragged = null;
		}
		try {
			client.close();
		} catch (Exception ignored) {
		} finally {
			client = null;
		}
	}
}