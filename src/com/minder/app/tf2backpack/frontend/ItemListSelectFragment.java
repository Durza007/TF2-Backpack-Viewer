package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class ItemListSelectFragment extends Fragment {
	public static interface OnItemSelectedListener {
		public void onSelect(String string);
	}
	
	public class ItemListSelectAdapter extends BaseAdapter {	
		private class ViewHolder {
			public TextView title;
			public ImageButton imageButton;
			
			public void setView(View v){
				title = (TextView)v.findViewById(R.id.textViewTitle);
				imageButton = (ImageButton)v.findViewById(R.id.imageButtonItem);
			}
		}
		
		private final ViewHolder holder = new ViewHolder();
		private LayoutInflater inflater;
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
				holder.setView(convertView);
				
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
				
				OnItemSelectedListener l = listener.get();
				if (l != null) {
					l.onSelect(links.getString((Integer)v.getTag()));
				}
			}
		};
	}
	
	private WeakReference<OnItemSelectedListener> listener;
	private GridView gridView;
	private ItemListSelectAdapter adapter;
	private int columns;
	private boolean isItemsSelectable;
	private int selectedIndex;
	
	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		this.listener = new WeakReference<OnItemSelectedListener>(listener);
	}
	
	public void setItemsSelectable(boolean selectable) {
		isItemsSelectable = selectable;
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}
	
	public void setNumberOfColumns(int columns) {
		this.columns = columns;
		if (gridView != null) {
			gridView.setNumColumns(columns);
		}
	}
	
	public ItemListSelectFragment() {
		listener = new WeakReference<OnItemSelectedListener>(null);
		columns = -1;
		selectedIndex = -1;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.item_list_select, container, false);
		
		gridView = (GridView)view.findViewById(R.id.itemListSelectGridView);
		if (columns != -1)
			setNumberOfColumns(columns);
		
		adapter = new ItemListSelectAdapter(getActivity());	
		gridView.setAdapter(adapter);
		
		return view;
	}
}
