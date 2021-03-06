package com.minder.app.tf2backpack.frontend;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;

public class SelectBackpack extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(R.string.select_backpack);      
        setContentView(R.layout.select_backpack);

        Typeface tf2TypeFace = Typeface.createFromAsset(getAssets(), "fonts/TF2Build.ttf");
        
        Button myBackpackButton = (Button)findViewById(R.id.ButtonMyBackpack);
        myBackpackButton.setTypeface(tf2TypeFace);
        myBackpackButton.setTextColor(0xFF2A2725);
        myBackpackButton.setOnClickListener(onSelectBackpackClick);
        
        Button buttonUserId = (Button)findViewById(R.id.ButtonUserId);
        buttonUserId.setTypeface(tf2TypeFace);
        buttonUserId.setTextColor(0xFF2A2725);
        buttonUserId.setOnClickListener(onSelectBackpackClick);
        
        Button buttonCancel = (Button)findViewById(R.id.ButtonCancel);
        buttonCancel.setTypeface(tf2TypeFace);
        buttonCancel.setTextColor(0xFF2A2725);
        buttonCancel.setOnClickListener(onSelectBackpackClick);
    }
    
    OnClickListener onSelectBackpackClick = new OnClickListener() {		
		public void onClick(View v) {
			switch (v.getId()){
				case R.id.ButtonMyBackpack:
			        SharedPreferences playerPrefs = SelectBackpack.this.getSharedPreferences("player", MODE_PRIVATE);
			        String playerId = playerPrefs.getString("id", null);
			        SteamUser user = new SteamUser();
			        user.steamdId64 = Long.parseLong(playerId);
			        startActivity(new Intent(SelectBackpack.this, BackpackActivity.class)
			        	.putExtra("com.minder.app.tf2backpack.SteamUser", user));
			        finish();
					break;
				case R.id.ButtonUserId:
					startActivityForResult(new Intent(SelectBackpack.this, SelectPlayerActivity.class).putExtra("return", true), 0);
					break;
				case R.id.ButtonCancel:
					finish();
					break;
			}
			
		}
	};
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (resultCode == RESULT_OK) {
	        final SteamUser user = data.getExtras().getParcelable("user");
	        if (user != null) {
	    		startActivity(new Intent(this, BackpackActivity.class)
	    			.putExtra("com.minder.app.tf2backpack.SteamUser", user));
	        }
    	}
		finish();
    }
}
