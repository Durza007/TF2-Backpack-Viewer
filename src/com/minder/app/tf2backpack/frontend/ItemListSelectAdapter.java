package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.minder.app.tf2backpack.R;

public class ItemListSelectAdapter extends BaseAdapter {	
	private class ViewHolder {
		public TextView title;
		public ImageButton imageButton;
		
		public void setView(View v){
			title = (TextView)v.findViewById(R.id.textViewTitle);
			imageButton = (ImageButton)v.findViewById(R.id.imageButtonItem);
		}
	}
	
	private WeakReference<OnItemSelectedListener> listener;
	private final ViewHolder holder = new ViewHolder();
	private LayoutInflater inflater;
	private final int itemSizePx;
	private TypedArray titles;
	private TypedArray icons;
	private TypedArray links;
	private boolean isItemsSelectable;
	private int selectedIndex;
	
	public ItemListSelectAdapter(Context context, int titleArrayId, int imageArrayId, int itemNameArrayId, int itemSizePx) {
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Resources res = context.getResources();
		
		this.titles = res.obtainTypedArray(titleArrayId);
		this.icons = res.obtainTypedArray(imageArrayId);
		this.links = res.obtainTypedArray(itemNameArrayId);
		
		this.itemSizePx = itemSizePx;
		
		this.selectedIndex = -1;
	}
	
	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		this.listener = new WeakReference<OnItemSelectedListener>(listener);
	}

	public int getCount() {
		return links.length();
	}

	public Object getItem(int index) {
		return links.getString(index);
	}

	public long getItemId(int index) {
		return index;
	}
	
	public int getSelectedIndex() {
		return this.selectedIndex;
	}
	
	public void setSelectedIndex(int selectedIndex) {
		this.selectedIndex = selectedIndex;
	}
	
	public void setItemSelectable(boolean itemSelectable) {
		this.isItemsSelectable = itemSelectable;
		notifyDataSetChanged();
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_list_item, null);
			holder.setView(convertView);
			
			LayoutParams params = holder.imageButton.getLayoutParams();
			params.width = itemSizePx;
			params.height = itemSizePx;
			holder.imageButton.setLayoutParams(params);
			
			holder.imageButton.setOnClickListener(onClickListener);
		} else {
			holder.setView(convertView);
		}
		
		holder.title.setText(titles.getString(position));
		holder.imageButton.setImageDrawable(icons.getDrawable(position));
		holder.imageButton.setTag(position);
		
		if (isItemsSelectable) {
			if (selectedIndex == position) {
				holder.imageButton.setBackgroundResource(R.drawable.tf_button);
			} else {
				holder.imageButton.setBackgroundResource(R.drawable.backpack_cell);
			}
		}
		
		return convertView;
	}
	
	private OnClickListener onClickListener = new OnClickListener() {
		public void onClick(View v) {
			selectedIndex = (Integer)v.getTag();
			
			if (isItemsSelectable)
				notifyDataSetChanged();
			
			final OnItemSelectedListener l = listener.get();
			if (l != null) {
				l.onSelect(links.getString((Integer)v.getTag()));
			}
		}
	};
}