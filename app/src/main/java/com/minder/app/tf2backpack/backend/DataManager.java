package com.minder.app.tf2backpack.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.minder.app.tf2backpack.ApiKey;
import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.AsyncTask;
import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.PersonaState;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;

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

	public static class SearchUserResult {
		public final int totalResults;
		public final ArrayList<SteamUser> users;

		public SearchUserResult(int totalResults, ArrayList<SteamUser> users) {
			this.totalResults = totalResults;
			this.users = users;
		}
	}


	public static final String PREF_NAME = "gamefiles";
	public static final String PREF_DOWNLOAD_VERSION = "download_version";
	public static final String PREF_DOWNLOAD_MODIFIED_DATE = "download_modified_date";
	public static final String PREF_DOWNLOAD_CHECKED_DATE = "download_checked_date";

	public static final long TIME_BETWEEN_GAMESCHEME_CHECKS_MS = 1000 * 60 * 60 * 12;
	
	public final static int PROGRESS_DOWNLOADING_SCHEMA_UPDATE = 1;
	public final static int PROGRESS_PARSING_SCHEMA = 2;
	public final static int PROGRESS_DOWNLOADING_IMAGES_UPDATE = 3;
	
	public final static int CURRENT_GAMESCHEMA_VERSION = 3;
	
	// Members
	private final static int TYPE_FRIEND_LIST = 7;
	private final static int TYPE_PLAYER_NAME = 8;
	private final static int TYPE_PLAYER_INFO = 9;
	private final static int TYPE_PLAYER_ITEM_LIST = 10;
	private final static int TYPE_SCHEMA_FILES = 11;
	private final static int TYPE_PLAYER_SEARCH = 12;
	private final static int TYPE_SCHEMA_OVERVIEW = 13;

	private static boolean gameSchemeChecked = false;
	private static boolean gameSchemeReady = false;
	private static boolean gameSchemeUpToDate = false;
	private static boolean downloadingGameScheme = false;
	private static float   gameSchemeDownloadProgress = 0.0f;
	private static Exception gameSchemeDownloadError = null;
	private static ArrayList<DownloadSchemaTask.ProgressListener> gameSchemeDownloadListeners
			= new ArrayList<DownloadSchemaTask.ProgressListener>();

	private static int currentGameSchemeVersion;
	
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

	public static boolean isGameSchemeReady() {
		if (!gameSchemeChecked) {
			getGameSchemeVersion();
			gameSchemeReady = currentGameSchemeVersion != -1;
			gameSchemeUpToDate = currentGameSchemeVersion == DataManager.CURRENT_GAMESCHEMA_VERSION;

			gameSchemeChecked = true;
		}

		return gameSchemeReady;
	}

	public static boolean isGameSchemeUpToDate() {
		if (!gameSchemeChecked) {
			isGameSchemeReady();
		}

		return gameSchemeUpToDate;
	}

	public static boolean shouldGameSchemeBeChecked() {
		SharedPreferences gamePrefs = App.getAppContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
		return (new Date().getTime() - gamePrefs.getLong(PREF_DOWNLOAD_CHECKED_DATE, -1)) > TIME_BETWEEN_GAMESCHEME_CHECKS_MS;
	}

	public static boolean isGameSchemeDownloading() {
		return downloadingGameScheme;
	}

	public static Exception getPendingGameSchemeException() {
		Exception error = gameSchemeDownloadError;
		gameSchemeDownloadError = null;
		return error;
	}

	public static void addGameSchemeDownloadListener(DownloadSchemaTask.ProgressListener listener) {
		gameSchemeDownloadListeners.add(listener);
		if (downloadingGameScheme) {
			listener.onProgress(gameSchemeDownloadProgress);
		}
	}

	public static void removeGameSchemeDownloadListener(DownloadSchemaTask.ProgressListener listener) {
		gameSchemeDownloadListeners.remove(listener);
	}

	private static void getGameSchemeVersion() {
		SharedPreferences gamePrefs = App.getAppContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
		currentGameSchemeVersion = gamePrefs.getInt(PREF_DOWNLOAD_VERSION, -1);
	}

	public static void saveGameSchemeDownloaded(long dataLastModified) {
		SharedPreferences gamePrefs = App.getAppContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);

		SharedPreferences.Editor editor = gamePrefs.edit();
		editor.putInt(PREF_DOWNLOAD_VERSION, DataManager.CURRENT_GAMESCHEMA_VERSION);
		editor.putLong(PREF_DOWNLOAD_MODIFIED_DATE, dataLastModified);
		editor.putLong(PREF_DOWNLOAD_CHECKED_DATE, new Date().getTime());
		editor.commit();

		downloadingGameScheme = false;
		gameSchemeReady = true;
		gameSchemeUpToDate = true;
		currentGameSchemeVersion = DataManager.CURRENT_GAMESCHEMA_VERSION;
	}

	public static void addPlayerNameToHistory(String name) {
		App.getDataManager()
				.getDatabaseHandler()
				.execSql("INSERT INTO name_history (name) \n" +
						"SELECT ? \n" +
						"WHERE NOT EXISTS(SELECT 1 FROM name_history WHERE name = ?)",
						new Object[] { name, name });
	}
	
	public Request requestPlayerItemList(AsyncTaskListener listener, SteamUser player) {
		Request request = new Request(TYPE_PLAYER_ITEM_LIST);
		GetPlayerItems asyncTask = new GetPlayerItems(this.context, listener, request);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, player);
		
		return request;
	}
	
	public Request requestFriendsList(AsyncTaskListener listener, SteamUser player) {
		Request request = new Request(TYPE_FRIEND_LIST);
		GetFriendListTask asyncTask = new GetFriendListTask(listener, request);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, player);
		
		return request;
	}

	public Request requestSteamUserInfo(AsyncTaskListener listener, List<SteamUser> players) {
		Request request = new Request(TYPE_PLAYER_INFO);
		GetPlayerInfo asyncTask = new GetPlayerInfo(listener, request);
		
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, players);
		
		return request;
	}

	public Request requestSchemaFilesDownload(boolean onlyIfChanged) {
		Request request = new Request(TYPE_SCHEMA_OVERVIEW);
		long lastModifiedTimestamp = -1;
		if (onlyIfChanged) {
			SharedPreferences gamePrefs = App.getAppContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
			lastModifiedTimestamp = gamePrefs.getLong(PREF_DOWNLOAD_MODIFIED_DATE, -1);
		}

		DownloadSchemaTask asyncTask = new DownloadSchemaTask(context, lastModifiedTimestamp, new DownloadSchemaTask.ProgressListener() {
			public void onProgress(float t) {
				DataManager.gameSchemeDownloadProgress = t;
				for (DownloadSchemaTask.ProgressListener l : gameSchemeDownloadListeners) {
					l.onProgress(t);
				}
			}
			public void onComplete(long dataLastModified) {
				DataManager.saveGameSchemeDownloaded(dataLastModified);
				for (DownloadSchemaTask.ProgressListener l : gameSchemeDownloadListeners) {
					l.onComplete(dataLastModified);
				}
				gameSchemeDownloadListeners.clear();
			}
			public void onError(Exception error) {
				DataManager.downloadingGameScheme = false;
				DataManager.gameSchemeReady = false;
				Log.e(Util.GetTag(), "Error while trying to download scheme files: " + error);
				if (gameSchemeDownloadListeners.size() == 0) {
					DataManager.gameSchemeDownloadError = error;
				}
				else {
					for (DownloadSchemaTask.ProgressListener l : gameSchemeDownloadListeners) {
						l.onError(error);
					}
				}
				gameSchemeDownloadListeners.clear();
			}
		});

		gameSchemeDownloadProgress = 0;
		downloadingGameScheme = true;
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

	private String sessionId;
	protected String getSessionId() {
		if (sessionId == null) {
			sessionId = UUID.randomUUID().toString().replaceAll("-", "");
			if (sessionId.length() > 16) {
				sessionId = sessionId.substring(0, 16);
			}
		}

		return sessionId;
	}
	
	/****************************
	 * 
	 * DATA FETCHING METHODS
	 * 
	 ****************************/
	private class GetPlayerItems extends AsyncTask<SteamUser, Void, PlayerItemListParser> {
		private final AsyncTaskListener listener;
		private final Request request;
		private final String cacheDir;
		
		public GetPlayerItems(Context context, AsyncTaskListener listener, Request request) {
			this.listener = listener;
			this.request = request;
			this.cacheDir = context.getCacheDir() + "/player_items/";
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
			String url = "http://api.steampowered.com/IEconItems_440/GetPlayerItems/v1/?key=" +
					ApiKey.get() + "&SteamID=" + steamId64;
			
			File tempCacheFile = new File(this.cacheDir + "temp");
			File cacheFile = new File(this.cacheDir + steamId64);

			if (BuildConfig.DEBUG) {
				Log.d("PlayerItemListParser", "GetPlayerInfo - " + url);
			}

			HttpConnection connection;
			try {
				connection = HttpConnection.string(url);
			}
			catch (MalformedURLException e) {
				request.exception = e;
				return null;
			}

			InputStream inputStream = connection.executeStream(null);
			
			if (inputStream == null) {
				request.exception = connection.getException();
				PlayerItemListParser parser = null;
				try {
					parser = new PlayerItemListParser(new FileInputStream(cacheFile));
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				return parser;
			}

			try {
				tempCacheFile.getParentFile().mkdirs();
				FileOutputStream out = new FileOutputStream(tempCacheFile);
				Util.CopyStream(inputStream, out);
				out.flush();
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			PlayerItemListParser parser = null;
			boolean couldParseNewData = true;
			try {
				parser = new PlayerItemListParser(new FileInputStream(tempCacheFile));
			} catch (IOException e1) {
				couldParseNewData = false;
				try {
					parser = new PlayerItemListParser(new FileInputStream(cacheFile));
				} catch (IOException e2) {
					e2.printStackTrace();
					request.exception = e1;
				}
			} finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (IOException e) {}
			}

			if (couldParseNewData) {
				if (!tempCacheFile.renameTo(cacheFile)) {
					Log.e(Util.GetTag(), "Could not mv temp cached player items file");
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
    
    private class DownloadSearchListTask extends AsyncTask<String, Void, SearchUserResult> {
		private class SearchResult {
			public int success;
			public String search_text;
			public int search_result_count;
			public String search_filter;
			public int search_page;
			public String html;
		}

		private final AsyncTaskListener listener;
		private final Request request;
		private final int pageNumber;
		
		public DownloadSearchListTask(AsyncTaskListener listener, Request request, int pageNumber) {
			this.listener = listener;
			this.request = request;
			this.pageNumber = pageNumber;
		}
		
		@Override
		protected void onPreExecute() {
			listener.onPreExecute();
		}

		@Override
		protected SearchUserResult doInBackground(String... params) {
			ArrayList<SteamUser> players = new ArrayList<SteamUser>();

			String sessionId = getSessionId();

			String data = downloadText("https://steamcommunity.com/search/SearchCommunityAjax?text="
					+ params[0] + "&filter=users&sessionid=" + sessionId + "&page=" + pageNumber, "sessionid=" + sessionId + ";");

			Gson gson = new Gson();
			SearchResult result = gson.fromJson(data, SearchResult.class);
			if (result == null || result.search_result_count == 0) {
				return new SearchUserResult(0, players);
			}

			final String id64Pattern = ".*/profiles/[\\d]*$";
			final String communityIdPattern = ".*/id/[^/]*$";

			Document doc = Jsoup.parse(result.html);
			Elements rows = doc.select(".search_row");
			for (Element e : rows) {
				SteamUser newPlayer = new SteamUser();
				Element link = e.select(".searchPersonaName").first();
				if (link == null) continue;

				newPlayer.steamName = link.text();
				String profileUrl = link.attr("href");

				if (profileUrl.matches(id64Pattern)) {
					int index  = profileUrl.lastIndexOf('/');
					newPlayer.steamdId64 = Long.parseLong(profileUrl.substring(index + 1));
				}
				else if (profileUrl.matches(communityIdPattern)) {
					int index  = profileUrl.lastIndexOf('/');
					newPlayer.communityId = profileUrl.substring(index + 1);
				}
				else {
					Log.d(Util.GetTag(), "Could not parse profile url: " + profileUrl);
					continue;
				}

				Element avatarDiv = e.select(".avatarMedium").first();
				if (avatarDiv != null) {
					Element img = avatarDiv.select("img").first();
					if (img != null) {
						newPlayer.avatarUrl = img.attr("src");
					}
				}

				// Log.d(Util.GetTag(), "Found player: " + newPlayer.steamName + ", avatar: " + newPlayer.avatarUrl);
				players.add(newPlayer);
			}

			return new SearchUserResult(result.search_result_count, players);
		}

		@Override
		protected void onPostExecute(SearchUserResult result) {
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

		private String downloadText(String urlString, String cookie) {
			int BUFFER_SIZE = 2000;
			InputStream in = null;
			URLConnection connection = null;
			try {

				URL url = new URL(urlString);
				connection = url.openConnection();

				if (!(connection instanceof HttpURLConnection))
					throw new IOException("Not an HTTP connection");

				try {
					HttpURLConnection httpConn = (HttpURLConnection) connection;
					httpConn.setAllowUserInteraction(false);
					httpConn.setInstanceFollowRedirects(true);
					httpConn.setRequestMethod("GET");
					httpConn.setRequestProperty("Cookie", cookie);
					httpConn.connect();

					int response = httpConn.getResponseCode();
					if (response == HttpURLConnection.HTTP_OK) {
						in = httpConn.getInputStream();
					}
				} catch (Exception ex) {
					throw new IOException("Error connecting");
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
						e.printStackTrace();
						return "";
					}
					return str;
				}
			} catch (IOException e) {

			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (connection != null && connection instanceof HttpURLConnection) ((HttpURLConnection) connection).disconnect();
			}
			return "";
		}
	}
}