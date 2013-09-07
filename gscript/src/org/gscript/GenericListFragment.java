package org.gscript;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public abstract class GenericListFragment extends SherlockFragment {

	static final String LOG_TAG = FragmentLibraries.class.getSimpleName();
	static final boolean DEBUG = false;
	ListAdapter mListAdapter;
	
	ActionMode mActionMode;
	View mFragmentView;
	ListView mListView;
	
	int mViewId;
	int mListId;
	
	int mChoiceMode = ListView.CHOICE_MODE_NONE;
	long[] mSavedCheckedIds;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mFragmentView = inflater.inflate(mViewId, container,
				false);

		mListView = ((ListView) mFragmentView.findViewById(mListId));

		mListView.setAdapter(mListAdapter);
		mListView.setChoiceMode(mChoiceMode);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				if (mActionMode != null) {
					/* invalidate to update title count */
					mActionMode.invalidate();
					/* finish action mode if there is no selection */
					if (getCheckedItemCount() == 0)
						mActionMode.finish();

				} else {
					/* handle as normal click event */
					GenericListFragment.this.onItemClick(parent, view, position, id);
				}
			}
		});
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {

				/* if null start the contextual action mode */
				if (mActionMode == null)
					getSherlockActivity().startActionMode(
							mContextualCallback);

				if (mActionMode != null) {
					/* check long-pressed item */
					ListView lv = (ListView) parent;
					lv.setItemChecked(position, !lv.isItemChecked(position));

					/* invalidate to update title count */
					mActionMode.invalidate();
					/* finish action mode if there is no selection */
					if (getCheckedItemCount() == 0)
						mActionMode.finish();
				}

				return true;
			}
		});

		return mFragmentView;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);

		if (mActionMode != null && !isVisibleToUser) {
			/* invisible so hide the contextual actionbar */
			if(DEBUG)
			Log.d(LOG_TAG,
					"became unvisible while contextual -> finish actionmode");

			mActionMode.finish();
		}

		if (mActionMode == null && isVisibleToUser) {
			int checkedItems = getCheckedItemCount();
			if (checkedItems > 0) {

				/*
				 * visible and we have items checked so show contextual
				 * actionbar
				 */
				if(DEBUG)
				Log.d(LOG_TAG,
						String.format(
								"became visible while %d item(s) checked -> start actionmode",
								checkedItems));

				getSherlockActivity().startActionMode(
						mContextualCallback);

			}
		}
	}

	private ActionMode.Callback mContextualCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {

			mActionMode = mode;

			GenericListFragment.this.onCreateActionMode(mode, menu);

			final ListView view = getListView();

			if (view != null) {

				mChoiceMode = ListView.CHOICE_MODE_MULTIPLE;
				view.setChoiceMode(mChoiceMode);
				view.invalidateViews();

			}

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

			final ListView view = getListView();

			if (view != null) {
				GenericListFragment.this.onPrepareActionMode(mode, menu);
			}
			
			/* store checked items so that we can restore the selection after an adapter change
			 * eg in onLoadFinished (LoaderCallbacks<Cursor>) */
			
			saveCheckedItems();

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {

			final ListView view = getListView();

			if (view != null) {

				if (getUserVisibleHint()) {

					if(DEBUG)
					Log.d(LOG_TAG,
							"onDestroyActionMode while visible -> uncheck items");

					for (int i = 0; i < view.getChildCount(); i++) {
						/*
						 * manually uncheck views to make sure views are updated
						 * properly
						 */
						View v = view.getChildAt(i);
						if (v instanceof Checkable) {
							((Checkable) v).setChecked(false);
						}
					}

					/* clear choices and invalidate all views */
					view.clearChoices();
				}

				mChoiceMode = ListView.CHOICE_MODE_NONE;
				view.setChoiceMode(mChoiceMode);
				view.invalidateViews();
			}

			mActionMode = null;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return GenericListFragment.this.onActionItemClicked(mode, item);
		}
	};

	public void initListFragment(int viewResource, int listResource, ListAdapter adapter) {
		this.mViewId = viewResource;
		this.mListId = listResource;
		this.mListAdapter = adapter;
	}
	
	public void setListAdapter(ListAdapter adapter) {
		mListAdapter = adapter;
	}
	
	public ListAdapter getListAdapter() {
		return mListAdapter;
	}

	int getCheckedItemCount() {
		/* support SDK < 11 */

		int itemCount = 0;

		final ListView view = getListView();

		if (view != null) {
			/* switch to multiple choice so that we get the correct item count */
			int prevMode = view.getChoiceMode();
			view.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

			itemCount = view.getCheckedItemIds().length;
			/* restore previous choicemode */
			view.setChoiceMode(prevMode);
		}

		return itemCount;
	}

	long[] getCheckedItemIds() {

		long[] checkedIds = null;

		final ListView view = getListView();

		if (view != null) {
			/* switch to multiple choice so that we get the correct item count */
			int prevMode = view.getChoiceMode();
			view.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

			checkedIds = view.getCheckedItemIds();
			/* restore previous choicemode */
			view.setChoiceMode(prevMode);
		}

		return checkedIds;
	}

	void saveCheckedItems() {
		mSavedCheckedIds = getCheckedItemIds();
	}

	void restoreCheckedItems() {

		final ListView view = this.getListView();

		if ((mSavedCheckedIds != null && mSavedCheckedIds.length > 0)
				&& view != null && mListAdapter != null) {

			ArrayList<Long> positions = new ArrayList<Long>();
			int count = mListAdapter.getCount();

			for (int pos = 0; pos < count; ++pos)
				positions.add(mListAdapter.getItemId(pos));

			for (int idx = 0; idx < mSavedCheckedIds.length; ++idx) {
				
				int pos = -1;
				for(int y=0; y < positions.size(); ++y) {
					if(positions.get(y).longValue()==mSavedCheckedIds[idx]) {
						pos = y;
						break;
					}
				}
				
				if (pos >= 0)
					view.setItemChecked(pos, true);
			}

			if (mActionMode != null)
				mActionMode.invalidate();
		}
	}

	View getFragmentView() {
		return mFragmentView;
	}
	
	ListView getListView() {
		return mListView;
	}	
	
	public abstract void onItemClick(AdapterView<?> parent, View view, int position, long id);
	public abstract void onCreateActionMode(ActionMode mode, Menu menu);
	public abstract void onPrepareActionMode(ActionMode mode, Menu menu);
	public abstract boolean onActionItemClicked(ActionMode mode, MenuItem item);
	
	
}
