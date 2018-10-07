package com.minder.app.tf2backpack.frontend;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;

public class BackpackActivity extends FragmentActivity {
	private BackpackFragment backpackFragment;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
        	requestWindowFeature(Window.FEATURE_NO_TITLE);
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 && 
    			Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        	// fix for bug in android 3.1 to 4.1
        	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	}
    	
    	setContentView(R.layout.activity_generic);
    	
    	//boolean scaleByWidth = getResources().getBoolean(R.bool.backpackScaleByWidth);
    	boolean scaleByWidth = false;
    	
    	SteamUser user = getIntent().getParcelableExtra("com.minder.app.tf2backpack.SteamUser");
    	if (user.steamName != null) {
    		setTitle(user.steamName);
		}
    	
        final FragmentManager fragmentManager = this.getSupportFragmentManager();     
        if (savedInstanceState != null) {
        	backpackFragment = (BackpackFragment) fragmentManager.findFragmentByTag("backpackFragment");
        } else {
        	if (scaleByWidth) {
        		backpackFragment = BackpackFragment.newInstance(user, getResources().getDisplayMetrics().widthPixels, true);
        	} else {
        		backpackFragment = BackpackFragment.newInstance(user);
        	}
        	fragmentManager
            	.beginTransaction()
            		.add(R.id.frameLayoutFragment, backpackFragment, "backpackFragment")
            		.commit();
        }
    }
}
