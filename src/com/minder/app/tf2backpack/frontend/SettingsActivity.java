package com.minder.app.tf2backpack.frontend;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.ImageLoader;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;

public class SettingsActivity extends PreferenceActivity {
	private final static int COMMUNITY_ID_TUTORIAL = 1;
	private final static int CONFIRMATION_DIALOG_CACHE = 2;
	private final static int CONFIRMATION_DIALOG_HISTORY = 3;
	
	private AdView adView;
	
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.list_content);     
        adView = (AdView)findViewById(R.id.ad);
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
        	preHoneycombOnCreate();
        } else {
            getFragmentManager().beginTransaction()
            	.replace(R.id.listContainer, new SettingsFragment())
            	.commit();
        }
    }
    
    @SuppressWarnings("deprecation")
	private void preHoneycombOnCreate() {
    	Log.d("SettingsActivity", "Loading pre HC settings...");
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);
        
        Preference community = (Preference)findPreference("communityId");
        community.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	showDialog(COMMUNITY_ID_TUTORIAL);
            	return true;
            }
        });

        Preference refresh = (Preference)findPreference("refreshfiles");
        refresh.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(SettingsActivity.this, DashboardActivity.class);
				intent.setAction("download_gamescheme");
				startActivity(intent);
				return true;
			}
		});
        
        Preference clearCache = (Preference)findPreference("clearcache");
        clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference preference) {
				showDialog(CONFIRMATION_DIALOG_CACHE);
				return true;
			}
		});
        
        Preference clearHistory = (Preference)findPreference("clearhistory");
        clearHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
				showDialog(CONFIRMATION_DIALOG_HISTORY);
            	return true;
            }
        });
        
        Preference aboutCreator = (Preference)findPreference("aboutcreator");
        aboutCreator.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	SteamUser user = new SteamUser();
            	user.steamdId64 = Long.parseLong("76561197992965248");
            	startActivity(new Intent(SettingsActivity.this, BackpackActivity.class)
            		.putExtra("com.minder.app.tf2backpack.SteamUser", user));
            	return true;
            }
        });
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	
    	if (adView != null) {
    		adView.destroy();
    	}
    }

    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case COMMUNITY_ID_TUTORIAL:
            return new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.community_id)
                .setMessage(R.string.tutorial_how_to_set_community_id)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
        case CONFIRMATION_DIALOG_CACHE:
        	return new AlertDialog.Builder(this)
    			.setIcon(android.R.drawable.ic_dialog_alert)
    			.setTitle(R.string.alert_dialog_clear_cache)
    			.setMessage(R.string.alert_dialog_are_you_sure)
    			.setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int whichButton) {
    					ImageLoader il = new ImageLoader(SettingsActivity.this.getApplicationContext(), 128);
    					il.clearCache();
    					
    					Toast.makeText(SettingsActivity.this, "Cache cleared", Toast.LENGTH_SHORT).show();
    				}
    			}).setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int whichButton) {
    				
    				}
    		}).create();
        case CONFIRMATION_DIALOG_HISTORY:
        	return new AlertDialog.Builder(this)
        		.setIcon(android.R.drawable.ic_dialog_alert)
        		.setTitle(R.string.alert_dialog_clear_history)
        		.setMessage(R.string.alert_dialog_are_you_sure)
        		.setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
                		App.getDataManager().getDatabaseHandler().execSql("DELETE FROM name_history");
                		
                		Toast.makeText(SettingsActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
        			}
        		}).setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				
        			}
        	}).create();
        }
        return null;
    }
}