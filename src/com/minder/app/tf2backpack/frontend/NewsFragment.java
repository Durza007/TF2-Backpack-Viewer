package com.minder.app.tf2backpack.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.R;

public class NewsFragment extends Fragment {
	public static class NewsItem {
		private String title;
		private String url;
		private String contents;
		private String date;
		
		public static NewsItem CreateNewsItem(){
			return new NewsItem();
		}
			
		public NewsItem(String title, String url, String contents, long timeStamp){
			this.setTitle(title);
			this.setUrl(url);
			this.setContents(contents);
			this.setDate(timeStamp);
		}

		public NewsItem() {
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getUrl() {
			return url;
		}

		public void setContents(String contents) {
			this.contents = contents;
		}

		public String getContents() {
			return contents;
		}

		public void setDate(long date) {
			Timestamp st = new Timestamp(date);
			this.date = (String)DateFormat.format("dd MMM yyyy", st);
		}

		public String getDate() {
			return date;
		}
	}
	
	private static class NewsAdapter extends BaseAdapter {
		private static class ViewHolder {
			TextView title;
			TextView date;
			TextView content;
		}
		
        private List<NewsItem> newsList;
        
        private LayoutInflater mInflater;        
        private Context mContext;
        
        public NewsAdapter(Context c) {
            mContext = c;
            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            newsList = new ArrayList<NewsItem>();
        }

        public int getCount() {
            return newsList.size();
        }

        public Object getItem(int position) {
            return newsList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {        
            ViewHolder holder;
            if (convertView == null) {
	            convertView = mInflater.inflate(R.layout.news_item, null);
	            holder = new ViewHolder();
	            holder.title = (TextView) convertView
	            .findViewById(R.id.textTitle);
	            holder.date = (TextView) convertView
	            .findViewById(R.id.textDate);
	            holder.content = (TextView) convertView
	            .findViewById(R.id.textContent);
	
	            convertView.setTag(holder);
            } else {
            	holder = (ViewHolder) convertView.getTag();
            }
            
            NewsItem item = newsList.get(position);
            
            holder.title.setText(item.getTitle());
            holder.date.setText(item.getDate());
            if (item.getContents() != null){
            	holder.content.setText(Html.fromHtml(item.getContents()));
            }
            
            return convertView;
        }
        
        private void addNews(NewsItem p) {
        	newsList.add(p);
        	notifyDataSetChanged();
        }
        
        public void setNewsList(List<NewsItem> list) {
        	this.newsList = list;
        }
	}
	
	private ListView newsList;
	private NewsAdapter adapter;
	private AdView adView;

	private View listContainer;
	private View progressContainer;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		this.setRetainInstance(true);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.list_content, container, false);
		
        // Look up the AdView as a resource and load a request.
        adView = (AdView)view.findViewById(R.id.ad);
        
		listContainer = view.findViewById(R.id.listContainer);
		progressContainer = view.findViewById(R.id.progressContainer);
        
        newsList = (ListView)view.findViewById(android.R.id.list);
        
        // Set up our adapter
        if (adapter == null)
        	adapter = new NewsAdapter(getActivity());
        
        newsList.setAdapter(adapter);
        
        newsList.setBackgroundResource(R.color.bg_color);
        newsList.setCacheColorHint(this.getResources().getColor(R.color.bg_color));
        
        newsList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				NewsItem item = (NewsItem)adapter.getItem(arg2);
				startActivity(new Intent().setData(Uri.parse(item.getUrl())).setAction("android.intent.action.VIEW"));
			}
		});
        
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        if (adapter.isEmpty()) {
        	new DownloadNewsTask().execute(sp.getString("newscount", "10"));
			progressContainer.setVisibility(View.VISIBLE);
			listContainer.setVisibility(View.GONE);
        } else {
			progressContainer.setVisibility(View.GONE);
			listContainer.setVisibility(View.VISIBLE);
        }
		
		return view;
	}
    
	@Override
    public void onResume(){
    	super.onResume();
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	if (adView != null) {
    		adView.destroy();
    	}
    }

    private class DownloadNewsTask extends AsyncTask<String, NewsItem, Void>{
    	private int error;
    	
        protected void onPreExecute() {
        	error = 0;
        }
    	
		@Override
		protected Void doInBackground(String... params) {			
			XmlPullParserFactory pullMaker;
			NewsItem result;
			try {
	        	URL xmlUrl = new URL("http://api.steampowered.com/ISteamNews/GetNewsForApp/v0001/?appid=440&count=" + params[0] + "&maxlength=150&format=xml");
	        	
	            pullMaker = XmlPullParserFactory.newInstance();
	
	            XmlPullParser parser = pullMaker.newPullParser();
	            InputStream fis = xmlUrl.openStream();
	
	            parser.setInput(fis, null);
	
	        	boolean newsitem = false;
	        	boolean title = false;
	        	boolean url = false;
	        	boolean contents = false;
	        	boolean date = false;
	        	
	        	result = new NewsItem();
	
	            int eventType = parser.getEventType();
	            while (eventType != XmlPullParser.END_DOCUMENT) {
	                switch (eventType) {
	                case XmlPullParser.START_DOCUMENT:
	                    break;
	                case XmlPullParser.START_TAG:
	                    if (parser.getName().equals("newsitem")) {
	                    	newsitem = true;
	                    	result = new NewsItem();
	                    } else if (parser.getName().equals("title")) {
	                    	title = true;
	                    } else if (parser.getName().equals("url")) {
	                    	url = true;
	                    } else if (parser.getName().equals("contents")) {
	                    	contents = true;
	                    } else if (parser.getName().equals("date")) {
	                    	date = true;
	                    }
	                    break;
	                case XmlPullParser.END_TAG:
	                    if (parser.getName().equals("newsitem")) {
	                    	newsitem = false;
	                    	publishProgress(result);
	                    } else if (parser.getName().equals("title")) {
	                    	title = false;
	                    } else if (parser.getName().equals("url")) {
	                    	url = false;
	                    } else if (parser.getName().equals("contents")) {
	                    	contents = false;
	                    } else if (parser.getName().equals("date")) {
	                    	date = false;
	                    }
	                    break;
	                case XmlPullParser.TEXT:
	                    if (newsitem) {
	                    	if (title){
	                    		result.setTitle(parser.getText());
	                    	} else if (url){
	                    		result.setUrl(parser.getText());
	                    	} else if (contents){
	                    		result.setContents(parser.getText());
	                    	} else if (date){
	                    		result.setDate(Long.parseLong((parser.getText() + "000").substring(0, 13)));
	                    	}
	                    }
	                    break;
	
	                }
	                eventType = parser.next();
	            }
	        } catch (UnknownHostException e) {
	        	error = 1;
	        } catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				error = 2;
				e.printStackTrace();
			}
			return null; 
		}
		
		protected void onProgressUpdate(NewsItem... item) {
			adapter.addNews(item[0]);
		}
		
        protected void onPostExecute(Void result) {
        	// handle errors if we got some
        	if (error == 1){
        		Toast.makeText(getActivity(), R.string.no_steam_api, Toast.LENGTH_LONG).show();
        	} else if (error == 2){
        		Toast.makeText(getActivity(), R.string.failed_download, Toast.LENGTH_LONG).show();
        	}
        	
			progressContainer.setVisibility(View.GONE);
			listContainer.setVisibility(View.VISIBLE);
        }
    	
    }
}
