package org.gscript;

import org.gscript.data.ContentUri;
import org.gscript.data.ContentUri.LibraryPathSegments;
import org.gscript.data.LibraryProvider;
import org.gscript.data.library.ItemAttributes;
import org.gscript.data.library.ItemConditions;
import org.gscript.data.library.LibraryItem;
import org.gscript.view.ItemAttributesView;
import org.gscript.view.ItemConditionsView;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class LibraryItemActivity extends SherlockActivity {

	static final String LOG_TAG = "LibraryActivity";
	static final String ACTION_EDIT = "org.gscript.LibraryItemActivity.action_edit";
	static final String EXTRA_ITEM_TYPE = "type";
	static final String EXTRA_ITEM_PATH = "path";

	String mIntentAction;
	Uri mItemPath;

	ItemAttributesView mAttributesView;
	ItemConditionsView mConditionsView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_library_item);

		Intent intent = this.getIntent();

		mIntentAction = intent.getAction();

		mItemPath = intent.getParcelableExtra(EXTRA_ITEM_PATH);

		ViewGroup placeholderAttributes = (ViewGroup) this
				.findViewById(R.id.placeholder_item_attributes);

		ViewGroup placeholderConditions = (ViewGroup) this
				.findViewById(R.id.placeholder_item_conditions);

		if (ACTION_EDIT.equals(mIntentAction)) {

			Cursor itemCursor = this.getContentResolver().query(mItemPath,
					null, null, null, null);
			
			if (itemCursor.moveToFirst()) {

				LibraryPathSegments seg = LibraryPathSegments.parse(mItemPath);

				LibraryItem item = LibraryItem.fromCursor(itemCursor);
				this.setTitle(item.getName());

				/* item attributes */
				{
					Uri attributesUri = ContentUri.URI_ITEM_ATTRIBS_PATH(seg.id,
							seg.path);
					
					Cursor cursorAttributes = this.getContentResolver().query(
							attributesUri, null, null, null, null);

					mAttributesView = new ItemAttributesView(this,
							new ItemAttributes(cursorAttributes));
					placeholderAttributes.addView(mAttributesView,
							LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);

					cursorAttributes.close();
				}

				/* item conditions */
				{
					Uri conditionsUri = ContentUri.URI_ITEM_CONDITIONS_PATH(seg.id,
							seg.path);
					
					
					Cursor cursorConditions = this.getContentResolver().query(
							conditionsUri, null, null, null, null);

					mConditionsView = new ItemConditionsView(this,
							new ItemConditions(cursorConditions));
					
					placeholderConditions.addView(mConditionsView,
							LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);

					cursorConditions.close();
				}

			} else {
				this.finish();
			}

			itemCursor.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.library_item, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_library_item_save:

			LibraryPathSegments seg = LibraryPathSegments.parse(mItemPath);

			/* attributes */
			{
				Uri attributesUri = ContentUri.URI_ITEM_ATTRIBS_PATH(seg.id,
						seg.path);

				/* delete all previous set attributes */

				this.getContentResolver().delete(attributesUri, null, null);

				/* insert new attributes */

				ItemAttributes attributes = mAttributesView.getAttributes();
				for (String attribute : attributes.keySet()) {

					ContentValues values = new ContentValues();
					values.put(LibraryProvider.COLUMN_KEY, attribute);
					values.put(LibraryProvider.COLUMN_VALUE,
							attributes.get(attribute));

					this.getContentResolver().insert(attributesUri, values);
				}
			}
			/* conditions */
			{
				Uri conditionsUri = ContentUri.URI_ITEM_CONDITIONS_PATH(seg.id,
						seg.path);

				/* delete all previous set attributes */

				this.getContentResolver().delete(conditionsUri, null, null);

				/* insert new attributes */

				ItemConditions conditions = mConditionsView.getConditions();
				for (String condition : conditions.keySet()) {

					ContentValues values = new ContentValues();
					values.put(LibraryProvider.COLUMN_KEY, condition);
					values.put(LibraryProvider.COLUMN_VALUE,
							conditions.get(condition));

					this.getContentResolver().insert(conditionsUri, values);
				}
			}
			
			this.finish();

		default:
			this.finish();
		}

		return true;
	}
}
