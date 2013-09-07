package org.gscript.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import org.gscript.data.ContentUri.LibraryPathSegments;
import org.gscript.data.library.Library;
import org.gscript.data.library.Library.LibraryNotificationListener;
import org.gscript.data.library.LibraryItem;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

public class LibraryProvider extends ContentProvider implements
		LibraryNotificationListener {

	static final String LOG_TAG = "LibraryProvider";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_PROPS = "props";
	public static final String COLUMN_LIBRARY = "library";
	public static final String COLUMN_PATH = "path";
	public static final String COLUMN_KEY = "key";
	public static final String COLUMN_VALUE = "value";

	LibraryDatabase mLibraryDatabase;
	SparseArray<Library> mLibraries = new SparseArray<Library>();

	@Override
	public boolean onCreate() {
		mLibraryDatabase = new LibraryDatabase(getContext());
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

		SQLiteTable table;

		switch (uriType) {
		case ContentUri.MATCH_LIBRARY:
		case ContentUri.MATCH_ITEM_CONDITIONS:
		case ContentUri.MATCH_ITEM_ATTRIBS:

			table = getTableForUriMatch(uriType);
			queryBuilder.setTables(table.getTableName());

			break;
		case ContentUri.MATCH_LIBRARY_ITEM:

			table = getTableForUriMatch(uriType);

			queryBuilder.setTables(table.getTableName());
			queryBuilder.appendWhere(table.getPrimaryKeyField() + "="
					+ uri.getLastPathSegment());

			break;
		case ContentUri.MATCH_LIBRARY_PATH:

			int flags = 0;

			try {

				String flagsQuery = uri
						.getQueryParameter(ContentUri.QUERY_FLAGS);
				if (flagsQuery != null) {
					flags = Integer.parseInt(flagsQuery);
				}

				LibraryPathSegments segments = LibraryPathSegments.parse(uri);

				/* check if library has already been loaded else load it now */
				Library library = getLibraryForId(segments.id);
				if (library != null) {

					Cursor cursor = LibraryItem.toCursor((library.query(
							segments.path, flags)));

					if (cursor != null) {

						Uri notificationUri = uri.buildUpon().clearQuery()
								.build();

						cursor.setNotificationUri(getContext()
								.getContentResolver(), notificationUri);

						return cursor;
					}
				}

			} catch (Exception e) {
				Log.e(LOG_TAG, "MATCH_LIBRARY_PATH: " + e.getMessage());
			}

			return LibraryItem.emptyCursor();

		case ContentUri.MATCH_ITEM_ATTRIBS_PATH:
		case ContentUri.MATCH_ITEM_CONDITIONS_PATH:

			LibraryPathSegments segments = LibraryPathSegments.parse(uri);

			table = getTableForUriMatch(uriType);

			queryBuilder.setTables(table.getTableName());

			queryBuilder.appendWhere("library" + "=" + segments.id);
			queryBuilder
					.appendWhere(" AND path" + "=\"" + segments.path + "\"");

			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = mLibraryDatabase.getReadableDatabase();

		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		int uriType = ContentUri.MATCHER.match(uri);

		SQLiteDatabase db = mLibraryDatabase.getWritableDatabase();
		SQLiteTable table = getTableForUriMatch(uriType);

		long id = 0;

		switch (uriType) {
		case ContentUri.MATCH_LIBRARY:
			id = db.insert(table.getTableName(), null, values);
			break;
		case ContentUri.MATCH_LIBRARY_PATH:
			/* TODO: handle insert of library item */
			break;
		case ContentUri.MATCH_ITEM_ATTRIBS_PATH:
		case ContentUri.MATCH_ITEM_CONDITIONS_PATH:

			LibraryPathSegments seg = LibraryPathSegments.parse(uri);
			values.put("library", seg.id);
			values.put("path", seg.path);

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

		SQLiteDatabase db = mLibraryDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		int rowsUpdated = 0;

		switch (uriType) {
		case ContentUri.MATCH_LIBRARY_ITEM:

			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {

				rowsUpdated = db.update(table.getTableName(), values,
						table.getPrimaryKeyField() + "=" + id, null);

			} else {

				rowsUpdated = db.update(table.getTableName(), values,
						table.getPrimaryKeyField() + "=" + id + " and "
								+ selection, selectionArgs);

			}

			/* unload library so that any property changes will be reflected */

			Library library = mLibraries.get(Integer.valueOf(id));
			if (library != null) {
				library.onDestroy();
				mLibraries.remove(library.getId());
			}

			break;
		case ContentUri.MATCH_LIBRARY_PATH:
			/* TODO: handle update of library item */
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

		SQLiteDatabase db = mLibraryDatabase.getWritableDatabase();

		SQLiteTable table = getTableForUriMatch(uriType);

		int rowsDeleted = 0;

		switch (uriType) {
		case ContentUri.MATCH_LIBRARY_ITEM:

			String id = uri.getLastPathSegment();

			if (TextUtils.isEmpty(selection)) {

				rowsDeleted = db.delete(table.getTableName(),
						table.getPrimaryKeyField() + "=" + id, null);

			} else {

				rowsDeleted = db.delete(table.getTableName(),
						table.getPrimaryKeyField() + "=" + id + " and "
								+ selection, selectionArgs);
			}

			if (rowsDeleted > 0) {

				/* remove all attributes and conditions */
				db.delete(ItemAttributesTable.TABLE_NAME, "library=?",
						new String[] { id });

				db.delete(ItemConditionsTable.TABLE_NAME, "library=?",
						new String[] { id });

				/* destroy library if loaded */
				Library library = mLibraries.get(Integer.valueOf(id));
				if (library != null) {
					library.onDestroy();
					mLibraries.remove(library.getId());
				}
			}

			break;
		case ContentUri.MATCH_LIBRARY_PATH:
			/* TODO: handle delete of library item */
			break;
		case ContentUri.MATCH_ITEM_ATTRIBS_PATH:
		case ContentUri.MATCH_ITEM_CONDITIONS_PATH:

			LibraryPathSegments segments = LibraryPathSegments.parse(uri);
			rowsDeleted = db
					.delete(table.getTableName(), "library=? and path=?",
							new String[] { String.valueOf(segments.id),
									segments.path });

			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowsDeleted;
	}

	SQLiteTable getTableForUriMatch(int uriType) {

		switch (uriType) {
		case ContentUri.MATCH_LIBRARY:
		case ContentUri.MATCH_LIBRARY_ITEM:
			return LibraryDatabase.TABLES[LibraryDatabase.TABLE_LIBRARY];
		case ContentUri.MATCH_ITEM_ATTRIBS:
		case ContentUri.MATCH_ITEM_ATTRIBS_PATH:
			return LibraryDatabase.TABLES[LibraryDatabase.TABLE_ITEM_ATTRIBS];
		case ContentUri.MATCH_ITEM_CONDITIONS:
		case ContentUri.MATCH_ITEM_CONDITIONS_PATH:
			return LibraryDatabase.TABLES[LibraryDatabase.TABLE_ITEM_CONDITIONS];
		}

		return null;
	}

	/* library */

	Library getLibraryForId(int id) {

		Library library = mLibraries.get(id);
		if (library == null) {

			/* try to load library */
			Uri uri = Uri.withAppendedPath(ContentUri.URI_LIBRARY,
					String.valueOf(id));
			Cursor c = this.query(uri, null, null, null, null);

			if (c.moveToFirst()) {

				String type = c.getString(c.getColumnIndex(COLUMN_TYPE));

				if ((library = Library.forType(type)) != null) {

					byte[] blob = c.getBlob(c.getColumnIndex(COLUMN_PROPS));
					ByteArrayInputStream is = new ByteArrayInputStream(blob);

					Properties props = new Properties();
					try {
						props.load(is);
					} catch (IOException e) {
						Log.e(LOG_TAG, "failed to load library properties");
					}

					library.create(this.getContext(), id, props, this);
					mLibraries.append(id, library);

				}
			} else {
				Log.d(LOG_TAG, String.format("no library with id %d", id));
			}
		}

		return library;
	}

	@Override
	public void OnLibraryChanged(Library library, String path) {

		Uri uri = Uri.withAppendedPath(ContentUri.URI_LIBRARY,
				String.valueOf(library.getId()) + "/" + path);

		getContext().getContentResolver().notifyChange(uri, null);
	}

	static class LibraryDatabase extends SQLiteOpenHelper {

		public static final String DATABASE_NAME = "library";
		public static final int DATABASE_VERSION = 1;

		static final SQLiteTable[] TABLES = new SQLiteTable[] {
				new LibraryTable(), new ItemAttributesTable(),
				new ItemConditionsTable() };

		static final int TABLE_LIBRARY = 0;
		static final int TABLE_ITEM_ATTRIBS = 1;
		static final int TABLE_ITEM_CONDITIONS = 2;

		LibraryDatabase(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			for (SQLiteTable table : TABLES) {
				table.onCreate(db);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			for (SQLiteTable table : TABLES) {
				table.onUpgrade(db, oldVersion, newVersion);
			}
		}

	}

	static class LibraryTable extends SQLiteTable {

		public static final String TABLE_NAME = "library";
		public static final String[] COLUMNS = { COLUMN_ID, COLUMN_TITLE,
				COLUMN_TYPE, COLUMN_PROPS };

		static final String SQL_CREATE_TABLE = "create table " + TABLE_NAME
				+ "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_TITLE + " TEXT NOT NULL, " + COLUMN_TYPE
				+ " TEXT NOT NULL, " + COLUMN_PROPS + " BLOB" + ");";

		public LibraryTable() {
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

	static class ItemAttributesTable extends SQLiteTable {

		public static final String TABLE_NAME = "attributes";
		public static final String[] COLUMNS = { COLUMN_ID, COLUMN_LIBRARY,
				COLUMN_PATH, COLUMN_KEY, COLUMN_VALUE };

		static final String SQL_CREATE_TABLE = "create table " + TABLE_NAME
				+ "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_LIBRARY + " INTEGER, " + COLUMN_PATH
				+ " TEXT NOT NULL, " + COLUMN_KEY + " TEXT NOT NULL, "
				+ COLUMN_VALUE + " TEXT NOT NULL" + ");";

		public ItemAttributesTable() {
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

	static class ItemConditionsTable extends SQLiteTable {

		public static final String TABLE_NAME = "conditions";
		public static final String[] COLUMNS = { COLUMN_ID, COLUMN_LIBRARY,
				COLUMN_PATH, COLUMN_KEY, COLUMN_VALUE };

		static final String SQL_CREATE_TABLE = "create table " + TABLE_NAME
				+ "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_LIBRARY + " INTEGER, " + COLUMN_PATH
				+ " TEXT NOT NULL, " + COLUMN_KEY + " TEXT NOT NULL, "
				+ COLUMN_VALUE + " TEXT NOT NULL" + ");";

		public ItemConditionsTable() {
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
