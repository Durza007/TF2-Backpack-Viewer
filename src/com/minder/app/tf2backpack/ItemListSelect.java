package com.minder.app.tf2backpack;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;

import com.minder.app.tf2backpack.ItemListSelectAdapter.OnItemSelectedListener;

public class ItemListSelect extends Activity {
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
