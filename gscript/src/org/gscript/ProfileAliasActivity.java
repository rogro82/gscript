package org.gscript;

import org.gscript.process.ProcessService;
import org.gscript.settings.ShellProfile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ProfileAliasActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent processIntent = new Intent(this, ProcessService.class);
		processIntent.setAction(ProcessService.ACTION_START);
		
		String profileKey = "";
		String title = this.getTitle().toString();
		String[] aliasTitles = getResources().getStringArray(R.array.alias_titles);
		
		for(int i=0; i < aliasTitles.length; ++i) {
			if(title.equals(aliasTitles[i])) {
				profileKey = ShellProfile.getAliasKey(this, (i+1));
				break;
			}
		}
		
		processIntent.putExtra(ProcessService.EXTRA_PROFILE, profileKey);
		
		this.startService(processIntent);
		this.finish();
	}
}
