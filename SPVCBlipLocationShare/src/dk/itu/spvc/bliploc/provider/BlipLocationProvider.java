package dk.itu.spvc.bliploc.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import dk.itu.spvc.bliploc.BlipLocation;

/**
 * Data provider for "BlipLocation"s. It implements the standard android
 * interface ContentProvider, and uses a SQLite database as back-end
 * 
 * @author frza
 * 
 */
public class BlipLocationProvider extends ContentProvider {
	/**
	 * Log TAG
	 */
	static final String TAG = BlipLocationProvider.class.getSimpleName();

	/**
	 * Database file name
	 */
	static final String DB_NAME = "bliplocation.db";
	/**
	 * Database version. If you modify the data definition, you should update
	 * this number
	 */
	static final int DB_VERSION = 1;
	/**
	 * Name of the table where the locations will be stored
	 */
	static final String BLIPLOC_TABLENAME = "bliplocation";

	/**
	 * Projection map for blip locations objects
	 */
	private static HashMap<String, String> locationsProjectionMap;

	/**
	 * uri match BlipLocation.AUTHORITY/bliplocation
	 */
	private static final int BLIPLOCATIONS = 1;
	/**
	 * uri match BlipLocation.AUTHORITY/bliplocation/#
	 */
	private static final int BLIPLOCATION_ID = 2;
	/**
	 * uri match BlipLocation.AUTHORITY/btaddr/*
	 */
	private static final int BLIPLOCATION_BTADDR = 3;

	/**
	 * Used to match content uris
	 */
	private static final UriMatcher uriMatcher;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(BlipLocation.AUTHORITY, "bliplocation",
				BLIPLOCATIONS);
		uriMatcher.addURI(BlipLocation.AUTHORITY, "bliplocation/#",
				BLIPLOCATION_ID);
		uriMatcher.addURI(BlipLocation.AUTHORITY, "btaddr/*",
				BLIPLOCATION_BTADDR);

