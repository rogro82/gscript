package org.gscript;

import org.gscript.process.IProcessCallback;
import org.gscript.process.IProcessService;
import org.gscript.process.ProcessDescriptor;
import org.gscript.process.ProcessService;
import org.gscript.process.ProcessState;
import org.gscript.settings.ShellProfile;
import org.gscript.terminal.EmulatorInput;
import org.gscript.terminal.EmulatorScreen;
import org.gscript.terminal.ScreenBufferParcelable;
import org.gscript.terminal.TerminalEvent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class EmulatorActivity extends SherlockActivity implements
		EmulatorScreen.EmulatorScreenListener,
		EmulatorInput.EmulatorInputListener {

	static final String LOG_TAG = "EmulatorActivity";
	public static final String EXTRA_DESCRIPTOR = "descriptor";

	IProcessService mProcessService;
	ProcessDescriptor mProcessDescriptor;
	ShellProfile mProfile;

	boolean mIsBound;

	Handler mHandler;

	EmulatorScreen mEmulatorScreen;
	EmulatorInput mEmulatorInput;

	MenuItem mMenuKill;
	MenuItem mMenuAutoscroll;
	MenuItem mMenuGestureMode;
	
	boolean mMenuKillEnabled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_emulator);

		Intent intent = this.getIntent();
		mProcessDescriptor = intent.getParcelableExtra(EXTRA_DESCRIPTOR);
		
		Uri data;
		if((data = mProcessDescriptor.getIntent().getData()) != null) {
			setTitle(data.getLastPathSegment());
		}

		mHandler = new Handler();
		
		mEmulatorScreen = (EmulatorScreen) findViewById(R.id.emulatorScreen);
		mEmulatorScreen.setEmulatorScreenListener(this);

		mEmulatorInput = (EmulatorInput) findViewById(R.id.emulatorInput);
		mEmulatorInput.setEmulatorInputListener(this);
		
		bindProcessService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		/* unbind service */
		unbindProcessService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.emulator, menu);

		mMenuAutoscroll = menu.findItem(R.id.menu_emulator_autoscroll);
		mMenuGestureMode = menu.findItem(R.id.menu_emulator_gesture_mode);
		mMenuKill = menu.findItem(R.id.menu_emulator_kill);
		mMenuKill.setEnabled(mMenuKillEnabled);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_emulator_kill:
			if (mIsBound && mProcessService != null) {
				try {
					mProcessService.requestKillProcess(mProcessDescriptor);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return true;
		case R.id.menu_emulator_autoscroll:
			if (mEmulatorScreen != null)
				mEmulatorScreen.toggleAutoScrollEnabled();

			return true;
		case R.id.menu_emulator_gesture_mode:
			if(mEmulatorScreen != null) {
				mEmulatorScreen.setGestureMode(Math.abs(mEmulatorScreen.getGestureMode()-1));
			}
			return true;
		case R.id.menu_emulator_softinput:
			if(mEmulatorInput != null) {
				mEmulatorInput.toggleSoftInput();
			}
			return true;
		case R.id.menu_emulator_softinput_additional:
			if(mEmulatorInput != null) {
				mEmulatorInput.toggleAdditionalSoftInput();
			}
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	IProcessCallback.Stub mProcessCallback = new IProcessCallback.Stub() {

		@Override
		public void OnProcessStateChanged(ProcessDescriptor pd, int state)
				throws RemoteException {

			final int currentState = state;

			mHandler.post(new Runnable() {

				@Override
				public void run() {

					boolean processActive = ProcessState.isActiveState(currentState);

					mMenuKillEnabled=processActive;
					
					if (mMenuKill != null)
						mMenuKill.setEnabled(mMenuKillEnabled);

					mEmulatorScreen.setCursorVisible(processActive);
				}
			});

			mEmulatorScreen.onProcessEvent(TerminalEvent.SCREEN_UPDATE);

		}

		@Override
		public void OnProcessRegistration(ProcessDescriptor pd, String profile,
				ScreenBufferParcelable screenBuffer) throws RemoteException {

			mHandler.post(new RegistrationRunnable(profile, screenBuffer));
		}

		@Override
		public void OnProcessEvent(ProcessDescriptor pd, int event)
				throws RemoteException {

			switch (event) {
			case TerminalEvent.SCREEN_UPDATE:
				mEmulatorScreen.onProcessEvent(event);
				break;
			case TerminalEvent.SCREEN_RESIZE:
				mEmulatorScreen.onProcessEvent(event);
				break;
			}
		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			mProcessService = IProcessService.Stub.asInterface(service);

			try {

				if (mProcessService != null)
					mProcessService.registerProcessCallback(mProcessDescriptor,
							mProcessCallback);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {

			mProcessService = null;

		}
	};

	void bindProcessService() {

		bindService(new Intent(this, ProcessService.class), mServiceConnection,
				Context.BIND_AUTO_CREATE);

		mIsBound = true;
	}

	
	void unbindProcessService() {

		if (mIsBound) {

			try {
				if (mProcessService != null)
					mProcessService.unregisterProcessCallback(
							mProcessDescriptor, mProcessCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			unbindService(mServiceConnection);

			mIsBound = false;
		}
	}

	
	class RegistrationRunnable implements Runnable {

		String profile;
		ScreenBufferParcelable screenBuffer;

		public RegistrationRunnable(String profile,
				ScreenBufferParcelable screenBuffer) {

			this.profile = profile;
			this.screenBuffer = screenBuffer;

		}

		@Override
		public void run() {

			/* get shell profile */

			mProfile = ShellProfile.forKey(
					EmulatorActivity.this, profile);

			/* initialize emulator view */

			mEmulatorScreen.initialize(mProfile, screenBuffer);
		}
	}

	/* EmulatorView / EmulatorInput */
	

	@Override
	public void onEmulatorInput(byte[] b, int length) {
		if (mIsBound && mProcessService != null) {
			try {

				mProcessService.dispatchProcessOutput(mProcessDescriptor,
						b, 0, length);

			} catch (RemoteException e) {
			}
		}
	}


	@Override
	public void onWindowSizeChangeRequested(int rows, int cols, int width,
			int height) {
		if (mIsBound && mProcessService != null) {
			try {
				
				mProcessService.requestWindowSizeChange(mProcessDescriptor,
						rows, cols, width, height);
				
			} catch (RemoteException e) {
			}
		}
	}


	@Override
	public void onAutoScrollChanged(boolean enabled) {
		
		if (mMenuAutoscroll != null) {
			if (enabled) {
				mMenuAutoscroll.setIcon(R.drawable.ic_autoscroll_enabled);
			} else {
				mMenuAutoscroll.setIcon(R.drawable.ic_autoscroll_disabled);
			}		
		}
	}
	

	@Override
	public void onGestureModeChanged(int mode) {
		if(mMenuGestureMode != null) {
			switch(mode) {
			case EmulatorScreen.GESTURE_MODE_SCROLL:
				mMenuGestureMode.setTitle(R.string.menu_emulator_cursor_mode);
				break;
			case EmulatorScreen.GESTURE_MODE_CURSOR:
				mMenuGestureMode.setTitle(R.string.menu_emulator_scroll_mode);
				break;
			}
		}
	}

}