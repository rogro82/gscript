package org.gscript.data.library;

import org.gscript.data.KeyValueMap;

import android.database.Cursor;

public class ItemAttributes extends KeyValueMap {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4927888736321990735L;

	public static final String ATTRIBUTE_SHELL = "shell";
	public static final String ATTRIBUTE_UNATTENDED = "unattended";
	public static final String ATTRIBUTE_WAKE_LOCK = "wake_lock";
	public static final String ATTRIBUTE_WIFI_WAKE_LOCK = "wifi_wake_lock";
	
	public ItemAttributes() { };
	
	public ItemAttributes(Cursor cursor) {
		super(cursor);
	}

}
