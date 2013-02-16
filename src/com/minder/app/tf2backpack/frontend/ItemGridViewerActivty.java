package com.minder.app.tf2backpack.frontend;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.backend.Item;
import com.minder.app.tf2backpack.frontend.ItemListSelectFragment.OnItemSelectedListener;

public class ItemGridViewerActivty extends FragmentActivity {
	private boolean hasDoublePane;
	private ItemGridViewerFragment itemGridviewerFragment;
	private ItemListSelectFragment itemListSelectFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.item_gridviewer_activity);
		
		String action = getIntent().getAction();
		
		hasDoublePane = null != findViewById(R.id.frameLayoutItemGrid);
		if (action != null) {
			if (action.equals("com.minder.app.tf2backpack.VIEW_ITEM_LIST")) {
				ArrayList<Item> itemList = getIntent().getParcelableArrayListExtra("list");
				
				if (itemList != null) {
					itemGridviewerFragment = ItemGridViewerFragment.createInstance(itemList);
					getSupportFragmentManager()
						.beginTransaction()
						.add(R.id.frameLayoutItemList, itemGridviewerFragment, "itemGridviewerFragment")
						.commit();				
				} else {
					finish();
				}
			}
		} else {
			itemListSelectFragment = new ItemListSelectFragment();
			itemListSelectFragment.setOnItemSelectedListener(onItemSelectListener);
			
			if (hasDoublePane) {
				itemListSelectFragment.setNumberOfColumns(1);
				itemListSelectFragment.setItemsSelectable(true);
			}
			
			getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.frameLayoutItemList, itemListSelectFragment, "itemListSelectFragment")
				.commit();
		}
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
