package com.minder.app.tf2backpack;

import java.io.File;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.frontend.DashBoard;

public class GameSchemeDownloaderService extends Service {
	private static final String DEBUG_TAG = "GameSchemeDownloaderService";
	private static final int DOWNLOAD_NOTIFICATION_ID = 1337;
	private Request gameSchemeRequest;
 
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
	    return START_STICKY;
	}
 
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void handleCommand(Intent intent) {
    	boolean refreshImages = intent.getExtras().getBoolean("refreshImages");
    	
    	// start the request
    	App.getDataManager().requestSchemaFilesDownload(gameSchemeListener, refreshImages);
    }
    
    private void saveGameSchemeDownloaded() {
    	SharedPreferences gamePrefs = this.getSharedPreferences("gamefiles", MODE_PRIVATE);
        Editor editor = gamePrefs.edit();
        editor.putInt("download_version", DataManager.CURRENT_GAMESCHEMA_VERSION);
        editor.commit();
    }
    
    AsyncTaskListener gameSchemeListener = new AsyncTaskListener() {  	
    	@TargetApi(16)
		public void onPreExecute() {
			final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			
			final Intent intent = new Intent(GameSchemeDownloaderService.this, DashBoard.class);
	        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			
			final NotificationCompat.Builder builder = new NotificationCompat.Builder(GameSchemeDownloaderService.this)
			.setOngoing(true)
            .setProgress(100, 0, true)
            .setContentTitle(getResources().getText(R.string.starting_download))
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.stat_sys_download);
			
			Notification notification = builder.build();
			
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_TAG, "Showing notification");
			notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
		}

		public void onProgressUpdate(ProgressUpdate progress) {	
			final Intent intent = new Intent(GameSchemeDownloaderService.this, DashBoard.class);
	        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			
			if (progress.updateType == DataManager.PROGRESS_PARSING_SCHEMA) {
				final NotificationCompat.Builder builder = new NotificationCompat.Builder(GameSchemeDownloaderService.this)
				.setOngoing(true)
	            .setProgress(100, 0, true)
	            .setContentTitle(getResources().getText(R.string.parsing_schema))
	            .setContentIntent(pendingIntent)
	            .setSmallIcon(android.R.drawable.stat_sys_download);
				
				Notification notification = builder.build();
				
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_TAG, "Updating notification");
				final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
				
			} else if (progress.updateType == DataManager.PROGRESS_DOWNLOADING_IMAGES) {
				// this means game files are downloaded
				saveGameSchemeDownloaded();

				final NotificationCompat.Builder builder = new NotificationCompat.Builder(GameSchemeDownloaderService.this)
				.setOngoing(true)
	            .setProgress(progress.totalCount, progress.count, false)
	            .setContentTitle(getResources().getText(R.string.downloading_images))
	            .setContentIntent(pendingIntent)
	            .setSmallIcon(android.R.drawable.stat_sys_download);
				
				if (Build.VERSION.SDK_INT <= 10) {
					builder.setSubText(progress.count + "/" + progress.totalCount);
				}
				
				Notification notification = builder.build();
				
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_TAG, "Updating notification");
				final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
			}
		}

		public void onPostExecute(Object object) {
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_TAG, "Removing notification");
			
			final Intent intent = new Intent(GameSchemeDownloaderService.this, DashBoard.class);
	        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			
			final NotificationCompat.Builder builder = new NotificationCompat.Builder(GameSchemeDownloaderService.this)
			.setOngoing(false)
            .setContentTitle(getResources().getText(R.string.download_successful))
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true);	
			
			Notification notification = builder.build();
			
			final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
			
			// we are done so stop the service
			GameSchemeDownloaderService.this.stopSelf();
		}
    };
}
