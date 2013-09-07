package org.gscript.receiver;

import java.util.Calendar;
import org.gscript.data.ContentUri;
import org.gscript.data.LibraryProvider;
import org.gscript.data.ScheduleProvider;
import org.gscript.data.library.ItemConditions;
import org.gscript.process.ProcessService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class ScheduleReceiver extends BroadcastReceiver {

	static final String LOG_TAG = "ScheduleReceiver";

	static final int NO_SCHEDULE_ID = -1;

	public static final String ACTION_RESCHEDULE = "org.gscript.action.RESCHEDULE";
	public static final String ACTION_ALARM = "org.gscript.action.ALARM";
	public static final String EXTRA_SCHEDULE_ID = "id";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (ACTION_RESCHEDULE.equals(intent.getAction())) {
			Log.d(LOG_TAG, "received reschedule intent");

			if (intent.hasExtra(EXTRA_SCHEDULE_ID)) {

				int scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID,
						NO_SCHEDULE_ID);
				if (scheduleId != NO_SCHEDULE_ID) {
					/* only reschedule a single schedule */
					Log.d(LOG_TAG, "-- only rescheduling single " + scheduleId);

					Cursor c = context.getContentResolver().query(
							Uri.withAppendedPath(ContentUri.URI_SCHEDULE,
									String.valueOf(scheduleId)), null, null,
							null, null);
					if (c.moveToFirst()) {

						int scheduleDays = c.getInt(c
								.getColumnIndex(ScheduleProvider.COLUMN_DAYS));
						int scheduleInterval = c
								.getInt(c
										.getColumnIndex(ScheduleProvider.COLUMN_INTERVAL));
						int scheduleTimeStart = c
								.getInt(c
										.getColumnIndex(ScheduleProvider.COLUMN_TIME_START));
						int scheduleTimeEnd = c
								.getInt(c
										.getColumnIndex(ScheduleProvider.COLUMN_TIME_END));

						scheduleAlarm(context, true, scheduleId, scheduleDays,
								scheduleTimeStart, scheduleTimeEnd,
								scheduleInterval);
					}
					c.close();
				}

			} else {
				/* reschedule all schedules */
				Log.d(LOG_TAG, "-- rescheduling all");

				Cursor c = context.getContentResolver().query(
						ContentUri.URI_SCHEDULE, null, null, null, null);
				while (c.moveToNext()) {

					int scheduleId = c.getInt(c
							.getColumnIndex(ScheduleProvider.COLUMN_ID));
					int scheduleDays = c.getInt(c
							.getColumnIndex(ScheduleProvider.COLUMN_DAYS));
					int scheduleInterval = c.getInt(c
							.getColumnIndex(ScheduleProvider.COLUMN_INTERVAL));
					int scheduleTimeStart = c
							.getInt(c
									.getColumnIndex(ScheduleProvider.COLUMN_TIME_START));
					int scheduleTimeEnd = c.getInt(c
							.getColumnIndex(ScheduleProvider.COLUMN_TIME_END));

					scheduleAlarm(context, true, scheduleId, scheduleDays,
							scheduleTimeStart, scheduleTimeEnd,
							scheduleInterval);
				}

				c.close();
			}
		}

		if (ACTION_ALARM.equals(intent.getAction())) {

			int scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID,
					NO_SCHEDULE_ID);
			if (scheduleId != NO_SCHEDULE_ID) {

				Log.d(LOG_TAG, "received alarm for schedule " + scheduleId);

				Cursor c = context.getContentResolver().query(
						Uri.withAppendedPath(ContentUri.URI_SCHEDULE,
								String.valueOf(scheduleId)), null, null, null,
						null);
				if (c.moveToFirst()) {

					executeScheduledProcesses(context, scheduleId);

					int scheduleDays = c.getInt(c
							.getColumnIndex(ScheduleProvider.COLUMN_DAYS));
					int scheduleInterval = c.getInt(c
							.getColumnIndex(ScheduleProvider.COLUMN_INTERVAL));
					int scheduleTimeStart = c
							.getInt(c
									.getColumnIndex(ScheduleProvider.COLUMN_TIME_START));
					int scheduleTimeEnd = c.getInt(c
							.getColumnIndex(ScheduleProvider.COLUMN_TIME_END));

					scheduleAlarm(context, false, scheduleId, scheduleDays,
							scheduleTimeStart, scheduleTimeEnd,
							scheduleInterval);
				}
				c.close();
			}
		}
	}

	void scheduleAlarm(Context context, boolean firstAlarm, int scheduleId,
			int scheduleDays, int scheduleStart, int scheduleEnd,
			int scheduleInterval) {

		Log.d(LOG_TAG, "scheduling alarm for schedule " + scheduleId);

		if (scheduleDays == 0) {
			Log.d(LOG_TAG, "no days selected in schedule... no alarm to set");
		} else {

			Intent intent = new Intent(ACTION_ALARM);
			intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					scheduleId, intent, 0);

			long scheduleAlarm = Long.MAX_VALUE;

			Calendar current = Calendar.getInstance();
			Calendar next = Calendar.getInstance();

			Log.d(LOG_TAG, "first alarm: " + firstAlarm);
			Log.d(LOG_TAG, "start: " + scheduleStart);
			Log.d(LOG_TAG,
					"current: "
							+ ((current.get(Calendar.HOUR_OF_DAY) * 60) + current
									.get(Calendar.MINUTE)));

			if ((scheduleDays & dayMask(current.get(Calendar.DAY_OF_WEEK))) != 0
					&& (scheduleInterval != 0 || firstAlarm)) {

				Log.d(LOG_TAG, "today in schedule days mask");

				if (firstAlarm
						&& ((current.get(Calendar.HOUR_OF_DAY) * 60) + current
								.get(Calendar.MINUTE)) < scheduleStart) {
					/*
					 * current time below scheduleStart.. set next alarm to
					 * scheduleStart
					 */

					next.set(Calendar.HOUR_OF_DAY, 0);
					next.set(Calendar.MINUTE, 0);
					next.set(Calendar.SECOND, 0);
					next.set(Calendar.MILLISECOND, 0);

					next.add(Calendar.MINUTE, scheduleStart);

					Log.d(LOG_TAG,
							"scheduling first alarm (today) on "
									+ next.toString());
					scheduleAlarm = next.getTimeInMillis();

				} else {

					if (!firstAlarm)
						next.add(Calendar.MINUTE, scheduleInterval);

					/*
					 * check if we are still in the same day and interval still
					 * before todays schedule end ( in minutes )
					 */
					if (next.get(Calendar.DAY_OF_MONTH) == current
							.get(Calendar.DAY_OF_MONTH)
							&& (((next.get(Calendar.HOUR_OF_DAY) * 60) + next
									.get(Calendar.MINUTE)) <= scheduleEnd)) {

						Log.d(LOG_TAG, "scheduling next alarm (today) on "
								+ next.toString());
						scheduleAlarm = next.getTimeInMillis();

					} else {

						/* revert next to previous state (-scheduleInterval) */
						Log.d(LOG_TAG,
								"interval exceeds today... looking for next day");

						if (!firstAlarm)
							next.add(Calendar.MINUTE, -scheduleInterval);
					}
				}
			}

			if (scheduleAlarm == Long.MAX_VALUE) {

				Log.d(LOG_TAG, "schedule not set... looking for next day");

				int daysAdded = 0;

				while (scheduleAlarm == Long.MAX_VALUE && daysAdded <= 7) {

					/*
					 * keep adding days until we come across a day which is set
					 * in this schedule
					 */
					next.roll(Calendar.DATE, true);

					/* check if we are still in the same week */
					if (next.get(Calendar.WEEK_OF_YEAR) != current
							.get(Calendar.WEEK_OF_YEAR)) {
						/* TODO: possibly allow skipping weeks in the future */

						/* allow looping through another week */
						// daysAdded = 0;
					}

					if ((scheduleDays & dayMask(next.get(Calendar.DAY_OF_WEEK))) != 0) {

						/* set next time to scheduleStart */

						next.set(Calendar.HOUR_OF_DAY, 0);
						next.set(Calendar.MINUTE, 0);
						next.set(Calendar.SECOND, 0);
						next.set(Calendar.MILLISECOND, 0);

						next.add(Calendar.MINUTE, scheduleStart);

						Log.d(LOG_TAG, "next alarm on " + next.toString());

						scheduleAlarm = next.getTimeInMillis();
					}
					daysAdded++;
				}
			}

			if (scheduleAlarm != Long.MAX_VALUE) {

				AlarmManager alarmManager = (AlarmManager) context
						.getSystemService(Context.ALARM_SERVICE);
				alarmManager.set(AlarmManager.RTC_WAKEUP, scheduleAlarm,
						pendingIntent);

			} else {

				/* should never happen */
				Log.e(LOG_TAG, "couldnt find next alarm for schedule "
						+ scheduleId);
			}
		}
	}

	void executeScheduledProcesses(Context context, int scheduleId) {

		Cursor c = context.getContentResolver().query(
				ContentUri.URI_ITEM_CONDITIONS,
				null,
				LibraryProvider.COLUMN_KEY + "=? AND "
						+ LibraryProvider.COLUMN_VALUE + "=?",
				new String[] { ItemConditions.CONDITION_SCHEDULE,
						String.valueOf(scheduleId) }, null);

		if (c.getCount() == 0) {
			Log.d(LOG_TAG, "no processes to execute for schedule " + scheduleId);
		}

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
								"executing process with schedule condition [ schedule:%d library:%d path:%s ]",
								scheduleId, libraryId, itemPath));

				Intent processIntent = new Intent(context, ProcessService.class);
				processIntent.setAction(ProcessService.ACTION_EXECUTE);
				processIntent.setData(itemUri);

				context.startService(processIntent);

			} else {
				Log.d(LOG_TAG, String.format("onschedule: could not load item [ library:%d path:%s ]",
								libraryId, itemPath));
			}
			itemCursor.close();
		}
		c.close();
	}

	int dayMask(int calendarDay) {
		switch (calendarDay) {
		case Calendar.MONDAY:
			return ScheduleProvider.MONDAY;
		case Calendar.TUESDAY:
			return ScheduleProvider.TUESDAY;
		case Calendar.WEDNESDAY:
			return ScheduleProvider.WEDNESDAY;
		case Calendar.THURSDAY:
			return ScheduleProvider.THURSDAY;
		case Calendar.FRIDAY:
			return ScheduleProvider.FRIDAY;
		case Calendar.SATURDAY:
			return ScheduleProvider.SATURDAY;
		case Calendar.SUNDAY:
			return ScheduleProvider.SUNDAY;
		}
		return 0;
	}

}
