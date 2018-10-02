package com.minder.app.tf2backpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlayerAdapter extends BaseAdapter {	
	private class ViewHolder implements ImageLoader.ImageLoadedInterface {
		ViewGroup container;
		ImageView avatar;
		ImageView state;
		TextView text;
		TextView text2;
		String loadingUrl;

		public void imageReady(String url, Bitmap bitmap) {
			if (url == loadingUrl) {
				avatar.setImageBitmap(bitmap);
			}
		}
	}
	
	private Activity activity;
    private List<SteamUser> playerList = new ArrayList<SteamUser>();
    
    private LayoutInflater mInflater;        
    private ImageLoader imageLoader;
    private Bitmap defaultImage;
    private int imageSize;
    private int selectedIndex;
    private boolean showAvatars = false;
    
    private Comparator<SteamUser> comparator;
    
    public PlayerAdapter(Activity activity) {
    	this.activity = activity;
    	
        mInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);    
        
        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(new int[] {android.R.attr.listPreferredItemHeight});
        imageSize = typedArray.getDimensionPixelSize(0, 0);
        typedArray.recycle();
        
        Log.d("PlayerAdapter", "list size: " + imageSize);
        
        //imageSize = activity.getResources().getDimensionPixelSize(android.R.attr.listPreferredItemHeight);
        
        imageLoader = new ImageLoader(activity);
		defaultImage = BitmapFactory.decodeResource(activity.getResources(), R.drawable.avatar_64blank);
		
		{ // scale the bitmap
			Bitmap newImage = Bitmap.createScaledBitmap(defaultImage, imageSize, imageSize, false);			
			if (newImage != defaultImage) {
				defaultImage.recycle();
			}			
			defaultImage = newImage;
		}
		
		selectedIndex = -1;
		comparator = null;
    }
    
    @Override
    public void notifyDataSetChanged() {
    	sort();
    	super.notifyDataSetChanged();
    }
    
    private OnImageDownloaded onBitmapLoaded = new OnImageDownloaded() {
		public void onBitmapLoaded(Bitmap bitmap) {
			notifyDataSetChanged();
		}
	};

    public int getCount() {
        return playerList.size();
    }

    public Object getItem(int position) {
        return playerList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        // Create textview to hold player name
        
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item, null);
            holder = new ViewHolder();
            holder.container = (ViewGroup)convertView.findViewById(R.id.RelativeLayoutItem);
            holder.avatar = (ImageView)convertView.findViewById(R.id.imageViewAvatar);
            holder.state = (ImageView)convertView.findViewById(R.id.imageViewState);
            holder.text = (TextView)convertView.findViewById(R.id.text1);
            holder.text2 = (TextView)convertView.findViewById(R.id.text2);

            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder)convertView.getTag();
        }
        
        if (position == selectedIndex) {
        	holder.container.setBackgroundResource(R.color.button_color);
        	holder.text.setTextAppearance(activity, android.R.style.TextAppearance_DeviceDefault_Large_Inverse);
        } else {
        	holder.container.setBackgroundResource(0);
        	holder.text.setTextAppearance(activity, android.R.style.TextAppearance_DeviceDefault_Large);
        }
        
        SteamUser player = playerList.get(position);
        
        if (showAvatars) {
        	//holder.avatar.setVisibility(View.VISIBLE);
        	//holder.state.setVisibility(View.VISIBLE);
        	
	        // set player avatar
	        if (player.avatarUrl != null && player.avatarUrl.length() > 0) {
				holder.loadingUrl = player.avatarUrl;
	    		Bitmap b = imageLoader.displayImage(player.avatarUrl, holder, imageSize,false);
	    		if (b != null)
	    			holder.avatar.setImageBitmap(b);
	    		else
	    			holder.avatar.setImageBitmap(defaultImage);
	        } else {
	            // set default image
	            holder.avatar.setImageBitmap(defaultImage);
	        }
        } else {
        	holder.avatar.setImageBitmap(defaultImage);
        }
        
        boolean statusUpdated = false;
        if (player.gameId != null) {
        	if (player.gameId.length() > 0) {
        		holder.state.setImageResource(R.drawable.avatar_border_in_game);
        		statusUpdated = true;
        	}
        }
        
        if (!statusUpdated) {
	        if (player.personaState.value >= PersonaState.Online.value) {
	        	holder.state.setImageResource(R.drawable.avatar_border_online);
	        } else {
	        	holder.state.setImageResource(R.drawable.avatar_border_offline);
	        }
        }
        
        if (player.steamName != null && player.steamName.length() > 0){
        	holder.text.setText(player.steamName);
        } else {
        	holder.text.setText(R.string.loading);
        }
        
        if (player.wrenchNumber != 0){
        	holder.text2.setText("#" + player.wrenchNumber);
        }
        
        return convertView;
    }

    public void clearPlayers() {
    	playerList.clear();
        notifyDataSetChanged();
    }
    
    public void addPlayer(SteamUser p) {
    	playerList.add(p);
    	notifyDataSetChanged();
    }
    
    public void setPlayers(List<SteamUser> data) {
    	playerList.clear();
    	playerList.addAll(data);
		notifyDataSetChanged();
    }
    
    /**
     * Get the list of players
     * @return The internal list of players/steam users
     */
    public List<SteamUser> getPlayers() {
    	return this.playerList;
    }
    
    public boolean addPlayerInfo(SteamUser p){
    	if (p != null) {
        	for (SteamUser baseP : playerList) {
        		if (baseP.steamdId64 == p.steamdId64){
        			/*int index = mPlayers.indexOf(baseP);
        			mPlayers.remove(index);
        			mPlayers.add(index, p);*/
        			if (p.steamName != null){
        				
        				baseP.steamName = p.steamName;
            			notifyDataSetChanged();
        			}
        			return false;
        		}
        	}
        	addPlayer(p);
    	}
    	return true;
    }
    
    public void addPlayerInfoList(ArrayList<SteamUser> players) {
		for(SteamUser p : players){
	    	boolean changed = false;
        	for (SteamUser baseP : playerList) {
        		if (baseP.steamdId64 == p.steamdId64){
        			if (p.steamName != null){
        				baseP.steamName = p.steamName;
        				changed = true;
        			}
        		}
        	}
        	if (!changed) {
        		addPlayer(p);
        	}
		}
		notifyDataSetChanged();
    }
    
    /**
     * Sorts the list if sorting is enabled
     */
    private void sort() {
    	if (comparator != null) {
    		Collections.sort(this.playerList, comparator);
    	}
    }

	public void setComparator(Comparator<SteamUser> comparator) {
		this.comparator = comparator;
		notifyDataSetChanged();
	}
	
	public void stopBackgroundLoading() {
		imageLoader.stopThread();
	}
	
	public void startBackgroundLoading() {
		imageLoader.startThread();
	}

	public void setShowAvatars(Boolean showAvatars) {
		this.showAvatars = showAvatars;
	}

	public void setSelectedItem(int index) {
		selectedIndex = index;
		notifyDataSetChanged();
	}
}
