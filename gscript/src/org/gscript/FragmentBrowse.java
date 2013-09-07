package org.gscript;

import org.gscript.data.ContentUri;
import org.gscript.data.library.Library;
import org.gscript.data.library.LibraryItem;
import org.gscript.process.ProcessService;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class FragmentBrowse extends GenericListFragment implements
		LoaderCallbacks<Cursor> {

	static final String LOG_TAG = FragmentLibraries.class.getSimpleName();
	static final String BUNDLE_LIBRARY_ID = "library_id";
	static final String BUNDLE_LIBRARY_PATH = "library_path";
	static final String BUNDLE_QUERY_FLAGS = "flags";

	static final int LOADER_LIBRARY_ITEM_CURSOR = 2;

	LibraryItemAdapter mLibraryItemAdapter;
	int mLibraryId;
	String mLibraryPath;

	MenuItem menuEdit;
	MenuItem menuShortcut;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = this.getArguments();

		mLibraryId = args.getInt(BUNDLE_LIBRARY_ID);
		mLibraryPath = args.getString(BUNDLE_LIBRARY_PATH);

		if (mLibraryPath != null)
			mLibraryItemAdapter = new LibraryItemAdapter(getActivity(), null, 0);

		initListFragment(R.layout.fragment_browse, R.id.listViewBrowse,
				mLibraryItemAdapter);
		
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

		this.getLoaderManager().restartLoader(LOADER_LIBRARY_ITEM_CURSOR, null,
				this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_browse, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Bundle bundle = new Bundle();
		bundle.putInt(BUNDLE_QUERY_FLAGS, Library.FLAG_MANUAL_REFRESH);

		switch (item.getItemId()) {
		case R.id.menu_browse_refresh:

			this.getLoaderManager().restartLoader(LOADER_LIBRARY_ITEM_CURSOR,
					bundle, this);

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		FragmentBrowse.FragmentBrowseListener listener = (FragmentBrowse.FragmentBrowseListener) FragmentBrowse.this
				.getActivity();

		LibraryItem item = (LibraryItem) view.getTag();

		switch (item.getType()) {
		case LibraryItem.TYPE_FOLDER:

			/* browse folder */
			listener.browseLibrary(
					ContentUri.URI_LIBRARY_PATH(item.getLibraryId(),
							item.getPath(true), 0), null);

			break;
		case LibraryItem.TYPE_SCRIPT:

			/* execute item */
			Intent processIntent = new Intent(getActivity(),
					ProcessService.class);

			processIntent.setAction(ProcessService.ACTION_EXECUTE);
			processIntent.setData(ContentUri.URI_LIBRARY_PATH(
					item.getLibraryId(), item.getPath()));

			getActivity().startService(processIntent);

			break;
		}
	}

	@Override
	public void onCreateActionMode(ActionMode mode, Menu menu) {

		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.fragment_browse_context, menu);

		menuEdit = menu.findItem(R.id.menu_browse_context_edit);
		menuShortcut = menu.findItem(R.id.menu_browse_context_shortcut);

	}

	@Override
	public void onPrepareActionMode(ActionMode mode, Menu menu) {

		int checkedItems = getCheckedItemCount();

		String title = getSherlockActivity().getResources().getString(
				R.string.menu_browse_context_title);

		mode.setTitle(String.format(title, checkedItems));

		menuEdit.setVisible(false);
		menuShortcut.setVisible(false);

		if (getCheckedItemCount() == 1) {

			int libraryId = (int) getCheckedItemIds()[0];

			Cursor cursor = (Cursor) mLibraryItemAdapter.getItem(libraryId);

			if (cursor != null) {

				LibraryItem libraryItem = LibraryItem.fromCursor(cursor);

				if (libraryItem.getType() == LibraryItem.TYPE_SCRIPT) {

					Uri itemUri = ContentUri.URI_LIBRARY_PATH(
							libraryItem.getLibraryId(), libraryItem.getPath());

					/* set edit intent */

					Intent editIntent = new Intent(getActivity(),
							LibraryItemActivity.class);

					editIntent.setAction(LibraryItemActivity.ACTION_EDIT);
					editIntent.putExtra(LibraryItemActivity.EXTRA_ITEM_PATH,
							itemUri);

					menuEdit.setVisible(true);
					menuEdit.setIntent(editIntent);

					/* set shortcut intent */

					// add the shortcut

					menuShortcut.setVisible(true);
					menuShortcut.setIntent(ExecuteDialog.createShortcutIntent(
							getActivity(), itemUri));
				}
			}
		}
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

		switch (item.getItemId()) {

		case R.id.menu_browse_context_edit:

			/* edit checked library item */
			Intent editIntent = item.getIntent();

			if (editIntent != null)
				getActivity().startActivity(editIntent);

			mode.finish();
			return true;

		case R.id.menu_browse_context_shortcut:

			Intent shortcutIntent = item.getIntent();

			if (shortcutIntent != null) {
				getActivity().sendBroadcast(shortcutIntent);

				Toast.makeText(getActivity(), "Desktop shortcut broadcasted",
						Toast.LENGTH_SHORT).show();
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
		case LOADER_LIBRARY_ITEM_CURSOR:

			int flags = Library.FLAG_INCLUDE_PERMISSIONS;
			if (bundle != null)
				flags |= bundle.getInt(BUNDLE_QUERY_FLAGS, 0);

			return new CursorLoader(getSherlockActivity(),
					ContentUri
							.URI_LIBRARY_PATH(mLibraryId, mLibraryPath, flags),
					null, null, null, null);

		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		switch (loader.getId()) {
		case LOADER_LIBRARY_ITEM_CURSOR:

			mLibraryItemAdapter.swapCursor(cursor);
			restoreCheckedItems();

			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_LIBRARY_ITEM_CURSOR:
			mLibraryItemAdapter.swapCursor(null);
			break;
		}
	}

	class LibraryItemAdapter extends CursorAdapter {

		public LibraryItemAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			final LibraryItem item = LibraryItem.fromCursor(cursor);
			view.setTag(item);

			ImageView imageViewIcon = (ImageView) view
					.findViewById(R.id.list_item_icon);

			switch (item.getType()) {
			case LibraryItem.TYPE_FOLDER:
				imageViewIcon.setImageResource(R.drawable.ic_folder);
				break;
			case LibraryItem.TYPE_SCRIPT:
				imageViewIcon.setImageResource(R.drawable.ic_item);
				break;
			}

			TextView textViewTitle = (TextView) view
					.findViewById(R.id.list_item_title);

			textViewTitle.setText(item.getName());
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			final LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.list_library_item, parent,
					false);

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

	public static interface FragmentBrowseListener {
		public void browseLibrary(Uri path, Bundle extra);
	}
}
