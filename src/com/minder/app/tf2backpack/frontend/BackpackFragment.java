package com.minder.app.tf2backpack.frontend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

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

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
	
	/*private static class WeaponImage {
		public int defIndex;
		private Bitmap image = null;
		
		public Bitmap getImage(){
			return this.image;
		}
		
		public WeaponImage(int defIndex, Bitmap image){
			this.defIndex = defIndex;
			this.image = image;
		}
	}*/
	
	public static class Holder {
		public static TextView textCount;
		public static TextView textEquipped;
		public static ImageButton imageButton;
		public static ImageView colorSplat;
		
		public static void setView(View v){
			colorSplat = (ImageView)v.findViewById(R.id.ImageViewItemColor);
			textCount = (TextView)v.findViewById(R.id.TextViewCount);
			textEquipped = (TextView)v.findViewById(R.id.TextViewEquipped);
			imageButton = (ImageButton)v.findViewById(R.id.ImageButtonCell);
		}
		
		public static void clear() {
			colorSplat = null;
			textCount = null;
			textEquipped = null;
			imageButton = null;
		}
	}
	
	private final int DIALOG_UNKNOWN_ITEM = 0;
	private final int DIALOG_STATS = 1;
	
	private SteamUser currentUser;

	private BackpackView backpack;
	private boolean addPlayerDataToView;
	private Button newButton;
	private Bitmap colorSplat;
	private Bitmap colorTeamSpirit;
	private boolean buttonChanged[];
	private int onPageNumber;
	private int numberOfPages = 6;
	private boolean checkUngivenItems;
	private TextView pageNumberText;
	private ProgressDialog myProgressDialog;
	private ArrayList<Item> playerItemList;
	//private ArrayList<WeaponImage> weaponImageList;
	private String playerId;
	private ArrayList<Item> ungivenList;
	
	private boolean coloredCells;
	
	public BackpackFragment(SteamUser user) {
		this.currentUser = user;
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
		backpack = (BackpackView) view.findViewById(R.id.TableLayoutBackPack);
		// layout.setLayoutParams( new TableLayout.LayoutParams(4,5) );
		// backpack.setStretchAllColumns(true);
		backpack.setOnClickListener(onButtonBackpackClick);
		backpack.setOnReadyListener(onLayoutReadyListener);

		Resources r = this.getResources();

		colorSplat = BitmapFactory.decodeResource(r, R.drawable.color_circle);
		colorTeamSpirit = BitmapFactory.decodeResource(r,
				R.drawable.color_circle_team_spirit);

		buttonChanged = new boolean[50];

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

		downloadPlayerData(playerId);

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
    
    public void downloadPlayerData(String playerId){
    	final Object data = null;
    	
    	if (data != null){
    		DataHolder holder = (DataHolder)data;
    		playerItemList = holder.playerItemList;
    		if (playerItemList != null){
	    		onPageNumber = holder.pageNumber;
	    		numberOfPages = holder.numberOfPages;
	    		setPageNumberText();
				checkUngivenItems = true;
				addPlayerDataToView();
				checkUngivenItems = false;
	    		return;
    		}
    	}
    	
    	// TODO Should get a SteamUser object as parameter
    	SteamUser su = new SteamUser();
    	su.steamdId64 = Long.parseLong(playerId);
    	App.getDataManager().requestPlayerItemList(asyncTasklistener, su);
    }
    
    public void addPlayerDataToView(){
    	if (!backpack.isTableCreated()) {
    		addPlayerDataToView = true;
    		return;
    	}
    	long start = System.currentTimeMillis();
    	
    	Log.d("NewBackpack", "AddPlayerDataToVIew");
    	
    	if (playerItemList == null) return;
    	/*for (int index = 0; index < 50; index++){
    		Holder.SetView(buttonList[index]);
    		Holder.imageButton.setImageBitmap(null);
    		Holder.textCount.setVisibility(View.GONE);
    		Holder.textEquipped.setVisibility(View.GONE);
    		Holder.colorSplat.setVisibility(View.GONE);
    		Holder.imageButton.setTag(null);
    	}*/
    	
    	Item playerItem;
    	//int imageSize = Util.GetPxFromDp(getApplicationContext(), r.getDimension(R.dimen.button_size));
    	int imageSize = (int) (backpack.backpackCellSize);
    	Log.d("Tf2Backpack", "Backpack cell size: " + imageSize);
    	for (int index = 0; index < playerItemList.size(); index++){
    		int backPackPos = playerItemList.get(index).getBackpackPosition();
    		
    		// if the item has been found but not yet given
    		if (backPackPos == -1 && checkUngivenItems) {
    			ungivenList.add(playerItemList.get(index));
    			newButton.setVisibility(View.VISIBLE);
    		}
    		/*int pageNumber = (int) Math.floor(backPackPos / 50) + 1;
    		if (pageNumber > numberOfPages){
    			numberOfPages = pageNumber;
    			SetPageNumberText();
    		}*/
    		if (backPackPos >= (onPageNumber - 1) * 50 && backPackPos < onPageNumber * 50){
    			playerItem = playerItemList.get(index);
				//int imageIndex = FindImage(playerItem.getDefIndex());
    			int imageIndex = -1;
				int buttonIndex = backPackPos - (onPageNumber - 1) * 50;
				
				Holder.setView(backpack.buttonList[buttonIndex]);
				buttonChanged[buttonIndex] = true;
				FileInputStream in = null;
				
				// get item image
    			try {
					in = getActivity().openFileInput(playerItem.getDefIndex() + ".png");
					
					Bitmap image = BitmapFactory.decodeStream(in);
					if (image != null){
						Bitmap newImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
						if (newImage != image) {
							image.recycle();
						}
						image = newImage;
					} else {
						throw new FileNotFoundException();
					}
					
					Holder.imageButton.setImageBitmap(image);

				} catch (FileNotFoundException e) {
					try {
						if (in != null)
							in.close();
					} catch (IOException e1) {
					}

					// set default image
					Holder.imageButton.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.unknown), imageSize, imageSize, false));
					Holder.imageButton.setTag(index);
					e.printStackTrace();
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(BackpackFragment.this.getActivity());
					if (!sp.getBoolean("skipunknownitemdialog", false)){
						//TODO showDialog(DIALOG_UNKNOWN_ITEM);
					}
				}
    			
				if (coloredCells == true){
    				final int quality = playerItem.getQuality();
    				if (quality >=1 && quality <= 13){
    					if (quality != 4 || quality != 6 || quality != 2 || quality != 12){
    						Holder.imageButton.setBackgroundResource(R.drawable.backpack_cell_white);
    						Holder.imageButton.getBackground().setColorFilter(Util.getItemColor(quality), PorterDuff.Mode.MULTIPLY);
    					}
    				}
				}
				
				Holder.imageButton.setTag(index);
				if (playerItem.getQuantity() > 1){
					Holder.textCount.setVisibility(View.VISIBLE);
					Holder.textCount.setText(String.valueOf(playerItem.getQuantity()));
				} else {
					Holder.textCount.setVisibility(View.GONE);
				}
				
				if (playerItem.isEquipped()){
					Holder.textEquipped.setVisibility(View.VISIBLE);
				} else {
					Holder.textEquipped.setVisibility(View.GONE);
				}
				
				int color = playerItem.getColor();
				if (color != 0){
					if (color == 1){		
			    		Holder.colorSplat.setImageBitmap(colorTeamSpirit);
						Holder.colorSplat.setVisibility(View.VISIBLE);
			    		Holder.colorSplat.setColorFilter(null);
					} else {
						//ColorFilter filter = new LightingColorFilter((0xFF << 24) | color, 1);
						Holder.colorSplat.setImageBitmap(colorSplat);
						Holder.colorSplat.setVisibility(View.VISIBLE);
			    		Holder.colorSplat.setColorFilter(null);
						Holder.colorSplat.setColorFilter((0xFF << 24) | color, PorterDuff.Mode.SRC_ATOP);
					}
				} else {
					Holder.colorSplat.setVisibility(View.GONE);
				}
				
				Holder.clear();
    		}
    	}
    	
    	// reset anything we haven't changed
    	final int count = buttonChanged.length;
    	for(int index = 0; index < count; index++){
    		if (buttonChanged[index] == false){
        		Holder.setView(backpack.buttonList[index]);
        		Holder.imageButton.setImageBitmap(null);
        		if (coloredCells){
        			Holder.imageButton.getBackground().clearColorFilter();
        			Holder.imageButton.setBackgroundResource(R.drawable.backpack_cell);
        		}
        		Holder.textCount.setVisibility(View.GONE);
        		Holder.textEquipped.setVisibility(View.GONE);
        		Holder.colorSplat.setVisibility(View.GONE);
        		Holder.imageButton.setTag(null);
        		
        		Holder.clear();
    		}
    		buttonChanged[index] = false;
    	}
    	
    	Log.i(Util.GetTag(), "Add data to view: " + (System.currentTimeMillis() - start) + " ms");
    }
}