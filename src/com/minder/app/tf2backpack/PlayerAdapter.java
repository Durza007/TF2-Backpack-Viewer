package com.minder.app.tf2backpack;

import java.util.ArrayList;

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
	
    public ArrayList<SteamUser> mPlayers = new ArrayList<SteamUser>();
    
    private LayoutInflater mInflater;        
    private Activity activity;
    private ImageLoader imageLoader;
    private Bitmap defaultImage;
    private int imageSize;
    
    public PlayerAdapter(Activity activity) {
        this.activity = activity;
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
    }

    public int getCount() {
        return mPlayers.size();
    }

    public Object getItem(int position) {
        return mPlayers.get(position);
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
            holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewProfile);
            holder.state = (ImageView) convertView.findViewById(R.id.imageViewState);
            holder.text = (TextView) convertView.findViewById(R.id.text1);
            holder.text2 = (TextView) convertView.findViewById(R.id.text2);

            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder)convertView.getTag();
        }
        
        SteamUser player = mPlayers.get(position);
        
        if (player.avatarUrl != null) {
        	if (player.avatarUrl.length() > 0) {
        		//imageLoader.DisplayImage(player.avatarUrl, activity, holder.avatar, false, defaultImage);
        	}
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
    	mPlayers.clear();
        notifyDataSetChanged();
    }
    
    public void addPlayer(SteamUser p) {
    	mPlayers.add(p);
    	notifyDataSetChanged();
    }
    
    public boolean addPlayerInfo(SteamUser p){
    	if (p != null){
        	for (SteamUser baseP : mPlayers) {
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
}
