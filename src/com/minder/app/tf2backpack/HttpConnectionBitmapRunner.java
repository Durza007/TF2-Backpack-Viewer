package com.minder.app.tf2backpack;

import com.minder.app.tf2backpack.DashBoard.ImageSaverTask;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

public class HttpConnectionBitmapRunner extends HttpConnection {
	private Runnable imageRunner;
	private DashBoard instance;
	
	public HttpConnectionBitmapRunner(Handler handler, DashBoard dashBoard) {
		super(handler);
		
		this.instance = dashBoard;
	}
	
	@Override
	public void sendResult(int message, Object result) {
		ImageSaverTask imageSaver = instance.new ImageSaverTask((Bitmap)result, message);
		imageSaver.run();
		if (message == DID_ERROR) {
			handler.sendMessage(Message.obtain(handler, message, result));
		} else {
			handler.sendMessage(Message.obtain(handler, message, null));
		}
	}
}
