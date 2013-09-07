package org.gscript.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.gscript.R;
import org.gscript.data.ContentUri;
import org.gscript.data.HistoryProvider;
import org.gscript.data.LibraryProvider;
import org.gscript.data.library.ItemAttributes;
import org.gscript.interop.InteropReceiver;
import org.gscript.jni.NativeSupport;
import org.gscript.settings.ShellProfile;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ProcessService extends Service implements ProcessStateListener {

	static final String LOG_TAG = "ProcessService";

	public static final String ACTION_START = "gscript.intent.action.START";
	public static final String ACTION_EXECUTE = "gscript.intent.action.EXECUTE";
	public static final String EXTRA_ATTRIBUTES = "attributes";
	public static final String EXTRA_PROFILE = "profile";

	ArrayList<ProcessTask> mTasks = new ArrayList<ProcessTask>();
	ArrayList<ProcessRegistration> mProcessRegistrations = new ArrayList<ProcessRegistration>();
	ArrayList<IServiceCallback> mServiceCallbacks = new ArrayList<IServiceCallback>();

	AtomicInteger mCurrentPid = new AtomicInteger(1);
	int mForeground = 0;
	
	InteropReceiver mInteropReceiver;
	
	boolean mServiceStarted;

	@Override
	public void onCreate() {

		if (!mServiceStarted)
			this.startService(new Intent(this, ProcessService.class));

		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(mInteropReceiver != null) {
			mInteropReceiver.stop();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (!mServiceStarted) {
			
			if (!NativeSupport.prepare(this)) {
				Log.e(LOG_TAG,
						"failed to prepare native support... additional binaries might be missing");
			}
			
			mInteropReceiver = new InteropReceiver(this);
			mInteropReceiver.start();

			mServiceStarted = true;
		}

		/* check if this start command has something for us to do */

		if (intent != null && ACTION_EXECUTE.equals(intent.getAction())
				&& intent.getData() != null) {

			/* start process */

			Intent processIntent = new Intent(intent);
			startProcess(processIntent, 0);
		}
		if(intent != null && ACTION_START.equals(intent.getAction()) && intent.hasExtra(EXTRA_PROFILE)) {

			/* start process */
			
			String profile = intent.getStringExtra(EXTRA_PROFILE);
			String profileName = ShellProfile.getName(this, profile);
			
			Intent processIntent = new Intent(intent);
			processIntent.setData(Uri.parse(profileName));
			
			startProcess(processIntent, 0);
		}
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mServiceBinder;
	}

	/* Process service binder implementation */
	IProcessService.Stub mServiceBinder = new IProcessService.Stub() {

		@Override
		public void registerProcessCallback(ProcessDescriptor pd,
				IProcessCallback cb) throws RemoteException {

			if (pd != null && cb != null) {

				ProcessRegistration reg = new ProcessRegistration(cb, pd);

				synchronized (mProcessRegistrations) {
					mProcessRegistrations.add(reg);
				}

				synchronized (mTasks) {
					for (ProcessTask task : mTasks) {

						ProcessDescriptor taskdescriptor = (ProcessDescriptor) task;
						if (pd.equals(taskdescriptor)) {

							/*
							 * task is already running send registration
							 * notification
							 */

							if (task.getState() != ProcessState.STATE_NONE) {
								
								/* notify here if task is no longer running because else 
								 * the callback will never receive any transcript etc */
								
								cb.OnProcessRegistration(pd, task.getAttributes()
										.get(ItemAttributes.ATTRIBUTE_SHELL), task
										.getScreenBuffer());

								cb.OnProcessStateChanged(pd, task.getState());
							}
						}
					}
				}
			}
		}

		@Override
		public void unregisterProcessCallback(ProcessDescriptor pd,
				IProcessCallback cb) throws RemoteException {

			synchronized (mProcessRegistrations) {
				/* use an iterator to allow safe removal from the collection */
				Iterator<ProcessRegistration> iter = mProcessRegistrations
						.iterator();
				while (iter.hasNext()) {
					ProcessRegistration reg = iter.next();
					if (reg.equalsCallback(cb)
							&& (reg.equalsDescriptor(pd) || pd == null)) {

						iter.remove();

					}
				}
			}
		}

		@Override
		public void registerServiceCallback(IServiceCallback cb)
				throws RemoteException {

			synchronized (mServiceCallbacks) {
				mServiceCallbacks.add(cb);
			}
		}

		@Override
		public void unregisterServiceCallback(IServiceCallback cb)
				throws RemoteException {

			synchronized (mServiceCallbacks) {
				mServiceCallbacks.remove(cb);
			}
		}

		@Override
		public void requestKillProcess(ProcessDescriptor pd)
				throws RemoteException {

			synchronized (mTasks) {
				for (ProcessTask task : mTasks) {
					ProcessDescriptor taskdescriptor = (ProcessDescriptor) task;
					if (pd.equals(taskdescriptor))
						task.requestKill();
				}
			}
		}

		@Override
		public void requestKillProcesses(IServiceCallback cb)
				throws RemoteException {

			synchronized (mTasks) {
				for (ProcessTask task : mTasks) {
					task.requestKill();
				}
			}
		}

		@Override
		public ProcessDescriptor[] getProcesses(IServiceCallback cb, int state)
				throws RemoteException {

			ArrayList<ProcessDescriptor> processes = new ArrayList<ProcessDescriptor>();
			synchronized (mTasks) {
				for (ProcessTask task : mTasks) {
					if (((task.getState() & state) != 0)
							|| state == ProcessState.STATE_NONE) {
						processes.add(new ProcessDescriptor(task));
					}
				}
			}

			ProcessDescriptor[] pds = new ProcessDescriptor[processes.size()];
			processes.toArray(pds);

			return pds;
		}

		@Override
		public int getProcessState(ProcessDescriptor pd) throws RemoteException {

			synchronized (mTasks) {
				for (ProcessTask task : mTasks) {
					ProcessDescriptor taskdescriptor = (ProcessDescriptor) task;
					if (taskdescriptor.equals(pd))
						return task.getState();
				}
			}

			return -1;
		}

		@Override
		public void dispatchProcessOutput(ProcessDescriptor pd, byte[] output,
				int offset, int length) throws RemoteException {

			synchronized (mTasks) {
				
				final ArrayList<ProcessTask> tasks = mTasks;
				int cnt = tasks.size();
				for (int i=0; i < cnt; ++i) {

					final ProcessTask task = tasks.get(i);
					
					ProcessDescriptor taskdescriptor = (ProcessDescriptor) task;
					if (taskdescriptor.equals(pd))
						task.sendOutput(output, offset, length);
				}
			}
		}

		@Override
		public void dispatchProcessEvent(ProcessDescriptor pd, int event,
				Bundle extras) throws RemoteException {
		}

		@Override
		public void requestWindowSizeChange(ProcessDescriptor pd, int rows,
				int cols, int width, int height) throws RemoteException {

			synchronized (mTasks) {
				for (ProcessTask task : mTasks) {
					ProcessDescriptor taskdescriptor = (ProcessDescriptor) task;
					if (taskdescriptor.equals(pd))
						task.requestWindowSizeChange(rows, cols, width, height);
				}
			}			
		}
	};

	public void startProcess(Intent i, int flags) {

		/* create a new process descriptor */
		int newPid = mCurrentPid.getAndAdd(1);
		ProcessDescriptor pd = new ProcessDescriptor(newPid, i);

		/* start new task from descriptor */
		ProcessTask task = new ProcessTask(ProcessService.this, pd,
				ProcessService.this);

		synchronized (mTasks) {
			mTasks.add(task);
		}
	}

	/* ProcessStateListener */
	@Override
	public void OnProcessStateChanged(ProcessTask task, int state) {

		ProcessDescriptor pd = (ProcessDescriptor) task;

		/* notify all matching ProcessCallbacks */
		synchronized (mProcessRegistrations) {

			Iterator<ProcessRegistration> iter = mProcessRegistrations
					.iterator();
			while (iter.hasNext()) {
				ProcessRegistration reg = iter.next();

				if (reg.equalsDescriptor(pd)) {
					try {
						
						if(state == ProcessState.STATE_RUNNING) {

							/* 
							 * notify callbacks who registered early while the process was in STATE_NONE 
							 * and the task did not have the needed info to dispatch a complete
							 * OnProgressRegistration event
							 * */
							
							reg.callback.OnProcessRegistration(pd, task.getAttributes()
									.get(ItemAttributes.ATTRIBUTE_SHELL), task
									.getScreenBuffer());
							
						}
						
						/* dispatch process state to callback */
						reg.callback.OnProcessStateChanged(pd, state);

					} catch (RemoteException e) {
						/*
						 * callback could be gone without unregistering
						 * correctly... remove it
						 */
						Log.d(LOG_TAG, "Trying to access dead process callback");
						iter.remove();
					}
				}
			}
		}

		/* notify MonitorCallback(s). normally there should be only one (main) */
		synchronized (mServiceCallbacks) {

			Iterator<IServiceCallback> iter = mServiceCallbacks.iterator();
			while (iter.hasNext()) {
				IServiceCallback cb = iter.next();
				try {
					cb.OnProcessStateChanged(pd, state);
				} catch (RemoteException e) {
					/*
					 * callback could be gone without unregistering correctly...
					 * remove it
					 */
					Log.d(LOG_TAG, "Trying to access dead service callback");
					iter.remove();
				}
			}
		}

		if (state == ProcessState.STATE_RUNNING) {
			createNotificationForTask(task);

			if (!task.getAttributes().containsKey(
					ItemAttributes.ATTRIBUTE_UNATTENDED)) {

				/* launch a process activity */
				startActivity(pd.getActivityIntent(this));
			}

		}

		if (!ProcessState.isActiveState(state)) {
			removeNotificationForTask(task);

			/* task is no longer running add to history */

			/* TODO: 
			 * instead of storing a transcript for history we can also store the actual screenbuffer bytes so 
			 * that we will be able to add color to the history log */
			
			
			ContentValues values = new ContentValues();

			values.put(HistoryProvider.COLUMN_INTENT, pd.mIntent.toUri(0));
			values.put(HistoryProvider.COLUMN_TIME_START, pd.mTime);
			values.put(HistoryProvider.COLUMN_TIME_END,
					System.currentTimeMillis());
			values.put(HistoryProvider.COLUMN_STATE, state);
			values.put(HistoryProvider.COLUMN_LOG, task.getTranscript());

			getContentResolver().insert(ContentUri.URI_HISTORY, values);
		}
	}

	@Override
	public void OnProcessEvent(ProcessTask task, int event) {
		ProcessDescriptor pd = (ProcessDescriptor) task;

		/* notify all matching ProcessCallbacks 
		 * make sure there is no allocation happening here
		 * as this function might get called very often.
		 * */
		
		synchronized (mProcessRegistrations) {
			
			final ArrayList<ProcessRegistration> registrations = mProcessRegistrations;
			int cnt = registrations.size();
			for(int i=0; i < cnt; ++i) {
				final ProcessRegistration reg = registrations.get(i);
				if (reg.equalsDescriptor(pd)) {

					try {
						reg.callback.OnProcessEvent(pd, event);

					} catch (RemoteException e) {
						Log.d(LOG_TAG, "Trying to access dead process callback");
					}
				}
			}
		}
	}

	public void createNotificationForTask(ProcessTask task) {

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				task.getPid(), task.getActivityIntent(this),
				PendingIntent.FLAG_UPDATE_CURRENT);

		int titleRes = ACTION_EXECUTE.equals(task.getIntent().getAction()) ? R.string.executing_script : R.string.executing_process;
		String title = getResources().getString(titleRes);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setContentIntent(pendingIntent).setContentTitle(title)
				.setContentText(task.getIntent().getDataString())
				.setTicker(title).setSmallIcon(R.drawable.ic_launcher)
				.setOngoing(true).setAutoCancel(false);

		/* set as foreground if no active task is already set as foreground else just create a normal notification,
		 * which we might later promote to foreground when needed */
		
		if(mForeground == 0) {
			
			Log.d(LOG_TAG, String.format("Creating foreground notification for task %d", task.getPid()));
			
			mForeground = task.getPid();
			startForeground(mForeground, mBuilder.build());
			
		} else {
			
			Log.d(LOG_TAG, String.format("Creating normal notification for task %d", task.getPid()));
			nm.notify(task.getPid(), mBuilder.build());
		}
		
	}

	public void removeNotificationForTask(ProcessTask task) {

		if(mForeground == task.getPid()) {
			Log.d(LOG_TAG, String.format("Removing foreground notification for task %d", task.getPid()));
			
			stopForeground(true);
			
			/* check if we have other tasks running and promote first active task to foreground */
			
			synchronized (mTasks) {
				
				/* set mForeground to 0 so that any newly created task/notification will set this service
				 * to a foreground service. If there is still an active task we will promote that to
				 * a foreground notification */
				
				mForeground = 0;

				for (ProcessTask _task : mTasks) {
					if(ProcessState.isActiveState(_task.getState())) {
						Log.d(LOG_TAG, String.format("Promoting task %d to foreground", _task.getPid()));
						createNotificationForTask(_task);
						break;
					}
				}
			}			
			
		} else {
		
			Log.d(LOG_TAG, String.format("Removing normal notification for task %d", task.getPid()));
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.cancel(task.getPid());
		}
	}
	
	/*
	 * simple pair to hold a registration between process callback and
	 * descriptor
	 */
	class ProcessRegistration {

		public IProcessCallback callback;
		public ProcessDescriptor descriptor;

		public ProcessRegistration(IProcessCallback cb, ProcessDescriptor pd) {

			this.callback = cb;
			this.descriptor = pd;
		}

		public boolean equalsDescriptor(ProcessDescriptor pd) {
			return descriptor.equals(pd);
		}

		public boolean equalsCallback(IProcessCallback cb) {
			return callback.equals(cb);
		}

		@Override
		public boolean equals(Object o) {
			boolean result = false;
			if (o instanceof ProcessRegistration) {
				ProcessRegistration oreg = (ProcessRegistration) o;
				result = (equalsCallback(oreg.callback) && equalsDescriptor(oreg.descriptor));
			}
			return result;
		}
	}	
}
