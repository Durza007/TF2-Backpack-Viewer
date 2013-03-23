package com.minder.app.tf2backpack;

import java.net.SocketTimeoutException;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.frontend.DashboardActivity;

public class GameSchemeDownloaderService extends Service {
	private static final String DEBUG_TAG = "GameSchemeDownloaderService";
	private static final String PREF_NAME = "gamefiles";
	private static final String PREF_DOWNLOAD_VERSION = "download_version";
	private static final String PREF_DOWNLOAD_HIGHRES = "download_highres";
	private static final int DOWNLOAD_NOTIFICATION_ID = 1337;
	private static boolean gameSchemeChecked = false;
	private static boolean gameSchemeReady = false;
	private static boolean gameSchemeUpToDate = false;
	private static boolean currentGameSchemeImagesIsHighres;
	
	private static int currentGameSchemeVersion;
	private static boolean downloadingGameScheme = false;
	public static boolean downloadGameSchemeSuccess = false;
	public static long totalBytes;
	public static long currentBytes;
	public static int totalImages;
	public static int currentAmountImages;
	public static int currentTaskStringId;
	
	private Request gameSchemeRequest;
	private boolean downloadHighresImages;
	
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
	
	public static boolean isGameSchemeDownloading() {
		return downloadingGameScheme;
	}
	
	public static boolean isCurrentGameSchemeHighres() {
		return currentGameSchemeImagesIsHighres;
	}
	
	public static void startGameSchemeDownload(Activity starterActivity, boolean refreshImages, boolean highresImages) {
    	Intent intent = new Intent(starterActivity, GameSchemeDownloaderService.class);
    	intent.putExtra("refreshImages", refreshImages);
    	intent.putExtra("highresImages", highresImages);
    	starterActivity.startService(intent);
	}
 
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
	    return START_NOT_STICKY;
	}
 
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void handleCommand(Intent intent) {
    	boolean refreshImages = intent.getExtras().getBoolean("refreshImages");
    	downloadHighresImages = intent.getExtras().getBoolean("highresImages");
    	
    	// start the request
    	gameSchemeRequest = App.getDataManager().requestSchemaFilesDownload(gameSchemeListener, refreshImages, downloadHighresImages);
    }
    
    private void removeGameSchemeDownloaded() {
    	gameSchemeReady = false;
    	
    	SharedPreferences gamePrefs = this.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    	
        Editor editor = gamePrefs.edit();
        editor.putInt(PREF_DOWNLOAD_VERSION, 0);
        editor.commit();
    }
    
    private void saveGameSchemeDownloaded() {
    	SharedPreferences gamePrefs = this.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    	
        Editor editor = gamePrefs.edit();
        editor.putInt(PREF_DOWNLOAD_VERSION, DataManager.CURRENT_GAMESCHEMA_VERSION);
        editor.putBoolean(PREF_DOWNLOAD_HIGHRES, downloadHighresImages);
        editor.commit();
        
        gameSchemeReady = true;
        currentGameSchemeImagesIsHighres = downloadHighresImages;
    }
    
    private static void getGameSchemeVersion() {
    	SharedPreferences gamePrefs = App.getAppContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);	
    	currentGameSchemeVersion = gamePrefs.getInt(PREF_DOWNLOAD_VERSION, -1);
    	currentGameSchemeImagesIsHighres = gamePrefs.getBoolean(PREF_DOWNLOAD_HIGHRES, false);
    }
    
    AsyncTaskListener gameSchemeListener = new AsyncTaskListener() {
    	private Notification notification;
    	
		public void onPreExecute() {
			downloadingGameScheme = true;
			
			final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			
			final Intent intent = new Intent(GameSchemeDownloaderService.this, DashboardActivity.class);
	        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			
			final NotificationCompat.Builder builder = new NotificationCompat.Builder(GameSchemeDownloaderService.this)
			.setOngoing(true)
			.setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setTicker(getResources().getText(R.string.starting_download));
			
			currentTaskStringId = R.string.starting_download;
			
			notification = builder.build();
			notification.contentView = new RemoteViews(getApplicationContext().getPackageName(),
                    R.layout.download_progress_notification);
			notification.contentView.setProgressBar(R.id.progressBarDownload, 100, 0, true);
			
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_TAG, "Showing notification");
			notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
			
			// the gamescheme files will be in a undefined state from
			// here on. Reflect that by saying they dont exist
			removeGameSchemeDownloaded();
		}

		public void onProgressUpdate(ProgressUpdate progress) {	
			final Intent intent = new Intent(GameSchemeDownloaderService.this, DashboardActivity.class);
	        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
	        
	        if (progress.updateType == DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE) {
	        	
				notification.contentView.setProgressBar(R.id.progressBarDownload, progress.totalCount, progress.count, false);
				notification.contentView.setTextViewText(R.id.textViewTitle, getResources().getText(R.string.downloading_schema));
				notification.contentView.setViewVisibility(R.id.textViewExtra, View.VISIBLE);
				notification.contentView.setTextViewText(R.id.textViewExtra, (progress.count / 1024) + "/" + (progress.totalCount / 1024) + " [kB]");
				
				currentTaskStringId = R.string.downloading_schema;
				totalBytes = progress.totalCount;
				currentBytes = progress.count;
	        } else if (progress.updateType == DataManager.PROGRESS_PARSING_SCHEMA) {
				notification.contentView.setProgressBar(R.id.progressBarDownload, 100, 0, true);
				notification.contentView.setTextViewText(R.id.textViewTitle, getResources().getText(R.string.parsing_schema));
				notification.contentView.setViewVisibility(R.id.textViewExtra, View.GONE);
				
				currentTaskStringId = R.string.parsing_schema;
				
			} else if (progress.updateType == DataManager.PROGRESS_DOWNLOADING_IMAGES_UPDATE) {
				// this means game files are downloaded
				saveGameSchemeDownloaded();

				notification.contentView.setProgressBar(R.id.progressBarDownload, progress.totalCount, progress.count, false);
				notification.contentView.setTextViewText(R.id.textViewTitle, getResources().getText(R.string.downloading_images));
				notification.contentView.setViewVisibility(R.id.textViewExtra, View.VISIBLE);
				notification.contentView.setTextViewText(R.id.textViewExtra, progress.count + "/" + progress.totalCount);
				
				currentTaskStringId = R.string.downloading_images;
				currentAmountImages = progress.count;
				totalImages = progress.totalCount;
			}
	        
			final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
		}

		public void onPostExecute(Request request) {
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_TAG, "Removing notification");
			
			int notificationTitle = R.string.download_successful;
			Exception exception = request.getException();
			if (exception != null) {
				downloadGameSchemeSuccess = false;
				// default error message
				notificationTitle = R.string.failed_download;
				
				if (exception instanceof SocketTimeoutException) {
					notificationTitle = R.string.failed_download;
				}
			} else {
				downloadGameSchemeSuccess = true;
			}
			
			final Intent intent = new Intent(GameSchemeDownloaderService.this, DashboardActivity.class);
	        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			
			final NotificationCompat.Builder builder = new NotificationCompat.Builder(GameSchemeDownloaderService.this)
			.setOngoing(false)
            .setContentTitle(getResources().getText(notificationTitle))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.icon)
            .setAutoCancel(true);	
			
			Notification notification = builder.build();
			
			final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
			
			downloadingGameScheme = false;
			// we are done so stop the service
			GameSchemeDownloaderService.this.stopSelf();
		}
    };
}
