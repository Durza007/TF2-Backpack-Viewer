package com.minder.app.tf2backpack.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.ItemListSelect;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.frontend.NewsList.NewsItem;

public class DashBoard extends Activity {
	private final int DIALOG_DOWNLOAD_GAMEFILES = 0;
	private final int DIALOG_PROGRESS = 1;
	private final int DIALOG_UPDATE_GAMEFILES = 2;
	
	private final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
	
	private ImageButton backpackButton;
	private ImageButton settingsButton;
	private ImageButton friendsButton;
	private ImageButton wrenchButton;
	private Intent backpackIntent;
	private Intent settingsIntent;
	private String playerId;
	private SharedPreferences gamePrefs;
	private ProgressDialog mProgress;
	
	private TextView newsTitle;
	private TextView newsDate;
	//private View newsView;
	private boolean newsLoaded = false;
	
	private int gameFilesVersion;
	private int totalDownloads;
	private int numberOfDownloads;
	private boolean resetImages;
	
	//private WorkerSystem workerSystem;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AdMobActivity.createAdmobActivity(this);
        
        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);       
        setContentView(R.layout.dashboard);
        
        /*Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            public void uncaughtException(Thread thread, Throwable ex)
            {
                try {
                    File f = new File(Environment.getExternalStorageDirectory(),"crash.hprof");
                    String path = f.getAbsolutePath();
                    Debug.dumpHprofData(path);
                    Log.d("error", "HREF dumped to " + path);
                    Log.d("CRASH", ex.getLocalizedMessage(), ex);
                }
                catch (IOException e) {
                    Log.d("error","Huh?",e);
                }
            }
        });*/
        
        mBitmapOptions.inScaled = false;
        
        //AdManager.setTestDevices( new String[] { "B135E5E10665286A9FA99BA95CE926D4" } );
        
        backpackIntent = new Intent(this, SelectBackpack.class);
        settingsIntent = new Intent(this, Settings.class);
        
        backpackButton = (ImageButton)findViewById(R.id.ImageButtonBackPack);
        backpackButton.setOnClickListener(onButtonBackpackClick);
        
        settingsButton = (ImageButton)findViewById(R.id.ImageButtonSettings);
        settingsButton.setOnClickListener(onButtonSettingsClick);
        
        friendsButton = (ImageButton)findViewById(R.id.ImageButtonMyFriends);
        friendsButton.setOnClickListener(onButtonFriendsClick);
        
        wrenchButton = (ImageButton)findViewById(R.id.ImageButtonCatalouge);
        wrenchButton.setOnClickListener(onButtonWrenchClick);
        
        final String newsData = (String)getLastNonConfigurationInstance();
        
        newsTitle = (TextView)findViewById(R.id.TextViewNewsTitle);
        newsDate = (TextView)findViewById(R.id.TextViewNewsDate);	
        
        if (newsData != null) {
        	newsLoaded = true;
        	int index = newsData.indexOf("|");
        	if (index != -1) {
        		newsTitle.setText(newsData.substring(0, index));
        		newsDate.setText(newsData.substring(index + 1));
        	}
        }
        View newsView = findViewById(R.id.LinearLayoutNews);
        newsView.setOnClickListener(onNewsItemClick);
        
        gamePrefs = this.getSharedPreferences("gamefiles", MODE_PRIVATE);
        gameFilesVersion = gamePrefs.getInt("download_version", 0);
        
        // TODO Old stuff - rewrite
        String action = this.getIntent().getAction();
        if (action != null){
            if (action.equals("com.minder.app.tf2backpack.DOWNLOAD_GAMEFILES")){
            	showDialog(DIALOG_DOWNLOAD_GAMEFILES);
            }
        }
        
        if (gameFilesVersion > 0){
        	if (gameFilesVersion < DataManager.CURRENT_GAMESCHEMA_VERSION){
        		showDialog(DIALOG_UPDATE_GAMEFILES);
        	}
        } else {
        	showDialog(DIALOG_DOWNLOAD_GAMEFILES);
        }
        
        new DownloadNewsTask().execute();
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	if (mProgress != null){
        	mProgress.dismiss();
    	}
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	mProgress = null;
    }
    
    OnClickListener onButtonBackpackClick = new OnClickListener(){
		public void onClick(View v) {
			backpackIntent.putExtra("id", playerId);
			startActivity(backpackIntent);
		}
    };
    
    OnClickListener onButtonSettingsClick = new OnClickListener(){
		public void onClick(View v) {
			startActivity(settingsIntent);
		}
    };
    
    OnClickListener onButtonFriendsClick = new OnClickListener(){
		public void onClick(View v) {
			startActivity(new Intent(DashBoard.this, PlayerList.class).setAction("com.minder.app.tf2backpack.VIEW_FRIENDS"));
		}
    };
    
    OnClickListener onButtonWrenchClick = new OnClickListener(){
		public void onClick(View v) {
			//startActivity(new Intent(DashBoard.this, PlayerList.class).setAction("com.minder.app.tf2backpack.VIEW_WRENCH"));
			//startActivity(new Intent(DashBoard.this, ItemGridViewer.class).setAction("com.minder.app.tf2backpack.VIEW_ALL_ITEMS"));
			startActivity(new Intent(DashBoard.this, ItemListSelect.class));
		}
    };
    
    OnClickListener onNewsItemClick = new OnClickListener(){
		public void onClick(View v) {
			startActivity(new Intent(DashBoard.this, NewsList.class));
		}
    };
    
    OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			resetImages = isChecked;
		}
    };
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	if (newsLoaded) {
    		return newsTitle.getText() + "|" + newsDate.getText();
    	}
    	return null;
    } 
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
	        case DIALOG_DOWNLOAD_GAMEFILES:      	
	        	Context mContext = getApplicationContext();
	        	LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
	        	View layout = inflater.inflate(R.layout.dialog_download_reset_images,
	        	                               (ViewGroup) findViewById(R.id.LinearLayout01));
	
	        	CheckBox cb = (CheckBox)layout.findViewById(R.id.checkBoxRefreshImages);
	        	cb.setOnCheckedChangeListener(onCheckedChangeListener);
	        	//return dialog;
	        	
	        	return new AlertDialog.Builder(this)
		            .setIcon(R.drawable.alert_dialog_icon)
		            .setTitle(R.string.download_notice)
	        		.setView(layout)
	        		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {

	                    }
	                })
	                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	if (gameFilesVersion == 0) {
	                    		finish();
	                    	}
	                    }
	                })
	        		.create();
	        case DIALOG_UPDATE_GAMEFILES:
	            return new AlertDialog.Builder(this)
	            .setIcon(R.drawable.alert_dialog_icon)
	            .setTitle(R.string.download_notice)
	            .setMessage(R.string.gamescheme_updateinfo1)
	            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {              	

	                }
	            })
	            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	finish();
	                }
	            })
	            .create();
	        case DIALOG_PROGRESS:{
	        	mProgress = new ProgressDialog(DashBoard.this);
	        	mProgress.setCancelable(false);
	        	mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        	mProgress.setMessage("Downloading...");
	        	mProgress.setMax(totalDownloads);
	        	mProgress.setProgress(numberOfDownloads);
	            return mProgress;
	        }  	
        }
        return null;
    }
    
    private class DownloadNewsTask extends AsyncTask<Void, Void, NewsItem>{
    	private boolean unknownHostException;
    	
		@Override
		protected NewsItem doInBackground(Void... params) {
			XmlPullParserFactory pullMaker;
			NewsItem result;
			try {
	        	URL xmlUrl = new URL("http://api.steampowered.com/ISteamNews/GetNewsForApp/v0001/?appid=440&count=1&maxlength=100&format=xml");
	        	
	            pullMaker = XmlPullParserFactory.newInstance();
	
	            XmlPullParser parser = pullMaker.newPullParser();
	            InputStream fis = xmlUrl.openStream();
	
	            parser.setInput(fis, null);
	
	        	boolean newsitem = false;
	        	boolean title = false;
	        	boolean url = false;
	        	boolean contents = false;
	        	boolean date = false;
	        	
	        	result = NewsItem.CreateNewsItem();
	
	            int eventType = parser.getEventType();
	            while (eventType != XmlPullParser.END_DOCUMENT) {
	                switch (eventType) {
	                case XmlPullParser.START_DOCUMENT:
	                    break;
	                case XmlPullParser.START_TAG:
	                    if (parser.getName().equals("newsitem")) {
	                    	newsitem = true;
	                    } else if (parser.getName().equals("title")) {
	                    	title = true;
	                    } else if (parser.getName().equals("url")) {
	                    	url = true;
	                    } else if (parser.getName().equals("contents")) {
	                    	contents = true;
	                    } else if (parser.getName().equals("date")) {
	                    	date = true;
	                    }
	                    break;
	                case XmlPullParser.END_TAG:
	                    if (parser.getName().equals("newsitem")) {
	                    	newsitem = false;
	                    } else if (parser.getName().equals("title")) {
	                    	title = false;
	                    } else if (parser.getName().equals("url")) {
	                    	url = false;
	                    } else if (parser.getName().equals("contents")) {
	                    	contents = false;
	                    } else if (parser.getName().equals("date")) {
	                    	date = false;
	                    }
	                    break;
	                case XmlPullParser.TEXT:
	                    if (newsitem) {
	                    	if (title){
	                    		result.setTitle(parser.getText());
	                    	} else if (url){
	                    		result.setUrl(parser.getText());
	                    	} else if (contents){
	                    		result.setContents(parser.getText());
	                    	} else if (date){
	                    		result.setDate(Long.parseLong((parser.getText() + "000").substring(0, 13)));
	                    	}
	                    }
	                    break;
	
	                }
	                eventType = parser.next();
	            }
	            return result;
	        } catch (UnknownHostException e) {
	            Log.e("xml_perf", "Pull parser failed", e);
	            unknownHostException = true;
	        } catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return null;
		}
		
        protected void onPostExecute(NewsItem result) {
        	if (result != null){
        		newsLoaded = true;
	        	newsTitle.setText(result.getTitle());
	        	newsDate.setText(result.getDate());
	        	//newsContent.setText(result.getContents());
        	} else {
        		if (unknownHostException == true){
        			newsTitle.setText("Connection to Steam API failed");
        		}
        	}
        }
    	
    }
}
