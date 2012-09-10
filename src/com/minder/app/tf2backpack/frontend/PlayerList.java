package com.minder.app.tf2backpack.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.HttpConnection;
import com.minder.app.tf2backpack.PersonaState;
import com.minder.app.tf2backpack.PlayerAdapter;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataBaseHelper;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;

public class PlayerList extends Activity implements ListView.OnScrollListener{
	Handler mHandler = new Handler();
	
	private boolean mBusy = false;
	private boolean ready = false;
	private boolean loadingMore = false;
	private boolean nothingMoreToLoad = false;
	private boolean loadAvatars = false;
	
	private final String SHARED_PREF_FRIENDS = "friendlist";
	private final int CONTEXTMENU_VIEW_BACKPACK = 0;
	private final int CONTEXTMENU_VIEW_STEAMPAGE = 1;
	
	private ProgressDialog mProgress;
	private AdView adView;
	
	private boolean setPlayerId;
	private boolean friendList;
	private boolean searchList;
	public int searchPage = 1;
	public String searchQuery;
	
	private ListView mList;
	private PlayerAdapter adapter;
	private View footerView;
	private View noResultFooterView;
	private int totalInfoDownloads;
	private int numberInfoDownloads;
	private int workingThreads;
	
	private Request currentRequest;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        this.setTitle(R.string.friends);
        
        setContentView(R.layout.list_layout);
        
        String action = this.getIntent().getAction();
        if (action == null) {
        	finish();
        	return;
        }
        
        getSettings();
        
        // Look up the AdView as a resource and load a request.
        adView = (AdView)findViewById(R.id.ad);

        /*if (adView != null) {
            AdRequest r = new AdRequest();
            r.setTesting(true);
            
            adView.loadAd(r);
        }*/
        
        mList = (ListView)findViewById(android.R.id.list);
        mList.setOnScrollListener(this);
        
        // Set up our adapter
        adapter = new PlayerAdapter(this);
        adapter.setShowAvatars(loadAvatars);
        footerView = getLayoutInflater().inflate(R.layout.loading_footer, null);
        noResultFooterView = getLayoutInflater().inflate(R.layout.noresult_footer, null);
        mList.addFooterView(footerView, null, false);
        mList.setAdapter(adapter);
        mList.removeFooterView(footerView);
        
        mList.setBackgroundResource(R.color.bg_color);
        mList.setCacheColorHint(this.getResources().getColor(R.color.bg_color));
        
