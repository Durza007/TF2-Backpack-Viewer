package com.minder.app.tf2backpack.frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.Item;
import com.minder.app.tf2backpack.backend.PlayerItemListParser;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.frontend.BackpackView.OnLayoutReadyListener;

public class BackpackFragment extends Fragment {
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
	private final int DIALOG_STATS = 1;
	
	private SteamUser currentUser;

	private BackpackView backpackView;
	private boolean addPlayerDataToView;
	private Button newButton;
	private int onPageNumber;
	private int numberOfPages = 6;
	private boolean checkUngivenItems;
	private TextView pageNumberText;
	private ProgressDialog myProgressDialog;
	private ArrayList<Item> playerItemList;
	//private ArrayList<WeaponImage> weaponImageList;
	private ArrayList<Item> ungivenList;
	
	private boolean coloredCells;
	private final Comparator<Item> comparator;
	
	public BackpackFragment(SteamUser user) {
		this.currentUser = user;
		this.comparator = new BackpackPosComparator();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.backpack, container, false);

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this.getActivity());
		coloredCells = sp.getBoolean("backpackrarity", true);

		// Create the backpack grid
		backpackView = (BackpackView) view.findViewById(R.id.TableLayoutBackPack);
		// layout.setLayoutParams( new TableLayout.LayoutParams(4,5) );
		// backpack.setStretchAllColumns(true);
		backpackView.setOnClickListener(onButtonBackpackClick);
		backpackView.setOnReadyListener(onLayoutReadyListener);

		Button nextButton = (Button) view.findViewById(R.id.ButtonNext);
		nextButton.setOnClickListener(onButtonBackpackClick);

		Button previousButton = (Button) view.findViewById(R.id.ButtonPrevious);
		previousButton.setOnClickListener(onButtonBackpackClick);

		newButton = (Button) view.findViewById(R.id.buttonNew);
		newButton.setOnClickListener(onButtonBackpackClick);

		onPageNumber = 1;
		pageNumberText = (TextView) view.findViewById(R.id.TextViewPageNumber);
		pageNumberText.setTypeface(Typeface.createFromAsset(getActivity()
				.getAssets(), "fonts/TF2Build.ttf"), 0);
		pageNumberText.setText(onPageNumber + "/" + numberOfPages);

		ungivenList = new ArrayList<Item>();

		downloadPlayerData();

		return null;
	}
	
    // listener used to make sure data is not added before view is ready for it
    OnLayoutReadyListener onLayoutReadyListener = new OnLayoutReadyListener() {
		public void onReady() {
			if (addPlayerDataToView) {
				addPlayerDataToView = false;
	    		checkUngivenItems = true;
	    		addPlayerDataToView();
	    		checkUngivenItems = false;
			}
		}  	
    };

	AsyncTaskListener asyncTasklistener = new AsyncTaskListener() {
		public void onPreExecute() {
			myProgressDialog = null;
			myProgressDialog = ProgressDialog.show(
					BackpackFragment.this.getActivity(), "Please wait...",
					"Downloading player data...", true);
		}

		public void onProgressUpdate(ProgressUpdate object) {
			// nothing
		}

		public void onPostExecute(Object object) {
			if (object != null) {
				PlayerItemListParser pl = (PlayerItemListParser) object;

				// handle the request
				if (pl.getStatusCode() == 1) {
					playerItemList = pl.getItemList();
					Collections.sort(playerItemList, BackpackFragment.this.comparator);
					
					numberOfPages = pl.getNumberOfBackpackSlots() / 50;
					setPageNumberText();
					checkUngivenItems = true;
					addPlayerDataToView();
					checkUngivenItems = false;
				} else {
					if (pl.getStatusCode() == 15) {
						Toast.makeText(BackpackFragment.this.getActivity(),
								R.string.backack_private, Toast.LENGTH_SHORT)
								.show();
					} else {
						Toast.makeText(BackpackFragment.this.getActivity(),
								R.string.unknown_error, Toast.LENGTH_SHORT)
								.show();
					}
				}
			} else {
				// TODO handle exception
				/*
				 * Exception e = request.getException(); e.printStackTrace(); if
				 * (Internet.isOnline(Backpack.this)){ if (e instanceof
				 * UnknownHostException){ Toast.makeText(Backpack.this,
				 * R.string.no_steam_api, Toast.LENGTH_LONG).show(); } else {
				 * Toast.makeText(Backpack.this, e.getLocalizedMessage(),
				 * Toast.LENGTH_LONG).show(); } } else {
				 * Toast.makeText(Backpack.this, R.string.no_internet,
				 * Toast.LENGTH_LONG).show(); }
				 */
			}

			myProgressDialog.dismiss();
		}
	};

    OnClickListener onButtonBackpackClick = new OnClickListener(){
		public void onClick(View v) {
			switch(v.getId()){
				case R.id.ButtonNext: {
					changePage(true);
					break;
				}
				case R.id.ButtonPrevious: {
					changePage(false);
					break;
				}
				
				case R.id.buttonNew: {
					Intent intent = new Intent(BackpackFragment.this.getActivity(), ItemGridViewer.class);
					intent.setAction("com.minder.app.tf2backpack.VIEW_ITEM_LIST");
					intent.putParcelableArrayListExtra("list", ungivenList);
					startActivity(intent);
					break;
				}
				default: {
					if (v.getTag() != null){
						/*Util.setTempItem(playerItemList.get(((Number)v.getTag()).intValue()));
						startActivity(new Intent(Backpack.this, WeaponInfo.class));*/
						
						Intent weaponInfoIntent = new Intent(BackpackFragment.this.getActivity(), WeaponInfo.class);
						weaponInfoIntent.putExtra("com.minder.app.tf2backpack.PlayerItemParser.Item", playerItemList.get(((Number)v.getTag()).intValue()));
						startActivity(weaponInfoIntent);
					}
					break;
				}
			}
		}
    };
    
    private void changePage(boolean nextPage){
    	if (nextPage == true){
    		if (onPageNumber < numberOfPages){
    			onPageNumber++;
    			pageNumberText.setText(onPageNumber + "/" + numberOfPages);
    			addPlayerDataToView();
    		}
    	} else {
    		if (onPageNumber > 1){
    			onPageNumber--;
    			pageNumberText.setText(onPageNumber + "/" + numberOfPages);
    			addPlayerDataToView();
    		}
    	}
    }
    
    private void setPageNumberText(){
    	pageNumberText.setText(onPageNumber + "/" + numberOfPages);
    }
    
    public void downloadPlayerData(){ 	
    	App.getDataManager().requestPlayerItemList(asyncTasklistener, currentUser);
    }
    
    public void addPlayerDataToView(){
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
    	
    	if (!backpackView.isTableCreated()) {
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
    	
    	backpackView.setItems(playerItemList.subList(startIndex, endIndex + 1), (onPageNumber - 1) * 50);

    	Log.i(Util.GetTag(), "Add data to view: " + (System.currentTimeMillis() - start) + " ms");
    }
    
    private static class BackpackPosComparator implements java.util.Comparator<Item> {
		public int compare(Item left, Item right) {
			return left.getBackpackPosition() - right.getBackpackPosition();
		}
    	
    }
}