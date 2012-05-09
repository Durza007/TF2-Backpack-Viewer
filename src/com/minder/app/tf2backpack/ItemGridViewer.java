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
					new LoadAllItems().execute((Void[])null);
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
    
    public class LoadAllItems extends AsyncTask<Void, Item, Void> {
		@Override
		protected Void doInBackground(Void... params) {
	        DataBaseHelper db = new DataBaseHelper(ItemGridViewer.this);
	        SQLiteDatabase sqlDb = db.getReadableDatabase();
	        
	        Cursor c = sqlDb.rawQuery("SELECT defindex, quality FROM items", null);
	        
	        if (c != null) {
	        	while (c.moveToNext()) {
	        		Item item = new Item(c.getInt(0), -1, c.getInt(1), 0, 0);
	        		publishProgress(item);
	        	}
	        }
	        c.close();
	        sqlDb.close();
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Item... item) {
			itemAdapter.addItem(item[0]);
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
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
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
