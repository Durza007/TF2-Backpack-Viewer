package com.minder.app.tf2backpack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.BaseAdapter;

public class ImageLoader {
    MemoryCache memoryCache = new MemoryCache();
    FileCache fileCache;
    private int requiredSize;
    private PhotosLoader photoLoaderThread;
    
    public ImageLoader(Context context, int requiredSize) {
    	createThread();
    	// TODO lazy start on this thread?
    	photoLoaderThread.start();
        
        this.requiredSize = requiredSize;
        
        fileCache = new FileCache(context);
    }
    
    final static int stub_id = R.drawable.unknown;
    
    public Bitmap displayImage(String url, Activity activity, BaseAdapter adapter, boolean isLocal)
    {
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null) {
            return bitmap;
        } else {
            queuePhoto(url, activity, adapter, isLocal);
            return null;
        }    
    }
        
    private void queuePhoto(String url, Activity activity, BaseAdapter adapter, boolean isLocal)
    {
        synchronized(photosQueue.photosToLoad){
            //This ImageView may be used for other images before. So there may be some old tasks in the queue. We need to discard them. 
            photosQueue.clean(adapter);
            PhotoToLoad p = new PhotoToLoad(url, activity, adapter, isLocal);
            
            photosQueue.photosToLoad.push(p);
            photosQueue.photosToLoad.notifyAll();
        }
    }
    
    private Bitmap getBitmap(String url, Context context, boolean isLocal) 
    {
    	if (isLocal) {
			try {
				FileInputStream in = context.openFileInput(url);
				
				Bitmap image = BitmapFactory.decodeStream(in);
				if (image != null){
					Bitmap newImage = Bitmap.createScaledBitmap(image, requiredSize, requiredSize, false);
					
					if (newImage != image) {
						image.recycle();
					}
					
					image = newImage;
				} else {
					throw new FileNotFoundException();
				}
				return image;
			} catch (FileNotFoundException e) {
				// does not really matter
			}
    	}
        File f = fileCache.getFile(url);
        
        //from SD cache
        Bitmap b = decodeFile(f);
        if(b != null)
            return b;
        
        //from web
        try {
            Bitmap bitmap = null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            InputStream is = conn.getInputStream();
            OutputStream os = new FileOutputStream(f);
            Util.CopyStream(is, os);
            os.close();
            bitmap = decodeFile(f);
            return bitmap;
        } catch (Exception ex){
           return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);
            
            //Find the correct scale value. It should be the power of 2.
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while(true){
                if(width_tmp/2 < requiredSize || height_tmp/2 < requiredSize)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
    
    //Task for the queue
    private class PhotoToLoad {
        public final String url;
        public final Activity activity;
        public final BaseAdapter adapter;
        public final boolean isLocal;
        
        public PhotoToLoad(String u, Activity ac, BaseAdapter a, boolean isLocal){
            url = u; 
            activity = ac;
            adapter = a;
            this.isLocal = isLocal;
        }
    }
    
    PhotosQueue photosQueue = new PhotosQueue();
    
    public void stopThread()
    {
    	if (photoLoaderThread != null) {
    		photoLoaderThread.interrupt();
    	}
    }
    
    public void startThread() {
    	if (createThread()) {
    		photoLoaderThread.start();
    	}
    }
    
    /**
     * Creates a new thread
     * @return True if a new thread was created - false otherwise
     */
    private boolean createThread() {
    	if (photoLoaderThread == null || !photoLoaderThread.isAlive()) {
        	photoLoaderThread = new PhotosLoader();
        	photoLoaderThread.setName("PhotoLoader");
            photoLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);
            return true;
    	}
    	return false;
    }
    
    //stores list of photos to download
    class PhotosQueue
    {
        private Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();
        
        //removes all instances of this ImageView
        public void clean(BaseAdapter adapter)
        {
            for (int j = 0; j < photosToLoad.size();){
                if (photosToLoad.get(j).adapter == adapter)
                    photosToLoad.remove(j);
                else
                    ++j;
            }
        }
    }
    
    class PhotosLoader extends Thread {
        public void run() {
            try {
                while (true) {
                    //thread waits until there are any images to load in the queue
                    if(photosQueue.photosToLoad.size() == 0)
                    {
                        synchronized(photosQueue.photosToLoad){
                            photosQueue.photosToLoad.wait();
                        }
                    }
                    
                    if (photosQueue.photosToLoad.size() != 0)
                    {
                        PhotoToLoad photoToLoad;
                        synchronized(photosQueue.photosToLoad){
                            photoToLoad = photosQueue.photosToLoad.pop();
                        }

                        Bitmap bmp = getBitmap(photoToLoad.url, photoToLoad.activity, photoToLoad.isLocal);
                        memoryCache.put(photoToLoad.url, bmp);
                        
                        BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad.adapter);
                        photoToLoad.activity.runOnUiThread(bd);
                    }
                    if (Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
                //allow thread to exit
            }
        }
    }
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        BaseAdapter adapter;
        public BitmapDisplayer(Bitmap b, BaseAdapter a) {
        	bitmap = b;
        	adapter = a;
        }
        
        public void run() {
            adapter.notifyDataSetChanged();
        }
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }

}
