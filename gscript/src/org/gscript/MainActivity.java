package org.gscript;

import org.gscript.FragmentBrowse.FragmentBrowseListener;
import org.gscript.data.ContentUri;
import org.gscript.settings.ShellProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends TabbedPagerActivity implements
		FragmentBrowseListener {

	static final String LOG_TAG = "MainActivity";

	/* TODO: fix (eg make FragmentTab parcelable and store in instance state) */
	static FragmentTab[] items;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* check first use */
		{
			SharedPreferences prefs = getSharedPreferences(
					"org.gscript_preferences", Context.MODE_PRIVATE);
			
			if(prefs.getBoolean("first-use", true)) {
				/* add default profiles and update first-use */
				ShellProfile.addDefaultProfiles(this);
				prefs.edit().putBoolean("first-use", false).commit();
			}
		}
		
		/* setup tabbed view */
		{
			if (items == null)
				items = new FragmentTab[] {
						new FragmentTab(R.string.tab_library, new FragmentSet(
								FragmentLibraries.class, null)),
						new FragmentTab(R.string.tab_process, new FragmentSet(
								FragmentProcess.class, null)),
						new FragmentTab(R.string.tab_history, new FragmentSet(
								FragmentHistory.class, null)),
						new FragmentTab(R.string.tab_schedules, new FragmentSet(
									FragmentSchedules.class, null)),
								
			};

			setFragmentTabs(items);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_main_settings:
			
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
			
			return true;
		case R.id.menu_main_schedules:

			Intent scheduleIntent = new Intent(this, ScheduleActivity.class);
			scheduleIntent.setAction(ScheduleActivity.ACTION_ADD);
			
			startActivity(scheduleIntent);
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void browseLibrary(Uri path, Bundle extra) {

		Bundle bundle = (extra != null) ? extra : new Bundle();

		ContentUri.LibraryPathSegments segments = ContentUri.LibraryPathSegments
				.parse(path);

		bundle.putInt(FragmentBrowse.BUNDLE_LIBRARY_ID, segments.id);
		bundle.putString(FragmentBrowse.BUNDLE_LIBRARY_PATH, segments.path);

		TabsAdapter tabs = getFragmentTabsAdapter();
		tabs.changeTabFragment(0, new FragmentSet(FragmentBrowse.class, bundle));
	}

}
