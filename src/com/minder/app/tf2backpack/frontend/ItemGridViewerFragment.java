package com.minder.app.tf2backpack.frontend;

import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.AsyncTask;
import com.minder.app.tf2backpack.ItemAdapter;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.DataBaseHelper;
import com.minder.app.tf2backpack.backend.Item;

public class ItemGridViewerFragment extends Fragment {
	private final int ITEM_FILTER_NONE = 0;
	private final int ITEM_FILTER_HATS = 1;
	private final int ITEM_FILTER_DEFAULT = 2;
	private final int ITEM_FILTER_TOOLS = 3;
	private final int ITEM_FILTER_MISC = 4;
	private final int ITEM_FILTER_PRIMARY = 5;
	private final int ITEM_FILTER_SECONDARY = 6;
	private final int ITEM_FILTER_MELEE = 7;
	
	private String title;
	private String action;
	private AdView adView;
	private ItemAdapter itemAdapter;
	private List<Item> itemList;
	
	public static ItemGridViewerFragment createInstance(String action) {
		final ItemGridViewerFragment fragment = new ItemGridViewerFragment();
		fragment.action = action;
		return fragment;
	}
	
	public static ItemGridViewerFragment createInstance(List<Item> list) {
		final ItemGridViewerFragment fragment = new ItemGridViewerFragment();
		fragment.action = "com.minder.app.tf2backpack.VIEW_ITEM_LIST";
		fragment.itemList = list;
		return fragment;
	}
	
	public void setNewAction(String action) {
		this.action = action;
		loadItems();
	}
	
	public ItemGridViewerFragment() {
		this.title = "";
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO find out why false is needed here and not anywhere else
		View view = inflater.inflate(R.layout.item_grid, container, false);
		
		adView = (AdView)view.findViewById(R.id.ad);
		
		final GridView grid = (GridView)view.findViewById(R.id.gridViewItems);
		itemAdapter = new ItemAdapter(getActivity(), true);
		itemAdapter.setOnClickListener(clickListener);
		
		if (itemList != null) {
			itemAdapter.setList(itemList);
		} else {
			if (action != null){
				if (action.equals("com.minder.app.tf2backpack.VIEW_ITEM_LIST")) {				
					if (itemList != null){
						itemAdapter.setList(itemList);
					} else {
						Log.e(Util.GetTag(), "Item list was null");
					}		
				} else {
					loadItems();
				}
			}
		}
		grid.setAdapter(itemAdapter);
    	itemList = itemAdapter.getList();
		
		if (title.length() != 0) {
			getActivity().setTitle(title);
		}
		
		return view;
	}
	
	OnClickListener clickListener = new OnClickListener() {
		public void onClick(View v) {	
			Intent weaponInfoIntent = new Intent(getActivity(), WeaponInfo.class);
			weaponInfoIntent.putExtra("com.minder.app.tf2backpack.PlayerItemParser.Item", (Item)itemAdapter.getItem((Integer)v.getTag()));
			startActivity(weaponInfoIntent);
		}	
	};
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	itemAdapter.stopImageLoader();
    	
    	if (adView != null) {
    		adView.destroy();
    	}
    }
    
    private void loadItems() {
		itemAdapter.setList(Collections.<Item> emptyList());
    	if (action.equals("VIEW_ALL_ITEMS")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_NONE);
		} else if (action.equals("VIEW_ALL_HATS")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_HATS);
		} else if (action.equals("VIEW_ALL_DEFAULT")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_DEFAULT);
		} else if (action.equals("VIEW_ALL_TOOLS")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_TOOLS);
		} else if (action.equals("VIEW_ALL_MISC")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_MISC);
		} else if (action.equals("VIEW_ALL_PRIMARY")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_PRIMARY);
		} else if (action.equals("VIEW_ALL_SECONDARY")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_SECONDARY);
		} else if (action.equals("VIEW_ALL_MELEE")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_MELEE);
		}
    }
    
    private class LoadAllItems extends AsyncTask<Integer, Item, Void> {
		@Override
		protected Void doInBackground(Integer... params) {
			final int filter = params[0];
	        DataBaseHelper db = new DataBaseHelper(getActivity());
	        SQLiteDatabase sqlDb = db.getReadableDatabase();
	        
	        String query;
	        switch (filter) {
		        case ITEM_FILTER_HATS:
		        	query = "SELECT defindex, quality, type_name FROM items WHERE type_name = 'Hat'";
		        	break;
		        	
		        case ITEM_FILTER_DEFAULT:
		        	query = "SELECT defindex, quality, type_name FROM items WHERE quality = 0";
		        	break;
		        	
		        case ITEM_FILTER_TOOLS:
		        	query = "SELECT defindex, quality, type_name FROM items WHERE type_name = 'Tool'";
		        	break;
		        	
		        case ITEM_FILTER_MISC:
		        	query = "SELECT defindex, quality, type_name FROM items WHERE item_slot = 'misc'";
		        	break;
		        	
		        case ITEM_FILTER_PRIMARY:
		        	query = "SELECT defindex, quality, type_name FROM items WHERE item_slot = 'primary'";
		        	break;
		        	
		        case ITEM_FILTER_SECONDARY:
		        	query = "SELECT defindex, quality, type_name FROM items WHERE item_slot = 'secondary'";
		        	break;
		        	
		        case ITEM_FILTER_MELEE:
		        	query = "SELECT defindex, quality, type_name FROM items WHERE item_slot = 'melee'";
		        	break;
		        	
		        // use the same for the default and no filter
		        case ITEM_FILTER_NONE:	        	
		        default:
		        	query = "SELECT defindex, quality FROM items";
		        	break;
	        }
	        
	        
	        Cursor c = sqlDb.rawQuery(query, null);
	        
	        if (c != null) {
	        	while (c.moveToNext()) {
	        		Item item = new Item(c.getInt(0), -1, c.getInt(1), 0, 0);
	        		publishProgress(item);
	        	}
	        	
		        c.close();
	        }
	        sqlDb.close();
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Item... item) {
			itemAdapter.addItem(item[0]);
			
			title = "Viewing " + itemAdapter.getCount() + " items";
			getActivity().setTitle(title);
		}
    }
}
