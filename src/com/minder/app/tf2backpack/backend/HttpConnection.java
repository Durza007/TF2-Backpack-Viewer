package com.minder.app.tf2backpack.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.minder.app.tf2backpack.BuildConfig;

public class HttpConnection {
	public static interface DownloadProgressListener {
		public void totalSize(long totalSize);
		public void progressUpdate(long currentSize);
	}
	public final static long MIN_TIME_BETWEEN_PROGRESS_UPDATES_MS = 400;
	
	private HttpClient httpClient;
	private Exception exception;
	private String url;
	private boolean isBitmap = false;
	
	/**
	 * Will return the latest exception that was raised
	 * @return The latest exception - null if none exists
	 */
	public Exception getException() {
		return exception;
	}
	
	/**
	 * Constructs a http connection for downloading a string
	 * @param url
	 * @return A HttpConnection object used for downloading
	 */
	public static HttpConnection string(String url) {
		return new HttpConnection(url);
	}
	
	/**
	 * Constructs a http connection for downloading a bitmap
	 * @param url
	 * @return A HttpConnection object used for downloading
	 */
	public static HttpConnection bitmap(String url) {
		HttpConnection conn = new HttpConnection(url);
		conn.isBitmap = true;
		return conn;
	}
	
	private HttpConnection(String url) {
		this.url = url;
		this.exception = null;
		
		httpClient = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), 15000);
	}
	
	/**
	 * Executes the http request
	 * 
	 * @return The data - null if it failed
	 */
	public Object execute(DownloadProgressListener listener) {
		HttpResponse response = null;
		try {
			response = httpClient.execute(new HttpGet(url));
		} catch (ClientProtocolException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			exception = e;
		} catch (IOException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			exception = e;
		}

		Object result = null;
		if (response != null) {
			try {
				if (isBitmap) {
					result = processBitmapEntity(response.getEntity());
				} else {
					result = processEntity(response.getEntity(), listener);
				}
			} catch (IllegalStateException e) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace();
				}
				exception = e;
			} catch (IOException e) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace();
				}
				exception = e;
			}
		}
		
		httpClient.getConnectionManager().shutdown();
		return result;
	}

	private String processEntity(HttpEntity entity, DownloadProgressListener listener) throws IllegalStateException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(entity
				.getContent()));
		
		StringBuilder result = null;
		if (entity.getContentLength() < 0)
			result = new StringBuilder();
		else
			result = new StringBuilder((int)entity.getContentLength() + 150);
		
		String line;
		
		if (listener != null)
			listener.totalSize(entity.getContentLength());

		long currentByteCount = 0;
		long lastUpdate = System.currentTimeMillis() - MIN_TIME_BETWEEN_PROGRESS_UPDATES_MS;
		while ((line = br.readLine()) != null) {
			if (listener != null) {
				// add 1 to account for missing line endings
				//currentByteCount += (line.getBytes().length) + 1;
				currentByteCount += (line.length()) + 1;
					
				if (System.currentTimeMillis() > lastUpdate + MIN_TIME_BETWEEN_PROGRESS_UPDATES_MS) {
					lastUpdate = System.currentTimeMillis();
					listener.progressUpdate(currentByteCount);
				}
			}
		    result.append(line);
		}
		
		return result.toString();
	}
	
	private Bitmap processBitmapEntity(HttpEntity entity) throws IOException {
		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		return BitmapFactory.decodeStream(bufHttpEntity.getContent());
	}
}