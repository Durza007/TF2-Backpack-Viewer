package com.minder.app.tf2backpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources.Theme;
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
	private class ViewHolder {
		ImageView avatar;
		ImageView state;
		TextView text;
		TextView text2;
	}
	
    private ArrayList<SteamUser> playerList = new ArrayList<SteamUser>();
    
    private LayoutInflater mInflater;        
    private ImageLoader imageLoader;
    private Bitmap defaultImage;
    private int imageSize;
    
    private boolean sort;
    private Comparator<SteamUser> comparator;
    
    public PlayerAdapter(Activity activity) {
        mInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);    
        
        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(new int[] {android.R.attr.listPreferredItemHeight});
        imageSize = typedArray.getDimensionPixelSize(0, 0);
        typedArray.recycle();
        
        Log.d("PLayerAdapter", "list size: " + imageSize);
        
        //imageSize = activity.getResources().getDimensionPixelSize(android.R.attr.listPreferredItemHeight);
        
        imageLoader = new ImageLoader(activity, imageSize);
        
		defaultImage = BitmapFactory.decodeResource(activity.getResources(), R.drawable.avatar_64blank);
		if (defaultImage != null){
			Bitmap newImage = Util.getResizedBitmap(defaultImage, imageSize, imageSize);
			defaultImage.recycle();
			defaultImage = newImage;
		}
		
		sort = false;
		comparator = null;
    }

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
        
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item, null);
            holder = new ViewHolder();
            holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewAvatar);
            holder.state = (ImageView) convertView.findViewById(R.id.imageViewState);
            holder.text = (TextView) convertView.findViewById(R.id.text1);
            holder.text2 = (TextView) convertView.findViewById(R.id.text2);

            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder)convertView.getTag();
        }
        
        SteamUser player = playerList.get(position);
        
        /*if (player.avatarUrl != null) {
        	if (player.avatarUrl.length() > 0) {
        		imageLoader.DisplayImage(player.avatarUrl, activity, holder.avatar, false, defaultImage);
        	}
        }*/
        
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
        
        if (player.steamName != null){
        	holder.text.setText(player.steamName);
        } else {
        	holder.text.setTag(this);
        	holder.text.setText(R.string.loading);
        }
        if (player.wrenchNumber != 0){
        	holder.text2.setText("#" + player.wrenchNumber);
        }
        
        return convertView;
        
        /*if (convertView == null) {
            text = (TextView)mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        } else {
            text = (TextView)convertView;
        }
        
        if (mPlayers.get(position).steamName != null){
            text.setText(mPlayers.get(position).steamName);
        } else {
        	text.setText(R.string.loading);
        }          

        return text;*/
    }

    public void clearPlayers() {
    	playerList.clear();
        notifyDataSetChanged();
    }
    
    public void addPlayer(SteamUser p) {
    	playerList.add(p);
    	sort();
    	notifyDataSetChanged();
    }
    
    public void setPlayers(ArrayList<SteamUser> data) {
    	playerList.addAll(data);
		notifyDataSetChanged();
    }
    
    /**
     * Get the list of players
     * @return The internal list of players/steam users
     */
    public ArrayList<SteamUser> getPlayers() {
    	return this.playerList;
    }
    
    public boolean addPlayerInfo(SteamUser p){
    	if (p != null){
        	for (SteamUser baseP : playerList) {
        		if (baseP.steamdId64 == p.steamdId64){
        			/*int index = mPlayers.indexOf(baseP);
        			mPlayers.remove(index);
        			mPlayers.add(index, p);*/
        			if (p.steamName != null){
        				
        				baseP.steamName = p.steamName;
            			notifyDataSetChanged();
        			}
        			if (p.wrenchNumber != 0){
	        			baseP.wrenchNumber = p.wrenchNumber;
	        			notifyDataSetChanged();
        			}
        			return false;
        		}
        	}
        	addPlayer(p);
    	}
    	return true;
    }
    
    /**
     * Sorts the list if sorting is enabled
     */
    private void sort() {
    	if (sort && comparator != null) {
    		Collections.sort(this.playerList, comparator);
    	}
    }
    
    /**
     * Sets if list should be sorted - Works only if comparator has been set
     * @param shouldSort
     */
    public void SetSort(boolean shouldSort) {
    	this.sort = shouldSort;
    }

	public void setComparator(Comparator<SteamUser> comparator) {
		this.comparator = comparator;
	}
}
