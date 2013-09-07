package org.gscript.data.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.gscript.R;
import org.gscript.view.LibraryPropertiesView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

@LibraryAttribute(title = "Google Drive", description = "Google Drive", version = 0, view = GoogleDriveLibrary.GoogleDriveLibraryView.class)
public class GoogleDriveLibrary extends Library {

	static final String LOG_TAG = "GoogleDriveLibrary";

	static final String ACCOUNT_PROPERTY = "account";
	static final String SCRIPT_EXTENSIONS = "sh";
	static final String TEXT_PLAIN_MIMETYPE = "text/plain";

	static final long REFRESH_REPEAT_DELAY = 5000;

	private Drive mDriveService;
	private GoogleAccountCredential mCredential;
	HashMap<String, LibraryItem> mCachedItems = new HashMap<String, LibraryItem>();

	String mAccount;
	Handler mSyncHandler;

	volatile boolean mSyncStop;
	volatile long mSyncScheduled = 0;
	volatile long mSyncTime = 0;

	@Override
	public void onCreate(Context context, Properties properties) {

		mCredential = GoogleAccountCredential.usingOAuth2(context,
				DriveScopes.DRIVE_READONLY);

		mAccount = properties.getProperty(ACCOUNT_PROPERTY);
		mCredential.setSelectedAccountName(mAccount);
		mDriveService = getDriveService(mCredential);

		restoreCache();

		mSyncThread.start();
	}

	@Override
	public void onDestroy() {
		mSyncStop = true;
	}

	@Override
	public ArrayList<LibraryItem> onQuery(String path, int flags) {

		/* check if we have a refresh flag and not repeating refreshes */
		if ((flags & Library.FLAG_MANUAL_REFRESH) != 0
				&& ((System.currentTimeMillis() - mSyncTime) > REFRESH_REPEAT_DELAY)) {
			mSyncScheduled = 0;
		}

		ArrayList<LibraryItem> items = new ArrayList<LibraryItem>();
		Uri pathUri = Uri.parse(path);

		synchronized (mCachedItems) {

			if (pathUri.getPathSegments().size() > 1) {
				/* try to get single file */
				final LibraryItem item = mCachedItems.get(path);
				
				if (item != null)
					items.add(item);

			} else {
				/* return all files */
				items.addAll(mCachedItems.values());
			}
		}

		return items;
	}

