package com.minder.app.tf2backpack.frontend;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.minder.app.tf2backpack.R;

public class CatalogActivity extends FragmentActivity {
	private boolean hasDoublePane;
	private ItemGridViewerFragment itemGridviewerFragment;
	private ItemListSelectFragment itemListSelectFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.catalog_activity);
		
		final FragmentManager fragmentManager = getSupportFragmentManager();
		
		hasDoublePane = null != findViewById(R.id.frameLayoutItemGrid);
		if (savedInstanceState != null) {
			itemListSelectFragment = (ItemListSelectFragment)fragmentManager.findFragmentByTag("itemListSelectFragment");
			itemGridviewerFragment = (ItemGridViewerFragment)fragmentManager.findFragmentByTag("itemGridviewerFragment");
			
			// update old references
			itemListSelectFragment.setOnItemSelectedListener(onItemSelectListener);
			if (hasDoublePane) {
				itemListSelectFragment.setNumberOfColumns(1);
				itemListSelectFragment.setItemsSelectable(true);
			} else {
				itemListSelectFragment.setNumberOfColumns(-1);
				itemListSelectFragment.setItemsSelectable(false);
			}
			
			if (itemGridviewerFragment != null) {
				if (!hasDoublePane) {
					fragmentManager
						.beginTransaction()
						.remove(itemGridviewerFragment)
						.commit();
					
					fragmentManager.executePendingTransactions();
					
					getSupportFragmentManager()
						.beginTransaction()
						.add(R.id.frameLayoutItemList, itemGridviewerFragment, "itemGridviewerFragment")
						.addToBackStack(null)
						.commit();
				} else {
					// we have double pane
					if (!savedInstanceState.getBoolean("hadDoublePane")) {
						fragmentManager.popBackStack();
					
						fragmentManager.executePendingTransactions();
						
						getSupportFragmentManager()
							.beginTransaction()
							.add(R.id.frameLayoutItemGrid, itemGridviewerFragment, "itemGridviewerFragment")
							.commit();
					}
				}
			}
		} else {
			itemListSelectFragment = new ItemListSelectFragment();
			itemListSelectFragment.setOnItemSelectedListener(onItemSelectListener);
			
			if (hasDoublePane) {
				itemListSelectFragment.setNumberOfColumns(1);
				itemListSelectFragment.setItemsSelectable(true);
			}
			
			fragmentManager
				.beginTransaction()
				.add(R.id.frameLayoutItemList, itemListSelectFragment, "itemListSelectFragment")
				.commit();
		}
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("hadDoublePane", hasDoublePane);
	}
	
	private OnItemSelectedListener onItemSelectListener = new OnItemSelectedListener() {	
		public void onSelect(String string) {
			if (hasDoublePane) {
				if (itemGridviewerFragment != null) {
					itemGridviewerFragment.setNewAction(string);
				} else {
					itemGridviewerFragment = ItemGridViewerFragment.createInstance(string);
					
					getSupportFragmentManager()
						.beginTransaction()
						.add(R.id.frameLayoutItemGrid, itemGridviewerFragment, "itemGridviewerFragment")
						.commit();
				}
			} else {
				itemGridviewerFragment = ItemGridViewerFragment.createInstance(string);
				
				getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.frameLayoutItemList, itemGridviewerFragment, "itemGridviewerFragment")
					.addToBackStack(null)
					.commit();
			}
		}
	};
}
