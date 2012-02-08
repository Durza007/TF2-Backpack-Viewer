package com.minder.app.tf2backpack.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.Util;

public class CacheManager {
	private static CacheManager instance;
	
	private Context context;
    private File cacheDir;
    
    private CacheManager(Context context){
    	this.context = context;
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "TF2BackpackViewer/Cache");
        else
            cacheDir = context.getCacheDir();
        
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public File getFile(String url) {
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        String filename = Util.md5Hash(url);
        File f = new File(cacheDir, filename);
        return f;
    }
    
    public File getFile(String url, long cacheTimeSeconds) {
    	File f = getFile(url);
    	
    	if (f.lastModified() + (cacheTimeSeconds * 1000) > System.currentTimeMillis()) {
    		return f;
    	} else {
    		return null;
    	}
    }
    
    public void cacheFile(String url, String data) throws IOException {
    	String filename = Util.md5Hash(url);
    	
		File f = new File(cacheDir, filename);
		
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(data.getBytes("UTF-8"));
		fos.flush();
		fos.close();
    }
    
    public void cacheBitmap(Bitmap bm, String url) throws IOException {
    	String filename = Util.md5Hash(url);
    	
		File f = new File(cacheDir, filename);
		
		FileOutputStream fos = new FileOutputStream(f);
		
		boolean saved = bm.compress(CompressFormat.PNG, 100, fos);
		fos.flush();
		fos.close();
    }
    
    public void clear(){
    	Log.v("TF2Backpack", "CacheManager: Cache cleared");
        File[] files = cacheDir.listFiles();
        for(File f : files)
            f.delete();
    }

	public static CacheManager getInstance() {
		if (instance == null) {
			instance = new CacheManager(App.getAppContext());
		}
		return instance;
	}
}