        mList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO This is NOT a good solution
				final SteamUser player = (SteamUser)adapter.getItem(arg2);
				if (player.steamdId64 == 0){
					// this only happens when we search
					Log.v("PlayerList", "64-bit id missing - fetching");
					Handler handler = new Handler() {
						public void handleMessage(Message message) {
							switch (message.what) {
								case HttpConnection.DID_START: {
									break;
								}
								case HttpConnection.DID_SUCCEED: {
									String textId = (String)message.obj;
									int idStartIndex = textId.indexOf("<steamID64>");
									int idEndIndex = textId.indexOf("</steamID64>");
									
									// check if player id was present
									if (idStartIndex == -1){
										idStartIndex = textId.indexOf("![CDATA[");
										idEndIndex = textId.indexOf("]]");
										if (idStartIndex == -1){
											//TODO Toast.makeText(PlayerList.this, "Failed to verify player", Toast.LENGTH_LONG).show();
										} else {
											//TODO Toast.makeText(PlayerList.this, textId.substring(idStartIndex + 8, idEndIndex), Toast.LENGTH_LONG).show();
										}
										
									} else {
										if (setPlayerId) {
											Intent result = new Intent();
											result.putExtra("name", "");
											result.putExtra("id", textId.substring(idStartIndex + 11, idEndIndex));
											setResult(RESULT_OK , result);
											
											/*SharedPreferences playerPrefs = PlayerList.this.getSharedPreferences("player", MODE_PRIVATE);
											Editor editor = playerPrefs.edit();
											editor.putString("id", textId.substring(idStartIndex + 11, idEndIndex));
											editor.commit();
											startActivity(new Intent(PlayerList.this, Main.class));*/
											finish();
										} else {
											startActivity(new Intent(PlayerList.this, Backpack.class).putExtra("id", textId.substring(idStartIndex + 11, idEndIndex)));
										}
									}
									break;
								}
								case HttpConnection.DID_ERROR: {
									break;
								}
							}
						}
					};
					
					new HttpConnection(handler)
						.getSpecificLines("http://steamcommunity.com/id/" + player.communityId + "/?xml=1", 2);
				} else {
					if (setPlayerId) {
						Intent result = new Intent();
						result.putExtra("name", player.steamName);
						result.putExtra("id", String.valueOf(player.steamdId64));
						setResult(RESULT_OK , result);
						
						finish();
					} else {
						startActivity(new Intent(PlayerList.this, Backpack.class).putExtra("id", String.valueOf(player.steamdId64)));
					}
				}
			}
		});
        
        mList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(R.string.player_options);
				menu.add(0, CONTEXTMENU_VIEW_BACKPACK, 0, R.string.view_backpack);
				menu.add(0, CONTEXTMENU_VIEW_STEAMPAGE, 0, R.string.view_steampage);
			}
       	
        });
        
	   	final Object data = getLastNonConfigurationInstance();
    	
    	if (data != null){
    		adapter.setPlayers((ArrayList<SteamUser>)data);
	        if (action.equals("com.minder.app.tf2backpack.VIEW_FRIENDS")){
	        	friendList = true;
	        } else if (action.equals("com.minder.app.tf2backpack.VIEW_WRENCH")){
	        	friendList = false;
	        	this.setTitle(R.string.golden_wrench_list);
	        } else if (action.equals("com.minder.app.tf2backpack.SEARCH")){
	        	/*SharedPreferences playerPrefs = this.getSharedPreferences("player", MODE_PRIVATE);
	            if (playerPrefs.getString("id", null) == null){
	            	setPlayerId = true;
	            }*/
	        	searchList = true;
	        	friendList = true;
	        	this.setTitle(R.string.search_result);
	        	if (adapter.getCount() == 0){
        			mList.addFooterView(noResultFooterView, null, false);
	        	}
	        }
	        setAdVisibility(View.VISIBLE);
    	} else {      
            setAdVisibility(View.GONE);
	        if (action.equals("com.minder.app.tf2backpack.VIEW_FRIENDS")){
	        	friendList = true;
	        	//this.setTitle(R.string.friends);
	            SharedPreferences playerPrefs = this.getSharedPreferences("player", MODE_PRIVATE);
	            
	            String playerId = playerPrefs.getString("id", null);
	            if (playerId != null) {
	            	SteamUser user = new SteamUser();
	            	user.steamdId64 = Long.parseLong(playerId);
	            	
	            	currentRequest = App.getDataManager().requestFriendsList(friendListListener, user);
	            }
	        } else if (action.equals("com.minder.app.tf2backpack.VIEW_WRENCH")){
	        	friendList = false;
	        	this.setTitle(R.string.golden_wrench_list);
	        	//new DownloadWrenchListTask().execute();
	        	setProgressBarIndeterminateVisibility(true);
	        } else if (action.equals("com.minder.app.tf2backpack.SEARCH")){
	        	/*SharedPreferences playerPrefs = this.getSharedPreferences("player", MODE_PRIVATE);
	            if (playerPrefs.getString("id", null) == null){
	            	setPlayerId = true;
	            }*/
	        	searchList = true;
	        	friendList = true;
	        	this.setTitle(R.string.search_result);
	        	setPlayerId = getIntent().getBooleanExtra("setid", false);
	        	searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
                new DownloadSearchListTask().execute(searchQuery);
                setProgressBarIndeterminateVisibility(true);
	        }
    	}
    }
    
    private void getSettings() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        loadAvatars = sp.getBoolean("showavatars", true);
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	
    	// cancel any ongoing work
    	if (currentRequest != null) {
    		App.getDataManager().cancelRequest(currentRequest);
    	}
    	
    	// remove dialog if it is showing
    	if (mProgress != null)
    	{
			if (mProgress.isShowing()){
				try {
					mProgress.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
    	}
    }
    
    @Override
    public void onRestart() {
    	super.onRestart();
    	
    	Log.d("PlayerList", "restart");
    	adapter.startBackgroundLoading();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	adapter.stopBackgroundLoading();
    	Log.d("PlayerList", "stop");
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	if (adView != null) {
    		adView.destroy();
    	}
    }
    
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (searchList){
			boolean loadMore = 
				firstVisibleItem + visibleItemCount >= totalItemCount;
			
			if(loadMore && ready && !loadingMore && !nothingMoreToLoad) {
				loadingMore = true;
				mList.addFooterView(footerView, null, false);
				DownloadSearchListTask searchTask = new DownloadSearchListTask();
				searchTask.pageNumber = ++searchPage;
				searchTask.execute(searchQuery);
			}
		}
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		if (!searchList){
	       switch (scrollState) {
	        case OnScrollListener.SCROLL_STATE_IDLE:
	            mBusy = false;
	            
	            int first = view.getFirstVisiblePosition();
	            int count = view.getLastVisiblePosition();
	            GetPlayerInfoRange(first, count);
	            break;
	        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
	            mBusy = true;
	            break;
	        case OnScrollListener.SCROLL_STATE_FLING:
	            mBusy = true;
	            break;
	        }
		}
	}
	
	private void GetPlayerInfoRange(int start, int count){
		// check if array is smaller then range to update
		/*if (mAdapter.mPlayers.size() <= count){
			count = mAdapter.mPlayers.size() - 1;
		}
		if (mAdapter.mPlayers.size() <= start){
			start = mAdapter.mPlayers.size() - 1;
			if (start < 0) start = 0;
		}
        for (int i = start; i <= count; i++) {
        	if (!mAdapter.mPlayers.get(i).fetchingData){
        		mAdapter.mPlayers.get(i).fetchingData = true;
            	totalInfoDownloads++;
            	setProgressBarIndeterminateVisibility(true);
            	new DownloadFilesTask().execute(mAdapter.mPlayers.get(i).steamdId64);
        	}
        }*/
	}
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (friendList == true){
        	inflater.inflate(R.menu.friend_menu, menu);
        } else {
        	inflater.inflate(R.menu.wrench_menu, menu);
        }
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_sort_number:
        	adapter.setComparator(new byWrenchNumber());
            return true;
        case R.id.menu_sort_name:
        	adapter.setComparator(new byPlayerName());
        	return true;
        case R.id.menu_sort_persona_state:
        	adapter.setComparator(new byPersonaState());
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	/* Switch on the ID of the item, to get what the user selected. */
    	switch (item.getItemId()) {
	    	case CONTEXTMENU_VIEW_BACKPACK:{
	    		/* Get the selected item out of the Adapter by its position. */
	    		//Favorite favContexted = (Favorite) mFavList.getAdapter().getItem(menuInfo.position);
	    		SteamUser player = (SteamUser)adapter.getItem(info.position);
	    		//TODO startActivity(new Intent(PlayerList.this, Backpack.class).putExtra("id", String.valueOf(player.steamdId64)));
	    		return true; /* true means: "we handled the event". */
	    	}
	    	case CONTEXTMENU_VIEW_STEAMPAGE: {
	    		SteamUser player = (SteamUser)adapter.getItem(info.position);
	    		Intent browser = new Intent();
	    		browser.setAction("android.intent.action.VIEW");
	    		browser.setData(Uri.parse("http://steamcommunity.com/profiles/" + player.steamdId64));
	    		startActivity(browser);
	    		return true; /* true means: "we handled the event". */
	    	}
    	}
    	return false;
    }
    
    private AsyncTaskListener friendListListener = new AsyncTaskListener() {
		public void onPreExecute() {
			mProgress = ProgressDialog.show(PlayerList.this, getResources().getText(R.string.please_wait_title), getResources().getText(R.string.downloading_player_list), true, true);
            setProgressBarIndeterminateVisibility(true);
		}

		public void onProgressUpdate(ProgressUpdate object) {
			// nothing
		}

		@SuppressWarnings("unchecked")
		public void onPostExecute(Object result) {
        	if (result != null){
        		currentRequest = 
        			App.getDataManager().requestSteamUserInfo(
        					getSteamUserInfoListener, 
        					((ArrayList<SteamUser>) result));
        		
        		if (totalInfoDownloads == 0){
    	    		setAdVisibility(View.VISIBLE);
            		System.gc();
        		}
        		
            	if (mProgress != null)
            	{
        			if (mProgress.isShowing()){
        				try {
        					mProgress.dismiss();
        				} catch (Exception e) {
        					e.printStackTrace();
        				}
        			}
            	}
        		
            	adapter.addPlayerInfoList((ArrayList<SteamUser>) result);
        	}
		}
	};
    

    private class DownloadSearchListTask extends AsyncTask<String, Void, ArrayList<SteamUser>> {
    	public int pageNumber = 1;
    	
    	protected void onPreExecute(){
    		workingThreads++;
    	}
    	
		@Override
		protected ArrayList<SteamUser> doInBackground(String... params) {    	
	    	totalInfoDownloads = 0;
        	ArrayList<SteamUser> players = new ArrayList<SteamUser>();
        	
	        String data = DownloadText("http://steamcommunity.com/actions/Search?p=" + pageNumber + "&T=Account&K=\"" + params[0] + "\"");
	        	
	        int curIndex = 0;
	        SteamUser newPlayer;
	        while (curIndex < data.length()){
	        	int index = data.indexOf("<a class=\"linkTitle\" href=\"http://steamcommunity.com/profiles/", curIndex);
	        	if (index != -1){
	        		int endIndex = data.indexOf("\">", index);
	        		newPlayer = new SteamUser();
	        		newPlayer.steamdId64 = Long.parseLong(data.substring(index + 62, endIndex));
	        		
	        		index = data.indexOf("</a>", endIndex);
	        		newPlayer.steamName = data.substring(endIndex + 2, index);
	        		
	        		players.add(newPlayer);
	        		curIndex = index;
	        	}
	        	else
	        	{
	        		index = data.indexOf("<a class=\"linkTitle\" href=\"http://steamcommunity.com/id/", curIndex);
		        	if (index != -1){
		        		int endIndex = data.indexOf("\">", index);
		        		newPlayer = new SteamUser();
		        		newPlayer.communityId = data.substring(index + 56, endIndex);
		        		
		        		index = data.indexOf("</a>", endIndex);
		        		newPlayer.steamName = data.substring(endIndex + 2, index);
		        		
		        		players.add(newPlayer);
		        		curIndex = index;
		        	}
	        		break;
	        	}
	        }
	        
			return players;
		}
    	
        protected void onPostExecute(ArrayList<SteamUser> result) {
        	if (result != null){
        		currentRequest = 
        			App.getDataManager().requestSteamUserInfo(
        					getSteamUserInfoListener, 
        					((ArrayList<SteamUser>) result));
        		
        		if (totalInfoDownloads == 0){
    	    		setProgressBarIndeterminateVisibility(false);
    	    		setAdVisibility(View.VISIBLE);
            		System.gc();
        		}
        		
            	if (mProgress != null)
            	{
        			if (mProgress.isShowing()){
        				try {
        					mProgress.dismiss();
        				} catch (Exception e) {
        					e.printStackTrace();
        				}
        			}
            	}
        		
        		nothingMoreToLoad = true;
        		if (result.size() > 0)
        		{
	        		for(SteamUser p : result){
	        			if (adapter.addPlayerInfo(p)){
	        				nothingMoreToLoad = false;
	        			}
	        		}
        		} else {
        			mList.removeFooterView(footerView);
        			mList.addFooterView(noResultFooterView, null, false);
        		}
        	}
        	workingThreads--;
        	//GetPlayerInfoRange(0, 10);
        	loadingMore = false;
        	mList.removeFooterView(footerView);
        	ready = true;
        }
        
        private String DownloadText(String URL)
        {
            int BUFFER_SIZE = 2000;
            InputStream in = null;
            try {
                in = OpenHttpConnection(URL);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return "";
            }
            
            if (in != null)
            {
	            InputStreamReader isr = new InputStreamReader(in);
	            int charRead;
	            String str = "";
	            char[] inputBuffer = new char[BUFFER_SIZE];          
	            try {
	                while ((charRead = isr.read(inputBuffer))>0)
	                {                    
	                    //---convert the chars to a String---
	                    String readString = 
	                        String.copyValueOf(inputBuffer, 0, charRead);                    
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
        
        private InputStream OpenHttpConnection(String urlString) 
        throws IOException {
            InputStream in = null;
            int response = -1;
                   
            URL url = new URL(urlString); 
            URLConnection conn = url.openConnection();
                     
            if (!(conn instanceof HttpURLConnection))                     
                throw new IOException("Not an HTTP connection");
            
            try{
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                httpConn.setAllowUserInteraction(false);
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setRequestMethod("GET");
                httpConn.connect(); 

                response = httpConn.getResponseCode();                 
                if (response == HttpURLConnection.HTTP_OK) {
                    in = httpConn.getInputStream();                                 
                }                     
            }
            catch (Exception ex)
            {
                throw new IOException("Error connecting");            
            }
            return in;     
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() { 	
        return adapter.getPlayers();
    } 
    
    private AsyncTaskListener getSteamUserInfoListener = new AsyncTaskListener () {
		public void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
		}

		public void onProgressUpdate(ProgressUpdate object) {
			adapter.notifyDataSetChanged();
		}

		public void onPostExecute(Object object) {
			setProgressBarIndeterminateVisibility(false);
		}
	};
    
    // obsolete
    public static class byWrenchNumber implements java.util.Comparator<SteamUser> {
    	public int compare(SteamUser boy, SteamUser girl) {
    		return boy.wrenchNumber - girl.wrenchNumber;
    	}
    }
    
    public static class byPlayerName implements java.util.Comparator<SteamUser> {
    	public int compare(SteamUser boy, SteamUser girl) {
    		if (boy.steamName != null && girl.steamName != null){
    			return boy.steamName.compareToIgnoreCase(girl.steamName);
    		} else {
    			return 0;
    		}
    	}
	}
    
    public static class byPersonaState implements java.util.Comparator<SteamUser> {
    	public int compare(SteamUser boy, SteamUser girl) {
    		int boyState = boy.personaState.value;
    		int girlState = girl.personaState.value;
    		
    		if (boyState > 1) boyState = 1;
    		if (girlState > 1) girlState = 1;
    		
    		if (boy.gameId.length() > 0) boyState = 2;
    		if (girl.gameId.length() > 0) girlState = 2;
    		
    		// sort by name secondly
    		if (boyState == girlState) {
        		if (boy.steamName != null && girl.steamName != null){
        			return boy.steamName.compareToIgnoreCase(girl.steamName);
        		} else {
        			return 0;
        		}
    		}
    		
    		return girlState - boyState;
    	}
	} 
    
    private void setAdVisibility(int visibility){
        if (adView != null){
        	adView.setVisibility(visibility);
        }
    }
}
