package org.gscript.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Set;

import org.gscript.terminal.ColorScheme;

@SuppressLint("DefaultLocale")
public class ShellProfile {

	public static final int PROFILE_VERSION = 1;

	static final String PROFILE_PREFIX = "shell-profile-";
	static final String PROFILE_NAME = "name";
	static final String PREFERENCES_NAME = "org.gscript_preferences";

	public static final String DEFAULT_COMMAND_EXEC = "/system/bin/sh -c %path%";
	public static final String DEFAULT_COMMAND_START = "/system/bin/sh";
	public static final String DEFAULT_PROFILE_NAME = "Unnamed profile";

	public static final String PREF_NAME = "_name";
	public static final String PREF_COMMAND_EXEC = "_command_exec";
	public static final String PREF_COMMAND_START = "_command_start";
	public static final String PREF_LEGACY_COMMAND = "_command";
	public static final String PREF_BACKCOLOR = "_backcolor";
	public static final String PREF_TEXTCOLOR = "_textcolor";
	public static final String PREF_FONTSIZE = "_fontsize";
	public static final String PREF_APPEND_PATH = "_path";
	public static final String PREF_APPEND_EXIT = "_exit";

	public static final String PREF_DEFAULT_PROFILE = "default_profile";
	public static final String PREF_ALIASX_ENABLED = "alias%d_enabled";
	public static final String PREF_ALIASX_PROFILE = "alias%d_profile";
	
	public static final CharSequence[] FONTSIZE_NAMES = 
		{ "10sp", "11sp", "12sp",	"13sp", "14sp", "15sp", "16sp", "17sp", "18sp" };
	public static final CharSequence[] FONTSIZE_VALUES = 
		{ "10", "11", "12", "13", "14", "15", "16", "17", "18" };

	public static ArrayList<String> getKeys(Context context) {

		ArrayList<String> names = new ArrayList<String>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Set<String> prefKeys = prefs.getAll().keySet();

		for (String prefKey : prefKeys) {
			if (prefKey.startsWith(PROFILE_PREFIX)) {
				if (!prefKey.replace(PROFILE_PREFIX, "").contains("_")) {
					/* assume this is */
					names.add(prefKey);
				}
			}
		}

		return names;
	}

	public static String getNewKey(Context context) {

		int last = 0;

		ArrayList<String> keys = getKeys(context);
		for (String key : keys) {
			String keyNr = key.replace(PROFILE_PREFIX, "");
			int nr = Integer.valueOf(keyNr);
			if (nr > last)
				last = nr;
		}

		return String.format("%s%d", PROFILE_PREFIX, (last + 1));
	}

	public static String getName(Context context, String key) {
		SharedPreferences profile = PreferenceManager.getDefaultSharedPreferences(context);

		return profile.getString(key + "_name", DEFAULT_PROFILE_NAME);
	}
	
	public static String getAliasKey(Context context, int aliasId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(String.format(PREF_ALIASX_PROFILE, aliasId), getDefaultOrFirstKey(context));
	}

