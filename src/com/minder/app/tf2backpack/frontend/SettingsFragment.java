package com.minder.app.tf2backpack.frontend;

import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.R;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;

@TargetApi(11)
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        this.addPreferencesFromResource(R.xml.settings);
        
        Preference refresh = (Preference)findPreference("refreshfiles");
        refresh.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference preference) {
		    	Intent intent = new Intent(getActivity(), GameSchemeDownloaderService.class);
		    	intent.putExtra("refreshImages", true);
		    	getActivity().startService(intent);
				return true;
			}
		});
    }
}
