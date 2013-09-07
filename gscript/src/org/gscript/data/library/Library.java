package org.gscript.data.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.gscript.view.LibraryPropertiesView;
import android.content.Context;
import android.util.Log;

@LibraryAttribute(title = "", description = "", version = 0, view = Library.LibraryStubView.class)
public abstract class Library {

	static final String LOG_TAG = "library";

	public static final int FLAG_INCLUDE_CONTENT = 0x1;
	public static final int FLAG_INCLUDE_PERMISSIONS = 0x2;
	public static final int FLAG_MANUAL_REFRESH = 0x4;
	
	private LibraryNotificationListener mListener;
	private Context mContext;
	private int mId;
	private Properties mProperties;

	public void create(Context context, int id, Properties properties,
			LibraryNotificationListener l) {

		mContext = context;
		mId = id;
		mProperties = properties;
		mListener = l;

		this.onCreate(context, properties);
	}

	public void destroy() {

		this.onDestroy();
	}

	public abstract void onCreate(Context context, Properties properties);

	public abstract void onDestroy();

	public int getId() {
		return mId;
	}

	public Properties getProperties() {
		return mProperties;
	}

	protected Context getContext() {
		return mContext;
	}

	public ArrayList<LibraryItem> query(String path, int flags) {
		ArrayList<LibraryItem> items = onQuery(path, flags);
		Collections.sort(items, LibraryItem.BaseComparator);
		
		return items;
	}

	public abstract ArrayList<LibraryItem> onQuery(String path, int flags);

	void notifyChange() {
		notifyChange("/");
	}
	
	void notifyChange(String path) {
		if (mListener != null)
			mListener.OnLibraryChanged(this, path);
	}

	public static interface LibraryNotificationListener {
		void OnLibraryChanged(Library library, String path);
	}

	public static Library forType(String type) {

		Library library = null;

		try {

			Class<?> clazz = Class.forName(type);
			library = (Library) clazz.newInstance();

		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}

		if (library == null)
			Log.d(LOG_TAG, String.format("No library for type %s", type));

		return library;
	}
	
	public static class LibraryStubView extends LibraryPropertiesView {
		public LibraryStubView(Context context, Properties properties,
				LibraryPropertiesListener listener) {
			super(context, properties, listener);
		}

		@Override
		public boolean hasValidProperties() {
			return false;
		}
	}
}