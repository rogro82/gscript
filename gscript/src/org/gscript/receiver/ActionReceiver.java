package org.gscript.receiver;

import org.gscript.ExecuteDialog;
import org.gscript.data.ContentUri;
import org.gscript.data.LibraryProvider;
import org.gscript.data.library.ItemConditions;
import org.gscript.process.ProcessService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class ActionReceiver extends BroadcastReceiver {

	static final String LOG_TAG = "ActionReceiver";
	public static final String ACTION_EXEC = "org.gscript.action.EXEC";
	static final String EMULATED_PATH = "/mnt/shell/emulated/0";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

			/* execute items with boot condition */

			Cursor c = context.getContentResolver().query(
					ContentUri.URI_ITEM_CONDITIONS, null,
					LibraryProvider.COLUMN_KEY + "=?",
					new String[] { ItemConditions.CONDITION_BOOT }, null);

			while (c.moveToNext()) {

				int libraryId = c.getInt(c
						.getColumnIndex(LibraryProvider.COLUMN_LIBRARY));
				String itemPath = c.getString(c
						.getColumnIndex(LibraryProvider.COLUMN_PATH));

				Uri itemUri = ContentUri.URI_LIBRARY_PATH(libraryId, itemPath);

				/* try to get the item so that we know it is accessible */
				Cursor itemCursor = context.getContentResolver().query(itemUri,
						null, null, null, null);

				if (itemCursor.getCount() > 0) {

					Log.d(LOG_TAG,
							String.format(
									"execute process with boot condition [ library:%d path:%s ]",
									libraryId, itemPath));

					Intent processIntent = new Intent(context,
							ProcessService.class);
					processIntent.setAction(ProcessService.ACTION_EXECUTE);
					processIntent.setData(itemUri);

					context.startService(processIntent);

				} else {
					Log.d(LOG_TAG, String.format("onboot: could not load item [ library:%d path:%s ]",
							libraryId, itemPath));
				}
				itemCursor.close();
			}
			c.close();

			/* trigger a reschedules so that all schedules will get started */

			Log.d(LOG_TAG, "starting schedules");

			Intent scheduleIntent = new Intent(
					ScheduleReceiver.ACTION_RESCHEDULE);
			context.sendBroadcast(scheduleIntent);

		}
		if (ACTION_EXEC.equals(intent.getAction())) {

			Uri requestUri = intent.getData();

			/*
			 * the intents data uri might contain a location to emulated
			 * storage, but this is only accessible from shell and not from
			 * within an application so we check if its the case and replace it
			 * with the correct external storage path
			 */

			if (requestUri.getPath().startsWith(EMULATED_PATH)) {
				String path = requestUri.toString();
				path = path.replaceFirst(EMULATED_PATH, Environment
						.getExternalStorageDirectory().getPath());

				requestUri = Uri.parse(path);
			}

			/* redirect intent to filedialog activity */
			Intent requestIntent = new Intent(context, ExecuteDialog.class);

			requestIntent.setData(requestUri).addFlags(
					Intent.FLAG_ACTIVITY_NEW_TASK);

			context.startActivity(requestIntent);
		}
	}
}
