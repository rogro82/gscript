package org.gscript;

import org.gscript.data.ContentUri;
import org.gscript.data.LibraryProvider;
import org.gscript.data.library.LibraryAttribute;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class FragmentLibraries extends GenericListFragment implements
		LoaderCallbacks<Cursor> {

	static final String LOG_TAG = FragmentLibraries.class.getSimpleName();

	static final int LOADER_LIBRARIES_CURSOR = 1;

	LibraryAdapter mLibraryAdapter;
	String[] mLibraryClasses;
	MenuItem menuEdit;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLibraryClasses = getResources()
				.getStringArray(R.array.library_classes);

		mLibraryAdapter = new LibraryAdapter(getActivity(), null, 0);

		initListFragment(R.layout.fragment_libraries, R.id.listViewLibraries,
				mLibraryAdapter);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();

		/*
		 * restart loader in onResume because of a "bug" with LoaderManager +
		 * fragments in a viewpager which causes the adapter to not get updated
		 * correctly when the fragment is paused.
		 */

		this.getLoaderManager().restartLoader(LOADER_LIBRARIES_CURSOR, null,
				this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_libraries, menu);

		/* add-submenu */
		MenuItem addItem = menu.findItem(R.id.menu_libraries_add);
		SubMenu submenu = addItem.getSubMenu();
		if (submenu != null) {

			for (String libraryClassName : mLibraryClasses) {
				try {

					Class<?> clazz = Class.forName(libraryClassName);
					LibraryAttribute attribute = clazz
							.getAnnotation(LibraryAttribute.class);
					if (attribute != null) {
						MenuItem item = submenu.add(attribute.title());

						Intent libraryIntent = new Intent(this.getActivity(),
								LibraryActivity.class);

						libraryIntent.setAction(LibraryActivity.ACTION_ADD);
						libraryIntent.putExtra(
								LibraryActivity.EXTRA_LIBRARY_TYPE,
								libraryClassName);

						item.setIntent(libraryIntent);
					}

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_libraries_add:
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		FragmentBrowse.FragmentBrowseListener listener = (FragmentBrowse.FragmentBrowseListener) FragmentLibraries.this
				.getActivity();

		listener.browseLibrary(ContentUri.URI_LIBRARY_PATH((int) id, "/", 0),
				null);
	}

	@Override
	public void onCreateActionMode(ActionMode mode, Menu menu) {

		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.fragment_libraries_context, menu);

		menuEdit = menu.findItem(R.id.menu_libraries_context_edit);

	}

	@Override
	public void onPrepareActionMode(ActionMode mode, Menu menu) {

		int checkedItems = getCheckedItemCount();

		String title = getSherlockActivity().getResources().getString(
				R.string.menu_libraries_context_title);

		mode.setTitle(String.format(title, checkedItems));

		if (menuEdit != null) {
			menuEdit.setVisible((checkedItems == 1));
		}

	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

		long[] ids = getCheckedItemIds();

		switch (item.getItemId()) {

		case R.id.menu_libraries_context_edit:

			/* edit checked library item */

			Intent editIntent = new Intent(getActivity(), LibraryActivity.class);

			int libraryId = (int) ids[0];

			editIntent.setAction(LibraryActivity.ACTION_EDIT);
			editIntent.putExtra(LibraryActivity.EXTRA_LIBRARY_ID, libraryId);

			getActivity().startActivity(editIntent);

			mode.finish();
			return true;

		case R.id.menu_libraries_context_remove:

			/* remove checked library items */
			for (long id : ids) {
				Uri itemUri = Uri.withAppendedPath(ContentUri.URI_LIBRARY, "/"
						+ id);
				getActivity().getContentResolver().delete(itemUri, null, null);
			}
			mode.finish();
			return true;

		default:
			mode.finish();
			return true;
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

		switch (id) {
		case LOADER_LIBRARIES_CURSOR:

			return new CursorLoader(getSherlockActivity(),
					ContentUri.URI_LIBRARY, null, null, null,
					LibraryProvider.COLUMN_TITLE + " ASC");

		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		switch (loader.getId()) {
		case LOADER_LIBRARIES_CURSOR:

			mLibraryAdapter.swapCursor(cursor);
			restoreCheckedItems();

			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_LIBRARIES_CURSOR:
			mLibraryAdapter.swapCursor(null);
			break;
		}
	}

	class LibraryAdapter extends CursorAdapter {

		public LibraryAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			String title = cursor.getString(cursor
					.getColumnIndex(LibraryProvider.COLUMN_TITLE));

			TextView textViewTitle = (TextView) view
					.findViewById(R.id.list_item_title);

			textViewTitle.setText(title);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			final LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.list_library, parent, false);

			bindView(view, context, cursor);

			return view;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {

			mCursor.moveToPosition(position);

			if (view == null) {
				view = newView(mContext, mCursor, parent);
			} else {
				bindView(view, mContext, mCursor);
				if (view instanceof Checkable) {
					Checkable checkable = (Checkable) view;
					checkable.setChecked(((ListView) parent)
							.isItemChecked(position));
				}
			}

			return view;
		}
	}

}
