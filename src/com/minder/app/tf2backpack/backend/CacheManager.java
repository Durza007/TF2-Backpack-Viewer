package com.minder.app.tf2backpack.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.Log;

import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.Util;

public class CacheManager {	
    private File cacheDir;
    private Exception exception;
    
    public Exception getException() {
    	return this.exception;
    }
    
    public CacheManager(Context context){
    	this.exception = null;
    	
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
        	cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "TF2BackpackViewer/Cache/");
        }
        else {
        	cacheDir = context.getCacheDir();
        }
        
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public String getString(String url) {
    	return getString(null, url);
    }
    
    public String getString(String folder, String url) {
    	String filename = Util.md5Hash(url);
    	
    	File f = null;
    	if (folder != null) {
    		f = new File(cacheDir, folder + "-" + filename);
    	} else {
    		f = new File(cacheDir, folder + "-" + filename);
    	}
    	
    	StringBuffer buffer = null;
    	FileInputStream fi = null;
    	try {
	    	fi = new FileInputStream(f);
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(fi));
	    	
			String row = "";
			buffer = new StringBuffer();
			while ((row = reader.readLine()) != null) {
				buffer.append(row);
				buffer.append("\n");
			}
    	} 
    	catch (IOException e) {
    		// could not load file - not really a big deal
    		exception = e;
    	} 
    	finally {
    		try {
    			if (fi != null) {
    				fi.close();
    			}
			} catch (IOException e) {
				// still not a really big deal
			}
    	}
    	
    	if (buffer != null) {
    		return buffer.toString();
    	} else {
    		return null;
    	}
    }
    
    public void cacheString(String url, String data) {
    	cacheString(null, url, data);
    }
    
    public void cacheString(String folder, String url, String data) {
    	String filename = Util.md5Hash(url);
    	
    	File f = null;
    	if (folder != null) {
    		f = new File(cacheDir, folder + "-" + filename);
    	} else {
    		f = new File(cacheDir, folder + "-" + filename);
    	}
		
    	FileOutputStream fos = null;
    	try {
			fos = new FileOutputStream(f);
			fos.write(data.getBytes("UTF-8"));
			fos.flush();
    	}
    	catch (IOException e) {
    		if (BuildConfig.DEBUG) {
    			e.printStackTrace();
    		}
    		exception = e;
    	} 
    	finally {
    		try {
    			if (fos != null) {
    				fos.close();
    			}
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }
    
    public void clear(){
    	Log.v("TF2Backpack", "CacheManager: Cache cleared");
        File[] files = cacheDir.listFiles();
        for(File f : files)
            f.delete();
    }
}
