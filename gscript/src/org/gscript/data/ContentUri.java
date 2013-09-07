package org.gscript.data;

import java.util.List;

import android.content.UriMatcher;
import android.net.Uri;

public class ContentUri {

	static final String HISTORY_AUTHORITY = "org.gscript.history";
	static final String LIBRARY_AUTHORITY = "org.gscript.library";
	static final String SCHEDULE_AUTHORITY = "org.gscript.schedule";

	static final String CONTENT_SCHEME = "content://";

	static final String HISTORY_BASE_PATH = "history";
	static final String LIBRARY_BASE_PATH = "library";
	static final String ITEM_ATTRIBUTES_BASE_PATH = "attributes";
	static final String ITEM_CONDITIONS_BASE_PATH = "conditions";
	static final String SCHEDULE_BASE_PATH = "schedule";

	static final int MAX_SUBPATHS = 100;

	public static final int MATCH_HISTORY = 0;
	public static final int MATCH_HISTORY_ITEM = MATCH_HISTORY + 1;
	public static final int MATCH_SCHEDULE = 20;
	public static final int MATCH_SCHEDULE_ITEM = MATCH_SCHEDULE + 1;
	public static final int MATCH_LIBRARY = 40;
	public static final int MATCH_LIBRARY_ITEM = MATCH_LIBRARY + 1;
	public static final int MATCH_LIBRARY_PATH = MATCH_LIBRARY + 2;
	public static final int MATCH_ITEM_ATTRIBS = 60;
	public static final int MATCH_ITEM_ATTRIBS_PATH = MATCH_ITEM_ATTRIBS + 1;
	public static final int MATCH_ITEM_CONDITIONS = 80;
	public static final int MATCH_ITEM_CONDITIONS_PATH = MATCH_ITEM_CONDITIONS + 1;

	public static final String QUERY_FLAGS = "flags";

	public static final Uri URI_HISTORY = Uri.parse(CONTENT_SCHEME
			+ HISTORY_AUTHORITY + "/" + HISTORY_BASE_PATH);

	public static final Uri URI_SCHEDULE = Uri.parse(CONTENT_SCHEME
			+ SCHEDULE_AUTHORITY + "/" + SCHEDULE_BASE_PATH);

	public static final Uri URI_LIBRARY = Uri.parse(CONTENT_SCHEME
			+ LIBRARY_AUTHORITY + "/" + LIBRARY_BASE_PATH);

	public static final Uri URI_ITEM_ATTRIBUTES = Uri.parse(CONTENT_SCHEME
			+ LIBRARY_AUTHORITY + "/" + ITEM_ATTRIBUTES_BASE_PATH);

	public static final Uri URI_ITEM_CONDITIONS = Uri.parse(CONTENT_SCHEME
			+ LIBRARY_AUTHORITY + "/" + ITEM_CONDITIONS_BASE_PATH);

	public static final Uri URI_LIBRARY_PATH(int library, String path) {
		return URI_LIBRARY_PATH(library, path, 0);
	}

	public static final Uri URI_LIBRARY_PATH(int library, String path, int flags) {

		if (!path.startsWith("/"))
			path = "/" + path;

		String flagsQuery = (flags != 0) ? "?" + QUERY_FLAGS + "=" + flags : "";

		Uri query = Uri.parse(CONTENT_SCHEME + LIBRARY_AUTHORITY + "/"
				+ LIBRARY_BASE_PATH + "/" + library + "/path" + path
				+ flagsQuery);

		return query;
	}

	public static final Uri URI_ITEM_ATTRIBS_PATH(int library, String path) {

		if (!path.startsWith("/"))
			path = "/" + path;

		Uri query = Uri.parse(CONTENT_SCHEME + LIBRARY_AUTHORITY + "/"
				+ ITEM_ATTRIBUTES_BASE_PATH + "/" + library + path);

		return query;
	}

	public static final Uri URI_ITEM_CONDITIONS_PATH(int library, String path) {

		if (!path.startsWith("/"))
			path = "/" + path;

		Uri query = Uri.parse(CONTENT_SCHEME + LIBRARY_AUTHORITY + "/"
				+ ITEM_CONDITIONS_BASE_PATH + "/" + library + path);

		return query;
	}

	public static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {

		/* history uris */
		MATCHER.addURI(HISTORY_AUTHORITY, HISTORY_BASE_PATH, MATCH_HISTORY);
		MATCHER.addURI(HISTORY_AUTHORITY, HISTORY_BASE_PATH + "/#",
				MATCH_HISTORY_ITEM);

		/* schedule uris */
		MATCHER.addURI(SCHEDULE_AUTHORITY, SCHEDULE_BASE_PATH, MATCH_SCHEDULE);
		MATCHER.addURI(SCHEDULE_AUTHORITY, SCHEDULE_BASE_PATH + "/#",
				MATCH_SCHEDULE_ITEM);

		/* library uris */
		MATCHER.addURI(LIBRARY_AUTHORITY, LIBRARY_BASE_PATH, MATCH_LIBRARY);
		MATCHER.addURI(LIBRARY_AUTHORITY, LIBRARY_BASE_PATH + "/#",
				MATCH_LIBRARY_ITEM);

		MATCHER.addURI(LIBRARY_AUTHORITY, ITEM_ATTRIBUTES_BASE_PATH,
				MATCH_ITEM_ATTRIBS);
		MATCHER.addURI(LIBRARY_AUTHORITY, ITEM_CONDITIONS_BASE_PATH,
				MATCH_ITEM_CONDITIONS);

		/* library root path uri */
		MATCHER.addURI(LIBRARY_AUTHORITY, LIBRARY_BASE_PATH + "/#/path/",
				MATCH_LIBRARY_PATH);

		/* library/attrib/conditions subpath uris */
		StringBuilder subpath = new StringBuilder(MAX_SUBPATHS * 2);
		for (int subpaths = 0; subpaths < MAX_SUBPATHS; ++subpaths) {
			subpath.append("/*");

			MATCHER.addURI(LIBRARY_AUTHORITY, LIBRARY_BASE_PATH + "/#/path"
					+ subpath, MATCH_LIBRARY_PATH);
			MATCHER.addURI(LIBRARY_AUTHORITY, ITEM_ATTRIBUTES_BASE_PATH + "/#"
					+ subpath, MATCH_ITEM_ATTRIBS_PATH);
			MATCHER.addURI(LIBRARY_AUTHORITY, ITEM_CONDITIONS_BASE_PATH + "/#"
					+ subpath, MATCH_ITEM_CONDITIONS_PATH);
		}
	}

	public static class LibraryPathSegments {
		public String base = LIBRARY_BASE_PATH;
		public int id = -1;
		public String path = "/";

		public static LibraryPathSegments parse(Uri uri) {

			List<String> segments = uri.getPathSegments();
			LibraryPathSegments segment = new LibraryPathSegments();

			if (segments != null && segments.size() >= 2) {
				segment.base = segments.get(0);
				segment.id = Integer.valueOf(segments.get(1));

				if (segment.base.equals(LIBRARY_BASE_PATH)) {
					segment.path = uri.getPath()
							.replaceFirst(
									"/" + segment.base + "/" + segment.id
											+ "/path", "");
				} else {
					segment.path = uri.getPath().replaceFirst(
							"/" + segment.base + "/" + segment.id, "");
				}
			}

			return segment;
		}
	}

}
