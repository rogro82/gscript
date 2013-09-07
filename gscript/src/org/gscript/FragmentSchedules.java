package org.gscript;

import org.gscript.data.ContentUri;
import org.gscript.data.ScheduleProvider;
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

public class FragmentSchedules extends GenericListFragment implements
		LoaderCallbacks<Cursor> {

	static final String LOG_TAG = FragmentSchedules.class.getSimpleName();

	static final int LOADER_SCHEDULE_CURSOR = 200;
	ScheduleAdapter mScheduleAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mScheduleAdapter = new ScheduleAdapter(getActivity(), null, 0);
		initListFragment(R.layout.fragment_schedules, R.id.listViewSchedules, mScheduleAdapter);

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
				.restartLoader(LOADER_SCHEDULE_CURSOR, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_schedules, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_schedules_add:

			Intent addIntent = new Intent(getActivity(), ScheduleActivity.class);
			addIntent.setAction(ScheduleActivity.ACTION_ADD);
			getActivity().startActivity(addIntent);			
			
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		Intent schedulesIntent = (Intent) view.getTag();
		getActivity().startActivity(schedulesIntent);
	}

	@Override
	public void onCreateActionMode(ActionMode mode, Menu menu) {

		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.fragment_schedules_context, menu);
		
	}

	@Override
	public void onPrepareActionMode(ActionMode mode, Menu menu) {

		int checkedItems = getCheckedItemCount();

		String title = getSherlockActivity().getResources().getString(
				R.string.menu_schedule_context_title);

		mode.setTitle(String.format(title,
				checkedItems));
	
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

		long[] ids = getCheckedItemIds();

		switch (item.getItemId()) {

		case R.id.menu_schedules_context_edit:

			/* edit checked schedule item */

			Intent editIntent = new Intent(getActivity(), ScheduleActivity.class);

			int scheduleId = (int) ids[0];

			editIntent.setAction(ScheduleActivity.ACTION_EDIT);
			editIntent.putExtra(ScheduleActivity.EXTRA_SCHEDULE_ID, scheduleId);

			getActivity().startActivity(editIntent);

			mode.finish();
			return true;

		case R.id.menu_schedules_context_remove:

			/* remove checked schedule items */
			for (long id : ids) {
				Uri itemUri = Uri.withAppendedPath(ContentUri.URI_SCHEDULE, "/"
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
		case LOADER_SCHEDULE_CURSOR:
			
			return new CursorLoader(getSherlockActivity(),
					ContentUri.URI_SCHEDULE, null, null, null,
					ScheduleProvider.COLUMN_ID + " DESC");
			
		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		switch (loader.getId()) {
		case LOADER_SCHEDULE_CURSOR:

			mScheduleAdapter.swapCursor(cursor);
			restoreCheckedItems();
			
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case LOADER_SCHEDULE_CURSOR:
			mScheduleAdapter.swapCursor(null);
			break;
		}
	}

	class ScheduleAdapter extends CursorAdapter {

		public ScheduleAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			
			int id = cursor.getInt(cursor.getColumnIndex(ScheduleProvider.COLUMN_ID));

			String title = cursor.getString(cursor
					.getColumnIndex(ScheduleProvider.COLUMN_TITLE));
			
			TextView textViewTitle = (TextView) view
					.findViewById(R.id.list_item_title);
			
			textViewTitle.setText(title);
			
			Intent editIntent = new Intent(context, ScheduleActivity.class);
			editIntent.setAction(ScheduleActivity.ACTION_EDIT);
			editIntent.putExtra(ScheduleActivity.EXTRA_SCHEDULE_ID, id);
			
			view.setTag(editIntent);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			final LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.list_schedule, parent, false);

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
