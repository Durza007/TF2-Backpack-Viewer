package com.minder.app.tf2backpack;

import com.minder.app.tf2backpack.backend.DataBaseHelper;
import com.minder.app.tf2backpack.backend.DataManager;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * Class that gives a static reference to my application context
 */
public class App extends Application {
    private static Context context;
    private static DataBaseHelper databaseHelper;
    private static DataManager dataManager;

    public void onCreate(){
    	super.onCreate();
        context = getApplicationContext();
        databaseHelper = new DataBaseHelper(context);
        dataManager = new DataManager(context);

        if (!dataManager.isGameSchemeReady() ||
                !dataManager.isGameSchemeUpToDate()) {
           dataManager.requestSchemaFilesDownload(false);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    public static Context getAppContext() {
		return context;
	}
    
    public static DataManager getDataManager() {
    	return dataManager;
    }

    public static DataBaseHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
