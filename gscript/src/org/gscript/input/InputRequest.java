package org.gscript.input;

import java.util.Locale;

import android.os.Bundle;

public class InputRequest {

	public static final String EXTRA_REQUEST_ID = "request_id";
	public static final String EXTRA_REQUEST_TYPE = "request_type";

	public static final String EXTRA_TITLE = "title";
	public static final String EXTRA_MESSAGE = "message";
	public static final String EXTRA_STYLE = "style";
	public static final String EXTRA_LIST = "list";
	
	public static final String TYPE_DIALOG_INFO = "dialog-info";
	public static final String TYPE_DIALOG_WARNING = "dialog-warning";
	public static final String TYPE_DIALOG_ERROR = "dialog-error";
	public static final String TYPE_DIALOG_MESSAGE = "dialog-message";
	public static final String TYPE_TEXT_ENTRY = "text-entry";
	public static final String TYPE_LIST = "list";
	public static final String TYPE_TOAST = "toast";

	public int id;
	public String type;
	public String path;
	public Bundle extras;

	InputRequest(int id, String type, String path, Bundle extras) {

		this.id = id;
		this.type = type;
		this.path = path;
		this.extras = extras;

	}

	@Override
	public String toString() {
		return String.format(Locale.getDefault(),
				"InputRequest [id:%d, type:%s, path:%s, extras:%s]", id, type,
				path, extras);
	}

}
