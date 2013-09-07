package org.gscript.data;

import java.util.HashMap;

import org.gscript.data.LibraryProvider;

import android.database.Cursor;

public class KeyValueMap extends HashMap<String, String> {

	/**
	 * Simple key value map used for attributes and conditions
	 */
	
	private static final long serialVersionUID = 4927888736321990735L;

	public KeyValueMap() {
	}
	
	public KeyValueMap(KeyValueMap map) {
		this.putAll(map);
	}
	
	public KeyValueMap(Cursor c) {
		
		while(c.moveToNext()) {
			
			String key = c.getString(c.getColumnIndex(LibraryProvider.COLUMN_KEY));
			String value = c.getString(c.getColumnIndex(LibraryProvider.COLUMN_VALUE));
			
			if(key != null && value != null)
				this.put(key, value);
		}
	}
}
