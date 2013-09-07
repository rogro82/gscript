package org.gscript;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.gscript.data.ContentUri;
import org.gscript.data.ScheduleProvider;
import org.gscript.receiver.ScheduleReceiver;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ScheduleActivity extends SherlockFragmentActivity {

	static final String LOG_TAG = "ScheduleActivity";
	static final int INTERVAL_MAX = (23*60)+59;
	static final int DIALOG_TIME_START_ID = 0;
	static final int DIALOG_TIME_END_ID = 1;
	
	public static final String ACTION_ADD = "org.gscript.ScheduleActivity.action_add";
	public static final String ACTION_EDIT = "org.gscript.ScheduleActivity.action_edit";
	public static final String EXTRA_SCHEDULE_ID = "id";

	static final SimpleDateFormat sTimeFormat = new SimpleDateFormat("hh:mm a");
	
	String mIntentAction;
	int mScheduleId;

	EditText mEditTextName;

	CheckBox mCheckBoxMonday;
	CheckBox mCheckBoxTuesday;
	CheckBox mCheckBoxWednesday;
	CheckBox mCheckBoxThursday;
	CheckBox mCheckBoxFriday;
	CheckBox mCheckBoxSaturday;
	CheckBox mCheckBoxSunday;

	Calendar mTimeStart;
	Calendar mTimeEnd;
	
	TextView mTextViewStart;
	TextView mTextViewEnd;
	
	EditText mEditTextInterval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_schedule);

		Intent intent = this.getIntent();

		mIntentAction = intent.getAction();
		mScheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, 0);

		mEditTextName = (EditText) this.findViewById(R.id.editTextName);

		mCheckBoxMonday = (CheckBox) this.findViewById(R.id.checkBoxMonday);
		mCheckBoxTuesday = (CheckBox) this.findViewById(R.id.checkBoxTuesday);
		mCheckBoxWednesday = (CheckBox) this.findViewById(R.id.checkBoxWednesday);
		mCheckBoxThursday = (CheckBox) this.findViewById(R.id.checkBoxThursday);
		mCheckBoxFriday = (CheckBox) this.findViewById(R.id.checkBoxFriday);
		mCheckBoxSaturday = (CheckBox) this.findViewById(R.id.checkBoxSaturday);
		mCheckBoxSunday = (CheckBox) this.findViewById(R.id.checkBoxSunday);

		mTimeStart = Calendar.getInstance();
		mTimeStart.set(Calendar.HOUR_OF_DAY, 0);
		mTimeStart.set(Calendar.MINUTE, 0);
		
		mTimeEnd = Calendar.getInstance();
		mTimeEnd.set(Calendar.HOUR_OF_DAY, 0);
		mTimeEnd.set(Calendar.MINUTE, 0);
		
		mTextViewStart = (TextView) this.findViewById(R.id.textViewStart);
		mTextViewStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				TimePickerDialogFragment timePicker = new TimePickerDialogFragment(mTimeStart, mTimeStartListener);
				timePicker.show(getSupportFragmentManager(), "start");
				
			}
		});
		
		mTextViewEnd = (TextView) this.findViewById(R.id.textViewEnd);
		mTextViewEnd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				TimePickerDialogFragment timePicker = new TimePickerDialogFragment(mTimeEnd, mTimeEndListener);
				timePicker.show(getSupportFragmentManager(), "end");
				
			}
		});
		
		mEditTextInterval = (EditText) this.findViewById(R.id.editTextInterval);
		mEditTextInterval.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				try {
					int val = Integer.parseInt(s.toString());
					if(val < 0) {
						mEditTextInterval.setText("0");
						mEditTextInterval.setSelection(0, 1);
					}
					if(val > (INTERVAL_MAX)) {
						mEditTextInterval.setText(String.valueOf(INTERVAL_MAX));
					}
					
				} catch(NumberFormatException e)
				{
					mEditTextInterval.setText("0");
					mEditTextInterval.setSelection(0, 1);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		if (ACTION_ADD.equals(mIntentAction)) {
			this.setTitle(R.string.schedule_add);

			/* set time end to end of day */
			
			mTimeEnd.set(Calendar.HOUR_OF_DAY, 23);
			mTimeEnd.set(Calendar.MINUTE, 59);

		} else {

			Cursor c = this.getContentResolver().query(
					Uri.withAppendedPath(ContentUri.URI_SCHEDULE,
							String.valueOf(mScheduleId)), null, null, null,
					null);

			if (c.moveToFirst()) {

				String scheduleName = c.getString(c
						.getColumnIndex(ScheduleProvider.COLUMN_TITLE));

				mEditTextName.setText(scheduleName);
				setTitle(String.format("Edit '%s'", scheduleName));

				int days = c.getInt(c
						.getColumnIndex(ScheduleProvider.COLUMN_DAYS));

				/* set checked days */
				mCheckBoxMonday
						.setChecked((days & ScheduleProvider.MONDAY) != 0);
				mCheckBoxTuesday
						.setChecked((days & ScheduleProvider.TUESDAY) != 0);
				mCheckBoxWednesday
						.setChecked((days & ScheduleProvider.WEDNESDAY) != 0);
				mCheckBoxThursday
						.setChecked((days & ScheduleProvider.THURSDAY) != 0);
				mCheckBoxFriday
						.setChecked((days & ScheduleProvider.FRIDAY) != 0);
				mCheckBoxSaturday
						.setChecked((days & ScheduleProvider.SATURDAY) != 0);
				mCheckBoxSunday
						.setChecked((days & ScheduleProvider.SUNDAY) != 0);

				int interval = c.getInt(c
						.getColumnIndex(ScheduleProvider.COLUMN_INTERVAL));
				
				mEditTextInterval.setText(String.valueOf(interval));
				
				int timeStart = c.getInt(c.getColumnIndex(ScheduleProvider.COLUMN_TIME_START));
				int timeEnd = c.getInt(c.getColumnIndex(ScheduleProvider.COLUMN_TIME_END));

				/* time is stored as minutes */
				
				mTimeStart.add(Calendar.MINUTE, timeStart);
				mTimeEnd.add(Calendar.MINUTE, timeEnd);

			}
			c.close();
		}
		
		updateTimeStartText();
		updateTimeEndText();
	}
	
	OnTimeChangedListener mTimeStartListener = new OnTimeChangedListener() {
		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			
			mTimeStart.set(Calendar.HOUR_OF_DAY, hourOfDay);
			mTimeStart.set(Calendar.MINUTE, minute);
			
			updateTimeStartText();
		}
	};
	
	OnTimeChangedListener mTimeEndListener = new OnTimeChangedListener() {
		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			
			mTimeEnd.set(Calendar.HOUR_OF_DAY, hourOfDay);
			mTimeEnd.set(Calendar.MINUTE, minute);
			
			updateTimeEndText();
		}
	};
	
	void updateTimeStartText() {
		mTextViewStart.setText(sTimeFormat.format(mTimeStart.getTime()));		
	}
	
	void updateTimeEndText() {
		mTextViewEnd.setText(sTimeFormat.format(mTimeEnd.getTime()));		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.schedule, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		ContentValues val = new ContentValues();

		String title = mEditTextName.getText().toString();
		if(title.length()==0) 
			title = "Unnamed schedule";
		
		val.put(ScheduleProvider.COLUMN_TITLE, title);

		/* possible week interval for future use */
		
		val.put(ScheduleProvider.COLUMN_WEEK, 0);
		
		/* add selected days to mask */
		
		int days = 0;
		
		days |= (mCheckBoxMonday.isChecked()) ? ScheduleProvider.MONDAY : 0;
		days |= (mCheckBoxTuesday.isChecked()) ? ScheduleProvider.TUESDAY : 0;
		days |= (mCheckBoxWednesday.isChecked()) ? ScheduleProvider.WEDNESDAY : 0;
		days |= (mCheckBoxThursday.isChecked()) ? ScheduleProvider.THURSDAY : 0;
		days |= (mCheckBoxFriday.isChecked()) ? ScheduleProvider.FRIDAY : 0;
		days |= (mCheckBoxSaturday.isChecked()) ? ScheduleProvider.SATURDAY : 0;
		days |= (mCheckBoxSunday.isChecked()) ? ScheduleProvider.SUNDAY : 0;

		val.put(ScheduleProvider.COLUMN_DAYS, days);

		/* store time in minutes */
		
		int timeStart = (mTimeStart.get(Calendar.HOUR_OF_DAY) * 60) + mTimeStart.get(Calendar.MINUTE);
		int timeEnd = (mTimeEnd.get(Calendar.HOUR_OF_DAY) * 60) + mTimeEnd.get(Calendar.MINUTE);

		/* check if we need to swap start/end time */
		if(timeStart > timeEnd) {
			int tmpEnd = timeStart;
			timeStart = timeEnd;
			timeEnd = tmpEnd;
		}
		
		val.put(ScheduleProvider.COLUMN_TIME_START, timeStart);
		val.put(ScheduleProvider.COLUMN_TIME_END, timeEnd);
		
		int interval = 0;

		try {
			interval = Integer.parseInt(mEditTextInterval.getText().toString());
		} catch (NumberFormatException e) {
		}

		val.put(ScheduleProvider.COLUMN_INTERVAL, interval);

		switch (item.getItemId()) {

		case R.id.menu_schedule_save:
			int scheduleId;
			
			if (ACTION_ADD.equals(mIntentAction)) {
				
				/* insert new schedule */
				Uri scheduleUri = getContentResolver().insert(ContentUri.URI_SCHEDULE, val);
				scheduleId = Integer.parseInt(scheduleUri.getLastPathSegment());
				
			} else {
				
				/* update existing schedule */
				getContentResolver().update(
						Uri.withAppendedPath(ContentUri.URI_SCHEDULE,
								String.valueOf(mScheduleId)), val, null, null);

				scheduleId = mScheduleId;
			}
			
			Intent rescheduleIntent = new Intent(ScheduleReceiver.ACTION_RESCHEDULE);
			rescheduleIntent.putExtra(ScheduleReceiver.EXTRA_SCHEDULE_ID, scheduleId);
			
			sendBroadcast(rescheduleIntent);
			
			this.finish();
			
			return true;

		default:
			this.finish();
		}

		return true;
	}

	public class TimePickerDialogFragment extends DialogFragment {

	    private TimePicker mTimePicker;
	    private Calendar mCalendar;
	    private OnTimeChangedListener mListener;

	    public TimePickerDialogFragment(Calendar calendar, OnTimeChangedListener listener) {
	    	mListener = listener;
	    	mCalendar = calendar;
	    }
	    
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {

	        mTimePicker = new TimePicker(this.getActivity());
	        if(mCalendar != null) {
	        	
		        mTimePicker.setCurrentHour(mCalendar.get(Calendar.HOUR_OF_DAY));
		        mTimePicker.setCurrentMinute(mCalendar.get(Calendar.MINUTE));
		        
	        }
	    	
	        Builder builder = new AlertDialog.Builder(this.getActivity());

	        builder.setView(mTimePicker);
	        builder.setMessage("Set time")
	                .setPositiveButton("Set",
	                        new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog, int id) {
	                            	mListener.onTimeChanged(mTimePicker, mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
	                            	getDialog().dismiss();
	                            }
	                        })
	                .setNegativeButton("Cancel",
	                        new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog, int id) {
	                                getDialog().cancel();
	                            }
	                        }); 
	        
	        return builder.create();
	    }
	}

}