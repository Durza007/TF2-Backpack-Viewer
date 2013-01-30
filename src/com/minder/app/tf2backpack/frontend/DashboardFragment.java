package com.minder.app.tf2backpack.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.minder.app.tf2backpack.R;

public class DashboardFragment extends Fragment {	
	private ImageButton backpackButton;
	private ImageButton settingsButton;
	private ImageButton friendsButton;
	private ImageButton wrenchButton;
	private Intent backpackIntent;
	private Intent settingsIntent;
	private String playerId;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_menu, container, false);
        
        //AdManager.setTestDevices( new String[] { "B135E5E10665286A9FA99BA95CE926D4" } );
        
        backpackIntent = new Intent(getActivity(), SelectBackpack.class);
        settingsIntent = new Intent(getActivity(), SettingsActivity.class);
        
        backpackButton = (ImageButton)view.findViewById(R.id.ImageButtonBackPack);
        backpackButton.setOnClickListener(onButtonBackpackClick);
        
        settingsButton = (ImageButton)view.findViewById(R.id.ImageButtonSettings);
        settingsButton.setOnClickListener(onButtonSettingsClick);
        
        friendsButton = (ImageButton)view.findViewById(R.id.ImageButtonMyFriends);
        friendsButton.setOnClickListener(onButtonFriendsClick);
        
        wrenchButton = (ImageButton)view.findViewById(R.id.ImageButtonCatalouge);
        wrenchButton.setOnClickListener(onButtonWrenchClick);
        
        return view;
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    OnClickListener onButtonBackpackClick = new OnClickListener(){
		public void onClick(View v) {
			backpackIntent.putExtra("id", playerId);
			startActivity(backpackIntent);
		}
    };
    
    OnClickListener onButtonSettingsClick = new OnClickListener(){
		public void onClick(View v) {
			startActivity(settingsIntent);
		}
    };
    
    OnClickListener onButtonFriendsClick = new OnClickListener(){
		public void onClick(View v) {
			startActivity(new Intent(getActivity(), PlayerListActivity.class).setAction("com.minder.app.tf2backpack.VIEW_FRIENDS"));
		}
    };
    
    OnClickListener onButtonWrenchClick = new OnClickListener(){
		public void onClick(View v) {
			//startActivity(new Intent(DashBoard.this, PlayerList.class).setAction("com.minder.app.tf2backpack.VIEW_WRENCH"));
			//startActivity(new Intent(DashBoard.this, ItemGridViewer.class).setAction("com.minder.app.tf2backpack.VIEW_ALL_ITEMS"));
			startActivity(new Intent(getActivity(), ItemListSelect.class));
		}
    };
    
    OnClickListener onNewsItemClick = new OnClickListener(){
		public void onClick(View v) {
			// TODO this aint ok
			startActivity(new Intent(getActivity(), NewsFragment.class));
		}
    };
}