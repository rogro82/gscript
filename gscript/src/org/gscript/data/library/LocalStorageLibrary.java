package org.gscript.data.library;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.gscript.R;
import org.gscript.view.LibraryPropertiesView;
import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

@LibraryAttribute(title = "Local storage", description = "Local storage", version = 0, view = LocalStorageLibrary.StorageLibraryView.class)
public class LocalStorageLibrary extends Library {

	static final String PATH_PROPERTY = "path";
	static final String SCRIPT_EXTENSIONS = ".sh";

	Uri mRootPath;

	@Override
	public void onCreate(Context context, Properties properties) {
		mRootPath = Uri.parse(properties.getProperty(PATH_PROPERTY));
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public ArrayList<LibraryItem> onQuery(String path, int flags) {

		ArrayList<LibraryItem> items = new ArrayList<LibraryItem>();

		if (path.endsWith(File.separator)) {

			/* get folder content */

			Uri folderUri = Uri.withAppendedPath(mRootPath, path);
			File folder = new File(folderUri.toString());

			if (folder.exists() && folder.isDirectory()) {
				for (File file : folder.listFiles()) {
					LibraryItem item = null;

					if (file.isDirectory())
						item = new LibraryItem(this, LibraryItem.TYPE_FOLDER,
								file.getPath().replaceFirst(
										mRootPath.toString(), ""));

					if (file.getName().endsWith(SCRIPT_EXTENSIONS))
						item = new LibraryItem(this, LibraryItem.TYPE_SCRIPT,
								file.getPath().replaceFirst(
										mRootPath.toString(), ""));

					if (item != null) {
						if ((flags & Library.FLAG_INCLUDE_PERMISSIONS) != 0)
							item.setPermissions(file.canWrite(),
									file.canWrite(), file.canWrite());

						if ((flags & Library.FLAG_INCLUDE_CONTENT) != 0
								&& file.isFile())
							item.setContent(readFile(file));

						items.add(item);
					}
				}
			}

		} else {

			Uri folderUri = Uri.withAppendedPath(mRootPath, path);
			File file = new File(folderUri.toString());

			if (file.exists()) {

				LibraryItem item = null;

				if (file.isDirectory())
					item = new LibraryItem(this, LibraryItem.TYPE_FOLDER, file
							.getPath().replaceFirst(mRootPath.toString(), ""));

				if (file.getName().endsWith(".sh"))
					item = new LibraryItem(this, LibraryItem.TYPE_SCRIPT, file
							.getPath().replaceFirst(mRootPath.toString(), ""));

				if (item != null) {
					if ((flags & Library.FLAG_INCLUDE_PERMISSIONS) != 0)
						item.setPermissions(file.canWrite(), file.canWrite(),
								file.canWrite());

					if ((flags & Library.FLAG_INCLUDE_CONTENT) != 0
							&& file.isFile())
						item.setContent(readFile(file));

					items.add(item);
				}

			}

		}

		return items;
	}

	String readFile(File file) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(file));
			while ((sCurrentLine = br.readLine()) != null) {
				sb.append(sCurrentLine + "\n");
			}

		} catch (IOException e) {
		} finally {
			try {

				if (br != null)
					br.close();

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return sb.toString();
	}

	public static class StorageLibraryView extends LibraryPropertiesView {

		EditText mEditTextPath;
		boolean mValid;

		public StorageLibraryView(Context context, Properties properties,
				LibraryPropertiesListener listener) {
			super(context, properties, listener);

			final LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.library_localstorage, null,
					false);

			this.addView(view, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);

			mEditTextPath = (EditText) view.findViewById(R.id.editTextPath);
			mEditTextPath.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {

					mValid = (s.length() > 0 && s.charAt(0) == '/');

					getProperties().setProperty(PATH_PROPERTY, s.toString());

					notifyPropertiesChanged(mValid);

				}

			});

			mEditTextPath.setText(properties.getProperty(PATH_PROPERTY));
		}

		@Override
		public boolean hasValidProperties() {
			return mValid;
		}
	}
}
