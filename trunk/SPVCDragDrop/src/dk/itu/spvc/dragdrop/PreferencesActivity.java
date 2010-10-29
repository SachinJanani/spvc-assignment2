package dk.itu.spvc.dragdrop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Activity to set-up the app (server ip and port)
 * 
 * @author frza
 * 
 */
public class PreferencesActivity extends Activity implements OnClickListener {
	/**
	 * The name of the file where the preferences will be stored
	 */
	public static final String PREFS_NAME = "spvcdragdropprefs";

	/**
	 * Android representation of "preferences"
	 */
	SharedPreferences prefs;
	/**
	 * Widgets containing the ip and port edit text
	 */
	EditText ip, port;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// nothing fancy here, usual super.onCreate call and set the layout
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prefs);
	}

	@Override
	protected void onStart() {
		super.onStart();

		// set "this" an click listener of the Save button
		((Button) findViewById(R.id.SavePrefsButton)).setOnClickListener(this);

		// get the preference file and edit texts
		prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		ip = (EditText) findViewById(R.id.ServerIpEditText);
		port = (EditText) findViewById(R.id.ServerPortEditText);

		// fill the text widgets if some data is already present in the prefs
		if (prefs.contains("server_ip")) {
			ip.setText(prefs.getString("server_ip", "unknown"));
		}
		if (prefs.contains("server_port")) {
			port.setText(""
					+ Integer.parseInt(prefs.getString("server_port", "0")));
			//ORIGINAL: port.setText( "" + prefs.getInt("server_port", 0) );
		}
	}

	/**
	 * Save button onClick callback
	 */
	@Override
	public void onClick(View v) {
		// get the prefs editor
		SharedPreferences.Editor editor = prefs.edit();
		// and save the data

		// # put the server_ip and server_port preferences in the editor!
		editor.putString("server_ip", ip.getText().toString());
		editor.putString("server_port", port.getText().toString());
		//

		// do not forget to commit!
		boolean res = editor.commit();
		Log.d("DRADROP_PREFSACTIVITY", "Commit preferences: " + res);

		// start the dragdrop activity
		startActivity(new Intent(this, DragDropActivity.class));
		finish();
	}
}
