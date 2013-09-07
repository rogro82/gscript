package org.gscript.input;

import java.util.ArrayList;
import org.gscript.R;
import org.gscript.input.InputReceiver;
import org.gscript.input.InputRequest;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class InputDialog extends SherlockActivity {

	static final String TYPE_RADIO = "radio";
	static final String STYLE_YESNO = "yesno";

	int mRequestId;
	String mRequestType;

	LinearLayout linearLayoutControls;
	TextView mTextViewMessage;
	Button mButtonCancel;
	Button mButtonOk;
	View mButtonDivider;
	InputControlView mControlView;

	boolean mResponseSend;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_input_dialog);

		Intent intent = this.getIntent();

		mRequestId = intent.getIntExtra(InputRequest.EXTRA_REQUEST_ID, -1);
		mRequestType = intent.getStringExtra(InputRequest.EXTRA_REQUEST_TYPE);

		linearLayoutControls = (LinearLayout) findViewById(R.id.linearLayoutControls);
		mTextViewMessage = (TextView) findViewById(R.id.textViewMessage);
		mButtonCancel = (Button) findViewById(R.id.buttonCancel);
		mButtonOk = (Button) findViewById(R.id.buttonOk);
		mButtonDivider = (View) findViewById(R.id.buttonDivider);

		this.setTitle(intent.getStringExtra(InputRequest.EXTRA_TITLE));

		mTextViewMessage.setText(intent
				.getStringExtra(InputRequest.EXTRA_MESSAGE));

		mButtonOk.setText(R.string.ok);
		mButtonOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				String responseOpt = "";
				if (mControlView != null)
					responseOpt = mControlView.getValue();

				Intent intent = new Intent(InputReceiver.ACTION_RESPONSE);

				intent.putExtra(InputReceiver.EXTRA_REQUEST_ID, mRequestId)
						.putExtra(InputReceiver.EXTRA_RESPONSE_CODE, 1)
						.putExtra(InputReceiver.EXTRA_RESPONSE_OPT, responseOpt);

				sendBroadcast(intent);
				mResponseSend = true;

				finish();
			}
		});

		mButtonCancel.setText(R.string.cancel);
		mButtonCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendCancelBroadcast();
				finish();
			}
		});

		if (InputRequest.TYPE_DIALOG_MESSAGE.equalsIgnoreCase(mRequestType)) {
			showCancel(true);

			String style = intent.getStringExtra(InputRequest.EXTRA_STYLE);
			if (STYLE_YESNO.equalsIgnoreCase(style)) {

				mButtonOk.setText(R.string.yes);
				mButtonCancel.setText(R.string.no);
			}
		}

		if (InputRequest.TYPE_TEXT_ENTRY.equalsIgnoreCase(mRequestType)) {
			showCancel(true);
			mControlView = new TextControlView(this);
		}
		if (InputRequest.TYPE_LIST.equalsIgnoreCase(mRequestType)) {
			showCancel(true);

			String style = intent.getStringExtra(InputRequest.EXTRA_STYLE);
			String list = intent.getStringExtra(InputRequest.EXTRA_LIST);

			ArrayList<ListItem> listItems = getListItems(list);

			if (TYPE_RADIO.equalsIgnoreCase(style)) {
				mControlView = new RadioControlView(this, listItems);
			} else {
				mControlView = new CheckboxControlView(this, listItems);
			}
		}

		if (mControlView != null) {
			linearLayoutControls.addView(mControlView,
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		}
	}

	void showCancel(boolean show) {

		mButtonCancel.setVisibility((show) ? View.VISIBLE : View.GONE);
		mButtonDivider.setVisibility((show) ? View.VISIBLE : View.GONE);
	}

	void sendCancelBroadcast() {

		Intent intent = new Intent(InputReceiver.ACTION_RESPONSE);

		intent.putExtra(InputReceiver.EXTRA_REQUEST_ID, mRequestId).putExtra(
				InputReceiver.EXTRA_RESPONSE_CODE, 0);

		sendBroadcast(intent);

		mResponseSend = true;
	}

	abstract class InputControlView extends LinearLayout {

		public InputControlView(Context context) {
			super(context);

			Resources r = getResources();
			int px = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics());
			
			this.setPadding(0, px, 0, 0);

			this.setOrientation(VERTICAL);
		}

		public abstract String getValue();
	}

	class TextControlView extends InputControlView {
		EditText mEditText;

		public TextControlView(Context context) {
			super(context);

			mEditText = new EditText(context);
			this.addView(mEditText, LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
		}

		@Override
		public String getValue() {
			return mEditText.getText().toString();
		}
	}

	class CheckboxControlView extends InputControlView {

		ArrayList<CheckBox> mCheckBoxes = new ArrayList<CheckBox>();

		public CheckboxControlView(Context context, ArrayList<ListItem> items) {
			super(context);

			for (ListItem item : items) {

				CheckBox cb = new CheckBox(context);

				cb.setText(item.value);
				cb.setChecked(item.checked);
				cb.setTag(item.key);

				this.addView(cb, LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);

				mCheckBoxes.add(cb);
			}
		}

		@Override
		public String getValue() {
			String checkedArray = "";

			int count = mCheckBoxes.size();
			for (int i = 0; i < count; ++i) {
				CheckBox cb = mCheckBoxes.get(i);
				if (cb.isChecked()) {
					checkedArray += cb.getTag().toString() + " ";
				}
			}
			checkedArray = checkedArray.trim();

			return checkedArray;
		}
	}

	class RadioControlView extends InputControlView {

		RadioGroup mRadioGroup;

		public RadioControlView(Context context, ArrayList<ListItem> items) {
			super(context);

			mRadioGroup = new RadioGroup(context);

			int checkedIndex = -1;
			for (ListItem item : items) {

				RadioButton rb = new RadioButton(context);

				rb.setText(item.value);
				rb.setTag(item.key);

				mRadioGroup.addView(rb, LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);

				/* only allow one radiobutton to be pre-checked */
				if (item.checked && checkedIndex == -1)
					checkedIndex = rb.getId();

			}
			mRadioGroup.check(checkedIndex);

			this.addView(mRadioGroup, LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
		}

		@Override
		public String getValue() {

			int checkedId = mRadioGroup.getCheckedRadioButtonId();

			return (checkedId == -1 ? "" : mRadioGroup.findViewById(checkedId)
					.getTag().toString());
		}
	}

	public ArrayList<ListItem> getListItems(String itemString) {

		ArrayList<ListItem> listItems = new ArrayList<ListItem>();

		/* build hashmap with key/value pairs */

		String[] listArr = itemString.split(";");
		for (String kvPair : listArr) {
			kvPair = kvPair.trim();

			if (kvPair.length() > 0) {

				ListItem item = new ListItem();

				String[] kvArr = kvPair.split(":");

				item.key = kvArr[0];
				item.key = item.key.trim().replace(" ", "_");

				/* check if pre-checked */
				if (item.key.startsWith("*")) {
					item.key = item.key.substring(1, item.key.length()).trim();
					item.checked = true;
				}
				
				item.value = (kvArr.length == 1) ? item.key : kvArr[1].trim();

				listItems.add(item);
			}
		}
		return listItems;
	}

	class ListItem {
		String key;
		String value;
		boolean checked;
	}
}
