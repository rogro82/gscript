package org.gscript.view;

import java.util.ArrayList;

import org.gscript.R;
import org.gscript.data.library.ItemAttributes;
import org.gscript.settings.ShellProfile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class ItemAttributesView extends LinearLayout {

	ItemAttributes mAttributes = new ItemAttributes();

	public ItemAttributesView(Context context) {
		super(context);
		initView();
	}

	public ItemAttributesView(Context context, ItemAttributes attributes) {
		super(context);
		mAttributes.putAll(attributes);
		initView();
	}

	void initView() {

		final LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = inflater.inflate(R.layout.item_attributes, null, false);

		addView(view, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

		/* attribute shell profile */

		final Spinner spinnerShellProfile = (Spinner) findViewById(R.id.spinnerShellProfile);

		String defaultKey = ShellProfile.getDefaultOrFirstKey(getContext());

		String selectedKey = mAttributes
				.containsKey(ItemAttributes.ATTRIBUTE_SHELL) ? mAttributes
				.get(ItemAttributes.ATTRIBUTE_SHELL) : "default";

		int selectedIndex = 0;

		final ArrayList<ShellProfileItem> profiles = new ArrayList<ShellProfileItem>();
		ArrayList<String> keys = ShellProfile.getKeys(getContext());

		profiles.add(new ShellProfileItem("default", "Default profile - "
				+ ShellProfile.getName(getContext(), defaultKey)));

		int index = 1;
		for (String key : keys) {
			profiles.add(new ShellProfileItem(key, ShellProfile.getName(
					getContext(), key)));
			if (key.equalsIgnoreCase(selectedKey))
				selectedIndex = index;

			index++;
		}

		ArrayAdapter<ShellProfileItem> adapter = new ArrayAdapter<ShellProfileItem>(
				getContext(), android.R.layout.simple_spinner_item, profiles);

		spinnerShellProfile.setAdapter(adapter);
		spinnerShellProfile.setSelection(selectedIndex);
		spinnerShellProfile
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {

						if (pos == 0) {
							mAttributes.remove(ItemAttributes.ATTRIBUTE_SHELL);
						} else {
							mAttributes.put(ItemAttributes.ATTRIBUTE_SHELL,
									profiles.get(pos).key);
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		/* attribute unattended */

		final CheckBox checkBoxUnattended = (CheckBox) findViewById(R.id.checkBoxAttributeUnattended);
		checkBoxUnattended.setChecked(mAttributes
				.containsKey(ItemAttributes.ATTRIBUTE_UNATTENDED));

		checkBoxUnattended
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							mAttributes
									.put(ItemAttributes.ATTRIBUTE_UNATTENDED,
											"true");
						} else {
							mAttributes
									.remove(ItemAttributes.ATTRIBUTE_UNATTENDED);
						}
					}
				});
	}

	public ItemAttributes getAttributes() {
		return mAttributes;
	}

	class ShellProfileItem {
		public ShellProfileItem(String key, String name) {
			this.key = key;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		String key;
		String name;
	}

}
