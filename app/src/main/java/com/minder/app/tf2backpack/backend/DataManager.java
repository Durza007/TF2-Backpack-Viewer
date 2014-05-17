package com.minder.app.tf2backpack.backend;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.minder.app.tf2backpack.ApiKey;
import com.minder.app.tf2backpack.AsyncTask;
import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.PersonaState;
import com.minder.app.tf2backpack.SteamUser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataManager {
	// Inner classes
	public static class Request {
		private final int type;
		private boolean finished;
		Object data;
		Exception exception;
		
		public Request(int type) {
			this.type = type;
		}
		
		public boolean isFinished() {
			return this.finished;
		}
		
		public Object getData() {
			return this.data;
		}
		
		private void setData(Object data) {
			this.data = data;
			finished = true;
		}
		
		public int getType() {
			return this.type;
		}
		
		public Exception getException() {
			return this.exception;
		}
	}
	
	public final static int PROGRESS_DOWNLOADING_SCHEMA_UPDATE = 1;
	public final static int PROGRESS_PARSING_SCHEMA = 2;
	public final static int PROGRESS_DOWNLOADING_IMAGES_UPDATE = 3;
	
	public final static int CURRENT_GAMESCHEMA_VERSION = 2;
	
	// Members
	private final static int TYPE_FRIEND_LIST = 7;
	private final static int TYPE_PLAYER_NAME = 8;
	private final static int TYPE_PLAYER_INFO = 9;
	private final static int TYPE_PLAYER_ITEM_LIST = 10;
	private final static int TYPE_SCHEMA_FILES = 11;
	private final static int TYPE_PLAYER_SEARCH = 12;
	
	Context context;
	
	private DatabaseHandler databaseHandler;      
	private CacheManager cacheManager;
	
	@SuppressWarnings("rawtypes")
	private HashMap<Request, AsyncTask> asyncWorkList;
	
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
	
	public Request requestPlayerItemList(AsyncTaskListener listener, SteamUser player) {
		Request request = new Request(TYPE_PLAYER_ITEM_LIST);
		GetPlayerItems asyncTask = new GetPlayerItems(listener, request);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, player);
		
		return request;
	}
	
	public Request requestFriendsList(AsyncTaskListener listener, SteamUser player) {
		Request request = new Request(TYPE_FRIEND_LIST);
		GetFriendListTask asyncTask = new GetFriendListTask(listener, request);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, player);
		
		return request;
	}
	
	@SuppressWarnings("unchecked")
	public Request requestSteamUserInfo(AsyncTaskListener listener, List<SteamUser> players) {
		Request request = new Request(TYPE_PLAYER_INFO);
		GetPlayerInfo asyncTask = new GetPlayerInfo(listener, request);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, players);
		
		return request;
	}
	
	public Request requestSchemaFilesDownload(AsyncTaskListener listener, boolean refreshImages, boolean downloadHighresImages) {
		Request request = new Request(TYPE_SCHEMA_FILES);	
		DownloadSchemaFilesTask asyncTask = new DownloadSchemaFilesTask(context, listener, request, refreshImages, downloadHighresImages);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		
		return request;
	}
	
	public Request requestSteamUserSearch(AsyncTaskListener listener, String searchTerm, int pageNumber) {
		Request request = new Request(TYPE_PLAYER_SEARCH);
		DownloadSearchListTask asyncTask = new DownloadSearchListTask(listener, request, pageNumber);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, searchTerm);
		
		return request;
	}
	
	public Request requestVerifyPlayer(AsyncTaskListener listener, String id) {
		Request request = new Request(TYPE_PLAYER_SEARCH);
		VerifyPlayerTask asyncTask = new VerifyPlayerTask(listener, request);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
		
		return request;
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
		if (task == null)
			return false;
		return task.cancel(true);
	}
	
	protected void removeRequest(Request request) {
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
			asyncWorkList.put(request, this);
			listener.onPreExecute();
		}

		@Override
		protected PlayerItemListParser doInBackground(SteamUser... params) {
			long steamId64 = params[0].steamdId64;
			
			// try to fetch online first
			HttpConnection connection = HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetPlayerItems/v0001/?key=" + 
					ApiKey.get() + "&SteamID=" + steamId64);
			
			InputStream inputStream = connection.executeStream(null);
			
			if (inputStream == null) {
				request.exception = connection.getException();
				return null;
			}
			
			PlayerItemListParser parser = null;
			try {
				parser = new PlayerItemListParser(inputStream);
			} catch (IOException e) {
				request.exception = e;
			} finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (IOException e) {
				}
			}
			
			return parser;
		}
		
		@Override
		protected void onPostExecute(PlayerItemListParser parser) {
			request.setData(parser);
			listener.onPostExecute(request);
			removeRequest(request);
		}
	}
	
    private class GetFriendListTask extends AsyncTask<SteamUser, Void, ArrayList<SteamUser>> {
		private final AsyncTaskListener listener;
		private final Request request;
		
		public GetFriendListTask(AsyncTaskListener listener, Request request) {
			this.listener = listener;
			this.request = request;
		}
    	
    	protected void onPreExecute() {
    		asyncWorkList.put(request, this);
    		listener.onPreExecute();
    	}
    	
		@Override
		protected ArrayList<SteamUser> doInBackground(SteamUser... params) {	
	        XmlPullParserFactory pullMaker;
	        InputStream fis = null;
	        
        	ArrayList<SteamUser> players = new ArrayList<SteamUser>();
        	
            SQLiteDatabase sqlDb = databaseHandler.getReadableDatabase();
	        try {
	        	//String xml = (String) new HttpConnection().getDirect("http://steamcommunity.com/profiles/" + params[0] + "/friends/?xml=1", 86400);
                URL url = new URL("http://api.steampowered.com/ISteamUser/GetFriendList/v1/?key=" + ApiKey.get() +
                		"&steamid=" + params[0].steamdId64 + "&relationship=all&format=xml");
	        	
	            pullMaker = XmlPullParserFactory.newInstance();

	            XmlPullParser parser = pullMaker.newPullParser();
	            fis = url.openStream();

	            parser.setInput(fis, null);

	            boolean friendslist = false;
	            boolean friends = true;
	        	boolean friend = false;
	        	boolean steamid = false;
	        	
	        	SteamUser newPlayer;

	            int eventType = parser.getEventType();
	            while (eventType != XmlPullParser.END_DOCUMENT) {
	                switch (eventType) {
	                case XmlPullParser.START_DOCUMENT:
	                    break;
	                case XmlPullParser.START_TAG:
	                    if (parser.getName().equals("friendslist")) {
	                    	friendslist = true;
	                    } else if (parser.getName().equals("friends")) {
	                    	friends = true;
	                    } else if (parser.getName().equals("friend")) {
	                    	friend = true;
	                    } else if (parser.getName().equals("steamid")) {
	                    	steamid = true;
	                    }
	                    break;
	                case XmlPullParser.END_TAG:
	                    if (parser.getName().equals("friendslist")) {
	                    	friendslist = false;
	                    } else if (parser.getName().equals("friends")) {
	                    	friends = false;
	                    } else if (parser.getName().equals("friend")) {
	                    	friend = false;
	                    } else if (parser.getName().equals("steamid")) {
	                    	steamid = false;
	                    }
	                    break;
	                case XmlPullParser.TEXT:
	                    if (friendslist && friends && friend && steamid) {
	                    	newPlayer = new SteamUser();
	                    	newPlayer.steamdId64 = Long.parseLong(parser.getText());
	                    	newPlayer.steamName = DataBaseHelper.getSteamUserName(sqlDb, newPlayer.steamdId64);
	                    	players.add(newPlayer);
	                    	//GetPlayerName(newPlayer.steamdId64);
	                    }
	                    break;

	                }
	                eventType = parser.next();
	            }
	        } catch (UnknownHostException e) {
	            Log.e("xml_perf", "Pull parser failed", e);
	            request.exception = e;
	            players = null;
	        } catch (XmlPullParserException e) {
	        	request.exception = e;
	        	players = null;
				e.printStackTrace();
			} catch (IOException e) {
				request.exception = e;
				players = null;
				e.printStackTrace();
			}
			
			return players;
		}
    	
        protected void onPostExecute(ArrayList<SteamUser> result) {
        	request.setData(result);
        	listener.onPostExecute(request);
        	removeRequest(request);
        }
    }
    
    private class GetPlayerInfo extends AsyncTask<List<SteamUser>, SteamUser[], Void> {
    	private final static int ID_CHUNK_SIZE = 50;
    	private final static int PUBLISH_BUFFER_SIZE = 5;
		private final AsyncTaskListener listener;
		private final Request request;
		
		public GetPlayerInfo(AsyncTaskListener listener, Request request) {
			this.listener = listener;
			this.request = request;
		}
    	
    	protected void onPreExecute() {
    		asyncWorkList.put(request, this);
    		listener.onPreExecute();
    	}
    	
		@Override
		protected Void doInBackground(final List<SteamUser>... params) {
			if (BuildConfig.DEBUG) {
				Log.d("DataManager", "GetPlayerInfo - start");
			}
			// used for performance timing
			long start = System.currentTimeMillis();
				
	        XmlPullParserFactory pullMaker;
	        XmlPullParser parser;
	        StringBuilder sb = new StringBuilder();
	        
	        final int length = params[0].size();
	        int index = 0;
	        
        	SteamUser[] playersToPublish = new SteamUser[PUBLISH_BUFFER_SIZE];
        	int currentPublishIndex = 0;
        	
            try {
				pullMaker = XmlPullParserFactory.newInstance();
				
				parser = pullMaker.newPullParser();
			} catch (XmlPullParserException e1) {
				e1.printStackTrace();
				return null;
			}
			
	        
	        while (index < length) {       
	        	for (int stringIndex = 0; stringIndex < ID_CHUNK_SIZE; stringIndex++) {
	        		if (stringIndex + index >= length) break;
	        		
	        		sb.append(params[0].get(stringIndex + index).steamdId64);
	        		sb.append(",");
	        	}
	        	
	        	// removes the last comma
	        	sb.deleteCharAt(sb.length() - 1);
	        	
		        try {
		        	//String xml = (String) new HttpConnection().getDirect("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + ApiKey.get() + "&steamids=" + player.steamdId64 + "&format=xml", 0);
		        	URL url = new URL("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + ApiKey.get() + "&format=xml&steamids=" + sb.toString());
	
		            InputStream fis = url.openStream();
	
		            parser.setInput(fis, null);
	
		            boolean steamid = false;
		            boolean playerTag = false;
		        	boolean personaName = false;
		        	boolean personaState = false;
		        	boolean avatar = false;
		        	boolean gameId = false;
		        	
		        	SteamUser player = null;
	
		            int eventType = parser.getEventType();
		            while (eventType != XmlPullParser.END_DOCUMENT) {
		                switch (eventType) {
		                case XmlPullParser.START_DOCUMENT:
		                    break;
		                case XmlPullParser.START_TAG:
		                	if (parser.getName().equals("player")) {
		                		playerTag = true;
		                		index++;
		                	} else if (parser.getName().equals("steamid")) {
		                		steamid = true;
		                	} else if (parser.getName().equals("personaname")) {
		                    	personaName = true;
		                    } else if (parser.getName().equals("personastate")) {
		                    	personaState = true;
		                    } else if (parser.getName().equals("avatarmedium")) {
		                    	avatar = true;
		                    } else if (parser.getName().equals("gameid")) {
		                    	gameId = true;
		                    }
		                    break;
		                case XmlPullParser.END_TAG:
		                	if (parser.getName().equals("player")) {
		                		playerTag = false;
		                		
		                		playersToPublish[currentPublishIndex] = player;
		                		currentPublishIndex++;
		                		
		                		// publish if our buffer is filled
		                		if (currentPublishIndex == playersToPublish.length) {
		                			currentPublishIndex = 0;
		                			publishProgress(playersToPublish);
		                			
		                			// clear buffer
		                			for (int i = 0; i < playersToPublish.length; i++) {
		                				playersToPublish[i] = null;
		                			}
		                		} // or publish if its the last user
		                		else if (index >= length - 1) {
		                			publishProgress(playersToPublish);
		                		}
		        		        
		        		        // for safety - if something goes wrong and we start writing
		        		        // to the wrong object then we will get a nullpointer exception
		        		        // instead
		        		        player = null;
		                	} else if (parser.getName().equals("steamid")) {
		                		steamid = false;
		                	} else if (parser.getName().equals("personaname")) {
		                    	personaName = false;
		                    } else if (parser.getName().equals("personastate")) {
		                    	personaState = false;
		                    } else if (parser.getName().equals("avatarmedium")) {
		                    	avatar = false;
		                    } else if (parser.getName().equals("gameid")) {
		                    	gameId = false;
		                    }
		                    break;
		                case XmlPullParser.TEXT:
		                	if (playerTag) {
		                		if (steamid) {
		                			long steamId64 = Long.parseLong(parser.getText());
		                			
		                			// need to find the right player - SLOW
		                			// TODO come up with better solution to this
		                			for (SteamUser s : params[0]) {
		                				if (steamId64 == s.steamdId64) {
		                					player = s;
		                					break;
		                				}
 		                			}
		                			
		                		} else if (personaName) {
		                			// TODO find out why this can be null
			                    	player.steamName = parser.getText();
			                    	DataBaseHelper.cacheSteamUserName(player.steamdId64, player.steamName);
			                    } else if (personaState) {
			                    	int state = Integer.parseInt(parser.getText());
			                    	
			                    	// check for handling future persona states that might be added
			                    	if (state < PersonaState.values().length) {
			                    		player.personaState = PersonaState.values()[state];
			                    	} else {
			                    		player.personaState = PersonaState.Online;
			                    	}
			                    } else if (avatar) {
			                    	player.avatarUrl = parser.getText();
			                    } else if (gameId) {
			                    	player.gameId = parser.getText();
			                    }
		                	}
		                    break;
	
		                }
		                eventType = parser.next();
		            }
		        } catch (XmlPullParserException e) {
		        	e.printStackTrace();
		        } catch (IOException e) {
					e.printStackTrace();
				}
	        	sb.delete(0, sb.length());
	        }
	        
	        if (BuildConfig.DEBUG) {
	        	Log.d("DataManager", "GetPlayerInfo - end: " + (System.currentTimeMillis() - start) + " ms");
	        }
			return null;
		}
		
		@Override
		protected void onProgressUpdate(SteamUser[]... users) {
			listener.onProgressUpdate(new ProgressUpdate(users[0]));
		}
		
		@Override
		protected void onPostExecute(Void voids) {
			listener.onPostExecute(null);
			asyncWorkList.remove(request);
		}
    }
    
    private class DownloadSearchListTask extends AsyncTask<String, Void, ArrayList<SteamUser>> {
		private final AsyncTaskListener listener;
		private final Request request;
		private final int pageNumber;
		
		public DownloadSearchListTask(AsyncTaskListener listener, Request request,int pageNumber) {
			this.listener = listener;
			this.request = request;
			this.pageNumber = pageNumber;
		}
		
		@Override
		protected void onPreExecute() {
			listener.onPreExecute();
		}

		@Override
		protected ArrayList<SteamUser> doInBackground(String... params) {
			ArrayList<SteamUser> players = new ArrayList<SteamUser>();

			String data = downloadText("http://steamcommunity.com/actions/Search?p="
					+ pageNumber + "&T=Account&K=\"" + params[0] + "\"");

			int curIndex = 0;
			SteamUser newPlayer;
			while (curIndex < data.length()) {
				int index = data
						.indexOf(
								"<a class=\"linkTitle\" href=\"http://steamcommunity.com/profiles/",
								curIndex);
				
				int avatarIndex = data.indexOf("<img src=\"http://media.steampowered.com/steamcommunity/public/images/avatars/", curIndex);
				if (index != -1) {
					int endIndex = data.indexOf("\">", index);
					newPlayer = new SteamUser();
					newPlayer.steamdId64 = Long.parseLong(data.substring(
							index + 62, endIndex));

					index = data.indexOf("</a>", endIndex);
					newPlayer.steamName = data.substring(endIndex + 2, index);

					players.add(newPlayer);
					curIndex = index;
				} else {
					index = data
							.indexOf(
									"<a class=\"linkTitle\" href=\"http://steamcommunity.com/id/",
									curIndex);
					if (index != -1) {
						int endIndex = data.indexOf("\">", index);
						newPlayer = new SteamUser();
						newPlayer.communityId = data.substring(index + 56,
								endIndex);

						index = data.indexOf("</a>", endIndex);
						newPlayer.steamName = data.substring(endIndex + 2,
								index);

						players.add(newPlayer);
						curIndex = index;
					} else {
						break;
					}
				}
				
				// get avatar
				int endIndex = data.indexOf("\" />", avatarIndex);
				newPlayer.avatarUrl = data.substring(avatarIndex + 10, endIndex);
			}

			return players;
		}

		@Override
		protected void onPostExecute(ArrayList<SteamUser> result) {
			if (result != null) {
				/*
				 * currentRequest = App.getDataManager().requestSteamUserInfo(
				 * getSteamUserInfoListener, ((ArrayList<SteamUser>) result));
				 */
				request.data = result;
				listener.onPostExecute(request);
				removeRequest(request);
			}
		}

		private String downloadText(String URL) {
			int BUFFER_SIZE = 2000;
			InputStream in = null;
			try {
				in = openHttpConnection(URL);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return "";
			}

			if (in != null) {
				InputStreamReader isr = new InputStreamReader(in);
				int charRead;
				String str = "";
				char[] inputBuffer = new char[BUFFER_SIZE];
				try {
					while ((charRead = isr.read(inputBuffer)) > 0) {
						// ---convert the chars to a String---
						String readString = String.copyValueOf(inputBuffer, 0,
								charRead);
						str += readString;
						inputBuffer = new char[BUFFER_SIZE];
					}
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}
				return str;
			}
			return "";
		}

		private InputStream openHttpConnection(String urlString)
				throws IOException {
			InputStream in = null;
			int response = -1;

			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();

			if (!(conn instanceof HttpURLConnection))
				throw new IOException("Not an HTTP connection");

			try {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				httpConn.setAllowUserInteraction(false);
				httpConn.setInstanceFollowRedirects(true);
				httpConn.setRequestMethod("GET");
				httpConn.connect();

				response = httpConn.getResponseCode();
				if (response == HttpURLConnection.HTTP_OK) {
					in = httpConn.getInputStream();
				}
			} catch (Exception ex) {
				throw new IOException("Error connecting");
			}
			return in;
		}
	}
}