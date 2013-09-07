package org.gscript;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.gscript.data.HistoryProvider;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class HistoryActivity extends SherlockActivity {

	static final String LOG_TAG = "HistoryActivity";

	String mTranscript;
	int mState;
	long mTimeStart;
	long mTimeEnd;
	String mDuration;
	Intent mIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_history);

		Intent intent = this.getIntent();

		Cursor cursor = this.getContentResolver().query(intent.getData(), null,
				null, null, null);

		if (cursor.moveToFirst()) {

			String intentStr = cursor.getString(cursor
					.getColumnIndex(HistoryProvider.COLUMN_INTENT));
			try {

				mIntent = Intent.parseUri(intentStr, 0);
				setTitle(mIntent.getData().getLastPathSegment());

			} catch (URISyntaxException e) {
			}

			mTranscript = cursor.getString(cursor
					.getColumnIndex(HistoryProvider.COLUMN_LOG));
			
			mState = cursor.getInt(cursor
					.getColumnIndex(HistoryProvider.COLUMN_STATE));
			
			mTimeStart = cursor.getLong(cursor
					.getColumnIndex(HistoryProvider.COLUMN_TIME_START));
			
			mTimeEnd = cursor.getLong(cursor
					.getColumnIndex(HistoryProvider.COLUMN_TIME_END));
			
			mDuration = ((mTimeEnd - mTimeStart) / 1000) + " second(s)";

			Date startDate = new Date(mTimeStart);

			SimpleDateFormat date_format = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss", Locale.getDefault());

			TextView textViewDate = (TextView) findViewById(R.id.textViewDate);
			textViewDate.setText(date_format.format(startDate));

			TextView textViewDuration = (TextView) findViewById(R.id.textViewDuration);
			textViewDuration.setText(mDuration);

			TextView textViewTranscript = (TextView) findViewById(R.id.textViewTranscript);
			textViewTranscript.setText(mTranscript);

		} else {
			finish();
		}

		cursor.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.history, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_history_email:
			
            String addr = "";
            Intent intent =
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"
                            + addr));

            String title = "Process transcript";
            
			Date startDate = new Date(mTimeStart);
			SimpleDateFormat date_format = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss", Locale.getDefault());            

            String text = mIntent.getData().getLastPathSegment() + "\n\n";
            text += "Started on: \n"+date_format.format(startDate)+"\n\n";
            text += "Duration: \n"+mDuration + "\n\n";
            text += "Transcript:\n" + mTranscript;

            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TEXT,
            		text);
            try {
                startActivity(Intent.createChooser(intent,
                        "Send transcript"));
            } catch (ActivityNotFoundException e) {
            	
                Toast.makeText(this,
                        "No activity found to send transcript",
                        Toast.LENGTH_LONG).show();
            }		
			
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
