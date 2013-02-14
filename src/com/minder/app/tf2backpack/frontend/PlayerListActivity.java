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
import android.view.MenuItem;
import android.view.View;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;

public class PlayerListActivity extends FragmentActivity {
	private boolean hasBackpackView;
	private PlayerListFragment playerListFragment;
	private BackpackFragment backpackFragment;
	private Request currentRequest;
	
    private void showDownloadDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DownloadDialog editNameDialog = new DownloadDialog();
        editNameDialog.show(fm, "download_dialog");
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String action = this.getIntent().getAction();
        if (action == null) {
        	finish();
        	return;
        }
        
        setContentView(R.layout.player_list_activity);
        
        final View backpackView = findViewById(R.id.frameLayoutBackpackView);
        hasBackpackView = backpackView != null;
        
        final FragmentManager fragmentManager = this.getSupportFragmentManager();
        // check if an old fragment still exists
        if (savedInstanceState != null) {
        	playerListFragment = (PlayerListFragment)fragmentManager.findFragmentByTag("playerListFragment");
        	if (!hasBackpackView) {
        		BackpackFragment bp = (BackpackFragment)fragmentManager.findFragmentByTag("backpackFragment");
        		
        		if (bp != null)
        			fragmentManager.beginTransaction().remove(bp).commit();
        	}
        } else {
        	playerListFragment = new PlayerListFragment();
        	fragmentManager
            	.beginTransaction()
            		.add(R.id.frameLayoutPlayerList, playerListFragment, "playerListFragment")
            		.commit();
        	
        	playerListFragment.setListItemSelectable(hasBackpackView);   
        	
        	// need to fetch friend data
            SharedPreferences playerPrefs = getSharedPreferences("player", Activity.MODE_PRIVATE);
            
            String playerId = playerPrefs.getString("id", null);
            if (playerId != null) {
            	SteamUser user = new SteamUser();
            	user.steamdId64 = Long.parseLong(playerId);
            	
            	currentRequest = App.getDataManager().requestFriendsList(friendListListener, user);
            }
        }
        
    	playerListFragment.addPlayerSelectedListener(onPlayerSelectedListener);
    }
    
    @Override
    public void onDestroy() {
    	playerListFragment.removePlayerSelectedListener(onPlayerSelectedListener);
    	super.onDestroy();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Shows the backpack for a given user. Will highlight selection if in tablet
     * mode. Will check if game-scheme is ready and if not display an appropriate dialog
     * @param user The user whose backpack to show
     * @param index The index of the user in the user list
     */
    private void showBackpack(SteamUser user, int index) {
    	if (GameSchemeDownloaderService.isGameSchemeReady()) {
			if (hasBackpackView) {
				
				// check if we already have a fragment
				if (backpackFragment == null) {
					// create a new fragment
			        FragmentManager fragmentManager = PlayerListActivity.this.getSupportFragmentManager();
			        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
			        
			        final View view = findViewById(R.id.frameLayoutBackpackView);   
			        backpackFragment = BackpackFragment.newInstance(user, view.getWidth(), false);
			        fragmentTransaction.add(R.id.frameLayoutBackpackView, backpackFragment, "backpackFragment");
			        fragmentTransaction.commit();
				} else {
					// update old one
					backpackFragment.setSteamUser(user);
				}
		        
		        playerListFragment.setSelectedItem(index);
			} else {
		        startActivity(new Intent(PlayerListActivity.this, BackpackActivity.class)
		        	.putExtra("com.minder.app.tf2backpack.SteamUser", user));
			}
    	} else {
    		if (GameSchemeDownloaderService.isGameSchemeDownloading()) {
    			showDownloadDialog();
    		}
    	}
    }
    
    private OnPlayerSelectedListener onPlayerSelectedListener = new OnPlayerSelectedListener() {
		public void onPlayerSelected(SteamUser user, int index) {
			showBackpack(user, index);
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
