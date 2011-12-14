package com.minder.app.tf2backpack;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.PlayerItemListParser.Item;

public class ItemGridViewer extends Activity {
	public class ItemAdapter extends BaseAdapter {
		private class ViewHolder {
			public TextView textCount;
			public ImageButton imageButton;
			public ImageView colorSplat;
			public FrameLayout root;
			
			public void SetView(View v){
				root = (FrameLayout)v.findViewById(R.id.FrameLayoutRoot);
				colorSplat = (ImageView)v.findViewById(R.id.ImageViewItemColor);
				textCount = (TextView)v.findViewById(R.id.TextViewCount);
				imageButton = (ImageButton)v.findViewById(R.id.ImageButtonCell);
			}
		}
		
	    private LayoutInflater mInflater;        
	    private Context mContext;
		private ArrayList<Item> itemList;
		private boolean coloredCells;
		private int imageSize;
		
		public ItemAdapter(Context c, boolean coloredCells) {
			this.mContext = c;
			this.mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.coloredCells = coloredCells;
			imageSize = c.getResources().getDimensionPixelSize(R.dimen.button_size_itemlist);
			itemList = new ArrayList<Item>();
		}

		public int getCount() {
			return itemList.size();
		}

		public Object getItem(int position) {
			return itemList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		public void setList(ArrayList<Item> list) {
			itemList.clear();
			itemList.addAll(list);
			notifyDataSetChanged();
		}
		
		public void addItem(Item item) {
			itemList.add(item);
			notifyDataSetChanged();
		}
		
		public ArrayList<Item> getList() {
			return itemList;
		}

		public View getView(int position, View convertView, ViewGroup parent) {	        
	        ViewHolder holder;
	        if (convertView == null) {
	            convertView = mInflater.inflate(R.layout.backpack_cell, null);
	            holder = new ViewHolder();
	            holder.SetView(convertView);            
	            holder.imageButton.setOnClickListener(clickListener);

	            convertView.setTag(holder);
	        } else {
	        	holder = (ViewHolder)convertView.getTag();
	        }
	        
	        Item item = itemList.get(position);
            holder.imageButton.setTag(position);
            
			try {
				FileInputStream in = openFileInput(item.getDefIndex() + ".png");
				
				Bitmap image = BitmapFactory.decodeStream(in);
				if (image != null){
					Bitmap newImage = Util.getResizedBitmap(image, imageSize, imageSize);
					image.recycle();
					image = newImage;
				} else {
					throw new FileNotFoundException();
				}

				holder.imageButton.setImageBitmap(image);
				
				if (coloredCells == true){
    				final int quality = item.getQuality();
    				if (quality >= 1 && quality <= 9){
    					if (quality != 4 || quality != 6 || quality != 2){
    						holder.imageButton.setBackgroundResource(R.drawable.backpack_cell_white);
    						holder.imageButton.getBackground().setColorFilter(Util.getItemColor(quality), PorterDuff.Mode.MULTIPLY);
    					}
    				}
    				else {
						holder.imageButton.setBackgroundResource(R.drawable.backpack_cell);
						holder.imageButton.getBackground().clearColorFilter();
    				}
				}
				
				if (item.getQuantity() > 1){
					holder.textCount.setVisibility(View.VISIBLE);
					holder.textCount.setText(String.valueOf(item.getQuantity()));
				} else {
					holder.textCount.setVisibility(View.GONE);
				}
				
				int color = item.getColor();
				if (color != 0){
					if (color == 1){		
						holder.colorSplat.setImageBitmap(
								BitmapFactory.decodeResource(
										getResources(), R.drawable.color_circle_team_spirit));
						holder.colorSplat.setVisibility(View.VISIBLE);
						holder.colorSplat.setColorFilter(null);
					} else {
						//ColorFilter filter = new LightingColorFilter((0xFF << 24) | color, 1);
						holder.colorSplat.setImageBitmap(
								BitmapFactory.decodeResource(
										getResources(), R.drawable.color_circle));
						holder.colorSplat.setVisibility(View.VISIBLE);
						holder.colorSplat.setColorFilter(null);
						holder.colorSplat.setColorFilter((0xFF << 24) | color, PorterDuff.Mode.SRC_ATOP);
					}
				} else {
					holder.colorSplat.setVisibility(View.GONE);
				}

			} catch (FileNotFoundException e) {
				// no image found - no problem
				holder.imageButton.setImageBitmap(Util.getResizedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.unknown), imageSize, imageSize));
				/*SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ItemGridViewer.this);
				if (!sp.getBoolean("skipunknownitemdialog", false)){
					showDialog(DIALOG_UNKNOWN_ITEM);
				}*/
			}
	        
	        return convertView;
		}
		
	}
	
	private final int DIALOG_UNKNOWN_ITEM = 0;
	private AdView adView;
	private com.minder.app.tf2backpack.ItemAdapter itemAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
		
		setContentView(R.layout.item_grid);
		
		adView = (AdView)findViewById(R.id.ad);
		
		GridView grid = (GridView)findViewById(R.id.gridViewItems);
		itemAdapter = new com.minder.app.tf2backpack.ItemAdapter(this, true);
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
