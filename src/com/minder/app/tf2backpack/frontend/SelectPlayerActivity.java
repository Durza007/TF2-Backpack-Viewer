package com.minder.app.tf2backpack.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.frontend.SelectPlayerFragment.OnSearchClickedListener;

public class SelectPlayerActivity extends FragmentActivity {
	private SelectPlayerFragment selectPlayerFragment;
	private SearchFragment searchFragment;
	private boolean hasDoublePane;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.select_player);
		
		// check if we have double-pane layout
		hasDoublePane = null != findViewById(R.id.frameLayoutSearch);;
		
		selectPlayerFragment = new SelectPlayerFragment();
		selectPlayerFragment.setPlayerSelectedListener(playerSelectedListener);
		selectPlayerFragment.setSearchClickedListener(searchClickedListener);
		
		getSupportFragmentManager()
			.beginTransaction()
			.add(R.id.frameLayoutSelectPlayer, selectPlayerFragment, "selectPlayerFragment")
			.commit();
	}
	
	private OnPlayerSelectedListener playerSelectedListener = new OnPlayerSelectedListener() {	
		public void onPlayerSelected(SteamUser user, int index) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("user", user);
			setResult(RESULT_OK, resultIntent);
			finish();
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
				final SearchFragment fragment = SearchFragment.createInstance(searchQuery);
				fragment.setPlayerSelectedListener(playerSelectedListener);
				
				getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.frameLayoutSelectPlayer, fragment, "searchFragment")
					.addToBackStack(null)
					.commit();		
			}
		}
	};
}