	Thread mSyncThread = new Thread("GoogleDriveSync " + this.getId()) {

		@Override
		public void run() {

			while (!mSyncStop) {

				long currentTime = System.currentTimeMillis();
				if (currentTime > mSyncScheduled) {
					/* start sync */

					boolean success = true;

					Log.d(LOG_TAG, "GoogleDriveSync started");

					List<File> result = new ArrayList<File>();

					try {

						Files.List request = mDriveService
								.files()
								.list()
								.setQ("mimeType = '" + TEXT_PLAIN_MIMETYPE
										+ "'");

						do {

							FileList files = request.execute();
							result.addAll(files.getItems());
							request.setPageToken(files.getNextPageToken());

						} while (request.getPageToken() != null
								&& request.getPageToken().length() > 0);

					} catch (Exception ex) {
						success = false;
					}

					if (success) {

						boolean itemsUpdated = false;

						final ArrayList<LibraryItem> items = new ArrayList<LibraryItem>();

						for (File file : result) {

							if (SCRIPT_EXTENSIONS.equals(file
									.getFileExtension())) {

								String itemPath = String.format("/%s/%s",
										file.getId(), file.getTitle());

								/* get current cached item for path */

								LibraryItem item = mCachedItems.get(itemPath);

								if (item == null) {
									/* create new item */
									item = new LibraryItem(
											GoogleDriveLibrary.this,
											LibraryItem.TYPE_SCRIPT, itemPath);
								}

								if (!item.hasContent()
										|| item.getModifiedDate() != file
												.getModifiedDate().getValue()) {

									/*
									 * either no content or the file has been
									 * modified so download content
									 */

									try {

										Log.d(LOG_TAG, "fetching content...");

										HttpResponse resp = mDriveService
												.getRequestFactory()
												.buildGetRequest(
														new GenericUrl(
																file.getDownloadUrl()))
												.execute();

										InputStream is = resp.getContent();
										StringBuilder sb = new StringBuilder();
										BufferedReader reader = new BufferedReader(
												new InputStreamReader(is));

										String line;
										while ((line = reader.readLine()) != null) {
											sb.append(line);
											sb.append('\n');
										}

										item.setContent(sb.toString());
										item.setModifiedDate(file
												.getModifiedDate().getValue());

									} catch (IOException e) {
									}

									itemsUpdated = true;
								}

								items.add(item);
							}
						}

						/* insert synced items to cache */

						synchronized (mCachedItems) {

							int prevSize = mCachedItems.size();

							mCachedItems.clear();

							int count = items.size();
							for (int i = 0; i < count; ++i) {

								final LibraryItem item = items.get(i);
								mCachedItems.put(item.path, item);
							}

							Log.d(LOG_TAG, "successfully synced " + count
									+ " items...");

							/* notify that we have new content */
							if (mCachedItems.size() != prevSize || itemsUpdated) {
								GoogleDriveLibrary.this.notifyChange();

								/* serialize data */
								saveCache(items);

							} else {
								Log.d(LOG_TAG,
										"cache still valid no need to notify");
							}
						}

						/* resync in 60 minutes */
						mSyncScheduled = System.currentTimeMillis()
								+ (1000 * 60 * 60);

						mSyncTime = System.currentTimeMillis();

					} else {

						Log.d(LOG_TAG, "failed to sync items...");

						/* retry in 30 minutes */

						mSyncScheduled = System.currentTimeMillis()
								+ (1000 * 60 * 30);
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
	};

	public void saveCache(ArrayList<LibraryItem> items) {

		java.io.File file = new java.io.File(this.getContext().getCacheDir(),
				String.format("library-%d.cache", this.getId()));

		if (LibraryItem.serializeItems(items, file.getPath())) {
			Log.d(LOG_TAG, "succesfully saved library cache..");
		}

	}

	public void restoreCache() {

		ArrayList<LibraryItem> deserializedItems = new ArrayList<LibraryItem>();

		java.io.File file = new java.io.File(this.getContext().getCacheDir(),
				String.format("library-%d.cache", this.getId()));

		if (LibraryItem.deserializeItems(deserializedItems, file.getPath())) {
			for (LibraryItem item : deserializedItems) {
				mCachedItems.put(item.path, item);

				Log.d(LOG_TAG, String.format(
						"restored cached item path: %s moddate: %d", item.path,
						item.moddate));
			}

			Log.d(LOG_TAG, "succesfully restored library cache.. "
					+ mCachedItems.size() + " items restored");
		}
	}

	public static class GoogleDriveLibraryView extends LibraryPropertiesView {

		static final int REQUEST_ACCOUNT_PICKER = 1;
		static final int REQUEST_AUTHORIZATION = 2;

		private Drive mService;
		private GoogleAccountCredential mCredential;

		Activity mActivity;
		String mAccount;
		boolean mValid;

		public GoogleDriveLibraryView(Context context, Properties properties,
				LibraryPropertiesListener listener) {
			super(context, properties, listener);

			final LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.library_googledrive, null,
					false);

			this.addView(view, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);

			Button buttonChooseAccount = (Button) findViewById(R.id.buttonChooseAccount);
			buttonChooseAccount.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					mActivity = (Activity) GoogleDriveLibraryView.this
							.getContext();
					mCredential = GoogleAccountCredential.usingOAuth2(
							mActivity, DriveScopes.DRIVE_READONLY);

					try {

						mActivity.startActivityForResult(
								mCredential.newChooseAccountIntent(),
								REQUEST_ACCOUNT_PICKER);

					} catch (ActivityNotFoundException e) {

					}
				}
			});

			mAccount = getProperties().getProperty(ACCOUNT_PROPERTY);

			notifyPropertiesChanged(hasValidProperties());
		}

		@Override
		public void onActivityResult(final int requestCode,
				final int resultCode, final Intent data) {

			switch (requestCode) {
			case REQUEST_ACCOUNT_PICKER:
				if (resultCode == Activity.RESULT_OK && data != null
						&& data.getExtras() != null) {
					mAccount = data
							.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

					if (mAccount != null) {

						getProperties().setProperty(ACCOUNT_PROPERTY, mAccount);

						mCredential.setSelectedAccountName(mAccount);
						mService = getDriveService(mCredential);

						new TestAuthorizationTask().execute();
					}
				}
				break;
			case REQUEST_AUTHORIZATION:
				new TestAuthorizationTask().execute(false);
				break;
			}
		}

		@Override
		public boolean hasValidProperties() {
			return (mAccount != null && mAccount.length() > 0);
		}

		private class TestAuthorizationTask extends
				AsyncTask<Boolean, Void, Void> {

			@Override
			protected Void doInBackground(Boolean... params) {

				boolean first = (params.length > 0) ? params[0] : true;

				/* test auth by trying to get one file */
				try {

					List<File> result = new ArrayList<File>();
					Files.List request = mService.files().list()
							.setQ("mimeType = '" + TEXT_PLAIN_MIMETYPE + "'")
							.setMaxResults(1);

					try {

						FileList files = request.execute();
						result.addAll(files.getItems());

						Log.d(LOG_TAG, "authorization succeeded");

					} catch (UserRecoverableAuthIOException e) {

						if (mActivity != null && first) {

							Log.d(LOG_TAG, "authorization failed");
							mActivity.startActivityForResult(e.getIntent(),
									REQUEST_AUTHORIZATION);
						} else {
							Toast.makeText(getContext(),
									"Authentication failed", Toast.LENGTH_LONG)
									.show();
						}

					} catch (IOException e) {
						Log.d(LOG_TAG, "IOException");
					}

				} catch (IOException ex) {
					Log.d(LOG_TAG, "Exception");
				}

				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				notifyPropertiesChanged(hasValidProperties());				
			}
		}
	}

	private static Drive getDriveService(GoogleAccountCredential credential) {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
				new GsonFactory(), credential).build();
	}
}
