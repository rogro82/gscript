package org.gscript.data.library;

import org.gscript.data.KeyValueMap;

import android.database.Cursor;

public class ItemConditions extends KeyValueMap {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7205849562784420648L;

	public static final String CONDITION_BOOT = "boot";
	public static final String CONDITION_SCHEDULE = "schedule";
	
	public ItemConditions() { };
	
	public ItemConditions(Cursor cursor) {
		super(cursor);
	}
	
}
