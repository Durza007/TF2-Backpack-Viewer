package com.minder.app.tf2backpack.frontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.frontend.SelectPlayerFragment.OnSearchClickedListener;

public class SelectPlayerActivity extends FragmentActivity {
	private SelectPlayerFragment selectPlayerFragment;
	private SearchFragment searchFragment;
	private boolean hasDoublePane;
	private boolean setSteamId;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.select_player);
		
		Bundle data = getIntent().getExtras();
		if (data != null) {
			String title = data.getString("title");
			if (title != null)
				setTitle(title);
		}
		
		String action = getIntent().getAction();
		if (action != null) {
			setSteamId = action.equals("com.minder.app.tf2backpack.SET_STEAMID");
		}
		
		// check if we have double-pane layout
		hasDoublePane = null != findViewById(R.id.frameLayoutSearch);;
		
		if (savedInstanceState != null) {
			final FragmentManager fragmentManager = this.getSupportFragmentManager();
			final boolean hadDoublePane = savedInstanceState.getBoolean("hadDoublePane");
			
			selectPlayerFragment = (SelectPlayerFragment)fragmentManager.findFragmentByTag("selectPlayerFragment");
			searchFragment = (SearchFragment)fragmentManager.findFragmentByTag("searchFragment");
			
			selectPlayerFragment.setPlayerSelectedListener(playerSelectedListener);
			selectPlayerFragment.setSearchClickedListener(searchClickedListener);
			
			// check if we are going from double-pane to
			// single-pane view
			if (searchFragment != null) {
				searchFragment.setPlayerSelectedListener(playerSelectedListener);
				
				if (hadDoublePane && !hasDoublePane) {
					fragmentManager
						.beginTransaction()
						.remove(searchFragment)
						.commit();
					
					fragmentManager.executePendingTransactions();
					
					fragmentManager
						.beginTransaction()
						.add(R.id.frameLayoutSelectPlayer, searchFragment, "searchFragment")
						.addToBackStack(null)
						.commit();
				} else if (!hadDoublePane && hasDoublePane) {
					fragmentManager.popBackStack();
					
					fragmentManager.executePendingTransactions();
					
					fragmentManager
						.beginTransaction()
						.add(R.id.frameLayoutSearch, searchFragment, "searchFragment")
						.commit();
				}
			}		
		} else {
			selectPlayerFragment = new SelectPlayerFragment();
			selectPlayerFragment.setPlayerSelectedListener(playerSelectedListener);
			selectPlayerFragment.setSearchClickedListener(searchClickedListener);
			
			getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.frameLayoutSelectPlayer, selectPlayerFragment, "selectPlayerFragment")
				.commit();
		}
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("hadDoublePane", hasDoublePane);
	}
	
	private OnPlayerSelectedListener playerSelectedListener = new OnPlayerSelectedListener() {	
		public void onPlayerSelected(SteamUser user, int index) {
			if (setSteamId) {
				SharedPreferences.Editor editor = getSharedPreferences("player", MODE_PRIVATE).edit();
				
				if (user != null) {
					editor.putString("id", String.valueOf(user.steamdId64));
					editor.commit();
					Toast.makeText(SelectPlayerActivity.this, R.string.changed_steam_id, Toast.LENGTH_SHORT).show();
					finish();
				}
			} else {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("user", user);
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		}
	};
	
	private OnSearchClickedListener searchClickedListener = new OnSearchClickedListener() {	
		public void onSearchClicked(String searchQuery) {
			if (hasDoublePane) {
				if (searchFragment == null) {
					searchFragment = SearchFragment.createInstance(searchQuery);
					searchFragment.setPlayerSelectedListener(playerSelectedListener);
					
					getSupportFragmentManager()
						.beginTransaction()
						.add(R.id.frameLayoutSearch, searchFragment, "searchFragment")
						.commit();			
				} else {
					searchFragment.newSearchQuery(searchQuery);
				}
			} else {
				if (getSupportFragmentManager().findFragmentByTag("searchFragment") == null) {
					searchFragment = SearchFragment.createInstance(searchQuery);
					searchFragment.setPlayerSelectedListener(playerSelectedListener);
					
					getSupportFragmentManager()
						.beginTransaction()
						.add(R.id.frameLayoutSelectPlayer, searchFragment, "searchFragment")
						.addToBackStack(null)
						.commit();		
				}
			}
		}
	};
}
