package org.gscript.data.library;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class LibraryItem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5951889942568184160L;

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_LIBRARY = "library";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_PATH = "path";
	public static final String COLUMN_PERMISSIONS = "permissions";
	public static final String COLUMN_CONTENT = "content";
	public static final String COLUMN_MODDATE = "moddate";
	public static final String[] COLUMNS = { COLUMN_ID, COLUMN_LIBRARY,
			COLUMN_TYPE, COLUMN_PATH, COLUMN_PERMISSIONS, COLUMN_CONTENT,
			COLUMN_MODDATE };

	public static final int TYPE_SCRIPT = 0;
	public static final int TYPE_FOLDER = 1;

	public static final int PERMISSION_ADD = 0x1;
	public static final int PERMISSION_EDIT = 0x2;
	public static final int PERMISSION_DELETE = 0x4;

	public static final long DATE_UNKNOWN = 0;

	int library;
	int type;
	String path;
	int permissions;
	String content;
	long moddate;

	private LibraryItem() {
	}

	public LibraryItem(Library library, int type, String path) {
		this.library = library.getId();
		this.type = type;
		this.path = path;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setPermissions(boolean add, boolean edit, boolean delete) {
		permissions = 0;
		permissions |= (add) ? PERMISSION_ADD : 0;
		permissions |= (edit) ? PERMISSION_EDIT : 0;
		permissions |= (delete) ? PERMISSION_DELETE : 0;
	}

	public void setModifiedDate(long date) {
		moddate = date;
	}

	public String getName() {
		Uri path = Uri.parse(this.path);

		return path.getLastPathSegment();
	}

	public String getPath() {
		return getPath(false);
	}

	public String getPath(boolean appendPathSeperator) {
		if (appendPathSeperator && type == TYPE_FOLDER)
			return this.path + "/";

		return path;
	}

	public int getLibraryId() {
		return this.library;
	}

	public boolean hasContent() {
		return (this.content != null && this.content.length() > 0);
	}

	public String getContent() {
		return (this.content != null) ? this.content : "";
	}

	public int getType() {
		return this.type;
	}

	public int getPermissions() {
		return this.permissions;
	}

	public boolean allowAdd() {
		return ((this.permissions & PERMISSION_ADD) != 0);
	}

	public boolean allowEdit() {
		return ((this.permissions & PERMISSION_EDIT) != 0);
	}

	public boolean allowDelete() {
		return ((this.permissions & PERMISSION_DELETE) != 0);
	}

	public long getModifiedDate() {
		return this.moddate;
	}

	public Object[] toObject() {
		return this.toObject(0);
	}

	public Object[] toObject(int _id) {
		return new Object[] { _id, library, type, path, permissions, content,
				moddate };
	}

	public MatrixCursor toCursor() {

		MatrixCursor itemCursor = new MatrixCursor(COLUMNS);
		itemCursor.addRow(this.toObject());

		return itemCursor;
	}

	public static LibraryItem fromCursor(Cursor c) {

		LibraryItem item = null;

		if (c != null) {

			int libraryId = c.getInt(c.getColumnIndex(COLUMN_LIBRARY));
			String path = c.getString(c.getColumnIndex(COLUMN_PATH));
			int perms = c.getInt(c.getColumnIndex(COLUMN_PERMISSIONS));
			int type = c.getInt(c.getColumnIndex(COLUMN_TYPE));
			String content = c.getString(c.getColumnIndex(COLUMN_CONTENT));
			long moddate = c.getLong(c.getColumnIndex(COLUMN_MODDATE));

			if (path != null) {

				item = new LibraryItem();

				item.library = libraryId;
				item.path = path;
				item.permissions = perms;
				item.type = type;
				item.content = content;
				item.moddate = moddate;
			}
		}
		return item;
	}

	public static MatrixCursor emptyCursor() {
		return new MatrixCursor(COLUMNS);
	}

	public static MatrixCursor toCursor(ArrayList<LibraryItem> items) {
		MatrixCursor itemCursor = new MatrixCursor(COLUMNS);

		int idx = 0;

		if (items != null)
			for (LibraryItem item : items) {
				itemCursor.addRow(item.toObject(idx));
				idx++;
			}

		return itemCursor;
	}

	public static boolean serializeItems(ArrayList<LibraryItem> items,
			String filename) {

		try {
			OutputStream file = new FileOutputStream(filename);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(items);
			} finally {
				output.close();
				buffer.close();
				file.close();
			}
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	public static boolean deserializeItems(ArrayList<LibraryItem> items,
			String filename) {

		try {
			// use buffering
			InputStream file = new FileInputStream(filename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				@SuppressWarnings("unchecked")
				ArrayList<LibraryItem> deserializedItems = (ArrayList<LibraryItem>) input
						.readObject();
				items.addAll(deserializedItems);
			} finally {
				input.close();
				buffer.close();
				file.close();
			}
		} catch (ClassNotFoundException ex) {
			return false;
		} catch (IOException ex) {
			return false;
		}
		
		return true;
	}

	public static Comparator<LibraryItem> BaseComparator = new Comparator<LibraryItem>() {

		public int compare(LibraryItem item1, LibraryItem item2) {

			Integer type1 = item1.getType();
			Integer type2 = item2.getType();

			if (item1.getType() != item2.getType()) {
				/* compare type in reverse order */
				return type2.compareTo(type1);

			} else {

				String name1 = item1.getName().toUpperCase(Locale.getDefault());
				String name2 = item2.getName().toUpperCase(Locale.getDefault());

				return name1.compareTo(name2);
			}
		};

	};
}
