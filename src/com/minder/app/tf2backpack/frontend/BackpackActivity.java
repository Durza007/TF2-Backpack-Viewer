package com.minder.app.tf2backpack.frontend;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;

public class BackpackActivity extends FragmentActivity {
	private BackpackFragment backpackFragment;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
        /*requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }*/
    	
    	setContentView(R.layout.activity_generic);
    	
    	SteamUser user = getIntent().getParcelableExtra("com.minder.app.tf2backpack.SteamUser");
    	
        final FragmentManager fragmentManager = this.getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        backpackFragment = new BackpackFragment(user);
        fragmentTransaction.add(R.id.frameLayoutFragment, backpackFragment);
        fragmentTransaction.commit();
    }
}
