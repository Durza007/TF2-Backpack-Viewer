package com.minder.app.tf2backpack;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.GridView;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.PlayerItemListParser.Item;

public class ItemGridViewer extends Activity {
	private final int ITEM_FILTER_NONE = 0;
	private final int ITEM_FILTER_HATS = 1;
	private final int ITEM_FILTER_DEFAULT = 2;
	private final int ITEM_FILTER_TOOLS = 3;
	private final int ITEM_FILTER_MISC = 4;
	private final int ITEM_FILTER_PRIMARY = 5;
	private final int ITEM_FILTER_SECONDARY = 6;
	private final int ITEM_FILTER_MELEE = 7;
	
	private final int DIALOG_UNKNOWN_ITEM = 0;
	private AdView adView;
	private ItemAdapter itemAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
		
		setContentView(R.layout.item_grid);
		
		adView = AdMobActivity.createAdView(adView, this);
		
		GridView grid = (GridView)findViewById(R.id.gridViewItems);
		itemAdapter = new ItemAdapter(this, true);
		itemAdapter.setOnClickListener(clickListener);

		@SuppressWarnings("unchecked")
		final ArrayList<Item> oldIitemList = (ArrayList<Item>)getLastNonConfigurationInstance();	
		
		if (oldIitemList != null) {
			itemAdapter.setList(oldIitemList);
		} else {
			String action = getIntent().getAction();
			if (action != null){
				if (action.equals("com.minder.app.tf2backpack.VIEW_ITEM_LIST")) {
					ArrayList<Item> itemList = getIntent().getParcelableArrayListExtra("list");
					
					if (itemList != null){
						itemAdapter.setList(itemList);
					} else {
						Log.e(Util.GetTag(), "Item list was null");
						finish();
					}		
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_ITEMS")){
					new LoadAllItems().execute(ITEM_FILTER_NONE);
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_HATS")){
					new LoadAllItems().execute(ITEM_FILTER_HATS);
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_DEFAULT")){
					new LoadAllItems().execute(ITEM_FILTER_DEFAULT);
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_TOOLS")){
					new LoadAllItems().execute(ITEM_FILTER_TOOLS);
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_MISC")){
					new LoadAllItems().execute(ITEM_FILTER_MISC);
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_PRIMARY")){
					new LoadAllItems().execute(ITEM_FILTER_PRIMARY);
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_SECONDARY")){
					new LoadAllItems().execute(ITEM_FILTER_SECONDARY);
				} else if (action.equals("com.minder.app.tf2backpack.VIEW_ALL_MELEE")){
					new LoadAllItems().execute(ITEM_FILTER_MELEE);
				}
			} else {
				finish();
			}
		}
		
		grid.setAdapter(itemAdapter);
	}
	
	OnClickListener clickListener = new OnClickListener() {
		public void onClick(View v) {
			/*Util.setTempItem((Item)itemAdapter.getItem((Integer)v.getTag()));
			startActivity(new Intent(ItemGridViewer.this, WeaponInfo.class));*/
			
			Intent weaponInfoIntent = new Intent(ItemGridViewer.this, WeaponInfo.class);
			weaponInfoIntent.putExtra("com.minder.app.tf2backpack.PlayerItemParser.Item", (Item)itemAdapter.getItem((Integer)v.getTag()));
			startActivity(weaponInfoIntent);
		}	
	};
	
    @Override
    public Object onRetainNonConfigurationInstance() {
    	return itemAdapter.getList();
    } 
    
    @Override
    public void onPause() {
    	super.onPause();
    }
	
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	if (adView != null) {
    		adView.destroy();
    	}
    }
    
    public class LoadAllItems extends AsyncTask<Integer, Item, Void> {
		@Override
		protected Void doInBackground(Integer... params) {
			final int filter = params[0];
	        DataBaseHelper db = new DataBaseHelper(ItemGridViewer.this);
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
			ItemGridViewer.this.setTitle("Viewing " + itemAdapter.getCount() + " items");
		}
    }
	
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_UNKNOWN_ITEM:
            return new AlertDialog.Builder(this)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.dialog_notice)
                .setMessage(R.string.message_unknown_item)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	startActivity(new Intent(ItemGridViewer.this, DashBoard.class).setAction("com.minder.app.tf2backpack.DOWNLOAD_GAMEFILES"));
                    	finish();
                    }
                })
                .setNeutralButton(R.string.alert_dialog_dont_show_again, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ItemGridViewer.this);
                    	Editor editor = sp.edit();
                    	editor.putBoolean("skipunknownitemdialog", true);
                    	editor.commit();
                    }
                })
                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .create();
        }
        return null;
    }
}
