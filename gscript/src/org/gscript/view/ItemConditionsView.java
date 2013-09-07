package org.gscript.view;

import java.util.ArrayList;

import org.gscript.R;
import org.gscript.data.ContentUri;
import org.gscript.data.ScheduleProvider;
import org.gscript.data.library.ItemConditions;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ItemConditionsView extends LinearLayout {

	ItemConditions mConditions = new ItemConditions();

	public ItemConditionsView(Context context) {
		super(context);
		initView();
	}

	public ItemConditionsView(Context context, ItemConditions conditions) {
		super(context);
		mConditions.putAll(conditions);
		initView();
	}

	void initView() {

		final LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = inflater.inflate(R.layout.item_conditions, null, false);

		addView(view, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		
		final CheckBox checkBoxBoot = (CheckBox) findViewById(R.id.checkBoxConditionBoot);
		checkBoxBoot.setChecked(mConditions
				.containsKey(ItemConditions.CONDITION_BOOT));

		checkBoxBoot.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					mConditions.put(ItemConditions.CONDITION_BOOT, "true");
				} else {
					mConditions.remove(ItemConditions.CONDITION_BOOT);
				}
			}
		});
		
		/* schedules */
		
		final Spinner spinnerSchedule = (Spinner) findViewById(R.id.spinnerSchedule);

		String selectedId = mConditions
				.containsKey(ItemConditions.CONDITION_SCHEDULE) ? mConditions
				.get(ItemConditions.CONDITION_SCHEDULE) : "";

		int selectedIndex = 0;

		final ArrayList<ScheduleItem> schedules = new ArrayList<ScheduleItem>();
		schedules.add(new ScheduleItem("", "No schedule"));

		int index = 1;
		
		Cursor c = getContext().getContentResolver().query(ContentUri.URI_SCHEDULE, null, null, null, null);
		while(c.moveToNext()) {
			
			String scheduleId = String.valueOf(c.getInt(c.getColumnIndex(ScheduleProvider.COLUMN_ID)));
			String scheduleTitle = c.getString(c.getColumnIndex(ScheduleProvider.COLUMN_TITLE));
			
			schedules.add(new ScheduleItem(scheduleId, scheduleTitle));
			
			if(scheduleId.equals(selectedId)) {
				selectedIndex = index;
			}
			index++;
		}

		ArrayAdapter<ScheduleItem> adapter = new ArrayAdapter<ScheduleItem>(
				getContext(), android.R.layout.simple_spinner_item, schedules);

		spinnerSchedule.setAdapter(adapter);
		spinnerSchedule.setSelection(selectedIndex);
		spinnerSchedule
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {

						if (pos == 0) {
							mConditions.remove(ItemConditions.CONDITION_SCHEDULE);
						} else {
							mConditions.put(ItemConditions.CONDITION_SCHEDULE,
									schedules.get(pos).id);
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
	}

	public ItemConditions getConditions() {
		return mConditions;
	}	
	
	class ScheduleItem {
		
		public ScheduleItem(String id, String title) {
			this.id = id;
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}

		String id;
		String title;
	}

	
}
