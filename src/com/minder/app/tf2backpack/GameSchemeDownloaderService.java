package com.minder.app.tf2backpack;

import java.io.File;

import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager.Request;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

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
		public void onPreExecute() {
			NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		}

		public void onProgressUpdate(Object object) {
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
