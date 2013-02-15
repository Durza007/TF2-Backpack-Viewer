package com.minder.app.tf2backpack.frontend;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;

@TargetApi(11)
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setRetainInstance(true);
        
        // Load the preferences from an XML resource
        this.addPreferencesFromResource(R.xml.settings);
        
        Preference community = (Preference)findPreference("communityId");
        community.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	//showDialog(COMMUNITY_ID_TUTORIAL);
            	return true;
            }
        });
        
        Preference refresh = (Preference)findPreference("refreshfiles");
        refresh.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(getActivity(), DashboardActivity.class);
				intent.setAction("download_gamescheme");
				startActivity(intent);
				return true;
			}
		});
        
        Preference clearCache = (Preference)findPreference("clearcache");
        clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference preference) {
				//showDialog(CONFIRMATION_DIALOG_CACHE);
				return true;
			}
		});
        
        Preference clearHistory = (Preference)findPreference("clearhistory");
        clearHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
				//showDialog(CONFIRMATION_DIALOG_HISTORY);
            	return true;
            }
        });
        
        Preference aboutCreator = (Preference)findPreference("aboutcreator");
        aboutCreator.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	SteamUser user = new SteamUser();
            	user.steamdId64 = Long.parseLong("76561197992965248");
            	startActivity(new Intent(getActivity(), BackpackActivity.class)
            		.putExtra("com.minder.app.tf2backpack.SteamUser", user));
            	return true;
            }
        });
    }
}
