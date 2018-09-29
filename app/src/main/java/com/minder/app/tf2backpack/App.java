package com.minder.app.tf2backpack;

import com.minder.app.tf2backpack.backend.DataManager;

import android.app.Application;
import android.content.Context;

/**
 * Class that gives a static reference to my application context
 */
public class App extends Application {
    private static Context context;
    private static DataManager dataManager;

    public void onCreate(){
    	super.onCreate();
        context = getApplicationContext();
        dataManager = new DataManager(context);

        if (!dataManager.isGameSchemeReady() ||
                !dataManager.isGameSchemeUpToDate()) {
           dataManager.requestSchemaFilesOverviewDownload();
        }
    }

    public static Context getAppContext() {
		return context;
	}
    
    public static DataManager getDataManager() {
    	return dataManager;
    }
}
