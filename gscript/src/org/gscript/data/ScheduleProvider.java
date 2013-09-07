package org.gscript.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class ScheduleProvider extends ContentProvider {

	ScheduleDatabase mScheduleDatabase;
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_WEEK = "week";
	public static final String COLUMN_DAYS = "days";
	public static final String COLUMN_TIME_START = "start";
	public static final String COLUMN_TIME_END = "end";
	public static final String COLUMN_INTERVAL = "interval";
	
	public static final int MONDAY 		= 0x1;
	public static final int TUESDAY 	= 0x2;
	public static final int WEDNESDAY 	= 0x4;
	public static final int THURSDAY 	= 0x8;
	public static final int FRIDAY 		= 0x10;
	public static final int SATURDAY 	= 0x20;
	public static final int SUNDAY 		= 0x40;

	@Override
	public boolean onCreate() {

		mScheduleDatabase = new ScheduleDatabase(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		int uriType = ContentUri.MATCHER.match(uri);

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		SQLiteTable table = getTableForUriMatch(uriType);

		switch (uriType) {
		case ContentUri.MATCH_SCHEDULE:

			queryBuilder.setTables(table.getTableName());

			break;
		case ContentUri.MATCH_SCHEDULE_ITEM:

			queryBuilder.setTables(table.getTableName());
			queryBuilder.appendWhere(table.getPrimaryKeyField() + "="
					+ uri.getLastPathSegment());

			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = mScheduleDatabase.getReadableDatabase();

		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		int uriType = ContentUri.MATCHER.match(uri);

		SQLiteDatabase db = mScheduleDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		long id = 0;

		switch (uriType) {
		case ContentUri.MATCH_SCHEDULE:

			id = db.insert(table.getTableName(), null, values);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return Uri.withAppendedPath(uri, String.valueOf(id));
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		int uriType = ContentUri.MATCHER.match(uri);

		SQLiteDatabase db = mScheduleDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		int rowsUpdated = 0;

		switch (uriType) {
		case ContentUri.MATCH_SCHEDULE:

			rowsUpdated = db.update(table.getTableName(), values, selection,
					selectionArgs);

			break;
		case ContentUri.MATCH_SCHEDULE_ITEM:

			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {

				rowsUpdated = db.update(table.getTableName(), values,
						table.getPrimaryKeyField() + "=" + id, null);

			} else {

				rowsUpdated = db.update(table.getTableName(), values,
						table.getPrimaryKeyField() + "=" + id + " and "
								+ selection, selectionArgs);

			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowsUpdated;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		int uriType = ContentUri.MATCHER.match(uri);

		SQLiteDatabase db = mScheduleDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		int rowsDeleted = 0;

		switch (uriType) {
		case ContentUri.MATCH_SCHEDULE:

			rowsDeleted = db.delete(table.getTableName(), selection,
					selectionArgs);

			break;
		case ContentUri.MATCH_SCHEDULE_ITEM:

			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {

				rowsDeleted = db.delete(table.getTableName(),
						table.getPrimaryKeyField() + "=" + id, null);

			} else {

				rowsDeleted = db.delete(table.getTableName(),
						table.getPrimaryKeyField() + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowsDeleted;
	}

	SQLiteTable getTableForUriMatch(int uriType) {

		switch (uriType) {
		case ContentUri.MATCH_SCHEDULE:
		case ContentUri.MATCH_SCHEDULE_ITEM:
			return ScheduleDatabase.TABLES[ScheduleDatabase.TABLE_SCHEDULE];
		}

		return null;
	}

	static class ScheduleDatabase extends SQLiteOpenHelper {

		public static final String DATABASE_NAME = "schedule";
		public static final int DATABASE_VERSION = 1;

		static final SQLiteTable[] TABLES = new SQLiteTable[] { new ScheduleTable() };
		static final int TABLE_SCHEDULE = 0;
		
		ScheduleDatabase(Context context) {
			super(context, DATABASE_NAME, null,
					DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			for(SQLiteTable table : TABLES) {
				table.onCreate(db);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			for(SQLiteTable table : TABLES) {
				table.onUpgrade(db, oldVersion, newVersion);
			}
		}	
	}
	
	static class ScheduleTable extends SQLiteTable {

		public static final String TABLE_NAME = "schedule";
		public static final String[] COLUMNS = { COLUMN_ID, COLUMN_TITLE, COLUMN_WEEK, COLUMN_DAYS, COLUMN_TIME_START, COLUMN_TIME_END, COLUMN_INTERVAL };
		
		static final String SQL_CREATE_TABLE = "create table "
				+ TABLE_NAME 
				+ "(" 
				+ COLUMN_ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_TITLE		+ " TEXT NOT NULL, "
				+ COLUMN_WEEK		+ " INTEGER, "
				+ COLUMN_DAYS		+ " INTEGER, "
				+ COLUMN_TIME_START	+ " INTEGER, "
				+ COLUMN_TIME_END	+ " INTEGER, "
				+ COLUMN_INTERVAL	+ " INTEGER"
				+ ");";
		
		public ScheduleTable() {
			super(TABLE_NAME);
		}

		@Override
		void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_TABLE);
		}

		@Override
		void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);		
		}

		@Override
		String getPrimaryKeyField() {
			return COLUMN_ID;
		}
	}
	
}
