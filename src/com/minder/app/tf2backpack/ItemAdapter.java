package com.minder.app.tf2backpack;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.minder.app.tf2backpack.PlayerItemListParser.Item;

public class ItemAdapter extends BaseAdapter {
	private class ViewHolder {
		public TextView textCount;
		public ImageButton imageButton;
		public ImageView colorSplat;
		public FrameLayout root;
		
		public void SetView(View v){
			root = (FrameLayout)v.findViewById(R.id.FrameLayoutRoot);
			colorSplat = (ImageView)v.findViewById(R.id.ImageViewItemColor);
			textCount = (TextView)v.findViewById(R.id.TextViewCount);
			imageButton = (ImageButton)v.findViewById(R.id.ImageButtonCell);
		}
	}
	
	private Activity activity;
    private LayoutInflater mInflater;
    private Context mContext;
    private ImageLoader imageLoader; 
	private ArrayList<Item> itemList;
	private boolean coloredCells;
	private int imageSize;
	private OnClickListener currentClickListener;
	private Bitmap defaultImage;
	
	public ItemAdapter(Activity activity, boolean coloredCells) {
		this.activity = activity;
		this.mInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.coloredCells = coloredCells;
		imageSize = activity.getResources().getDimensionPixelSize(R.dimen.button_size_itemlist);
		imageLoader = new ImageLoader(activity, imageSize);
		itemList = new ArrayList<Item>();
		
		defaultImage = BitmapFactory.decodeResource(activity.getResources(), R.drawable.unknown);
		if (defaultImage != null){
			Bitmap newImage = Bitmap.createScaledBitmap(defaultImage, imageSize, imageSize, false);
			if (newImage != defaultImage) {
				defaultImage.recycle();
			}
			defaultImage = newImage;
		}
	}
	
	public void setOnClickListener(OnClickListener l) {
		this.currentClickListener = l;
	}

	public int getCount() {
		return itemList.size();
	}

	public Object getItem(int position) {
		return itemList.get(position);
	}

	public long getItemId(int position) {
		return position;
	}
	
	public void setList(ArrayList<Item> list) {
		itemList.clear();
		itemList.addAll(list);
		notifyDataSetChanged();
	}
	
	public void addItem(Item item) {
		itemList.add(item);
		notifyDataSetChanged();
	}
	
	public ArrayList<Item> getList() {
		return itemList;
	}

	public View getView(int position, View convertView, ViewGroup parent) {	        
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.backpack_cell, null);
            holder = new ViewHolder();
            holder.SetView(convertView);            
            holder.imageButton.setOnClickListener(onClickListener);

            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder)convertView.getTag();
        }
        
        Item item = itemList.get(position);
        holder.imageButton.setTag(position);

		imageLoader.DisplayImage(item.getDefIndex() + ".png", (Activity)mContext, holder.imageButton, true, defaultImage);
		
		if (coloredCells == true){
			final int quality = item.getQuality();
			if (quality >= 1 && quality <= 9){
				if (quality != 4 || quality != 6 || quality != 2){
					holder.imageButton.setBackgroundResource(R.drawable.backpack_cell_white);
					holder.imageButton.getBackground().setColorFilter(Util.getItemColor(quality), PorterDuff.Mode.MULTIPLY);
				}
			}
			else {
				holder.imageButton.setBackgroundResource(R.drawable.backpack_cell);
				holder.imageButton.getBackground().clearColorFilter();
			}
		}
		
		if (item.getQuantity() > 1){
			holder.textCount.setVisibility(View.VISIBLE);
			holder.textCount.setText(String.valueOf(item.getQuantity()));
		} else {
			holder.textCount.setVisibility(View.GONE);
		}
		
		int color = item.getColor();
		if (color != 0){
			if (color == 1){		
				holder.colorSplat.setImageBitmap(
						BitmapFactory.decodeResource(
								activity.getResources(), R.drawable.color_circle_team_spirit));
				holder.colorSplat.setVisibility(View.VISIBLE);
				holder.colorSplat.setColorFilter(null);
			} else {
				//ColorFilter filter = new LightingColorFilter((0xFF << 24) | color, 1);
				holder.colorSplat.setImageBitmap(
						BitmapFactory.decodeResource(
								activity.getResources(), R.drawable.color_circle));
				holder.colorSplat.setVisibility(View.VISIBLE);
				holder.colorSplat.setColorFilter(null);
				holder.colorSplat.setColorFilter((0xFF << 24) | color, PorterDuff.Mode.SRC_ATOP);
			}
		} else {
			holder.colorSplat.setVisibility(View.GONE);
		}
        
        return convertView;
	}
	
	OnClickListener onClickListener = new OnClickListener() {
		public void onClick(View v) {
			if (currentClickListener != null) {
				currentClickListener.onClick(v);
			}
		}	
	};
}
