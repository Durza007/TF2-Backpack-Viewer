package com.minder.app.tf2backpack;

import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager.Request;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GameSchemeDownloaderService extends Service {
	private static final String DEBUG_TAG = "GameSchemeDownloaderService";
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
    	
    }
    
    AsyncTaskListener gameSchemeListener = new AsyncTaskListener() {
		public void onPreExecute() {
		}

		public void onProgressUpdate(Object object) {
		}

		public void onPostExecute(Object object) {
		}
    };

}