	public static String getDefaultKey(Context context) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		return prefs.getString(PREF_DEFAULT_PROFILE, null);
	}

	public static String getDefaultOrFirstKey(Context context) {

		String defaultKey = getDefaultKey(context);

		if (isValidProfile(context, defaultKey))
			return defaultKey;

		ArrayList<String> keys = getKeys(context);
		if (keys.size() > 0)
			return keys.get(0);

		return null;
	}

	public static void removeProfile(Context context, String key) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Set<String> prefKeys = prefs.getAll().keySet();

		Editor editor = prefs.edit();

		for (String prefKey : prefKeys) {
			if (prefKey.startsWith(key))
				editor.remove(prefKey);
		}

		editor.commit();
	}

	public static void addDefaultProfiles(Context context) {

		/* default sh-profile */
		String shKey = addProfile(
				context, 
				"sh", 
				"/system/bin/sh -c %path%", 
				"/system/bin/sh",
				ColorScheme.INDEX_BLACK, 
				ColorScheme.INDEX_WHITE,
				FONTSIZE_VALUES[0].toString(), 
				"", 
				true);

		/* default su-profile */
		addProfile(
				context, 
				"su", 
				"/system/xbin/su -c %path%", 
				"/system/xbin/su",
				ColorScheme.INDEX_BLACK, 
				ColorScheme.INDEX_WHITE,
				FONTSIZE_VALUES[0].toString(), 
				"", 
				true);

		setAsDefault(context, shKey);
	}

	public static boolean isValidProfile(Context context, String key) {
		if (key == null)
			return false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		return (prefs.getInt(key, -1) != -1);
	}

	public static String addProfile(Context context, String name,
			String commandExec, String commandStart, int backColor, int textColor, String fontSize,
			String appendPath, boolean appendExit) {

		String newKey = getNewKey(context);

		SharedPreferences prefs = context.getSharedPreferences(
				PREFERENCES_NAME, Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(newKey, PROFILE_VERSION);
		editor.putString(newKey + PREF_NAME, name);
		editor.putString(newKey + PREF_COMMAND_EXEC, commandExec);
		editor.putString(newKey + PREF_COMMAND_START, commandStart);
		editor.putString(newKey + PREF_BACKCOLOR, String.valueOf(backColor));
		editor.putString(newKey + PREF_TEXTCOLOR, String.valueOf(textColor));
		editor.putString(newKey + PREF_FONTSIZE, fontSize);
		editor.putString(newKey + PREF_APPEND_PATH, appendPath);
		editor.putBoolean(newKey + PREF_APPEND_EXIT, appendExit);

		editor.commit();

		return newKey;
	}

	public static void setAsDefault(Context context, String key) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(PREF_DEFAULT_PROFILE, key).commit();

	}

	public String name;
	public String cmdExec;
	public String cmdStart;
	public int backcolor;
	public int textcolor;
	public String fontsize;
	public String appendPath;
	public boolean appendExit;
	
	public static ShellProfile forKey(Context context, String key) {

		if (key == null || !isValidProfile(context, key)) {
			key = getDefaultOrFirstKey(context);
		}

		ShellProfile profile = new ShellProfile();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		
		/* upgrade profiles with legacy _command preference to newer _exec_command */
		if(prefs.contains(key + PREF_LEGACY_COMMAND) && !prefs.contains(key + PREF_COMMAND_EXEC))
			prefs.edit().putString(key + PREF_COMMAND_EXEC, prefs.getString(key + PREF_LEGACY_COMMAND, DEFAULT_COMMAND_EXEC)).commit();
		
		
		profile.name = prefs.getString(key + PREF_NAME, DEFAULT_PROFILE_NAME);
		profile.cmdExec = prefs.getString(key + PREF_COMMAND_EXEC, DEFAULT_COMMAND_EXEC);
		profile.cmdStart = prefs.getString(key + PREF_COMMAND_START, DEFAULT_COMMAND_START);
		profile.backcolor = Integer.valueOf(prefs.getString(key + PREF_BACKCOLOR, String.valueOf(ColorScheme.INDEX_BLACK)));
		profile.textcolor = Integer.valueOf(prefs.getString(key + PREF_TEXTCOLOR, String.valueOf(ColorScheme.INDEX_WHITE)));
		profile.fontsize = prefs.getString(key + PREF_FONTSIZE,	FONTSIZE_VALUES[0].toString());
		profile.appendPath = prefs.getString(key + PREF_APPEND_PATH, "");
		profile.appendExit = prefs
				.getBoolean(key + PREF_APPEND_EXIT, true);

		return profile;
	}

	@Override
	public String toString() {
		return String
				.format("ProfilePrefences [name=%s, exec=%s, start=%s bgcolor=%d, textcolor=%d, fontsize=%s, appendPath=%s, appendExit=%b]",
						name, cmdExec, cmdStart, backcolor, textcolor, fontsize,
						appendPath, appendExit);
	}	
}