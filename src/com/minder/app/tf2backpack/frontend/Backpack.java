package com.minder.app.tf2backpack.frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.Item;
import com.minder.app.tf2backpack.backend.PlayerItemListParser;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.frontend.BackpackView.OnLayoutReadyListener;

public class Backpack extends Activity {
	private static class DataHolder {
		public ArrayList<Item> playerItemList;
		public int pageNumber;
		public int numberOfPages;
		
		public DataHolder(ArrayList<Item> list, int pageNumber, int numberOfPages){
			this.playerItemList = list;
			this.pageNumber = pageNumber;
			this.numberOfPages = numberOfPages;
		}
	}
	
	private final int DIALOG_UNKNOWN_ITEM = 0;
	
	private AdView adView;
	private BackpackView backpack;
	private boolean addPlayerDataToView;
	private Button newButton;
	private int onPageNumber;
	private int numberOfPages = 6;
	private boolean checkUngivenItems;
	private TextView pageNumberText;
	private ProgressDialog myProgressDialog;
	private ArrayList<Item> playerItemList;
	private String playerId;
	private ArrayList<Item> ungivenList;
	
	
	
	private Comparator<Item> comparator = new BackpackPosComparator();
	
	
	private boolean coloredCells;
	//private final int defaultColor = 0xFF847569;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.backpack);
        
        Bundle extras = getIntent().getExtras();
        playerId = "";
        if(extras != null){
        	playerId = extras.getString("id");
        }
        
        //dataPath = this.getFilesDir().toURI();
        
        Log.d("NewBackpack", "onCreate");
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        coloredCells = sp.getBoolean("backpackrarity", true);
        
        // Create the backpack grid
        backpack = (BackpackView)findViewById(R.id.TableLayoutBackPack);
        //layout.setLayoutParams( new TableLayout.LayoutParams(4,5) );
        //backpack.setStretchAllColumns(true);    
        backpack.setOnClickListener(onButtonBackpackClick);
        backpack.setOnReadyListener(onLayoutReadyListener);
        backpack.setColoredCells(coloredCells);
        
        Button nextButton = (Button)findViewById(R.id.ButtonNext);
        nextButton.setOnClickListener(onButtonBackpackClick);
        
        Button previousButton = (Button)findViewById(R.id.ButtonPrevious);
        previousButton.setOnClickListener(onButtonBackpackClick);
        
        newButton = (Button)findViewById(R.id.buttonNew);
        newButton.setOnClickListener(onButtonBackpackClick);
        
        onPageNumber = 1;
        pageNumberText = (TextView)findViewById(R.id.TextViewPageNumber);
        pageNumberText.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/TF2Build.ttf"), 0);
        pageNumberText.setText(onPageNumber + "/" + numberOfPages);
        
        ungivenList = new ArrayList<Item>();
        
        DownloadPlayerData(playerId);
        
