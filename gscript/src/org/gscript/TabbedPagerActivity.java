package org.gscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.view.MenuItem;

public class TabbedPagerActivity extends SherlockFragmentActivity {

	private TabsAdapter mTabsAdapter;
	private ViewPager mViewPager;

	public void setFragmentTabs(FragmentTab[] tabs) {

		mViewPager = new ViewPager(this);
		mTabsAdapter = new TabsAdapter(this, mViewPager, tabs);

		mViewPager.setAdapter(mTabsAdapter);
		mViewPager.setOnPageChangeListener(mTabsAdapter);
		mViewPager.setId(0x101);
		mViewPager.setOffscreenPageLimit(3);

		this.setContentView(mViewPager);
	}

	public TabsAdapter getFragmentTabsAdapter() {
		return mTabsAdapter;
	}

	protected class FragmentSet {

		public final Class<? extends Fragment> fragmentClass;
		public Bundle bundle;

		public FragmentSet(Class<? extends Fragment> fragmentClass, Bundle args) {
			this.fragmentClass = fragmentClass;
			this.bundle = (args != null) ? args : new Bundle();
		}

	}

	protected class FragmentTab {

		public final Stack<FragmentSet> stack = new Stack<FragmentSet>();
		public final int resTitle;

		public FragmentTab(int resTitle, FragmentSet frag) {

			this.stack.push(frag);
			this.resTitle = resTitle;

		}

		FragmentSet peek() {
			return stack.peek();
		}

		FragmentSet push(FragmentSet frag) {
			stack.push(frag);
			return frag;
		}

		boolean pop() {
			if (stack.size() > 1) {

				stack.pop();
				return true;
			}
			return false;
		}

		int stackSize() {
			return stack.size();
		}

	}

	class TabsAdapter extends FragmentStatePagerAdapter implements TabListener,
			ViewPager.OnPageChangeListener {

		private final SherlockFragmentActivity mActivity;
		private final ActionBar mActionBar;
		private final ViewPager mPager;

		private List<FragmentTab> mTabs = new ArrayList<FragmentTab>();

		public TabsAdapter(SherlockFragmentActivity activity, ViewPager pager,
				FragmentTab[] frags) {
			super(activity.getSupportFragmentManager());

			this.mActivity = activity;
			this.mActionBar = activity.getSupportActionBar();
			this.mPager = pager;

			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

			for (FragmentTab frag : frags) {

				Tab tab = mActionBar.newTab();
				tab.setText(frag.resTitle);
				tab.setTabListener(this);
				tab.setTag(frag);

				mTabs.add(frag);
				mActionBar.addTab(tab);
			}

			notifyDataSetChanged();
		}

		boolean dispatchBackEvent() {

			int current = mPager.getCurrentItem();
			if (mTabs.get(current).pop()) {
				notifyDataSetChanged();
				return true;
			}

			return false;
		}

		public void changeTabFragment(int pos, FragmentSet frag) {

			FragmentTab tab = mTabs.get(pos);
			tab.push(frag);

			notifyDataSetChanged();
			updateActionBar();
		}

		@Override
		public int getItemPosition(Object object) {

			Fragment frag = (Fragment) object;

			for (int i = 0; i < mPager.getChildCount(); ++i) {
				if (frag.getView().equals(mPager.getChildAt(i))) {
					/* check if we have changed */

					FragmentTab tab = mTabs.get(i);
					FragmentSet set = tab.peek();

					/* TODO: add stack identifier to arguments and use that to verify equality */
					if (set.fragmentClass.equals(frag.getClass())
							&& set.bundle.equals(frag.getArguments())) {
						
						return POSITION_UNCHANGED;
					}

				}
			}

			return POSITION_NONE;
		}

		@Override
		public Fragment getItem(int position) {

			final FragmentTab tabInfo = mTabs.get(position);

			FragmentSet state = tabInfo.stack.peek();

			return (Fragment) Fragment.instantiate(mActivity,
					state.fragmentClass.getName(), state.bundle);
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		public void onPageScrollStateChanged(int arg0) {
		}

		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		public void onPageSelected(int position) {

			mActionBar.setSelectedNavigationItem(position);

		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {

			FragmentTab frag = (FragmentTab) tab.getTag();

			for (int i = 0; i < mTabs.size(); i++) {
				if (mTabs.get(i) == frag) {
					mPager.setCurrentItem(i);
					updateActionBar();
				}
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

		public void updateActionBar() {

			int current = mViewPager.getCurrentItem();
			FragmentTab tab = mTabs.get(current);

			boolean enabled = (tab.stackSize() > 1);

			mActionBar.setHomeButtonEnabled(enabled);
			mActionBar.setDisplayHomeAsUpEnabled(enabled);

		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {

		if (!mTabsAdapter.dispatchBackEvent()) {
			super.onBackPressed();
		}

		mTabsAdapter.updateActionBar();
	}

}