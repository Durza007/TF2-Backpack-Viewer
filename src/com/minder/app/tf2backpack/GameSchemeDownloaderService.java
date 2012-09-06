package com.minder.app.tf2backpack;

import java.io.File;

import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

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
    	// start the request
    	App.getDataManager().requestSchemaFilesDownload(gameSchemeListener);
    }
    
    AsyncTaskListener gameSchemeListener = new AsyncTaskListener() {
    	private Notification notification;
    	
    	@TargetApi(16)
		public void onPreExecute() {
			final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			
			// build the notification differently depending on which platform
			// we are running on
			if (android.os.Build.VERSION.SDK_INT >= 11) {
			// configure the notification
				
				final Notification.Builder builder = new Notification.Builder(GameSchemeDownloaderService.this)
				.setOngoing(true)
				.setContent(new RemoteViews(getApplicationContext().getPackageName(),
                        R.layout.download_progress_notification));
				
				if (android.os.Build.VERSION.SDK_INT >= 16) {
					notification = builder.build();
				} else {
					notification = builder.getNotification();
				}
			} else {
				// configure the notification
				this.notification = new Notification();
				notification.contentView = new RemoteViews(getApplicationContext().getPackageName(),
                        R.layout.download_progress_notification);
				notification.flags = Notification.FLAG_ONGOING_EVENT;
			}
			
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_TAG, "Showing notification");
			notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
		}

		public void onProgressUpdate(ProgressUpdate progress) {
			if (progress.updateType == DataManager.PROGRESS_DOWNLOADING_IMAGES) {
				notification.contentView.setTextViewText(R.id.textViewStatus, getResources().getText(R.string.downloading_images));
				notification.contentView.setProgressBar(R.id.progressBarDownload, progress.totalCount, 0, false);
				
				final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
			}
		}

		public void onPostExecute(Object object) {
		}
    };

    /**
     * Deletes all item images
     */
    private void deleteItemImages() {
		File file = new File(this.getFilesDir().getPath());
		if (file.isDirectory()) {
	        String[] children = file.list();
	        for (int i = 0; i < children.length; i++) {
	            new File(file, children[i]).delete();
	        }
	    }

    }
}
