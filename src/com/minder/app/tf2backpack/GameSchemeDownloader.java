package com.minder.app.tf2backpack;

import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.minder.app.tf2backpack.GameSchemeParser.ImageInfo;

public class GameSchemeDownloader implements Runnable{	
	public static final int MSG_IMAGE_TOTAL = 0;
	public static final int MSG_IMAGE_TICK = 1;
	public static final int MSG_SCHEME_START = 2;
	public static final int MSG_SCHEME_END = 3;
	public static final int MSG_ERROR = 4;
	public static final int MSG_FINISHED = 5;
	
	private Handler mHandler;
	
	private int totalDownloads;
	private int numberOfDownloads;
	
	public GameSchemeDownloader(Handler handler){
		mHandler = handler;
	}
	
	public void run() {
		Download();
	}
    
    public void Download(){
		Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
				case HttpConnection.DID_START: {
					mHandler.sendMessage(Message.obtain(mHandler, MSG_SCHEME_START));
					break;
				}
				case HttpConnection.DID_SUCCEED: {
					GameSchemeParser gs = new GameSchemeParser((String)message.obj, null);
					mHandler.sendMessage(Message.obtain(mHandler, MSG_SCHEME_END));
					DownloadImages(gs.GetImageURList());
					break;
				}
				case HttpConnection.DID_ERROR: {
					Exception e = (Exception) message.obj;
					e.printStackTrace();
					break;
				}
				}
			}
		};
		new HttpConnection(handler)
		.get("http://api.steampowered.com/ITFItems_440/GetSchema/v0001/?key=" + 
			Util.GetAPIKey() + "&format=json&language=en");
    }
    
    private void DownloadImages(ArrayList<ImageInfo> imageList){
		Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
				case HttpConnection.DID_START:{
					break;
				}
				
				case HttpConnection.DID_SUCCEED:{
					numberOfDownloads++;
				}
				
				case HttpConnection.DID_ERROR: {
					numberOfDownloads++;
					Exception e = (Exception) message.obj;
					e.printStackTrace();
					Log.d("Error", "failed to download weapon image");
					break;
				}
				default: {
					numberOfDownloads++;
					mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_TICK, message.obj));
					break;
				}
				}
				if (numberOfDownloads == totalDownloads){
					mHandler.sendMessage(Message.obtain(mHandler, MSG_FINISHED));
					// Save that we have downloaded the files
					//Editor editor = GameSchemeDownloader.this.getSharedPreferences("gamefiles", 0).edit();
					//editor.putBoolean("downloaded", true);
					//editor.commit();
				}
			}
		};
		for (ImageInfo ii : imageList){
			new HttpConnection(handler).bitmap(ii.getLink(), ii.getDefIndex());
			totalDownloads++;
		}
		mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_TOTAL, totalDownloads));    
	}
   
}
