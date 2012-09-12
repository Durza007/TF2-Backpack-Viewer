package com.minder.app.tf2backpack.frontend;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;

public class PlayerListActivity extends FragmentActivity {
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
        
        setContentView(R.layout.activity_playerlist);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        playerListFragment = new PlayerListFragment();
        fragmentTransaction.add(R.id.frameLayoutFragment, playerListFragment);
        fragmentTransaction.commit();
        
        SharedPreferences playerPrefs = getSharedPreferences("player", Activity.MODE_PRIVATE);
        
        String playerId = playerPrefs.getString("id", null);
        if (playerId != null) {
        	SteamUser user = new SteamUser();
        	user.steamdId64 = Long.parseLong(playerId);
        	
        	currentRequest = App.getDataManager().requestFriendsList(friendListListener, user);
        }
    }

    private AsyncTaskListener friendListListener = new AsyncTaskListener() {
		public void onPreExecute() {
			//mProgress = ProgressDialog.show(PlayerListFragment.this.getActivity(), getResources().getText(R.string.please_wait_title), getResources().getText(R.string.downloading_player_list), true, true);
		}

		public void onProgressUpdate(ProgressUpdate object) {
			// nothing
		}

		@SuppressWarnings("unchecked")
		public void onPostExecute(Object result) {
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

		public void onPostExecute(Object object) {
			//setProgressBarIndeterminateVisibility(false);
		}
	};
}
