package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;

public class DataManager implements Runnable {
	// Inner classes
	public static class Request {
		private final int type;
		private final Activity activity;
		private final OnRequestReadyListener readyListener;
		private boolean requestSucceded;
		private Exception exception;
		private Object[] args;
		public Object data;
		
		public Request(Activity activity, OnRequestReadyListener readyListener, int type, Object[] args) {
			this.type = type;
			this.activity = activity;
			this.readyListener = readyListener;
			this.args = args;
		}
		
		public int getType() {
			return this.type;
		}
		
		public boolean isRequestSuccess() {
			return this.requestSucceded;
		}
		
		public Exception getException() {
			return this.exception;
		}
	}
	
	// Members
	private final static int TYPE_PLAYER_NAME = 8;
	private final static int TYPE_PLAYER_ITEM_LIST = 10;
	
	private Context context;
	private Thread dataThread;
	
	private DatabaseHandler databaseHandler;      
	private CacheManager cacheManager;
	
	private ArrayList<Request> todoList;
	private ArrayList<Request> finishedWork;
	
	// Constructor
	public DataManager(Context context) {
		this.context = context;
		
		todoList = new ArrayList<Request>();
		finishedWork = new ArrayList<Request>();
		
		dataThread = new Thread(this);
		dataThread.setDaemon(true);
		dataThread.setName("DataManagerThread");
		dataThread.start();
		
		databaseHandler = new DatabaseHandler(context);
		cacheManager = new CacheManager(context);
	}
	
	public DatabaseHandler getDatabaseHandler() {
		return this.databaseHandler;
	}
	
	/**
	 * Create a request for getting a player item list
	 * @param activity The activity that wants to get the ready event
	 * @param listener The listener - leave null if you want to check result manually
	 * @param player The SteamUser whose itemlist will be downloaded
	 * @return A request object used for retrieving data when ready
	 */
	public Request requestPlayerItemList(Activity activity, OnRequestReadyListener listener, SteamUser player) {
		// start download
		Request request = new Request(activity, listener, TYPE_PLAYER_ITEM_LIST, new Object[] { player });
		
		synchronized (todoList) {
			todoList.add(0, request);
			todoList.notify();
		}
		
		return request;
	}
	
	public void requestPlayerItemList(AsyncTaskListener listener, SteamUser player) {
		GetPlayerItems asyncTask = new GetPlayerItems(listener);
		asyncTask.execute(player);
	}
	
	public Request requestPlayerName(Activity activity, OnRequestReadyListener listener, SteamUser player) {
		// fetch from our db cache if name is available
		String name = DataBaseHelper.getSteamUserName(databaseHandler.getReadableDatabase(), player.steamdId64);
		
		// check if name was available
		if (name.length() == 0) {
			// start download instead
			Request request = new Request(activity, listener, TYPE_PLAYER_NAME, new Object[] { player });
			
			synchronized (todoList) {
				todoList.add(request);
				todoList.notify();
			}
		}
		
		return null;
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
			if (request.readyListener != null) {
				EventMessenger em = new EventMessenger(request);
				request.activity.runOnUiThread(em);
			} else {
				synchronized (finishedWork) {
					finishedWork.add(request);
				}
			}
		}
	}
	
	private class GetPlayerItems extends AsyncTask<SteamUser, Void, PlayerItemListParser> {
		private AsyncTaskListener listener;
		
		public GetPlayerItems(AsyncTaskListener listener) {
			this.listener = listener;
		}
		
		@Override
		protected void onPreExecute() {
			listener.onPreExecute();
		}

		@Override
		protected PlayerItemListParser doInBackground(SteamUser... params) {
			long steamId64 = params[0].steamdId64;
			
			// try to fetch online first
			HttpConnection connection = new HttpConnection("http://api.steampowered.com/IEconItems_440/GetPlayerItems/v0001/?key=" + 
					Util.GetAPIKey() + "&SteamID=" + steamId64);
			
			String data = connection.execute();
			// fetch latest exception, if there is one
			//request.exception = connection.getException();

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
				return new PlayerItemListParser(data);
			} else {
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(PlayerItemListParser parser) {
			listener.onPostExecute(parser);
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
		// fetch latest exception, if there is one
		request.exception = connection.getException();

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
			request.requestSucceded = true;
		} else {
			request.requestSucceded = false;
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
			request.readyListener.onRequestReady(request);
		}		
	}
}
