package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.minder.app.tf2backpack.R;

public class ItemListSelectFragment extends Fragment {
	private WeakReference<OnItemSelectedListener> listener;
	private GridView gridView;
	private ItemListSelectAdapter adapter;
	private int columns;
	private boolean isItemsSelectable;
	
	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		this.listener = new WeakReference<OnItemSelectedListener>(listener);
		
		if (adapter != null)
			adapter.setOnItemSelectedListener(listener);
	}
	
	public void setItemsSelectable(boolean selectable) {
		isItemsSelectable = selectable;
		if (adapter != null)
			adapter.setItemSelectable(isItemsSelectable);
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
		
		final int columnWidth = getResources().getDimensionPixelSize(R.dimen.button_size_itemlist);
		adapter = new ItemListSelectAdapter(getActivity(), 
				R.array.title, 
				R.array.images, 
				R.array.list,
				columnWidth);
		
		adapter.setItemSelectable(isItemsSelectable);
		
		final OnItemSelectedListener l = listener.get();
		if (l != null)
			adapter.setOnItemSelectedListener(l);
		
		gridView.setAdapter(adapter);
		gridView.setColumnWidth(columnWidth);
		
		return view;
	}
}
