package com.minder.app.tf2backpack.frontend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import com.minder.app.tf2backpack.HttpConnection;
import com.minder.app.tf2backpack.HttpConnectionBitmapRunner;
import com.minder.app.tf2backpack.Internet;
import com.minder.app.tf2backpack.ItemListSelect;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.GameSchemeParser;
import com.minder.app.tf2backpack.backend.GameSchemeParser.ImageInfo;
import com.minder.app.tf2backpack.frontend.NewsList.NewsItem;

public class DashBoard extends Activity {
	private final int CURRENT_DOWNLOAD_VERSION = 1;
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
	private int failedDownloads;
	private int totalDownloads;
	private int numberOfDownloads;
	private String errorMessage;
	private boolean resetImages;
	
	private Bitmap paintColor;
	private Bitmap teamPaintRed;
	private Bitmap teamPaintBlue;
	private ArrayList<ImageInfo> imageUrlList;
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
        
        String action = this.getIntent().getAction();
        if (action != null){
            if (action.equals("com.minder.app.tf2backpack.DOWNLOAD_GAMEFILES")){
            	showDialog(DIALOG_DOWNLOAD_GAMEFILES);
            }
        }
        
        if (gameFilesVersion > 0){
        	if (gameFilesVersion < CURRENT_DOWNLOAD_VERSION){
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
                    	if (resetImages) {
                    		DeleteItemImages();
                    	}
                    	DownloadGameFiles();
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
        	
        	
            /*return new AlertDialog.Builder(this)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.download_notice)
                .setMessage(R.string.gamescheme_downloadinfo)
                .setMultiChoiceItems(new CharSequence[] {"Reset images"}, null, onMultiListener)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	/*gameSchemePref = GameSchemeDownloader.this.getSharedPreferences("gamescheme", MODE_PRIVATE);
                    	GameSchemeDownloader.this.setContentView(R.layout.downloader);
                    	
                    	downloadText = (TextView)findViewById(R.id.TextViewDownloadStatus);
                    	
                    	imageDownloadProgress = (ProgressBar)findViewById(R.id.ProgressBarImageDownload);
                    	imageDownloadProgress.setProgress(0);
                    	Download();*/
                    	/*if (resetImages) {
                    		DeleteItemImages();
                    	}
                    	DownloadGameFiles();
                    }
                })
                /*.setNeutralButton(R.string.alert_dialog_something, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {


                    }
                })*/
                /*.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	finish();
                    }
                })
                .create();*/
        case DIALOG_UPDATE_GAMEFILES:
            return new AlertDialog.Builder(this)
            .setIcon(R.drawable.alert_dialog_icon)
            .setTitle(R.string.download_notice)
            .setMessage(R.string.gamescheme_updateinfo1)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {              	
                	// download new ones
                	DownloadGameFiles();
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
    
    /**
     * Checsk if weaponimages are downloaded and if so 
     * shows a dialog box asking if you want to refresh them
     */
    private void DeleteItemImages() {
		File file = new File(this.getFilesDir().getPath());
		if (file.isDirectory()) {
	        String[] children = file.list();
	        for (int i = 0; i < children.length; i++) {
	            new File(file, children[i]).delete();
	        }
	    }

    }
    
    private void DownloadGameFiles(){
    	showDialog(DIALOG_PROGRESS);

    	paintColor = BitmapFactory.decodeResource(this.getResources(), R.drawable.paintcan_paintcolor, mBitmapOptions);
    	teamPaintRed = BitmapFactory.decodeResource(this.getResources(), R.drawable.teampaint_red_mask, mBitmapOptions);
    	teamPaintBlue = BitmapFactory.decodeResource(this.getResources(), R.drawable.teampaint_blu_mask, mBitmapOptions);
    	
		Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
				case HttpConnection.DID_START: {
					break;
				}
				case HttpConnection.DID_SUCCEED: {
					if (mProgress != null) {
						mProgress.setMessage("Parsing gamescheme...");
					}
					GameSchemeParser gs = new GameSchemeParser((String)message.obj, DashBoard.this.getApplicationContext());
					message.obj = null;
					
					if (gs.error != null){
						Toast.makeText(DashBoard.this, gs.error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						
						if (mProgress != null) {
							mProgress.dismiss();
						}
						break;
					}
					
					//Intent serviceIntent = new Intent(DashBoard.this, DatabaseService.class);
					//serviceIntent.putStringArrayListExtra("sql", gs.sqlExecList);
					//startService(serviceIntent);
					
					if (mProgress != null) {
						mProgress.setMessage("Downloading images...");
					}
					if (gs.GetImageURList() != null){
				    	imageUrlList = gs.GetImageURList();
						gs = null;
						System.gc();
						
						DownloadImages();
						
						gamePrefs = DashBoard.this.getSharedPreferences("gamefiles", MODE_PRIVATE);
						Editor editor = gamePrefs.edit();
						editor.putInt("download_version", CURRENT_DOWNLOAD_VERSION);
						editor.commit();
					} else {
						Toast.makeText(DashBoard.this, gs.error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					}
					gs = null;
					System.gc();
					Log.d("Dashboard", "GameScheme download complete");
					break;
				}
				case HttpConnection.DID_ERROR: {
					Exception e = (Exception) message.obj;
					e.printStackTrace();
					if (Internet.isOnline(DashBoard.this)){
						if (e instanceof UnknownHostException){
							Toast.makeText(DashBoard.this, R.string.no_steam_api, Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(DashBoard.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						}	
					} else {
						Toast.makeText(DashBoard.this, R.string.no_internet, Toast.LENGTH_LONG).show();
					}
					
					if (mProgress != null) {
						mProgress.dismiss();
					}
					break;
				}
				}
			}
		};
		new HttpConnection(handler)
		.get("http://api.steampowered.com/IEconItems_440/GetSchema/v0001/?key=" + 
			Util.GetAPIKey() + "&format=json&language=en");
    }
    
    private void DownloadImages(){
		Handler handler = new Handler() {
			public void handleMessage(Message message) {
				if (mProgress == null || mProgress.isShowing() == false) showDialog(DIALOG_PROGRESS);
				switch (message.what) {
					case HttpConnection.DID_START:{
						break;
					}
					
					case HttpConnection.DID_SUCCEED:{
						numberOfDownloads++;
					}
					
					case HttpConnection.DID_ERROR: {
						numberOfDownloads++;
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						Log.e(Util.GetTag(), "Failed to download image. Image probably doesnt exist");
						break;
					}
					default: {
						numberOfDownloads++;
						if (mProgress != null) {
							mProgress.incrementProgressBy(1);
						}
						//workerSystem.addWork(new ImageSaverTask((Bitmap)message.obj, message.what));
					    //Thread imageSaver = new Thread(new ImageSaverTask((Bitmap)message.obj, message.what));
					    //imageSaver.setName("ImageSaver" + numberOfDownloads);
					    //imageSaver.start();
					    message.obj = null;
						break;
					}
				}
				if (numberOfDownloads == totalDownloads){
					// Save that we have downloaded the files
					Editor editor = DashBoard.this.getSharedPreferences("gamefiles", 0).edit();
					editor.putBoolean("downloaded", true);
					editor.commit();
                	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(DashBoard.this);
                	editor = sp.edit();
                	editor.putBoolean("skipunknownitemdialog", false);
                	editor.commit();
                	// close progressdialog
                	if (mProgress != null){
                		if (mProgress.isShowing()) {
                			mProgress.dismiss();
                        	mProgress = null;
                		}
                	}
					
					// check if there was some problem
					if (failedDownloads > 0){
						Toast.makeText(DashBoard.this.getApplicationContext(), "Failed to save some images: " + errorMessage, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(DashBoard.this.getApplicationContext(), R.string.download_successful, Toast.LENGTH_SHORT).show();
					}
					
					//workerSystem.stopWhenFinished();
					//workerSystem = null;
					imageUrlList.clear();
					// recycle bitmaps
					paintColor.recycle();
					teamPaintRed.recycle();
					teamPaintBlue.recycle();
				}
			}
		};
		
		Log.d("Dashboard", "Total images: " + imageUrlList.size());
		
		//workerSystem = new WorkerSystem(4);
		
		for (ImageInfo ii : imageUrlList){
			File file = new File(this.getFilesDir().getPath() + "/" + ii.getDefIndex() + ".png");
			if (!file.exists()){
				new HttpConnectionBitmapRunner(handler, this).bitmap(ii.getLink(), ii.getFormatedIndexColor());
				totalDownloads++;
			}
		}
		
		Log.d("Dashboard", "Total downloads: " + totalDownloads);
		if (mProgress != null){
			mProgress.setMax(totalDownloads);
		}
	}
    
    public class ImageSaverTask implements Runnable {
    	private Bitmap image;
    	private int id;
    	private boolean isPaintCan;
    	private boolean isTeamPaintCan;
    	private int color;
    	private int color2;
    	
		public ImageSaverTask(Bitmap image, int id){
			this.image = image;
			if ((id & 1073741824) == 1073741824){
				this.isPaintCan = true;
				this.id = id ^ 1073741824;
				
				for (ImageInfo i : DashBoard.this.imageUrlList){
					if (i.getDefIndex() == this.id){
						this.color = i.getColor();
						this.color2 = i.getColor2();
						if (this.color2 != 0) {
							this.isTeamPaintCan = true;
						}
						break;
					}
				}
			} else {
				this.isPaintCan = false;
				this.id = id;
			}
		}

		public void run() {
			try {
				if (image != null){
					if (isPaintCan){
						if (!isTeamPaintCan) {
							// Regular paintcan
							Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
							Canvas canvas = new Canvas(newBitmap);
							Paint paint = new Paint();
							paint.setColorFilter(new LightingColorFilter((0xFF << 24) | color, 1));
							// draw paintcan
							canvas.drawBitmap(image, 0, 0, null);
							// draw paint color
							canvas.drawBitmap(paintColor, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);
							image.recycle();
							image = newBitmap;
						} else {
							// Team-paintcan
							Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
							Canvas canvas = new Canvas(newBitmap);
							Paint paint = new Paint();
							paint.setColorFilter(new LightingColorFilter((0xFF << 24) | color, 1));
							// draw paintcan
							canvas.drawBitmap(image, 0, 0, null);
							// draw first paint color
							canvas.drawBitmap(teamPaintRed, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);
							
							paint.setColorFilter(new LightingColorFilter((0xFF << 24) | color2, 1));
							canvas.drawBitmap(teamPaintBlue, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);
							
							image.recycle();
							image = newBitmap;
						}
					}
					FileOutputStream fos = openFileOutput(id + ".png", 0);
					boolean saved = image.compress(CompressFormat.PNG, 100, fos);
					fos.flush();
					fos.close();
					image.recycle();
					if (saved == false){
						failedDownloads++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				errorMessage = e.getLocalizedMessage();
				failedDownloads++;
			}
		}
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
    
    class PaintCan {
    	public int color;
    	public int defIndex;
    	
    	public PaintCan(int defIndex, int color) {
    		this.defIndex = defIndex;
    		this.color = color;
    	}
    }
    
}
