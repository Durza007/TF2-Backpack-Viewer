package com.minder.app.tf2backpack.backend;

import java.io.File;
import java.io.FileOutputStream;
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.minder.app.tf2backpack.AsyncTask;
import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.PersonaState;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.GameSchemeParser.ImageInfo;
import com.minder.app.tf2backpack.backend.HttpConnection.DownloadProgressListener;

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
	
	public final static int CURRENT_GAMESCHEMA_VERSION = 1;
	
	// Members
	private final static int TYPE_FRIEND_LIST = 7;
	private final static int TYPE_PLAYER_NAME = 8;
	private final static int TYPE_PLAYER_INFO = 9;
	private final static int TYPE_PLAYER_ITEM_LIST = 10;
	private final static int TYPE_SCHEMA_FILES = 11;
	private final static int TYPE_PLAYER_SEARCH = 12;
	
	private Context context;
	
	private DatabaseHandler databaseHandler;      
	private CacheManager cacheManager;
	
	@SuppressWarnings("rawtypes")
	private HashMap<Request, AsyncTask> asyncWorkList;;
	
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
		DownloadSchemaFiles asyncTask = new DownloadSchemaFiles(listener, request, refreshImages, downloadHighresImages);
		
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
					Util.GetAPIKey() + "&SteamID=" + steamId64);
			
			String data = (String) connection.execute(null);
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
                URL url = new URL("http://api.steampowered.com/ISteamUser/GetFriendList/v1/?key=" + Util.GetAPIKey() + 
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
		        	//String xml = (String) new HttpConnection().getDirect("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=***REMOVED***&steamids=" + player.steamdId64 + "&format=xml", 0);
		        	URL url = new URL("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=***REMOVED***&format=xml&steamids=" + sb.toString());
	
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
    
    /**
     * This behemoth downloads all the tf2 schema files
     */
    private class DownloadSchemaFiles extends AsyncTask<Void, ProgressUpdate, Void> {
    	private final static int NUMBER_OF_IMAGE_THREADS = 4;
    	private final static int VALUE_DIFF_FOR_PROGRESS_UPDATE = 7;
    	
		private final AsyncTaskListener listener;
		private final Request request;
		private final boolean refreshImages;
		private final boolean highresImages;
		
		private final Object imageListLock = new Object();
		private ArrayList<ImageInfo> imageUrlList;
		
		private final Object resultLock = new Object();
		private int downloadedImages;
		private int valueSinceLastProgressUpdate;
		private int finishedThreads;
		
		private Bitmap paintColor;
		private Bitmap teamPaintRed;
		private Bitmap teamPaintBlue;
		
		public DownloadSchemaFiles(AsyncTaskListener listener, Request request, boolean refreshImages, boolean downloadHighresImages) {
			this.listener = listener;
			this.request = request;
			this.refreshImages = refreshImages;
			this.highresImages = downloadHighresImages;
		}
		
		@Override
		protected void onPreExecute() {
			listener.onPreExecute();
		}
    	
		@Override
		protected Void doInBackground(Void... params) {
			if (refreshImages)
				deleteItemImages();
			
			HttpConnection connection = 
				HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetSchema/v0001/?key=" + 
					Util.GetAPIKey() + "&format=json&language=en");
			
			String data = (String) connection.execute(new DownloadProgressListener() {
				private int totalSize;
				public void totalSize(long totalSize) {
					this.totalSize = (int)totalSize;
					publishProgress(new ProgressUpdate(PROGRESS_DOWNLOADING_SCHEMA_UPDATE, this.totalSize, 0));
				}
				
				public void progressUpdate(long currentSize) {
					publishProgress(new ProgressUpdate(PROGRESS_DOWNLOADING_SCHEMA_UPDATE, totalSize, (int)currentSize));
				}
			});
			
			if (data != null) {
				publishProgress(new ProgressUpdate(PROGRESS_PARSING_SCHEMA, 0, 0));
				
				GameSchemeParser gs = 
					new GameSchemeParser(data, context, highresImages);
				// set to null as soon as possible since it is holding >2 MB
				data = null;
				
				if (gs.error != null){
					// TODO handle error
				}
				
				// download images
				if (gs.getImageURList() != null){
			    	imageUrlList = gs.getImageURList();
			    	// set this to null since it as pretty big object
			    	gs = null;
			    	
			    	final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			    	bitmapOptions.inScaled = false;
			    	paintColor = BitmapFactory.decodeResource(context.getResources(), R.drawable.paintcan_paintcolor, bitmapOptions);
			    	teamPaintRed = BitmapFactory.decodeResource(context.getResources(), R.drawable.teampaint_red_mask, bitmapOptions);
			    	teamPaintBlue = BitmapFactory.decodeResource(context.getResources(), R.drawable.teampaint_blu_mask, bitmapOptions);
					
			    	long start = System.nanoTime();
	                for (int index = 0; index < imageUrlList.size(); index++){
                        final File file = new File(context.getFilesDir().getPath() + "/" + imageUrlList.get(index).getDefIndex() + ".png");
                        if (file.exists()){
                        	imageUrlList.remove(index);
                        	index--;
                        }
	                }
	                int totalDownloads = imageUrlList.size();
	                Log.d("DataManager", "File image check: " + (System.nanoTime() - start) / 1000000 + " ms");
	                
			    	publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_IMAGES_UPDATE, totalDownloads, 0));
			    	
			    	// start up some download threads
			    	for (int index = 0; index < NUMBER_OF_IMAGE_THREADS; index++) {
			    		ImageDownloader downloader = new ImageDownloader(index);
			    		Thread thread = new Thread(downloader);
			    		thread.setName("ImageDownloadThread #" + index);
			    		thread.start();
			    	}
			    	
			    	// wait for download threads to finish
			    	while (true) {
			    		synchronized (resultLock) {
			    			valueSinceLastProgressUpdate = downloadedImages;
			    			publishProgress(new ProgressUpdate(
			    					DataManager.PROGRESS_DOWNLOADING_IMAGES_UPDATE, 
			    					totalDownloads, 
			    					downloadedImages));
			    			
			    			if (finishedThreads == NUMBER_OF_IMAGE_THREADS) {
			    				break;
			    			} else {
			    				try {
									resultLock.wait();
								} catch (InterruptedException e) {
									// Doesn't matter
								}
			    			}
			    		}
			    	}
			    	
			    	paintColor.recycle();
			    	teamPaintBlue.recycle();
			    	teamPaintRed.recycle();
				} else {
					// handle error
				}
				gs = null;
				System.gc();
				Log.d("Dashboard", "GameScheme download complete");
			} else {
				// handle error
				request.exception = connection.getException();
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(ProgressUpdate... progress) {
			listener.onProgressUpdate(progress[0]);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			listener.onPostExecute(request);
			removeRequest(request);
		}
		
	    private class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	        public void uncaughtException(Thread thread, Throwable ex) {
	            Log.e("UncaughtException", "Got an uncaught exception: " + ex.toString());
	            if(ex.getClass().equals(OutOfMemoryError.class))
	            {
	                try {
	                    android.os.Debug.dumpHprofData("/sdcard/dump.hprof");
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                System.exit(1337);
	            }
	            ex.printStackTrace();
	        }
	    }

		
	    /**
	     * Deletes all item images
	     */
	    private void deleteItemImages() {
			File file = new File(context.getFilesDir().getPath());
			if (file.isDirectory()) {
		        String[] children = file.list();
		        for (int i = 0; i < children.length; i++) {
		            new File(file, children[i]).delete();
		        }
		    }

	    }
		
		private class ImageDownloader implements Runnable {
			private int index;
			
			public ImageDownloader(int index) {
				this.index = index;
			}
			
			public void run() {
				// TODO probably remove this? can't remember why it is here
				// Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
				while (true) {
					ImageInfo imageInfo = null;
					synchronized (imageListLock) {
						if (!imageUrlList.isEmpty()) {
							imageInfo = imageUrlList.remove(0);
						} else {
							break;
						}
						imageListLock.notifyAll();
					}
					
					// TODO Better image download error handling
					Object data = null;
					if (imageInfo.getLink().length() != 0) {
						HttpConnection conn = HttpConnection.bitmap(imageInfo.getLink());
						data = conn.execute(null);
					}
					if (data != null) {
						// save
						saveImage((Bitmap)data, imageInfo);
						data = null;
					} else {
						if (BuildConfig.DEBUG) {
							Log.i("DataManager", "Failed to download image with id: " + imageInfo.getDefIndex());
						}
					}
					
					// Tell everybody else that we have downloaded a image
					synchronized (resultLock) {
						downloadedImages++;
						if (downloadedImages >= valueSinceLastProgressUpdate + VALUE_DIFF_FOR_PROGRESS_UPDATE)
							resultLock.notifyAll();
					}
				}
				
				// tell the world we are done here
				synchronized (resultLock) {
					finishedThreads++;
					resultLock.notifyAll();
				}
			}
			
			public void saveImage(Bitmap image, ImageInfo imageInfo) {
		    	boolean isPaintCan = false;
		    	boolean isTeamPaintCan = false;
				
		    	// get some info about the image
				if (imageInfo.getColor() != 0) {
					isPaintCan = true;

					if (imageInfo.getColor2() != 0) {
						isTeamPaintCan = true;
					}
				} else {
					isPaintCan = false;
				}
				
				try {
					if (image != null){
						if (isPaintCan){
							if (!isTeamPaintCan) {
								// Regular paintcan
								Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
								Canvas canvas = new Canvas(newBitmap);
								Paint paint = new Paint();
								paint.setColorFilter(new LightingColorFilter((0xFF << 24) | imageInfo.getColor(), 1));
								// draw paintcan
								canvas.drawBitmap(image, 0, 0, null);
								// draw paint color
								canvas.drawBitmap(paintColor, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);
								// recycle old image
								image.recycle();
								image = newBitmap;
							} else {
								// Team-paintcan
								Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
								Canvas canvas = new Canvas(newBitmap);
								Paint paint = new Paint();
								paint.setColorFilter(new LightingColorFilter((0xFF << 24) | imageInfo.getColor(), 1));
								// draw paintcan
								canvas.drawBitmap(image, 0, 0, null);
								// draw first paint color
								canvas.drawBitmap(teamPaintRed, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);	
								// draw second paint color
								paint.setColorFilter(new LightingColorFilter((0xFF << 24) | imageInfo.getColor2(), 1));
								canvas.drawBitmap(teamPaintBlue, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);
								
								image.recycle();
								image = newBitmap;
							}
						}
						FileOutputStream fos = context.openFileOutput(imageInfo.getDefIndex() + ".png", 0);
						boolean saved = image.compress(CompressFormat.PNG, 100, fos);
						fos.flush();
						fos.close();
						image.recycle();
						// TODO maybe check if saved was false
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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