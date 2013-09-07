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

public class HistoryProvider extends ContentProvider {

	HistoryDatabase mHistoryDatabase;
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_INTENT = "intent";
	public static final String COLUMN_TIME_START = "start";
	public static final String COLUMN_TIME_END = "end";
	public static final String COLUMN_STATE = "state";
	public static final String COLUMN_LOG = "log";
	

	@Override
	public boolean onCreate() {

		mHistoryDatabase = new HistoryDatabase(getContext());

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
		case ContentUri.MATCH_HISTORY:

			queryBuilder.setTables(table.getTableName());

			break;
		case ContentUri.MATCH_HISTORY_ITEM:

			queryBuilder.setTables(table.getTableName());
			queryBuilder.appendWhere(table.getPrimaryKeyField() + "="
					+ uri.getLastPathSegment());

			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = mHistoryDatabase.getReadableDatabase();

		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		int uriType = ContentUri.MATCHER.match(uri);

		SQLiteDatabase db = mHistoryDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		long id = 0;

		switch (uriType) {
		case ContentUri.MATCH_HISTORY:

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

		SQLiteDatabase db = mHistoryDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		int rowsUpdated = 0;

		switch (uriType) {
		case ContentUri.MATCH_HISTORY:

			rowsUpdated = db.update(table.getTableName(), values, selection,
					selectionArgs);

			break;
		case ContentUri.MATCH_HISTORY_ITEM:

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

		SQLiteDatabase db = mHistoryDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		int rowsDeleted = 0;

		switch (uriType) {
		case ContentUri.MATCH_HISTORY:

			rowsDeleted = db.delete(table.getTableName(), selection,
					selectionArgs);

			break;
		case ContentUri.MATCH_HISTORY_ITEM:

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
		case ContentUri.MATCH_HISTORY:
		case ContentUri.MATCH_HISTORY_ITEM:
			return HistoryDatabase.TABLES[HistoryDatabase.TABLE_HISTORY];
		}

		return null;
	}

	static class HistoryDatabase extends SQLiteOpenHelper {

		public static final String DATABASE_NAME = "history";
		public static final int DATABASE_VERSION = 1;

		static final SQLiteTable[] TABLES = new SQLiteTable[] { new HistoryTable() };
		static final int TABLE_HISTORY = 0;
		
		HistoryDatabase(Context context) {
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
	
	static class HistoryTable extends SQLiteTable {

		public static final String TABLE_NAME = "history";
		public static final String[] COLUMNS = { COLUMN_ID, COLUMN_INTENT, COLUMN_TIME_START, COLUMN_TIME_END, COLUMN_STATE, COLUMN_LOG };
		
		static final String SQL_CREATE_TABLE = "create table "
				+ TABLE_NAME 
				+ "(" 
				+ COLUMN_ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_INTENT		+ " TEXT NOT NULL, "
				+ COLUMN_TIME_START	+ " INTEGER, "
				+ COLUMN_TIME_END	+ " INTEGER, "
				+ COLUMN_STATE		+ " INTEGER, "
				+ COLUMN_LOG		+ " TEXT NOT NULL"
				+ ");";
		
		public HistoryTable() {
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
