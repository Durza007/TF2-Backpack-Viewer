package com.minder.app.tf2backpack.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.R;

public class DashboardFragment extends Fragment {
	private ItemListSelectAdapter adapter;
	private Intent backpackIntent;
	private Intent settingsIntent;
	private String playerId;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_menu, container, false);
        
        backpackIntent = new Intent(getActivity(), SelectBackpack.class);
        settingsIntent = new Intent(getActivity(), SettingsActivity.class);
        
        final GridView gridView = (GridView)view.findViewById(R.id.itemListSelectGridView);
        
        final int columnWidth = getResources().getDimensionPixelSize(R.dimen.button_size_dashboard_item);
        adapter = new ItemListSelectAdapter(getActivity(), 
        		R.array.dashboardTitles, 
        		R.array.dashboardImages, 
        		R.array.dashboardActions,
        		columnWidth);
        
        adapter.setOnItemSelectedListener(itemSelectedListener);
        
        gridView.setAdapter(adapter);
        gridView.setColumnWidth(columnWidth);
        
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
    
    OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {	
		public void onSelect(String string) {
			if (string.equals("VIEW_BACKPACK")) {
				backpackIntent.putExtra("id", playerId);
				startActivity(backpackIntent);
			} else if (string.equals("VIEW_FRIENDS")) {
				startActivity(new Intent(getActivity(), PlayerListActivity.class).setAction("com.minder.app.tf2backpack.VIEW_FRIENDS"));
			} else if (string.equals("VIEW_ITEM_LISTS")) {
		    	if (GameSchemeDownloaderService.isGameSchemeReady()) {
					startActivity(new Intent(getActivity(), CatalogActivity.class));
		    	} else {
		    		if (GameSchemeDownloaderService.isGameSchemeDownloading()) {
		    			DownloadProgressDialog.show(getActivity().getSupportFragmentManager());
		    		} else {
		    			DownloadGameSchemeDialog.show(getActivity().getSupportFragmentManager(), false);
		    		}
		    	}
			} else if (string.equals("VIEW_SETTINGS")) {
				startActivity(settingsIntent);
			}
		}
	};
}