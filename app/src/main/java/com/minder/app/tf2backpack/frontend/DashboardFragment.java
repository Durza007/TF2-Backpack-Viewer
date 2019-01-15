package com.minder.app.tf2backpack.frontend;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.DataManager;

public class DashboardFragment extends Fragment {
	private ItemListSelectAdapter adapter;
	private Intent backpackIntent;
	private Intent settingsIntent;
	private String playerId;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_menu, container, false);
        
        backpackIntent = new Intent(getActivity(), SelectBackpack.class);
        settingsIntent = new Intent(getActivity(), SettingsActivity.class);
        
        final GridView gridView = view.findViewById(R.id.itemListSelectGridView);
        
        final int columnWidth = getResources().getDimensionPixelSize(R.dimen.button_size_dashboard_item);
        adapter = new ItemListSelectAdapter(getActivity(), 
        		R.array.dashboardTitles, 
        		R.array.dashboardImages, 
        		R.array.dashboardActions,
        		columnWidth);
        
        adapter.setOnItemSelectedListener(itemSelectedListener);
        
        gridView.setAdapter(adapter);
        gridView.setColumnWidth(columnWidth);
        
        return view;
    }

    void ensureGameSchemeReady(final Intent intent) {
		if (DataManager.isGameSchemeReady()) {
			startActivity(intent);
		} else {
			Exception error = DataManager.getPendingGameSchemeException();
			if (error != null) {
				GenericDialogHC.newInstance(
						getResources().getString(R.string.failed_download),
						getResources().getString(R.string.download_gamescheme_error_message) + " " + error.getLocalizedMessage())
						.setPositiveButtonText(R.string.try_again)
						.setNegativeButtonText(R.string.dismiss)
						.setClickListener(new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialogInterface, int which) {
								dialogInterface.dismiss();
								if (which == DialogInterface.BUTTON_POSITIVE) {
									App.getDataManager().requestSchemaFilesDownload(false);
									DownloadProgressDialog.show(getActivity().getSupportFragmentManager(), (DownloadProgressDialog.ClosedListener) null);
								}
							}
						})
						.show(getActivity().getFragmentManager(), "errorDialog");
			} else {
				if (!DataManager.isGameSchemeDownloading()) {
					App.getDataManager().requestSchemaFilesDownload(false);
				}

				DownloadProgressDialog.show(getActivity().getSupportFragmentManager(), new DownloadProgressDialog.ClosedListener() {
					public void onClosed(boolean dismissed) {
						if (!dismissed && DataManager.isGameSchemeReady()) {
							startActivity(intent);
						}
					}
				});
			}
		}
	}
    
    OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {	
		public void onSelect(String string) {
			if (string.equals("VIEW_BACKPACK")) {
				ensureGameSchemeReady(backpackIntent);
			} else if (string.equals("VIEW_FRIENDS")) {
				startActivity(new Intent(getActivity(), PlayerListActivity.class).setAction("com.minder.app.tf2backpack.VIEW_FRIENDS"));
			} else if (string.equals("VIEW_ITEM_LISTS")) {
				ensureGameSchemeReady(new Intent(getActivity(), CatalogActivity.class));
			} else if (string.equals("VIEW_SETTINGS")) {
				startActivity(settingsIntent);
			}
		}
	};
}