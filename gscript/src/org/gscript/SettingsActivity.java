package org.gscript;

import java.util.ArrayList;

import org.gscript.settings.ShellProfile;
import org.gscript.terminal.ColorScheme;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsActivity extends PreferenceActivity {

	PreferenceCategory profilesCategory;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		addDynamicPreferences();
	}

	@SuppressWarnings("deprecation")
	private void addDynamicPreferences() {

		PreferenceScreen screen = getPreferenceScreen();

		profilesCategory = new PreferenceCategory(this);
		profilesCategory.setTitle(R.string.settings_shell_profiles);
		profilesCategory.setSummary(R.string.settings_shell_profiles_summary);

		screen.addPreference(profilesCategory);

		reloadProfilesCategory();
		
		screen.addPreference(createAliasScreen());
	}

	private void reloadProfilesCategory() {

		profilesCategory.removeAll();

		ArrayList<String> profileKeys = ShellProfile.getKeys(this);
		for (String profileKey : profileKeys) {
			profilesCategory.addPreference(createProfileScreen(profileKey));
		}

		Preference createProfilePref = new Preference(this);
		createProfilePref.setTitle(R.string.settings_shell_profiles_create);
		createProfilePref.setOrder(1000);
		createProfilePref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {

						createProfileScreen(ShellProfile
								.getNewKey(SettingsActivity.this));

						reloadProfilesCategory();
						return true;
					}
				});

		profilesCategory.addPreference(createProfilePref);

		CharSequence[] profileValues = new CharSequence[profileKeys.size()];
		CharSequence[] profileNames = new CharSequence[profileKeys.size()];

		profileKeys.toArray(profileValues);
		for (int i = 0; i < profileValues.length; ++i)
			profileNames[i] = ShellProfile.getName(this,
					profileValues[i].toString());

		ListPreference defaultProfilePref = new ListPreference(this);
		defaultProfilePref.setTitle(R.string.settings_shell_profiles_default);
		defaultProfilePref
				.setSummary(R.string.settings_shell_profiles_default_summary);
		defaultProfilePref
				.setDialogTitle(R.string.settings_shell_profiles_default);
		defaultProfilePref.setOrder(2000);
		defaultProfilePref.setKey(ShellProfile.PREF_DEFAULT_PROFILE);
		defaultProfilePref.setEntries(profileNames);
		defaultProfilePref.setEntryValues(profileValues);

		if (profileValues.length > 0)
			defaultProfilePref.setDefaultValue(profileValues[0]);

		profilesCategory.addPreference(defaultProfilePref);

	}

	@SuppressWarnings("deprecation")
	private PreferenceScreen createProfileScreen(String key) {

		PreferenceManager manager = getPreferenceManager();

		String name = ShellProfile.getName(this, key);

		final PreferenceScreen profileScreen = manager
				.createPreferenceScreen(this);
		
		profileScreen.setTitle(name);
		
		/* name */

		EditTextPreference namePref = new EditTextPreference(this);
		namePref.setTitle(R.string.settings_shell_profile_name);
		namePref.setSummary(R.string.settings_shell_profile_name_summary);
		namePref.setDialogTitle(R.string.settings_shell_profile_name);
		namePref.setKey(key + ShellProfile.PREF_NAME);
		namePref.setDefaultValue("Unnamed profile");
		namePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {

				profileScreen.setTitle(newValue.toString());
				return true;
			}
		});

		profileScreen.addPreference(namePref);

		/* execute command */
		
		EditTextPreference commandExecPref = new EditTextPreference(this);
		commandExecPref.setTitle(R.string.settings_shell_profile_cmd_exec);
		commandExecPref.setSummary(R.string.settings_shell_profile_cmd_exec_summary);
		commandExecPref.setDialogTitle(R.string.settings_shell_profile_cmd_exec);
		commandExecPref.setKey(key + ShellProfile.PREF_COMMAND_EXEC);
		commandExecPref.setDefaultValue(ShellProfile.DEFAULT_COMMAND_EXEC);
		commandExecPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				
				return true;
			}
		});

		profileScreen.addPreference(commandExecPref);

		/* start command */
		
		EditTextPreference commandStartPref = new EditTextPreference(this);
		commandStartPref.setTitle(R.string.settings_shell_profile_cmd_start);
		commandStartPref.setSummary(R.string.settings_shell_profile_cmd_start_summary);
		commandStartPref.setDialogTitle(R.string.settings_shell_profile_cmd_start);
		commandStartPref.setKey(key + ShellProfile.PREF_COMMAND_START);
		commandStartPref.setDefaultValue(ShellProfile.DEFAULT_COMMAND_START);
		commandStartPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				
				return true;
			}
		});

		profileScreen.addPreference(commandStartPref);
		
		
		/* background color */

		ListPreference backgroundPref = new ListPreference(this);
		backgroundPref.setTitle(R.string.settings_shell_profile_background);
		backgroundPref
				.setSummary(R.string.settings_shell_profile_background_summary);
		backgroundPref
				.setDialogTitle(R.string.settings_shell_profile_background);
		backgroundPref.setKey(key + ShellProfile.PREF_BACKCOLOR);
		backgroundPref.setEntries(ColorScheme.SEQUENCE_COLOR_NAMES);
		backgroundPref.setEntryValues(ColorScheme.SEQUENCE_COLOR_INDICES);
		backgroundPref.setDefaultValue(ColorScheme.SEQUENCE_COLOR_INDICES[ColorScheme.INDEX_BLACK]);

		profileScreen.addPreference(backgroundPref);

		/* text color */

		ListPreference textcolorPref = new ListPreference(this);
		textcolorPref.setTitle(R.string.settings_shell_profile_textcolor);
		textcolorPref
				.setSummary(R.string.settings_shell_profile_textcolor_summary);
		textcolorPref.setDialogTitle(R.string.settings_shell_profile_textcolor);
		textcolorPref.setKey(key + ShellProfile.PREF_TEXTCOLOR);
		textcolorPref.setEntries(ColorScheme.SEQUENCE_COLOR_NAMES);
		textcolorPref.setEntryValues(ColorScheme.SEQUENCE_COLOR_INDICES);
		textcolorPref.setDefaultValue(ColorScheme.SEQUENCE_COLOR_INDICES[ColorScheme.INDEX_WHITE]);

		profileScreen.addPreference(textcolorPref);

		/* font size */

		ListPreference fontsizePref = new ListPreference(this);
		fontsizePref.setTitle(R.string.settings_shell_profile_fontsize);
		fontsizePref
				.setSummary(R.string.settings_shell_profile_fontsize_summary);
		fontsizePref.setDialogTitle(R.string.settings_shell_profile_fontsize);
		fontsizePref.setKey(key + ShellProfile.PREF_FONTSIZE);
		fontsizePref.setEntries(ShellProfile.FONTSIZE_NAMES);
		fontsizePref.setEntryValues(ShellProfile.FONTSIZE_VALUES);
		fontsizePref.setDefaultValue(ShellProfile.FONTSIZE_VALUES[0]);

		profileScreen.addPreference(fontsizePref);

		/* append path */

		EditTextPreference appendPathPref = new EditTextPreference(this);
		appendPathPref.setTitle(R.string.settings_shell_profile_append_path);
		appendPathPref
				.setSummary(R.string.settings_shell_profile_append_path_summary);
		appendPathPref
				.setDialogTitle(R.string.settings_shell_profile_append_path);
		appendPathPref.setKey(key + ShellProfile.PREF_APPEND_PATH);
		appendPathPref.setDefaultValue("");

		profileScreen.addPreference(appendPathPref);

		/* append exit */

		CheckBoxPreference appendExitPref = new CheckBoxPreference(this);
		appendExitPref.setTitle(R.string.settings_shell_profile_append_exit);
		appendExitPref
				.setSummary(R.string.settings_shell_profile_append_exit_summary);
		appendExitPref.setKey(key + ShellProfile.PREF_APPEND_EXIT);
		appendExitPref.setDefaultValue(false);

		profileScreen.addPreference(appendExitPref);

		/* delete button */

		Preference deleteProfilePref = new Preference(this);
		deleteProfilePref.setTitle(R.string.settings_shell_profile_delete);
		deleteProfilePref.setKey(key);
		deleteProfilePref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {

						ShellProfile.removeProfile(SettingsActivity.this,
								preference.getKey());

						Intent intent = new Intent(SettingsActivity.this,
								SettingsActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						SettingsActivity.this.startActivity(intent);
						SettingsActivity.this.finish();

						return true;
					}
				});

		profileScreen.addPreference(deleteProfilePref);

		manager.getSharedPreferences().edit()
				.putInt(key, ShellProfile.PROFILE_VERSION).commit();

		return profileScreen;
	}

	@SuppressWarnings("deprecation")
	private PreferenceScreen createAliasScreen() {
		
		PreferenceManager manager = getPreferenceManager();
		
		ArrayList<String> profileKeys = ShellProfile.getKeys(this);
		
		CharSequence[] profileValues = new CharSequence[profileKeys.size()];
		CharSequence[] profileNames = new CharSequence[profileKeys.size()];

		profileKeys.toArray(profileValues);
		for (int i = 0; i < profileValues.length; ++i)
			profileNames[i] = ShellProfile.getName(this,
					profileValues[i].toString());
		
		
		final PreferenceScreen aliasScreen = manager
				.createPreferenceScreen(this);
		
		aliasScreen.setTitle(R.string.settings_aliases);
		aliasScreen.setSummary(R.string.settings_aliases_summary);

		/* aliases */
		
		String[] aliasTitles = getResources().getStringArray(R.array.alias_titles);
		
		for(int i=0; i < aliasTitles.length; ++i) {
			final int idx = i + 1;
			
			CheckBoxPreference aliasEnabledPref = new CheckBoxPreference(this);
			aliasEnabledPref.setTitle(String.format("Alias %d (%s) enabled", idx, aliasTitles[i]));
			aliasEnabledPref.setKey(String.format(ShellProfile.PREF_ALIASX_ENABLED, idx));
			aliasEnabledPref.setDefaultValue(false);
			aliasEnabledPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					
					Boolean boolVal = (Boolean) newValue;
					getPackageManager().setComponentEnabledSetting(
							new ComponentName("org.gscript", String.format("org.gscript.ProfileAlias%d", idx)), 
							boolVal.booleanValue() ?
							PackageManager.COMPONENT_ENABLED_STATE_ENABLED : 
								PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
								PackageManager.DONT_KILL_APP);
					
					return true;
				}
			});
			
			aliasScreen.addPreference(aliasEnabledPref);
		
			ListPreference aliasProfilePref = new ListPreference(this);
			aliasProfilePref.setTitle(String.format("Alias %d profile", idx));
			aliasProfilePref.setDialogTitle(String.format("Alias %d profile", idx));
			aliasProfilePref.setKey(String.format(ShellProfile.PREF_ALIASX_PROFILE, idx));
			aliasProfilePref.setEntries(profileNames);
			aliasProfilePref.setEntryValues(profileValues);

			if (profileValues.length > 0)
				aliasProfilePref.setDefaultValue(profileValues[0]);

			aliasScreen.addPreference(aliasProfilePref);			
			
		}
		
		return aliasScreen;
	}
}
