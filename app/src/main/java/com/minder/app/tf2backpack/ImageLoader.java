package com.minder.app.tf2backpack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class ImageLoader {
    public interface ImageLoadedInterface {
        void imageReady(String url, Bitmap bitmap);
    }
    MemoryCache memoryCache = new MemoryCache();
    FileCache fileCache;
    private Activity activity;
    private PhotosLoader photoLoaderThread;
    
    public ImageLoader(Activity activity) {
        this.activity = activity;
    	createThread();
    	// TODO lazy start on this thread?
    	photoLoaderThread.start();
        
        fileCache = new FileCache(activity);
    }
    
    final static int stub_id = R.drawable.unknown;
    
    public Bitmap displayImage(String url, ImageLoadedInterface callbackObj, int requiredSize, boolean isLocal)
    {
        if (requiredSize < 1) {
            throw new RuntimeException("requiredSize cannot be lower than 1");
        }
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null) {
            return bitmap;
        } else {
            queuePhoto(url, callbackObj, requiredSize, isLocal);
            return null;
        }    
    }

    public Bitmap displayImageWithCachedTemporary(String mainUrl, String tempUrl, ImageLoadedInterface callbackObj, int requiredSize, boolean isLocal)
    {
        if (mainUrl == null) return null;
        if (requiredSize < 1) {
            throw new RuntimeException("requiredSize cannot be lower than 1");
        }

        Bitmap bitmap = memoryCache.get(mainUrl);
        if (bitmap != null) {
            return bitmap;
        } else {
            if (tempUrl != null) {
                bitmap = memoryCache.get(tempUrl);
                if (bitmap != null) {
                    callbackObj.imageReady(mainUrl, bitmap);
                }
                else {
                    File f = fileCache.getFile(tempUrl);

                    //from SD cache
                    bitmap = decodeFile(f, requiredSize);
                    if (bitmap != null) {
                        callbackObj.imageReady(mainUrl, bitmap);
                    }
                }
            }
            queuePhoto(mainUrl, callbackObj, requiredSize, isLocal);
            return null;
        }
    }
        
    private void queuePhoto(String url, ImageLoadedInterface callbackObj, int requiredSize, boolean isLocal)
    {
        synchronized(photosQueue.photosToLoad){
            //This ImageView may be used for other images before. So there may be some old tasks in the queue. We need to discard them. 
            //photosQueue.clean(adapter);
            PhotoToLoad p = new PhotoToLoad(url, callbackObj, requiredSize, isLocal);

            photosQueue.clean(callbackObj);
            photosQueue.photosToLoad.push(p);
            photosQueue.photosToLoad.notifyAll();
        }
    }
    
    private Bitmap getBitmap(String url, int requiredSize, boolean isLocal)
    {
    	if (isLocal) {
			try {
				FileInputStream in = activity.openFileInput(url);
				
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
        Bitmap b = decodeFile(f, requiredSize);
        if(b != null)
            return b;

        //from web
        HttpURLConnection conn = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            int itemColor = 0;
            int itemColor2 = 0;
            if (url.startsWith("paint:")) {
                int secondColon = url.indexOf(":", 6);
                int thirdColon = url.indexOf(":", secondColon + 1);
                itemColor = Integer.parseInt(url.substring(6, secondColon));
                itemColor2 = Integer.parseInt(url.substring(secondColon + 1, thirdColon));
                url = url.substring(thirdColon + 1);
            }

            URL imageUrl = new URL(url);
            conn = (HttpURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            is = conn.getInputStream();
            if (itemColor != 0 || itemColor2 != 0) {
                b = BitmapFactory.decodeStream(is);

                boolean isLarge = b.getWidth() > 128;
                // General paint can stuff
                Bitmap newBitmap = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColorFilter(new LightingColorFilter((0xFF << 24) | itemColor, 1));
                // draw paintcan
                canvas.drawBitmap(b, 0, 0, null);

                final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                bitmapOptions.inScaled = false;
                if (itemColor2 != 0) {
                    Bitmap teamPaintRed = BitmapFactory.decodeResource(this.activity.getResources(), isLarge ? R.drawable.teampaint_red_mask_large : R.drawable.teampaint_red_mask, bitmapOptions);
                    Bitmap teamPaintBlue = BitmapFactory.decodeResource(this.activity.getResources(), isLarge ? R.drawable.teampaint_blu_mask_large : R.drawable.teampaint_blu_mask, bitmapOptions);
                    // draw first paint color
                    canvas.drawBitmap(teamPaintRed, null, new Rect(0, 0, b.getWidth(), b.getHeight()), paint);
                    // draw second paint color
                    paint.setColorFilter(new LightingColorFilter((0xFF << 24) | itemColor2, 1));
                    canvas.drawBitmap(teamPaintBlue, null, new Rect(0, 0, b.getWidth(), b.getHeight()), paint);
                }
                else {
                    Bitmap paintColor = BitmapFactory.decodeResource(this.activity.getResources(), isLarge ? R.drawable.paintcan_paintcolor_large : R.drawable.paintcan_paintcolor, bitmapOptions);
                    // draw paint color
                    canvas.drawBitmap(paintColor, null, new Rect(0, 0, b.getWidth(), b.getHeight()), paint);
                }

                // recycle old image
                b.recycle();
                b = newBitmap;

                os = new FileOutputStream(f);
                boolean saved = b.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                if (!saved) {
                    Log.e(Util.GetTag(), "Failed to save image: " + url);
                }
            }
            else {
                os = new FileOutputStream(f);
                Util.CopyStream(is, os);
                b = decodeFile(f, requiredSize);
            }
        } catch (Exception ex) {
            Log.e(Util.GetTag(), "Failed to download image: " + url + ": " + ex.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) conn.disconnect();
        }
        return b;
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f, int requiredSize) {
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
        public final ImageLoadedInterface callbackObj;
        public final int requiredSize;
        public final boolean isLocal;
        
        public PhotoToLoad(String url, ImageLoadedInterface callbackObj, int requiredSize, boolean isLocal){
            this.url = url;
            this.callbackObj = callbackObj;
            this.requiredSize = requiredSize;
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
        public void clean(ImageLoadedInterface callbackObj)
        {
            for (int j = 0; j < photosToLoad.size();){
                if (photosToLoad.get(j).callbackObj == callbackObj)
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

                        Bitmap bmp = getBitmap(photoToLoad.url, photoToLoad.requiredSize, photoToLoad.isLocal);
                        memoryCache.put(photoToLoad.url, bmp);
                        
                        BitmapDisplayer bd = new BitmapDisplayer(photoToLoad.url, bmp, photoToLoad.callbackObj);
                        activity.runOnUiThread(bd);
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
        final String url;
        final Bitmap bitmap;
        final ImageLoadedInterface callbackObj;
        public BitmapDisplayer(String url, Bitmap b, ImageLoadedInterface a) {
            this.url = url;
        	bitmap = b;
            callbackObj = a;
        }
        
        public void run() {
            callbackObj.imageReady(url, bitmap);
        }
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }

}
