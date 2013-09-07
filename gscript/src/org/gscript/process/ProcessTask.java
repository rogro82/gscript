package org.gscript.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gscript.data.ContentUri;
import org.gscript.data.ContentUri.LibraryPathSegments;
import org.gscript.data.library.ItemAttributes;
import org.gscript.data.library.Library;
import org.gscript.data.library.LibraryItem;
import org.gscript.jni.JNIReference.JNIReferenceException;
import org.gscript.settings.ShellProfile;
import org.gscript.terminal.ScreenBuffer;
import org.gscript.terminal.TerminalEmulator;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class ProcessTask extends ProcessDescriptor implements TerminalEmulator.OnEmulatorEventListener {

	static final String LOG_TAG = "ProcessTask";

	static final String SCHEME_FILE = "file";
	static final String SCHEME_HTTP = "http";
	static final String SCHEME_HTTPS = "https";
	static final String SCHEME_FTP = "ftp";
	static final String SCHEME_CONTENT = "content";

	static final String PATHS_DEFAULT = "/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin";
	static final String PATHS_GSCRIPT = ":/data/data/org.gscript/bin";
	static final String PATH_ARG = "%path%";

	static final String ENV_GSCRIPT_VERSION = "GSCRIPT_VERSION";
	static final String ENV_GSCRIPT_PID = "GSCRIPT_PID";

	static final String EOL = System.getProperty("line.separator", "\n");
	static final String EXIT_CMD = EOL + "exit" + EOL;

	static final String PROCESS_TASK_THREAD = "ProcessTask";
	static final String PROCESS_INPUT_THREAD = "ProcessInput";
	
	final ProcessStateListener mStateListener;
	final Context mContext;
	ItemAttributes mAttributes;

	int mState;
	boolean mKillRequested;

	ProcessThread mProcessThread;
	SubProcess mSubProcess;
	TerminalEmulator mEmulator;

	public ProcessTask(Context context, ProcessDescriptor pd,
			ProcessStateListener l) {
		super(pd);

		mAttributes = new ItemAttributes();
		mStateListener = l;
		mContext = context;
		mProcessThread = new ProcessThread();
		mProcessThread.setName(PROCESS_TASK_THREAD);
		mProcessThread.start();
	}

	private void setState(int state) {
		mState = state;
		mStateListener.OnProcessStateChanged(ProcessTask.this, mState);
	}

	private void dispatchEvent(int event) {
		mStateListener.OnProcessEvent(ProcessTask.this, event);
	}
	
	@Override
	public void OnEmulatorEvent(int event) {
		dispatchEvent(event);
	}
	
	public void sendOutput(byte[] output, int offset, int length) {
		if (isActive() && mEmulator != null) {
			mEmulator.processOutput(output, offset, length);
		}
	}

	public int getState() {
		return mState;
	}

	Context getContext() {
		return mContext;
	}

	public ScreenBuffer getScreenBuffer() {
		return (mEmulator != null) ? mEmulator.getScreenBuffer() : null;
	}
	
	public String getTranscript() {
		return (mEmulator != null) ? mEmulator.getScreenBuffer().getTranscript() : "";
	}

	public ItemAttributes getAttributes() {
		return mAttributes;
	}

	public boolean isActive() {
		return ProcessState.isActiveState(mState);
	}

	public void requestKill() {
		if (isActive() && mSubProcess.sigKill(SubProcess.SIGHUP) > 0) {
			setState(ProcessState.STATE_KILLED);
		}
	}
	
	public void requestWindowSizeChange(int rows, int cols, int width, int height) {

		if(mEmulator != null)
			mEmulator.resizeWindow(rows, cols, width, height);

		if(mSubProcess != null)
			mSubProcess.resizeWindow(rows, cols, width, height);
		
	}

	class ProcessThread extends Thread {

		@SuppressWarnings("unchecked")
		@Override
		public void run() {

			final Intent intent = getIntent();
			final Uri uri = intent.getData();

			int error = 0;

			File scriptFile = null;
			String content = null;
			
			ShellProfile profile = null;
			String profileCommand = null;

			if(ProcessService.ACTION_EXECUTE.equals(intent.getAction())) {
			
				content = getContentForUri(uri);
				if (content == null) {
	
					error = ProcessState.ERROR_NO_CONTENT;
	
					Log.e(LOG_TAG, String.format(
							"Error trying to start process: %d", error));
	
					setState(ProcessState.STATE_ERROR(error));
					return;
	
				}
	
				/*
				 * merge attributes with any attributes received in initial intent
				 * so that we can have different defaults for processes launched
				 * from a specific condition
				 */
	
				if (intent.hasExtra(ProcessService.EXTRA_ATTRIBUTES))
					mAttributes.putAll((HashMap<String, String>) intent
							.getSerializableExtra(ProcessService.EXTRA_ATTRIBUTES));
			
				/* get shell profile from attributes */

				profile = ShellProfile.forKey(getContext(),
								mAttributes.get(ItemAttributes.ATTRIBUTE_SHELL));
				
				profileCommand = profile.cmdExec;
				
			
			} if(ProcessService.ACTION_START.equals(intent.getAction())) {
				
				/* get shell profile directly from extra */
				
				profile = ShellProfile.forKey(getContext(), intent.getStringExtra(ProcessService.EXTRA_PROFILE));
				
				profileCommand = profile.cmdStart;
			}


			mEmulator = new TerminalEmulator(profile, ProcessTask.this);
			
			/*
			 * we now have everything ( content and attributes ) we need to
			 * enter a running state. notify callbacks we are running and then
			 * continue executing the actual process
			 */
			
			setState(ProcessState.STATE_RUNNING);

			/* build argument list */

			List<String> arguments = getArguments(profileCommand);

			/* check if arguments contains %path% */

			boolean hasPathArg = arguments.contains(PATH_ARG);

			/* append exit if needed and we have content */

			if (content != null && profile.appendExit) {
				content += EXIT_CMD;
			}
			
			/* create executable script file if needed */
			
			if (content != null && hasPathArg) {

				scriptFile = createScriptFile(content);

				/* replace PATH_ARG with real file path */
				if (scriptFile != null) {

					int args = arguments.size();
					for (int i = 0; i < args; ++i) {

						if (arguments.get(i).equals(PATH_ARG)) {
							arguments.set(i, scriptFile.getPath());
						}
					}
				}
			}

			/* create SubProcess and execute */

			String env[] = getEnvironment(profile.appendPath);
			String args[] = arguments.toArray(new String[0]);
			String command = args[0];

			try {

				mSubProcess = new SubProcess(command, args, env);

				/* execute and resize according to emulator dimensions */
				
				mSubProcess.execute();
				mSubProcess.resizeWindow(
						mEmulator.getScreenRows(), 
						mEmulator.getScreenCols(), 
						mEmulator.getScreenWidth(), 
						mEmulator.getScreenHeight());

			} catch (JNIReferenceException ex) {

				Log.e(LOG_TAG, ex.getMessage());
				error = ProcessState.ERROR_EXECUTION_FAILED;

			}

			if (error != 0) {

				Log.e(LOG_TAG, String.format(
						"Error trying to start process: %d", error));

				setState(ProcessState.STATE_ERROR(error));

			} else {

				/* start input / output stream handling */

				String initialOutput = null;

				if (scriptFile != null && hasPathArg) {
					initialOutput = null;
				}
				if (scriptFile != null && !hasPathArg) {
					initialOutput = scriptFile.getPath() + EOL;
				}
				if (scriptFile == null) {
					initialOutput = content;
				}

				final InputStream in = mSubProcess.getInputStream();
				final OutputStream out = mSubProcess.getOutputStream();

				/*
				 * write initial output directly to outputstream bypassing the
				 * emulator
				 */

				if (initialOutput != null) {

					byte[] b = initialOutput.getBytes();
					try {
						out.write(b, 0, b.length);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				new InputThread(in, mEmulator).start();
				
				mEmulator.setOutputStream(out);

				/* wait for process to finish */

				mSubProcess.waitFor();
//				try {
//					
//					out.close();
//					in.close();
//
//				} catch (IOException e) {
//				}

				/*
				 * only change state if previous state was active because we
				 * might have received a kill request and finished because of
				 * that
				 */

				if (mState == ProcessState.STATE_RUNNING) {
					setState(ProcessState.STATE_FINISHED);
				}
			}

			/* clean up */
			if (scriptFile != null && scriptFile.exists()) {
				scriptFile.delete();
			}
		}
	};

	private String[] getEnvironment(String appendPath) {

		String envPath = System.getenv("PATH");
		{
			if (envPath == null)
				envPath = PATHS_DEFAULT;

			envPath += PATHS_GSCRIPT;

			if (appendPath != null && appendPath.length() > 0) {

				if (!appendPath.startsWith(":"))
					envPath += ":";

				envPath += appendPath;
			}
		}

		/* build environment */

		String env[] = { 
				ENV_GSCRIPT_VERSION + "=0.1",
				ENV_GSCRIPT_PID + "=" + getPid(), 
				"PATH=" + envPath,
				"TERM=screen" };

		return env;
	}

	private ArrayList<String> getArguments(String command) {

		ArrayList<String> arguments = new ArrayList<String>();

		Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
		Matcher regexMatcher = regex.matcher(command);

		while (regexMatcher.find()) {
			if (regexMatcher.group(1) != null) {
				arguments.add(regexMatcher.group(1)); // double-quoted
			} else if (regexMatcher.group(2) != null) {
				arguments.add(regexMatcher.group(2)); // single-quoted
			} else {
				arguments.add(regexMatcher.group()); // unquoted
			}
		}

		return arguments;
	}

	private String getContentForUri(Uri uri) {

		String content = null;
		String scheme = uri.getScheme();

		if (SCHEME_FILE.equals(scheme) || SCHEME_HTTP.equals(scheme)
				|| SCHEME_HTTPS.equals(scheme) || SCHEME_FTP.equals(scheme)) {

			/* handle directly executed scripts from file or http/https/ftp */

			try {
				URL url = new URL(uri.toString());

				StringBuilder sb = new StringBuilder(100);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						url.openStream()));

				String line;
				while ((line = in.readLine()) != null) {
					sb.append(line);
					sb.append(EOL);
				}
				in.close();

				content = sb.toString();

			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}

		} else if (SCHEME_CONTENT.equals(uri.getScheme())) {

			/* handle library content uris */

			LibraryPathSegments seg = LibraryPathSegments.parse(uri);
			final ContentResolver cr = getContext().getContentResolver();

			/* get item content */
			{
				Uri itemUri = ContentUri.URI_LIBRARY_PATH(seg.id, seg.path,
						Library.FLAG_INCLUDE_CONTENT);

				Cursor itemCursor = cr.query(itemUri, null, null, null, null);
				if (itemCursor != null) {
					
					if (itemCursor.moveToFirst()) {
						LibraryItem item = LibraryItem.fromCursor(itemCursor);
						content = item.getContent();
					}
				}
				itemCursor.close();
			}
			/* get item attributes */
			{
				Uri attributesUri = ContentUri.URI_ITEM_ATTRIBS_PATH(seg.id,
						seg.path);
				Cursor attributesCursor = cr.query(attributesUri, null, null,
						null, null);
				if (attributesCursor != null) {

					ItemAttributes itemAttributes = new ItemAttributes(
							attributesCursor);

					mAttributes.putAll(itemAttributes);

					attributesCursor.close();
				}
			}

		}

		return content;
	}

	private File createScriptFile(String content) {

		File scriptFile = null;
		try {
			scriptFile = File.createTempFile(ProcessTask.this.getPid() + "_"
					+ ProcessTask.this.getTime(), ".tmp", ProcessTask.this
					.getContext().getCacheDir());

			BufferedWriter writer = null;

			try {
				writer = new BufferedWriter(new FileWriter(scriptFile));
				writer.write(content);

			} catch (IOException e) {
			} finally {
				try {
					if (writer != null)
						writer.close();
				} catch (IOException e) {
				}
			}

			scriptFile.setExecutable(true);
			
			return scriptFile;

		} catch (IOException e1) {
		}
		
		return scriptFile;
	}

	@Override
	public boolean equals(Object o) {

		if (o instanceof ProcessDescriptor) {

			ProcessDescriptor opd = (ProcessDescriptor) o;
			ProcessDescriptor pd = (ProcessDescriptor) this;

			return (opd.equals(pd));
		}
		return super.equals(o);
	}

	/* TODO: move to TerminalEmulator */
	class InputThread extends Thread {

		private final InputStream is;
		private final TerminalEmulator emulator;

		public InputThread(InputStream is, TerminalEmulator emulator) {
			this.setName(PROCESS_INPUT_THREAD);
			
			this.is = is;
			this.emulator = emulator;
		}

		@Override
		public void run() {

			try {

				byte buffer[] = new byte[4096];
				int read = 0;

				while ((read = is.read(buffer)) != -1) {
					emulator.processInput(buffer, 0, read);
				}

			} catch (Exception e) {
			}
		}
	}
}