		locationsProjectionMap = new HashMap<String, String>();
		locationsProjectionMap.put(BlipLocation.Location._ID,
				BlipLocation.Location._ID);
		locationsProjectionMap.put(BlipLocation.Location.NAME,
				BlipLocation.Location.NAME);
		locationsProjectionMap.put(BlipLocation.Location.BLIPLOCATION,
				BlipLocation.Location.BLIPLOCATION);
		locationsProjectionMap.put(BlipLocation.Location.TIMESTAMP,
				BlipLocation.Location.TIMESTAMP);
		locationsProjectionMap.put(BlipLocation.Location.BT_ADDR,
				BlipLocation.Location.BT_ADDR);
	}

	/**
	 * Helper class to manage the SQLlite database
	 * 
	 * @author frza
	 * 
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, "crating db");
			db.execSQL("CREATE TABLE " + BLIPLOC_TABLENAME + " ("
					+ BlipLocation.Location._ID + " INTEGER PRIMARY KEY, "
					+ BlipLocation.Location.BT_ADDR + " TEXT, "
					+ BlipLocation.Location.BLIPLOCATION + " TEXT, "
					+ BlipLocation.Location.NAME + " TEXT, "
					+ BlipLocation.Location.TIMESTAMP + " INTEGER" + ");");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + BLIPLOC_TABLENAME);
			onCreate(db);
		}
	}

	DatabaseHelper helper;

	@Override
	public boolean onCreate() {
		helper = new DatabaseHelper(getContext());
		return true;
	}

	/**
	 * Create and return a cursor over the selected data
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(BLIPLOC_TABLENAME);
		Log.i(TAG, "querying bliplocations");

		String groupBy = null;
		String having = null;

		switch (uriMatcher.match(uri)) {
		case BLIPLOCATIONS:
			// normal query on bliplocations, just set the projection map
			qb.setProjectionMap(locationsProjectionMap);
			break;
		case BLIPLOCATION_ID:
			// a precise bliplocation has been requested, set the projection map
			qb.setProjectionMap(locationsProjectionMap);
			// ..and specify the _ID of the requested bliplocation
			qb.appendWhere(BlipLocation.Location._ID + "="
					+ uri.getPathSegments().get(1));
			break;
		case BLIPLOCATION_BTADDR:
			// a bliplocation for a BT address has been requested, set the
			// projection map
			qb.setProjectionMap(locationsProjectionMap);
			// ..and specify the BT_ADDR of the requested bliplocation
			qb.appendWhere(BlipLocation.Location.BT_ADDR + "='"
					+ uri.getPathSegments().get(1) + "'");
			break;
		default:
			// ..what uri we received??
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// use the DEFAULT_SORT_ORDER if not specified
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = BlipLocation.Location.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// create a Cursor from the SQLite database
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
				having, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * Insert a new BlipLocation
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initValues) {
		Log.i(TAG, "creating new tweet");
		// accept only the BLIPLOCATIONS uri
		if (uriMatcher.match(uri) != BLIPLOCATIONS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// construct a new ContentValues, if null was passed as parameter. We
		// could also raise an exception,
		// since users are not supposed to create an empty bliplocation
		ContentValues values = (initValues != null) ? new ContentValues(
				initValues) : new ContentValues();

		// what's the time?
		Long now = Long.valueOf(System.currentTimeMillis());

		// use the current timestamp, if not provided
		if (!values.containsKey(BlipLocation.Location.TIMESTAMP)) {
			values.put(BlipLocation.Location.TIMESTAMP, now);
		} else {
			// check if row already exists
			int id = rowExists(values
					.getAsString(BlipLocation.Location.BT_ADDR), values
					.getAsLong(BlipLocation.Location.TIMESTAMP));
			if (id != -1) {
				// if it does, then return the current _ID uri
				Log.w(TAG, "tweet exists: "
						+ values.get(BlipLocation.Location.BT_ADDR) + "/"
						+ values.get(BlipLocation.Location.TIMESTAMP));
				return ContentUris.withAppendedId(
						BlipLocation.Location.CONTENT_URI, id);
			}
		}
		// put empty location if empty
		if (!values.containsKey(BlipLocation.Location.BLIPLOCATION)) {
			values.put(BlipLocation.Location.BLIPLOCATION, "");
		}

		// get a writable database and insert the row
		SQLiteDatabase db = helper.getWritableDatabase();
		long rowId = db.insert(BLIPLOC_TABLENAME,
				BlipLocation.Location.UNKNOWN_LOC, values);
		if (rowId > 0) {
			// if a row has been added, create a Uri object that point to it
			// with the new _ID
			Uri out = ContentUris.withAppendedId(
					BlipLocation.Location.CONTENT_URI, rowId);
			// announce that the database has changed
			getContext().getContentResolver().notifyChange(out, null);
			// return the new uri
			return out;
		}
		// something went wrong!
		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * Utility method to test if a bliplocation already exists (same btaddress,
	 * same timestamp)
	 * 
	 * @param btAddr
	 * @param timestamp
	 * @return
	 */
	private int rowExists(String btAddr, long timestamp) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(BLIPLOC_TABLENAME,
				new String[] { BlipLocation.Location._ID },
				BlipLocation.Location.BT_ADDR + "=? AND "
						+ BlipLocation.Location.TIMESTAMP + "=?", new String[] {
						btAddr, timestamp + "" }, null, null, null);
		int out = -1;
		if (c.moveToFirst()) {
			out = c.getInt(0);
		}
		c.close();
		return out;
	}

	/**
	 * Delete some rows
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = helper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
		case BLIPLOCATIONS:
			count = db.delete(BLIPLOC_TABLENAME, selection, selectionArgs);
			break;
		case BLIPLOCATION_ID:
			String tweetId = uri.getPathSegments().get(1);
			count = db.delete(BLIPLOC_TABLENAME, BlipLocation.Location._ID
					+ "="
					+ tweetId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ")" : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/**
	 * Update some rows
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = helper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
		case BLIPLOCATIONS:
			//normal update
			count = db.update(BLIPLOC_TABLENAME, values, selection, selectionArgs);
			break;

		case BLIPLOCATION_ID:
			//update specified by id
			String tweetId = uri.getPathSegments().get(1);
			count = db.update(BLIPLOC_TABLENAME, values,
					BlipLocation.Location._ID
							+ "="
							+ tweetId
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		//announce database changed
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

}
