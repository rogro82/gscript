package org.gscript;

import org.gscript.process.ProcessService;
import org.gscript.view.ItemAttributesView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class ExecuteDialog extends SherlockActivity {

	public static final String ACTION_SHORTCUT = "org.gscript.action.SHORTCUT";
	ItemAttributesView mAttributesView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = this.getIntent();
		final Uri data = intent.getData();
		
		this.setContentView(R.layout.activity_execute_dialog);

		TextView textViewFile = (TextView) findViewById(R.id.textViewFile);
		textViewFile.setText(String.format("Execution requested for %s", data));

		ViewGroup placeholderAttributes = (ViewGroup) this
				.findViewById(R.id.placeholder_item_attributes);

		if(intent.getAction().equals(ACTION_SHORTCUT)) {
			placeholderAttributes.setVisibility(View.GONE);
		} else {
			/* not a shortcut action so show item attributes */
			mAttributesView = new ItemAttributesView(this);
			placeholderAttributes.addView(mAttributesView,
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		}
		
		Button btnExecute = (Button) findViewById(R.id.buttonExecute);
		btnExecute.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent processIntent;

				/* directly execute script from service */

				processIntent = new Intent(v.getContext(), ProcessService.class);
				processIntent.setAction(ProcessService.ACTION_EXECUTE);

				/* add attributes if available */
				
				if(mAttributesView != null) {
					processIntent.putExtra(ProcessService.EXTRA_ATTRIBUTES,
							mAttributesView.getAttributes());
				}
				
				processIntent.setData(data);
				startService(processIntent);

				finish();

			}
		});

		Button btnCancel = (Button) findViewById(R.id.buttonCancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	public static Intent createShortcutIntent(Context context, Uri data) {
		
		Intent shortcutIntent = new Intent(context,
				ExecuteDialog.class);
		shortcutIntent
				.setAction(ExecuteDialog.ACTION_SHORTCUT);
		shortcutIntent.setData(data);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Bitmap icon = BitmapFactory.decodeResource(
				context.getResources(), R.drawable.ic_launcher);

		final Intent broadcastIntent = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		broadcastIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
				shortcutIntent);
		broadcastIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				data.getLastPathSegment());
		if (icon != null)
			broadcastIntent.putExtra(
					Intent.EXTRA_SHORTCUT_ICON, icon);
		
		return broadcastIntent;
	}	

}
