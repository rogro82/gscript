package org.gscript.view;

import java.util.Properties;

import android.content.Context;
import android.content.Intent;
import android.widget.LinearLayout;

public abstract class LibraryPropertiesView extends LinearLayout {

	private Properties mProperties;
	private LibraryPropertiesListener mListener;
	
	public LibraryPropertiesView(Context context, Properties properties, LibraryPropertiesListener listener) {
		super(context);
		
		mListener = listener;
		mProperties = properties;
	}
	
	public void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
	}
	
	public Properties getProperties() {
		return mProperties;
	}
	
	public void notifyPropertiesChanged(boolean valid) {
		mListener.OnPropertiesValidChanged(this, valid);
	}
	
	public abstract boolean hasValidProperties();
	
	public interface LibraryPropertiesListener
	{
		public void OnPropertiesValidChanged(LibraryPropertiesView view, boolean valid);
	}
}
