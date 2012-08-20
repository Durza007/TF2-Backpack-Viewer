package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;

import android.app.Activity;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;

public class DataManager implements Runnable {
	// Inner classes
	public static class Request {
		private final int type;
		private final Activity activity;
		private Object[] args;
		public Object data;
		
		public Request(Activity activity, int type, Object[] args) {
			this.type = type;
			this.activity = activity;
			this.args = args;
		}
		
		public int getType() {
			return this.type;
		}
	}
	
	public static interface OnDataReadyListener {
		public void OnDataReady(Request request);
	}
	
	// Members
	private final static int TYPE_PLAYER_ITEM_LIST = 10;
	
	private Thread dataThread;
	
	private CacheManager cacheManager;
	
	private ArrayList<Request> todoList;
	private ArrayList<Request> finishedWork;
	
	// Constructor
	public DataManager() {	
		todoList = new ArrayList<Request>();
		finishedWork = new ArrayList<Request>();
		
		dataThread = new Thread(this);
		dataThread.setDaemon(true);
		dataThread.setName("DataThread");
		dataThread.start();
		
		cacheManager = new CacheManager(App.getAppContext());
	}
	
	/**
	 * Create a request for getting a player item list
	 * @param player
	 * @return
	 */
	public Request requestPlayerItemList(Activity activity, SteamUser player) {
		// start download
		Request request = new Request(activity, TYPE_PLAYER_ITEM_LIST, new Object[] { player });
		
		synchronized (todoList) {
			todoList.add(0, request);
			todoList.notify();
		}
		
		return request;
	}
	
	public boolean isRequestAvail(Request request) {
		return finishedWork.contains(request);
	}
	
	public Object getRequestedData(Request request) {
		if (finishedWork.remove(request)) {
			return request.data;
		} else {
			throw new RuntimeException("Data not available");
		}
	}
	
	/****************************
	 * 
	 * DATA FETCHING METHODS
	 * 
	 ****************************/
	
	// The Data worker method
	public void run() {
		// TODO should maybe have a variable here
		while (true) {
			Request request = null;
			
			synchronized (todoList) {
	            while (todoList.isEmpty())
	            {
					try {
						todoList.wait();
					} catch (InterruptedException e) {
						// its ok
					}
	            }
	            // work available!
	            request = todoList.remove(0);
	            todoList.notify();
			}
			
			switch (request.getType()) {
				case TYPE_PLAYER_ITEM_LIST:
					request.data = getPlayerItemList((SteamUser) request.args[0]);
					break;
				default:
					throw new RuntimeException("Unkown type");
			}
		}
	}
	
	/**
	 * Get player item list from cache or Internet
	 * @return The player item list
	 */
	private ArrayList<Item> getPlayerItemList(SteamUser user) {
		// try to fetch online first
		HttpConnection connection = new HttpConnection("http://api.steampowered.com/IEconItems_440/GetPlayerItems/v0001/?key=" + 
				Util.GetAPIKey() + "&SteamID=" + user.steamdId64);
		
		String data = connection.execute();
	
		if (data == null) {
			// if we could not get it from internet - check if we have it cached
			data = cacheManager.getString("itemlist", Long.toString(user.steamdId64));
		} else {
			cacheManager.cacheString("itemlist", Long.toString(user.steamdId64), data);
		}
		
		// Parse data if available
		if (data != null) {
			PlayerItemListParser parser = new PlayerItemListParser(data);
			return parser.GetItemList();
		}
		
		return null;
	}
}
