package ru.dikpost.acs35test.activity;


import android.os.Bundle;
import android.preference.PreferenceActivity;

import ru.dikpost.acs35test.R;

public class SettingsActivity extends PreferenceActivity {
	
	   @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        // setContentView(R.layout.activity_main);
	        addPreferencesFromResource(R.xml.prefs);
	   }

}
