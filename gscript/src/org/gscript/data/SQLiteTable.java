package org.gscript.data;

import android.database.sqlite.SQLiteDatabase;

public abstract class SQLiteTable {
	
	public static final String LOG_TAG = "SQLiteTable";
	
	private String mName;
	
	public SQLiteTable(String name) {
		mName = name;
	}
	
	public String getTableName() {
		return mName;
	}
	
	abstract void onCreate(SQLiteDatabase db);
	abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
	abstract String getPrimaryKeyField();
	
}
