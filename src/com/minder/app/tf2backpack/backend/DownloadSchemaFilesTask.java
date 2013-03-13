package com.minder.app.tf2backpack.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.AsyncTask;
import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.GameSchemeParser.ImageInfo;
import com.minder.app.tf2backpack.backend.HttpConnection.DownloadProgressListener;

/**
 * This behemoth downloads all the tf2 schema files
 */
public class DownloadSchemaFilesTask extends AsyncTask<Void, ProgressUpdate, Void> {
	private final static int NUMBER_OF_IMAGE_THREADS = 4;
	private final static int VALUE_DIFF_FOR_PROGRESS_UPDATE = 7;
	
	private Context context;
	private final AsyncTaskListener listener;
	private final Request request;
	private final boolean refreshImages;
	private final boolean highresImages;
	
	private final Object imageListLock = new Object();
	private ArrayList<ImageInfo> imageUrlList;
	
	private final Object resultLock = new Object();
	private int downloadedImages;
	private int valueSinceLastProgressUpdate;
	private int finishedThreads;
	
	private Bitmap paintColor;
	private Bitmap teamPaintRed;
	private Bitmap teamPaintBlue;
	
	public DownloadSchemaFilesTask(Context context, AsyncTaskListener listener, Request request, boolean refreshImages, boolean downloadHighresImages) {
		this.context = context;
		this.listener = listener;
		this.request = request;
		this.refreshImages = refreshImages;
		this.highresImages = downloadHighresImages;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		if (refreshImages)
			deleteItemImages();
		
		String data = null;//getSchemaFromServer();
		
		HttpConnection connection = null;
		if (data == null) {
			connection = 
				HttpConnection.string("http://api.steampowered.com/IEconItems_440/GetSchema/v0001/?key=" + 
					Util.GetAPIKey() + "&format=json&language=en");
			
			data = (String) connection.execute(new DownloadProgressListener() {
				private int totalSize;
				public void totalSize(long totalSize) {
					this.totalSize = (int)totalSize;
					publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE, this.totalSize, 0));
				}
				
				public void progressUpdate(long currentSize) {
					publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE, totalSize, (int)currentSize));
				}
			});
		}
		
		if (data != null) {
			publishProgress(new ProgressUpdate(DataManager.PROGRESS_PARSING_SCHEMA, 0, 0));
			
			GameSchemeParser gs = 
				new GameSchemeParser(data, this.context, highresImages);
			// set to null as soon as possible since it is holding >2 MB
			data = null;
			
			if (gs.error != null){
				// TODO handle error
			}
			
			// download images
			if (gs.getImageURList() != null){
		    	imageUrlList = gs.getImageURList();
		    	// set this to null since it as pretty big object
		    	gs = null;
		    	
		    	final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		    	bitmapOptions.inScaled = false;
		    	paintColor = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.paintcan_paintcolor, bitmapOptions);
		    	teamPaintRed = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.teampaint_red_mask, bitmapOptions);
		    	teamPaintBlue = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.teampaint_blu_mask, bitmapOptions);
				
		    	long start = System.nanoTime();
                for (int index = 0; index < imageUrlList.size(); index++){
                    final File file = new File(this.context.getFilesDir().getPath() + "/" + imageUrlList.get(index).getDefIndex() + ".png");
                    if (file.exists()){
                    	imageUrlList.remove(index);
                    	index--;
                    }
                }
                int totalDownloads = imageUrlList.size();
                Log.d("DataManager", "File image check: " + (System.nanoTime() - start) / 1000000 + " ms");
                
		    	publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_IMAGES_UPDATE, totalDownloads, 0));
		    	
		    	// start up some download threads
		    	for (int index = 0; index < NUMBER_OF_IMAGE_THREADS; index++) {
		    		ImageDownloader downloader = new ImageDownloader(index);
		    		Thread thread = new Thread(downloader);
		    		thread.setName("ImageDownloadThread #" + index);
		    		thread.start();
		    	}
		    	
		    	// wait for download threads to finish
		    	while (true) {
		    		synchronized (resultLock) {
		    			valueSinceLastProgressUpdate = downloadedImages;
		    			publishProgress(new ProgressUpdate(
		    					DataManager.PROGRESS_DOWNLOADING_IMAGES_UPDATE, 
		    					totalDownloads, 
		    					downloadedImages));
		    			
		    			if (finishedThreads == NUMBER_OF_IMAGE_THREADS) {
		    				break;
		    			} else {
		    				try {
								resultLock.wait();
							} catch (InterruptedException e) {
								// Doesn't matter
							}
		    			}
		    		}
		    	}
		    	
		    	paintColor.recycle();
		    	teamPaintBlue.recycle();
		    	teamPaintRed.recycle();
			} else {
				// handle error
			}
			gs = null;
			System.gc();
			Log.d("Dashboard", "GameScheme download complete");
		} else {
			// handle error
			if (connection != null)
				request.exception = connection.getException();
		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(ProgressUpdate... progress) {
		listener.onProgressUpdate(progress[0]);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		listener.onPostExecute(request);
		App.getDataManager().removeRequest(request);
	}
	
	/*private String getSchemaFromServer() {
		String result = null;
		long start = System.currentTimeMillis();
		try {
			Log.d("GameSchemeDownloader", "Trying data server");
			DataServerConnection connection = new DataServerConnection();
			Log.d("GameSchemeDownloader", "Downloading from data server");
			connection.getOutputStream().write(1);
			
			int totalSize = (int)connection.readLong();
			publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE, totalSize, 0));
			
			/*byte[] data = new byte[totalSize];
			byte[] buffer = new byte[(int) HttpConnection.BYTE_UPDATE_INTERVALL];
			
			InputStream in = connection.getInputStream();
			int byteCount = 0;
			long lastProgressUpdate = System.currentTimeMillis();
			while (true) {
				int bytesRead = in.read(buffer);
				
				if (bytesRead == -1)
					break;
				
				System.arraycopy(buffer, 0, data, byteCount, bytesRead);
				
				byteCount += bytesRead;
				
				if (System.currentTimeMillis() > lastProgressUpdate + MIN_TIME_BETWEEN_PROGRESS_UPDATES_MS) {
					lastProgressUpdate = System.currentTimeMillis();
					publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE, totalSize, byteCount));
				}
			}*/
			
			/*BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			StringBuilder sb = new StringBuilder();
			String line;

			long currentByteCount = 0;
			long lastUpdate = 0;
			long lastProgressUpdate = 0;
			while ((line = reader.readLine()) != null) {
				// add 1 to account for missing line endings
				//currentByteCount += (line.getBytes().length) + 1;
				currentByteCount += (line.length()) + 1;
				
				if (System.currentTimeMillis() > lastProgressUpdate + MIN_TIME_BETWEEN_PROGRESS_UPDATES_MS) {
					lastProgressUpdate = System.currentTimeMillis();
					publishProgress(new ProgressUpdate(DataManager.PROGRESS_DOWNLOADING_SCHEMA_UPDATE, totalSize, (int)currentByteCount));
				}

				sb.append(line);
			}
			
			result = sb.toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Log.d("GameSchemeDownload", "Data server download time: " + (System.currentTimeMillis() - start) + " ms");
		return result;
	}*/
	
    private class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e("UncaughtException", "Got an uncaught exception: " + ex.toString());
            if(ex.getClass().equals(OutOfMemoryError.class))
            {
                try {
                    android.os.Debug.dumpHprofData("/sdcard/dump.hprof");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(1337);
            }
            ex.printStackTrace();
        }
    }

	
    /**
     * Deletes all item images
     */
    private void deleteItemImages() {
		File file = new File(context.getFilesDir().getPath());
		if (file.isDirectory()) {
	        String[] children = file.list();
	        for (int i = 0; i < children.length; i++) {
	            new File(file, children[i]).delete();
	        }
	    }

    }
	
	private class ImageDownloader implements Runnable {
		private int index;
		
		public ImageDownloader(int index) {
			this.index = index;
		}
		
		public void run() {
			// TODO probably remove this? can't remember why it is here
			// Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
			while (true) {
				ImageInfo imageInfo = null;
				synchronized (imageListLock) {
					if (!imageUrlList.isEmpty()) {
						imageInfo = imageUrlList.remove(0);
					} else {
						break;
					}
					imageListLock.notifyAll();
				}
				
				// TODO Better image download error handling
				Object data = null;
				if (imageInfo.getLink().length() != 0) {
					HttpConnection conn = HttpConnection.bitmap(imageInfo.getLink());
					data = conn.execute(null);
				}
				if (data != null) {
					// save
					saveImage((Bitmap)data, imageInfo);
					data = null;
				} else {
					if (BuildConfig.DEBUG) {
						Log.i("DataManager", "Failed to download image with id: " + imageInfo.getDefIndex());
					}
				}
				
				// Tell everybody else that we have downloaded a image
				synchronized (resultLock) {
					downloadedImages++;
					if (downloadedImages >= valueSinceLastProgressUpdate + VALUE_DIFF_FOR_PROGRESS_UPDATE)
						resultLock.notifyAll();
				}
			}
			
			// tell the world we are done here
			synchronized (resultLock) {
				finishedThreads++;
				resultLock.notifyAll();
			}
		}
		
		public void saveImage(Bitmap image, ImageInfo imageInfo) {
	    	boolean isPaintCan = false;
	    	boolean isTeamPaintCan = false;
			
	    	// get some info about the image
			if (imageInfo.getColor() != 0) {
				isPaintCan = true;

				if (imageInfo.getColor2() != 0) {
					isTeamPaintCan = true;
				}
			} else {
				isPaintCan = false;
			}
			
			try {
				if (image != null){
					if (isPaintCan){
						if (!isTeamPaintCan) {
							// Regular paintcan
							Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
							Canvas canvas = new Canvas(newBitmap);
							Paint paint = new Paint();
							paint.setColorFilter(new LightingColorFilter((0xFF << 24) | imageInfo.getColor(), 1));
							// draw paintcan
							canvas.drawBitmap(image, 0, 0, null);
							// draw paint color
							canvas.drawBitmap(paintColor, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);
							// recycle old image
							image.recycle();
							image = newBitmap;
						} else {
							// Team-paintcan
							Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
							Canvas canvas = new Canvas(newBitmap);
							Paint paint = new Paint();
							paint.setColorFilter(new LightingColorFilter((0xFF << 24) | imageInfo.getColor(), 1));
							// draw paintcan
							canvas.drawBitmap(image, 0, 0, null);
							// draw first paint color
							canvas.drawBitmap(teamPaintRed, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);	
							// draw second paint color
							paint.setColorFilter(new LightingColorFilter((0xFF << 24) | imageInfo.getColor2(), 1));
							canvas.drawBitmap(teamPaintBlue, null, new Rect(0, 0, image.getWidth(), image.getHeight()), paint);
							
							image.recycle();
							image = newBitmap;
						}
					}
					FileOutputStream fos = context.openFileOutput(imageInfo.getDefIndex() + ".png", 0);
					boolean saved = image.compress(CompressFormat.PNG, 100, fos);
					fos.flush();
					fos.close();
					image.recycle();
					// TODO maybe check if saved was false
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}