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

import com.minder.app.tf2backpack.AsyncTask;
import com.minder.app.tf2backpack.ItemAdapter;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.DataBaseHelper;
import com.minder.app.tf2backpack.backend.GameSchemeParser;
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
	private final int ITEM_FILTER_SCOUT = 8;
	private final int ITEM_FILTER_PYRO = 9;
	private final int ITEM_FILTER_DEMOMAN = 10;
	private final int ITEM_FILTER_SOLDIER = 11;
	private final int ITEM_FILTER_HEAVY = 12;
	private final int ITEM_FILTER_ENGINEER = 13;
	private final int ITEM_FILTER_MEDIC = 14;
	private final int ITEM_FILTER_SNIPER = 15;
	private final int ITEM_FILTER_SPY = 16;
	
	private String title;
	private String action;
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
		} else if (action.equals("VIEW_ALL_SCOUT")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_SCOUT);
		} else if (action.equals("VIEW_ALL_PYRO")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_PYRO);
		} else if (action.equals("VIEW_ALL_DEMOMAN")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_DEMOMAN);
		} else if (action.equals("VIEW_ALL_SOLDIER")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_SOLDIER);
		} else if (action.equals("VIEW_ALL_HEAVY")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_HEAVY);
		} else if (action.equals("VIEW_ALL_ENGINEER")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_ENGINEER);
		} else if (action.equals("VIEW_ALL_MEDIC")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_MEDIC);
		} else if (action.equals("VIEW_ALL_SNIPER")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_SNIPER);
		} else if (action.equals("VIEW_ALL_SPY")){
			new LoadAllItems().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ITEM_FILTER_SPY);
		}
    }
    
    private class LoadAllItems extends AsyncTask<Integer, Item, Void> {
		@Override
		protected Void doInBackground(Integer... params) {
			final int filter = params[0];
	        DataBaseHelper db = new DataBaseHelper(getActivity());
	        SQLiteDatabase sqlDb = db.getReadableDatabase();
	        
	        String where;
	        switch (filter) {
		        case ITEM_FILTER_HATS:
                    where = "type_name = 'Hat'";
		        	break;
		        	
		        case ITEM_FILTER_DEFAULT:
                    where = "quality = 0";
		        	break;
		        	
		        case ITEM_FILTER_TOOLS:
                    where = "type_name = 'Tool'";
		        	break;
		        	
		        case ITEM_FILTER_MISC:
                    where = "item_slot = 'misc'";
		        	break;
		        	
		        case ITEM_FILTER_PRIMARY:
                    where = "item_slot = 'primary'";
		        	break;
		        	
		        case ITEM_FILTER_SECONDARY:
                    where = "item_slot = 'secondary'";
		        	break;

				case ITEM_FILTER_MELEE:
					where = "item_slot = 'melee'";
					break;
		        	
		        case ITEM_FILTER_SCOUT:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_SCOUT + ") != 0";
					break;

				case ITEM_FILTER_PYRO:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_PYRO + ") != 0";
					break;

				case ITEM_FILTER_DEMOMAN:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_DEMOMAN + ") != 0";
					break;

				case ITEM_FILTER_SOLDIER:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_SOLDIER + ") != 0";
					break;

				case ITEM_FILTER_HEAVY:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_HEAVY + ") != 0";
					break;

				case ITEM_FILTER_ENGINEER:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_ENGINEER + ") != 0";
					break;

				case ITEM_FILTER_MEDIC:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_MEDIC + ") != 0";
					break;

				case ITEM_FILTER_SNIPER:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_SNIPER + ") != 0";
					break;

				case ITEM_FILTER_SPY:
					where = "used_by_classes != " + GameSchemeParser.TF2Weapon.USED_BY_ALL + " AND (used_by_classes & " + GameSchemeParser.TF2Weapon.USED_BY_SPY + ") != 0";
					break;
		        	
		        // use the same for the default and no filter
                case ITEM_FILTER_NONE:
		        default:
                    where = null;
		        	break;
	        }

	        String query = "SELECT defindex, quality, image_url FROM items" + (where != null ? " WHERE " + where : "");
	        Cursor c = sqlDb.rawQuery(query, null);
	        
	        if (c != null) {
	        	while (c.moveToNext()) {
	        		Item item = new Item(c.getInt(0), c.getInt(1), c.getString(2));
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
