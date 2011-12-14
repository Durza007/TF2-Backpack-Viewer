package com.minder.app.tf2backpack;

import android.app.Application;
import android.content.Context;

/**
 * Class that gives a static reference to my application context
 * 
 * @by Rohit Ghatol
 */
public class App extends Application{
    private static Context context;

    public void onCreate(){
    	super.onCreate();
        context = getApplicationContext();
    }

    public static Context getAppContext() {
		return context;
	}
}
