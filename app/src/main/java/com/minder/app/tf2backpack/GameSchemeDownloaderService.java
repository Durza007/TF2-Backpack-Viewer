package com.minder.app.tf2backpack;

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
	private static final String DEBUG_TAG = "DownloaderService";

	private static final int DOWNLOAD_NOTIFICATION_ID = 1337;
	public static boolean downloadGameSchemeSuccess = false;
	public static long totalBytes;
	public static long currentBytes;
	public static int totalImages;
	public static int currentAmountImages;
	public static int currentTaskStringId;
	
	private Request gameSchemeRequest;
	private boolean downloadHighresImages;

	
	public static void startGameSchemeDownload(Activity starterActivity, boolean refreshImages, boolean highresImages) {
    	Intent intent = new Intent(starterActivity, GameSchemeDownloaderService.class);
    	intent.putExtra("refreshImages", refreshImages);
    	intent.putExtra("highresImages", highresImages);
    	starterActivity.startService(intent);
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
    	gameSchemeRequest = null;//App.getDataManager().requestSchemaFilesDownload(gameSchemeListener, refreshImages, downloadHighresImages);
    }
    
    AsyncTaskListener gameSchemeListener = new AsyncTaskListener() {
    	private Notification notification;
    	
		public void onPreExecute() {
			
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
				
				Util.handleNetworkException(exception, GameSchemeDownloaderService.this);
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

			// we are done so stop the service
			GameSchemeDownloaderService.this.stopSelf();
		}
    };
}
