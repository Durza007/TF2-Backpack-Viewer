package com.minder.app.tf2backpack.downloadmanager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import com.minder.app.tf2backpack.App;

import android.content.Context;

/**
 * A simple download manager that handles downloads and caching
 * 
 * @author Patrik Minder
 */
public class DownloadManager {
	private static DownloadManager instance;
	
	private CacheManager cacheManager;
	private Context context;
	
	private DownloadManager(Context context) {
		this.context = context;
		//cacheManager = new CacheManager(context);
	}
	
	/**
	 * Returns the file data for a file synchronized
	 * @param url The url of the file
	 * @param cacheTime How long the file will be cached and for how old a cached file to load can be
	 * @return An InputStream pointing to the file
	 * @throws IOException If the file cant be opened
	 * @throws MalformedURLException If the url is malformed
	 */
	public String getFileDirect(String url, int cacheTimeSeconds) throws MalformedURLException, IOException {
		File f = cacheManager.getFile(url, cacheTimeSeconds);
		
		if (f != null) {
			if (f.exists()) {
				// TODO Load file
			}
		} 
		else 
		{
			// TODO download file
		}
		
		return null;	
	}
	
	public static DownloadManager getInstance() {
		if (instance == null) {
			instance = new DownloadManager(App.getAppContext());
		}
		return instance;
	}
}
