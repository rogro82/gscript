package org.gscript;

import java.util.ArrayList;

import org.gscript.process.IProcessService;
import org.gscript.process.IServiceCallback;
import org.gscript.process.ProcessDescriptor;
import org.gscript.process.ProcessService;
import org.gscript.process.ProcessState;
import org.gscript.settings.ShellProfile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class FragmentProcess extends GenericListFragment {

	static final String LOG_TAG = FragmentProcess.class.getSimpleName();

	IProcessService mProcessService;
	ProcessAdapter mProcessAdapter;
	boolean mIsBound;
	Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHandler = new Handler();
		mProcessAdapter = new ProcessAdapter(getActivity());

		initListFragment(R.layout.fragment_active, R.id.listViewActive, mProcessAdapter);
		setHasOptionsMenu(true);

		/* bind service */
		bindProcessService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		/* unbind service */
		unbindProcessService();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_process, menu);
		
		/* add start sub-menu */

		MenuItem startProcess = menu.findItem(R.id.menu_process_start);
		SubMenu submenu = startProcess.getSubMenu();
		if (submenu != null) {

			ArrayList<String> profileKeys = ShellProfile.getKeys(this.getActivity());
			for(String profileKey : profileKeys) {
				
				String profileName = ShellProfile.getName(getActivity(), profileKey);
				
				MenuItem item = submenu.add(profileName);
				
				Intent processIntent = new Intent(getActivity(), ProcessService.class);
				processIntent.setAction(ProcessService.ACTION_START);
				processIntent.putExtra(ProcessService.EXTRA_PROFILE, profileKey);
				
				item.setIntent(processIntent);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_process_kill_active:
			/* kill all running processes */
			if (mProcessService != null) {

				try {
					mProcessService.requestKillProcesses(mServiceCallback);
				} catch (RemoteException e) {
				}
			}

			return true;

		default:
			
			if(item.getIntent() != null && ProcessService.ACTION_START.equals(item.getIntent().getAction())) {
				getActivity().startService(item.getIntent());
				return true;
			}
			
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		ProcessDescriptor pd = (ProcessDescriptor) parent.getAdapter().getItem(
				position);
		
		startActivity(pd.getActivityIntent(getActivity()));
	}

	@Override
	public void onCreateActionMode(ActionMode mode, Menu menu) {

		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.fragment_process_context, menu);

	}

	@Override
	public void onPrepareActionMode(ActionMode mode, Menu menu) {

		int checkedItems = getCheckedItemCount();

		String title = getSherlockActivity().getResources().getString(
				R.string.menu_process_context_title);

		mode.setTitle(String.format(title, checkedItems));
		
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

		switch (item.getItemId()) {

		case R.id.menu_process_context_kill:

			/* stop checked process items */

			long[] ids = getCheckedItemIds();
			for (long id : ids) {

				/* send kill request to selected process(es) */

				Log.d(LOG_TAG, "stop process with id " + id);

				ProcessDescriptor pd = mProcessAdapter.getItemForId((int) id);
				if (pd != null)
					try {

						mProcessService.requestKillProcess(pd);

					} catch (RemoteException e) {
					}

			}
			mode.finish();
			return true;

		default:
			mode.finish();
			return true;
		}

	}

	class ProcessAdapter extends BaseAdapter {

		ProcessDescriptor[] pds;
		Context context;

		public ProcessAdapter(Context context) {
			this.context = context;
		}

		public void set(ProcessDescriptor[] pds) {
			this.pds = pds;
			this.notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ProcessDescriptor pd = (ProcessDescriptor) getItem(position);

			View view = convertView;

			if (view == null) {

				LayoutInflater inflater;
				inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.list_active, null);

			} else {
				if (view instanceof Checkable) {
					Checkable checkable = (Checkable) view;
					checkable.setChecked(((ListView) parent)
							.isItemChecked(position));
				}
			}

			if (pd != null) {

				TextView textViewTitle = (TextView) view
						.findViewById(R.id.list_item_title);

				textViewTitle.setText(pd.getIntent().getData()
						.getLastPathSegment());

			}

			return view;
		}

		@Override
		public int getCount() {
			return (pds != null ? pds.length : 0);
		}

		@Override
		public Object getItem(int position) {
			return (pds != null ? pds[position] : null);
		}

		public ProcessDescriptor getItemForId(int id) {

			if (pds == null)
				return null;

			for (ProcessDescriptor pd : pds)
				if (pd.hashCode() == id)
					return pd;

			return null;
		}

		@Override
		public long getItemId(int position) {
			return ((pds != null && position < pds.length) ? pds[position]
					.hashCode() : -1);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

	}

	void reloadProcesses() {
		this.mHandler.post(mReloadRunnable);
	}

	Runnable mReloadRunnable = new Runnable() {
		@Override
		public void run() {

			try {

				if (mProcessService != null) {

					ProcessDescriptor[] pds = mProcessService.getProcesses(
							mServiceCallback, ProcessState.STATE_RUNNING);

					mProcessAdapter.set(pds);
					
					restoreCheckedItems();
				}

			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	};

	IServiceCallback.Stub mServiceCallback = new IServiceCallback.Stub() {

		@Override
		public void OnProcessStateChanged(ProcessDescriptor pd, int state)
				throws RemoteException {

			reloadProcesses();
		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			mProcessService = IProcessService.Stub.asInterface(service);

			try {

				if (mProcessService != null)
					mProcessService.registerServiceCallback(mServiceCallback);

				reloadProcesses();

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mProcessService = null;
		}
	};
	
	void bindProcessService() {

		this.getActivity().bindService(
				new Intent(getActivity(), ProcessService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);

		mIsBound = true;
	}

	void unbindProcessService() {

		if (mIsBound) {

			try {
				if (mProcessService != null)
					mProcessService.unregisterServiceCallback(mServiceCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			this.getActivity().unbindService(mServiceConnection);

			mIsBound = false;
		}
	}
}
