package com.minder.app.tf2backpack;

import java.io.File;
import android.content.Context;

public class FileCache {  
    private File cacheDir;
    
    public FileCache(Context context){
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "TF2BackpackViewer/Cache");
        else
            cacheDir = context.getCacheDir();
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public File getFile(String url){
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        String filename = String.valueOf(url.hashCode());
        return new File(cacheDir, filename);
        
    }
    
    public void clear() {
        File[] files = cacheDir.listFiles();
        for(File f : files)
            f.delete();
    }

}