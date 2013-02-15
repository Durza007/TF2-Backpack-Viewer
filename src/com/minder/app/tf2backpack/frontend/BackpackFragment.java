package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.Item;
import com.minder.app.tf2backpack.backend.PlayerItemListParser;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.frontend.BackpackView.OnLayoutReadyListener;

public class BackpackFragment extends Fragment {
	public static interface OnFullscreenClickListener {
		public void onFullscreenButtonClicked();
	}
	
	private final static int DEFAULT_NUMBER_OF_PAGES = 6;

	private WeakReference<OnFullscreenClickListener> listener;
	private boolean retainInstance;
	private BackpackView backpackView;
	private SteamUser currentSteamUser;
	private boolean addPlayerDataToView;
	private Button newButton;
	private Button fullscreenButton;
	private int onPageNumber;
	private int numberOfPages;
	private boolean checkUngivenItems;
	private TextView pageNumberText;
	private ProgressDialog myProgressDialog;
	private boolean dataDownloaded;
	private ArrayList<Item> playerItemList;
	private ArrayList<Item> ungivenList;
	
	private final Comparator<Item> comparator;
	
    /**
     * Create a new instance of BackpackFragment, initialized to
     * show backpack for 'user'.
     */
    public static BackpackFragment newInstance(SteamUser user) {
    	final BackpackFragment f = new BackpackFragment();
    	f.retainInstance = true;

        // Supply user input as an argument.
        Bundle args = new Bundle();
        args.putParcelable("user", user);
        f.setArguments(args);

        return f;
    }
    
    /**
     * Create a new instance of BackpackFragment, initialized to
     * show backpack for 'user' and scaled to fit on the x axis
     * @param retainInstance 
     */
    public static BackpackFragment newInstance(SteamUser user, int fixedWidth, boolean retainInstance) {
    	final BackpackFragment f = new BackpackFragment();
    	f.retainInstance = retainInstance;

        // Supply user input as an argument.
        Bundle args = new Bundle();
        args.putParcelable("user", user);
        args.putInt("fixedWidth", fixedWidth);
        f.setArguments(args);

        return f;
    }
    
    public BackpackFragment() {
		this.comparator = new BackpackPosComparator();
		this.numberOfPages = DEFAULT_NUMBER_OF_PAGES;
		this.retainInstance = true;
		this.dataDownloaded = false;
		
		this.playerItemList = new ArrayList<Item>();
		this.ungivenList = new ArrayList<Item>();
		
		listener = new WeakReference<OnFullscreenClickListener>(null);
    }
    
    public SteamUser getSteamUser() {
    	return currentSteamUser;
    }
    
    /**
     * Change SteamUser to display. Call this only after fragment is
     * up and running
     * @param user The user whose backpack to display
     */
    public void setSteamUser(SteamUser user) {
    	currentSteamUser = user;
    	
        downloadPlayerData();
    }
	
    private int getFixedWidth() {
    	return getArguments().getInt("fixedWidth", 0);
    }
    
    public void setFullscreenClickListener(OnFullscreenClickListener listener) {
    	this.listener = new WeakReference<OnFullscreenClickListener>(listener);
    	
    	if (fullscreenButton != null) {
	    	if (listener != null) {
	    		fullscreenButton.setVisibility(View.VISIBLE);
	    	} else {
	    		fullscreenButton.setVisibility(View.GONE);
	    	}
    	}
    }
    
    private void notifyFullscreenClickListener() {
    	OnFullscreenClickListener l = listener.get();
    	if (l != null)
    		l.onFullscreenButtonClicked();
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setRetainInstance(retainInstance);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.backpack, container, false);
		
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this.getActivity());
		final boolean coloredCells = sp.getBoolean("backpackrarity", true);

		// Create the backpack grid
		backpackView = (BackpackView) view.findViewById(R.id.TableLayoutBackPack);
		backpackView.setOnClickListener(onButtonBackpackClick);
		backpackView.setOnReadyListener(onLayoutReadyListener);
		backpackView.setColoredCells(coloredCells);
		
		final int fixedWidth = getFixedWidth();
		if (fixedWidth != 0)
			backpackView.setFixedWidth(fixedWidth);

		Button nextButton = (Button) view.findViewById(R.id.ButtonNext);
		nextButton.setOnClickListener(onButtonBackpackClick);

		Button previousButton = (Button) view.findViewById(R.id.ButtonPrevious);
		previousButton.setOnClickListener(onButtonBackpackClick);

		newButton = (Button) view.findViewById(R.id.buttonNew);
		newButton.setOnClickListener(onButtonBackpackClick);
		
		fullscreenButton = (Button) view.findViewById(R.id.buttonFullscreen);
		fullscreenButton.setOnClickListener(onFullscreenClickListener);
		if (listener.get() == null)
			fullscreenButton.setVisibility(View.GONE);

		onPageNumber = 1;
		pageNumberText = (TextView) view.findViewById(R.id.TextViewPageNumber);
		pageNumberText.setTypeface(Typeface.createFromAsset(getActivity()
				.getAssets(), "fonts/TF2Build.ttf"), 0);
		pageNumberText.setText(onPageNumber + "/" + numberOfPages);
		
		currentSteamUser = getArguments().getParcelable("user");
		if (!dataDownloaded)
			downloadPlayerData();
		else
			addPlayerDataToView();

		return view;
	}
	
    // listener used to make sure data is not added before view is ready for it
    private OnLayoutReadyListener onLayoutReadyListener = new OnLayoutReadyListener() {
		public void onReady() {
			if (addPlayerDataToView) {
				addPlayerDataToView = false;
	    		checkUngivenItems = true;
	    		addPlayerDataToView();
	    		checkUngivenItems = false;
			}
		}  	
    };
    
    private OnClickListener onFullscreenClickListener = new OnClickListener() {	
		public void onClick(View v) {
			notifyFullscreenClickListener();
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


		public void onPostExecute(Request request) {
			Object object = request.getData();
			
			dataDownloaded = true;
			if (object != null) {
				PlayerItemListParser pl = (PlayerItemListParser) object;

				// handle the request
				if (pl.getStatusCode() == 1) {
					playerItemList.clear();
					playerItemList.addAll(pl.getItemList());
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
					numberOfPages = DEFAULT_NUMBER_OF_PAGES;
					onPageNumber = 1;
					setPageNumberText();
					playerItemList.clear();
					addPlayerDataToView();
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
						weaponInfoIntent.putExtra("com.minder.app.tf2backpack.PlayerItemParser.Item", (Item)v.getTag());
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
    	App.getDataManager().requestPlayerItemList(asyncTasklistener, getSteamUser());
    }
    
    public void addPlayerDataToView(){
    	long start = System.currentTimeMillis();
    	
    	Log.d("NewBackpack", "AddPlayerDataToVIew");
    	
    	if (checkUngivenItems) {
    		ungivenList.clear();
    		
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
    	
    	if (startIndex == -1 || endIndex == -1) {
    		backpackView.setItems(Collections.<Item>emptyList(), 0);
    	} else {
    		backpackView.setItems(playerItemList.subList(startIndex, endIndex + 1), (onPageNumber - 1) * 50);
    	}

    	Log.i(Util.GetTag(), "Add data to view: " + (System.currentTimeMillis() - start) + " ms");
    }
    
    private static class BackpackPosComparator implements java.util.Comparator<Item> {
		public int compare(Item left, Item right) {
			return left.getBackpackPosition() - right.getBackpackPosition();
		}
    	
    }
}