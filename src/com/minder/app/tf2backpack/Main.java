package com.minder.app.tf2backpack;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class Main extends Activity {
	private SharedPreferences playerPrefs;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Start the dummy admob activity.  Don't try to start it twice or an exception will be thrown
        if (AdMobActivity.AdMobMemoryLeakWorkAroundActivity == null) {
            Log.i("TF2Backpack", "starting the AdMobActivity");
            AdMobActivity.startAdMobActivity(this);
        }

        
        Util.dbHandler = new DatabaseHandler(this.getApplicationContext());
        
        playerPrefs = this.getSharedPreferences("player", MODE_PRIVATE);
        
        String playerId = playerPrefs.getString("id", null);
        if (playerId != null){
        	startActivity(new Intent(this, DashBoard.class));
            finish();
        }
        else {
        	startActivityForResult(new Intent(this, GetPlayer.class)
        		.putExtra("title", "Enter your user name"), 0);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (resultCode == RESULT_OK) {
    		SharedPreferences.Editor editor = playerPrefs.edit();
    		Bundle bundle = data.getExtras();
    		editor.putString("name", bundle.getString("name"));
    		editor.putString("id", bundle.getString("id"));
    		editor.commit();
    		startActivity(new Intent(this, DashBoard.class));
    	}
    	finish();
    }

}