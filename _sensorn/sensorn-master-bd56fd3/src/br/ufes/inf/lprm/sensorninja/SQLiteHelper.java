package br.ufes.inf.lprm.sensorninja;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {

	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "ninjaDB";
	// Package
	private static final String PACKAGE = "br.ufes.inf.lprm.sensorninja";

	// Checkpoints table name
	private static final String TABLE_BOOKS = "checkpoints";

	// Checkpoints Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_LAT = "lat";
	private static final String KEY_LNG = "lng";
	private static final String KEY_DAT = "dat";

	// private static final String[] COLUMNS = { KEY_ID, KEY_LAT, KEY_LNG,
	// KEY_DAT };

	public SQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// SQL statement to create book table
		String CREATE_CHECKPOINT_TABLE = "CREATE TABLE checkpoints (id INTEGER PRIMARY KEY AUTOINCREMENT, lat DOUBLE, lng DOUBLE, dat DATETIME)";

		// create books table
		db.execSQL(CREATE_CHECKPOINT_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older books table if existed
		db.execSQL("DROP TABLE IF EXISTS checkpoints");

		// create fresh books table
		this.onCreate(db);
	}

	public void checkpoint(double lat, double lng, String dat) {
		// 1. get reference to writable DB
		SQLiteDatabase db = this.getWritableDatabase();

		// 2. create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_LAT, lat);
		values.put(KEY_LNG, lng);
		values.put(KEY_DAT, dat);

		// 3. insert
		db.insert(TABLE_BOOKS, // table
				null, // nullColumnHack
				values); // key/value -> keys = column names/ values = column
							// values

		// for logging
		Log.v("checkpoint", lat + " -- " + lng + " -- " + dat);

		// 4. close
		db.close();
	}

	public static void exportDB() {
		File sd = Environment.getExternalStorageDirectory();
		File data = Environment.getDataDirectory();
		FileChannel source = null;
		FileChannel destination = null;
		String currentDBPath = "/data/" + PACKAGE + "/databases/"
				+ DATABASE_NAME;
		Date now = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String backupDBPath = DATABASE_NAME + "_" + sdf.format(now) + ".db";
		File currentDB = new File(data, currentDBPath);
		File backupDB = new File(sd, backupDBPath);
		try {
			source = new FileInputStream(currentDB).getChannel();
			destination = new FileOutputStream(backupDB).getChannel();
			destination.transferFrom(source, 0, source.size());
			source.close();
			destination.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
