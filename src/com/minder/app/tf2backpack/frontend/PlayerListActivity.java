package com.minder.app.tf2backpack.frontend;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.frontend.PlayerListFragment.OnPlayerSelectedListener;

public class PlayerListActivity extends FragmentActivity {
	private boolean hasBackpackView;
	private PlayerListFragment playerListFragment;
	private Request currentRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String action = this.getIntent().getAction();
        if (action == null) {
        	finish();
        	return;
        }
        
        setContentView(R.layout.player_list_activity);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        final View backpackView = findViewById(R.id.frameLayoutBackpackView);
        hasBackpackView = backpackView != null;
        
        final FragmentManager fragmentManager = this.getSupportFragmentManager();
        // check if an old fragment still exists
        if (savedInstanceState != null) {
        	playerListFragment = (PlayerListFragment) fragmentManager.findFragmentByTag("playerListFragment");
        } else {
        	playerListFragment = new PlayerListFragment();
        	fragmentManager
            	.beginTransaction()
            		.add(R.id.frameLayoutPlayerList, playerListFragment, "playerListFragment")
            		.commit();
        	
        	// need to fetch friend data
            SharedPreferences playerPrefs = getSharedPreferences("player", Activity.MODE_PRIVATE);
            
            String playerId = playerPrefs.getString("id", null);
            if (playerId != null) {
            	SteamUser user = new SteamUser();
            	user.steamdId64 = Long.parseLong(playerId);
            	
            	currentRequest = App.getDataManager().requestFriendsList(friendListListener, user);
            }
        }
        
        
        playerListFragment.setListItemSelectable(hasBackpackView);   
    	playerListFragment.addPlayerSelectedListener(onPlayerSelectedListener);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	playerListFragment.removePlayerSelectedListener(onPlayerSelectedListener);
    }
    
    private OnPlayerSelectedListener onPlayerSelectedListener = new OnPlayerSelectedListener() {
		public void onPlayerSelected(SteamUser user) {
			if (hasBackpackView) {
		        FragmentManager fragmentManager = PlayerListActivity.this.getSupportFragmentManager();
		        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		        
		        final View view = findViewById(R.id.frameLayoutBackpackView);   
		        BackpackFragment backpackFragment = BackpackFragment.newInstance(user, view.getWidth());
		        fragmentTransaction.add(R.id.frameLayoutBackpackView, backpackFragment);
		        fragmentTransaction.commit();
			} else {
		        startActivity(new Intent(PlayerListActivity.this, BackpackActivity.class)
		        	.putExtra("com.minder.app.tf2backpack.SteamUser", user));
			}
		}
	};

    private AsyncTaskListener friendListListener = new AsyncTaskListener() {
		public void onPreExecute() {
			//mProgress = ProgressDialog.show(PlayerListFragment.this.getActivity(), getResources().getText(R.string.please_wait_title), getResources().getText(R.string.downloading_player_list), true, true);
		}

		public void onProgressUpdate(ProgressUpdate object) {
			// nothing
		}

		@SuppressWarnings("unchecked")
		public void onPostExecute(Request request) {
			Object result = request.getData();
        	if (result != null){
        		currentRequest = 
        			App.getDataManager().requestSteamUserInfo(
        					getSteamUserInfoListener, 
        					((ArrayList<SteamUser>) result));
        		playerListFragment.setPlayerList((List<SteamUser>)result);
        	}
		}
	};
	
    private AsyncTaskListener getSteamUserInfoListener = new AsyncTaskListener () {
		public void onPreExecute() {
			//setProgressBarIndeterminateVisibility(true);
		}

		public void onProgressUpdate(ProgressUpdate object) {
			playerListFragment.refreshList();
		}

		public void onPostExecute(Request request) {
			//setProgressBarIndeterminateVisibility(false);
		}
	};
}
