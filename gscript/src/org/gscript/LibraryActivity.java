package org.gscript;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.gscript.data.ContentUri;
import org.gscript.data.LibraryProvider;
import org.gscript.data.library.LibraryAttribute;
import org.gscript.view.LibraryPropertiesView;
import org.gscript.view.LibraryPropertiesView.LibraryPropertiesListener;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ScrollView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class LibraryActivity extends SherlockActivity implements
		LibraryPropertiesView.LibraryPropertiesListener {

	static final String LOG_TAG = "LibraryActivity";
	static final String ACTION_ADD = "org.gscript.LibraryActivity.action_add";
	static final String ACTION_EDIT = "org.gscript.LibraryActivity.action_edit";
	static final String EXTRA_LIBRARY_TYPE = "type";
	static final String EXTRA_LIBRARY_ID = "id";

	String mIntentAction;
	String mLibraryType;
	int mLibraryId;
	boolean mValid;

	EditText mEditTextName;
	MenuItem mSaveItem;
	ScrollView mPropertiesPlaceholder;

	LibraryPropertiesView mPropertiesView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_library);

		Intent intent = this.getIntent();

		mIntentAction = intent.getAction();
		mLibraryType = intent.getStringExtra(EXTRA_LIBRARY_TYPE);
		mLibraryId = intent.getIntExtra(EXTRA_LIBRARY_ID, 0);

		mEditTextName = (EditText) this.findViewById(R.id.editTextName);

		if (ACTION_ADD.equals(mIntentAction)) {

			this.setTitle(R.string.library_new);

			mPropertiesView = getPropertiesView(mLibraryType, new Properties());

		} else {

			Cursor c = this
					.getContentResolver()
					.query(Uri.withAppendedPath(ContentUri.URI_LIBRARY,
							String.valueOf(mLibraryId)), null, null, null, null);
			if (c.moveToFirst()) {

				mLibraryType = c.getString(c
						.getColumnIndex(LibraryProvider.COLUMN_TYPE));

				String libraryName = c.getString(c
						.getColumnIndex(LibraryProvider.COLUMN_TITLE));
				mEditTextName.setText(libraryName);

				setTitle(String.format("Edit '%s'", libraryName));

				byte[] blob = c.getBlob(c
						.getColumnIndex(LibraryProvider.COLUMN_PROPS));
				ByteArrayInputStream is = new ByteArrayInputStream(blob);

				Properties props = new Properties();
				try {
					props.load(is);
				} catch (IOException e) {
				}

				mPropertiesView = getPropertiesView(mLibraryType, props);
			}
			c.close();
		}

		mPropertiesPlaceholder = (ScrollView) this
				.findViewById(R.id.placeholderProperties);

		if (mPropertiesView != null) {

			mPropertiesPlaceholder.addView(mPropertiesView,
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		} else {
			/* should never happen */

			this.finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.library, menu);

		mSaveItem = menu.findItem(R.id.menu_library_save);
		mSaveItem.setEnabled(mValid);

		return true;
	}
	
	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		
		if(mPropertiesView != null)
			mPropertiesView.onActivityResult(requestCode, resultCode, data);
	}	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_library_save:

			String libraryName = mEditTextName.getEditableText().toString();
			if (libraryName.length() == 0)
				libraryName = "unnamed";

			if (mIntentAction.equals(ACTION_ADD)) {

				/* insert library */
				ByteArrayOutputStream propstream = new ByteArrayOutputStream();
				Properties properties = mPropertiesView.getProperties();

				try {
					properties.store(propstream, "");
				} catch (IOException e) {
					e.printStackTrace();
				}

				ContentValues val = new ContentValues();

				val.put(LibraryProvider.COLUMN_TITLE, libraryName);
				val.put(LibraryProvider.COLUMN_TYPE, mLibraryType);
				val.put(LibraryProvider.COLUMN_PROPS, propstream.toByteArray());

				getContentResolver().insert(ContentUri.URI_LIBRARY, val);

				this.finish();

			} else {

				/* update library */

				ByteArrayOutputStream propstream = new ByteArrayOutputStream();
				Properties properties = mPropertiesView.getProperties();

				try {
					properties.store(propstream, "");
				} catch (IOException e) {
					e.printStackTrace();
				}

				ContentValues val = new ContentValues();

				val.put(LibraryProvider.COLUMN_TITLE, libraryName);
				val.put(LibraryProvider.COLUMN_TYPE, mLibraryType);
				val.put(LibraryProvider.COLUMN_PROPS, propstream.toByteArray());

				getContentResolver().update(
						Uri.withAppendedPath(ContentUri.URI_LIBRARY,
								String.valueOf(mLibraryId)), val, null, null);

				this.finish();
			}

			return true;

		default:
			this.finish();
		}

		return true;
	}

	@Override
	public void OnPropertiesValidChanged(LibraryPropertiesView view,
			boolean valid) {

		mValid = valid;

		if (mSaveItem != null)
			mSaveItem.setEnabled(mValid);

	}

	LibraryPropertiesView getPropertiesView(String libraryType,
			Properties properties) {

		LibraryPropertiesView view = null;

		try {

			Class<?> libraryClass = Class.forName(libraryType);
			LibraryAttribute attribute = libraryClass
					.getAnnotation(LibraryAttribute.class);

			if (attribute != null) {

				Constructor<? extends LibraryPropertiesView> c = attribute
						.view().getConstructor(Context.class, Properties.class,
								LibraryPropertiesListener.class);

				view = c.newInstance(this, properties, this);
			}

		} catch (ClassNotFoundException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}

		return view;
	}

}
