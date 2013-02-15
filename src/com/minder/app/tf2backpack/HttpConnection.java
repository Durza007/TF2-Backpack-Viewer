package com.minder.app.tf2backpack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

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
	protected Handler handler;
	private String data;
	private int id;
	private int linesToRead = -1;

	private HttpClient httpClient;

	public HttpConnection() {
		this(new Handler());
	}

	public HttpConnection(Handler handler) {
		this.handler = handler;
	}

	public void create(int method, String url, String data) {
		this.method = method;
		this.url = url;
		this.data = data;
		this.id = -1;
		
		ConnectionManager.getInstance().push(this);
	}
	
	public void create(int method, String url, String data, int id) {
		this.method = method;
		this.url = url;
		this.data = data;
		this.id = id;
		ConnectionManager.getInstance().push(this);
	}

	public void get(String url) {
		create(GET, url, null);
	}
	
	public void get(String url, int id) {
		create(GET, url, null, id);
	}
	
	public void getSpecificLines(String url, int linesToRead) {
		create(GET, url, null);
		this.linesToRead = linesToRead;
	}

	public void post(String url, String data) {
		create(POST, url, data);
	}

	public void put(String url, String data) {
		create(PUT, url, data);
	}

	public void delete(String url) {
		create(DELETE, url, null);
	}

	public void bitmap(String url) {
		create(BITMAP, url, null);
	}
	
	public void bitmap(String url, int id) {
		create(BITMAP, url, null, id);
	}

	public void run() {
		handler.sendMessage(Message.obtain(handler, HttpConnection.DID_START));
		httpClient = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), 10000);
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
				processBitmapEntity(response.getEntity());
				break;
			}
			if (method < BITMAP) {
				if (BuildConfig.DEBUG) {
					Header[] headers = response.getAllHeaders();
					
					for (int i = 0; i < headers.length; i++) {
						Log.d("HttpConnection-HEader", headers[i].toString());
					}
				}
				
				processEntity(response.getEntity());
			}
		} catch (Exception e) {
			handler.sendMessage(Message.obtain(handler,
					HttpConnection.DID_ERROR, e));
		}
		ConnectionManager.getInstance().didComplete(this);
	}

	private void processEntity(HttpEntity entity) throws IllegalStateException,
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
		
		sendResult(DID_SUCCEED, result.toString());
	}

	private void processBitmapEntity(HttpEntity entity) throws IOException {
		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
		if (this.id > -1){
			sendResult(this.id, bm);
		} else {
			sendResult(DID_SUCCEED, bm);
		}
	}

	public void sendResult(int message, Object result) {
		handler.sendMessage(Message.obtain(handler, message, result));
	}
}
