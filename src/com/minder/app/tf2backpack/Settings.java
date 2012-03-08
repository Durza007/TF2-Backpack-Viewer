package com.minder.app.tf2backpack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.minder.app.tf2backpack.downloadmanager.CacheManager;

public class Settings extends PreferenceActivity {
	private final static int COMMUNITY_ID_TUTORIAL = 1;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.list_layout);
        
        // Look up the AdView as a resource and load a request.
        AdView adView = (AdView)this.findViewById(R.id.ad);
        if (adView != null) {
        	adView.loadAd(new AdRequest());
        }
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);
        
        Preference myPref = (Preference)findPreference("communityId");
        myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	showDialog(COMMUNITY_ID_TUTORIAL);
            	return true;
            }
        });

        Preference refresh = (Preference)findPreference("refreshfiles");
        refresh.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(Settings.this, DashBoard.class);
				intent.setAction("com.minder.app.tf2backpack.DOWNLOAD_GAMEFILES");
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			}
		});
        
        Preference clearCache = (Preference)findPreference("clearcache");
        clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference preference) {
				ImageLoader il = new ImageLoader(Settings.this.getApplicationContext(), 128);
				il.clearCache();
				
				Toast.makeText(Settings.this, "Cache cleared", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
        
        Preference clearHistory = (Preference)findPreference("clearhistory");
        clearHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	/*DataBaseHelper db = new DataBaseHelper(Settings.this.getApplicationContext());
        		SQLiteDatabase sqlDb =  db.getReadableDatabase();
        		
        		sqlDb.execSQL("DELETE FROM name_history");
        		
        		sqlDb.close();
        		db.close();*/
        		
        		Util.dbHandler.ExecSql("DELETE FROM name_history");
        		
        		Toast.makeText(Settings.this, "History cleared", Toast.LENGTH_SHORT).show();
            	return true;
            }
        });
        
        /*Preference clearCache = (Preference)findPreference("clearcache");
        clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				CacheManager.getInstance().clear();
				Toast.makeText(Settings.this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
				return true;
			}     	
        });*/
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case COMMUNITY_ID_TUTORIAL:
            return new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.community_id)
                .setMessage(R.string.tutorial_how_to_set_community_id)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
        }
        return null;
        }
}