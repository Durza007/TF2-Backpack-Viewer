package com.minder.app.tf2backpack.backend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

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
import android.util.Log;

import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.Util;

public class HttpConnection {
	public static interface DownloadProgressListener {
		public void totalSize(long totalSize);
		public void progressUpdate(long currentSize);
	}
	public final static long MIN_TIME_BETWEEN_PROGRESS_UPDATES_MS = 400;
	
	private static class ProgressInputStream extends FilterInputStream {
		private DownloadProgressListener listener;
		private long byteCount;
		private long lastUpdate;

		protected ProgressInputStream(InputStream in, DownloadProgressListener listener) {
			super(in);
			
			this.listener = listener;
			this.byteCount = 0;
			this.lastUpdate = System.currentTimeMillis();
		}
		
		private void progressUpdate() {
			if (listener == null)
				return;
			
			if (byteCount == -1)
				listener.progressUpdate(-1);
			
			if (System.currentTimeMillis() > lastUpdate + MIN_TIME_BETWEEN_PROGRESS_UPDATES_MS) {
				lastUpdate = System.currentTimeMillis();
				listener.progressUpdate(byteCount);
			}
		}
		
		@Override
		public int read() throws IOException {
			++byteCount;		
			progressUpdate();
			
			return super.read();
		}
		
		@Override
		public int read(byte[] buffer, int offset, int count) throws IOException {
			int read = super.read(buffer, offset, count);
			if (read == -1)
				byteCount = -1;
			else
				byteCount += read;
			
			progressUpdate();
			
			return read;
		}
	}
	
	private HttpClient httpClient;
	private Exception exception;
	private URL url;
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
	public static HttpConnection string(String url) throws MalformedURLException {
		return new HttpConnection(url);
	}
	
	/**
	 * Constructs a http connection for downloading a bitmap
	 * @param url
	 * @return A HttpConnection object used for downloading
	 */
	public static HttpConnection bitmap(String url) throws MalformedURLException {
		HttpConnection conn = new HttpConnection(url);
		conn.isBitmap = true;
		return conn;
	}
	
	private HttpConnection(String url) throws MalformedURLException {
		this.url = new URL(url);
		this.exception = null;
		
		httpClient = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), 15000);
	}
	
	public InputStream executeStream(DownloadProgressListener listener) {
		InputStream stream = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			Log.d(Util.GetTag(), "HttpConnection: contentLength: " + connection.getContentLength());

			if (listener != null)
				listener.totalSize(connection.getContentLength());

			stream = connection.getInputStream();
		} catch (ClientProtocolException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			exception = e;
		} catch (FileNotFoundException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			exception = new RuntimeException("Content not found");
		} catch (IOException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			exception = e;
		}

		if (stream != null)
			return new ProgressInputStream(stream, listener);
		else
			return null;
	}
	
	/**
	 * Executes the http request
	 * 
	 * @return The data - null if it failed
	 */
	public Object execute(DownloadProgressListener listener) {
		HttpResponse response = null;
		try {
			response = httpClient.execute(new HttpGet(url.toURI()));
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
		} catch (URISyntaxException e) {
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