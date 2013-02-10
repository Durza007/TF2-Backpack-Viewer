package com.minder.app.tf2backpack.frontend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.frontend.ItemListSelect.ItemListSelectAdapter.OnItemSelectedListener;

public class ItemListSelect extends Activity {
	public static class ItemListSelectAdapter extends BaseAdapter {
		public static interface OnItemSelectedListener {
			public void onSelect(String string);
		}
		
		private class ViewHolder {
			public TextView title;
			public ImageButton imageButton;
			
			public void SetView(View v){
				title = (TextView)v.findViewById(R.id.textViewTitle);
				imageButton = (ImageButton)v.findViewById(R.id.imageButtonItem);
			}
		}
		
		private final ViewHolder holder = new ViewHolder();
		private LayoutInflater inflater;
		private OnItemSelectedListener onSelectListener;
		private TypedArray titles;
		private TypedArray icons;
		private TypedArray links;
		
		public ItemListSelectAdapter(Context context) {
			this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			Resources res = context.getResources();
			
			this.titles = res.obtainTypedArray(R.array.title);
			this.icons = res.obtainTypedArray(R.array.images);
			this.links = res.obtainTypedArray(R.array.list);
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

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_list_item, null);
				holder.SetView(convertView);
				
				holder.imageButton.setOnClickListener(onClickListener);
			} else {
				holder.SetView(convertView);
			}
			
			holder.title.setText(titles.getString(position));
			holder.imageButton.setImageDrawable(icons.getDrawable(position));
			holder.imageButton.setTag(links.getString(position));
			
			return convertView;
		}
		
		OnClickListener onClickListener = new OnClickListener() {
			public void onClick(View v) {
				Log.d("ItemListSelectAdapter", "Clicked: " + (String)v.getTag());
				
				if (onSelectListener != null) {
					onSelectListener.onSelect((String)v.getTag());
				}
			}
		};
		
		public void setOnItemSelectedListener(OnItemSelectedListener listener) {
			this.onSelectListener = listener;
		}
	}
	
	private GridView gridView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_list_select);
		
		gridView = (GridView)findViewById(R.id.itemListSelectGridView);
		
		ItemListSelectAdapter adapter = new ItemListSelectAdapter(this);
		adapter.setOnItemSelectedListener(listener);
		
		gridView.setAdapter(adapter);
	}
	
	OnItemSelectedListener listener = new OnItemSelectedListener() {
		public void onSelect(String string) {
			startActivity(new Intent(ItemListSelect.this, ItemGridViewer.class)
				.setAction("com.minder.app.tf2backpack." + string));
		}
	};
}