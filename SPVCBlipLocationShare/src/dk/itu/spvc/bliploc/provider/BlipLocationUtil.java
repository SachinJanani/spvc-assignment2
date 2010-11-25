package dk.itu.spvc.bliploc.provider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.SimpleCursorAdapter;
import dk.itu.spvc.bliploc.BlipLocation;
import dk.itu.spvc.bliploc.R;

/**
 * Utility class to ease the access to some common BlipLocation data
 * interactions
 * 
 * @author frza
 * 
 */
public class BlipLocationUtil {
	static final String TAG = BlipLocationUtil.class.getSimpleName();

	static final String[] proj = new String[] { BlipLocation.Location._ID,
			BlipLocation.Location.BT_ADDR, BlipLocation.Location.BLIPLOCATION,
			BlipLocation.Location.NAME, BlipLocation.Location.TIMESTAMP };

	/**
	 * Create a ListAdapter on the BlipLocation data.
	 * 
	 * @param Activity c
	 * @param itemLayoutId
	 * @param cellLayoutIds
	 * @return
	 */
	public SimpleCursorAdapter standardListAdapter(Activity c) {
		return new SimpleCursorAdapter(
				c, R.layout.location_item,
				standardManagedQuery(c),
				new String[]{ BlipLocation.Location.NAME, BlipLocation.Location.BLIPLOCATION, BlipLocation.Location.TIMESTAMP },
				new int[]{ R.id.Name, R.id.BlipLocation, R.id.Timestamp });
	}
	
	private Cursor standardManagedQuery(Activity a) {
		return a.managedQuery(BlipLocation.Location.CONTENT_URI, proj, null,
				null, BlipLocation.Location.DEFAULT_SORT_ORDER);
	}

	/**
	 * Load the current BlipLocation for the specified Bluetooth device, if any.
	 * Returns null is nothing has been found.
	 * 
	 * @param context
	 * @param btaddress
	 * @return
	 */
	public BlipLocationDO lookupFromBluetooth(Context context, String btaddress) {
		Cursor c = context.getContentResolver().query(
				BlipLocation.Location.BTADDR_CONTENT_URI.buildUpon()
						.appendPath(btaddress).build(), proj, null, null, null);
		BlipLocationDO out = null;
		if (c.moveToFirst()) {
			out = fromCursor(c);
		}
		c.close();
		return out;
	}

	/**
	 * create/update a BlipLocation for the current device. Returns the
	 * associated uri.
	 * 
	 * @param context
	 * @param adapter
	 * @param location
	 * @return
	 */
	public Uri insertMyLocation(Context context, BluetoothAdapter adapter,
			String location) {

		BlipLocationDO myBlipLoc = lookupFromBluetooth(context, adapter.getAddress());
		Uri out = null;

		if (myBlipLoc == null) {
			myBlipLoc = new BlipLocationDO(adapter.getAddress(), adapter
					.getName(), location, System.currentTimeMillis());
		} else {
			myBlipLoc.setLocation(location);
			myBlipLoc.setTimestamp(System.currentTimeMillis());
		}

		out = insertOrUpdate(context, myBlipLoc);
		return out;
	}

	/**
	 * Create/update a BlipLocation for a remote device. Returns the associated
	 * uri. In the case that the given timestamp is older than the one in the
	 * database, the information is discarded.
	 * 
	 * @param context
	 * @param bluetoothAddress
	 * @param name
	 * @param location
	 * @param timestamp
	 * @return
	 */
	public Uri insertNewLocation(Context context, String bluetoothAddress,
			String name, String location, long timestamp) {
		BlipLocationDO current = lookupFromBluetooth(context, bluetoothAddress);
		if (current != null) {
			if (current.timestamp >= timestamp) {
				Log.i(TAG, "received old bliplocation for " + bluetoothAddress
						+ ". Ignore.");
				return forBlipLocation(current);
			}

			current.setLocation(location);
			current.setTimestamp(timestamp);
		} else {
			current = new BlipLocationDO(bluetoothAddress, name, location,
					timestamp);
		}
		return insertOrUpdate(context, current);
	}

