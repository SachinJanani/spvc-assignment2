package dk.itu.spvc.bliploc;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Constants for the BlipLocationProvider
 * @author frza
 *
 */
public class BlipLocation {
	/**
	 * Name of the authority that can access the BlipLocation data store.
	 * The same as the one specified in the AndroidManifest.xml file:<br/>
	 * <code>
	 * &lt;provider android:name=".provider.BlipLocationProvider" android:authorities="dk.itu.spvc.bliploc.provider.BlipLocationProvider"&gt;&lt;/provider&gt;
	 * </code>
	 */
	public static final String AUTHORITY = "dk.itu.spvc.bliploc.provider.BlipLocationProvider";
	
	private BlipLocation(){}
	
	/**
	 * Constants for the BlipLocation database table
	 * @author frza
	 *
	 */
	public static final class Location implements BaseColumns {
		private Location(){}
		
		/**
		 * URI associated to the table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/bliplocation");
		/**
		 * URI associated to the row of a particular bluetooth device
		 */
		public static final Uri BTADDR_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/btaddr");

		/**
		 * Bluetooth device address column
		 */
		public static final String BT_ADDR = "BTADDR";
		/**
		 * Timestamp column
		 */
		public static final String TIMESTAMP = "TIMESTAMP";
		/**
		 * Name column
		 */
		public static final String NAME = "DEVICEFRIENDLYNAME";
		/**
		 * BlipLocation column
		 */
		public static final String BLIPLOCATION = "BLIPLOCATION";
		
		/**
		 * Default ordering: TIMESTAMP DESC
		 */
		public static final String DEFAULT_SORT_ORDER = TIMESTAMP+" DESC";
		
		/**
		 * Default null value: unknown
		 */
		public static final String UNKNOWN_LOC = "unknown";

		
	}

}
