package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;

public class DataManager {
	// Inner classes
	public static class Request {
		private final int type;
		private Exception exception;
		
		public Request(int type) {
			this.type = type;
		}
		
		public int getType() {
			return this.type;
		}
		
		public Exception getException() {
			return this.exception;
		}
	}
	
	// Members
	private final static int TYPE_PLAYER_NAME = 8;
	private final static int TYPE_PLAYER_ITEM_LIST = 10;
	
	private Context context;
	
	private DatabaseHandler databaseHandler;      
	private CacheManager cacheManager;
	
	private HashMap<Request, AsyncTask> asyncWorkList;;
	private ArrayList<Request> todoList;
	private ArrayList<Request> finishedWork;
	
	// Constructor
	public DataManager(Context context) {
		this.context = context;
		
		databaseHandler = new DatabaseHandler(context);
		cacheManager = new CacheManager(context);
		
		asyncWorkList = new HashMap<Request, AsyncTask>();
	}
	
	public DatabaseHandler getDatabaseHandler() {
		return this.databaseHandler;
	}
	
	public void requestPlayerItemList(AsyncTaskListener listener, SteamUser player) {
		Request request = new Request(TYPE_PLAYER_ITEM_LIST);
		GetPlayerItems asyncTask = new GetPlayerItems(listener, request);
		asyncWorkList.put(request, asyncTask);
		
		asyncTask.execute(player);
	}
	
	/*public Request requestPlayerName(Activity activity, OnRequestReadyListener listener, SteamUser player) {
		// fetch from our db cache if name is available
		String name = DataBaseHelper.getSteamUserName(databaseHandler.getReadableDatabase(), player.steamdId64);
		
		// check if name was available
		if (name.length() == 0) {
			// start download instead
			Request request = new Request(TYPE_PLAYER_NAME);
			
			synchronized (todoList) {
				todoList.add(request);
				todoList.notify();
			}
		}
		
		return null;
	}*/
	
	public boolean cancelRequest(Request request) {
		@SuppressWarnings("rawtypes")
		final AsyncTask task = asyncWorkList.remove(request);
		return task.cancel(true);
	}
	
	private void removeRequest(Request request) {
		asyncWorkList.remove(request);
	}
	
	/****************************
	 * 
	 * DATA FETCHING METHODS
	 * 
	 ****************************/
	private class GetPlayerItems extends AsyncTask<SteamUser, Void, PlayerItemListParser> {
		private final AsyncTaskListener listener;
		private final Request request;
		
		public GetPlayerItems(AsyncTaskListener listener, Request request) {
			this.listener = listener;
			this.request = request;
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
				return new PlayerItemListParser(data);
			} else {
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(PlayerItemListParser parser) {
			listener.onPostExecute(parser);
			removeRequest(request);
		}
	}
}
