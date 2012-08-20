package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;

import android.app.Activity;
import android.util.Log;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;

public class DataManager implements Runnable {
	// Inner classes
	public static class Request {
		private final int type;
		private final Activity activity;
		private final OnDataReadyListener listener;
		private int resultCode;
		private Object[] args;
		public Object data;
		
		public Request(Activity activity, OnDataReadyListener listener, int type, Object[] args) {
			this.type = type;
			this.activity = activity;
			this.listener = listener;
			this.args = args;
		}
		
		public int getType() {
			return this.type;
		}
		
		public int getResultCode() {
			return this.resultCode;
		}
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
	public Request requestPlayerItemList(Activity activity, OnDataReadyListener listener, SteamUser player) {
		// start download
		Request request = new Request(activity, listener, TYPE_PLAYER_ITEM_LIST, new Object[] { player });
		
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
			
			// handle the request
			switch (request.getType()) {
				case TYPE_PLAYER_ITEM_LIST:
					request = getPlayerItemList(request);
					break;
				default:
					throw new RuntimeException("Unknown type");
			}
			
			// post results
			if (request.listener != null) {
				EventMessenger em = new EventMessenger(request);
				request.activity.runOnUiThread(em);
			} else {
				synchronized (finishedWork) {
					finishedWork.add(request);
				}
			}
		}
	}
	
	/**
	 * Get player item list from cache or Internet
	 * @return The player item list
	 */
	private Request getPlayerItemList(Request request) {
		long steamId64 = ((SteamUser)request.args[0]).steamdId64;
		
		// try to fetch online first
		HttpConnection connection = new HttpConnection("http://api.steampowered.com/IEconItems_440/GetPlayerItems/v0001/?key=" + 
				Util.GetAPIKey() + "&SteamID=" + steamId64);
		
		String data = connection.execute();
		data = null;
		if (data == null) {
			if (BuildConfig.DEBUG)
				Log.d("DataManager", "loading item list from cache");
			// if we could not get it from Internet - check if we have it cached
			data = cacheManager.getString("itemlist", Long.toString(steamId64));
		} else {
			if (BuildConfig.DEBUG)
				Log.d("DataManager", "loading item list from internet");
			cacheManager.cacheString("itemlist", Long.toString(steamId64), data);
		}
		
		// Parse data if available
		if (data != null) {			
			request.data = new PlayerItemListParser(data);
		}
		
		return request;
	}
	
	/**
	 * Used for sending an event to the activity on it's UI thread
	 */
	private static class EventMessenger implements Runnable {
		private final Request request;
		
		public EventMessenger(Request request) {
			this.request = request;
		}

		public void run() {
			request.listener.onDataReady(request);
		}		
	}
}
