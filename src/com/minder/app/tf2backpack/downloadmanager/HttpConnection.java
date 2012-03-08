package com.minder.app.tf2backpack.downloadmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import com.minder.app.tf2backpack.ConnectionManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Asynchronous HTTP connections
 * 
 * @author Greg Zavitz & Joseph Roth
 */
public class HttpConnection implements Runnable {
	public static final int DID_START = -1;
	public static final int DID_ERROR = -2;
	public static final int DID_SUCCEED = -3;

	private static final int GET = 0;
	private static final int POST = 1;
	private static final int PUT = 2;
	private static final int DELETE = 3;
	private static final int BITMAP = 4;

	private String url;
	private int method;
	private Handler handler;
	private String data;
	private int id;
	private int linesToRead = -1;
	private int cacheTimeSeconds;
	private boolean shouldCache;
	private File localFile;

	private HttpClient httpClient;

	public HttpConnection() {
		this(null);
	}

	public HttpConnection(Handler handler) {
		this.handler = handler;
	}
	
	private void create(int method, String url, String data, int id, int cacheTimeSeconds) {
		this.method = method;
		this.url = url;
		this.data = data;
		this.id = id;
		this.cacheTimeSeconds = cacheTimeSeconds;
		if (cacheTimeSeconds > 0) {
			if (checkCacheFile()) {
				Thread t = new Thread(this);
				t.start();
				return;
			}
		} else if (cacheTimeSeconds == 0) {
			shouldCache = true;
		}
		
		ConnectionManager.getInstance().push(this);
	}
	
	private void create(int method, String url, String data, int cacheTimeSeconds) {
		create(method, url, data, -1, cacheTimeSeconds);
	}
	
	private Object createDirect(int method, String url, String data, int id, int cacheTimeSeconds) {
		this.method = method;
		this.url = url;
		this.data = data;
		this.id = id;
		this.cacheTimeSeconds = cacheTimeSeconds;
		if (cacheTimeSeconds > 0) {
			checkCacheFile();
		} else if (cacheTimeSeconds == 0) {
			shouldCache = true;
		}
		
		return process();
	}
	
	private Object createDirect(int method, String url, String data, int cacheTimeSeconds) {
		return createDirect(method, url, data, -1, cacheTimeSeconds);
	}
	
	private boolean checkCacheFile() {
		localFile = CacheManager.getInstance().getFile(url, cacheTimeSeconds);
		
		if (localFile == null) {
			shouldCache = true;
			return false;
		}
		
		shouldCache = false;
		return true;
	}

	public void get(String url, int cacheTimeSeconds) {
		create(GET, url, null, cacheTimeSeconds);
	}
	
	public Object getDirect(String url, int cacheTimeSeconds) {
		return createDirect(GET, url, null, cacheTimeSeconds);
	}
	
	public void get(String url, int id, int cacheTimeSeconds) {
		create(GET, url, null, id, cacheTimeSeconds);
	}
	
	public void getSpecificLines(String url, int linesToRead, int cacheTimeSeconds) {
		create(GET, url, null, cacheTimeSeconds);
		this.linesToRead = linesToRead;
	}

	public void post(String url, String data, int cacheTimeSeconds) {
		create(POST, url, data, cacheTimeSeconds);
	}

	public void put(String url, String data, int cacheTimeSeconds) {
		create(PUT, url, data, cacheTimeSeconds);
	}

	public void delete(String url, int cacheTimeSeconds) {
		create(DELETE, url, null, cacheTimeSeconds);
	}

	public void bitmap(String url, int cacheTimeSeconds) {
		create(BITMAP, url, null, cacheTimeSeconds);
	}
	
	public void bitmap(String url, int id, int cacheTimeSeconds) {
		create(BITMAP, url, null, id, cacheTimeSeconds);
	}

	public void run() {
		process();
	}
	
	private Object process() {
		Object returnObject = null;
		if (handler != null) 
			handler.sendMessage(Message.obtain(handler, HttpConnection.DID_START));
		if (localFile != null) {
			Log.d("HttpConnection", "Loaded from cache");
			FileEntity fileEntity = new FileEntity(localFile, "data");

			try {
				if (method < BITMAP) {
					returnObject =  processEntity(fileEntity);
				} else {
					returnObject =  processBitmapEntity(fileEntity);
				}
			} catch (Exception e) {
				if (handler != null) 
					handler.sendMessage(Message.obtain(handler,
							HttpConnection.DID_ERROR, e));
			}
		}
		else
		{
			Log.d("HttpConnection", "Loaded from internet");
			httpClient = new DefaultHttpClient();
			HttpConnectionParams.setSoTimeout(httpClient.getParams(), 25000);
			try {
				HttpResponse response = null;
				switch (method) {
				case GET:
					response = httpClient.execute(new HttpGet(url));
					break;
				case POST:
					HttpPost httpPost = new HttpPost(url);
					httpPost.setEntity(new StringEntity(data));
					response = httpClient.execute(httpPost);
					break;
				case PUT:
					HttpPut httpPut = new HttpPut(url);
					httpPut.setEntity(new StringEntity(data));
					response = httpClient.execute(httpPut);
					break;
				case DELETE:
					response = httpClient.execute(new HttpDelete(url));
					break;
				case BITMAP:
					response = httpClient.execute(new HttpGet(url));
					returnObject = processBitmapEntity(response.getEntity());
					break;
				}
				if (method < BITMAP)
					returnObject = processEntity(response.getEntity());
			} catch (Exception e) {
				if (handler != null) 
					handler.sendMessage(Message.obtain(handler,
							HttpConnection.DID_ERROR, e));
			}
			ConnectionManager.getInstance().didComplete(this);
		}
		return returnObject;
	}

	private Object processEntity(HttpEntity entity) throws IllegalStateException,
			IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(entity
				.getContent()));
		StringBuilder result = new StringBuilder();
		String line;
		if (linesToRead > -1){
			int linesRead = 0;
			while ((line = br.readLine()) != null) {
			    result.append(line);
			    linesRead++;
			    if (linesRead >= linesToRead){
			    	br.close();
			    	break;
			    }
			}
		} else {
			while ((line = br.readLine()) != null) {
			    result.append(line);
			}
		}
		
		String resultData = result.toString();
		if (shouldCache) {
			CacheManager.getInstance().cacheFile(url, resultData);
		}	
		
		if (handler != null) {
			Message message = Message.obtain(handler, DID_SUCCEED, resultData);
			handler.sendMessage(message);
		}
		return resultData;
	}

	private Object processBitmapEntity(HttpEntity entity) throws IOException {
		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
		
		if (handler != null) {
			if (this.id > -1){
				handler.sendMessage(Message.obtain(handler, this.id, bm));
			} else {
				handler.sendMessage(Message.obtain(handler, DID_SUCCEED, bm));
			}
		}
		return bm;
	}

}
