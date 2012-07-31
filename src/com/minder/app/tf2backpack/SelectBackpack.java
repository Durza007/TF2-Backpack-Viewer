package com.minder.app.tf2backpack;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.R.id;
import com.minder.app.tf2backpack.R.layout;
import com.minder.app.tf2backpack.R.string;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SelectBackpack extends Activity {
	private Typeface tf2TypeFace;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(R.string.select_backpack);      
        setContentView(R.layout.select_backpack);
        
        tf2TypeFace = Typeface.createFromAsset(getAssets(), "fonts/TF2Build.ttf");
        
        Button myBackpackButton = (Button)findViewById(R.id.ButtonMyBackpack);
        myBackpackButton.setTypeface(tf2TypeFace);
        myBackpackButton.setTextColor(0xFF2A2725);
        myBackpackButton.setOnClickListener(onSelectBackpackClick);
        
        Button buttonUserId = (Button)findViewById(R.id.ButtonUserId);
        buttonUserId.setTypeface(tf2TypeFace);
        buttonUserId.setTextColor(0xFF2A2725);
        buttonUserId.setOnClickListener(onSelectBackpackClick);
        
        Button buttonFriends = (Button)findViewById(R.id.ButtonFriends);
        buttonFriends.setTypeface(tf2TypeFace);
        buttonFriends.setTextColor(0xFF2A2725);
        buttonFriends.setOnClickListener(onSelectBackpackClick);
        
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
			        startActivity(new Intent(SelectBackpack.this, Backpack.class).putExtra("id", playerId));
			        finish();
					break;
				case R.id.ButtonUserId:
					startActivityForResult(new Intent(SelectBackpack.this, GetPlayer.class).putExtra("return", true), 0);
					break;
				case R.id.ButtonFriends:
					startActivity(new Intent(SelectBackpack.this, PlayerList.class).setAction("com.minder.app.tf2backpack.VIEW_FRIENDS"));
					finish();
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
    		Bundle bundle = data.getExtras();
    		startActivity(new Intent(this, Backpack.class).putExtra("id", bundle.getString("id")));
    	}
		finish();
    }
}
