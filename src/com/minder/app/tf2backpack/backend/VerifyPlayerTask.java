package com.minder.app.tf2backpack.backend;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.AsyncTask;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.DataManager.Request;

class VerifyPlayerTask extends AsyncTask<String, Void, SteamUser> {
	private static final int NUM_RETRIES = 5;
	private final AsyncTaskListener listener;
	private final Request request;
	
	public VerifyPlayerTask(AsyncTaskListener listener, Request request) {
		this.listener = listener;
		this.request = request;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}

	@Override
	protected SteamUser doInBackground(String... params) {
        final XmlPullParserFactory pullMaker;
        final XmlPullParser parser;
    	
        try {
			pullMaker = XmlPullParserFactory.newInstance();
			
			parser = pullMaker.newPullParser();
		} catch (XmlPullParserException e1) {
			e1.printStackTrace();
			return null;
		}
		
		String id = params[0];
		
		// try to parse the id
		String newId = convertTo64BitId(id);
		
		for (int tries = 0; tries < NUM_RETRIES; tries++) {
			SteamUser user = null;
			Log.d("VerifyPlayerTask", "Check " + tries);
			boolean steamError = false;
			
			// if the string was changed, it was changed to 64 bit steam id
			if (newId.equals(id)) {
				id = java.net.URLEncoder.encode(id);
				
				try {
					URL url = new URL("http://steamcommunity.com/id/" + id + "/?xml=1");
					
					InputStream fis = url.openStream();
					
		            parser.setInput(fis, null);
		            
		            user = parseXml(parser);
				} catch (MalformedURLException e) {
					request.exception = e;
					return null;
				} catch (XmlPullParserException e) {
					e.printStackTrace();
				} catch (IOException e) {
					request.exception = e;
					e.printStackTrace();
				} catch (SteamException e) {
					request.exception = e;
					steamError = true;
				}
				
			}
			
			if (user != null)
				return user;
			Log.d("VerifyPlayerTask", "Check " + tries + " 2");
			try {
				Long.parseLong(newId);
				URL url = new URL("http://steamcommunity.com/profiles/" + newId + "/?xml=1");
				
				InputStream fis = url.openStream();
				
	            parser.setInput(fis, null);      

	            user = parseXml(parser);
			} catch (MalformedURLException e) {
				request.exception = e;
				return null;
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				request.exception = e;
				e.printStackTrace();
			} catch (SteamException e) {
				request.exception = e;
				return null;
			} catch (NumberFormatException e) {
				if (steamError)
					return null;
			}
			
			if (user != null)
				return user;
		}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(SteamUser result) {
		request.data = result;
		listener.onPostExecute(request);
		App.getDataManager().removeRequest(request);
	}
	
	private SteamUser parseXml(XmlPullParser parser) throws XmlPullParserException, IOException, SteamException {
		boolean steamID64 = false;
		boolean steamName = false;
		boolean error = false;
		
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
            case XmlPullParser.START_DOCUMENT:
                break;
            case XmlPullParser.START_TAG:
                if (parser.getName().equals("steamID64")) {
                	steamID64 = true;
                } else if (parser.getName().equals("error")) {
                	error = true;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getName().equals("steamID64")) {
                	steamID64 = false;
                } else if (parser.getName().equals("error")) {
                	error = false;
                }
                break;
            case XmlPullParser.TEXT:
                if (steamID64) {
                	SteamUser user = new SteamUser();
                	user.steamdId64 = Long.parseLong(parser.getText());
                	
                	return user;
                } else if (error) {
                	throw new SteamException(parser.getText());
                }
                break;

            }

			eventType = parser.next();

        }
		return null;
	}
	
	private String convertTo64BitId(String id) {
		//TODO these two regex statements seem redundant
		boolean steamId = id.matches("(?i)STEAM_\\d:\\d:\\d+");
		if (!steamId) {
			steamId = id.matches("\\d:\\d:\\d+");
		}
		if (steamId) {
			final int yIndex = id.indexOf(":");
			final long y = Long.parseLong(id.substring(yIndex + 1, yIndex + 2));

			final int zIndex = id.indexOf(':', yIndex + 1);
			final long z = Long.parseLong(id.substring(zIndex + 1));
			;

			final long communityId = (z * 2) + 76561197960265728l + y;

			id = String.valueOf(communityId);
		} else if (id.matches("\\d:\\d+")) {
			// X:XXXXXX format
			final long y = Long.parseLong(id.substring(0, 1));

			final int zIndex = id.indexOf(':', 1);
			final long z = Long.parseLong(id.substring(zIndex + 1));
			;

			final long communityId = (z * 2) + 76561197960265728l + y;

			id = String.valueOf(communityId);
		}

		return id;
	}
}