	Uri forBlipLocation(BlipLocationDO blipLoc) {
		return BlipLocation.Location.CONTENT_URI.buildUpon().appendEncodedPath(
				blipLoc.id + "").build();
	}

	Uri insertOrUpdate(Context context, BlipLocationDO blipLoc) {
		Uri out = null;
		String msg = null;

		if (blipLoc.id < 0) {
			out = context.getContentResolver().insert(
					BlipLocation.Location.CONTENT_URI,
					blipLoc.toContentValues());
			if (out == null) {
				msg = "cannot create bliplocation!";
			} else {
				msg = "new bliplocation uri: " + out;
			}
		} else {
			out = forBlipLocation(blipLoc);
			int updated = context.getContentResolver().update(out,
					blipLoc.toContentValues(), null, null);
			msg = "updated " + updated + " rows";
		}

		Log.i(TAG, msg);
		return out;
	}
	
	/**
	 * Load all current BlipLocations. The returned collections is not ordered in any particular way.
	 * @param context
	 * @return
	 */
	public Collection<BlipLocationDO> loadAll(Context context) {
		Cursor c = context.getContentResolver().query(BlipLocation.Location.CONTENT_URI, proj, null, null, null);
		
		Set<BlipLocationDO> out = new HashSet<BlipLocationDO>();
		
		if(c.moveToFirst()) {
			do {
				out.add( fromCursor(c) );
			} while(c.moveToNext());
		}
		c.close();
		
		return out;
	}

	/**
	 * DataObject class for BlipLocation
	 * @author frza
	 *
	 */
	public class BlipLocationDO {

		int id;
		String btaddr;
		String name;
		String location;
		long timestamp;

		BlipLocationDO(String bta, String n, String l, long ts) {
			this(bta, n, l, ts, -1);
		}

		BlipLocationDO(String bta, String n, String l, long ts, int _id) {
			btaddr = bta;
			name = n;
			location = l;
			timestamp = ts;
			id = _id;
		}

		/**
		 * Device Bluetooth address
		 * @return
		 */
		public String getBtaddr() {
			return btaddr;
		}

		/**
		 * BlipLocation
		 * @return
		 */
		public String getLocation() {
			return location;
		}

		/**
		 * Device name
		 * @return
		 */
		public String getName() {
			return name;
		}

		/**
		 * Timestamp
		 * @return
		 */
		public long getTimestamp() {
			return timestamp;
		}

		/**
		 * SQLite ID
		 * @return
		 */
		public int getId() {
			return id;
		}

		/**
		 * Set the BlipLocation
		 * @param location
		 */
		public void setLocation(String location) {
			this.location = location;
		}

		/**
		 * Set the device name
		 * @param name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Set the timestamp
		 * @param timestamp
		 */
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		/**
		 * Set the SQlite ID (automatic)
		 * @param id
		 */
		protected void setId(int id) {
			this.id = id;
		}

		ContentValues toContentValues() {
			ContentValues out = new ContentValues();
			out.put(BlipLocation.Location.BT_ADDR, btaddr);
			out.put(BlipLocation.Location.BLIPLOCATION, location);
			out.put(BlipLocation.Location.NAME, name);
			out.put(BlipLocation.Location.TIMESTAMP, timestamp);
			return out;
		}
	}

	BlipLocationDO fromCursor(Cursor c) {
		return new BlipLocationDO(getString(c, BlipLocation.Location.BT_ADDR),
				getString(c, BlipLocation.Location.NAME), getString(c,
						BlipLocation.Location.BLIPLOCATION), getLong(c,
						BlipLocation.Location.TIMESTAMP), getInt(c,
						BlipLocation.Location._ID));
	}

	private String getString(Cursor c, String colName) {
		return c.getString(c.getColumnIndex(colName));
	}

	private long getLong(Cursor c, String colName) {
		return c.getLong(c.getColumnIndex(colName));
	}

	private int getInt(Cursor c, String colName) {
		return c.getInt(c.getColumnIndex(colName));
	}

}