        adView = (AdView)findViewById(R.id.ad);
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    	
    	if (myProgressDialog != null) myProgressDialog.dismiss();
    }
    
    @Override
    public void onDestroy() { 	
    	super.onDestroy();
    	
    	if (adView != null) {
    		adView.destroy();
    	}
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.backpack, menu);

        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_view_profile:
        	startActivity(new Intent().setData(Uri.parse("http://steamcommunity.com/profiles/" + playerId)).setAction("android.intent.action.VIEW"));
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    // listener used to make sure data is not added before view is ready for it
    OnLayoutReadyListener onLayoutReadyListener = new OnLayoutReadyListener() {
		public void onReady() {
			if (addPlayerDataToView) {
				addPlayerDataToView = false;
	    		checkUngivenItems = true;
	    		AddPlayerDataToView();
	    		checkUngivenItems = false;
			}
		}  	
    };
    
    AsyncTaskListener asyncTasklistener = new AsyncTaskListener() {
		public void onPreExecute() {
	    	myProgressDialog = null;
	    	myProgressDialog = ProgressDialog.show(Backpack.this, 
	    			"Please wait...", "Downloading player data...", true);
		}

		public void onProgressUpdate(ProgressUpdate object) {
			// nothing
		}

		public void onPostExecute(Object object) {
			if (object != null) {
				PlayerItemListParser pl = (PlayerItemListParser)object;
				
				// handle the request
				if (pl.getStatusCode() == 1){
					playerItemList = pl.getItemList();
					Collections.sort(playerItemList, Backpack.this.comparator);
					
					numberOfPages = pl.getNumberOfBackpackSlots() / 50;
					SetPageNumberText();
					checkUngivenItems = true;
					AddPlayerDataToView();
					checkUngivenItems = false;
				} else {
					if (pl.getStatusCode() == 15){
						Toast.makeText(Backpack.this, R.string.backack_private, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(Backpack.this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				// TODO handle exception
				/*
				 * 				Exception e = request.getException();
				e.printStackTrace();
				if (Internet.isOnline(Backpack.this)){
					if (e instanceof UnknownHostException){
						Toast.makeText(Backpack.this, R.string.no_steam_api, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(Backpack.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					}					
				} else {
					Toast.makeText(Backpack.this, R.string.no_internet, Toast.LENGTH_LONG).show();
				}
				 */
			}
			
			myProgressDialog.dismiss();
		}
	};
    
    public void DownloadPlayerData(String playerId){
    	final Object data = getLastNonConfigurationInstance();
    	
    	if (data != null){
    		DataHolder holder = (DataHolder)data;
    		playerItemList = holder.playerItemList;
    		if (playerItemList != null){
	    		onPageNumber = holder.pageNumber;
	    		numberOfPages = holder.numberOfPages;
	    		SetPageNumberText();
				checkUngivenItems = true;
				AddPlayerDataToView();
				checkUngivenItems = false;
	    		return;
    		}
    	}
    	
    	// TODO Should get a SteamUser object as parameter
    	SteamUser su = new SteamUser();
    	su.steamdId64 = Long.parseLong(playerId);
    	App.getDataManager().requestPlayerItemList(asyncTasklistener, su);
    }
    
    public void AddPlayerDataToView(){
    	long start = System.currentTimeMillis();
    	
    	Log.d("NewBackpack", "AddPlayerDataToVIew");
    	
    	if (playerItemList == null) return;
    	
    	if (checkUngivenItems) {	
	    	for (int index = 0; index < playerItemList.size(); index++){
	    		final int backpackPos = playerItemList.get(index).getBackpackPosition();
	    		
	    		// if the item has been found but not yet given
	    		if (backpackPos == -1) {
	    			ungivenList.add(playerItemList.get(index));
	    			newButton.setVisibility(View.VISIBLE);
	    		}
	    	}
    	}
    	
    	if (!backpack.isTableCreated()) {
    		addPlayerDataToView = true;
    		return;
    	}
    	
    	final int minIndex = (onPageNumber - 1) * 50;
    	final int maxIndex = onPageNumber * 50 - 1;
    	int startIndex = -1;
    	int endIndex = -1;
    	
    	for (int index = 0; index < playerItemList.size(); index++) {
    		final int backpackPos = playerItemList.get(index).getBackpackPosition();
    		
    		if (backpackPos >= minIndex && startIndex == -1) {
    			startIndex = index;
    		}
    		if (backpackPos <= maxIndex) {
    			endIndex = index;
    		} else {
    			break;
    		}
    	}
    	
    	backpack.setItems(playerItemList.subList(startIndex, endIndex + 1), (onPageNumber - 1) * 50);

    	Log.i(Util.GetTag(), "Add data to view: " + (System.currentTimeMillis() - start) + " ms");
    }
    
    private void ChangePage(boolean nextPage){
    	if (nextPage == true){
    		if (onPageNumber < numberOfPages){
    			onPageNumber++;
    			pageNumberText.setText(onPageNumber + "/" + numberOfPages);
    			AddPlayerDataToView();
    		}
    	} else {
    		if (onPageNumber > 1){
    			onPageNumber--;
    			pageNumberText.setText(onPageNumber + "/" + numberOfPages);
    			AddPlayerDataToView();
    		}
    	}
    }
    
    private void SetPageNumberText(){
    	pageNumberText.setText(onPageNumber + "/" + numberOfPages);
    }
    
    OnClickListener onButtonBackpackClick = new OnClickListener(){
		public void onClick(View v) {
			switch(v.getId()){
				case R.id.ButtonNext: {
					ChangePage(true);
					break;
				}
				case R.id.ButtonPrevious: {
					ChangePage(false);
					break;
				}
				
				case R.id.buttonNew: {
					Intent intent = new Intent(Backpack.this, ItemGridViewer.class);
					intent.setAction("com.minder.app.tf2backpack.VIEW_ITEM_LIST");
					intent.putParcelableArrayListExtra("list", ungivenList);
					startActivity(intent);
					break;
				}
				default: {
					if (v.getTag() != null){
						/*Util.setTempItem(playerItemList.get(((Number)v.getTag()).intValue()));
						startActivity(new Intent(Backpack.this, WeaponInfo.class));*/
						
						Intent weaponInfoIntent = new Intent(Backpack.this, WeaponInfo.class);
						weaponInfoIntent.putExtra("com.minder.app.tf2backpack.PlayerItemParser.Item", (Item)v.getTag());
						startActivity(weaponInfoIntent);
					}
					break;
				}
			}
		}
    };
    
    @Override
    public Object onRetainNonConfigurationInstance() { 	
        return new DataHolder(playerItemList, onPageNumber, numberOfPages);
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
                    	startActivity(new Intent(Backpack.this, DashBoard.class).setAction("com.minder.app.tf2backpack.DOWNLOAD_GAMEFILES"));
                    	finish();
                    }
                })
                .setNeutralButton(R.string.alert_dialog_dont_show_again, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Backpack.this);
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
    
    private static class BackpackPosComparator implements java.util.Comparator<Item> {
		public int compare(Item left, Item right) {
			return left.getBackpackPosition() - right.getBackpackPosition();
		}
    	
    }
}