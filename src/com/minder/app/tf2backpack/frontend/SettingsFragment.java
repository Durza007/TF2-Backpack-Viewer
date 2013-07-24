package com.minder.app.tf2backpack.frontend;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.ImageLoader;
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
            	GenericDialogHC dialog = GenericDialogHC.newInstance(R.string.community_id, R.string.tutorial_how_to_set_community_id);
            	dialog.setNeutralButtonText(android.R.string.ok);
            	dialog.setIcon(android.R.drawable.ic_dialog_info);
            	dialog.setClickListener(new DialogInterface.OnClickListener() {				
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
            	
            	dialog.show(getFragmentManager(), "communityDialog");
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
            	GenericDialogHC dialog = GenericDialogHC.newInstance(R.string.alert_dialog_clear_cache, R.string.alert_dialog_are_you_sure);
            	dialog.setPositiveButtonText(android.R.string.yes);
            	dialog.setNegativeButtonText(android.R.string.no);
            	dialog.setIcon(android.R.drawable.ic_dialog_alert);
            	dialog.setClickListener(new DialogInterface.OnClickListener() {				
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
	    					ImageLoader il = new ImageLoader(App.getAppContext(), 128);
	    					il.clearCache();
	    					il.stopThread();
	    					
	    					Toast.makeText(getActivity(), "Cache cleared", Toast.LENGTH_SHORT).show();
						}

						dialog.dismiss();
					}
				});
            	
            	dialog.show(getFragmentManager(), "cacheDialog");
				return true;
			}
		});
        
        Preference clearHistory = (Preference)findPreference("clearhistory");
        clearHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	GenericDialogHC dialog = GenericDialogHC.newInstance(R.string.alert_dialog_clear_history, R.string.alert_dialog_are_you_sure);
            	dialog.setPositiveButtonText(android.R.string.yes);
            	dialog.setNegativeButtonText(android.R.string.no);
            	dialog.setIcon(android.R.drawable.ic_dialog_alert);
            	dialog.setClickListener(new DialogInterface.OnClickListener() {				
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
	                		App.getDataManager().getDatabaseHandler().execSql("DELETE FROM name_history");
	                		
	                		Toast.makeText(getActivity(), "History cleared", Toast.LENGTH_SHORT).show();
						}

						dialog.dismiss();
					}
				});
            	
            	dialog.show(getFragmentManager(), "historyDialog");
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
