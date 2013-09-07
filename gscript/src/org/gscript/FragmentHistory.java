package org.gscript;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.gscript.data.ContentUri;
import org.gscript.data.HistoryProvider;
import org.gscript.process.ProcessState;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
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

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class FragmentHistory extends GenericListFragment implements
		LoaderCallbacks<Cursor> {

	static final String LOG_TAG = FragmentHistory.class.getSimpleName();

	static final int LOADER_HISTORY_CURSOR = 100;
	HistoryAdapter mHistoryAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHistoryAdapter = new HistoryAdapter(getActivity(), null, 0);
		initListFragment(R.layout.fragment_history, R.id.listViewHistory, mHistoryAdapter);

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

		this.getLoaderManager()
				.restartLoader(LOADER_HISTORY_CURSOR, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_history, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_history_clear:
			getActivity().getContentResolver().delete(ContentUri.URI_HISTORY,
					null, null);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		Intent historyIntent = (Intent) view.getTag();
		getActivity().startActivity(historyIntent);
	}

	@Override
	public void onCreateActionMode(ActionMode mode, Menu menu) {

		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.fragment_history_context, menu);
		
	}

	@Override
	public void onPrepareActionMode(ActionMode mode, Menu menu) {

		int checkedItems = getCheckedItemCount();

		String title = getSherlockActivity().getResources().getString(
				R.string.menu_history_context_title);

		mode.setTitle(String.format(title,
				checkedItems));
	
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

		switch (item.getItemId()) {

		case R.id.menu_history_context_remove:

			/* remove checked history items */

			final ListView view = getListView();

			if (view != null) {
				long[] ids = view.getCheckedItemIds();
				for (long id : ids) {
					Uri itemUri = Uri.withAppendedPath(
							ContentUri.URI_HISTORY, "/" + id);
					getActivity().getContentResolver().delete(itemUri,
							null, null);
				}
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
		case LOADER_HISTORY_CURSOR:
			
			return new CursorLoader(getSherlockActivity(),
					ContentUri.URI_HISTORY, null, null, null,
					HistoryProvider.COLUMN_ID + " DESC");
			
		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		switch (loader.getId()) {
		case LOADER_HISTORY_CURSOR:

			mHistoryAdapter.swapCursor(cursor);
			restoreCheckedItems();
			
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_HISTORY_CURSOR:
			mHistoryAdapter.swapCursor(null);
			break;
		}
	}

	class HistoryAdapter extends CursorAdapter {

		public HistoryAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			String intent = cursor.getString(cursor
					.getColumnIndex(HistoryProvider.COLUMN_INTENT));
			
			int id = cursor.getInt(cursor.getColumnIndex(HistoryProvider.COLUMN_ID));
			int state = cursor.getInt(cursor.getColumnIndex(HistoryProvider.COLUMN_STATE));
			
			ImageView imageViewIcon = (ImageView) view.findViewById(R.id.list_item_icon);
			
			long start = cursor.getLong(cursor.getColumnIndex(HistoryProvider.COLUMN_TIME_START));
			
			Date startDate = new Date(start);

			SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
			SimpleDateFormat time_format = new SimpleDateFormat("HH:mm", Locale.getDefault());
			
			TextView textViewDate = (TextView) view.findViewById(R.id.list_item_when_date);
			textViewDate.setText(date_format.format(startDate));
			
			TextView textViewTime = (TextView) view.findViewById(R.id.list_item_when_time);
			textViewTime.setText(time_format.format(startDate));
			
			if(state >= ProcessState.STATE_ERROR) {
				imageViewIcon.setColorFilter(Color.RED);				
			} else {
				imageViewIcon.clearColorFilter();
			}

			TextView textViewTitle = (TextView) view
					.findViewById(R.id.list_item_title);
			
			Intent historyIntent = new Intent(context, HistoryActivity.class);
			historyIntent.setData(Uri.withAppendedPath(ContentUri.URI_HISTORY, String.valueOf(id)));
			
			view.setTag(historyIntent);

			try {

				Intent processIntent = Intent.parseUri(intent, 0);
				textViewTitle.setText(processIntent.getData().getLastPathSegment());

			} catch (URISyntaxException e) {
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			final LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.list_history, parent, false);

			bindView(view, context, cursor);

			return view;
		}
		
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			
			mCursor.moveToPosition(position);
			
			if(view == null) {
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
