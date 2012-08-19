package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;

import com.minder.app.tf2backpack.SteamUser;

public class DataManager {
	public static class Request {
		private int type;
		public Object data;
		
		public Request(int type) {
			this.type = type;
		}
		
		public int getType() {
			return this.type;
		}
	}
	
	private final static int TYPE_PLAYER_ITEM_LIST = 10;
	
	private ArrayList<Request> todoList;
	private ArrayList<Request> finishedWork;
	
	public DataManager() {	
		
	}

	public Request requestPlayerItemList(SteamUser player) {
		// start download
		
		final Request request = new Request(TYPE_PLAYER_ITEM_LIST);
		
		return request;
	}
	
	public boolean isRequestAvail(Request request) {
		return finishedWork.contains(request);
	}
	
	/**
	 * Get the requested data - will wait until data is available
	 * @param request The request corresponding to the data requested
	 * @return The list of items
	 */
	public ArrayList<Item> getPlayerItemList(Request request) {
		return (ArrayList<Item>) request.data;
	}
}
