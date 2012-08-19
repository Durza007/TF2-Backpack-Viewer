package com.minder.app.tf2backpack.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.MemoryCache;
import com.minder.app.tf2backpack.Util;

public class CacheManager {	
    private File cacheDir;
    
    public CacheManager(Context context){
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
    
    public File getFile(String url) {
    	return getFile(null, url);
    }
    
    public File getFile(String folder, String url) {
    	String filename = Util.md5Hash(url);
    	
    	File f = null;
    	if (folder != null) {
    		f = new File(cacheDir, folder +"/" + filename);
    	} else {
    		f = new File(cacheDir, folder +"/" + filename);
    	}
        return f;
    }
    
    public void cacheFile(String url, String data) throws IOException {
    	String filename = Util.md5Hash(url);
    	
		File f = new File(cacheDir, filename);
		
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(data.getBytes("UTF-8"));
		fos.flush();
		fos.close();
    }
    
    public void clear(){
    	Log.v("TF2Backpack", "CacheManager: Cache cleared");
        File[] files = cacheDir.listFiles();
        for(File f : files)
            f.delete();
    }
}
