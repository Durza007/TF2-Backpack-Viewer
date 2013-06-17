package com.minder.app.tf2backpack.frontend;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.backend.Item;

public class ItemGridViewerActivity extends FragmentActivity {
	private ItemGridViewerFragment itemGridviewerFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_generic);
		
		final FragmentManager fragmentManager = getSupportFragmentManager();
		
		ArrayList<Item> itemList = getIntent().getParcelableArrayListExtra("list");	
		if (itemList != null) {
			itemGridviewerFragment = ItemGridViewerFragment.createInstance(itemList);
			fragmentManager
				.beginTransaction()
				.add(R.id.frameLayoutFragment, itemGridviewerFragment, "itemGridviewerFragment")
				.commit();				
		} else {
			finish();
		}
	}
}
